package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 站点精细化阈值表（weather_service_detail_threshold）
 * 每个站点+要素可以有独立的预警阈值，精确到点/线级别。
 * 若某站点未配置，则回退使用 warning_threshold 全局阈值。
 */
@Data
@TableName(value = "weather_service_detail_threshold")
public class WeatherServiceDetailThreshold {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 站点ID，对应 weather_service_geo_detail.staId */
    @TableField("staId")
    private Long staId;

    /** 要素：RAIN / WIND / VISIBILITY */
    private String element;

    /** 级别：YELLOW / ORANGE / RED */
    private String level;

    /** 阈值 */
    private Double value;
}
