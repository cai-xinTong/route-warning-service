package com.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warning.entity.WarningInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarningInfoMapper extends BaseMapper<WarningInfo> {

    List<WarningInfo> selectByGeoIds(@Param("geoIds") List<Long> geoIds);
}
