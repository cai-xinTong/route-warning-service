package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

@Slf4j
@Service
public class VisibilityWarningService {

    @Resource
    private GridDataService gridDataService;

    @Resource
    private WarningThresholdMapper thresholdMapper;

    @Resource
    private WeatherServiceDetailThresholdMapper detailThresholdMapper;

    // 默认阈值（硬编码兜底，能见度越低越危险，单位 m）
    private static final double DEFAULT_YELLOW = 1000.0;
    private static final double DEFAULT_ORANGE = 500.0;
    private static final double DEFAULT_RED = 200.0;

    /**
     * 加载阈值：优先查 weather_service_detail_threshold（站点精准阈值），
     * 查不到则回退 warning_threshold（全局阈值），再查不到用硬编码默认值。
     */
    private Map<String, Double> loadThresholds(Long staId) {
        Map<String, Double> thresholds = new HashMap<>();

        // 1. 尝试从站点精准阈值表加载
        List<WeatherServiceDetailThreshold> detailList =
                detailThresholdMapper.selectByStaIdAndElement(staId, "VISIBILITY");
        if (detailList != null && !detailList.isEmpty()) {
            for (WeatherServiceDetailThreshold t : detailList) {
                thresholds.put(t.getLevel(), t.getValue());
            }
            fillDefaults(thresholds);
            log.info("能见度阈值(站点): staId={}, {}", staId, thresholds);
            return thresholds;
        }

        // 2. 回退全局阈值表
        QueryWrapper<WarningThreshold> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_type", "VISIBILITY");
        List<WarningThreshold> list = thresholdMapper.selectList(wrapper);
        if (list != null && !list.isEmpty()) {
            for (WarningThreshold t : list) {
                thresholds.put(t.getLevelName(), t.getThresholdValue());
            }
            fillDefaults(thresholds);
            log.info("能见度阈值(全局表): {}", thresholds);
            return thresholds;
        }

        // 3. 最终回退硬编码默认值
        thresholds.put("YELLOW", DEFAULT_YELLOW);
        thresholds.put("ORANGE", DEFAULT_ORANGE);
        thresholds.put("RED", DEFAULT_RED);
        log.info("能见度阈值(默认): {}", thresholds);
        return thresholds;
    }

    private void fillDefaults(Map<String, Double> thresholds) {
        thresholds.putIfAbsent("YELLOW", DEFAULT_YELLOW);
        thresholds.putIfAbsent("ORANGE", DEFAULT_ORANGE);
        thresholds.putIfAbsent("RED", DEFAULT_RED);
    }

    public WarningCheckResult checkWarning(GeoLineNode node) {
        Map<String, Double> thresholds = loadThresholds(node.getStaId());
        Date now = new Date();
        try {
            // 获取实况能见度（接口返回km，×1000转为m）
            GridDataSet actualGrid = gridDataService.getGridData("HOR-VIS");
            Double actualValue = null;
            if (actualGrid != null) {
                actualValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), actualGrid);
                if (actualValue != null) {
                    actualValue = actualValue * 1000;
                }
            }

            // 获取预报能见度（接口返回km，×1000转为m）
            GridDataSet forecastGrid = gridDataService.getGridData("VIS");
            Double forecastValue = null;
            if (forecastGrid != null) {
                forecastValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), forecastGrid);
                if (forecastValue != null) {
                    forecastValue = forecastValue * 1000;
                }
            }

            String level = determineLevel(actualValue, forecastValue, thresholds);
            if (level != null) {
                WarningInfo warning = new WarningInfo();
                warning.setStaId(node.getStaId());
                warning.setGeoId(node.getGeoId());
                warning.setStationCode(node.getName());
                warning.setStationName(node.getRoadName());
                warning.setLon(node.getLon());
                warning.setLat(node.getLat());
                warning.setWarningType("VISIBILITY");
                warning.setWarningLevel(level);
                warning.setActualValue(actualValue);
                warning.setForecastValue(forecastValue);
                warning.setForecastTime(now);
                warning.setForecastPeriod(3);
                return new WarningCheckResult(warning, actualValue, forecastValue, now, 3, null);
            }
            // 无预警也返回实测/预报值
            return new WarningCheckResult(null, actualValue, forecastValue, now, 3, null);

        } catch (Exception e) {
            log.error("检查能见度预警失败: node={}", node.getName(), e);
        }
        return new WarningCheckResult(null, null, null, null, null, null);
    }

    private String determineLevel(Double actualValue, Double forecastValue, Map<String, Double> thresholds) {
        // 取实况和预报的最小值
        Double minValue = null;
        if (actualValue != null) minValue = actualValue;
        if (forecastValue != null) minValue = (minValue == null) ? forecastValue : Math.min(minValue, forecastValue);

        if (minValue == null) return null;

        Double red = thresholds.get("RED");
        Double orange = thresholds.get("ORANGE");
        Double yellow = thresholds.get("YELLOW");

        // 能见度低于阈值触发
        if (below(minValue, red)) return "RED";
        if (below(minValue, orange)) return "ORANGE";
        if (below(minValue, yellow)) return "YELLOW";
        return null;
    }

    private boolean below(Double value, Double threshold) {
        return value != null && threshold != null && value < threshold;
    }
}
