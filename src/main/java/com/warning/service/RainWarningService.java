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
public class RainWarningService {

    @Resource
    private GridDataService gridDataService;

    @Resource
    private WarningThresholdMapper thresholdMapper;

    private Map<String, Double> loadThresholds() {
        QueryWrapper<WarningThreshold> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_type", "RAIN");
        List<WarningThreshold> list = thresholdMapper.selectList(wrapper);
        Map<String, Double> thresholds = new HashMap<>();
        for (WarningThreshold t : list) {
            thresholds.put(t.getLevelName(), t.getThresholdValue());
        }
        log.debug("暴雨阈值: {}", thresholds);
        return thresholds;
    }

    public WarningInfo checkWarning(GeoLineNode node) {
        Map<String, Double> thresholds = loadThresholds();
        try {
            GridDataSet actualGrid = gridDataService.getGridData("HOR-PRE");
            Double actualValue = null;
            if (actualGrid != null) {
                actualValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), actualGrid);
            }

            GridDataSet forecastGrid = gridDataService.getGridData("ER01");
            Double forecastValue = null;
            if (forecastGrid != null) {
                forecastValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), forecastGrid);
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
                warning.setWarningType("RAIN");
                warning.setWarningLevel(level);
                warning.setActualValue(actualValue);
                warning.setForecastValue(forecastValue);
                warning.setForecastTime(new Date());
                warning.setForecastPeriod(1);
                return warning;
            }

        } catch (Exception e) {
            log.error("检查暴雨预警失败: node={}", node.getName(), e);
        }
        return null;
    }

    private String determineLevel(Double actualValue, Double forecastValue, Map<String, Double> thresholds) {
        Double red = thresholds.get("RED");
        Double orange = thresholds.get("ORANGE");
        Double yellow = thresholds.get("YELLOW");

        if (exceeds(actualValue, red) || exceeds(forecastValue, red)) return "RED";
        if (exceeds(actualValue, orange) || exceeds(forecastValue, orange)) return "ORANGE";
        if (exceeds(actualValue, yellow) || exceeds(forecastValue, yellow)) return "YELLOW";
        return null;
    }

    private boolean exceeds(Double value, Double threshold) {
        return value != null && threshold != null && value > threshold;
    }
}
