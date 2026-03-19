package com.warning.service;

import com.warning.entity.GeoLineNode;
import com.warning.entity.WarningInfo;
import com.warning.mapper.GeoLineNodeMapper;
import com.warning.mapper.WarningInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WarningScheduleService {

    @Resource
    private GeoLineNodeMapper geoLineNodeMapper;

    @Resource
    private WarningInfoMapper warningInfoMapper;

    @Resource
    private RainWarningService rainWarningService;

    @Resource
    private WindWarningService windWarningService;

    @Resource
    private VisibilityWarningService visibilityWarningService;

    @Resource
    private GridDataService gridDataService;

    /**
     * 每6分钟执行一次预警更新任务
     */
    @Scheduled(cron = "0 */6 * * * ?")
    public void updateWarnings() {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始执行预警更新任务 ==========");

        try {
            // 1. 清空预警表
            warningInfoMapper.delete(null);
            log.info("已清空预警表");

            // 2. 清空网格数据缓存
            gridDataService.clearCache();

            // 3. 获取所有路段节点
            List<GeoLineNode> nodes = geoLineNodeMapper.selectList(null);
            log.info("共{}个路段节点需要检查", nodes.size());

            if (nodes.isEmpty()) {
                log.warn("路段节点为空，请先导入Excel数据");
                return;
            }

            // 4. 遍历所有节点，检查预警（时间由 GridDataService 自动从接口获取）
            List<WarningInfo> warnings = new ArrayList<>();
            int rainCount = 0, windCount = 0, visCount = 0;

            for (GeoLineNode node : nodes) {
                // 暴雨预警
                WarningInfo rainWarning = rainWarningService.checkWarning(node);
                if (rainWarning != null) {
                    warnings.add(rainWarning);
                    rainCount++;
                }

                // 大风预警
                WarningInfo windWarning = windWarningService.checkWarning(node);
                if (windWarning != null) {
                    warnings.add(windWarning);
                    windCount++;
                }

                // 能见度预警
                WarningInfo visWarning = visibilityWarningService.checkWarning(node);
                if (visWarning != null) {
                    warnings.add(visWarning);
                    visCount++;
                }
            }

            // 5. 批量插入预警信息
            if (!warnings.isEmpty()) {
                for (WarningInfo warning : warnings) {
                    warningInfoMapper.insert(warning);
                }
                log.info("成功插入{}条预警信息", warnings.size());
            } else {
                log.info("本次未产生预警信息");
            }

            long endTime = System.currentTimeMillis();
            log.info("========== 预警更新任务完成 ==========");
            log.info("统计信息: 检查节点数={}, 暴雨预警={}, 大风预警={}, 能见度预警={}, 总预警数={}, 耗时={}ms",
                    nodes.size(), rainCount, windCount, visCount, warnings.size(), (endTime - startTime));

        } catch (Exception e) {
            log.error("预警更新任务执行失败", e);
        }
    }

    /**
     * 手动触发预警更新（用于测试）
     */
    public void manualUpdate() {
        log.info("手动触发预警更新");
        updateWarnings();
    }
}
