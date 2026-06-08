-- warning_info 和 warning_current_status 增加 plan_id，支持方案级隔离
ALTER TABLE warning_info ADD (plan_id BIGINT);
ALTER TABLE warning_current_status ADD (plan_id BIGINT);

-- 新唯一键索引：(plan_id, geo_id, sta_id, warning_type)
-- 注意：历史数据 plan_id 为 NULL，索引仅在 plan_id 非空时生效
CREATE INDEX idx_wcs_plan_key ON warning_current_status (plan_id, geo_id, sta_id, warning_type);
