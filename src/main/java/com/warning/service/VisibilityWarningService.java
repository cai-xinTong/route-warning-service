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

import javax.annotation.PostConstruct;
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

    private final Map<String, Double> thresholds = new HashMap<>();

    @PostConstruct
    public void init() {
        loadThresholds();
    }

    public void loadThresholds() {
        QueryWrapper<WarningThreshold> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_type", "VISIBILITY");
        List<WarningThreshold> list = thresholdMapper.selectList(wrapper);

        thresholds.clear();
        for (WarningThreshold threshold : list) {
            thresholds.put(threshold.getLevelName(), threshold.getThresholdValue());
        }

        log.debug("加载能见度预警阈值: {}", thresholds);
    }

    public WarningInfo checkWarning(GeoLineNode node) {
        try {
            // 获取实况能见度（过去1小时）
            GridDataSet actualGrid = gridDataService.getGridData("HOR-VIS");
            Double actualValue = null;
            if (actualGrid != null) {
                actualValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), actualGrid);
                // 接口返回单位是km，转换为m
                if (actualValue != null) {
                    actualValue = actualValue * 1000;
                }
            }

            // 获取预报能见度（未来3h）
            GridDataSet forecastGrid = gridDataService.getGridData("VIS");
            Double forecastValue = null;
            if (forecastGrid != null) {
                forecastValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), forecastGrid);
                // 接口返回单位是km，转换为m
                if (forecastValue != null) {
                    forecastValue = forecastValue * 1000;
                }
            }

            // 判断预警等级（取实况和预报的最小值）
            String level = determineLevel(actualValue, forecastValue);

            if (level != null) {
                WarningInfo warning = new WarningInfo();
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

    private String determineLevel(Double actualValue, Double forecastValue) {
        // 取实况和预报的最小值
        Double minValue = Double.MAX_VALUE;
        if (actualValue != null) {
            minValue = Math.min(minValue, actualValue);
        }
        if (forecastValue != null) {
            minValue = Math.min(minValue, forecastValue);
        }

        if (minValue == Double.MAX_VALUE) {
            return null;
        }

        Double redThreshold = thresholds.get("RED");
        Double orangeThreshold = thresholds.get("ORANGE");
        Double yellowThreshold = thresholds.get("YELLOW");

        // 能见度是小于阈值触发预警
        if (belowThreshold(minValue, redThreshold)) {
            return "RED";
        } else if (belowThreshold(minValue, orangeThreshold)) {
            return "ORANGE";
        } else if (belowThreshold(minValue, yellowThreshold)) {
            return "YELLOW";
        }

        return null;
    }

    private boolean belowThreshold(Double value, Double threshold) {
        return value != null && threshold != null && value < threshold;
    }
}
