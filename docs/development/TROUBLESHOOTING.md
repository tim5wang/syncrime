# SyncRime 故障排查指南

## 🚨 问题：找不到插件配置

### 原因说明

**SyncRime 当前版本没有 Trime 插件！**

当前实现方案：
```
┌─────────────────────────────────────┐
│  SyncRime Android App               │
│  ┌─────────────────────────────┐    │
│  │  无障碍服务 (InputCapture)   │    │
│  │  - 监听系统输入事件          │    │
│  │  - 采集输入内容              │    │
│  │  - 存储到 Room 数据库         │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
              ↑ 监听
              │
    ┌─────────┴──────────┐
    │  任何输入法          │
    │  (Trime/Gboard 等)   │
    └────────────────────┘
```

### 解决方案

**无需插件配置**，按照以下步骤使用：

1. **安装 SyncRime App**
   ```bash
   adb install ~/claw_workspaces/tim/syncrime/SyncRime-v1.0-debug.apk
   ```

2. **开启无障碍服务**
   - 设置 → 无障碍 → 找到 "SyncRime 输入采集"
   - 开启服务

3. **开始使用**
   - 打开 SyncRime App
   - 点击 "开始采集"
   - 正常输入即可

---

## 🚨 问题：没有同步

### 原因分析

**当前同步状态**：

| 功能 | 状态 | 说明 |
|------|------|------|
| 本地存储 | ✅ 完成 | Room 数据库 |
| 后台同步任务 | ✅ 完成 | WorkManager |
| 云端 API | ⏳ 未实现 | Phase 5 |
| 网络传输 | ⏳ 未实现 | 需要后端服务 |

### 当前同步流程

```
输入采集
   ↓
Room 数据库 (本地)
   ↓
SyncWorker (后台任务)
   ↓
⏳ 等待云端 API (Phase 5)
```

### 验证本地同步

**查看数据库**：
```bash
# 检查数据库文件
adb shell "ls -la /data/data/com.syncrime.android.debug/databases/"

# 输出示例:
# total 64
# -rw-rw---- 1 u0_a123 u0_a123 32768 syncrime.db
```

**查看数据**（需要 root）：
```bash
adb shell "sqlite3 /data/data/com.syncrime.android.debug/databases/syncrime.db 'SELECT COUNT(*) FROM input_records;'"
```

**查看同步任务状态**：
```bash
adb shell dumpsys jobscheduler | grep -A 10 "com.syncrime.android"
```

### 解决方案

**短期方案**：使用本地功能
- ✅ 查看统计数据
- ✅ 本地数据库查询
- ✅ 导出数据库文件

**长期方案**：等待 Phase 5
- 云端 API 服务
- 多设备同步
- 数据备份

---

## 🔧 常见问题排查

### 问题 1: 无障碍服务无法开启

**症状**：
- 设置中找不到 "SyncRime 输入采集"
- 开启后立即关闭

**排查步骤**：

1. **检查 App 安装**
   ```bash
   adb shell pm list packages | grep syncrime
   # 应输出：package:com.syncrime.android.debug
   ```

2. **检查服务声明**
   ```bash
   adb shell dumpsys package com.syncrime.android.debug | grep -A 5 "AccessibilityService"
   ```

3. **重启服务**
   ```bash
   adb shell am force-stop com.syncrime.android.debug
   # 然后重新打开 App
   ```

4. **重启手机**
   ```bash
   adb reboot
   ```

### 问题 2: 采集不到输入

**症状**：
- 无障碍服务已开启
- 但统计显示 0 输入

**排查步骤**：

1. **检查输入法**
   ```bash
   adb shell settings get secure default_input_method
   # 应输出当前输入法，如：com.osfans.trime/.TrimeIME
   ```

2. **检查日志**
   ```bash
   adb logcat -s SyncRime -s InputCaptureService
   ```

   **期望日志**：
   ```
   I/InputCaptureService: 无障碍服务已连接
   I/InputCaptureService: 创建新会话：xxx
   D/InputCaptureService: 收到事件：TYPE_VIEW_TEXT_CHANGED
   ```

3. **检查过滤规则**
   - 密码输入框会被过滤
   - 长度<1 的输入会被过滤
   - 敏感应用会被过滤

4. **测试输入**
   ```bash
   # 打开备忘录等普通应用
   # 输入测试文字
   # 查看日志是否有事件
   ```

### 问题 3: 应用崩溃

**症状**：
- 打开 SyncRime 就崩溃
- 采集服务崩溃

**排查步骤**：

1. **查看崩溃日志**
   ```bash
   adb logcat -s AndroidRuntime
   ```

2. **清除应用数据**
   ```bash
   adb shell pm clear com.syncrime.android.debug
   ```

3. **重新安装**
   ```bash
   adb install -r ~/claw_workspaces/tim/syncrime/SyncRime-v1.0-debug.apk
   ```

4. **检查 Android 版本**
   ```bash
   adb shell getprop ro.build.version.release
   # 应 >= 7.0
   ```

### 问题 4: 耗电过高

**症状**：
- 手机发热
- 电池消耗快

**解决方案**：

1. **降低采集频率**
   ```bash
   # 编辑配置（需要 root）
   adb shell "echo '<config><notification_timeout>500</notification_timeout></config>' > /data/data/com.syncrime.android.debug/shared_prefs/config.xml"
   ```

2. **关闭实时通知**
   - 打开 SyncRime App
   - 设置 → 通知 → 关闭

3. **限制后台活动**
   - 设置 → 应用 → SyncRime → 电池 → 限制

---

## 📊 诊断工具

### 运行诊断脚本

```bash
cd ~/claw_workspaces/tim/syncrime
./scripts/check_syncrime.sh
```

**输出示例**：
```
======================================
  SyncRime 快速配置工具
======================================

📱 检查设备连接...
✅ 设备已连接

🔍 检查 SyncRime 安装状态...
✅ SyncRime 已安装

🔧 检查无障碍服务状态...
✅ 无障碍服务已启用

⌨️  检查 Trime 输入法...
✅ Trime 输入法已安装

💾 检查 SyncRime 数据库...
✅ 数据库存在

📊 当前统计数据：
（需要 root 权限查看详细统计）

📝 最近日志（最后 10 条）：
I/InputCaptureService: 无障碍服务已连接
I/InputCaptureService: 创建新会话...
```

### 手动诊断命令

```bash
# 1. 检查服务状态
adb shell settings get secure enabled_accessibility_services

# 2. 查看数据库
adb shell "ls -la /data/data/com.syncrime.android*/databases/"

# 3. 查看日志
adb logcat -s SyncRime --pid=$(adb shell pidof -s com.syncrime.android.debug) -d

# 4. 检查输入法
adb shell ime list

# 5. 查看 App 信息
adb shell dumpsys package com.syncrime.android.debug
```

---

## 📞 获取帮助

### 收集诊断信息

**提交 Issue 时请提供**：

1. **设备信息**
   ```bash
   adb shell getprop ro.product.model
   adb shell getprop ro.build.version.release
   ```

2. **App 版本**
   ```bash
   adb shell dumpsys package com.syncrime.android.debug | grep version
   ```

3. **日志文件**
   ```bash
   adb logcat -d > syncrime_log.txt
   ```

4. **数据库文件**（可选）
   ```bash
   adb pull /data/data/com.syncrime.android.debug/databases/syncrime.db
   ```

### 联系方式

- **GitHub Issues**: https://github.com/tim5wang/syncrime/issues
- **文档**: ~/claw_workspaces/tim/syncrime/docs/

---

## 📝 总结

**当前版本定位**：
- ✅ 本地输入采集（无障碍服务）
- ✅ 本地数据存储（Room）
- ✅ 统计展示
- ⏳ 云端同步（Phase 5）

**无需 Trime 插件**即可使用核心功能！

**下一步**：
1. 完成云端 API 服务
2. 实现完整同步
3. 可选：编译 Trime 插件深度集成

---

*最后更新：2026-03-18*
