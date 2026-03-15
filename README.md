# SyncRime - 智能输入内容采集与同步系统

[English](./docs/USER_GUIDE.md) | [中文](./docs/USER_GUIDE.md)

## 概述

SyncRime 是一款与 Trime 输入法深度集成的智能输入内容采集与同步系统，提供智能化的输入体验。

## 核心特性

- **智能输入采集**: 自动采集输入内容用于智能推荐
- **云端同步**: 安全同步输入数据到云端
- **智能推荐**: 基于上下文的个性化输入建议
- **多语言支持**: 支持 11 种语言的智能切换
- **隐私保护**: 本地数据加密，保护用户隐私

## 项目结构

```
SyncRime/
├── trime-plugin/                 # Phase 1: Trime 插件
├── android-client/               # Phase 3: Android 客户端
├── docs/                         # 项目文档
└── README.md                     # 项目说明
```

## 技术栈

### Android 客户端
- Kotlin + Jetpack Compose
- MVVM + Clean Architecture
- Hilt 依赖注入
- Room + Retrofit + WorkManager

### 智能化功能
- TensorFlow Lite / PyTorch Mobile
- BERT / GPT / Transformers
- 个性化推荐算法

## 快速开始

1. 安装 Trime 输入法
2. 安装 SyncRime 插件 APK
3. 启用插件并完成初始设置

详细文档见 [用户指南](./docs/USER_GUIDE.md)

## 版本

当前版本: 1.0.0

## 许可证

MIT License

## 联系方式

- GitHub: https://github.com/tim5wang/syncrime
