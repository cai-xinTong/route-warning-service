-- 新增：warning_info 补 status 字段（1=有预警）
ALTER TABLE warning_info ADD status INT DEFAULT 1;
UPDATE warning_info SET status = 1 WHERE status IS NULL;

-- 新增：当前预警状态表（select+insert/update 维护，每个站点+类型只有一条记录）
CREATE TABLE warning_current_status (
    id               BIGINT        NOT NULL PRIMARY KEY,
    geo_id           BIGINT        NOT NULL,
    sta_id           BIGINT,
    station_code     VARCHAR(64),
    station_name     VARCHAR(128),
    warning_type     VARCHAR(32)   NOT NULL,
    warning_level    VARCHAR(16),
    status           INT           NOT NULL,
    no_warning_count INT           DEFAULT 0,
    is_released      INT           DEFAULT 0,
    last_batch_time  TIMESTAMP     NOT NULL
);

CREATE SEQUENCE seq_warning_current_status START WITH 1 INCREMENT BY 1;

CREATE INDEX idx_wcs_geo ON warning_current_status (geo_id);
CREATE INDEX idx_wcs_key ON warning_current_status (geo_id, warning_type, station_code);
