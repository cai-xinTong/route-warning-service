package com.warning.service;

import com.warning.dto.GridDataResponse;
import com.warning.dto.GridDataSet;
import com.warning.dto.TimeListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GridDataService {

    @Resource
    private RestTemplate restTemplate;

    @Value("${grid.api.base-url}")
    private String baseUrl;

    @Value("${grid.api.time-list-url}")
    private String timeListUrl;

    @Value("${grid.api.api-key}")
    private String apiKey;

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 缓存网格数据，避免重复请求
    private final Map<String, GridDataSet> dataCache = new HashMap<>();

    // 缓存各 element 的最新时间，同一任务周期内只查一次
    private final Map<String, String> latestTimeCache = new HashMap<>();

    /**
     * 查询指定 element 的数据最新时间
     */
    private String fetchLatestTime(String element) {
        if (latestTimeCache.containsKey(element)) {
            return latestTimeCache.get(element);
        }

        try {
            String url = String.format("%s?element=%s", timeListUrl, element);
            log.info("查询数据最新时间: element={}, url={}", element, url);

            TimeListResponse response = restTemplate.getForObject(url, TimeListResponse.class);

            if (response == null || response.getReturnCode() == null || response.getReturnCode() != 0) {
                log.error("时间列表接口返回异常: element={}, response={}", element, response);
                return null;
            }

            List<String> ds = response.getDs();
            if (ds == null || ds.isEmpty()) {
                log.warn("时间列表为空: element={}", element);
                return null;
            }

            // ds[0] 为最新时间，格式 "yyyy-MM-dd HH:mm:ss"，转为 "yyyyMMddHHmmss"
            String latestTimeStr = ds.get(0);
            LocalDateTime latestTime = LocalDateTime.parse(latestTimeStr, INPUT_FORMATTER);
            String formattedTime = latestTime.format(OUTPUT_FORMATTER);

            log.info("获取到数据最新时间: element={}, time={}", element, formattedTime);
            latestTimeCache.put(element, formattedTime);
            return formattedTime;

        } catch (Exception e) {
            log.error("查询数据最新时间失败: element={}", element, e);
            return null;
        }
    }

    /**
     * 获取网格数据（自动查询最新时间）
     * @param element 要素类型：HOR-PRE(降水实况), ER01(1h降水预报), ER03(3h降水预报),
     *                HOR-WIN(风速实况), EDA10(风速预报), HOR-VIS(能见度实况), VIS(能见度预报)
     */
    public GridDataSet getGridData(String element) {
        String latestTime = fetchLatestTime(element);
        if (latestTime == null) {
            log.warn("无法获取最新时间，跳过网格数据请求: element={}", element);
            return null;
        }
        return getGridData(element, latestTime);
    }

    /**
     * 获取网格数据（指定时间）
     * @param element 要素类型
     * @param time    时间，格式：yyyyMMddHHmmss
     */
    public GridDataSet getGridData(String element, String time) {
        String cacheKey = element + "_" + time;

        if (dataCache.containsKey(cacheKey)) {
            log.debug("从缓存获取网格数据: {}", cacheKey);
            return dataCache.get(cacheKey);
        }

        try {
            String url = String.format("%s?elements=%s&times=%s&apikey=%s",
                    baseUrl, element, time, apiKey);

            log.info("请求网格数据接口: element={}, time={}", element, time);

            GridDataResponse response = restTemplate.getForObject(url, GridDataResponse.class);

            if (response == null || response.getReturnCode() != 0) {
                log.error("网格数据接口返回异常: {}", response);
                return null;
            }

            if (response.getDs() == null || response.getDs().isEmpty()) {
                log.warn("网格数据为空: element={}, time={}", element, time);
                return null;
            }

            GridDataSet gridData = response.getDs().get(0);
            dataCache.put(cacheKey, gridData);
            log.info("成功获取网格数据: element={}, 网格数={}", element, gridData.getDataArray().size());

            return gridData;

        } catch (Exception e) {
            log.error("获取网格数据失败: element={}, time={}", element, time, e);
            return null;
        }
    }

    /**
     * 清空缓存（每次任务开始时调用）
     */
    public void clearCache() {
        dataCache.clear();
        latestTimeCache.clear();
        log.info("网格数据缓存已清空");
    }
}
