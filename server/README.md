# SyncRime Server

轻量级后端服务 - Node.js + Fastify + SQLite

## 快速开始

```bash
# 安装依赖
npm install

# 复制环境变量
cp .env.example .env

# 初始化数据库
npm run db:init

# 开发模式
npm run dev

# 生产模式
npm run build && npm start
```

## API 文档

- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/login` - 用户登录
- `POST /api/v1/sync/push` - 推送数据
- `GET /api/v1/sync/pull` - 拉取数据
- `POST /api/v1/ai/completion` - 智能续写
- `POST /api/v1/ai/summarize` - 智能摘要

## 技术栈

- **框架**: Fastify 4.x
- **数据库**: SQLite (better-sqlite3)
- **认证**: JWT
- **AI**: 通义千问 API

## 项目结构

```
src/
├── index.ts          # 入口
├── app.ts            # 应用配置
├── config/           # 配置
├── modules/          # 业务模块
│   ├── auth/         # 认证
│   ├── sync/         # 同步
│   ├── ai/           # AI 服务
│   ├── records/      # 记录管理
│   └── statistics/   # 统计
├── database/         # 数据库
├── middleware/       # 中间件
└── utils/            # 工具函数
```

## License

MIT