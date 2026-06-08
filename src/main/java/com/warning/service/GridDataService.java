package com.warning.service;

import com.warning.dto.GridDataResponse;
import com.warning.dto.GridDataSet;
import com.warning.dto.TimeListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GridDataService {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Map<String, Integer> FORECAST_HOURS = new HashMap<>();

    static {
        FORECAST_HOURS.put("ER01", 1);
        FORECAST_HOURS.put("ER06M", 2);
        FORECAST_HOURS.put("EDA10", 3);
        FORECAST_HOURS.put("VIS", 3);
    }

    @Resource
    private RestTemplate restTemplate;

    @Value("${grid.api.base-url}")
    private String baseUrl;

    @Value("${grid.api.time-list-url}")
    private String timeListUrl;

    @Value("${grid.api.api-key}")
    private String apiKey;

    // Cache grid data to avoid repeated requests within one update cycle.
    // Stores both single GridDataSet and List<GridDataSet> (batch)
    private final Map<String, Object> dataCache = new HashMap<>();

    // Cache the selected time for each element within one update cycle.
    private final Map<String, String> selectedTimeCache = new HashMap<>();

    // Cache time series results (element_count_forward -> selected time list)
    private final Map<String, List<String>> timeSeriesCache = new HashMap<>();

    // 记录本轮已打印过网格元数据的要素，避免重复刷屏
    private final java.util.Set<String> loggedGridElements = new java.util.HashSet<>();

    /**
     * Select a grid time relative to the current moment.
     * Actual elements use the latest time not after now.
     * Forecast elements use the time closest to now + forecast horizon.
     */
    private String fetchTargetTime(String element) {
        if (selectedTimeCache.containsKey(element)) {
            return selectedTimeCache.get(element);
        }

        try {
            String url = String.format("%s?element=%s", timeListUrl, element);
            TimeListResponse response = restTemplate.getForObject(url, TimeListResponse.class);

            if (response == null || response.getReturnCode() == null || response.getReturnCode() != 0) {
                log.error("时间列表接口异常: element={}, response={}", element, response);
                return null;
            }

            List<String> ds = response.getDs();
            if (ds == null || ds.isEmpty()) {
                log.error("时间列表为空: element={}", element);
                return null;
            }

            List<LocalDateTime> candidates = ds.stream()
                    .map(t -> LocalDateTime.parse(t, INPUT_FORMATTER))
                    .sorted()
                    .collect(Collectors.toList());

            LocalDateTime selectedTime = selectBestTime(element, candidates);
            if (selectedTime == null) {
                log.error("未找到合适时次: element={}, ds={}", element, ds);
                return null;
            }

            String formattedTime = selectedTime.format(OUTPUT_FORMATTER);
            selectedTimeCache.put(element, formattedTime);
            log.info("网格时次选择: element={}, selected={}", element, selectedTime.format(INPUT_FORMATTER));
            return formattedTime;
        } catch (Exception e) {
            log.error("查询目标时次失败: element={}", element, e);
            return null;
        }
    }

    private LocalDateTime selectBestTime(String element, List<LocalDateTime> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        Integer forecastHours = FORECAST_HOURS.get(element);

        if (forecastHours == null) {
            return candidates.stream()
                    .filter(t -> !t.isAfter(now))
                    .max(Comparator.naturalOrder())
                    .orElse(candidates.get(0));
        }

        LocalDateTime target = now.plusHours(forecastHours);
        return candidates.stream()
                .min(Comparator
                        .comparing((LocalDateTime t) -> Math.abs(Duration.between(target, t).toMinutes()))
                        .thenComparing(t -> t.isBefore(target) ? 1 : 0))
                .orElse(null);
    }

    /**
     * Get grid data using the selected time relative to the current moment.
     * element: 10MIN-PRE(actual rain 10min), HOR-PRE(actual rain), ER06M(6min rain forecast), ER01(1h rain forecast),
     * HOR-WIN(actual wind), EDA10(wind forecast), HOR-VIS(actual visibility), VIS(visibility forecast)
     */
    public GridDataSet getGridData(String element) {
        String selectedTime = fetchTargetTime(element);
        if (selectedTime == null) {
            return null;
        }
        return getGridData(element, selectedTime);
    }

    /**
     * Get grid data by explicit time.
     *
     * @param element element code
     * @param time time in yyyyMMddHHmmss
     */
    public GridDataSet getGridData(String element, String time) {
        String cacheKey = element + "_" + time;

        if (dataCache.containsKey(cacheKey)) {
            log.debug("从缓存获取网格数据: {}", cacheKey);
            return (GridDataSet) dataCache.get(cacheKey);
        }

        try {
            String url = String.format("%s?elements=%s&times=%s&apikey=%s",
                    baseUrl, element, time, apiKey);

            GridDataResponse response = restTemplate.getForObject(url, GridDataResponse.class);

            if (response == null || response.getReturnCode() != 0) {
                log.error("网格数据接口异常: element={}, code={}", element, response == null ? "null" : response.getReturnCode());
                return null;
            }

            if (response.getDs() == null || response.getDs().isEmpty()) {
                log.error("网格数据为空: element={}, time={}", element, time);
                return null;
            }

            GridDataSet gridData = response.getDs().get(0);
            dataCache.put(cacheKey, gridData);
            logGridMetaOnce(element, gridData);
            log.info("网格数据OK: element={}, time={}, points={}",
                    element, time,
                    gridData.getDataArray() == null ? 0 : gridData.getDataArray().size());
            return gridData;
        } catch (Exception e) {
            log.error("获取网格数据失败: element={}", element, e);
            return null;
        }
    }

    /**
     * 获取多个时间点的网格数据。
     * @param element 要素编码
     * @param count   需要的时间点数量
     * @param forward true=未来时间序列（预报用），false=过去时间序列（实况用）
     * @return 多个时次对应的网格数据列表（按时间升序）
     */
    public List<GridDataSet> getGridDataSeries(String element, int count, boolean forward) {
        List<String> times = fetchTimeSeries(element, count, forward);
        if (times == null || times.isEmpty()) {
            log.error("未获取到时间序列: element={}, count={}, forward={}", element, count, forward);
            return null;
        }
        return getGridDataForTimes(element, times);
    }

    /**
     * 获取要素的时间序列（多个时次），失败后等1秒重试一次。
     */
    private List<String> fetchTimeSeries(String element, int count, boolean forward) {
        String cacheKey = element + "_" + count + "_" + forward;
        if (timeSeriesCache.containsKey(cacheKey)) {
            log.debug("从缓存获取时间序列: {}", cacheKey);
            return timeSeriesCache.get(cacheKey);
        }

        List<String> result = doFetchTimeSeries(element, count, forward);
        if (result != null) {
            timeSeriesCache.put(cacheKey, result);
            return result;
        }

        // 第一次失败，等1秒重试
        log.warn("时间序列首次失败, 1秒后重试: element={}, count={}, forward={}", element, count, forward);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        result = doFetchTimeSeries(element, count, forward);
        if (result != null) {
            log.info("时间序列重试成功: element={}, count={}", element, result.size());
            timeSeriesCache.put(cacheKey, result);
            return result;
        }

        log.error("时间序列重试仍失败: element={}, count={}, forward={}", element, count, forward);
        return null;
    }

    /** 执行一次时间序列请求，失败返回 null */
    private List<String> doFetchTimeSeries(String element, int count, boolean forward) {

        try {
            String url = String.format("%s?element=%s", timeListUrl, element);
            TimeListResponse response = restTemplate.getForObject(url, TimeListResponse.class);

            if (response == null || response.getReturnCode() == null || response.getReturnCode() != 0) {
                log.error("时间列表接口异常: element={}, response={}", element, response);
                return null;
            }

            List<String> ds = response.getDs();
            if (ds == null || ds.isEmpty()) {
                log.error("时间列表为空: element={}", element);
                return null;
            }

            List<LocalDateTime> candidates = ds.stream()
                    .map(t -> LocalDateTime.parse(t, INPUT_FORMATTER))
                    .sorted()
                    .collect(Collectors.toList());

            LocalDateTime now = LocalDateTime.now();

            List<LocalDateTime> selected;
            if (forward) {
                // 预报：取未来最近的时间序列，从 now 之后开始
                selected = candidates.stream()
                        .filter(t -> t.isAfter(now))
                        .limit(count)
                        .collect(Collectors.toList());
            } else {
                // 实况：取不晚于 now 的最近 count 个
                List<LocalDateTime> pastCandidates = candidates.stream()
                        .filter(t -> !t.isAfter(now))
                        .collect(Collectors.toList());
                int size = pastCandidates.size();
                int startIdx = Math.max(0, size - count);
                selected = pastCandidates.subList(startIdx, size);
            }

            if (selected.isEmpty()) {
                log.error("未找到合适时次: element={}, count={}, forward={}", element, count, forward);
                return null;
            }

            List<String> result = selected.stream()
                    .map(t -> t.format(OUTPUT_FORMATTER))
                    .collect(Collectors.toList());

            log.info("时间序列: element={}, {}×{}", element, forward ? "预报" : "实况", result.size());
            return result;
        } catch (Exception e) {
            log.error("获取时间序列失败: element={}, count={}, forward={}", element, count, forward, e);
            return null;
        }
    }

    /**
     * 批量获取多个时次的网格数据（合并为一次请求），失败后等1秒重试一次。
     */
    private List<GridDataSet> getGridDataForTimes(String element, List<String> times) {
        if (times == null || times.isEmpty()) {
            return null;
        }

        String timesParam = String.join(",", times);
        String cacheKey = element + "_BATCH_" + timesParam.hashCode();

        if (dataCache.containsKey(cacheKey)) {
            log.debug("从缓存获取批量网格数据: {}", cacheKey);
            @SuppressWarnings("unchecked")
            List<GridDataSet> cached = (List<GridDataSet>) dataCache.get(cacheKey);
            return cached;
        }

        List<GridDataSet> result = doFetchGridDataForTimes(element, timesParam);
        if (result != null) {
            dataCache.put(cacheKey, result);
            return result;
        }

        // 第一次失败，等1秒重试
        log.warn("批量网格数据首次失败, 1秒后重试: element={}, times={}", element, times.size());
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        result = doFetchGridDataForTimes(element, timesParam);
        if (result != null) {
            log.info("批量网格数据重试成功: element={}, points={}", element, result.size());
            dataCache.put(cacheKey, result);
            return result;
        }

        // 重试也失败，打印响应详情
        log.error("批量网格数据重试仍失败: element={}, times={}", element, times);
        return null;
    }

    /** 执行一次批量网格数据请求，失败返回 null */
    private List<GridDataSet> doFetchGridDataForTimes(String element, String timesParam) {
        try {
            String url = String.format("%s?elements=%s&times=%s&apikey=%s",
                    baseUrl, element, timesParam, apiKey);

            GridDataResponse response = restTemplate.getForObject(url, GridDataResponse.class);

            if (response == null || response.getReturnCode() != 0) {
                log.error("批量网格数据接口异常: element={}, code={}, msg={}",
                        element,
                        response == null ? "null" : response.getReturnCode(),
                        response == null ? "null" : response.getReturnMessage());
                return null;
            }

            if (response.getDs() == null || response.getDs().isEmpty()) {
                log.warn("批量网格数据返回空: element={}, timesParam={}", element, timesParam);
                return null;
            }

            List<GridDataSet> dataList = response.getDs();
            if (!dataList.isEmpty()) {
                logGridMetaOnce(element, dataList.get(0));
            }
            log.info("批量网格数据OK: element={}, times={}, points={}",
                    element, timesParam.split(",").length, dataList.size());
            return dataList;
        } catch (Exception e) {
            log.error("获取批量网格数据失败: element={}", element, e);
            return null;
        }
    }

    /**
     * Clear caches at the start of each warning update cycle.
     */
    public void clearCache() {
        dataCache.clear();
        selectedTimeCache.clear();
        timeSeriesCache.clear();
        loggedGridElements.clear();
    }

    /**
     * 每轮预警周期中，每个要素首次获取网格数据时打印网格元数据。
     * 用于快速对比不同要素的网格系统（范围、间距、分辨率等）是否一致。
     */
    private void logGridMetaOnce(String element, GridDataSet grid) {
        if (loggedGridElements.contains(element)) {
            return;
        }
        loggedGridElements.add(element);
        log.info("网格元数据首次记录: element={} | lon=[{},{}] lat=[{},{}] | 间距lon={} lat={} | 网格{}×{} | noData={} | reftime={} updatetime={}",
                element,
                grid.getMinLon(), grid.getMaxLon(),
                grid.getMinLat(), grid.getMaxLat(),
                grid.getLonGridSpace(), grid.getLatGridSpace(),
                grid.getLonGridNumber(), grid.getLatGridNumber(),
                grid.getNoDataValue(),
                grid.getReftime(), grid.getUpdatetime());
    }
}
