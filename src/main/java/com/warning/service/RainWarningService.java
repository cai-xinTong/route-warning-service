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
public class RainWarningService {

    @Resource
    private GridDataService gridDataService;

    @Resource
    private WarningThresholdMapper thresholdMapper;

    // 阈值缓存
    private final Map<String, Double> thresholds = new HashMap<>();

    @PostConstruct
    public void init() {
        loadThresholds();
    }

    /**
     * 从数据库加载阈值
     */
    public void loadThresholds() {
        QueryWrapper<WarningThreshold> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_type", "RAIN");
        List<WarningThreshold> list = thresholdMapper.selectList(wrapper);

        thresholds.clear();
        for (WarningThreshold threshold : list) {
            thresholds.put(threshold.getLevelName(), threshold.getThresholdValue());
        }

        log.info("加载暴雨预警阈值: {}", thresholds);
    }

    /**
     * 检查暴雨预警
     */
    public WarningInfo checkWarning(GeoLineNode node) {
        try {
            // 获取实况降水量（过去60分钟）
            GridDataSet actualGrid = gridDataService.getGridData("HOR-PRE");
            Double actualValue = null;
            if (actualGrid != null) {
                actualValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), actualGrid);
            }

            // 获取预报降水量（未来1h）
            GridDataSet forecastGrid = gridDataService.getGridData("ER01");
            Double forecastValue = null;
            if (forecastGrid != null) {
                forecastValue = GridCalculator.getGridValueByLonLat(node.getLon(), node.getLat(), forecastGrid);
            }

            // 判断预警等级（从高到低判断）
            String level = determineLevel(actualValue, forecastValue);

            if (level != null) {
                WarningInfo warning = new WarningInfo();
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

    /**
     * 判断预警等级
     */
    private String determineLevel(Double actualValue, Double forecastValue) {
        Double redThreshold = thresholds.get("RED");
        Double orangeThreshold = thresholds.get("ORANGE");
        Double yellowThreshold = thresholds.get("YELLOW");

        // 从高到低判断
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
