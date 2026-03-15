# SyncRime Android 客户端

SyncRime 的 Android 客户端应用，提供现代化的用户界面和完整的输入管理功能。

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)

---

## 📱 功能特性

### 核心功能
- 📊 **输入统计** - 可视化输入数据分析
- 👤 **个人中心** - 用户配置和管理
- ⚙️ **智能设置** - 个性化配置选项
- 🔄 **数据同步** - 云端同步管理
- 🔔 **通知系统** - 智能提醒

### 智能化功能
- 🧠 **智能推荐** - 基于上下文的输入建议
- 🌍 **多语言支持** - 11 种语言智能切换
- ✏️ **智能纠错** - 实时纠错和补全
- 📖 **语义分析** - 深度语义理解
- 🎯 **个性化学习** - 自适应学习引擎

---

## 🏗️ 技术架构

### 架构模式
- **MVVM** - Model-View-ViewModel
- **Clean Architecture** - 清晰的分层架构
- **Repository Pattern** - 数据访问抽象

### 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Kotlin |
| **UI** | Jetpack Compose + Material Design 3 |
| **架构组件** | ViewModel, StateFlow, Navigation |
| **依赖注入** | Hilt |
| **数据库** | Room + SQLite |
| **网络** | Retrofit + OkHttp |
| **异步** | Coroutines + Flow |
| **后台任务** | WorkManager |
| **测试** | JUnit, MockK, Espresso |

---

## 📁 项目结构

```
android-client/
├── app/
│   ├── src/main/
│   │   ├── java/com/syncrime/android/
│   │   │   ├── presentation/          # UI 层
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── navigation/        # 导航系统
│   │   │   │   ├── ui/
│   │   │   │   │   ├── main/          # 主界面
│   │   │   │   │   ├── settings/      # 设置界面
│   │   │   │   │   ├── statistics/    # 统计界面
│   │   │   │   │   ├── profile/       # 个人界面
│   │   │   │   │   └── intelligence/  # 智能设置
│   │   │   │   └── theme/             # 主题系统
│   │   │   │
│   │   │   ├── domain/                # 业务逻辑层
│   │   │   │   ├── model/             # 数据模型
│   │   │   │   ├── usecase/           # 用例
│   │   │   │   └── repository/        # 仓库接口
│   │   │   │
│   │   │   ├── data/                  # 数据层
│   │   │   │   ├── local/             # 本地数据 (Room)
│   │   │   │   ├── remote/            # 远程 API (Retrofit)
│   │   │   │   └── repository/        # 仓库实现
│   │   │   │
│   │   │   ├── intelligence/          # 智能引擎
│   │   │   │   └── engines/           # 各引擎实现
│   │   │   │
│   │   │   ├── sync/                  # 同步服务
│   │   │   │   ├── SyncWorker.kt
│   │   │   │   └── SyncScheduler.kt
│   │   │   │
│   │   │   ├── notification/          # 通知系统
│   │   │   │   └── NotificationManager.kt
│   │   │   │
│   │   │   └── permission/            # 权限管理
│   │   │       └── PermissionManager.kt
│   │   │
│   │   ├── res/                       # 资源文件
│   │   └── AndroidManifest.xml
│   │
│   ├── test/                          # 单元测试
│   └── androidTest/                   # 仪器测试
│
├── docs/                              # 文档
│   ├── architecture-design.md
│   └── phase3-core-completion-report.md
│
└── build.gradle                       # 构建配置
```

---

## 🚀 快速开始

### 前置要求

- Android Studio Arctic Fox 或更高版本
- JDK 11+
- Android SDK 31+

### 构建项目

```bash
# 克隆项目
git clone https://github.com/tim5wang/syncrime.git
cd syncrime/android-client

# 使用 Gradle 构建
./gradlew assembleDebug

# 或使用 Android Studio
# File → Open → 选择 android-client 目录
```

### 安装到设备

```bash
# 通过 ADB 安装
adb install app/build/outputs/apk/debug/app-debug.apk

# 或使用 Gradle
./gradlew installDebug
```

---

## 🧪 测试

```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 运行仪器测试
./gradlew connectedDebugAndroidTest

# 运行所有测试
./gradlew test
```

### 测试覆盖率

```bash
# 生成覆盖率报告
./gradlew jacocoTestReport

# 查看报告
# 打开 app/build/reports/jacoco/jacocoTestReport/html/index.html
```

---

## 📦 构建变体

| 变体 | 描述 | 用途 |
|------|------|------|
| `debug` | Debug 版本 | 开发调试 |
| `release` | Release 版本 | 生产发布 |

### 构建 Release 版本

```bash
# 签名配置
# 在 gradle.properties 中配置:
# SYNCRIME_RELEASE_STORE_FILE=release.keystore
# SYNCRIME_RELEASE_KEY_ALIAS=syncrime
# SYNCRIME_RELEASE_STORE_PASSWORD=******
# SYNCRIME_RELEASE_KEY_PASSWORD=******

./gradlew assembleRelease
```

---

## 🔧 开发指南

### 代码规范

遵循 [Kotlin 官方代码规范](https://kotlinlang.org/docs/coding-conventions.html)

```bash
# 检查代码格式
./gradlew ktlintCheck

# 格式化代码
./gradlew ktlintFormat
```

### 依赖注入

使用 Hilt 进行依赖注入：

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel()
```

### 架构原则

1. **单一职责** - 每个类只负责一个功能
2. **依赖倒置** - 依赖抽象而非具体实现
3. **数据流单向** - UI → ViewModel → UseCase → Repository → Data
4. **状态管理** - 使用 StateFlow 管理 UI 状态

---

## 📚 文档

- [架构设计](./docs/architecture-design.md) - 详细架构说明
- [Phase 3 完成报告](./docs/phase3-core-completion-report.md) - 开发进展
- [项目主文档](../docs/README.md) - 完整文档导航

---

## 🐛 常见问题

### Q: 如何调试应用？
A: 使用 Android Studio 的 Debug 功能，或使用 Logcat：
```bash
adb logcat -s SyncRime
```

### Q: 数据库在哪里？
A: 本地数据库路径：`/data/data/com.syncrime.android/databases/syncrime.db`

### Q: 如何清除应用数据？
A: 
```bash
adb shell pm clear com.syncrime.android
```

---

## 📄 许可证

MIT License - 详见 [LICENSE](../LICENSE)

---

*最后更新：2026-03-16*
