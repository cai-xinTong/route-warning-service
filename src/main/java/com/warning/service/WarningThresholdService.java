package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.warning.entity.WarningThreshold;
import com.warning.mapper.WarningThresholdMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarningThresholdService {

    @Resource
    private WarningThresholdMapper warningThresholdMapper;

    /**
     * 查询阈值列表，支持按 warningType、levelName 过滤
     */
    public List<WarningThreshold> listThresholds(String warningType, String levelName) {
        LambdaQueryWrapper<WarningThreshold> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(warningType), WarningThreshold::getWarningType, warningType)
               .eq(StringUtils.hasText(levelName), WarningThreshold::getLevelName, levelName)
               .orderByAsc(WarningThreshold::getWarningType, WarningThreshold::getLevelName);
        return warningThresholdMapper.selectList(wrapper);
    }

    /**
     * 根据ID灵活更新阈值（只更新非null字段），自动刷新 updateTime
     */
    public boolean updateThreshold(WarningThreshold threshold) {
        if (threshold.getId() == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        LambdaUpdateWrapper<WarningThreshold> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WarningThreshold::getId, threshold.getId())
               .set(StringUtils.hasText(threshold.getWarningType()), WarningThreshold::getWarningType, threshold.getWarningType())
               .set(StringUtils.hasText(threshold.getLevelName()), WarningThreshold::getLevelName, threshold.getLevelName())
               .set(threshold.getThresholdValue() != null, WarningThreshold::getThresholdValue, threshold.getThresholdValue())
               .set(StringUtils.hasText(threshold.getDescription()), WarningThreshold::getDescription, threshold.getDescription())
               .set(WarningThreshold::getUpdateTime, LocalDateTime.now());
        return warningThresholdMapper.update(null, wrapper) > 0;
    }
}
