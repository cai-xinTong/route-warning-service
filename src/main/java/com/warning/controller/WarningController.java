package com.warning.controller;

import com.warning.dto.PageResult;
import com.warning.dto.RouteWarningStatDTO;
import com.warning.dto.WarningQueryDTO;
import com.warning.dto.WarningStatisticsDTO;
import com.warning.entity.WarningInfo;
import com.warning.entity.WarningThreshold;
import com.warning.service.ExcelImportService;
import com.warning.service.WarningQueryService;
import com.warning.service.WarningScheduleService;
import com.warning.service.WarningThresholdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class WarningController {

    @Resource
    private WarningScheduleService warningScheduleService;

    @Resource
    private ExcelImportService excelImportService;

    @Resource
    private WarningQueryService warningQueryService;

    @Resource
    private WarningThresholdService warningThresholdService;

    /**
     * 分页查询预警信息
     */
    @PostMapping("/warning/query")
    public Map<String, Object> queryWarnings(@RequestBody WarningQueryDTO query) {
        Map<String, Object> result = new HashMap<>();
        try {
            PageResult<WarningInfo> pageResult = warningQueryService.queryWarnings(query);
            result.put("success", true);
            result.put("data", pageResult);
        } catch (Exception e) {
            log.error("查询预警信息失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取预警统计信息
     */
    @GetMapping("/warning/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            WarningStatisticsDTO statistics = warningQueryService.getStatistics();
            result.put("success", true);
            result.put("data", statistics);
        } catch (Exception e) {
            log.error("获取预警统计失败", e);
            result.put("success", false);
            result.put("message", "获取统计失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询预警详情
     */
    @GetMapping("/warning/{id}")
    public Map<String, Object> getWarningById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            WarningInfo warning = warningQueryService.getWarningById(id);
            if (warning != null) {
                result.put("success", true);
                result.put("data", warning);
            } else {
                result.put("success", false);
                result.put("message", "预警信息不存在");
            }
        } catch (Exception e) {
            log.error("查询预警详情失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取最新的N条预警
     */
    @GetMapping("/warning/latest")
    public Map<String, Object> getLatestWarnings(@RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WarningInfo> warnings = warningQueryService.getLatestWarnings(limit);
            result.put("success", true);
            result.put("data", warnings);
            result.put("total", warnings.size());
        } catch (Exception e) {
            log.error("获取最新预警失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 按类型查询预警列表
     */
    @GetMapping("/warning/type/{warningType}")
    public Map<String, Object> getWarningsByType(@PathVariable String warningType) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WarningInfo> warnings = warningQueryService.getWarningsByType(warningType);
            result.put("success", true);
            result.put("data", warnings);
            result.put("total", warnings.size());
        } catch (Exception e) {
            log.error("按类型查询预警失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 按级别查询预警列表
     */
    @GetMapping("/warning/level/{warningLevel}")
    public Map<String, Object> getWarningsByLevel(@PathVariable String warningLevel) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WarningInfo> warnings = warningQueryService.getWarningsByLevel(warningLevel);
            result.put("success", true);
            result.put("data", warnings);
            result.put("total", warnings.size());
        } catch (Exception e) {
            log.error("按级别查询预警失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 手动触发预警更新
     */
    @PostMapping("/warning/update")
    public Map<String, Object> manualUpdate() {
        Map<String, Object> result = new HashMap<>();
        try {
            warningScheduleService.manualUpdate();
            result.put("success", true);
            result.put("message", "预警更新任务已触发");
        } catch (Exception e) {
            log.error("手动触发预警更新失败", e);
            result.put("success", false);
            result.put("message", "预警更新失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 导入Excel数据
     */
    @PostMapping("/import/excel")
    public Map<String, Object> importExcel(@RequestParam String filePath) {
        Map<String, Object> result = new HashMap<>();
        try {
            excelImportService.importExcelWithDownsampling(filePath);
            result.put("success", true);
            result.put("message", "Excel数据导入成功");
        } catch (Exception e) {
            log.error("导入Excel失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据预警方案ID查询对应的预警记录
     */
    @GetMapping("/warning/plan")
    public Map<String, Object> getWarningsByPlanId(@RequestParam Long planId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WarningInfo> warnings = warningQueryService.queryWarningsByPlanId(planId);
            result.put("success", true);
            result.put("data", warnings);
            result.put("total", warnings.size());
        } catch (Exception e) {
            log.error("按方案ID查询预警失败, planId={}", planId, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "route-warning-service");
        return result;
    }

    /**
     * 统计每条线路的预警情况（各级别预警节点数、最高风险级别）
     */
    @GetMapping("/warning/route/statistics")
    public Map<String, Object> getRouteWarningStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<RouteWarningStatDTO> data = warningQueryService.getRouteWarningStatistics();
            result.put("success", true);
            result.put("data", data);
            result.put("total", data.size());
        } catch (Exception e) {
            log.error("统计线路预警失败", e);
            result.put("success", false);
            result.put("message", "统计失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 查询预警阈值列表（支持按 warningType、levelName 过滤）
     */
    @GetMapping("/threshold/list")
    public Map<String, Object> listThresholds(
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) String levelName) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WarningThreshold> data = warningThresholdService.listThresholds(warningType, levelName);
            result.put("success", true);
            result.put("data", data);
            result.put("total", data.size());
        } catch (Exception e) {
            log.error("查询预警阈值失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID灵活更新预警阈值（只更新传入的非null字段）
     */
    @PutMapping("/threshold/update")
    public Map<String, Object> updateThreshold(@RequestBody WarningThreshold threshold) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean updated = warningThresholdService.updateThreshold(threshold);
            result.put("success", updated);
            result.put("message", updated ? "更新成功" : "记录不存在或无可更新字段");
        } catch (Exception e) {
            log.error("更新预警阈值失败", e);
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }
}
