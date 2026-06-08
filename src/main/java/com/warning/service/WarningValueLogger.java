package com.warning.service;

import com.warning.entity.WeatherServiceGeoDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 每次预警计算将实测/预报值写入文件（30分钟一个文件），并自动清理1天前的旧文件。
 */
@Slf4j
@Service
public class WarningValueLogger {

    private static final String DIR = "./warning-logs";
    private static final SimpleDateFormat FILE_DATE = new SimpleDateFormat("yyyyMMdd_HHmm");
    private static final SimpleDateFormat LINE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private PrintWriter writer;
    private File currentFile;

    /**
     * 每个批次开始前调用：创建新文件，清理旧文件。
     */
    public void beginBatch(Date batchTime) {
        close();

        File dir = new File(DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "warning_" + FILE_DATE.format(batchTime) + ".log";
        currentFile = new File(dir, fileName);
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(currentFile, true)), true);
            log.info("预警数值日志: {}", currentFile.getPath());
        } catch (IOException e) {
            log.error("创建预警日志文件失败: {}", currentFile.getPath(), e);
            writer = null;
        }

        cleanOldFiles(batchTime);
    }

    /**
     * 写一条记录。
     */
    public void log(Long planId, String planName,
                    WeatherServiceGeoDetail detail, String warningType,
                    Double actualValue, Double forecastValue,
                    Date batchTime, Double forecast3hValue) {
        if (writer == null) return;
        String line = String.format("%s | plan=%d(%s) | staId=%d, %s(%s) | %s | actual=%s forecast=%s%s",
                LINE_TIME.format(batchTime),
                planId, planName == null ? "" : planName,
                detail.getStaId(), detail.getStationName(), detail.getStationCode(),
                warningType,
                actualValue == null ? "null" : String.format("%.2f", actualValue),
                forecastValue == null ? "null" : String.format("%.2f", forecastValue),
                forecast3hValue != null ? String.format(" forecast3h=%.2f", forecast3hValue) : "");
        writer.println(line);
    }

    /**
     * 批次结束后关闭文件。
     */
    public void endBatch() {
        close();
    }

    private void close() {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
            currentFile = null;
        }
    }

    /**
     * 清理24小时前的日志文件。
     */
    private void cleanOldFiles(Date now) {
        File dir = new File(DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("warning_") && name.endsWith(".log"));
        if (files == null) return;

        long cutoff = now.getTime() - 24L * 60 * 60 * 1000;
        int deleted = 0;
        for (File f : files) {
            if (f.lastModified() < cutoff) {
                if (f.delete()) deleted++;
            }
        }
        if (deleted > 0) {
            log.info("清理过期预警日志: {} 个文件", deleted);
        }
    }
}
