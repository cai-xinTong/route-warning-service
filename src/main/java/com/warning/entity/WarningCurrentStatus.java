package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.warning.typehandler.XuguDateTypeHandler;
import lombok.Data;

import java.util.Date;

/**
 * 预警当前状态表
 * 每个 (geo_id, station_code, warning_type) 只有一条记录，每次计算后 upsert。
 * 用于查询各站点当前预警状态，warning_info 保留历史明细用于历史时间点查询。
 */
@Data
@TableName(value = "warning_current_status", autoResultMap = true)
public class WarningCurrentStatus {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 所属预警方案ID */
    private Long planId;

    /** 所属 geoId */
    private Long geoId;

    /** 站点 staId */
    private Long staId;

    /** 站点桩号 */
    private String stationCode;

    /** 站点名称 */
    private String stationName;

    /** 预警类型：RAIN / WIND / VISIBILITY */
    private String warningType;

    /** 预警级别：YELLOW / ORANGE / RED，无预警时为 null */
    private String warningLevel;

    /**
     * 预警状态：1-有预警，0-无预警（已解除或从未触发）
     */
    private Integer status;

    /**
     * 连续无预警次数。有预警时归零，无预警时累加。
     * 达到 4 时触发解除逻辑。
     */
    private Integer noWarningCount;

    /**
     * 是否已解除：1-已解除，0-预警中或从未触发
     * 仅在曾经有过预警、且连续4次无预警后置1
     */
    private Integer isReleased;

    /**
     * 降雨预警专用解除计数器：连续满足解除条件（未来2h累计<5mm）的次数。
     * 每30分钟检查一次，满足则+1，不满足则归零。达到2时触发解除。
     */
    private Integer rainReleaseCount;

    /** 最近一次计算时间 */
    @TableField(typeHandler = XuguDateTypeHandler.class)
    private Date lastBatchTime;

    /** 实测值 */
    private Double actualValue;

    /** 预报值 */
    private Double forecastValue;

    /** 未来3h预报累计值（降雨规则2专用） */
    private Double forecast3hValue;

    /** 预报时间 */
    @TableField(typeHandler = XuguDateTypeHandler.class)
    private Date forecastTime;

    /** 预报周期(小时) */
    private Integer forecastPeriod;
}
