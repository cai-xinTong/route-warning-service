package com.warning.dto;

import lombok.Data;

import java.util.List;

@Data
public class TimeListResponse {
    private Integer returnCode;
    private String returnMessage;
    private List<String> ds;
}
