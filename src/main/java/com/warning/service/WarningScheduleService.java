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
import java.util.Date;
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
     * 每10分钟执行一次预警更新任务
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void updateWarnings() {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 清空网格数据缓存
            gridDataService.clearCache();

            // 2. 获取所有路段节点
            List<GeoLineNode> nodes = geoLineNodeMapper.selectList(null);

            if (nodes.isEmpty()) {
                log.warn("路段节点为空，请先导入Excel数据");
                return;
            }

            // 3. 遍历所有节点，检查预警（时间由 GridDataService 自动从接口获取）
            List<WarningInfo> warnings = new ArrayList<>();
            int rainCount = 0, windCount = 0, visCount = 0;
            Date now = new Date();

            for (GeoLineNode node : nodes) {
                // 暴雨预警
                WarningInfo rainWarning = rainWarningService.checkWarning(node);
                if (rainWarning != null) {
                    rainWarning.setCreateTime(now);
                    warnings.add(rainWarning);
                    rainCount++;
                }

                // 大风预警
                WarningInfo windWarning = windWarningService.checkWarning(node);
                if (windWarning != null) {
                    windWarning.setCreateTime(now);
                    warnings.add(windWarning);
                    windCount++;
                }

                // 能见度预警
                WarningInfo visWarning = visibilityWarningService.checkWarning(node);
                if (visWarning != null) {
                    visWarning.setCreateTime(now);
                    warnings.add(visWarning);
                    visCount++;
                }
            }

            // 4. 批量插入预警信息（保留历史记录）
            if (!warnings.isEmpty()) {
                for (WarningInfo warning : warnings) {
                    warningInfoMapper.insert(warning);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("预警更新完成: 节点={}, 雨={}, 风={}, 能见度={}, 总计={}, 耗时={}ms",
                    nodes.size(), rainCount, windCount, visCount, warnings.size(), (endTime - startTime));

        } catch (Exception e) {
            log.error("预警更新任务失败", e);
        }
    }

    public void manualUpdate() {
        log.info("手动触发预警更新");
        updateWarnings();
    }
}
