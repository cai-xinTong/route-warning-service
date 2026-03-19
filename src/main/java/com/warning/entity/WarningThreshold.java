package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.warning.typehandler.XuguLocalDateTimeTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "warning_threshold", autoResultMap = true)
public class WarningThreshold {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String warningType;

    private String levelName;

    private Double thresholdValue;

    private String description;

    @TableField(typeHandler = XuguLocalDateTimeTypeHandler.class)
    private LocalDateTime createTime;

    @TableField(typeHandler = XuguLocalDateTimeTypeHandler.class)
    private LocalDateTime updateTime;
}
