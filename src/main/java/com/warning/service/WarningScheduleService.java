package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warning.dto.WarningCheckResult;
import com.warning.entity.*;
import com.warning.mapper.WarningCurrentStatusMapper;
import com.warning.mapper.WarningInfoMapper;
import com.warning.mapper.WeatherServiceGeoDetailMapper;
import com.warning.mapper.WeatherServiceGeoMapper;
import com.warning.mapper.WeatherServicePlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WarningScheduleService {

    @Resource
    private WeatherServicePlanMapper planMapper;

    @Resource
    private WeatherServiceGeoMapper geoMapper;

    @Resource
    private WeatherServiceGeoDetailMapper geoDetailMapper;

    @Resource
    private WarningInfoMapper warningInfoMapper;

    @Resource
    private WarningCurrentStatusMapper currentStatusMapper;

    @Resource
    private RainWarningService rainWarningService;

    @Resource
    private WindWarningService windWarningService;

    @Resource
    private VisibilityWarningService visibilityWarningService;

    @Resource
    private GridDataService gridDataService;

    @Resource
    private WarningValueLogger valueLogger;

    /**
     * 每30分钟执行一次预警更新任务
     */
    @PostConstruct
    @Scheduled(cron = "0 */30 * * * ?")
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
            List<WarningCurrentStatus> statusUpdates = new ArrayList<>();
            int rainCount = 0, windCount = 0, visCount = 0;
            Date now = new Date();

            // 0. 创建本批次日志文件 + 清理旧文件
            valueLogger.beginBatch(now);

            for (WeatherServicePlan plan : plans) {
                log.info("处理方案[id={}, name={}]", plan.getId(), plan.getPlanName());

                String upPlanIdStr = plan.getUpPlanId();
                if (upPlanIdStr == null || upPlanIdStr.trim().isEmpty()) {
                    log.warn("方案[{}]的upPlanId为空，跳过", plan.getId());
                    continue;
                }
                List<Long> upPlanIds = Arrays.stream(upPlanIdStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                log.info("方案[{}]解析upPlanIds: {}", plan.getId(), upPlanIds);

                List<Long> geoIds = geoMapper.selectGeoIdsByUpPlanIds(upPlanIds);
                if (geoIds.isEmpty()) {
                    log.warn("方案[{}]的upPlanIds{}未在weather_service_geo中找到geoId", plan.getId(), upPlanIds);
                    continue;
                }
                log.info("方案[{}]查询到geoIds: {}", plan.getId(), geoIds);

                List<WeatherServiceGeoDetail> geoDetails = geoDetailMapper.selectByGeoIds(geoIds);
                if (geoDetails.isEmpty()) {
                    log.warn("方案[{}]的GeoIds{}未查到路段数据", plan.getId(), geoIds);
                    continue;
                }
                log.info("方案[{}]查询到{}个路段站点", plan.getId(), geoDetails.size());

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

                for (WeatherServiceGeoDetail detail : geoDetails) {
                    GeoLineNode node = toGeoLineNode(detail);
                    Long planId = plan.getId();

                    if (elements.contains("RAIN")) {
                        WarningCheckResult rainResult = rainWarningService.checkWarning(node);
                        valueLogger.log(planId, plan.getPlanName(), detail, "RAIN",
                                rainResult.getActualValue(), rainResult.getForecastValue(), now,
                                rainResult.getForecast3hValue());
                        WarningInfo rainWarning = rainResult.getWarning();
                        if (rainWarning != null) {
                            rainWarning.setPlanId(planId);
                            rainWarning.setCreateTime(now);
                            rainWarning.setStatus(1);
                            warnings.add(rainWarning);
                            rainCount++;
                        }
                        // 降雨预警状态更新（专用解除逻辑）
                        handleRainStatusUpdate(planId, detail, rainResult, node, now);
                    }

                    if (elements.contains("WIND")) {
                        WarningCheckResult windResult = windWarningService.checkWarning(node);
                        valueLogger.log(planId, plan.getPlanName(), detail, "WIND",
                                windResult.getActualValue(), windResult.getForecastValue(), now, null);
                        WarningInfo windWarning = windResult.getWarning();
                        if (windWarning != null) {
                            windWarning.setPlanId(planId);
                            windWarning.setCreateTime(now);
                            windWarning.setStatus(1);
                            warnings.add(windWarning);
                            windCount++;
                        }
                        statusUpdates.add(buildCurrentStatus(planId, detail, "WIND", windResult, now));
                    }

                    if (elements.contains("VISIBILITY")) {
                        WarningCheckResult visResult = visibilityWarningService.checkWarning(node);
                        valueLogger.log(planId, plan.getPlanName(), detail, "VISIBILITY",
                                visResult.getActualValue(), visResult.getForecastValue(), now, null);
                        WarningInfo visWarning = visResult.getWarning();
                        if (visWarning != null) {
                            visWarning.setPlanId(planId);
                            visWarning.setCreateTime(now);
                            visWarning.setStatus(1);
                            warnings.add(visWarning);
                            visCount++;
                        }
                        statusUpdates.add(buildCurrentStatus(planId, detail, "VISIBILITY", visResult, now));
                    }
                }
            }

            // 批量插入预警明细（保留历史）
            for (WarningInfo warning : warnings) {
                warningInfoMapper.insert(warning);
            }

            // upsert 当前状态表（select + insert/update），RAIN已在上面单独处理
            for (WarningCurrentStatus incoming : statusUpdates) {
                if ("RAIN".equals(incoming.getWarningType())) {
                    continue; // 降雨已单独处理
                }
                WarningCurrentStatus existing = currentStatusMapper.selectByKey(
                        incoming.getPlanId(), incoming.getGeoId(), incoming.getStaId(), incoming.getWarningType());
                if (existing == null) {
                    // 首次写入：有预警 count=0，无预警 count=1
                    incoming.setNoWarningCount(incoming.getStatus() == 1 ? 0 : 1);
                    incoming.setIsReleased(0);
                    currentStatusMapper.insertRecord(incoming);
                } else {
                    // 更新：有预警归零，无预警累加，满4次置解除
                    if (incoming.getStatus() == 1) {
                        incoming.setNoWarningCount(0);
                        incoming.setIsReleased(0);
                    } else {
                        int newCount = (existing.getNoWarningCount() == null ? 0 : existing.getNoWarningCount()) + 1;
                        incoming.setNoWarningCount(newCount);
                        incoming.setIsReleased(newCount >= 4 ? 1 : (existing.getIsReleased() == null ? 0 : existing.getIsReleased()));
                    }
                    currentStatusMapper.updateRecord(incoming);
                }
            }

            // 抽样检查一条风/能见度记录的 forecastTime
            if (!statusUpdates.isEmpty()) {
                WarningCurrentStatus sample = statusUpdates.get(0);
                log.info("状态更新样本: type={} staId={} forecastTime={} lastBatchTime={}",
                        sample.getWarningType(), sample.getStaId(), sample.getForecastTime(), sample.getLastBatchTime());
            }

            long endTime = System.currentTimeMillis();
            log.info("预警更新完成: 方案={}, 雨={}, 风={}, 能见度={}, 明细={}, 状态更新={}, 耗时={}ms",
                    plans.size(), rainCount, windCount, visCount, warnings.size(), statusUpdates.size(), (endTime - startTime));

        } catch (Exception e) {
            log.error("预警更新任务失败", e);
        } finally {
            valueLogger.endBatch();
        }
    }

    public void manualUpdate() {
        log.info("手动触发预警更新");
        updateWarnings();
    }

    private WarningCurrentStatus buildCurrentStatus(Long planId,
                                                    WeatherServiceGeoDetail detail,
                                                    String warningType,
                                                    WarningCheckResult result,
                                                    Date batchTime) {
        WarningCurrentStatus cs = new WarningCurrentStatus();
        cs.setPlanId(planId);
        cs.setGeoId(detail.getGeoId());
        cs.setStaId(detail.getStaId());
        cs.setStationCode(detail.getStationCode());
        cs.setStationName(detail.getStationName());
        cs.setWarningType(warningType);
        cs.setLastBatchTime(batchTime);
        WarningInfo warningInfo = result.getWarning();
        if (warningInfo != null) {
            cs.setStatus(1);
            cs.setWarningLevel(warningInfo.getWarningLevel());
        } else {
            cs.setStatus(0);
            cs.setWarningLevel(null);
        }
        // 实测/预报值：无论是否触发预警都写入
        cs.setActualValue(result.getActualValue());
        cs.setForecastValue(result.getForecastValue());
        cs.setForecast3hValue(result.getForecast3hValue());
        cs.setForecastTime(result.getForecastTime());
        cs.setForecastPeriod(result.getForecastPeriod());
        return cs;
    }

    /**
     * 降雨预警状态更新（专用解除逻辑）。
     * 解除规则：连续2个30分钟间隔判断未来2h累计 < 5mm 则解除。
     */
    private void handleRainStatusUpdate(Long planId,
                                        WeatherServiceGeoDetail detail,
                                        WarningCheckResult result,
                                        GeoLineNode node,
                                        Date batchTime) {
        WarningCurrentStatus incoming = buildCurrentStatus(planId, detail, "RAIN", result, batchTime);
        WarningCurrentStatus existing = currentStatusMapper.selectByKey(
                planId, detail.getGeoId(), detail.getStaId(), "RAIN");

        if (existing == null) {
            // 首次写入
            incoming.setNoWarningCount(incoming.getStatus() == 1 ? 0 : 1);
            incoming.setIsReleased(0);
            incoming.setRainReleaseCount(0);
            currentStatusMapper.insertRecord(incoming);
            return;
        }

        if (incoming.getStatus() == 1) {
            // 有预警：重置所有解除计数器
            incoming.setNoWarningCount(0);
            incoming.setIsReleased(0);
            incoming.setRainReleaseCount(0);
            currentStatusMapper.updateRecord(incoming);
            return;
        }

        // 无预警：判断解除条件
        boolean isReleaseCheckTime = isThirtyMinuteBoundary();
        int existingRainCount = existing.getRainReleaseCount() == null ? 0 : existing.getRainReleaseCount();
        int existingIsReleased = existing.getIsReleased() == null ? 0 : existing.getIsReleased();

        if (isReleaseCheckTime) {
            boolean releasable = rainWarningService.checkReleaseCondition(node);
            if (releasable) {
                int newRainCount = existingRainCount + 1;
                incoming.setRainReleaseCount(newRainCount);
                incoming.setIsReleased(newRainCount >= 2 ? 1 : existingIsReleased);
                log.debug("降雨解除计数累加: node={}, rainReleaseCount={}/2", node.getName(), newRainCount);
            } else {
                // 不满足解除条件，重置计数器
                incoming.setRainReleaseCount(0);
                incoming.setIsReleased(existingIsReleased);
            }
        } else {
            // 非30分钟边界，保持现有计数器不变
            incoming.setRainReleaseCount(existingRainCount);
            incoming.setIsReleased(existingIsReleased);
        }

        // 维护 noWarningCount（兼容旧逻辑）
        int newNoWarningCount = (existing.getNoWarningCount() == null ? 0 : existing.getNoWarningCount()) + 1;
        incoming.setNoWarningCount(newNoWarningCount);

        log.info("降雨状态更新: staId={} forecastTime={} lastBatchTime={}",
                detail.getStaId(), incoming.getForecastTime(), incoming.getLastBatchTime());

        currentStatusMapper.updateRecord(incoming);
    }

    /**
     * 判断当前时间是否是30分钟边界（:00 或 :30），用于降雨解除检查。
     */
    private boolean isThirtyMinuteBoundary() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int minute = cal.get(java.util.Calendar.MINUTE);
        return minute == 0 || minute == 30;
    }

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
