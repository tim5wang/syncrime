# SyncRime Trime Plugin

SyncRime 的 Trime 输入法插件，实现输入内容采集和同步功能。

## 功能特性

- 🎯 实时输入内容采集
- 🔄 智能同步机制
- 📊 输入行为分析
- 🔗 知识库集成
- ⚡ 高性能数据处理

## 架构设计

```
SyncRime Plugin
├── Kotlin 层 (插件接口)
├── JNI 层 (原生桥接)
├── C++ 层 (核心逻辑)
└── 配置层 (用户设置)
```

## 快速开始

1. 将插件复制到 Trime 插件目录
2. 在 Trime 设置中启用 SyncRime 插件
3. 配置同步服务器地址
4. 开始使用同步功能

## 开发指南

### 构建要求

- Android Studio Arctic Fox+
- Kotlin 1.7+
- NDK 21+
- Trime 3.2+

### 构建步骤

```bash
# 克隆项目
git clone https://github.com/syncrime/trime-plugin.git
cd trime-plugin

# 构建插件
./gradlew assembleRelease

# 安装到设备
adb install app/build/outputs/apk/release/app-release.apk
```

## API 文档

### 核心接口

#### SyncRimePlugin
```kotlin
interface SyncRimePlugin {
    fun initialize(context: Context)
    fun startCapture()
    fun stopCapture()
    fun syncData()
    fun getStatus(): PluginStatus
}
```

#### DataCollector
```kotlin
interface DataCollector {
    fun captureInput(text: String, metadata: InputMetadata)
    fun analyzePattern(): InputPattern
    fun syncToServer()
}
```

## 配置选项

### 基础配置
```yaml
syncrime:
  enabled: true
  server_url: "https://api.syncrime.com"
  sync_interval: 300
  capture_mode: "smart"
```

### 高级配置
```yaml
syncrime:
  advanced:
    cache_size: 1000
    compression: true
    encryption: true
    batch_size: 50
```

## 故障排除

### 常见问题

1. **插件无法加载**
   - 检查 Trime 版本兼容性
   - 确认插件权限设置

2. **同步失败**
   - 验证网络连接
   - 检查服务器配置

3. **性能问题**
   - 调整缓存大小
   - 优化同步间隔

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 发起 Pull Request

## 联系方式

- 项目主页: https://syncrime.com
- 问题反馈: https://github.com/syncrime/trime-plugin/issues
- 邮箱: support@syncrime.com