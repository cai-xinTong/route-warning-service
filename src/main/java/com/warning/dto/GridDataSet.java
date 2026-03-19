package com.warning.dto;

import lombok.Data;

import java.util.List;

@Data
public class GridDataSet {
    private String dataCode;
    private String element;
    private String reftime;
    private Integer time;
    private Double minLon;
    private Double minLat;
    private Double maxLon;
    private Double maxLat;
    private Integer latGridNumber;
    private Integer lonGridNumber;
    private Double latGridSpace;
    private Double lonGridSpace;
    private Double noDataValue;
    private List<Double> dataArray;
    private String updatetime;
    private String description;
    private String dataKey;
}
