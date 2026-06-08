package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("weather_service_geo")
public class WeatherServiceGeo {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long upPlanId;
}
