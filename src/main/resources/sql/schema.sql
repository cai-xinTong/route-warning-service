-- 创建数据库
CREATE DATABASE IF NOT EXISTS route_warning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE route_warning;

-- 1. 路段节点表（3公里抽稀后）
DROP TABLE IF EXISTS geo_line_node;
CREATE TABLE geo_line_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    object_id BIGINT COMMENT '对象ID',
    seq INT COMMENT '序号',
    name VARCHAR(100) COMMENT '桩号',
    code VARCHAR(100) COMMENT '编码',
    lon DOUBLE COMMENT '经度',
    lat DOUBLE COMMENT '纬度',
    properties JSON COMMENT '扩展属性',
    company VARCHAR(200) COMMENT '公司',
    road_code VARCHAR(100) COMMENT '路段编号',
    road_name VARCHAR(200) COMMENT '路段名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_lon_lat (lon, lat),
    INDEX idx_road_code (road_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路段节点表（3公里抽稀后）';

-- 2. 预警阈值配置表
DROP TABLE IF EXISTS warning_threshold;
CREATE TABLE warning_threshold (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    warning_type VARCHAR(50) NOT NULL COMMENT '预警类型：RAIN/WIND/VISIBILITY',
    level_name VARCHAR(20) NOT NULL COMMENT '预警级别：YELLOW/ORANGE/RED',
    threshold_value DOUBLE NOT NULL COMMENT '阈值',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_level (warning_type, level_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警阈值配置表';

-- 3. 预警信息表
DROP TABLE IF EXISTS warning_info;
CREATE TABLE warning_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    station_code VARCHAR(100) COMMENT '站号/桩号',
    station_name VARCHAR(200) COMMENT '站点名称',
    lon DOUBLE COMMENT '经度',
    lat DOUBLE COMMENT '纬度',
    warning_type VARCHAR(50) NOT NULL COMMENT '预警类型：RAIN/WIND/VISIBILITY',
    warning_level VARCHAR(20) NOT NULL COMMENT '预警级别：YELLOW/ORANGE/RED',
    actual_value DOUBLE COMMENT '实况值',
    forecast_value DOUBLE COMMENT '预报值',
    forecast_time DATETIME COMMENT '起报时间',
    forecast_period INT COMMENT '预报时效（小时）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type_level (warning_type, warning_level),
    INDEX idx_station (station_code),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警信息表';

-- 初始化阈值数据
-- 暴雨阈值
INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('RAIN', 'YELLOW', 30, '黄色预警：30mm');

INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('RAIN', 'ORANGE', 50, '橙色预警：50mm');

INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('RAIN', 'RED', 70, '红色预警：70mm');

-- 大风阈值
INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('WIND', 'YELLOW', 10.8, '黄色预警：10.8m/s');

INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('WIND', 'ORANGE', 13.9, '橙色预警：13.9m/s');

INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('WIND', 'RED', 17.2, '红色预警：17.2m/s');

-- 能见度阈值
INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('VISIBILITY', 'YELLOW', 1000, '黄色预警：1000m');

INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('VISIBILITY', 'ORANGE', 500, '橙色预警：500m');

INSERT INTO warning_threshold (warning_type, level_name, threshold_value, description)
VALUES ('VISIBILITY', 'RED', 200, '红色预警：200m');
