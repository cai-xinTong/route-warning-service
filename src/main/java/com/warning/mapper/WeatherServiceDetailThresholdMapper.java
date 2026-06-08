package com.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warning.entity.WeatherServiceDetailThreshold;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 站点精细化阈值 Mapper
 */
@Mapper
public interface WeatherServiceDetailThresholdMapper extends BaseMapper<WeatherServiceDetailThreshold> {

    /**
     * 根据站点ID和要素查询该站点的所有级别阈值。
     * @param staId   站点ID
     * @param element 要素 (RAIN/WIND/VISIBILITY)
     * @return 阈值列表，可能为空
     */
    @Select("SELECT id, staId, element, level, value FROM weather_service_detail_threshold WHERE staId = #{staId} AND element = #{element}")
    List<WeatherServiceDetailThreshold> selectByStaIdAndElement(@Param("staId") Long staId,
                                                                @Param("element") String element);
}
