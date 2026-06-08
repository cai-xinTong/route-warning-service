package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warning.dto.*;
import com.warning.entity.GeoLineNode;
import com.warning.entity.WarningCurrentStatus;
import com.warning.entity.WarningInfo;
import com.warning.entity.WeatherServiceGeoDetail;
import com.warning.mapper.WarningCurrentStatusMapper;
import com.warning.entity.WeatherServicePlan;
import com.warning.mapper.GeoLineNodeMapper;
import com.warning.mapper.WarningInfoMapper;
import com.warning.mapper.WeatherServiceGeoDetailMapper;
import com.warning.mapper.WeatherServiceGeoMapper;
import com.warning.mapper.WeatherServicePlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WarningQueryService {

    @Resource
    private WarningInfoMapper warningInfoMapper;

    @Resource
    private WarningCurrentStatusMapper currentStatusMapper;

    @Resource
    private GeoLineNodeMapper geoLineNodeMapper;

    @Resource
    private WeatherServicePlanMapper weatherServicePlanMapper;

    @Resource
    private WeatherServiceGeoMapper weatherServiceGeoMapper;

    @Resource
    private WeatherServiceGeoDetailMapper geoDetailMapper;

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询预警信息
     */
    public PageResult<WarningInfo> queryWarnings(WarningQueryDTO query) {
        QueryWrapper<WarningInfo> wrapper = new QueryWrapper<>();

        // 气象站ID
        if (query.getStaId() != null) {
            wrapper.eq("sta_id", query.getStaId());
        }

        // 预警类型
        if (StringUtils.hasText(query.getWarningType())) {
            wrapper.eq("warning_type", query.getWarningType());
        }

        // 预警级别
        if (StringUtils.hasText(query.getWarningLevel())) {
            wrapper.eq("warning_level", query.getWarningLevel());
        }

        // 站号（模糊查询）
        if (StringUtils.hasText(query.getStationCode())) {
            wrapper.like("station_code", query.getStationCode());
        }

        // 路段名称（模糊查询）
        if (StringUtils.hasText(query.getStationName())) {
            wrapper.like("station_name", query.getStationName());
        }

        // 时间范围
        if (query.getStartTime() != null) {
            wrapper.ge("create_time", query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le("create_time", query.getEndTime());
        }

        // 按创建时间倒序
        wrapper.orderByDesc("create_time");

        // 分页查询
        Page<WarningInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<WarningInfo> result = warningInfoMapper.selectPage(page, wrapper);

        return new PageResult<>(
                result.getTotal(),
                result.getRecords(),
                query.getPageNum(),
                query.getPageSize()
        );
    }

    /**
     * 获取预警统计信息
     */
    public WarningStatisticsDTO getStatistics() {
        WarningStatisticsDTO statistics = new WarningStatisticsDTO();

        // 总数
        Long totalCount = warningInfoMapper.selectCount(null);
        statistics.setTotalCount(totalCount);

        if (totalCount == 0) {
            return statistics;
        }

        // 按类型统计
        List<Map<String, Object>> typeStats = warningInfoMapper.selectMaps(
                new QueryWrapper<WarningInfo>()
                        .select("warning_type", "COUNT(*) as count")
                        .groupBy("warning_type")
        );

        Map<String, Long> countByType = new HashMap<>();
        for (Map<String, Object> stat : typeStats) {
            String type = (String) stat.get("warning_type");
            Long count = ((Number) stat.get("count")).longValue();
            countByType.put(type, count);
        }
        statistics.setCountByType(countByType);

        // 按级别统计
        List<Map<String, Object>> levelStats = warningInfoMapper.selectMaps(
                new QueryWrapper<WarningInfo>()
                        .select("warning_level", "COUNT(*) as count")
                        .groupBy("warning_level")
        );

        Map<String, Long> countByLevel = new HashMap<>();
        for (Map<String, Object> stat : levelStats) {
            String level = (String) stat.get("warning_level");
            Long count = ((Number) stat.get("count")).longValue();
            countByLevel.put(level, count);
        }
        statistics.setCountByLevel(countByLevel);

        // 按类型和级别统计
        List<Map<String, Object>> typeAndLevelStats = warningInfoMapper.selectMaps(
                new QueryWrapper<WarningInfo>()
                        .select("warning_type", "warning_level", "COUNT(*) as count")
                        .groupBy("warning_type", "warning_level")
        );

        Map<String, Map<String, Long>> countByTypeAndLevel = new HashMap<>();
        for (Map<String, Object> stat : typeAndLevelStats) {
            String type = (String) stat.get("warning_type");
            String level = (String) stat.get("warning_level");
            Long count = ((Number) stat.get("count")).longValue();

            countByTypeAndLevel.putIfAbsent(type, new HashMap<>());
            countByTypeAndLevel.get(type).put(level, count);
        }
        statistics.setCountByTypeAndLevel(countByTypeAndLevel);

        // 最新更新时间
        WarningInfo latest = warningInfoMapper.selectOne(
                new QueryWrapper<WarningInfo>()
                        .orderByDesc("create_time")
                        .last("LIMIT 1")
        );

        if (latest != null && latest.getCreateTime() != null) {
            statistics.setLastUpdateTime(FORMATTER.format(latest.getCreateTime()));
        }

        return statistics;
    }

    /**
     * 根据ID查询预警详情
     */
    public WarningInfo getWarningById(Long id) {
        return warningInfoMapper.selectById(id);
    }

    /**
     * 获取最新的N条预警
     */
    public List<WarningInfo> getLatestWarnings(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        QueryWrapper<WarningInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time")
                .last("LIMIT " + limit);

        return warningInfoMapper.selectList(wrapper);
    }

    /**
     * 按类型查询预警列表
     */
    public List<WarningInfo> getWarningsByType(String warningType) {
        QueryWrapper<WarningInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_type", warningType)
                .orderByDesc("create_time");

        return warningInfoMapper.selectList(wrapper);
    }

    /**
     * 按级别查询预警列表
     */
    public List<WarningInfo> getWarningsByLevel(String warningLevel) {
        QueryWrapper<WarningInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("warning_level", warningLevel)
                .orderByDesc("create_time");

        return warningInfoMapper.selectList(wrapper);
    }

    /**
     * 统计每条线路的预警情况
     * 逻辑：
     *   1. 查询 geo_line_node，按 roadCode 分组，得到每条线路的节点集合
     *   2. 查询 warning_current_status 当前有效状态，以 stationCode（桩号）为 key 建立索引
     *   3. 遍历每条线路的节点，统计各级别预警节点数
     *   4. 计算最高风险级别（RED > ORANGE > YELLOW）
     */
    public List<RouteWarningStatDTO> getRouteWarningStatistics() {
        // 1. 查所有节点，按 roadCode 分组
        List<GeoLineNode> allNodes = geoLineNodeMapper.selectList(null);
        Map<String, List<GeoLineNode>> nodesByRoad = allNodes.stream()
                .filter(n -> StringUtils.hasText(n.getRoadCode()))
                .collect(Collectors.groupingBy(GeoLineNode::getRoadCode));

        // 2. 查当前有效预警，以 stationCode（桩号）为 key，取最高级别
        //    warning_info 是历史明细，不能用于当前线路颜色统计。
        List<WarningCurrentStatus> allWarnings = currentStatusMapper.selectList(
                new QueryWrapper<WarningCurrentStatus>()
                        .eq("status", 1)
                        .and(w -> w.eq("is_released", 0).or().isNull("is_released"))
                        .isNotNull("warning_level")
        );
        // stationCode -> 最高 warningLevel
        Map<String, String> maxLevelByStation = new HashMap<>();
        for (WarningCurrentStatus w : allWarnings) {
            if (!StringUtils.hasText(w.getStationCode())) continue;
            String existing = maxLevelByStation.get(w.getStationCode());
            if (existing == null || levelRank(w.getWarningLevel()) > levelRank(existing)) {
                maxLevelByStation.put(w.getStationCode(), w.getWarningLevel());
            }
        }

        // 3. 按线路统计
        List<RouteWarningStatDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<GeoLineNode>> entry : nodesByRoad.entrySet()) {
            String roadCode = entry.getKey();
            List<GeoLineNode> nodes = entry.getValue();

            RouteWarningStatDTO stat = new RouteWarningStatDTO();
            stat.setRoadCode(roadCode);
            // 取第一个节点的 roadName
            stat.setRoadName(nodes.get(0).getRoadName());
            stat.setTotalNodes(nodes.size());

            Map<String, Integer> levelCount = new HashMap<>();
            int warningNodes = 0;
            String maxLevel = null;

            for (GeoLineNode node : nodes) {
                String level = maxLevelByStation.get(node.getName());
                if (level == null) continue;
                warningNodes++;
                levelCount.merge(level, 1, Integer::sum);
                if (maxLevel == null || levelRank(level) > levelRank(maxLevel)) {
                    maxLevel = level;
                }
            }

            stat.setLevelCount(levelCount);
            stat.setWarningNodes(warningNodes);
            stat.setMaxLevel(maxLevel);
            result.add(stat);
        }

        // 按最高风险级别降序排列
        result.sort((a, b) -> levelRank(b.getMaxLevel()) - levelRank(a.getMaxLevel()));
        return result;
    }

    /**
     * 根据方案ID查询预警（合并接口）：
     * - 传 pointTime（历史时间）或 startTime/endTime → 查 warning_info 历史明细，只含有预警记录
     * - 否则（不传时间 / 传未来时间）→ 查 warning_current_status 当前状态，含无预警/已解除
     * 统一返回 WarningPlanQueryDTO，不适用的字段为 null。
     */
    public List<WarningPlanQueryDTO> queryWarningsByPlan(Long planId, java.util.Date startTime, java.util.Date endTime, java.util.Date pointTime) {
        // 验证方案是否存在
        WeatherServicePlan plan = weatherServicePlanMapper.selectById(planId);
        if (plan == null) {
            return new ArrayList<>();
        }

        java.util.Date now = new java.util.Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Date currentHourStart = cal.getTime();
        java.util.Date oneHourLater = new java.util.Date(now.getTime() + 60 * 60 * 1000L);
        boolean isHistoryQuery = (pointTime != null && !pointTime.after(oneHourLater) && pointTime.before(currentHourStart))
                || startTime != null || endTime != null;

        log.info("预警查询 planId={} | now={} | pointTime={} | currentHourStart={} | history={}",
                planId,
                FORMATTER.format(now),
                pointTime != null ? FORMATTER.format(pointTime) : "null",
                FORMATTER.format(currentHourStart),
                isHistoryQuery);

        List<WarningPlanQueryDTO> result;
        if (isHistoryQuery) {
            // 历史查询：查 warning_info
            List<WarningInfo> records;
            if (pointTime != null && pointTime.before(oneHourLater)) {
                java.util.Date pointTimeEnd = new java.util.Date(pointTime.getTime() + 60 * 60 * 1000L);
                java.util.Date batchTime = warningInfoMapper.selectLatestBatchTime(planId, pointTime, pointTimeEnd);
                if (batchTime == null) return new ArrayList<>();
                records = warningInfoMapper.selectByPlanId(planId, null, null, batchTime);
            } else {
                records = warningInfoMapper.selectByPlanId(planId, startTime, endTime, null);
            }
            result = records.stream().map(w -> {
                WarningPlanQueryDTO dto = new WarningPlanQueryDTO();
                dto.setId(w.getId());
                dto.setStaId(w.getStaId());
                dto.setGeoId(w.getGeoId());
                dto.setStationCode(w.getStationCode());
                dto.setStationName(w.getStationName());
                dto.setLon(w.getLon());
                dto.setLat(w.getLat());
                dto.setWarningType(w.getWarningType());
                dto.setWarningLevel(w.getWarningLevel());
                dto.setStatus(w.getStatus());
                dto.setActualValue(w.getActualValue());
                dto.setForecastValue(w.getForecastValue());
                dto.setForecast3hValue(w.getForecast3hValue());
                dto.setForecastTime(w.getForecastTime());
                dto.setForecastPeriod(w.getForecastPeriod());
                dto.setBatchTime(w.getCreateTime());
                return dto;
            }).collect(Collectors.toList());
        } else {
            // 当前状态查询：查 warning_current_status，一步到位
            List<WarningCurrentStatus> records = currentStatusMapper.selectByPlanId(planId);
            // 补查 lon/lat（需从 geo_detail 反查）
            List<Long> geoIds = resolveGeoIds(plan);
            Map<String, WeatherServiceGeoDetail> detailMap = new HashMap<>();
            if (geoIds != null) {
                List<WeatherServiceGeoDetail> geoDetails = geoDetailMapper.selectByGeoIds(geoIds);
                if (geoDetails != null) {
                    for (WeatherServiceGeoDetail d : geoDetails) {
                        detailMap.put(d.getGeoId() + "_" + d.getStaId(), d);
                    }
                }
            }
            result = records.stream().map(s -> {
                WarningPlanQueryDTO dto = new WarningPlanQueryDTO();
                dto.setId(s.getId());
                dto.setGeoId(s.getGeoId());
                dto.setStaId(s.getStaId());
                dto.setStationCode(s.getStationCode());
                dto.setStationName(s.getStationName());
                WeatherServiceGeoDetail detail = detailMap.get(s.getGeoId() + "_" + s.getStaId());
                dto.setLon(detail != null ? detail.getLon() : null);
                dto.setLat(detail != null ? detail.getLat() : null);
                dto.setWarningType(s.getWarningType());
                dto.setWarningLevel(s.getWarningLevel());
                dto.setStatus(s.getStatus());
                dto.setNoWarningCount(s.getNoWarningCount() == null ? 0 : s.getNoWarningCount());
                dto.setIsReleased(s.getIsReleased() == null ? 0 : s.getIsReleased());
                dto.setActualValue(s.getActualValue());
                dto.setForecastValue(s.getForecastValue());
                dto.setForecast3hValue(s.getForecast3hValue());
                dto.setForecastTime(s.getForecastTime());
                dto.setForecastPeriod(s.getForecastPeriod());
                dto.setBatchTime(s.getLastBatchTime());
                return dto;
            }).collect(Collectors.toList());
        }
        // 按预警等级排序：RED > ORANGE > YELLOW > null（无预警排最后）
        result.sort((a, b) -> levelRank(b.getWarningLevel()) - levelRank(a.getWarningLevel()));
        return result;
    }

    /**
     * 查当前预警状态（warning_current_status），返回指定 planId 下所有站点最新状态
     * @deprecated 请使用 {@link #queryWarningsByPlan}
     */
    public List<WarningCurrentStatus> queryCurrentStatus(Long planId) {
        return currentStatusMapper.selectByPlanId(planId);
    }

    /**
     * 查历史时间点预警记录（warning_info）
     * @deprecated 请使用 {@link #queryWarningsByPlan}
     */
    public List<WarningInfo> queryHistoryWarnings(Long planId, java.util.Date startTime, java.util.Date endTime, java.util.Date pointTime) {
        java.util.Date now = new java.util.Date();

        if (pointTime != null && !pointTime.after(now)) {
            java.util.Date pointTimeEnd = new java.util.Date(pointTime.getTime() + 60 * 60 * 1000L);
            java.util.Date batchTime = warningInfoMapper.selectLatestBatchTime(planId, pointTime, pointTimeEnd);
            if (batchTime == null) return new ArrayList<>();
            return warningInfoMapper.selectByPlanId(planId, null, null, batchTime);
        }

        if (startTime == null && endTime == null) {
            java.util.Date batchTime = warningInfoMapper.selectLatestBatchTime(planId, null, null);
            if (batchTime == null) return new ArrayList<>();
            return warningInfoMapper.selectByPlanId(planId, null, null, batchTime);
        }

        return warningInfoMapper.selectByPlanId(planId, startTime, endTime, null);
    }

    /** 级别排名：RED=3, ORANGE=2, YELLOW=1, null/其他=0 */
    private int levelRank(String level) {
        if ("RED".equals(level)) return 3;
        if ("ORANGE".equals(level)) return 2;
        if ("YELLOW".equals(level)) return 1;
        return 0;
    }

    /**
     * 根据 plan 的 upPlanId（逗号分隔）查 weather_service_geo 得到 geoId 列表。
     * plan 为 null 或 upPlanId 为空或查不到时返回 null。
     */
    private List<Long> resolveGeoIds(WeatherServicePlan plan) {
        if (plan == null || plan.getUpPlanId() == null || plan.getUpPlanId().trim().isEmpty()) {
            return null;
        }
        List<Long> upPlanIds = Arrays.stream(plan.getUpPlanId().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        List<Long> geoIds = weatherServiceGeoMapper.selectGeoIdsByUpPlanIds(upPlanIds);
        return geoIds.isEmpty() ? null : geoIds;
    }
}
