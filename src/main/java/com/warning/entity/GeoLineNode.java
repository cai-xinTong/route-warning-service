package com.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.warning.typehandler.XuguLocalDateTimeTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "geo_line_node", autoResultMap = true)
public class GeoLineNode {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long objectId;

    private Integer seq;

    private String name;

    private String code;

    private Double lon;

    private Double lat;

    private String properties;

    private String company;

    private String roadCode;

    private String roadName;

    @TableField(typeHandler = XuguLocalDateTimeTypeHandler.class)
    private LocalDateTime createTime;
}
