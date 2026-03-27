package com.warning.dto;

import lombok.Data;

import java.util.Date;

/**
 * 预警信息查询条件
 */
@Data
public class WarningQueryDTO {

    /**
     * 气象站ID（weather_service_geo_detail.staId）
     */
    private Long staId;

    /**
     * 预警类型：RAIN/WIND/VISIBILITY
     */
    private String warningType;

    /**
     * 预警级别：YELLOW/ORANGE/RED
     */
    private String warningLevel;

    /**
     * 站号/桩号（模糊查询）
     */
    private String stationCode;

    /**
     * 路段名称（模糊查询）
     */
    private String stationName;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}
