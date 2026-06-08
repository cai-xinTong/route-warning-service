package com.warning.dto;

import lombok.Data;

import java.util.Date;

/**
 * 预警查询统一返回 DTO
 * 查当前状态时：actualValue/forecastValue/forecastTime/forecastPeriod/staId/lon/lat 为 null
 * 查历史明细时：isReleased/noWarningCount 为 null
 */
@Data
public class WarningPlanQueryDTO {

    private Long id;

    private Long staId;

    private Long geoId;

    private String stationCode;

    private String stationName;

    private Double lon;

    private Double lat;

    private String warningType;

    /** 预警级别：YELLOW/ORANGE/RED，无预警时为 null */
    private String warningLevel;

    /** 预警状态：1 有预警，0 无预警 */
    private Integer status;

    /** 实况值（仅历史明细有值） */
    private Double actualValue;

    /** 预报值（仅历史明细有值） */
    private Double forecastValue;

    /** 未来3h预报累计值（仅历史明细有值，降雨规则2专用） */
    private Double forecast3hValue;

    /** 起报时间（仅历史明细有值） */
    private Date forecastTime;

    /** 预报时效，小时（仅历史明细有值） */
    private Integer forecastPeriod;

    /** 批次/计算时间 */
    private Date batchTime;

    /** 连续无预警次数（仅当前状态有值） */
    private Integer noWarningCount;

    /** 是否已解除：1 已解除，0 未解除（仅当前状态有值） */
    private Integer isReleased;
}
