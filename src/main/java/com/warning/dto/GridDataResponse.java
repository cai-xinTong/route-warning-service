package com.warning.dto;

import lombok.Data;

import java.util.List;

@Data
public class GridDataResponse {
    private Integer returnCode;
    private String returnMessage;
    private String requestTime;
    private String responseTime;
    private Integer takeTime;
    private Integer rowCount;
    private List<GridDataSet> ds;
}
