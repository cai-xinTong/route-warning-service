package com.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warning.entity.WeatherServiceGeo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeatherServiceGeoMapper extends BaseMapper<WeatherServiceGeo> {

    List<Long> selectGeoIdsByUpPlanIds(@Param("upPlanIds") List<Long> upPlanIds);
}
