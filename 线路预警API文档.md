# 路线预警服务接口文档

## 基础信息

- **Base URL**: `http://172.22.96.137/XLYJ`
- **返回格式**: JSON
- **字符编码**: UTF-8
- **预警数据更新频率**: 每10分钟一次（历史数据保留）

---

## 接口列表

### 1. 分页查询预警信息

**POST** `/warning/query`

**请求体**（所有字段均可选）:

| 字段 | 类型 | 说明 |
|---|---|---|
| warningType | String | 预警类型：RAIN / WIND / VISIBILITY |
| warningLevel | String | 预警级别：YELLOW / ORANGE / RED |
| stationCode | String | 桩号，模糊匹配 |
| stationName | String | 路段名称，模糊匹配 |
| startTime | String | 创建时间下限，格式：`yyyy-MM-dd HH:mm:ss` |
| endTime | String | 创建时间上限，格式：`yyyy-MM-dd HH:mm:ss` |
| pageNum | Integer | 页码，默认 1 |
| pageSize | Integer | 每页大小，默认 20 |

**请求示例**:
```json
{
  "warningType": "RAIN",
  "warningLevel": "YELLOW",
  "pageNum": 1,
  "pageSize": 20
}
```

**返回示例**:
```json
{
  "success": true,
  "data": {
    "total": 42,
    "records": [
      {
        "id": 1001,
        "stationCode": "K95+740",
        "stationName": "S22桂河高速(融河路)-罗城管辖",
        "lon": 109.282925,
        "lat": 24.913966,
        "warningType": "RAIN",
        "warningLevel": "YELLOW",
        "actualValue": 35.5,
        "forecastValue": 40.2,
        "forecastTime": "2026-03-23 17:40:00",
        "forecastPeriod": 1,
        "createTime": "2026-03-23 17:40:01"
      }
    ],
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 3
  }
}
```

**失败返回**:
```json
{
  "success": false,
  "message": "查询失败: ..."
}
```

---

### 2. 获取预警统计信息

**GET** `/warning/statistics`

**请求参数**: 无

**返回示例**:
```json
{
  "success": true,
  "data": {
    "totalCount": 150,
    "countByType": {
      "RAIN": 80,
      "WIND": 50,
      "VISIBILITY": 20
    },
    "countByLevel": {
      "YELLOW": 100,
      "ORANGE": 40,
      "RED": 10
    },
    "countByTypeAndLevel": {
      "RAIN": {
        "YELLOW": 50,
        "ORANGE": 25,
        "RED": 5
      },
      "WIND": {
        "YELLOW": 35,
        "ORANGE": 12,
        "RED": 3
      },
      "VISIBILITY": {
        "YELLOW": 15,
        "ORANGE": 3,
        "RED": 2
      }
    },
    "lastUpdateTime": "2026-03-23 17:40:01"
  }
}
```

> 若预警表为空，`totalCount` 为 0，其余统计字段均为空 Map，`lastUpdateTime` 为 null。

---

### 3. 根据ID查询预警详情

**GET** `/warning/{id}`

**路径参数**:
- `id`: 预警记录ID（Long）

**返回示例（存在）**:
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "stationCode": "K95+740",
    "stationName": "S22桂河高速(融河路)-罗城管辖",
    "lon": 109.282925,
    "lat": 24.913966,
    "warningType": "RAIN",
    "warningLevel": "YELLOW",
    "actualValue": 35.5,
    "forecastValue": 40.2,
    "forecastTime": "2026-03-23 17:40:00",
    "forecastPeriod": 1,
    "createTime": "2026-03-23 17:40:01"
  }
}
```

**返回示例（不存在）**:
```json
{
  "success": false,
  "message": "预警信息不存在"
}
```

---

### 4. 获取最新N条预警

**GET** `/warning/latest?limit={limit}`

**Query 参数**:
- `limit`: 返回数量，默认 10

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1050,
      "stationCode": "K102+300",
      "stationName": "S22桂河高速(融河路)-罗城管辖",
      "lon": 109.301234,
      "lat": 24.925000,
      "warningType": "WIND",
      "warningLevel": "ORANGE",
      "actualValue": 18.5,
      "forecastValue": 20.1,
      "forecastTime": "2026-03-23 17:40:00",
      "forecastPeriod": 3,
      "createTime": "2026-03-23 17:40:01"
    }
  ],
  "total": 10
}
```

---

### 5. 按类型查询预警列表

**GET** `/warning/type/{warningType}`

**路径参数**:
- `warningType`: RAIN / WIND / VISIBILITY

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "stationCode": "K95+740",
      "stationName": "S22桂河高速(融河路)-罗城管辖",
      "lon": 109.282925,
      "lat": 24.913966,
      "warningType": "RAIN",
      "warningLevel": "RED",
      "actualValue": 72.3,
      "forecastValue": 80.0,
      "forecastTime": "2026-03-23 17:40:00",
      "forecastPeriod": 1,
      "createTime": "2026-03-23 17:40:01"
    }
  ],
  "total": 80
}
```

---

### 6. 按级别查询预警列表

**GET** `/warning/level/{warningLevel}`

**路径参数**:
- `warningLevel`: YELLOW / ORANGE / RED

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "stationCode": "K95+740",
      "stationName": "S22桂河高速(融河路)-罗城管辖",
      "lon": 109.282925,
      "lat": 24.913966,
      "warningType": "VISIBILITY",
      "warningLevel": "RED",
      "actualValue": 180.0,
      "forecastValue": 150.0,
      "forecastTime": "2026-03-23 17:40:00",
      "forecastPeriod": 3,
      "createTime": "2026-03-23 17:40:01"
    }
  ],
  "total": 12
}
```

---

### 7. 统计各线路预警情况

**GET** `/warning/route/statistics`

**请求参数**: 无

按线路聚合预警节点数，结果按最高风险级别降序排列（RED > ORANGE > YELLOW > 无预警）。

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "roadCode": "S22",
      "roadName": "S22桂河高速(融河路)-罗城管辖",
      "totalNodes": 120,
      "warningNodes": 35,
      "maxLevel": "RED",
      "levelCount": {
        "RED": 5,
        "ORANGE": 12,
        "YELLOW": 18
      }
    },
    {
      "roadCode": "G210",
      "roadName": "G210国道",
      "totalNodes": 98,
      "warningNodes": 10,
      "maxLevel": "YELLOW",
      "levelCount": {
        "YELLOW": 10
      }
    },
    {
      "roadCode": "S301",
      "roadName": "S301省道",
      "totalNodes": 60,
      "warningNodes": 0,
      "maxLevel": null,
      "levelCount": {}
    }
  ],
  "total": 3
}
```

> `levelCount` 中同一节点按最高级别计数（即同一桩号有多种预警类型时，只取最高级别）。无预警节点不计入 `warningNodes`。

---

### 8. 手动触发预警更新

**POST** `/warning/update`

**请求参数**: 无

触发一次预警检测任务（与定时任务逻辑相同），用于调试或验证。

**返回示例**:
```json
{
  "success": true,
  "message": "预警更新任务已触发"
}
```

---

### 9. 查询预警阈值列表

**GET** `/threshold/list?warningType={warningType}&levelName={levelName}`

**Query 参数**（均可选）:

| 字段 | 类型 | 说明 |
|---|---|---|
| warningType | String | 预警类型：RAIN / WIND / VISIBILITY |
| levelName | String | 级别名称：YELLOW / ORANGE / RED |

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "warningType": "RAIN",
      "levelName": "YELLOW",
      "thresholdValue": 25.0,
      "description": "暴雨黄色预警阈值（mm/h）",
      "createTime": "2026-03-01 10:00:00",
      "updateTime": "2026-03-01 10:00:00"
    },
    {
      "id": 2,
      "warningType": "RAIN",
      "levelName": "ORANGE",
      "thresholdValue": 50.0,
      "description": "暴雨橙色预警阈值（mm/h）",
      "createTime": "2026-03-01 10:00:00",
      "updateTime": "2026-03-01 10:00:00"
    },
    {
      "id": 3,
      "warningType": "RAIN",
      "levelName": "RED",
      "thresholdValue": 70.0,
      "description": "暴雨红色预警阈值（mm/h）",
      "createTime": "2026-03-01 10:00:00",
      "updateTime": "2026-03-01 10:00:00"
    }
  ],
  "total": 3
}
```

---

### 10. 更新预警阈值

**PUT** `/threshold/update`

**请求体**: 传入需要更新的字段，`id` 必填，其余字段只更新非 null 的部分。

**请求示例**:
```json
{
  "id": 1,
  "thresholdValue": 30.0,
  "description": "暴雨黄色预警阈值（mm/h）"
}
```

**返回示例（成功）**:
```json
{
  "success": true,
  "message": "更新成功"
}
```

**返回示例（记录不存在或无可更新字段）**:
```json
{
  "success": false,
  "message": "记录不存在或无可更新字段"
}
```

---

### 11. 健康检查

**GET** `/health`

**返回示例**:
```json
{
  "status": "UP",
  "service": "route-warning-service"
}
```

---

## 数据字典

### 预警类型 (warningType)

| 值 | 说明 | 实况数据源 | 预报数据源 | 预报时效 |
|---|---|---|---|---|
| RAIN | 暴雨预警 | HOR-PRE（过去60分钟降水量，mm） | ER01（未来1h降水量，mm） | 1小时 |
| WIND | 大风预警 | HOR-WIN（风速，m/s） | EDA10（预报风速，m/s） | 3小时 |
| VISIBILITY | 能见度预警 | HOR-VIS（能见度，km→转换为m） | VIS（预报能见度，km→转换为m） | 3小时 |

### 预警级别 (warningLevel)

| 值 | 说明 | 触发条件 |
|---|---|---|
| YELLOW | 黄色预警 | 实况或预报值超过黄色阈值 |
| ORANGE | 橙色预警 | 实况或预报值超过橙色阈值 |
| RED | 红色预警 | 实况或预报值超过红色阈值 |

> 能见度预警触发条件相反：实况与预报取最小值，**低于**阈值时触发。

### 预警信息字段说明 (WarningInfo)

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 预警ID，自增 |
| stationCode | String | 桩号（如 K95+740） |
| stationName | String | 所属路段名称 |
| lon | Double | 经度 |
| lat | Double | 纬度 |
| warningType | String | 预警类型 |
| warningLevel | String | 预警级别 |
| actualValue | Double | 实况值（单位见预警类型表） |
| forecastValue | Double | 预报值（单位见预警类型表） |
| forecastTime | String | 起报时间 |
| forecastPeriod | Integer | 预报时效（小时） |
| createTime | String | 本条预警的生成时间 |

---

## 通用返回结构

| 字段 | 类型 | 说明 |
|---|---|---|
| success | Boolean | true=成功，false=失败 |
| data | Object/Array | 业务数据，成功时返回 |
| message | String | 失败时的错误描述 |
| total | Integer | 部分列表接口额外返回总数 |
