package com.warning.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 将LocalDateTime转换为接口需要的时间格式
     */
    public static String formatTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    /**
     * 获取当前时间的格式化字符串
     */
    public static String getCurrentTimeStr() {
        return formatTime(LocalDateTime.now());
    }
}
