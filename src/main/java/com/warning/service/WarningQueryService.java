package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warning.dto.PageResult;
import com.warning.dto.RouteWarningStatDTO;
import com.warning.dto.WarningQueryDTO;
import com.warning.dto.WarningStatisticsDTO;
import com.warning.entity.GeoLineNode;
import com.warning.entity.WarningInfo;
import com.warning.entity.WeatherServicePlan;
import com.warning.mapper.GeoLineNodeMapper;
import com.warning.mapper.WarningInfoMapper;
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
    private GeoLineNodeMapper geoLineNodeMapper;

    @Resource
    private WeatherServicePlanMapper weatherServicePlanMapper;

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
     *   2. 查询 warning_info 全量（当前快照），以 stationCode（桩号）为 key 建立索引
     *   3. 遍历每条线路的节点，统计各级别预警节点数
     *   4. 计算最高风险级别（RED > ORANGE > YELLOW）
     */
    public List<RouteWarningStatDTO> getRouteWarningStatistics() {
        // 1. 查所有节点，按 roadCode 分组
        List<GeoLineNode> allNodes = geoLineNodeMapper.selectList(null);
        Map<String, List<GeoLineNode>> nodesByRoad = allNodes.stream()
                .filter(n -> StringUtils.hasText(n.getRoadCode()))
                .collect(Collectors.groupingBy(GeoLineNode::getRoadCode));

        // 2. 查所有预警，以 stationCode（桩号）为 key，取最高级别
        //    同一桩号可能有多种预警类型，这里取级别最高的那条
        List<WarningInfo> allWarnings = warningInfoMapper.selectList(null);
        // stationCode -> 最高 warningLevel
        Map<String, String> maxLevelByStation = new HashMap<>();
        for (WarningInfo w : allWarnings) {
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
     * 根据预警方案ID查询对应的预警记录
     * 逻辑：从 plan 表取出 GeoIds → 按 geoId 查 warning_info
     */
    public List<WarningInfo> queryWarningsByPlanId(Long planId) {
        WeatherServicePlan plan = weatherServicePlanMapper.selectById(planId);
        if (plan == null || plan.getGeoIds() == null || plan.getGeoIds().trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> geoIds = Arrays.stream(plan.getGeoIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return warningInfoMapper.selectByGeoIds(geoIds);
    }

    /** 级别排名：RED=3, ORANGE=2, YELLOW=1, null/其他=0 */
    private int levelRank(String level) {
        if ("RED".equals(level)) return 3;
        if ("ORANGE".equals(level)) return 2;
        if ("YELLOW".equals(level)) return 1;
        return 0;
    }
}
