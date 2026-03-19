package com.warning.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 预警统计信息
 */
@Data
public class WarningStatisticsDTO {

    /**
     * 总预警数
     */
    private Long totalCount;

    /**
     * 按类型统计
     */
    private Map<String, Long> countByType = new HashMap<>();

    /**
     * 按级别统计
     */
    private Map<String, Long> countByLevel = new HashMap<>();

    /**
     * 按类型和级别统计
     */
    private Map<String, Map<String, Long>> countByTypeAndLevel = new HashMap<>();

    /**
     * 最新更新时间
     */
    private String lastUpdateTime;
}
