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
public class WindWarningService {

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
        wrapper.eq("warning_type", "WIND");
        List<WarningThreshold> list = thresholdMapper.selectList(wrapper);

        thresholds.clear();
        for (WarningThreshold threshold : list) {
            thresholds.put(threshold.getLevelName(), threshold.getThresholdValue());
        }

        log.debug("加载大风预警阈值: {}", thresholds);
    }

    public WarningInfo checkWarning(GeoLineNode node) {
        try {
            // 获取实况风速（过去1小时）
            GridDataSet actualGrid = gridDataService.getGridData("HOR-WIN");
            Double actualValue = null;
            if (actualGrid != null) {
                actualValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), actualGrid);
            }

            // 获取预报风速（未来3h）
            GridDataSet forecastGrid = gridDataService.getGridData("EDA10");
            Double forecastValue = null;
            if (forecastGrid != null) {
                forecastValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), forecastGrid);
            }

            // 判断预警等级
            String level = determineLevel(actualValue, forecastValue);

            if (level != null) {
                WarningInfo warning = new WarningInfo();
                warning.setStationCode(node.getName());
                warning.setStationName(node.getRoadName());
                warning.setLon(node.getLon());
                warning.setLat(node.getLat());
                warning.setWarningType("WIND");
                warning.setWarningLevel(level);
                warning.setActualValue(actualValue);
                warning.setForecastValue(forecastValue);
                warning.setForecastTime(new Date());
                warning.setForecastPeriod(3);

                return warning;
            }

        } catch (Exception e) {
            log.error("检查大风预警失败: node={}", node.getName(), e);
        }

        return null;
    }

    private String determineLevel(Double actualValue, Double forecastValue) {
        Double redThreshold = thresholds.get("RED");
        Double orangeThreshold = thresholds.get("ORANGE");
        Double yellowThreshold = thresholds.get("YELLOW");

        if (exceedsThreshold(actualValue, redThreshold) || exceedsThreshold(forecastValue, redThreshold)) {
            return "RED";
        } else if (exceedsThreshold(actualValue, orangeThreshold) || exceedsThreshold(forecastValue, orangeThreshold)) {
            return "ORANGE";
        } else if (exceedsThreshold(actualValue, yellowThreshold) || exceedsThreshold(forecastValue, yellowThreshold)) {
            return "YELLOW";
        }

        return null;
    }

    private boolean exceedsThreshold(Double value, Double threshold) {
        return value != null && threshold != null && value > threshold;
    }
}
