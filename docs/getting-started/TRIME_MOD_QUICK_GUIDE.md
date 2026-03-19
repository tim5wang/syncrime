# SyncRime 修改 Trime 源码 - 快速实施指南

## 🎯 目标

**1 周内**完成 Trime 输入法源码修改，内置 SyncRime 输入采集功能！

---

## 📋 实施步骤

### 第 1 步：Fork Trime 仓库（10 分钟）

1. 打开 https://github.com/osfans/trime
2. 点击右上角 **Fork**
3. 等待 Fork 完成

### 第 2 步：克隆到本地（30 分钟）

```bash
# 克隆你的 Fork
cd ~/workspace
git clone https://github.com/YOUR_USERNAME/trime.git
cd trime

# 初始化子模块（重要！）
git submodule update --init --recursive

# 创建功能分支
git checkout -b feature/syncrime
```

### 第 3 步：添加 SyncRime 代码（2-3 小时）

#### 3.1 创建 SyncRime 管理器

```bash
mkdir -p app/src/main/java/com/osfans/trime/ime/syncrime
```

创建文件 `app/src/main/java/com/osfans/trime/ime/syncrime/SyncRimeManager.kt`:

```kotlin
package com.osfans.trime.ime.syncrime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

class SyncRimeManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SyncRime"
        
        @Volatile
        private var instance: SyncRimeManager? = null
        
        fun getInstance(context: Context): SyncRimeManager {
            return instance ?: synchronized(this) {
                instance ?: SyncRimeManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isCapturing = true
    
    fun initialize() {
        Log.i(TAG, "SyncRime 初始化完成")
    }
    
    fun captureInput(text: String, app: String, isPassword: Boolean) {
        if (!isCapturing || isPassword || text.isBlank()) return
        
        scope.launch {
            // TODO: 保存到数据库
            Log.d(TAG, "采集：$text (${text.length} 字符) in $app")
        }
    }
    
    fun cleanup() {
        isCapturing = false
        scope.cancel()
    }
}
```

#### 3.2 创建数据库实体

```bash
mkdir -p app/src/main/java/com/osfans/trime/data/db
```

创建 `app/src/main/java/com/osfans/trime/data/db/InputRecord.kt`:

```kotlin
package com.osfans.trime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "syncrime_input")
data class InputRecord(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val application: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSensitive: Boolean = false
)
```

### 第 4 步：集成到 Trime 核心（2-3 小时）

#### 4.1 修改 TrimeInputMethodService.kt

编辑 `app/src/main/java/com/osfans/trime/ime/core/TrimeInputMethodService.kt`:

**在文件开头添加导入**（在 package 后面）：
```kotlin
import com.osfans.trime.ime.syncrime.SyncRimeManager
```

**在类中添加成员**（在其他变量后面）：
```kotlin
// SyncRime 管理器
private val syncRimeManager by lazy { 
    SyncRimeManager.getInstance(this) 
}
```

**修改 onCreate 方法**：
```kotlin
override fun onCreate() {
    super.onCreate()
    // 初始化 SyncRime
    syncRimeManager.initialize()
}
```

**修改 onDestroy 方法**：
```kotlin
override fun onDestroy() {
    // 清理 SyncRime
    syncRimeManager.cleanup()
    super.onDestroy()
}
```

**修改 commitText 方法**（找到这个方法并修改）：
```kotlin
override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
    val result = super.commitText(text, newCursorPosition)
    
    // 采集输入
    val isPassword = currentInputEditorInfo?.let { 
        it.inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 
    } ?: false
    
    syncRimeManager.captureInput(
        text = text.toString(),
        app = packageName,
        isPassword = isPassword
    )
    
    return result
}
```

### 第 5 步：添加依赖（30 分钟）

编辑 `app/build.gradle.kts`，在 `dependencies` 中添加：

```kotlin
// Room 数据库
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// 协程
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

**在 plugins 中添加**：
```kotlin
id("com.google.devtools.ksp") version "1.9.0-1.0.13"
```

### 第 6 步：编译测试（1-2 小时）

```bash
# 编译 Debug 版本
./gradlew assembleDebug

# 如果编译失败，根据错误修复
# 常见问题：
# - 缺少依赖 → 添加依赖
# - 导入错误 → 检查包名
# - 类型错误 → 检查代码
```

### 第 7 步：安装测试（30 分钟）

```bash
# 连接手机后安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 查看日志
adb logcat -s SyncRime

# 测试输入
# 1. 切换到 Trime 输入法
# 2. 在微信/备忘录输入文字
# 3. 查看日志输出
```

**期望日志**：
```
I/SyncRime: SyncRime 初始化完成
D/SyncRime: 采集：你好 (2 字符) in com.tencent.mm
D/SyncRime: 采集：测试 (2 字符) in com.android.browser
```

---

## ✅ 验证清单

- [ ] Trime 输入法正常工作
- [ ] 日志显示输入采集
- [ ] 密码输入框不采集
- [ ] 无明显性能问题
- [ ] 输入法不崩溃

---

## 🐛 常见问题

### Q1: 编译失败 "Unresolved reference"

**解决**：
```bash
# 清理并重新编译
./gradlew clean
./gradlew assembleDebug
```

### Q2: 输入法崩溃

**解决**：
```bash
# 查看崩溃日志
adb logcat -s AndroidRuntime

# 通常是空指针或类型错误
# 检查 syncRimeManager 是否初始化
```

### Q3: 没有采集日志

**解决**：
1. 检查是否切换到 Trime 输入法
2. 检查日志过滤器：`adb logcat -s SyncRime`
3. 确认不是密码输入框

---

## 📊 下一步

### 基础版完成后（1 周）

1. ✅ 输入采集
2. ✅ 日志输出
3. ⏳ 本地存储（Room）
4. ⏳ 统计界面

### 完整版（2-4 周）

1. 完整的 Room 数据库
2. 后台同步服务
3. 统计 Activity
4. 设置界面
5. 云端同步

---

## 📝 代码位置总结

| 文件 | 路径 | 作用 |
|------|------|------|
| SyncRimeManager | `app/src/main/java/com/osfans/trime/ime/syncrime/` | 采集管理器 |
| InputRecord | `app/src/main/java/com/osfans/trime/data/db/` | 数据实体 |
| TrimeInputMethodService | `app/src/main/java/com/osfans/trime/ime/core/` | 核心服务（修改） |
| build.gradle.kts | `app/build.gradle.kts` | 依赖配置（修改） |

---

## 🚀 快速命令

```bash
# 编译
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 查看日志
adb logcat -s SyncRime

# 清除数据（重新测试）
adb shell pm clear com.osfans.trime
```

---

## 💡 提示

1. **先实现基础功能** - 能采集就行，存储后续再加
2. **多查看日志** - 问题都在日志里
3. **小步快跑** - 每次只改一点，编译测试通过再继续
4. **参考 SyncRime** - 数据采集逻辑可以复用

---

*最后更新：2026-03-18*
*预计完成时间：1 周*
