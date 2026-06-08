package com.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warning.entity.WarningCurrentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WarningCurrentStatusMapper extends BaseMapper<WarningCurrentStatus> {

    List<WarningCurrentStatus> selectByPlanId(@Param("planId") Long planId);

    WarningCurrentStatus selectByKey(@Param("planId") Long planId,
                                     @Param("geoId") Long geoId,
                                     @Param("staId") Long staId,
                                     @Param("warningType") String warningType);

    void insertRecord(@Param("record") WarningCurrentStatus record);

    void updateRecord(@Param("record") WarningCurrentStatus record);

    @Select("SELECT * FROM warning_current_status WHERE ROWNUM <= 1")
    Map<String, Object> selectOneAsMap();

    @Select("SELECT * FROM warning_info WHERE ROWNUM <= 1")
    Map<String, Object> selectWarningInfoAsMap();
}
