# SyncRime API 文档

本文档描述 SyncRime 后端 API 接口规范。

> **注意**: 后端服务目前在开发中（Phase 5），以下 API 为设计稿，实际实现可能有所调整。

---

## 📋 目录

- [概述](#概述)
- [认证](#认证)
- [数据模型](#数据模型)
- [API 端点](#api-端点)
- [错误处理](#错误处理)
- [速率限制](#速率限制)

---

## 概述

### Base URL

```
生产环境：https://api.syncrime.com/v1
开发环境：https://dev-api.syncrime.com/v1
```

### 请求格式

所有请求使用 JSON 格式：

```http
Content-Type: application/json
Accept: application/json
```

### 响应格式

```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": "2026-03-16T00:00:00Z"
}
```

---

## 认证

### OAuth 2.0

SyncRime 使用 OAuth 2.0 进行认证。

#### 获取 Token

```http
POST /auth/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&username={username}&password={password}&client_id={client_id}
```

#### 响应

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "dGhpcyBpcyBhIHJlZnJl..."
}
```

#### 使用 Token

在所有 API 请求中添加 Authorization 头：

```http
Authorization: Bearer {access_token}
```

---

## 数据模型

### User (用户)

```json
{
  "id": "user_123",
  "username": "string",
  "email": "string",
  "created_at": "2026-03-16T00:00:00Z",
  "updated_at": "2026-03-16T00:00:00Z",
  "preferences": {
    "language": "zh-CN",
    "theme": "dark",
    "sync_enabled": true
  }
}
```

### InputSession (输入会话)

```json
{
  "id": "session_123",
  "user_id": "user_123",
  "started_at": "2026-03-16T00:00:00Z",
  "ended_at": "2026-03-16T00:30:00Z",
  "input_count": 150,
  "word_count": 500,
  "language": "zh-CN"
}
```

### InputRecord (输入记录)

```json
{
  "id": "record_123",
  "session_id": "session_123",
  "content": "string",
  "context": {
    "app": "com.tencent.mm",
    "timestamp": "2026-03-16T00:00:00Z"
  },
  "corrections": [],
  "recommendations": []
}
```

### SyncRecord (同步记录)

```json
{
  "id": "sync_123",
  "user_id": "user_123",
  "type": "incremental",
  "status": "completed",
  "started_at": "2026-03-16T00:00:00Z",
  "completed_at": "2026-03-16T00:00:30Z",
  "records_synced": 100
}
```

---

## API 端点

### 认证

#### POST /auth/register

用户注册

```json
// Request
{
  "username": "string",
  "email": "string",
  "password": "string"
}

// Response
{
  "user_id": "user_123",
  "message": "注册成功"
}
```

#### POST /auth/login

用户登录

```json
// Request
{
  "username": "string",
  "password": "string"
}

// Response
{
  "access_token": "string",
  "refresh_token": "string",
  "expires_in": 3600
}
```

#### POST /auth/refresh

刷新 Token

```json
// Request
{
  "refresh_token": "string"
}

// Response
{
  "access_token": "string",
  "expires_in": 3600
}
```

---

### 用户

#### GET /users/me

获取当前用户信息

```http
GET /users/me
Authorization: Bearer {token}
```

#### PUT /users/me

更新用户信息

```json
// Request
{
  "preferences": {
    "language": "en-US",
    "theme": "light"
  }
}
```

#### DELETE /users/me

删除用户账号

---

### 输入会话

#### GET /sessions

获取会话列表

```http
GET /sessions?limit=20&offset=0&start_date=2026-03-01&end_date=2026-03-16
Authorization: Bearer {token}
```

#### GET /sessions/{id}

获取单个会话详情

```http
GET /sessions/session_123
Authorization: Bearer {token}
```

#### POST /sessions

创建新会话

```json
// Request
{
  "started_at": "2026-03-16T00:00:00Z"
}

// Response
{
  "session_id": "session_123"
}
```

#### PUT /sessions/{id}

更新会话

```json
// Request
{
  "ended_at": "2026-03-16T00:30:00Z",
  "input_count": 150
}
```

#### DELETE /sessions/{id}

删除会话

---

### 输入记录

#### GET /sessions/{id}/records

获取会话的输入记录

```http
GET /sessions/session_123/records?limit=50&offset=0
Authorization: Bearer {token}
```

#### POST /sessions/{id}/records

添加输入记录

```json
// Request
{
  "content": "string",
  "context": {
    "app": "com.tencent.mm",
    "timestamp": "2026-03-16T00:00:00Z"
  }
}
```

#### GET /records/{id}

获取单个记录

#### DELETE /records/{id}

删除记录

---

### 同步

#### POST /sync/start

开始同步

```json
// Request
{
  "type": "incremental",
  "last_sync_time": "2026-03-15T00:00:00Z"
}

// Response
{
  "sync_id": "sync_123",
  "status": "processing"
}
```

#### GET /sync/{id}/status

获取同步状态

```http
GET /sync/sync_123/status
Authorization: Bearer {token}
```

#### POST /sync/conflict/resolve

解决同步冲突

```json
// Request
{
  "conflict_id": "conflict_123",
  "resolution": "local" // local | remote | merge
}
```

---

### 智能推荐

#### POST /recommendations

获取智能推荐

```json
// Request
{
  "input": "string",
  "context": {
    "app": "com.tencent.mm",
    "language": "zh-CN"
  }
}

// Response
{
  "recommendations": [
    {
      "text": "推荐内容",
      "score": 0.95,
      "type": "completion"
    }
  ]
}
```

#### POST /recommendations/feedback

提交推荐反馈

```json
// Request
{
  "recommendation_id": "rec_123",
  "accepted": true,
  "modified_text": "string"
}
```

---

### 统计

#### GET /statistics/overview

获取统计概览

```http
GET /statistics/overview?period=7d
Authorization: Bearer {token}
```

#### GET /statistics/language

获取语言使用统计

#### GET /statistics/app

获取应用使用统计

#### GET /statistics/trends

获取趋势数据

---

## 错误处理

### 错误响应格式

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": {}
  },
  "timestamp": "2026-03-16T00:00:00Z"
}
```

### 错误代码

| 代码 | HTTP 状态码 | 描述 |
|------|------------|------|
| `AUTH_REQUIRED` | 401 | 需要认证 |
| `AUTH_INVALID` | 401 | 认证无效 |
| `PERMISSION_DENIED` | 403 | 权限不足 |
| `RESOURCE_NOT_FOUND` | 404 | 资源不存在 |
| `VALIDATION_ERROR` | 400 | 参数验证失败 |
| `RATE_LIMIT_EXCEEDED` | 429 | 超出速率限制 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 |

---

## 速率限制

| 端点 | 限制 |
|------|------|
| 认证相关 | 10 次/分钟 |
| 数据读取 | 100 次/分钟 |
| 数据写入 | 50 次/分钟 |
| 同步相关 | 10 次/分钟 |
| 推荐相关 | 30 次/分钟 |

### 速率限制响应头

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1647408000
```

---

## WebSocket

### 实时同步

```javascript
const ws = new WebSocket('wss://api.syncrime.com/v1/ws');

ws.onopen = () => {
  ws.send(JSON.stringify({
    type: 'auth',
    token: 'Bearer {token}'
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('收到消息:', data);
};
```

### 消息类型

- `sync.update` - 同步更新
- `recommendation.new` - 新推荐
- `notification` - 通知

---

## SDK

### Android (Kotlin)

```kotlin
val client = SyncRimeClient(
    baseUrl = "https://api.syncrime.com/v1",
    token = "your-token"
)

// 获取用户信息
val user = client.users.me()

// 创建会话
val session = client.sessions.create()

// 同步数据
client.sync.start(type = SyncType.INCREMENTAL)
```

---

## 更新日志

- **v1.0.0** (2026-03-16) - 初始版本

---

*API 文档最后更新：2026-03-16*
