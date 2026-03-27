package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warning.entity.GeoLineNode;
import com.warning.entity.WarningInfo;
import com.warning.entity.WeatherServiceGeoDetail;
import com.warning.entity.WeatherServicePlan;
import com.warning.mapper.WarningInfoMapper;
import com.warning.mapper.WeatherServiceGeoDetailMapper;
import com.warning.mapper.WeatherServicePlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WarningScheduleService {

    @Resource
    private WeatherServicePlanMapper planMapper;

    @Resource
    private WeatherServiceGeoDetailMapper geoDetailMapper;

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
    @PostConstruct
    @Scheduled(cron = "0 */10 * * * ?")
    public void updateWarnings() {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 清空网格数据缓存
            gridDataService.clearCache();

            // 2. 查询 status=1 的预警方案
            QueryWrapper<WeatherServicePlan> planQuery = new QueryWrapper<>();
            planQuery.eq("status", "1");
            List<WeatherServicePlan> plans = planMapper.selectList(planQuery);

            if (plans.isEmpty()) {
                log.warn("未找到状态为1的预警方案，跳过本次预警计算");
                return;
            }
            log.info("查询到{}条启用的预警方案", plans.size());

            List<WarningInfo> warnings = new ArrayList<>();
            int rainCount = 0, windCount = 0, visCount = 0;
            Date now = new Date();

            for (WeatherServicePlan plan : plans) {
                log.info("处理方案[id={}, name={}]", plan.getId(), plan.getPlanName());

                // 3. 解析 GeoIds（逗号分隔的整数字符串）
                String geoIdsStr = plan.getGeoIds();
                if (geoIdsStr == null || geoIdsStr.trim().isEmpty()) {
                    log.warn("方案[{}]的GeoIds为空，跳过", plan.getId());
                    continue;
                }
                List<Long> geoIds = Arrays.stream(geoIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                log.info("方案[{}]解析GeoIds: {}", plan.getId(), geoIds);

                // 4. 按 GeoIds 查询对应的路段站点
                List<WeatherServiceGeoDetail> geoDetails = geoDetailMapper.selectByGeoIds(geoIds);
                if (geoDetails.isEmpty()) {
                    log.warn("方案[{}]的GeoIds{}未查到路段数据", plan.getId(), geoIds);
                    continue;
                }
                log.info("方案[{}]查询到{}个路段站点", plan.getId(), geoDetails.size());

                // 5. 解析 elements（逗号分隔，如 WIND,VISIBILITY,RAIN）
                String elementsStr = plan.getElements();
                List<String> elements = (elementsStr == null || elementsStr.trim().isEmpty())
                        ? new ArrayList<>()
                        : Arrays.stream(elementsStr.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());

                if (elements.isEmpty()) {
                    log.warn("方案[{}]的elements为空，无要素可计算", plan.getId());
                    continue;
                }
                log.info("方案[{}]启用要素: {}, 匹配RAIN={}, WIND={}, VISIBILITY={}",
                        plan.getId(), elements,
                        elements.contains("RAIN"),
                        elements.contains("WIND"),
                        elements.contains("VISIBILITY"));

                // 6. 对每个站点按 elements 计算预警
                for (WeatherServiceGeoDetail detail : geoDetails) {
                    GeoLineNode node = toGeoLineNode(detail);
                    log.debug("计算站点[stationCode={}, lon={}, lat={}]", detail.getStationCode(), detail.getLon(), detail.getLat());

                    if (elements.contains("RAIN")) {
                        WarningInfo rainWarning = rainWarningService.checkWarning(node);
                        if (rainWarning != null) {
                            rainWarning.setCreateTime(now);
                            warnings.add(rainWarning);
                            rainCount++;
                            log.debug("站点[{}]触发RAIN预警, 级别={}", detail.getStationCode(), rainWarning.getWarningLevel());
                        }
                    }

                    if (elements.contains("WIND")) {
                        WarningInfo windWarning = windWarningService.checkWarning(node);
                        if (windWarning != null) {
                            windWarning.setCreateTime(now);
                            warnings.add(windWarning);
                            windCount++;
                            log.debug("站点[{}]触发WIND预警, 级别={}", detail.getStationCode(), windWarning.getWarningLevel());
                        }
                    }

                    if (elements.contains("VISIBILITY")) {
                        WarningInfo visWarning = visibilityWarningService.checkWarning(node);
                        if (visWarning != null) {
                            visWarning.setCreateTime(now);
                            warnings.add(visWarning);
                            visCount++;
                            log.debug("站点[{}]触发VISIBILITY预警, 级别={}", detail.getStationCode(), visWarning.getWarningLevel());
                        }
                    }
                }
            }

            // 7. 批量插入预警信息（保留历史记录）
            if (!warnings.isEmpty()) {
                for (WarningInfo warning : warnings) {
                    warningInfoMapper.insert(warning);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("预警更新完成: 方案={}, 雨={}, 风={}, 能见度={}, 总计={}, 耗时={}ms",
                    plans.size(), rainCount, windCount, visCount, warnings.size(), (endTime - startTime));

        } catch (Exception e) {
            log.error("预警更新任务失败", e);
        }
    }

    public void manualUpdate() {
        log.info("手动触发预警更新");
        updateWarnings();
    }

    /**
     * 将 WeatherServiceGeoDetail 映射为 GeoLineNode，供现有 WarningService 使用
     */
    private GeoLineNode toGeoLineNode(WeatherServiceGeoDetail detail) {
        GeoLineNode node = new GeoLineNode();
        node.setStaId(detail.getStaId());
        node.setGeoId(detail.getGeoId());
        node.setLon(detail.getLon());
        node.setLat(detail.getLat());
        node.setName(detail.getStationCode());
        node.setRoadName(detail.getStationName());
        return node;
    }
}
