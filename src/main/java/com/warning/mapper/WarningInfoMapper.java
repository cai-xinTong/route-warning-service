package com.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warning.entity.WarningInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface WarningInfoMapper extends BaseMapper<WarningInfo> {

    /**
     * 查询指定 planId 下最近一批 create_time。
     */
    Date selectLatestBatchTime(@Param("planId") Long planId,
                               @Param("pointTime") Date pointTime,
                               @Param("endTime") Date endTime);

    /**
     * 按 planId 查询预警记录。
     * - batchTime 不为 null：精确匹配该批次时间（时间点模式）
     * - batchTime 为 null：按 startTime/endTime 范围过滤（时间范围模式）
     */
    List<WarningInfo> selectByPlanId(@Param("planId") Long planId,
                                     @Param("startTime") Date startTime,
                                     @Param("endTime") Date endTime,
                                     @Param("batchTime") Date batchTime);
}
