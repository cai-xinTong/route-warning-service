package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warning.dto.GridDataSet;
import com.warning.entity.GeoLineNode;
import com.warning.entity.WarningInfo;
import com.warning.entity.WarningThreshold;
import com.warning.mapper.WarningThresholdMapper;
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

    private Map<String, Double> loadThresholds() {
        QueryWrapper<WarningThreshold> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_type", "VISIBILITY");
        List<WarningThreshold> list = thresholdMapper.selectList(wrapper);
        Map<String, Double> thresholds = new HashMap<>();
        for (WarningThreshold t : list) {
            thresholds.put(t.getLevelName(), t.getThresholdValue());
        }
        log.debug("能见度阈值: {}", thresholds);
        return thresholds;
    }

    public WarningInfo checkWarning(GeoLineNode node) {
        Map<String, Double> thresholds = loadThresholds();
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
                warning.setForecastTime(new Date());
                warning.setForecastPeriod(3);
                return warning;
            }

        } catch (Exception e) {
            log.error("检查能见度预警失败: node={}", node.getName(), e);
        }
        return null;
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
