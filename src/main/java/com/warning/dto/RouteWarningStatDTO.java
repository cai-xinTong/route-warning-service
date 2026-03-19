package com.warning.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 单条线路预警统计
 */
@Data
public class RouteWarningStatDTO {

    /** 路段编号 */
    private String roadCode;

    /** 路段名称 */
    private String roadName;

    /** 该线路节点总数 */
    private int totalNodes;

    /**
     * 各预警级别的节点数
     * key: RED / ORANGE / YELLOW
     * value: 节点数
     */
    private Map<String, Integer> levelCount = new HashMap<>();

    /**
     * 最高风险级别（RED > ORANGE > YELLOW > null）
     */
    private String maxLevel;

    /**
     * 有预警的节点数（不论级别）
     */
    private int warningNodes;
}