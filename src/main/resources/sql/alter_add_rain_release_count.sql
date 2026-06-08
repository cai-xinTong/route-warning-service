-- 新增：warning_current_status 补 rain_release_count 字段（降雨预警解除专用计数器）
ALTER TABLE warning_current_status ADD rain_release_count INT DEFAULT 0;
