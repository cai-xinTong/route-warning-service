-- 虚谷数据库建表脚本

-- 1. 路段节点表（3公里抽稀后）
CREATE TABLE geo_line_node (
    id BIGINT NOT NULL PRIMARY KEY,
    object_id BIGINT,
    seq INT,
    name VARCHAR(100),
    code VARCHAR(100),
    lon DOUBLE,
    lat DOUBLE,
    properties VARCHAR(2000),
    company VARCHAR(200),
    road_code VARCHAR(100),
    road_name VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lon_lat ON geo_line_node (lon, lat);
CREATE INDEX idx_road_code ON geo_line_node (road_code);

-- 创建序列
CREATE SEQUENCE seq_geo_line_node START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE geo_line_node IS '路段节点表（3公里抽稀后）';
COMMENT ON COLUMN geo_line_node.id IS '主键ID';
COMMENT ON COLUMN geo_line_node.object_id IS '对象ID';
COMMENT ON COLUMN geo_line_node.seq IS '序号';
COMMENT ON COLUMN geo_line_node.name IS '桩号';
COMMENT ON COLUMN geo_line_node.code IS '编码';
COMMENT ON COLUMN geo_line_node.lon IS '经度';
COMMENT ON COLUMN geo_line_node.lat IS '纬度';
COMMENT ON COLUMN geo_line_node.properties IS '扩展属性';
COMMENT ON COLUMN geo_line_node.company IS '公司';
COMMENT ON COLUMN geo_line_node.road_code IS '路段编号';
COMMENT ON COLUMN geo_line_node.road_name IS '路段名称';
COMMENT ON COLUMN geo_line_node.create_time IS '创建时间';

-- 2. 预警阈值配置表
CREATE TABLE warning_threshold (
    id BIGINT NOT NULL PRIMARY KEY,
    warning_type VARCHAR(50) NOT NULL,
    level_name VARCHAR(20) NOT NULL,
    threshold_value DOUBLE NOT NULL,
    description VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_type_level UNIQUE (warning_type, level_name)
);

CREATE SEQUENCE seq_warning_threshold START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE warning_threshold IS '预警阈值配置表';
COMMENT ON COLUMN warning_threshold.id IS '主键ID';
COMMENT ON COLUMN warning_threshold.warning_type IS '预警类型：RAIN/WIND/VISIBILITY';
COMMENT ON COLUMN warning_threshold.level_name IS '预警级别：YELLOW/ORANGE/RED';
COMMENT ON COLUMN warning_threshold.threshold_value IS '阈值';
COMMENT ON COLUMN warning_threshold.description IS '描述';
COMMENT ON COLUMN warning_threshold.create_time IS '创建时间';
COMMENT ON COLUMN warning_threshold.update_time IS '更新时间';

-- 3. 预警信息表
CREATE TABLE warning_info (
    id BIGINT NOT NULL PRIMARY KEY,
    station_code VARCHAR(100),
    station_name VARCHAR(200),
    lon DOUBLE,
    lat DOUBLE,
    warning_type VARCHAR(50) NOT NULL,
    warning_level VARCHAR(20) NOT NULL,
    actual_value DOUBLE,
    forecast_value DOUBLE,
    forecast_time TIMESTAMP,
    forecast_period INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_type_level ON warning_info (warning_type, warning_level);
CREATE INDEX idx_station ON warning_info (station_code);
CREATE INDEX idx_create_time ON warning_info (create_time);

CREATE SEQUENCE seq_warning_info START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE warning_info IS '预警信息表';
COMMENT ON COLUMN warning_info.id IS '主键ID';
COMMENT ON COLUMN warning_info.station_code IS '站号/桩号';
COMMENT ON COLUMN warning_info.station_name IS '站点名称';
COMMENT ON COLUMN warning_info.lon IS '经度';
COMMENT ON COLUMN warning_info.lat IS '纬度';
COMMENT ON COLUMN warning_info.warning_type IS '预警类型：RAIN/WIND/VISIBILITY';
COMMENT ON COLUMN warning_info.warning_level IS '预警级别：YELLOW/ORANGE/RED';
COMMENT ON COLUMN warning_info.actual_value IS '实况值';
COMMENT ON COLUMN warning_info.forecast_value IS '预报值';
COMMENT ON COLUMN warning_info.forecast_time IS '起报时间';
COMMENT ON COLUMN warning_info.forecast_period IS '预报时效（小时）';
COMMENT ON COLUMN warning_info.create_time IS '创建时间';

-- 初始化阈值数据
-- 暴雨阈值
INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'RAIN', 'YELLOW', 30, '黄色预警：30mm');

INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'RAIN', 'ORANGE', 50, '橙色预警：50mm');

INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'RAIN', 'RED', 70, '红色预警：70mm');

-- 大风阈值
INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'WIND', 'YELLOW', 10.8, '黄色预警：10.8m/s');

INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'WIND', 'ORANGE', 13.9, '橙色预警：13.9m/s');

INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'WIND', 'RED', 17.2, '红色预警：17.2m/s');

-- 能见度阈值
INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'VISIBILITY', 'YELLOW', 1000, '黄色预警：1000m');

INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'VISIBILITY', 'ORANGE', 500, '橙色预警：500m');

INSERT INTO warning_threshold (id, warning_type, level_name, threshold_value, description)
VALUES (seq_warning_threshold.NEXTVAL, 'VISIBILITY', 'RED', 200, '红色预警：200m');
