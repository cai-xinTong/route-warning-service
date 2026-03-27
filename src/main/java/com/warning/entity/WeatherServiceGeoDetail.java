package com.warning.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("weather_service_geo_detail")
public class WeatherServiceGeoDetail {

    @TableId("staId")
    private Long staId;

    @TableField("lon")
    private Double lon;

    @TableField("lat")
    private Double lat;

    @TableField("geoId")
    private Long geoId;

    @TableField("stationName")
    private String stationName;

    @TableField("stationCode")
    private String stationCode;
}
