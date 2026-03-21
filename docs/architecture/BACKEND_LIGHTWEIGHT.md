# SyncRime 轻量级后端方案

## 📋 方案对比

### 编程语言选型

| 维度 | Node.js | Python | Rust |
|------|---------|--------|------|
| **性能** | ⭐⭐⭐ 良好 | ⭐⭐ 中等 | ⭐⭐⭐⭐⭐ 极好 |
| **开发效率** | ⭐⭐⭐⭐⭐ 极快 | ⭐⭐⭐⭐⭐ 极快 | ⭐⭐⭐ 中等 |
| **学习曲线** | ⭐⭐⭐⭐ 低 | ⭐⭐⭐⭐⭐ 低 | ⭐⭐ 高 |
| **生态丰富度** | ⭐⭐⭐⭐⭐ 极好 | ⭐⭐⭐⭐⭐ 极好 | ⭐⭐⭐ 中等 |
| **AI 集成** | ⭐⭐⭐⭐ 良好 | ⭐⭐⭐⭐⭐ 极好 | ⭐⭐⭐ 中等 |
| **并发处理** | ⭐⭐⭐⭐ 良好 | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐⭐ 极好 |
| **内存占用** | ⭐⭐⭐⭐ 低 | ⭐⭐⭐⭐ 低 | ⭐⭐⭐⭐⭐ 极低 |
| **调试体验** | ⭐⭐⭐⭐⭐ 极好 | ⭐⭐⭐⭐⭐ 极好 | ⭐⭐⭐ 中等 |

### 推荐方案

**首选：Node.js + Fastify**

理由：
- ✅ 开发效率最高，快速迭代
- ✅ 轻量级，资源占用低
- ✅ 异步 I/O 天然适合 API 服务
- ✅ TypeScript 类型安全
- ✅ 前后端语言统一（可选）
- ✅ 调试方便，生态成熟

**备选：Python + FastAPI**

理由：
- ✅ AI/ML 生态最好
- ✅ FastAPI 性能优秀（异步）
- ✅ 自动生成 API 文档
- ✅ 开发效率高

---

## 🏗️ 技术架构

### 方案 A：Node.js + Fastify（推荐）

```
┌─────────────────────────────────────────────────────────────┐
│                      SyncRime 后端服务                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 Fastify (Node.js)                    │   │
│  │  - 高性能 HTTP 框架 (比 Express 快 2x)               │   │
│  │  - 内置 JSON Schema 验证                            │   │
│  │  - 插件化架构                                       │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │                      │     业务层                    │   │
│  │  ┌────────────┐ ┌────▼────┐ ┌────────────┐         │   │
│  │  │  Auth      │ │  Sync   │ │  AI        │         │   │
│  │  │  Module    │ │  Module │ │  Module    │         │   │
│  │  └────────────┘ └─────────┘ └────────────┘         │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │                      │     数据层                    │   │
│  │  ┌────────────┐ ┌────▼────┐ ┌────────────┐         │   │
│  │  │ SQLite     │ │ Redis   │ │ MeiliSearch│         │   │
│  │  │ (主数据库)  │ │ (可选)  │ │ (搜索)     │         │   │
│  │  └────────────┘ └─────────┘ └────────────┘         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 方案 B：Python + FastAPI

```
┌─────────────────────────────────────────────────────────────┐
│                      SyncRime 后端服务                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 FastAPI (Python)                     │   │
│  │  - 高性能异步框架                                    │   │
│  │  - 自动生成 OpenAPI 文档                            │   │
│  │  - Pydantic 数据验证                                │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │                      │     业务层                    │   │
│  │  ┌────────────┐ ┌────▼────┐ ┌────────────┐         │   │
│  │  │ Auth       │ │ Sync    │ │ AI         │         │   │
│  │  │ Router     │ │ Router  │ │ Router     │         │   │
│  │  └────────────┘ └─────────┘ └────────────┘         │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │                      │     数据层                    │   │
│  │  ┌────────────┐ ┌────▼────┐ ┌────────────┐         │   │
│  │  │ SQLite     │ │ SQLite  │ │ MeiliSearch│         │   │
│  │  │ + SQLAlchemy│ │ (搜索)  │ │ (可选)     │         │   │
│  │  └────────────┘ └─────────┘ └────────────┘         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 轻量级中间件选型

### 数据库

| 方案 | 内存占用 | 适用场景 | 推荐度 |
|------|---------|---------|--------|
| **SQLite** | ~1MB | 单机部署、低并发 | ⭐⭐⭐⭐⭐ |
| **PostgreSQL** | ~50MB | 高并发、复杂查询 | ⭐⭐⭐ |
| **MongoDB** | ~100MB | 文档存储 | ⭐⭐ |

**推荐：SQLite**
- ✅ 零配置、零依赖
- ✅ 单文件存储，备份简单
- ✅ 内存占用极低
- ✅ 性能足够（读 > 写场景）
- ✅ 支持全文搜索 (FTS5)

### 缓存（可选）

| 方案 | 内存占用 | 适用场景 | 推荐度 |
|------|---------|---------|--------|
| **内存缓存** | 可控 | 单机、简单缓存 | ⭐⭐⭐⭐⭐ |
| **Redis** | ~10MB | 分布式、复杂缓存 | ⭐⭐⭐ |
| **Keyv** | 可控 | 轻量级 KV 存储 | ⭐⭐⭐⭐ |

**推荐：内存缓存 / 不使用缓存**
- 初期用户量小，数据库查询足够快
- 需要时再引入 Redis

### 搜索（可选）

| 方案 | 内存占用 | 适用场景 | 推荐度 |
|------|---------|---------|--------|
| **SQLite FTS5** | ~0 | 内置全文搜索 | ⭐⭐⭐⭐⭐ |
| **MeiliSearch** | ~50MB | 快速搜索、中文支持 | ⭐⭐⭐⭐ |
| **Elasticsearch** | ~500MB | 企业级搜索 | ⭐ |

**推荐：SQLite FTS5**
- ✅ 内置功能，零额外依赖
- ✅ 支持中文分词（需配置）
- ✅ 性能足够 MVP 使用

---

## 🚀 Node.js + Fastify 项目结构

```
syncrime-server/
├── src/
│   ├── index.ts              # 入口文件
│   ├── app.ts                # Fastify 应用配置
│   ├── config/
│   │   ├── index.ts          # 配置管理
│   │   └── database.ts       # 数据库配置
│   │
│   ├── modules/
│   │   ├── auth/
│   │   │   ├── routes.ts     # 认证路由
│   │   │   ├── controller.ts # 控制器
│   │   │   ├── service.ts    # 业务逻辑
│   │   │   └── schema.ts     # 请求/响应 Schema
│   │   │
│   │   ├── sync/
│   │   │   ├── routes.ts
│   │   │   ├── controller.ts
│   │   │   └── service.ts
│   │   │
│   │   ├── ai/
│   │   │   ├── routes.ts
│   │   │   ├── controller.ts
│   │   │   └── service.ts    # AI 服务集成
│   │   │
│   │   └── statistics/
│   │       ├── routes.ts
│   │       └── service.ts
│   │
│   ├── database/
│   │   ├── connection.ts     # SQLite 连接
│   │   ├── migrations/       # 数据库迁移
│   │   └── seeds/            # 初始数据
│   │
│   ├── middleware/
│   │   ├── auth.ts           # JWT 验证
│   │   ├── errorHandler.ts   # 错误处理
│   │   └── rateLimit.ts      # 限流
│   │
│   ├── utils/
│   │   ├── crypto.ts         # 加密工具
│   │   ├── logger.ts         # 日志
│   │   └── validators.ts     # 验证器
│   │
│   └── types/
│       └── index.ts          # TypeScript 类型定义
│
├── tests/
│   ├── unit/
│   └── integration/
│
├── package.json
├── tsconfig.json
├── .env.example
└── README.md
```

---

## 📋 核心代码示例

### 入口文件 (src/index.ts)

```typescript
import Fastify from 'fastify'
import sqlite from 'better-sqlite3'
import jwt from '@fastify/jwt'
import cors from '@fastify/cors'

import authRoutes from './modules/auth/routes'
import syncRoutes from './modules/sync/routes'
import aiRoutes from './modules/ai/routes'

const fastify = Fastify({
  logger: true
})

// 注册插件
await fastify.register(jwt, { secret: process.env.JWT_SECRET! })
await fastify.register(cors, { origin: '*' })

// 数据库
const db = sqlite('syncrime.db')
fastify.decorate('db', db)

// 路由
await fastify.register(authRoutes, { prefix: '/api/v1/auth' })
await fastify.register(syncRoutes, { prefix: '/api/v1/sync' })
await fastify.register(aiRoutes, { prefix: '/api/v1/ai' })

// 启动服务
const start = async () => {
  try {
    await fastify.listen({ port: 3000, host: '0.0.0.0' })
    console.log('🚀 Server running on http://localhost:3000')
  } catch (err) {
    fastify.log.error(err)
    process.exit(1)
  }
}

start()
```

### 认证模块 (src/modules/auth/routes.ts)

```typescript
import { FastifyInstance } from 'fastify'
import { registerSchema, loginSchema } from './schema'
import * as controller from './controller'

export default async function authRoutes(fastify: FastifyInstance) {
  // 注册
  fastify.post('/register', {
    schema: registerSchema
  }, controller.register)

  // 登录
  fastify.post('/login', {
    schema: loginSchema
  }, controller.login)

  // 刷新 Token
  fastify.post('/refresh', {
    onRequest: [fastify.authenticate]
  }, controller.refreshToken)
}
```

### 同步服务 (src/modules/sync/service.ts)

```typescript
import { FastifyInstance } from 'fastify'

export class SyncService {
  constructor(private db: any) {}

  async pushRecords(userId: string, records: any[]) {
    const stmt = this.db.prepare(`
      INSERT INTO input_records (id, user_id, content, app, timestamp)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        content = excluded.content,
        sync_status = 'SYNCED'
    `)

    const insertMany = this.db.transaction((records) => {
      for (const record of records) {
        stmt.run(record.id, userId, record.content, record.app, record.timestamp)
      }
    })

    insertMany(records)
    return { syncedCount: records.length }
  }

  async pullRecords(userId: string, lastSyncTime: number) {
    const records = this.db.prepare(`
      SELECT * FROM input_records
      WHERE user_id = ? AND updated_at > ?
      ORDER BY updated_at ASC
      LIMIT 1000
    `).all(userId, lastSyncTime)

    return records
  }
}
```

### AI 服务集成 (src/modules/ai/service.ts)

```typescript
import axios from 'axios'

export class AIService {
  private apiKey: string
  private baseUrl: string

  constructor() {
    this.apiKey = process.env.DASHSCOPE_API_KEY!
    this.baseUrl = 'https://dashscope.aliyuncs.com/api/v1'
  }

  // 智能续写
  async completion(context: string, maxLength: number = 100) {
    const response = await axios.post(
      `${this.baseUrl}/services/aigc/text-generation/generation`,
      {
        model: 'qwen-turbo',
        input: {
          messages: [
            { role: 'system', content: '你是一个智能写作助手，帮助用户续写内容。' },
            { role: 'user', content: `请续写以下内容（不超过${maxLength}字）：\n${context}` }
          ]
        }
      },
      {
        headers: {
          'Authorization': `Bearer ${this.apiKey}`,
          'Content-Type': 'application/json'
        }
      }
    )

    return response.data.output.text
  }

  // 智能摘要
  async summarize(content: string) {
    const response = await axios.post(
      `${this.baseUrl}/services/aigc/text-generation/generation`,
      {
        model: 'qwen-turbo',
        input: {
          messages: [
            { role: 'system', content: '你是一个文本摘要专家，提取关键信息。' },
            { role: 'user', content: `请为以下内容生成摘要：\n${content}` }
          ]
        }
      },
      {
        headers: {
          'Authorization': `Bearer ${this.apiKey}`,
          'Content-Type': 'application/json'
        }
      }
    )

    return response.data.output.text
  }
}
```

---

## 📊 数据库设计 (SQLite)

```sql
-- 用户表
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 输入记录表
CREATE TABLE input_records (
  id INTEGER PRIMARY KEY,
  user_id TEXT NOT NULL,
  session_id INTEGER,
  content TEXT NOT NULL,
  app TEXT,
  timestamp INTEGER NOT NULL,
  category TEXT,
  tags TEXT,  -- JSON 数组
  is_sensitive INTEGER DEFAULT 0,
  sync_status TEXT DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 全文搜索索引
CREATE VIRTUAL TABLE input_records_fts USING fts5(
  content,
  content='input_records',
  content_rowid='id',
  tokenize='unicode61'  -- 支持中文
);

-- 知识剪藏表
CREATE TABLE knowledge_clips (
  id INTEGER PRIMARY KEY,
  user_id TEXT NOT NULL,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  source_url TEXT,
  source_type TEXT,
  category TEXT,
  tags TEXT,
  summary TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 同步记录表
CREATE TABLE sync_records (
  id INTEGER PRIMARY KEY,
  user_id TEXT NOT NULL,
  device_id TEXT NOT NULL,
  sync_type TEXT NOT NULL,  -- PUSH / PULL
  record_count INTEGER DEFAULT 0,
  status TEXT DEFAULT 'PENDING',
  started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 索引
CREATE INDEX idx_input_records_user ON input_records(user_id);
CREATE INDEX idx_input_records_timestamp ON input_records(timestamp);
CREATE INDEX idx_knowledge_clips_user ON knowledge_clips(user_id);
CREATE INDEX idx_sync_records_user ON sync_records(user_id);
```

---

## 📦 依赖列表

### package.json

```json
{
  "name": "syncrime-server",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "test": "vitest",
    "lint": "eslint src/",
    "db:migrate": "tsx scripts/migrate.ts"
  },
  "dependencies": {
    "fastify": "^4.26.0",
    "@fastify/jwt": "^7.2.0",
    "@fastify/cors": "^9.0.0",
    "better-sqlite3": "^9.4.0",
    "axios": "^1.6.0",
    "bcryptjs": "^2.4.3",
    "uuid": "^9.0.0",
    "dotenv": "^16.4.0",
    "pino": "^8.18.0",
    "zod": "^3.22.0"
  },
  "devDependencies": {
    "@types/node": "^20.11.0",
    "@types/better-sqlite3": "^7.6.8",
    "@types/bcryptjs": "^2.4.6",
    "typescript": "^5.3.0",
    "tsx": "^4.7.0",
    "vitest": "^1.2.0",
    "eslint": "^8.56.0"
  }
}
```

---

## 🚀 部署方案

### Dockerfile

```dockerfile
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY dist ./dist
COPY migrations ./migrations

EXPOSE 3000

CMD ["node", "dist/index.js"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  syncrime-server:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - JWT_SECRET=your-secret-key
      - DASHSCOPE_API_KEY=your-api-key
    volumes:
      - ./data:/app/data
    restart: unless-stopped
```

### 服务器要求

| 配置 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 1 核 | 2 核 |
| 内存 | 512MB | 1GB |
| 存储 | 10GB | 20GB |
| 带宽 | 1Mbps | 5Mbps |

**月成本估算：**
- 阿里云 ECS (1核1G): ¥30/月
- 腾讯云轻量 (1核1G): ¥25/月
- **总计：¥25-30/月** (不含 AI API 费用)

---

## 🔄 与原方案对比

| 维度 | Spring Boot 方案 | Node.js + Fastify 方案 |
|------|-----------------|----------------------|
| 启动时间 | ~10s | ~1s |
| 内存占用 | ~200MB | ~50MB |
| 开发效率 | 中等 | 极高 |
| 调试体验 | 一般 | 极好 |
| 学习曲线 | 较陡 | 平缓 |
| 部署复杂度 | 高 | 低 |
| 服务器成本 | ¥200+/月 | ¥30/月 |
| MVP 开发周期 | 4-6 周 | 2-3 周 |

---

## 📅 开发计划

### 第 1 周：基础框架

- [ ] 项目初始化
- [ ] 数据库设计与迁移
- [ ] 用户认证模块
- [ ] 基础 API 框架

### 第 2 周：核心功能

- [ ] 同步服务
- [ ] 输入记录 API
- [ ] AI 服务集成

### 第 3 周：完善与部署

- [ ] 搜索功能
- [ ] 单元测试
- [ ] 部署上线

---

*文档版本：v2.0 (轻量级方案)*
*更新时间：2026-03-21*