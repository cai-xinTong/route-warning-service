package com.warning.service;

import com.warning.dto.GridDataSet;
import com.warning.dto.WarningCheckResult;
import com.warning.entity.GeoLineNode;
import com.warning.entity.WarningInfo;
import com.warning.entity.WarningThreshold;
import com.warning.entity.WeatherServiceDetailThreshold;
import com.warning.mapper.WarningThresholdMapper;
import com.warning.mapper.WeatherServiceDetailThresholdMapper;
import com.warning.util.GridCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 降雨预警服务（新版累计降水逻辑）
 * <p>
 * 数据源：
 * - 实况：10MIN-PRE（逐10分钟降水）
 * - 预报：ER06M（逐6分钟降水）
 * <p>
 * 触发规则（两规则独立判断，取较高等级）：
 * - 规则1（实况+预报）：实况1h>20mm 且 预报2h>30mm 时，实况1h+预报2h累计 超阈值触发
 * - 规则2（纯预报）：未来3h预报累计 直接超阈值触发，无前置条件
 * 阈值 >50(Y) >70(O) >90(R)
 * <p>
 * 解除规则：连续2个30分钟判断未来2h累计 < 5mm
 */
@Slf4j
@Service
public class RainWarningService {

    /**
     * 过去1h实况时间点数（10MIN-PRE，每10分钟一个）
     */
    private static final int ACTUAL_COUNT_1H = 6;

    /**
     * 未来2h预报时间点数（ER06M，每6分钟一个）
     */
    private static final int FORECAST_COUNT_2H = 20;

    /**
     * 未来3h预报时间点数（ER06M，每6分钟一个）
     */
    private static final int FORECAST_COUNT_3H = 30;

    /**
     * 预警等级默认阈值（全局回退用）
     */
    private static final double DEFAULT_YELLOW = 50.0;
    private static final double DEFAULT_ORANGE = 70.0;
    private static final double DEFAULT_RED = 90.0;

    /**
     * 解除阈值：未来2h累计 < 此值(mm)
     */
    private static final double RELEASE_THRESHOLD = 5.0;

    @Resource
    private GridDataService gridDataService;

    @Resource
    private WeatherServiceDetailThresholdMapper detailThresholdMapper;

    @Resource
    private WarningThresholdMapper thresholdMapper;

    /**
     * 加载降雨预警等级阈值。
     * 优先查 weather_service_detail_threshold（站点精准阈值），
     * 查不到则回退 warning_threshold（全局阈值），再查不到用硬编码默认值。
     */
    private Map<String, Double> loadRainThresholds(Long staId) {
        Map<String, Double> thresholds = new HashMap<>();

        // 1. 尝试从站点精准阈值表加载
        List<WeatherServiceDetailThreshold> detailList =
                detailThresholdMapper.selectByStaIdAndElement(staId, "RAIN");
        if (detailList != null && !detailList.isEmpty()) {
            for (WeatherServiceDetailThreshold t : detailList) {
                thresholds.put(t.getLevel(), t.getValue());
            }
            log.debug("降雨阈值(站点): staId={}, {}", staId, thresholds);
            // 补齐缺失级别（如果只配了部分级别）
            fillDefaults(thresholds);
            return thresholds;
        }

        // 2. 回退全局阈值表
        List<WarningThreshold> globalList = thresholdMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WarningThreshold>()
                        .eq("warning_type", "RAIN"));
        if (globalList != null && !globalList.isEmpty()) {
            for (WarningThreshold t : globalList) {
                thresholds.put(t.getLevelName(), t.getThresholdValue());
            }
            log.debug("降雨阈值(全局表): {}", thresholds);
            fillDefaults(thresholds);
            return thresholds;
        }

        // 3. 最终回退硬编码默认值
        thresholds.put("YELLOW", DEFAULT_YELLOW);
        thresholds.put("ORANGE", DEFAULT_ORANGE);
        thresholds.put("RED", DEFAULT_RED);
        log.debug("降雨阈值(默认): {}", thresholds);
        return thresholds;
    }

    /**
     * 补齐缺失的级别为默认值（保证 YELLOW/ORANGE/RED 三者齐全）。
     */
    private void fillDefaults(Map<String, Double> thresholds) {
        thresholds.putIfAbsent("YELLOW", DEFAULT_YELLOW);
        thresholds.putIfAbsent("ORANGE", DEFAULT_ORANGE);
        thresholds.putIfAbsent("RED", DEFAULT_RED);
    }

    /**
     * 检查降雨预警（为单个站点计算）。
     *
     * @param node 路段节点
     * @return 计算结果，包含预警信息（可能为 null）和实测/预报值
     */
    public WarningCheckResult checkWarning(GeoLineNode node) {
        // 加载该站点的降雨阈值
        Map<String, Double> thresholds = loadRainThresholds(node.getStaId());
        String label = nodeLabel(node);
        Date now = new Date();
        try {
            // 1. 获取过去1h实况累计（10MIN-PRE，取最近6个时次）
            Double actual1hSum = getCumulativeValue(node, "10MIN-PRE", ACTUAL_COUNT_1H, false);

            // 2. 获取未来2h预报累计（ER06M，取未来20个时次）
            Double forecast2hSum = getCumulativeValue(node, "ER06M", FORECAST_COUNT_2H, true);

            // 3. 获取未来3h预报累计（ER06M，取未来30个时次）
            Double forecast3hSum = getCumulativeValue(node, "ER06M", FORECAST_COUNT_3H, true);

            // 始终打印本节点降水数据（方便排查）
            log.debug("降雨数据: {} | 实况1h={}mm 预报2h={}mm 预报3h={}mm",
                    label,
                    actual1hSum == null ? "无" : String.format("%.1f", actual1hSum),
                    forecast2hSum == null ? "无" : String.format("%.1f", forecast2hSum),
                    forecast3hSum == null ? "无" : String.format("%.1f", forecast3hSum));

            // 4. 计算等级
            // 规则1（实况+预报）：需满足 实况1h>20 且 预报2h>30，再判断累计是否超阈值
            String level1 = null;
            if (actual1hSum != null && forecast2hSum != null
                    && actual1hSum > 20.0 && forecast2hSum > 30.0) {
                double sum1 = actual1hSum + forecast2hSum;
                level1 = determineLevel(sum1, thresholds);
            }

            // 规则2（纯预报）：未来3h预报累计直接与阈值比较，无前置条件
            String level2 = forecast3hSum != null ? determineLevel(forecast3hSum, thresholds) : null;

            // 取两规则中较高级别
            String finalLevel = maxLevel(level1, level2);
            if (finalLevel == null) {
                return new WarningCheckResult(null, actual1hSum, forecast2hSum, now, 3, forecast3hSum);
            }

            // INFO 级别打印触发详情（方便排查规则1 vs 规则2）
            boolean fromRule1 = finalLevel.equals(level1);
            String ruleTag = fromRule1 ? "规则1(实况+预报)" : "规则2(纯预报)";
            double cumulativeForLog = fromRule1 ? (actual1hSum + forecast2hSum) : forecast3hSum;
            log.info("降雨预警触发: {} | {} level={} | 实况1h={}mm 预报2h={}mm 预报3h={}mm | 累计={}mm 阈值={{RED={},ORANGE={},YELLOW={}}}",
                    label, ruleTag, finalLevel,
                    actual1hSum == null ? "?" : String.format("%.1f", actual1hSum),
                    forecast2hSum == null ? "?" : String.format("%.1f", forecast2hSum),
                    forecast3hSum == null ? "?" : String.format("%.1f", forecast3hSum),
                    String.format("%.1f", cumulativeForLog),
                    thresholds.get("RED"), thresholds.get("ORANGE"), thresholds.get("YELLOW"));

            WarningInfo warning = new WarningInfo();
            warning.setStaId(node.getStaId());
            warning.setGeoId(node.getGeoId());
            warning.setStationCode(node.getName());
            warning.setStationName(node.getRoadName());
            warning.setLon(node.getLon());
            warning.setLat(node.getLat());
            warning.setWarningType("RAIN");
            warning.setWarningLevel(finalLevel);
            // actualValue 存储过去1h实况累计
            warning.setActualValue(actual1hSum);
            // forecastValue 存储未来2h预报累计（规则1用）
            warning.setForecastValue(forecast2hSum);
            // forecast3hValue 存储未来3h预报累计（规则2用）
            warning.setForecast3hValue(forecast3hSum);
            warning.setForecastTime(now);
            warning.setForecastPeriod(3);
            return new WarningCheckResult(warning, actual1hSum, forecast2hSum, now, 3, forecast3hSum);

        } catch (Exception e) {
            log.error("检查暴雨预警失败: {}", nodeLabel(node), e);
        }
        return new WarningCheckResult(null, null, null, null, null, null);
    }

    /**
     * 检查该站点是否满足预警解除条件。
     * 未来2h预报累计 < 解除阈值。
     *
     * @param node 路段节点
     * @return true=满足解除条件（累计 < 阈值），false=不满足
     */
    public boolean checkReleaseCondition(GeoLineNode node) {
        String label = nodeLabel(node);
        try {
            Double forecast2hSum = getCumulativeValue(node, "ER06M", FORECAST_COUNT_2H, true);
            if (forecast2hSum == null) {
                return false;
            }
            boolean releasable = forecast2hSum < RELEASE_THRESHOLD;
            if (releasable) {
                log.debug("降雨解除条件满足: {}, forecast2hSum={}mm < {}mm",
                        label, forecast2hSum, RELEASE_THRESHOLD);
            }
            return releasable;
        } catch (Exception e) {
            log.error("检查降雨预警解除失败: {}", label, e);
            return false;
        }
    }

    /**
     * 获取指定站点在多个时次网格数据的累计值。
     */
    private Double getCumulativeValue(GeoLineNode node, String element, int count, boolean forward) {
        String label = nodeLabel(node);
        String direction = forward ? "预报" : "实况";

        List<GridDataSet> dataList = gridDataService.getGridDataSeries(element, count, forward);
        if (dataList == null || dataList.isEmpty()) {
            log.debug("{}[{}] 网格接口无数据, 跳过", direction, element);
            return null;
        }

        double sum = 0;
        int validCount = 0;
        for (int i = 0; i < dataList.size(); i++) {
            GridDataSet grid = dataList.get(i);
            Double value = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), grid);
            if (value != null) {
                sum += value;
                validCount++;
            }
        }

        if (validCount == 0) {
            GridDataSet first = dataList.get(0);
            int idx = GridCalculator.calculateGridIndex(node.getLon(), node.getLat(), first);
            log.debug("{}[{}] 站点({}) lon={} lat={} col={} row={} 无有效值 | 网格[lon={}-{} lat={}-{} 格距{}/{}, 行列{}/{}, noData={}]",
                    direction, element, label, node.getLon(), node.getLat(),
                    idx == -1 ? "?" : String.valueOf(idx % first.getLonGridNumber()),
                    idx == -1 ? "?" : String.valueOf(idx / first.getLonGridNumber()),
                    first.getMinLon(), first.getMaxLon(), first.getMinLat(), first.getMaxLat(),
                    first.getLonGridSpace(), first.getLatGridSpace(),
                    first.getLonGridNumber(), first.getLatGridNumber(),
                    first.getNoDataValue());
            return null;
        }

        log.debug("{}[{}] 站点({}) 累计={}mm (有效{}/{}时次)",
                direction, element, label, sum, validCount, dataList.size());
        return sum;
    }

    /** 站点可读标识：staId + 路名 */
    private String nodeLabel(GeoLineNode node) {
        String road = node.getRoadName();
        return "staId=" + node.getStaId() + (road != null ? "," + road : "");
    }

    /**
     * 根据累计值和站点阈值判定预警等级。
     */
    private String determineLevel(double cumulativeValue, Map<String, Double> thresholds) {
        Double red = thresholds.get("RED");
        Double orange = thresholds.get("ORANGE");
        Double yellow = thresholds.get("YELLOW");
        if (red != null && cumulativeValue > red) return "RED";
        if (orange != null && cumulativeValue > orange) return "ORANGE";
        if (yellow != null && cumulativeValue > yellow) return "YELLOW";
        return null;
    }

    /**
     * 取两个等级中较高者。RED > ORANGE > YELLOW
     */
    private String maxLevel(String level1, String level2) {
        if (level1 == null) return level2;
        if (level2 == null) return level1;
        return levelRank(level1) >= levelRank(level2) ? level1 : level2;
    }

    private int levelRank(String level) {
        if ("RED".equals(level)) return 3;
        if ("ORANGE".equals(level)) return 2;
        if ("YELLOW".equals(level)) return 1;
        return 0;
    }
}
