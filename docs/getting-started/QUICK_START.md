# SyncRime 快速使用指南

## 🚨 重要说明

**SyncRime 当前版本通过 Android 无障碍服务实现输入采集，不需要 Trime 插件！**

Trime 输入法只是作为一个**被监听的对象**，SyncRime 会监听所有输入法的输入事件。

---

## 📱 安装步骤（5 分钟搞定）

### 步骤 1: 安装 Trime 输入法

**方法 A: 直接下载**
1. 手机浏览器打开：https://github.com/osfans/trime/releases
2. 下载 `trime-3.4.2-arm64-v8a.apk`
3. 点击安装

**方法 B: 酷安下载（更快）**
1. 打开酷安：https://www.coolapk.com/apk/com.osfans.trime
2. 下载安装

### 步骤 2: 安装 SyncRime

```bash
# 电脑连接手机后执行
adb install ~/claw_workspaces/tim/syncrime/SyncRime-v1.0-debug.apk
```

或者直接传输 APK 到手机安装。

### 步骤 3: 启用 Trime 输入法

1. 手机 **设置 → 系统 → 语言和输入法**
2. **虚拟键盘 → 管理键盘**
3. **开启 Trime**
4. **切换当前输入法** → 选择 Trime

### 步骤 4: 启用 SyncRime 无障碍服务

1. 打开 **SyncRime** App
2. 点击 **"开始采集"** 按钮
3. 系统会跳转到无障碍设置
4. 找到 **"SyncRime 输入采集"** 并开启
5. 返回 SyncRime App

---

## ✅ 验证是否工作

### 测试 1: 检查服务状态

打开 SyncRime App，主界面应该显示：
- ✅ **"正在采集输入数据..."**
- ✅ 输入计数在增加

### 测试 2: 实际输入测试

1. 打开微信/备忘录/任何可以输入的地方
2. 用 Trime 输入法输入一些文字
3. 回到 SyncRime App
4. 查看 **"今日输入"** 统计，应该有数据

### 测试 3: 查看日志（可选）

```bash
# 连接手机后执行
adb logcat -s SyncRime

# 应该看到类似日志:
# I/InputCaptureService: 无障碍服务已连接
# I/InputCaptureService: 创建新会话：xxx, 应用：com.tencent.mm
# D/InputCaptureService: 收到事件：TYPE_VIEW_TEXT_CHANGED
```

---

## 🔧 常见问题

### Q1: 为什么找不到插件配置？

**A**: SyncRime 当前版本**没有 Trime 插件**，是通过无障碍服务工作的！

**工作原理**：
```
SyncRime App (无障碍服务) → 监听系统输入事件 → 采集数据
                            ↑
                    任何输入法 (包括 Trime)
```

### Q2: 为什么没有同步？

**A**: 当前版本的同步功能是**本地同步**，云端同步需要后端服务（Phase 5）。

**当前同步状态**：
- ✅ 本地数据库存储（Room）
- ✅ 后台定时任务（WorkManager）
- ⏳ 云端同步（待实现）

**查看本地数据**：
```bash
# 查看数据库文件
adb shell "ls -la /data/data/com.syncrime.android.debug/databases/"

# 导出数据库
adb pull /data/data/com.syncrime.android.debug/databases/syncrime.db
```

### Q3: 无障碍服务无法开启？

**解决方法**：
1. 设置 → 无障碍 → 找到 "SyncRime 输入采集"
2. 如果找不到，重启手机
3. 重新打开 SyncRime App

### Q4: 采集不到输入？

**检查清单**：
- [ ] SyncRime 无障碍服务已开启
- [ ] Trime 是当前输入法
- [ ] 不是密码输入框（会自动过滤）
- [ ] 输入内容超过 1 个字符

**调试步骤**：
```bash
# 查看无障碍服务状态
adb shell settings get secure enabled_accessibility_services

# 应该包含: com.syncrime.android.debug/.accessibility.InputCaptureService
```

---

## 📊 功能状态

### 已实现 ✅
- ✅ 输入内容采集（无障碍服务）
- ✅ 本地数据库存储（Room）
- ✅ 统计界面（今日输入/会话数）
- ✅ 后台同步框架（WorkManager）
- ✅ 通知管理
- ✅ 权限管理

### 部分实现 🔄
- 🔄 数据同步（仅本地，云端待实现）
- 🔄 智能推荐（框架完成，模型待集成）
- 🔄 个性化学习（基础功能完成）

### 待实现 ⏳
- ⏳ Trime 插件原生集成
- ⏳ 云端同步服务
- ⏳ 完整智能推荐引擎
- ⏳ 多语言支持

---

## 🛠️ 高级配置

### 修改采集配置

编辑配置文件（需要 root）：
```
/data/data/com.syncrime.android.debug/shared_prefs/config.xml
```

**配置项**：
```xml
<config>
    <!-- 采集模式：smart, all, filtered -->
    <capture_mode>smart</capture_mode>
    
    <!-- 会话超时（毫秒） -->
    <session_timeout>300000</session_timeout>
    
    <!-- 最小输入长度 -->
    <min_input_length>1</min_input_length>
    
    <!-- 同步间隔（秒） -->
    <sync_interval>300</sync_interval>
</config>
```

### 手动触发同步

```bash
# 通过 ADB 触发 WorkManager
adb shell am broadcast -a com.syncrime.android.debug.SYNC_NOW
```

### 导出数据

```bash
# 导出数据库
adb pull /data/data/com.syncrime.android.debug/databases/syncrime.db ~/syncrime_backup.db

# 查看数据
sqlite3 ~/syncrime_backup.db "SELECT * FROM input_records LIMIT 10;"
```

---

## 📝 使用技巧

### 技巧 1: 查看实时统计

打开 SyncRime App → 主界面 → 查看：
- 今日输入次数
- 今日会话数
- 最近使用应用

### 技巧 2: 保护隐私

在 SyncRime App 中设置：
- 排除密码输入框（默认开启）
- 排除敏感应用（银行/支付类）
- 设置数据保留天数

### 技巧 3: 优化性能

如果感觉耗电：
1. 减少采集频率
2. 关闭实时通知
3. 设置 WiFi 同步

---

## 🔗 相关文档

- [Trime 下载指南](./TRIME_DOWNLOAD.md)
- [Trime 集成指南](./TRIME_INTEGRATION.md)
- [用户指南](../user-guide/USER_GUIDE.md)
- [架构设计](../architecture/system-integration-architecture.md)

---

## 💡 总结

**当前版本可以做什么**：
1. ✅ 采集所有输入法的输入内容
2. ✅ 本地存储和管理
3. ✅ 查看统计数据
4. ✅ 后台自动同步（本地）

**下一步计划**：
1. 云端同步服务（Phase 5）
2. 智能推荐引擎（Phase 2 完整实现）
3. Trime 插件深度集成

---

**有问题？** 查看日志或联系开发团队！

*最后更新：2026-03-18*
