package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.warning.typehandler.XuguDateTypeHandler;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "warning_info", autoResultMap = true)
public class WarningInfo {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 所属预警方案ID */
    private Long planId;

    private Long staId;

    private Long geoId;

    private String stationCode;

    private String stationName;

    private Double lon;

    private Double lat;

    private String warningType;

    private String warningLevel;

    private Double actualValue;

    private Double forecastValue;

    /** 未来3h预报累计值（降雨规则2专用） */
    private Double forecast3hValue;

    @TableField(typeHandler = XuguDateTypeHandler.class)
    private Date forecastTime;

    private Integer forecastPeriod;

    @TableField(typeHandler = XuguDateTypeHandler.class)
    private Date createTime;

    /**
     * 预警状态：1-有预警
     */
    private Integer status;
}
