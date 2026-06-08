-- 站点精细化阈值表：每个站点+要素可配置独立阈值，精确到点/线级别
CREATE TABLE weather_service_detail_threshold (
    id       BIGINT       NOT NULL PRIMARY KEY,
    staId    BIGINT       NOT NULL,
    element  VARCHAR(32)  NOT NULL,
    level    VARCHAR(16)  NOT NULL,
    value    DOUBLE       NOT NULL
);

CREATE SEQUENCE seq_weather_service_detail_threshold START WITH 1 INCREMENT BY 1;

CREATE INDEX idx_wsdt_sta_element ON weather_service_detail_threshold (staId, element);
