# 线路预警服务

## 项目简介
基于SpringBoot的线路预警服务，监测线路沿线的气象要素（暴雨、能见度、风）并生成预警信息。

## 功能特性
- 每6分钟自动更新预警信息
- 支持暴雨、大风、能见度三种预警类型
- 阈值可通过数据库动态配置
- 支持Excel数据导入（10米抽稀到3公里）

## 技术栈
- SpringBoot 2.7.14
- MyBatis Plus 3.5.3.1
- MySQL 8.0
- Druid连接池
- Apache POI

## 快速开始

### 1. 创建数据库
```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

### 2. 修改配置
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/route_warning
    username: root
    password: your_password
```

### 3. 导入Excel数据
启动项目后，调用接口导入路段经纬度数据：
```bash
curl -X POST "http://localhost:8080/api/import/excel?filePath=D:/ideaProjectNew/xianLuYUjing/路段经纬度.xlsx"
```

### 4. 手动触发预警更新（测试用）
```bash
curl -X POST http://localhost:8080/api/warning/update
```

## 接口说明

### 1. 导入Excel数据
- **URL**: `/api/import/excel`
- **方法**: POST
- **参数**: `filePath` - Excel文件绝对路径
- **说明**: 将10米分辨率的路段数据抽稀到3公里并导入数据库

### 2. 手动触发预警更新
- **URL**: `/api/warning/update`
- **方法**: POST
- **说明**: 立即执行一次预警更新任务（正常情况下每6分钟自动执行）

### 3. 健康检查
- **URL**: `/api/health`
- **方法**: GET
- **说明**: 检查服务是否正常运行

## 预警规则

### 暴雨预警
- **黄色**: 实况或预报降水量 > 30mm
- **橙色**: 实况或预报降水量 > 50mm
- **红色**: 实况或预报降水量 > 70mm

### 大风预警
- **黄色**: 实况或预报风速 > 10.8 m/s
- **橙色**: 实况或预报风速 > 13.9 m/s
- **红色**: 实况或预报风速 > 17.2 m/s

### 能见度预警
- **黄色**: 实况或预报能见度 < 1000m
- **橙色**: 实况或预报能见度 < 500m
- **红色**: 实况或预报能见度 < 200m

## 阈值配置
阈值存储在 `warning_threshold` 表中，可以通过SQL直接修改：
```sql
UPDATE warning_threshold
SET threshold_value = 35
WHERE warning_type = 'RAIN' AND level_name = 'YELLOW';
```

修改后需要重启服务或等待下次服务启动时自动加载新阈值。

## 定时任务
系统默认每6分钟执行一次预警更新，定时表达式为：`0 */6 * * * ?`

如需修改执行频率，编辑 `WarningScheduleService.java` 中的 `@Scheduled` 注解。

## 数据库表结构

### geo_line_node（路段节点表）
存储3公里抽稀后的路段节点信息

### warning_threshold（阈值配置表）
存储三种预警类型的阈值配置

### warning_info（预警信息表）
存储当前有效的预警信息（每6分钟全量更新）

## 注意事项
1. 确保网格数据接口可访问（http://172.22.96.169）
2. 首次启动前必须先导入Excel数据
3. 预警表每6分钟全量更新，不保留历史记录
4. 能见度接口返回单位是km，系统会自动转换为m
5. 风速和能见度预报只有未来3h，不是24h/72h

## 日志
日志输出到控制台，级别为INFO，可在 `application.yml` 中调整。

## 开发者
- 项目路径: `D:\ideaProjectNew\xianLuYUjing\route-warning-service`
- 主类: `com.warning.RouteWarningApplication`
