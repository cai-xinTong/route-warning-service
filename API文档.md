# 预警查询接口文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **返回格式**: JSON
- **字符编码**: UTF-8

## 接口列表

### 1. 分页查询预警信息

**接口地址**: `/warning/query`

**请求方式**: POST

**请求参数**:
```json
{
  "warningType": "RAIN",           // 可选，预警类型：RAIN/WIND/VISIBILITY
  "warningLevel": "YELLOW",        // 可选，预警级别：YELLOW/ORANGE/RED
  "stationCode": "K95",            // 可选，站号/桩号（模糊查询）
  "stationName": "桂河高速",        // 可选，路段名称（模糊查询）
  "startTime": "2026-03-18 00:00:00",  // 可选，开始时间
  "endTime": "2026-03-18 23:59:59",    // 可选，结束时间
  "pageNum": 1,                    // 可选，页码，默认1
  "pageSize": 20                   // 可选，每页大小，默认20
}
```

**返回示例**:
```json
{
  "success": true,
  "data": {
    "total": 100,
    "records": [
      {
        "id": 1,
        "stationCode": "K95+740",
        "stationName": "S22桂河高速(融河路)-罗城管辖",
        "lon": 109.282925,
        "lat": 24.913966,
        "warningType": "RAIN",
        "warningLevel": "YELLOW",
        "actualValue": 35.5,
        "forecastValue": 40.2,
        "forecastTime": "2026-03-18T15:00:00",
        "forecastPeriod": 1,
        "createTime": "2026-03-18T15:00:05"
      }
    ],
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

**使用示例**:
```bash
# 查询所有预警
curl -X POST http://localhost:8080/api/warning/query \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":20}'

# 查询暴雨预警
curl -X POST http://localhost:8080/api/warning/query \
  -H "Content-Type: application/json" \
  -d '{"warningType":"RAIN","pageNum":1,"pageSize":20}'

# 查询红色预警
curl -X POST http://localhost:8080/api/warning/query \
  -H "Content-Type: application/json" \
  -d '{"warningLevel":"RED","pageNum":1,"pageSize":20}'
```

---

### 2. 获取预警统计信息

**接口地址**: `/warning/statistics`

**请求方式**: GET

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
    "lastUpdateTime": "2026-03-18 15:00:05"
  }
}
```

**使用示例**:
```bash
curl http://localhost:8080/api/warning/statistics
```

---

### 3. 根据ID查询预警详情

**接口地址**: `/warning/{id}`

**请求方式**: GET

**路径参数**:
- `id`: 预警ID

**返回示例**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "stationCode": "K95+740",
    "stationName": "S22桂河高速(融河路)-罗城管辖",
    "lon": 109.282925,
    "lat": 24.913966,
    "warningType": "RAIN",
    "warningLevel": "YELLOW",
    "actualValue": 35.5,
    "forecastValue": 40.2,
    "forecastTime": "2026-03-18T15:00:00",
    "forecastPeriod": 1,
    "createTime": "2026-03-18T15:00:05"
  }
}
```

**使用示例**:
```bash
curl http://localhost:8080/api/warning/1
```

---

### 4. 获取最新的N条预警

**接口地址**: `/warning/latest`

**请求方式**: GET

**请求参数**:
- `limit`: 可选，返回数量，默认10

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 150,
      "stationCode": "K95+740",
      "stationName": "S22桂河高速(融河路)-罗城管辖",
      "lon": 109.282925,
      "lat": 24.913966,
      "warningType": "RAIN",
      "warningLevel": "YELLOW",
      "actualValue": 35.5,
      "forecastValue": 40.2,
      "forecastTime": "2026-03-18T15:00:00",
      "forecastPeriod": 1,
      "createTime": "2026-03-18T15:00:05"
    }
  ],
  "total": 10
}
```

**使用示例**:
```bash
# 获取最新10条
curl http://localhost:8080/api/warning/latest

# 获取最新20条
curl http://localhost:8080/api/warning/latest?limit=20
```

---

### 5. 按类型查询预警列表

**接口地址**: `/warning/type/{warningType}`

**请求方式**: GET

**路径参数**:
- `warningType`: 预警类型，可选值：RAIN/WIND/VISIBILITY

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "stationCode": "K95+740",
      "warningType": "RAIN",
      "warningLevel": "YELLOW",
      "actualValue": 35.5,
      "createTime": "2026-03-18T15:00:05"
    }
  ],
  "total": 80
}
```

**使用示例**:
```bash
# 查询暴雨预警
curl http://localhost:8080/api/warning/type/RAIN

# 查询大风预警
curl http://localhost:8080/api/warning/type/WIND

# 查询能见度预警
curl http://localhost:8080/api/warning/type/VISIBILITY
```

---

### 6. 按级别查询预警列表

**接口地址**: `/warning/level/{warningLevel}`

**请求方式**: GET

**路径参数**:
- `warningLevel`: 预警级别，可选值：YELLOW/ORANGE/RED

**返回示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "stationCode": "K95+740",
      "warningType": "RAIN",
      "warningLevel": "YELLOW",
      "actualValue": 35.5,
      "createTime": "2026-03-18T15:00:05"
    }
  ],
  "total": 100
}
```

**使用示例**:
```bash
# 查询黄色预警
curl http://localhost:8080/api/warning/level/YELLOW

# 查询橙色预警
curl http://localhost:8080/api/warning/level/ORANGE

# 查询红色预警
curl http://localhost:8080/api/warning/level/RED
```

---

## 数据字典

### 预警类型 (warningType)
| 值 | 说明 |
|---|---|
| RAIN | 暴雨预警 |
| WIND | 大风预警 |
| VISIBILITY | 能见度预警 |

### 预警级别 (warningLevel)
| 值 | 说明 | 颜色 |
|---|---|---|
| YELLOW | 黄色预警 | 🟡 |
| ORANGE | 橙色预警 | 🟠 |
| RED | 红色预警 | 🔴 |

### 字段说明
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 预警ID |
| stationCode | String | 站号/桩号 |
| stationName | String | 站点名称/路段名称 |
| lon | Double | 经度 |
| lat | Double | 纬度 |
| warningType | String | 预警类型 |
| warningLevel | String | 预警级别 |
| actualValue | Double | 实况值 |
| forecastValue | Double | 预报值 |
| forecastTime | DateTime | 起报时间 |
| forecastPeriod | Integer | 预报时效（小时） |
| createTime | DateTime | 创建时间 |

---

## 错误码

| 错误码 | 说明 |
|---|---|
| success: true | 请求成功 |
| success: false | 请求失败，查看message字段获取错误信息 |

---

## 前端集成示例

### Vue.js 示例

```javascript
// 查询预警列表
async function queryWarnings(params) {
  const response = await fetch('http://localhost:8080/api/warning/query', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(params)
  });
  const result = await response.json();
  return result;
}

// 获取统计信息
async function getStatistics() {
  const response = await fetch('http://localhost:8080/api/warning/statistics');
  const result = await response.json();
  return result;
}

// 使用示例
const warnings = await queryWarnings({
  warningType: 'RAIN',
  pageNum: 1,
  pageSize: 20
});

const stats = await getStatistics();
```

### React 示例

```javascript
import { useState, useEffect } from 'react';

function WarningList() {
  const [warnings, setWarnings] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchWarnings();
  }, []);

  const fetchWarnings = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/warning/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pageNum: 1, pageSize: 20 })
      });
      const result = await response.json();
      if (result.success) {
        setWarnings(result.data.records);
      }
    } catch (error) {
      console.error('获取预警失败', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {loading ? '加载中...' : warnings.map(w => (
        <div key={w.id}>{w.stationName} - {w.warningLevel}</div>
      ))}
    </div>
  );
}
```

### jQuery 示例

```javascript
// 查询预警
$.ajax({
  url: 'http://localhost:8080/api/warning/query',
  type: 'POST',
  contentType: 'application/json',
  data: JSON.stringify({
    warningType: 'RAIN',
    pageNum: 1,
    pageSize: 20
  }),
  success: function(result) {
    if (result.success) {
      console.log('预警列表:', result.data.records);
    }
  }
});

// 获取统计
$.get('http://localhost:8080/api/warning/statistics', function(result) {
  if (result.success) {
    console.log('统计信息:', result.data);
  }
});
```

---

## 注意事项

1. **跨域问题**: 如果前端和后端不在同一域名下，需要配置CORS
2. **时间格式**: 时间字段使用ISO 8601格式（yyyy-MM-ddTHH:mm:ss）
3. **分页**: 页码从1开始，不是从0开始
4. **数据更新**: 预警数据每6分钟全量更新一次
5. **性能**: 建议前端做适当的缓存，避免频繁请求

---

## 测试工具

推荐使用以下工具测试接口：
- **Postman**: 图形化接口测试工具
- **curl**: 命令行工具
- **浏览器开发者工具**: 直接在浏览器中测试GET接口
