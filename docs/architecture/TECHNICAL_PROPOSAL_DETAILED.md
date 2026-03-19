# SyncRime 个人知识库 Clipper - 详细技术方案

**版本**: v1.0  
**制定时间**: 2026-03-18  
**文档状态**: 待评审  
**负责人**: 王总  
**技术负责人**: TBD  
**预计开发周期**: 12 周

---

## 目录

1. [产品概述](#1-产品概述)
2. [功能规格](#2-功能规格)
3. [系统架构](#3-系统架构)
4. [数据模型](#4-数据模型)
5. [API 设计](#5-api-设计)
6. [安全与隐私](#6-安全与隐私)
7. [部署架构](#7-部署架构)
8. [开发计划](#8-开发计划)
9. [测试策略](#9-测试策略)
10. [监控与日志](#10-监控与日志)
11. [团队与成本](#11-团队与成本)
12. [风险评估](#12-风险评估)
13. [附录](#13-附录)

---

## 1. 产品概述

### 1.1 产品定位

**"输入即剪藏，键盘即入口"**

通过输入法这个最高频入口，实现无感知的知识采集，构建个人知识库。

### 1.2 目标用户

| 用户类型 | 占比 | 核心需求 | 付费意愿 |
|---------|------|---------|---------|
| 知识工作者 | 30% | 信息管理、知识沉淀 | 高 |
| 内容创作者 | 25% | 素材收集、灵感记录 | 高 |
| 学生/学习者 | 25% | 学习笔记、资料整理 | 中 |
| 开发者 | 15% | 代码片段、技术文档 | 高 |
| 其他 | 5% | 日常记录 | 低 |

### 1.3 核心价值主张

| 痛点 | SyncRime 解决方案 | 用户价值 |
|------|----------------|---------|
| 看到好内容，懒得保存 | 输入法一键剪藏 | 省时省力 |
| 保存后找不到 | 智能分类 + 语义搜索 | 快速找到 |
| 知识分散在多个 App | 统一知识库 | 一站式管理 |
| 输入时想不起之前的内容 | 智能推荐相关内容 | 提升创作效率 |
| 手动整理太耗时 | AI 自动分类/打标签/摘要 | 自动化整理 |

### 1.4 竞品分析

| 竞品 | 优势 | 劣势 | SyncRime 差异化 |
|------|------|------|---------------|
| 印象笔记 | 品牌知名、功能完整 | 臃肿、同步慢、贵 | 轻量化、输入场景集成、AI 驱动 |
| Notion | 强大灵活、数据库功能 | 学习成本高、国内访问慢 | 中文优化、自动采集、输入集成 |
| Cubox | 专注剪藏、体验好 | 功能单一、仅内容收藏 | 输入内容采集、知识库 + 创作 |
| Flomo | 轻量、卡片式笔记 | 功能简单、无剪藏 | 自动采集、智能分类、AI 驱动 |
| Readwise | 优秀的内容聚合 | 主要支持英文、订阅制贵 | 中文优化、本地化、价格优势 |

### 1.5 成功指标

#### 产品指标

| 阶段 | MAU | DAU/MAU | 日采集量 | 7 日留存 | 付费转化 |
|------|-----|---------|---------|---------|---------|
| MVP (1 月) | 1,000 | >30% | 1,000 条 | >30% | - |
| 成长 (6 月) | 10,000 | >35% | 10,000 条 | >40% | >3% |
| 成熟 (12 月) | 100,000 | >40% | 100,000 条 | >50% | >5% |

#### 技术指标

| 指标 | 目标 | 测量方法 |
|------|------|---------|
| App 启动时间 | < 2 秒 | Firebase Performance |
| 输入延迟 | < 50ms | 自定义埋点 |
| 搜索响应时间 | < 500ms | API Gateway 日志 |
| 同步成功率 | > 98% | 同步服务监控 |
| Crash 率 | < 0.5% | Firebase Crashlytics |
| API 可用性 | > 99.9% | 云监控 |

---

## 2. 功能规格

### 2.1 功能优先级

#### P0 - 必备功能（4 周）

| 功能 ID | 功能名称 | 描述 | 验收标准 |
|--------|---------|------|---------|
| F001 | 智能脱敏 | 自动识别并过滤敏感信息 | 密码/银行卡/身份证号自动过滤，准确率>95% |
| F002 | 输入采集 | 监听并采集用户输入内容 | 支持主流 App，采集延迟<100ms |
| F003 | 本地存储 | Room 数据库持久化 | 支持 10 万 + 记录，查询<200ms |
| F004 | 基础搜索 | 全文搜索功能 | 支持关键词搜索，响应<500ms |

#### P1 - 核心功能（8 周）

| 功能 ID | 功能名称 | 描述 | 验收标准 |
|--------|---------|------|---------|
| F101 | 智能续写 | AI 辅助写作续写 | 支持 3 种续写风格，准确率>80% |
| F102 | AI 对话模式 | 长按触发 AI 对话 | 响应<3 秒，支持多轮对话 |
| F103 | 语义搜索 | 基于语义的智能搜索 | 支持自然语言查询，相关度>85% |
| F104 | 智能笔记 | 自动分类/打标签/摘要 | 自动分类准确率>85% |
| F105 | 输入云同步 | 多设备云端同步 | 同步延迟<5 分钟，冲突解决率 100% |
| F106 | Clipper 剪藏 | 一键/自动保存内容 | 支持分享菜单，剪藏成功率>98% |
| F107 | 智能表单填充 | 自动填充常用信息 | 支持 10+ 字段类型，填充准确率>95% |

#### P2 - 增强功能（后续迭代）

| 功能 ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F201 | 浏览器插件 | Chrome/Safari 网页剪藏 | P2 |
| F202 | 知识图谱 | 可视化知识关联 | P2 |
| F203 | 团队协作 | 知识共享与协作 | P2 |
| F204 | 开放 API | 第三方集成 | P2 |
| F205 | 语音输入 | 语音转文字 + 采集 | P3 |
| F206 | OCR 识别 | 图片文字提取 | P3 |

### 2.2 功能详细规格

#### F001: 智能脱敏

**用户故事**:
```
作为用户，我希望敏感信息自动被过滤，
这样我的隐私就能得到保护。
```

**功能需求**:
1. 自动识别以下敏感信息：
   - 密码（pattern: `password[:\s]+\S+`）
   - 银行卡号（pattern: `\b\d{16}\b`）
   - 身份证号（pattern: `\b\d{18}[Xx]?\b`）
   - 手机号（pattern: `\b1[3-9]\d{9}\b`）
   - 邮箱（pattern: 标准邮箱正则）
2. 敏感内容自动标记为 `isSensitive=true`
3. 敏感内容仅本地加密存储，不同步到云端
4. 用户可查看和管理敏感内容列表

**技术实现**:
```kotlin
class PrivacyFilter {
    private val sensitivePatterns = listOf(
        Regex("(?i)password[:\\s]+\\S+"),
        Regex("\\b\\d{16}\\b"),
        Regex("\\b\\d{18}[Xx]?\\b"),
        Regex("\\b1[3-9]\\d{9}\\b"),
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
    )
    
    fun isSensitive(content: String): Boolean {
        return sensitivePatterns.any { it.containsMatchIn(content) }
    }
    
    fun filter(content: String): FilteredResult {
        var filtered = content
        var foundSensitive = false
        
        sensitivePatterns.forEach { pattern ->
            if (pattern.containsMatchIn(filtered)) {
                foundSensitive = true
                filtered = pattern.replace(filtered, "[REDACTED]")
            }
        }
        
        return FilteredResult(
            content = filtered,
            isSensitive = foundSensitive
        )
    }
}
```

**验收测试**:
```kotlin
@Test
fun `test password detection`() {
    val filter = PrivacyFilter()
    
    assertTrue(filter.isSensitive("password: 123456"))
    assertTrue(filter.isSensitive("PASSWORD: abcdef"))
    assertFalse(filter.isSensitive("normal text"))
}

@Test
fun `test bank card detection`() {
    val filter = PrivacyFilter()
    
    assertTrue(filter.isSensitive("6222021234567890"))
    assertFalse(filter.isSensitive("123456"))
}
```

---

#### F002: 输入采集

**用户故事**:
```
作为用户，我希望我的输入内容能被自动保存，
这样我就不用手动记录重要信息了。
```

**功能需求**:
1. 通过无障碍服务监听输入事件
2. 支持以下应用：
   - 微信、QQ、钉钉（社交）
   - 微博、知乎、小红书（内容）
   - 备忘录、印象笔记（笔记）
   - 浏览器（搜索）
   - 其他主流 App
3. 采集内容包括：
   - 输入文本
   - 应用包名
   - 时间戳
   - 输入上下文（前后文）
4. 性能要求：
   - 采集延迟 < 100ms
   - 不影响正常输入体验
   - 电池消耗 < 5%/天

**技术实现**:
```kotlin
class InputCaptureService : AccessibilityService() {
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
        }
    }
    
    private fun handleTextChanged(event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = source.text?.toString() ?: return
        
        // 过滤无效内容
        if (text.isBlank() || text.length < 1) return
        
        // 隐私过滤
        val filtered = privacyFilter.filter(text)
        
        // 创建采集记录
        val record = InputRecord(
            content = filtered.content,
            application = event.packageName.toString(),
            timestamp = System.currentTimeMillis(),
            isSensitive = filtered.isSensitive
        )
        
        // 异步存储
        scope.launch {
            repository.saveRecord(record)
        }
    }
}
```

---

#### F101: 智能续写

**用户故事**:
```
作为用户，我希望在我写作时能获得智能续写建议，
这样可以提高我的写作效率。
```

**功能需求**:
1. 支持 3 种续写风格：
   - 正式（邮件/文档）
   - 随意（聊天/社交）
   - 创意（创作/写作）
2. 续写长度可配置：
   - 短句（10-20 字）
   - 中等（20-50 字）
   - 长句（50-100 字）
3. 提供 3 个备选方案
4. 支持 Tab 键快速接受
5. 性能要求：
   - 响应时间 < 2 秒
   - 准确率 > 80%

**技术实现**:
```kotlin
class SmartCompletionEngine(
    private val llmClient: LLMClient,
    private val contextManager: ContextManager
) {
    suspend fun generateCompletions(
        context: String,
        style: CompletionStyle,
        length: CompletionLength
    ): List<Completion> {
        // 构建提示词
        val prompt = buildPrompt(context, style, length)
        
        // 调用大模型
        val response = llmClient.generate(
            prompt = prompt,
            temperature = style.temperature,
            maxTokens = length.maxTokens,
            n = 3 // 3 个备选
        )
        
        // 解析响应
        return response.choices.map { choice ->
            Completion(
                text = choice.text.trim(),
                confidence = calculateConfidence(context, choice.text),
                style = style
            )
        }
    }
    
    private fun buildPrompt(
        context: String,
        style: CompletionStyle,
        length: CompletionLength
    ): String {
        return """
            请根据以下上下文，以${style.description}的风格续写，长度${length.description}。
            提供 3 个不同的续写方案。
            
            上下文：$context
            
            续写：
        """.trimIndent()
    }
}

enum class CompletionStyle(val description: String, val temperature: Double) {
    FORMAL("正式", 0.5),
    CASUAL("随意", 0.8),
    CREATIVE("创意", 1.0)
}

enum class CompletionLength(val description: String, val maxTokens: Int) {
    SHORT("短句", 20),
    MEDIUM("中等", 50),
    LONG("长句", 100)
}
```

---

#### F106: Clipper 剪藏

**用户故事**:
```
作为用户，我希望看到好内容时能快速保存，
这样以后需要时就能找到。
```

**功能需求**:
1. 支持多种剪藏方式：
   - 输入法工具栏按钮
   - 系统分享菜单
   - 快捷指令
   - 自动识别（可选）
2. 剪藏内容处理：
   - 自动提取标题
   - 自动提取正文
   - 自动提取图片
   - 保留来源 URL
3. 智能处理：
   - 自动分类
   - 自动打标签
   - 自动生成摘要
4. 支持的内容类型：
   - 文章
   - 图片
   - 视频链接
   - 代码片段

**技术实现**:
```kotlin
class ClipperManager(
    private val contentExtractor: ContentExtractor,
    private val aiService: AIService,
    private val repository: ClipRepository
) {
    suspend fun clip(
        url: String,
        title: String? = null,
        content: String? = null
    ): KnowledgeClip {
        // 1. 提取内容
        val extracted = if (url.isNotEmpty()) {
            contentExtractor.extract(url)
        } else {
            ExtractedContent(
                title = title ?: "无标题",
                content = content ?: "",
                images = emptyList()
            )
        }
        
        // 2. AI 处理
        val aiResult = aiService.processContent(
            title = extracted.title,
            content = extracted.content
        )
        
        // 3. 创建剪藏
        val clip = KnowledgeClip(
            title = extracted.title,
            content = extracted.content,
            sourceUrl = url,
            sourceType = SourceType.CLIP,
            category = aiResult.category,
            tags = aiResult.tags,
            summary = aiResult.summary,
            images = extracted.images,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // 4. 保存
        return repository.save(clip)
    }
}
```

---

## 3. 系统架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端层                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  输入法插件   │  │  独立 App     │  │  浏览器插件   │          │
│  │  Android     │  │  Android/iOS │  │  Web         │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                  │
│         └──────────────────┼──────────────────┘                  │
│                            │                                     │
│                  ┌─────────▼────────┐                            │
│                  │   本地服务层      │                            │
│                  │  - SyncRime Mgr  │                            │
│                  │  - 本地数据库     │                            │
│                  │  - 缓存管理       │                            │
│                  └──────────────────┘                            │
│                            │                                     │
└────────────────────────────┼─────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │    云端服务层     │
                    ├─────────────────┤
                    │  - API Gateway  │
                    │  - 认证服务      │
                    │  - 业务服务      │
                    │  - AI 服务       │
                    │  - 搜索服务      │
                    └─────────────────┘
```

### 3.2 客户端架构

#### 输入法插件模块

```
com.syncrime.inputmethod/
├── core/                          # 核心模块
│   ├── SyncRimeInputService.kt    # 输入法服务
│   ├── InputCollector.kt          # 输入采集器
│   ├── PrivacyFilter.kt           # 隐私过滤器
│   └── Constants.kt               # 常量定义
│
├── intelligence/                  # AI 智能模块
│   ├── SmartCompletion.kt         # 智能续写
│   ├── AIDialog.kt                # AI 对话
│   ├── ContentAnalyzer.kt         # 内容分析
│   └── models/                    # AI 模型
│
├── storage/                       # 存储模块
│   ├── AppDatabase.kt             # 数据库
│   ├── dao/                       # DAO 接口
│   ├── entity/                    # 实体类
│   ├── SyncManager.kt             # 同步管理器
│   └── CacheManager.kt            # 缓存管理
│
├── ui/                            # UI 模块
│   ├── toolbar/                   # 工具栏
│   ├── dialog/                    # 对话框
│   ├── popup/                     # 弹窗
│   └── theme/                     # 主题
│
├── network/                       # 网络模块
│   ├── ApiClient.kt               # API 客户端
│   ├── ApiService.kt              # API 接口
│   └── model/                     # 网络模型
│
└── util/                          # 工具模块
    ├── Encryption.kt              # 加密工具
    ├── Logger.kt                  # 日志工具
    ├── Analytics.kt               # 分析工具
    └── Extensions.kt              # 扩展函数
```

#### 独立 App 模块

```
com.syncrime.app/
├── presentation/                  # UI 层
│   ├── MainActivity.kt
│   ├── SyncRimeApp.kt
│   ├── screens/
│   │   ├── home/                  # 首页
│   │   ├── library/               # 知识库
│   │   ├── search/                # 搜索
│   │   ├── settings/              # 设置
│   │   └── profile/               # 个人
│   ├── components/                # 通用组件
│   └── theme/                     # 主题系统
│
├── domain/                        # 业务逻辑层
│   ├── model/                     # 数据模型
│   ├── usecase/                   # 用例
│   │   ├── CaptureInputUseCase.kt
│   │   ├── SearchClipsUseCase.kt
│   │   ├── SyncDataUseCase.kt
│   │   └── ...
│   └── repository/                # 仓库接口
│       ├── ClipRepository.kt
│       ├── SyncRepository.kt
│       └── ...
│
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── dao/                   # DAO
│   │   ├── entity/                # 实体
│   │   └── LocalDataSource.kt
│   ├── remote/                    # 远程数据源
│   │   ├── api/                   # API
│   │   ├── dto/                   # DTO
│   │   └── RemoteDataSource.kt
│   └── repository/                # 仓库实现
│       ├── ClipRepositoryImpl.kt
│       └── ...
│
└── di/                            # 依赖注入
    ├── AppModule.kt
    ├── DatabaseModule.kt
    ├── NetworkModule.kt
    └── RepositoryModule.kt
```

### 3.3 服务端架构

#### 微服务划分

```
┌─────────────────────────────────────────────────────────┐
│                     API Gateway                          │
│                   (Kong / Nginx)                         │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┬────────────┐
        │            │            │            │
┌───────▼───────┐ ┌─▼─────────┐ ┌─▼─────────┐ ┌─▼─────────┐
│  认证服务      │ │ 用户服务   │ │ 数据服务   │ │ AI 服务     │
│  Auth Service  │ │ User Svc  │ │ Data Svc  │ │ AI Service │
│  :8081         │ │ :8082     │ │ :8083     │ │ :8084      │
└───────┬───────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
        │               │             │             │
        │        ┌──────▼──────┐     │             │
        │        │  PostgreSQL  │     │             │
        │        │  (主数据库)   │     │             │
        │        └─────────────┘     │             │
        │               │             │             │
        │        ┌──────▼──────┐     │             │
        │        │    Redis     │     │             │
        │        │   (缓存)     │     │             │
        │        └─────────────┘     │             │
        │                           │             │
┌───────▼───────┐           ┌───────▼─────┐ ┌───▼─────────┐
│ Elasticsearch  │           │   OSS/COS    │ │  大模型 API  │
│  (搜索服务)    │           │  (文件存储)  │ │ (通义/文心) │
└───────────────┘           └─────────────┘ └─────────────┘
```

#### 服务职责

| 服务 | 职责 | 技术栈 | 端口 |
|------|------|--------|------|
| API Gateway | 路由、限流、认证 | Kong/Nginx | 80/443 |
| 认证服务 | 用户认证、JWT 签发 | Kotlin + Spring Boot | 8081 |
| 用户服务 | 用户管理、设备管理 | Kotlin + Spring Boot | 8082 |
| 数据服务 | 数据存储、同步 | Kotlin + Spring Boot | 8083 |
| AI 服务 | AI 调用、缓存 | Kotlin + Spring Boot | 8084 |
| 搜索服务 | 全文搜索、语义搜索 | Elasticsearch | 9200 |

---

## 4. 数据模型

### 4.1 数据库 ER 图

```
┌─────────────────────┐      ┌─────────────────────┐
│     users           │      │     devices         │
├─────────────────────┤      ├─────────────────────┤
│ id (PK)             │◀────┤ id (PK)             │
│ email               │      │ user_id (FK)        │
│ password_hash       │      │ device_name         │
│ created_at          │      │ device_type         │
│ updated_at          │      │ public_key          │
│ last_login_at       │      │ created_at          │
└─────────────────────┘      └─────────────────────┘
         │
         │
         │
┌────────▼─────────────┐      ┌─────────────────────┐
│    input_records     │      │   knowledge_clips   │
├──────────────────────┤      ├─────────────────────┤
│ id (PK)              │      │ id (PK)             │
│ user_id (FK)         │      │ user_id (FK)        │
│ session_id           │      │ title               │
│ content              │      │ content             │
│ application          │      │ source_url          │
│ category             │      │ source_type         │
│ tags                 │      │ category            │
│ summary              │      │ tags                │
│ is_sensitive         │      │ summary             │
│ is_encrypted         │      │ images              │
│ visibility           │      │ attachments         │
│ sync_status          │      │ view_count          │
│ sync_time            │      │ favorite_count      │
│ device_id (FK)       │      │ created_at          │
│ created_at           │      │ updated_at          │
│ updated_at           │      │ sync_status         │
└──────────────────────┘      └─────────────────────┘
         │
         │
┌────────▼─────────────┐      ┌─────────────────────┐
│  smart_suggestions   │      │      categories     │
├──────────────────────┤      ├─────────────────────┤
│ id (PK)              │      │ id (PK)             │
│ user_id (FK)         │      │ user_id (FK)        │
│ context              │      │ name                │
│ suggestion           │      │ color               │
│ type                 │      │ icon                │
│ confidence           │      │ created_at          │
│ source_record_ids    │      │ updated_at          │
│ is_accepted          │      └─────────────────────┘
│ created_at           │
└──────────────────────┘
```

### 4.2 核心表结构

#### users 表

```sql
CREATE TABLE users (
    id              VARCHAR(64) PRIMARY KEY,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(500),
    subscription    VARCHAR(50) DEFAULT 'free',
    storage_quota   BIGINT DEFAULT 1073741824,  -- 1GB
    storage_used    BIGINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_created_at (created_at)
);
```

#### input_records 表

```sql
CREATE TABLE input_records (
    id              BIGINT PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL,
    session_id      BIGINT NOT NULL,
    device_id       VARCHAR(64),
    content         TEXT NOT NULL,
    application     VARCHAR(255) NOT NULL,
    category        VARCHAR(100),
    tags            JSON,
    summary         TEXT,
    is_sensitive    BOOLEAN DEFAULT FALSE,
    is_encrypted    BOOLEAN DEFAULT FALSE,
    visibility      VARCHAR(20) DEFAULT 'private',
    sync_status     VARCHAR(20) DEFAULT 'pending',
    sync_time       TIMESTAMP,
    metadata        JSON,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_application (application),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at),
    INDEX idx_sync_status (sync_status),
    FULLTEXT INDEX idx_content (content)
);
```

#### knowledge_clips 表

```sql
CREATE TABLE knowledge_clips (
    id              BIGINT PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    content         TEXT NOT NULL,
    source_url      VARCHAR(1000),
    source_type     VARCHAR(20) NOT NULL,
    category        VARCHAR(100),
    tags            JSON,
    summary         TEXT,
    images          JSON,
    attachments     JSON,
    view_count      INT DEFAULT 0,
    favorite_count  INT DEFAULT 0,
    reminder_at     TIMESTAMP,
    sync_status     VARCHAR(20) DEFAULT 'pending',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_source_type (source_type),
    INDEX idx_created_at (created_at),
    INDEX idx_favorite_count (favorite_count),
    FULLTEXT INDEX idx_title_content (title, content)
);
```

### 4.3 Room Entity 定义

```kotlin
@Entity(tableName = "input_records")
data class InputRecordEntity(
    @PrimaryKey
    val id: Long,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    
    @ColumnInfo(name = "device_id")
    val deviceId: String?,
    
    @ColumnInfo(name = "content", type = ColumnInfo.TEXT)
    val content: String,
    
    @ColumnInfo(name = "application")
    val application: String,
    
    @ColumnInfo(name = "category")
    val category: String?,
    
    @ColumnInfo(name = "tags")
    @TypeConverters(Converters::class)
    val tags: List<String>,
    
    @ColumnInfo(name = "summary", type = ColumnInfo.TEXT)
    val summary: String?,
    
    @ColumnInfo(name = "is_sensitive")
    val isSensitive: Boolean = false,
    
    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = false,
    
    @ColumnInfo(name = "visibility")
    @Enumerated(EnumType.STRING)
    val visibility: Visibility = Visibility.PRIVATE,
    
    @ColumnInfo(name = "sync_status")
    @Enumerated(EnumType.STRING)
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    @ColumnInfo(name = "sync_time")
    val syncTime: Long?,
    
    @ColumnInfo(name = "metadata")
    @TypeConverters(Converters::class)
    val metadata: Map<String, String>,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

enum class Visibility {
    PRIVATE,
    SYNCED,
    SHARED
}

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}
```

---

## 5. API 设计

### 5.1 API 规范

**Base URL**: `https://api.syncrime.com/v1`

**认证方式**: JWT Bearer Token

**请求头**:
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
X-Device-Id: <device_id>
X-App-Version: 1.0.0
```

**响应格式**:
```json
{
  "success": true,
  "data": { ... },
  "message": "操作成功",
  "timestamp": 1710777600000
}
```

**错误响应**:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "参数验证失败",
    "details": [
      {
        "field": "email",
        "message": "邮箱格式不正确"
      }
    ]
  },
  "timestamp": 1710777600000
}
```

### 5.2 认证 API

#### 5.2.1 用户注册

```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "display_name": "张三",
  "device_info": {
    "device_id": "xxx-xxx-xxx",
    "device_name": "iPhone 15 Pro",
    "device_type": "PHONE",
    "os_version": "iOS 17.0",
    "app_version": "1.0.0"
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "user_id": "usr_xxx",
    "email": "user@example.com",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "expires_in": 2592000,
    "subscription": "free",
    "storage_quota": 1073741824
  },
  "timestamp": 1710777600000
}
```

#### 5.2.2 用户登录

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "device_info": {
    "device_id": "xxx-xxx-xxx",
    "device_name": "iPhone 15 Pro",
    "device_type": "PHONE"
  }
}
```

**响应**: 同注册

#### 5.2.3 刷新 Token

```http
POST /auth/refresh
Content-Type: application/json
Authorization: Bearer <refresh_token>

{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 2592000
  },
  "timestamp": 1710777600000
}
```

### 5.3 数据同步 API

#### 5.3.1 推送数据

```http
POST /sync/push
Content-Type: application/json
Authorization: Bearer <token>

{
  "device_id": "xxx-xxx-xxx",
  "last_sync_time": 1710777600000,
  "records": [
    {
      "id": 1710777600000,
      "type": "INPUT_RECORD",
      "data": {
        "content": "输入内容",
        "application": "com.tencent.mm",
        "category": "聊天",
        "tags": ["重要"],
        "is_sensitive": false
      },
      "timestamp": 1710777600000,
      "signature": "device_signature"
    }
  ]
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "synced_count": 10,
    "failed_count": 0,
    "conflicts": [],
    "next_sync_time": 1710777900000,
    "server_time": 1710777600000
  },
  "timestamp": 1710777600000
}
```

#### 5.3.2 拉取数据

```http
GET /sync/pull?since=1710777600000&limit=100
Authorization: Bearer <token>
```

**响应**:
```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1710777600000,
        "type": "INPUT_RECORD",
        "data": {
          "content": "输入内容",
          "application": "com.tencent.mm",
          "category": "聊天"
        },
        "timestamp": 1710777600000
      }
    ],
    "last_sync_time": 1710777900000,
    "has_more": false
  },
  "timestamp": 1710777600000
}
```

### 5.4 知识库 API

#### 5.4.1 获取剪藏列表

```http
GET /library/clips?page=1&size=20&category=技术&tag=AI
Authorization: Bearer <token>
```

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，默认 1 |
| size | int | 每页数量，默认 20，最大 100 |
| category | string | 分类过滤 |
| tag | string | 标签过滤 |
| search | string | 关键词搜索 |
| sort_by | string | 排序字段：created_at/updated_at/title |
| order | string | 排序：asc/desc |

**响应**:
```json
{
  "success": true,
  "data": {
    "clips": [
      {
        "id": 1710777600000,
        "title": "文章标题",
        "summary": "简短摘要...",
        "category": "技术",
        "tags": ["AI", "知识库"],
        "source_url": "https://...",
        "source_type": "CLIP",
        "view_count": 10,
        "favorite_count": 5,
        "created_at": 1710777600000,
        "updated_at": 1710777600000
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "total_pages": 5
    }
  },
  "timestamp": 1710777600000
}
```

#### 5.4.2 创建剪藏

```http
POST /library/clips
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "文章标题",
  "content": "文章内容...",
  "source_url": "https://example.com/article",
  "source_type": "CLIP",
  "category": "技术",
  "tags": ["AI", "知识库"],
  "images": ["https://..."],
  "attachments": []
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1710777600000,
    "title": "文章标题",
    "created_at": 1710777600000
  },
  "timestamp": 1710777600000
}
```

#### 5.4.3 更新剪藏

```http
PUT /library/clips/{id}
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "更新后的标题",
  "category": "更新后的分类",
  "tags": ["新标签 1", "新标签 2"],
  "is_favorite": true
}
```

#### 5.4.4 删除剪藏

```http
DELETE /library/clips/{id}
Authorization: Bearer <token>
```

**响应**:
```json
{
  "success": true,
  "message": "删除成功",
  "timestamp": 1710777600000
}
```

### 5.5 搜索 API

#### 5.5.1 全文搜索

```http
POST /search/query
Content-Type: application/json
Authorization: Bearer <token>

{
  "query": "AI 知识库",
  "filters": {
    "categories": ["技术"],
    "tags": ["AI"],
    "source_types": ["CLIP", "INPUT"],
    "date_range": {
      "from": 1710777600000,
      "to": 1710864000000
    }
  },
  "sort_by": "relevance",
  "order": "desc",
  "page": 1,
  "size": 20
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "results": [
      {
        "id": 1710777600000,
        "type": "CLIP",
        "title": "AI 知识库入门",
        "excerpt": "本文介绍如何使用 AI 构建个人知识库...",
        "score": 0.95,
        "highlights": ["<em>AI</em> <em>知识库</em>"],
        "category": "技术",
        "tags": ["AI", "知识库"],
        "created_at": 1710777600000
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 50,
      "total_pages": 3
    },
    "suggestions": ["AI 技术", "知识管理", "个人知识库"],
    "aggregations": {
      "categories": [
        {"name": "技术", "count": 30},
        {"name": "学习", "count": 20}
      ],
      "tags": [
        {"name": "AI", "count": 25},
        {"name": "知识库", "count": 20}
      ]
    }
  },
  "timestamp": 1710777600000
}
```

### 5.6 AI 服务 API

#### 5.6.1 智能续写

```http
POST /ai/complete
Content-Type: application/json
Authorization: Bearer <token>

{
  "context": "我正在写一封邮件，关于项目进度汇报...",
  "style": "FORMAL",
  "length": "MEDIUM",
  "options": {
    "temperature": 0.7,
    "max_tokens": 100
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "completions": [
      {
        "text": "尊敬的领导，您好！现将项目进展情况汇报如下：...",
        "confidence": 0.85
      },
      {
        "text": "您好！关于本项目的最新进展，特此向您汇报：...",
        "confidence": 0.78
      },
      {
        "text": "领导好，项目目前进展顺利，具体情况如下：...",
        "confidence": 0.72
      }
    ],
    "style": "FORMAL",
    "processing_time_ms": 1200
  },
  "timestamp": 1710777600000
}
```

#### 5.6.2 内容摘要

```http
POST /ai/summarize
Content-Type: application/json
Authorization: Bearer <token>

{
  "content": "长文本内容...",
  "options": {
    "max_length": 200,
    "language": "zh",
    "style": "concise"
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "summary": "简短摘要...",
    "keywords": ["关键词 1", "关键词 2", "关键词 3"],
    "category": "技术",
    "processing_time_ms": 800
  },
  "timestamp": 1710777600000
}
```

#### 5.6.3 智能分类

```http
POST /ai/classify
Content-Type: application/json
Authorization: Bearer <token>

{
  "content": "文本内容...",
  "categories": ["技术", "生活", "工作", "学习", "其他"]
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "category": "技术",
    "confidence": 0.92,
    "all_scores": {
      "技术": 0.92,
      "工作": 0.65,
      "学习": 0.45,
      "生活": 0.12,
      "其他": 0.05
    },
    "tags": ["AI", "技术", "编程"],
    "processing_time_ms": 500
  },
  "timestamp": 1710777600000
}
```

---

## 6. 安全与隐私

### 6.1 数据加密

#### 6.1.1 本地加密

**方案**: AES-256-GCM + Android Keystore

```kotlin
class LocalEncryption(private val context: Context) {
    
    private val keyStore: KeyStore
    private val secretKey: SecretKey
    
    init {
        keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        secretKey = getOrCreateKey()
    }
    
    private fun getOrCreateKey(): SecretKey {
        val existingKey = getSecretKey(KEY_ALIAS)
        if (existingKey != null) {
            return existingKey
        }
        
        // 生成新密钥
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
    
    fun encrypt(data: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        return EncryptedData(
            ciphertext = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }
    
    fun decrypt(encryptedData: EncryptedData): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, "AES")
        val gcmParameterSpec = GCMParameterSpec(
            GCM_TAG_BIT_LENGTH,
            Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        )
        
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec)
        val decrypted = cipher.doFinal(
            Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)
        )
        
        return String(decrypted, Charsets.UTF_8)
    }
    
    companion object {
        private const val KEY_ALIAS = "syncrime_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BIT_LENGTH = 128
    }
}

data class EncryptedData(
    val ciphertext: String,
    val iv: String
)
```

#### 6.1.2 云端加密

**方案**: RSA-OAEP + AES-256 混合加密

```kotlin
class EndToEndEncryption {
    
    private val keyPair: KeyPair
    
    init {
        keyPair = generateKeyPair()
    }
    
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }
    
    fun encryptForCloud(data: String, recipientPublicKey: PublicKey): String {
        // 生成随机 AES 密钥
        val aesKey = generateAesKey()
        
        // 使用 AES 加密数据
        val encryptedData = aesEncrypt(data, aesKey)
        
        // 使用 RSA 加密 AES 密钥
        val encryptedKey = rsaEncrypt(aesKey.encoded, recipientPublicKey)
        
        // 组合结果
        return EncryptedPackage(
            encryptedData = Base64.encodeToString(encryptedData, Base64.NO_WRAP),
            encryptedKey = Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
            iv = Base64.encodeToString(aesKey.iv, Base64.NO_WRAP)
        ).toJson()
    }
    
    fun decryptFromCloud(encryptedPackage: String): String {
        val package = EncryptedPackage.fromJson(encryptedPackage)
        
        // 使用 RSA 解密 AES 密钥
        val decryptedKey = rsaDecrypt(
            Base64.decode(package.encryptedKey, Base64.NO_WRAP),
            keyPair.private
        )
        
        // 使用 AES 解密数据
        val decryptedData = aesDecrypt(
            Base64.decode(package.encryptedData, Base64.NO_WRAP),
            decryptedKey,
            Base64.decode(package.iv, Base64.NO_WRAP)
        )
        
        return String(decryptedData, Charsets.UTF_8)
    }
}

data class EncryptedPackage(
    val encryptedData: String,
    val encryptedKey: String,
    val iv: String
) {
    fun toJson(): String = """
        {
            "encrypted_data": "$encryptedData",
            "encrypted_key": "$encryptedKey",
            "iv": "$iv"
        }
    """.trimIndent()
    
    companion object {
        fun fromJson(json: String): EncryptedPackage {
            // JSON 解析实现
        }
    }
}
```

### 6.2 隐私保护

#### 6.2.1 数据分级

| 级别 | 内容示例 | 存储方式 | 同步策略 | 用户控制 |
|------|---------|---------|---------|---------|
| 公开 | 网页剪藏、分享链接 | 云端明文 | 自动同步 | 可删除 |
| 私人 | 个人输入、笔记 | 本地加密 + 云端加密 | 用户选择 | 可关闭 |
| 敏感 | 密码、财务信息 | 仅本地加密 | 不同步 | 可查看/删除 |

#### 6.2.2 敏感内容识别

```kotlin
class PrivacyFilter {
    
    private val sensitivePatterns = listOf(
        // 密码
        Regex("(?i)password[:\\s=]+\\S+"),
        Regex("(?i)passwd[:\\s=]+\\S+"),
        Regex("(?i)pwd[:\\s=]+\\S+"),
        
        // 银行卡号
        Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
        Regex("\\b\\d{16}\\b"),
        Regex("\\b\\d{19}\\b"),
        
        // 身份证号
        Regex("\\b\\d{17}[\\dXx]\\b"),
        Regex("\\b\\d{18}[Xx]?\\b"),
        
        // 手机号
        Regex("\\b1[3-9]\\d{9}\\b"),
        
        // 邮箱
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        
        // 机密信息
        Regex("(?i)secret[:\\s=]+\\S+"),
        Regex("(?i)confidential[:\\s=]+\\S+"),
        
        // API 密钥
        Regex("\\b[a-zA-Z0-9]{32,}\\b")
    )
    
    fun isSensitive(content: String): Boolean {
        return sensitivePatterns.any { it.containsMatchIn(content) }
    }
    
    fun filter(content: String): FilteredResult {
        var filtered = content
        var foundSensitive = false
        val detectedTypes = mutableListOf<String>()
        
        sensitivePatterns.forEachIndexed { index, pattern ->
            if (pattern.containsMatchIn(filtered)) {
                foundSensitive = true
                filtered = pattern.replace(filtered, "[REDACTED]")
                detectedTypes.add(getPatternName(index))
            }
        }
        
        return FilteredResult(
            content = filtered,
            isSensitive = foundSensitive,
            detectedTypes = detectedTypes
        )
    }
    
    private fun getPatternName(index: Int): String {
        return when (index) {
            0, 1, 2 -> "PASSWORD"
            3, 4, 5 -> "BANK_CARD"
            6, 7 -> "ID_CARD"
            8 -> "PHONE"
            9 -> "EMAIL"
            10, 11 -> "CONFIDENTIAL"
            else -> "UNKNOWN"
        }
    }
}

data class FilteredResult(
    val content: String,
    val isSensitive: Boolean,
    val detectedTypes: List<String>
)
```

#### 6.2.3 权限管理

```kotlin
class PermissionManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)
    
    fun requestPermission(
        permissionType: PermissionType,
        rationale: String
    ): Flow<PermissionResult> = callbackFlow {
        // 检查是否已授权
        if (hasPermission(permissionType)) {
            trySend(PermissionResult.GRANTED)
            close()
            return@callbackFlow
        }
        
        // 显示授权对话框
        showPermissionDialog(
            type = permissionType,
            rationale = rationale,
            onConfirm = {
                prefs.edit().putBoolean(permissionType.name, true).apply()
                trySend(PermissionResult.GRANTED)
                close()
            },
            onReject = {
                trySend(PermissionResult.DENIED)
                close()
            }
        )
    }
    
    fun hasPermission(permissionType: PermissionType): Boolean {
        return prefs.getBoolean(permissionType.name, false)
    }
    
    fun revokePermission(permissionType: PermissionType) {
        prefs.edit().remove(permissionType.name).apply()
    }
    
    fun getAllPermissions(): Map<PermissionType, Boolean> {
        return PermissionType.values().associateWith { hasPermission(it) }
    }
}

enum class PermissionType(
    val title: String,
    val description: String,
    val icon: String
) {
    INPUT_CAPTURE(
        "输入采集",
        "采集您的输入内容用于知识库构建",
        "keyboard"
    ),
    CLOUD_SYNC(
        "云端同步",
        "将数据同步到云端以便多设备访问",
        "cloud"
    ),
    AI_PROCESSING(
        "AI 处理",
        "使用 AI 进行内容分析和推荐",
        "smart_toy"
    ),
    ANALYTICS(
        "使用分析",
        "收集匿名使用数据以改进产品",
        "analytics"
    )
}

sealed class PermissionResult {
    object GRANTED : PermissionResult()
    object DENIED : PermissionResult()
}
```

### 6.3 合规设计

#### 6.3.1 GDPR 合规

- ✅ 用户同意机制
- ✅ 数据可携带
- ✅ 被遗忘权（数据删除）
- ✅ 隐私政策透明
- ✅ 数据最小化

#### 6.3.2 个人信息保护法合规

- ✅ 明示收集目的
- ✅ 单独同意机制
- ✅ 敏感信息保护
- ✅ 境内存储
- ✅ 安全保护措施

---

## 7. 部署架构

### 7.1 开发环境

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  api-gateway:
    image: kong:latest
    ports:
      - "8000:8000"
      - "8443:8443"
    environment:
      - KONG_DATABASE=off
      - KONG_DECLARATIVE_CONFIG=/kong/declarative/kong.yml
    volumes:
      - ./kong:/kong/declarative
  
  auth-service:
    build: ./services/auth
    ports:
      - "8081:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/syncrime
      - JWT_SECRET=dev_secret_change_in_production
      - REDIS_URL=redis://redis:6379
  
  user-service:
    build: ./services/user
    ports:
      - "8082:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/syncrime
      - REDIS_URL=redis://redis:6379
  
  data-service:
    build: ./services/data
    ports:
      - "8083:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/syncrime
      - REDIS_URL=redis://redis:6379
      - ELASTICSEARCH_URL=http://elasticsearch:9200
  
  ai-service:
    build: ./services/ai
    ports:
      - "8084:8080"
    environment:
      - LLM_API_KEY=${LLM_API_KEY}
      - LLM_API_URL=https://dashscope.aliyuncs.com
  
  db:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=syncrime
      - POSTGRES_USER=syncrime
      - POSTGRES_PASSWORD=dev_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
  
  elasticsearch:
    image: elasticsearch:8.9.0
    ports:
      - "9200:9200"
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    volumes:
      - es_data:/usr/share/elasticsearch/data

volumes:
  postgres_data:
  redis_data:
  es_data:
```

### 7.2 生产环境

#### Kubernetes 部署

```yaml
# kubernetes/production/api-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: syncrime-api
  namespace: production
  labels:
    app: syncrime-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: syncrime-api
  template:
    metadata:
      labels:
        app: syncrime-api
        version: v1.0.0
    spec:
      containers:
      - name: api
        image: registry.cn-shanghai.aliyuncs.com/syncrime/api:1.0.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: syncrime-api
  namespace: production
spec:
  selector:
    app: syncrime-api
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: syncrime-api-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: syncrime-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 7.3 CI/CD 流程

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Upload coverage
      uses: codecov/codecov-action@v3
  
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Build Docker image
      run: docker build -t syncrime/api:${{ github.sha }} .
    
    - name: Push to registry
      run: |
        docker login registry.cn-shanghai.aliyuncs.com -u ${{ secrets.REGISTRY_USER }} -p ${{ secrets.REGISTRY_PASSWORD }}
        docker tag syncrime/api:${{ github.sha }} registry.cn-shanghai.aliyuncs.com/syncrime/api:${{ github.sha }}
        docker push registry.cn-shanghai.aliyuncs.com/syncrime/api:${{ github.sha }}
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
    - uses: actions/checkout@v3
    
    - name: Deploy to Kubernetes
      run: |
        kubectl apply -f kubernetes/production/
        kubectl rollout restart deployment/syncrime-api
```

---

## 8. 开发计划

### 8.1 总体时间线

```
Week 1-2:  基础架构 + 技术验证
Week 3-6:  MVP 开发 (P0 功能)
Week 7-10: 核心功能开发 (P1 功能)
Week 11-12: 测试优化 + 发布准备
```

### 8.2 详细迭代计划

#### Sprint 1 (Week 1-2): 基础架构

**目标**: 完成技术选型和基础架构搭建

**任务**:
- [ ] 项目初始化
  - [ ] Git 仓库创建
  - [ ] CI/CD 配置
  - [ ] 代码规范配置
  
- [ ] 技术验证
  - [ ] 输入采集 POC
  - [ ] AI API 选型测试
  - [ ] 数据库设计评审
  
- [ ] 基础架构
  - [ ] 数据库 Schema 设计
  - [ ] Room 配置
  - [ ] 网络框架配置
  
- [ ] UI 框架
  - [ ] App 主界面
  - [ ] 导航结构
  - [ ] 主题配置

**交付物**:
- 可运行的基础框架
- 技术验证报告
- 数据库设计文档

---

#### Sprint 2-3 (Week 3-6): MVP 开发

**目标**: 完成 P0 功能开发

**Sprint 2 (Week 3-4)**:
- [ ] 输入采集
  - [ ] AccessibilityService 实现
  - [ ] 隐私过滤器
  - [ ] 本地存储
  
- [ ] 基础搜索
  - [ ] SQLite FTS 配置
  - [ ] 搜索界面
  - [ ] 搜索结果展示

**Sprint 3 (Week 5-6)**:
- [ ] 统计功能
  - [ ] 数据看板
  - [ ] 图表展示
  - [ ] 数据导出
  
- [ ] 设置功能
  - [ ] 权限管理
  - [ ] 隐私设置
  - [ ] 同步设置

**交付物**:
- MVP 版本
- 内部测试报告

---

#### Sprint 4-5 (Week 7-10): 核心功能

**目标**: 完成 P1 功能开发

**Sprint 4 (Week 7-8)**: AI 功能
- [ ] 智能续写
  - [ ] LLM API 集成
  - [ ] 续写界面
  - [ ] 多风格支持
  
- [ ] AI 对话
  - [ ] 对话界面
  - [ ] 历史记录
  - [ ] 语音输入

**Sprint 5 (Week 9-10)**: 云同步
- [ ] 用户系统
  - [ ] 注册/登录
  - [ ] JWT 认证
  - [ ] 设备管理
  
- [ ] 云同步
  - [ ] 同步协议
  - [ ] 冲突解决
  - [ ] 增量同步

**交付物**:
- 完整功能版本
- 封闭测试版本

---

#### Sprint 6 (Week 11-12): 发布准备

**目标**: 完成测试和发布准备

**任务**:
- [ ] 公开测试
  - [ ] 种子用户招募 (100 人)
  - [ ] 反馈收集
  - [ ] 快速迭代
  
- [ ] 应用商店
  - [ ] 材料准备 (截图/描述)
  - [ ] 审核提交
  - [ ] 上架发布
  
- [ ] 运营准备
  - [ ] 用户文档
  - [ ] 客服培训
  - [ ] 数据分析配置

**交付物**:
- 公开发布版本
- 用户文档
- 运营手册

---

### 8.3 里程碑

| 里程碑 | 时间 | 交付物 | 成功标准 |
|--------|------|--------|---------|
| M1: 技术验证 | Week 2 | POC 演示 | 核心技术可行 |
| M2: MVP 完成 | Week 6 | MVP 版本 | P0 功能完成 |
| M3: 功能完整 | Week 10 | Beta 版本 | P1 功能完成 |
| M4: 公开发布 | Week 12 | v1.0.0 | 应用商店上架 |

---

## 9. 测试策略

### 9.1 测试层次

```
┌─────────────────────────────────────┐
│         E2E 测试 (5%)                │
│    (关键用户流程)                    │
├─────────────────────────────────────┤
│       集成测试 (20%)                 │
│    (模块间交互)                      │
├─────────────────────────────────────┤
│       单元测试 (75%)                 │
│    (函数/类级别)                     │
└─────────────────────────────────────┘
```

### 9.2 单元测试

```kotlin
// 示例：PrivacyFilter 单元测试
class PrivacyFilterTest {
    
    private lateinit var filter: PrivacyFilter
    
    @Before
    fun setup() {
        filter = PrivacyFilter()
    }
    
    @Test
    fun `test password detection`() {
        assertTrue(filter.isSensitive("password: 123456"))
        assertTrue(filter.isSensitive("PASSWORD: abcdef"))
        assertTrue(filter.isSensitive("pwd=secret123"))
        assertFalse(filter.isSensitive("normal text"))
    }
    
    @Test
    fun `test bank card detection`() {
        assertTrue(filter.isSensitive("6222021234567890"))
        assertTrue(filter.isSensitive("6222 0212 3456 7890"))
        assertTrue(filter.isSensitive("6222-0212-3456-7890"))
        assertFalse(filter.isSensitive("123456"))
    }
    
    @Test
    fun `test filter functionality`() {
        val result = filter.filter("My password is 123456")
        
        assertTrue(result.isSensitive)
        assertTrue(result.detectedTypes.contains("PASSWORD"))
        assertTrue(result.content.contains("[REDACTED]"))
    }
}
```

### 9.3 集成测试

```kotlin
// 示例：数据同步集成测试
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class SyncIntegrationTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Test
    fun `test push sync`() {
        // 准备测试数据
        val user = createTestUser()
        val token = generateToken(user)
        
        val request = SyncPushRequest(
            deviceId = "test-device",
            lastSyncTime = 0,
            records = listOf(
                SyncRecord(
                    id = 1710777600000,
                    type = "INPUT_RECORD",
                    data = mapOf(
                        "content" to "测试内容",
                        "application" to "com.test.app"
                    )
                )
            )
        )
        
        // 执行请求
        mockMvc.perform(
            post("/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $token")
                .content(JsonUtils.toJson(request))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.synced_count").value(1))
    }
}
```

### 9.4 E2E 测试

```kotlin
// 示例：用户注册登录 E2E 测试
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/resources/features"],
    glue = ["com.syncrime.steps"]
)
class E2ETest

// Step definitions
@Given("用户访问注册页面")
fun userVisitsRegistrationPage() {
    // 打开注册页面
}

@When("用户填写注册信息")
fun userFillsRegistrationInfo() {
    // 填写表单
}

@Then("用户应该成功注册")
fun userShouldSuccessfullyRegister() {
    // 验证注册成功
}
```

### 9.5 性能测试

```yaml
# k6 性能测试脚本
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 },  //  ramp up to 100 users
    { duration: '5m', target: 100 },  //  stay at 100 users
    { duration: '2m', target: 200 },  //  ramp up to 200 users
    { duration: '5m', target: 200 },  //  stay at 200 users
    { duration: '2m', target: 0 },    //  ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.01'],   // error rate should be < 1%
  },
};

export default function () {
  const token = authenticate();
  
  // 测试搜索 API
  const searchResponse = http.post(
    'https://api.syncrime.com/v1/search/query',
    JSON.stringify({ query: 'test' }),
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    }
  );
  
  check(searchResponse, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
  
  sleep(1);
}
```

---

## 10. 监控与日志

### 10.1 监控指标

#### 应用指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| QPS | 每秒请求数 | > 1000 |
| 响应时间 | P95 响应时间 | > 500ms |
| 错误率 | HTTP 5xx 比例 | > 1% |
| 活跃用户 | DAU/MAU | 突降 20% |
| 同步延迟 | 平均同步时间 | > 5 分钟 |

#### 系统指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| CPU 使用率 | 服务器 CPU | > 80% |
| 内存使用率 | 服务器内存 | > 85% |
| 磁盘使用率 | 存储使用 | > 80% |
| 数据库连接 | 连接池使用 | > 90% |
| Redis 内存 | Redis 使用 | > 80% |

### 10.2 日志规范

```kotlin
// 日志级别使用规范
class SyncService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun syncData(userId: String) {
        // DEBUG: 详细调试信息
        logger.debug("Starting sync for user: $userId")
        
        try {
            // INFO: 重要业务操作
            logger.info("Sync started for user: $userId")
            
            // 业务逻辑...
            
            // INFO: 操作成功
            logger.info("Sync completed for user: $userId, records: $count")
            
        } catch (e: BusinessException) {
            // WARN: 可恢复的错误
            logger.warn("Sync failed for user: $userId, reason: ${e.message}")
            throw e
            
        } catch (e: Exception) {
            // ERROR: 严重错误
            logger.error("Sync error for user: $userId", e)
            throw e
        }
    }
}
```

### 10.3 监控工具栈

| 工具 | 用途 | 部署方式 |
|------|------|---------|
| Prometheus | 指标采集 | Kubernetes |
| Grafana | 指标展示 | Kubernetes |
| ELK Stack | 日志收集 | Kubernetes |
| Sentry | 错误追踪 | SaaS |
| Firebase | 移动端监控 | SaaS |

---

## 11. 团队与成本

### 11.1 团队配置

| 角色 | 人数 | 月薪 (¥) | 月成本 (¥) |
|------|------|---------|-----------|
| 产品经理 | 1 | 35k | 35k |
| 技术负责人 | 1 | 50k | 50k |
| Android 开发 | 2 | 40k | 80k |
| 后端开发 | 1 | 40k | 40k |
| 前端开发 | 1 | 35k | 35k |
| UI/UX 设计师 | 1 | 30k | 30k |
| 测试工程师 | 1 | 25k | 25k |
| **小计** | **7** | | **295k** |

### 11.2 开发成本

| 项目 | 金额 (¥) | 说明 |
|------|---------|------|
| 人力成本 | 1,030k | 7 人 × 3 月 × 平均 42k |
| 外包成本 | 180k | 设计 + 插件 + 测试 |
| 设备成本 | 50k | 测试设备 |
| 办公成本 | 90k | 场地 + 设备 |
| **小计** | **1,350k** | |

### 11.3 运营成本（月）

| 项目 | 金额 (¥) | 说明 |
|------|---------|------|
| 云服务器 | 5k | 阿里云/腾讯云 |
| CDN | 2k | 静态资源加速 |
| 大模型 API | 10k | 按调用量 |
| 存储 | 3k | OSS/COS |
| 域名证书 | 1k | 年费摊销 |
| 监控服务 | 1k | Sentry 等 |
| **小计** | **22k/月** | |

### 11.4 总预算

```
开发成本：¥1,350k
运营储备：¥264k (12 月)
不可预见：¥200k (15%)
━━━━━━━━━━━━━━━━━━━━
总计：¥1,814k ≈ 180 万
```

---

## 12. 风险评估

### 12.1 技术风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|---------|
| 无障碍服务限制 | 中 (40%) | 高 | 多方案备选 (IME/辅助功能) |
| 大模型 API 不稳定 | 低 (20%) | 中 | 多供应商 + 降级方案 |
| 数据同步冲突 | 中 (50%) | 中 | 完善的冲突解决机制 |
| 性能问题 | 低 (20%) | 中 | 早期性能测试 + 优化 |

### 12.2 市场风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|---------|
| 竞品模仿 | 高 (70%) | 中 | 快速迭代 + 建立壁垒 |
| 用户接受度低 | 中 (50%) | 高 | 用户教育 + 降低门槛 |
| 获客成本高 | 中 (50%) | 中 | 内容营销 + 口碑传播 |
| 付费意愿低 | 中 (50%) | 高 | 免费 + 增值模式 |

### 12.3 隐私风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|---------|
| 数据泄露 | 低 (10%) | 高 | 端到端加密 + 安全审计 |
| 隐私投诉 | 中 (40%) | 高 | 明确告知 + 用户可控 |
| 合规问题 | 中 (40%) | 高 | 法律顾问 + 合规审查 |

### 12.4 风险矩阵

```
影响
 高 │  隐私投诉      用户接受度
    │  合规问题      付费意愿低
    │
    │  无障碍限制    竞品模仿
 中 │  同步冲突      获客成本
    │  性能问题      API 不稳定
    │
    │  数据泄露
 低 │
    └─────────────────────────────
      低        中        高
              概率
```

---

## 13. 附录

### 13.1 术语表

| 术语 | 说明 |
|------|------|
| Clipper | 剪藏工具，用于保存网页/内容 |
| LLM | 大语言模型 (Large Language Model) |
| FTS | 全文搜索 (Full-Text Search) |
| JWT | JSON Web Token，认证令牌 |
| POC | 概念验证 (Proof of Concept) |
| MVP | 最小可行产品 (Minimum Viable Product) |
| DAU | 日活跃用户 (Daily Active Users) |
| MAU | 月活跃用户 (Monthly Active Users) |
| NPS | 净推荐值 (Net Promoter Score) |

### 13.2 参考文档

1. [Android 无障碍服务开发指南](https://developer.android.com/guide/topics/ui/accessibility/service)
2. [Room 数据库官方文档](https://developer.android.com/training/data-storage/room)
3. [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
4. [Elasticsearch 官方文档](https://www.elastic.co/guide/index.html)
5. [通义千问 API 文档](https://help.aliyun.com/zh/dashscope/)

### 13.3 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-03-18 | 初始版本 | TBD |

---

**文档结束**

*本技术方案为 SyncRime 项目 v1.0 版本，下次评审时间：MVP 完成后*
