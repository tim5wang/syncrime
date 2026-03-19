# SyncRime 简化版架构设计

**版本**: v2.0 (简化版)  
**制定时间**: 2026-03-18  
**架构类型**: 单体应用 (Monolithic)

---

## 1. 架构概述

### 1.1 简化原因

- 单人全栈开发，需要快速验证
- 前期无需复杂微服务
- 本地运行，降低运维成本
- 快速迭代，即时验证

### 1.2 架构目标

**MVP 阶段** (4 周):
- ✅ 输入采集
- ✅ 本地存储
- ✅ 基础搜索
- ✅ 简单统计

**V1.0 阶段** (8 周):
- ✅ AI 功能（续写/对话）
- ✅ 智能搜索
- ✅ 知识库管理
- ✅ 本地同步

---

## 2. 应用架构

```
SyncRime.apk
│
├── 📱 主应用 (app)
│   ├── UI 界面 (Jetpack Compose)
│   ├── 知识库管理
│   ├── 搜索界面
│   └── 设置界面
│
├── ⌨️ 输入法插件 (inputmethod)
│   ├── 输入采集
│   ├── 智能续写
│   ├── AI 对话
│   └── 工具栏
│
├── 🔧 本地服务 (local-service)
│   ├── 数据管理
│   ├── AI 服务代理
│   ├── 搜索服务
│   └── 同步服务
│
└── 📦 共享库 (shared)
    ├── 数据模型
    ├── 工具类
    └── 常量定义
```

---

## 3. 数据架构

### 3.1 本地数据库 (Room)

```
Room Database (SQLite)
├── input_records (输入记录)
├── knowledge_clips (知识剪藏)
├── categories (分类)
├── tags (标签)
└── settings (设置)
```

### 3.2 数据流

```
输入法采集
    ↓
输入记录 → Room 数据库
    ↓
本地服务处理 (分类/标签/摘要)
    ↓
知识库 → 搜索索引
```

---

## 4. 技术栈

### 客户端
- **语言**: Kotlin
- **UI**: Jetpack Compose
- **数据库**: Room (SQLite)
- **依赖注入**: Hilt/Koin
- **异步**: Coroutines + Flow

### AI 服务
- **本地**: 大模型 API (通义/文心)
- **备用**: 本地 ML 模型 (TensorFlow Lite)

### 搜索
- **本地**: SQLite FTS (全文搜索)
- **增强**: 简单语义搜索 (本地向量)

---

## 5. 模块划分

### 5.1 app 模块

**职责**: 主应用界面和功能

**包结构**:
```
com.syncrime.app/
├── presentation/     # UI 层
├── domain/          # 业务逻辑
└── data/            # 数据层
```

### 5.2 inputmethod 模块

**职责**: 输入法插件功能

**包结构**:
```
com.syncrime.inputmethod/
├── core/            # 核心服务
├── intelligence/    # AI 功能
└── ui/              # 输入法 UI
```

### 5.3 local-service 模块

**职责**: 本地后台服务

**包结构**:
```
com.syncrime.local/
├── data/            # 数据管理
├── search/          # 搜索服务
├── ai/              # AI 代理
└── sync/            # 同步服务
```

### 5.4 shared 模块

**职责**: 共享代码和模型

**包结构**:
```
com.syncrime.shared/
├── model/           # 数据模型
├── util/            # 工具类
└── constant/        # 常量
```

---

## 6. 核心功能实现

### 6.1 输入采集

```kotlin
// 无障碍服务
class InputCaptureService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                captureInput(event)
            }
        }
    }
}
```

### 6.2 本地存储

```kotlin
// Room Database
@Database(
    entities = [
        InputRecord::class,
        KnowledgeClip::class,
        Category::class,
        Tag::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inputDao(): InputDao
    abstract fun clipDao(): ClipDao
}
```

### 6.3 智能续写

```kotlin
// AI 服务代理
class AIServiceProxy {
    suspend fun complete(context: String): List<String> {
        // 调用大模型 API
        return llmClient.generate(context)
    }
}
```

### 6.4 搜索功能

```kotlin
// SQLite FTS
@Dao
interface SearchDao {
    @Query("SELECT * FROM input_records WHERE content MATCH :query")
    fun search(query: String): Flow<List<InputRecord>>
}
```

---

## 7. 项目目录结构

```
syncrime/
├── android-client/              # Android 项目
│   ├── app/                     # 主应用模块
│   ├── inputmethod/             # 输入法模块
│   ├── local-service/           # 本地服务模块
│   ├── shared/                  # 共享模块
│   └── build.gradle.kts         # 项目构建配置
│
├── docs/                        # 文档
│   ├── architecture/            # 架构文档
│   ├── api/                     # API 文档
│   └── user-guide/              # 用户指南
│
├── scripts/                     # 脚本工具
│   ├── build.sh
│   └── deploy.sh
│
└── README.md                    # 项目说明
```

---

## 8. 开发计划

### Week 1-2: 基础架构
- [ ] 项目初始化
- [ ] 数据库实现
- [ ] 输入采集 POC
- [ ] 基础 UI 框架

### Week 3-4: MVP 功能
- [ ] 完整输入采集
- [ ] 本地存储
- [ ] 基础搜索
- [ ] 简单统计

### Week 5-6: AI 功能
- [ ] 智能续写
- [ ] AI 对话
- [ ] 智能分类

### Week 7-8: 完善发布
- [ ] 知识库管理
- [ ] 设置界面
- [ ] 性能优化
- [ ] 发布准备

---

## 9. 技术决策记录

### 决策 1: 单体应用架构
- **时间**: 2026-03-18
- **原因**: 单人开发，快速验证
- **影响**: 简化部署，降低复杂度

### 决策 2: 本地优先
- **时间**: 2026-03-18
- **原因**: 降低运维成本，保护隐私
- **影响**: 无需云端服务，数据本地存储

### 决策 3: Room 数据库
- **时间**: 2026-03-18
- **原因**: Android 官方推荐，成熟稳定
- **影响**: 良好的本地数据管理

---

*文档版本：v2.0 (简化版)*  
*最后更新：2026-03-18*
