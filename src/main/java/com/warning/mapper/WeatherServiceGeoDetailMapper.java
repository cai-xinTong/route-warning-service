package com.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warning.entity.WeatherServiceGeoDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeatherServiceGeoDetailMapper extends BaseMapper<WeatherServiceGeoDetail> {

    List<WeatherServiceGeoDetail> selectByGeoIds(@Param("geoIds") List<Long> geoIds);
}
