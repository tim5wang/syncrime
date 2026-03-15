# SyncRime - 智能输入内容采集与同步系统

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)

[English](./docs/user-guide/README.en.md) | [中文](./README.md) | [文档中心](./docs/README.md)

---

## 📖 简介

SyncRime 是一款与 **Trime 输入法**（同文输入法）深度集成的智能输入内容采集与同步系统。它提供：

- 🧠 **智能输入采集** - 自动采集输入内容用于智能推荐
- ☁️ **云端同步** - 安全同步输入数据到云端
- 🤖 **智能推荐** - 基于上下文的个性化输入建议
- 🌍 **多语言支持** - 支持 11 种语言的智能切换
- 🔒 **隐私保护** - 本地数据加密，保护用户隐私

---

## ✨ 核心特性

### 🎯 智能输入
- 上下文感知输入
- 智能纠错和补全
- 语义分析和理解
- 个性化学习引擎

### 📊 数据同步
- 增量同步，节省流量
- 冲突自动解决
- 后台自动同步
- 离线支持

### 🎨 现代化 UI
- Material Design 3 设计语言
- Jetpack Compose 声明式 UI
- 流畅的动画效果
- 深色模式支持

---

## 🏗️ 项目架构

```
SyncRime/
├── 📱 android-client/          # Android 客户端 (Phase 3)
│   ├── app/src/main/
│   │   ├── java/com/syncrime/android/
│   │   │   ├── presentation/   # UI 层 (Jetpack Compose)
│   │   │   ├── domain/         # 业务逻辑层
│   │   │   ├── data/           # 数据层
│   │   │   ├── intelligence/   # 智能引擎
│   │   │   └── sync/           # 同步服务
│   │   └── res/                # 资源文件
│   └── docs/                   # Android 文档
│
├── 🔌 trime-plugin/            # Trime 插件 (Phase 1)
│   ├── app/src/main/
│   │   ├── java/               # Kotlin 代码
│   │   └── cpp/                # C++ 原生代码
│   └── docs/                   # 插件文档
│
└── 📚 docs/                    # 项目文档中心
    ├── architecture/           # 架构设计
    ├── design-notes/           # 设计文档
    ├── user-guide/             # 用户指南
    └── development/            # 开发指南
```

---

## 🚀 快速开始

### 前置要求

- Android Studio Arctic Fox 或更高版本
- JDK 11+
- Android SDK 31+
- Trime 输入法 3.0+

### 安装步骤

#### 方法 1: 直接安装 APK
1. 从 [Releases](https://github.com/tim5wang/syncrime/releases) 下载最新 APK
2. 在 Android 设备上安装
3. 启用 Trime 插件

#### 方法 2: 源码编译
```bash
# 克隆项目
git clone https://github.com/tim5wang/syncrime.git
cd syncrime

# 编译 Android 客户端
cd android-client
./gradlew assembleDebug

# 编译 Trime 插件
cd ../trime-plugin
./gradlew assembleDebug
```

### 配置

1. 打开 SyncRime Android 客户端
2. 完成初始设置向导
3. 启用输入采集功能
4. 配置云同步（可选）

详细文档见 [用户指南](./docs/user-guide/USER_GUIDE.md)

---

## 🧠 智能化功能

SyncRime 集成了 6 大智能引擎：

| 引擎 | 功能 | 准确率 |
|------|------|--------|
| **智能推荐引擎** | 多维度推荐系统 | > 85% |
| **上下文感知引擎** | 7 种上下文维度分析 | > 90% |
| **个性化学习引擎** | 10 个个性化维度 | - |
| **智能纠错引擎** | 多层次纠错系统 | > 85% |
| **语义分析引擎** | 深度语义理解 | > 80% |
| **多语言引擎** | 11 种语言支持 | > 95% |

详见 [Phase 2 完成报告](./docs/phase2-completion-report.md)

---

## 📱 技术栈

### Android 客户端
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据库**: Room + SQLite
- **网络**: Retrofit + OkHttp
- **异步**: Coroutines + Flow
- **后台任务**: WorkManager

### Trime 插件
- **语言**: Kotlin + C++ (JNI)
- **集成**: Trime SDK
- **原生库**: libsyncrime_plugin.so

### 智能化
- **ML 框架**: TensorFlow Lite / PyTorch Mobile
- **NLP**: BERT / GPT / Transformers
- **语言检测**: FastText / Google ML Kit

---

## 📊 项目进展

| 阶段 | 描述 | 状态 |
|------|------|------|
| **Phase 1** | Trime 深度集成 | ✅ 100% |
| **Phase 2** | 智能化功能增强 | ✅ 100% |
| **Phase 3** | Android 客户端开发 | ✅ 95% |
| **Phase 4** | 基础设施完善 | 🔄 0% |
| **Phase 5** | 后端服务开发 | ⏳ 计划中 |

详细进展见 [PROJECT_PROGRESS.md](./docs/development/PROJECT_PROGRESS.md)

---

## 📚 文档

- **[文档中心](./docs/README.md)** - 完整文档导航
- **[用户指南](./docs/user-guide/USER_GUIDE.md)** - 安装和使用指南
- **[架构设计](./docs/architecture/)** - 系统架构文档
- **[开发路线图](./docs/getting-started/roadmap.md)** - 未来规划

---

## 🤝 贡献

欢迎贡献！请查看：

1. [贡献指南](./CONTRIBUTING.md)（待创建）
2. [开发文档](./docs/development/)
3. [代码规范](./docs/development/CODE_STYLE.md)（待创建）

### 开发环境设置
```bash
# 克隆项目
git clone https://github.com/tim5wang/syncrime.git
cd syncrime

# 使用 Android Studio 打开
# File -> Open -> 选择 android-client 或 trime-plugin 目录
```

---

## 📄 许可证

MIT License - 详见 [LICENSE](./LICENSE) 文件

---

## 📬 联系方式

- **GitHub**: [@tim5wang](https://github.com/tim5wang/syncrime)
- **Issues**: [GitHub Issues](https://github.com/tim5wang/syncrime/issues)
- **Discussions**: [GitHub Discussions](https://github.com/tim5wang/syncrime/discussions)

---

## 🙏 致谢

- [Trime 输入法](https://github.com/osfans/trime) - 强大的开源输入法
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化 UI 框架
- 所有贡献者和用户

---

*最后更新：2026-03-16*
