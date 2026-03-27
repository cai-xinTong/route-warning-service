-- 为 warning_info 表新增 sta_id 字段（对应 weather_service_geo_detail.staId）
ALTER TABLE warning_info ADD COLUMN sta_id BIGINT;

COMMENT ON COLUMN warning_info.sta_id IS '气象站ID（weather_service_geo_detail.staId）';

-- 为 warning_info 表新增 geo_id 字段（对应 weather_service_geo_detail.geoId）
ALTER TABLE warning_info ADD COLUMN geo_id BIGINT;

COMMENT ON COLUMN warning_info.geo_id IS '地理区域ID（weather_service_geo_detail.geoId）';
