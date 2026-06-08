package com.warning.dto;

import com.warning.entity.WarningInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 预警计算结果，包含预警信息（可能为 null）以及实测/预报值。
 * 无论是否触发预警，实测/预报值都应当被保存到 current_status。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarningCheckResult {

    /** 预警信息，未触发时为 null */
    private WarningInfo warning;

    /** 实测值 */
    private Double actualValue;

    /** 预报值 */
    private Double forecastValue;

    /** 预报时间 */
    private Date forecastTime;

    /** 预报周期(小时) */
    private Integer forecastPeriod;

    /** 预报3h累计值（降雨规则2用，其它类型为null） */
    private Double forecast3hValue;
}
