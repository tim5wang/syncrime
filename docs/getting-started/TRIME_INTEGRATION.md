# SyncRime Trime 集成指南

## 🎯 问题说明

当前 SyncRime Android 客户端通过**无障碍服务**实现输入采集，不需要编译 Trime 插件！

**工作原理**：
```
SyncRime Android App (无障碍服务)
         ↓
监听系统输入事件
         ↓
采集输入内容 → Room 数据库 → 同步
```

---

## 📦 安装步骤

### 1. 安装 Trime 输入法

**下载**: https://github.com/osfans/trime/releases

```bash
# 下载最新版本 (ARM64)
curl -LO https://github.com/osfans/trime/releases/download/3.4.2/trime-3.4.2-arm64-v8a.apk

# 安装
adb install trime-3.4.2-arm64-v8a.apk
```

### 2. 安装 SyncRime Android 客户端

```bash
# 安装之前编译的 APK
adb install ~/claw_workspaces/tim/syncrime/SyncRime-v1.0-debug.apk
```

### 3. 配置 Trime 输入法

1. **设置 → 系统 → 语言和输入法**
2. **虚拟键盘 → 管理键盘 → 开启 Trime**
3. **切换输入法**: 通知栏 → 选择输入法 → Trime

### 4. 配置 SyncRime 无障碍服务

1. **打开 SyncRime App**
2. **授予权限**:
   - 无障碍权限
   - 通知权限
   - 存储权限（可选）
3. **开启采集服务**

---

## 🔧 Rime 配置集成（可选）

如果想让 Trime 和 SyncRime 更好地协同，可以配置 Rime：

### 1. 创建 SyncRime Rime 配置

在手机上的配置目录：
```
/sdcard/rime/
├── default.yaml
├── syncrime.schema.yaml
└── syncrime.custom.yaml
```

### 2. 配置文件示例

**syncrime.custom.yaml**:
```yaml
# Rime 配置扩展
patch:
  # 启用 SyncRime 集成
  syncrime:
    enabled: true
    capture_mode: smart
    
  # 键盘布局
  key_binder:
    bindings:
      # Ctrl+Shift+S 触发同步
      - { when: always, from: "Control+Shift+S", to: "F1" }
      
  # 候选词增强
  translator:
    # 启用 SyncRime 推荐
    syncrime_recommendations: true
```

### 3. 部署配置

1. 打开 Trime 输入法
2. **设置 → 用户词典 → 同步**
3. **重新部署**

---

## 🔍 验证集成

### 检查 SyncRime 服务状态

```bash
# 检查无障碍服务
adb shell settings get secure enabled_accessibility_services

# 应该包含: com.syncrime.android/.accessibility.InputCaptureService
```

### 测试输入采集

1. 打开任意应用（微信/备忘录）
2. 使用 Trime 输入法输入文字
3. 打开 SyncRime App 查看统计
4. 应该能看到输入记录

### 检查日志

```bash
# 查看 SyncRime 日志
adb logcat -s SyncRime

# 查看无障碍服务日志
adb logcat -s InputCaptureService
```

---

## ⚠️ 当前限制

### 已实现 ✅
- ✅ 无障碍服务采集输入
- ✅ Room 数据库存储
- ✅ 后台同步服务
- ✅ 统计界面
- ✅ 通知管理

### 待实现 🔄
- ⏳ Trime 插件原生集成（需要编译）
- ⏳ Rime 配置深度集成
- ⏳ 智能推荐引擎（Phase 2 未完成）
- ⏳ 云端同步（需要后端服务）

---

## 🛠️ 故障排除

### 问题 1: 找不到 SyncRime 服务

**解决**:
1. 设置 → 无障碍 → 找到 SyncRime
2. 开启服务
3. 重启 SyncRime App

### 问题 2: 无法采集输入

**解决**:
1. 检查无障碍权限
2. 确保 Trime 是当前输入法
3. 重启 SyncRime 服务

### 问题 3: 同步失败

**解决**:
```bash
# 清除应用数据
adb shell pm clear com.syncrime.android.debug

# 重新安装
adb install ~/claw_workspaces/tim/syncrime/SyncRime-v1.0-debug.apk
```

---

## 📊 数据流向

```
用户打字 (Trime 输入法)
         ↓
Android 系统输入事件
         ↓
SyncRime 无障碍服务 (监听)
         ↓
InputCaptureService.kt
         ↓
InputRepository (数据仓库)
         ↓
Room Database (本地存储)
         ↓
SyncWorker (后台同步)
         ↓
云端 API (待实现)
```

---

## 🚀 下一步

### 方案 A: 使用当前无障碍服务（推荐）
- ✅ 无需编译插件
- ✅ 即装即用
- ✅ 兼容所有输入法

### 方案 B: 编译 Trime 插件（深度集成）
需要完成：
1. 创建 Gradle Wrapper
2. 配置 Trime 依赖
3. 编译 C++ 原生库
4. 集成到 Trime 插件系统

---

## 📝 总结

**当前方案**（无障碍服务）已经可以实现：
- ✅ 输入内容采集
- ✅ 本地存储
- ✅ 后台同步
- ✅ 统计展示

**无需 Trime 插件**即可使用核心功能！

如需深度集成，再考虑编译 Trime 插件。

---

*最后更新：2026-03-18*
