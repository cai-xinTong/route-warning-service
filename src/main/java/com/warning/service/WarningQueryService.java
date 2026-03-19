package com.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warning.dto.PageResult;
import com.warning.dto.WarningQueryDTO;
import com.warning.dto.WarningStatisticsDTO;
import com.warning.entity.WarningInfo;
import com.warning.mapper.WarningInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WarningQueryService {

    @Resource
    private WarningInfoMapper warningInfoMapper;

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询预警信息
     */
    public PageResult<WarningInfo> queryWarnings(WarningQueryDTO query) {
        QueryWrapper<WarningInfo> wrapper = new QueryWrapper<>();

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
}
