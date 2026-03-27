package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("weather_service_plan")
public class WeatherServicePlan {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("departmentId")
    private String departmentId;

    @TableField("elements")
    private String elements;

    @TableField("GeoIds")
    private String geoIds;

    @TableField("status")
    private String status;

    @TableField("type")
    private String type;

    @TableField("planName")
    private String planName;
}
