# SyncRime 集成 Trime 源码修改方案

## 🎯 背景

**问题**：Android 无障碍权限获取困难，用户需要手动配置，体验差。

**解决方案**：直接修改 Trime 输入法源码，内置 SyncRime 功能，无需额外权限！

---

## 📊 可行性分析

### 方案对比

| 方案 | 优势 | 劣势 | 难度 |
|------|------|------|------|
| **无障碍服务** | 无需修改输入法 | 权限难获取，不稳定 | ⭐⭐ |
| **Trime 插件** | 深度集成 | 需要编译，仅支持 Trime | ⭐⭐⭐⭐ |
| **修改 Trime 源码** | 原生集成，体验最佳 | 需要维护 fork 版本 | ⭐⭐⭐ |

### 推荐方案：修改 Trime 源码

**理由**：
1. ✅ **无需额外权限** - 输入法本身就有输入事件
2. ✅ **用户体验最佳** - 安装即用，无需配置
3. ✅ **性能最优** - 无中间层，直接采集
4. ✅ **功能最强** - 可以访问输入法内部状态

---

## 🏗️ 技术架构

### 当前 Trime 架构

```
TrimeInputMethodService (核心服务)
    ↓
RimeSession (Rime 引擎)
    ↓
Keyboard (键盘布局)
    ↓
InputView (输入视图)
```

### 集成 SyncRime 后的架构

```
TrimeInputMethodService (核心服务)
    ├─ SyncRimeManager (新增)
    │   ├─ InputCollector (采集器)
    │   ├─ LocalStorage (本地存储)
    │   └─ SyncWorker (同步任务)
    │
    ├─ RimeSession (Rime 引擎)
    ├─ Keyboard (键盘布局)
    └─ InputView (输入视图)
```

---

## 🔧 实现方案

### 方案 A: 轻量级集成（推荐）

**修改点最少，快速实现**

#### 1. 添加 SyncRime 模块

在 `app/build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    // 现有依赖...
    
    // SyncRime 集成
    implementation(project(":syncrime")) // 或编译为 AAR
}
```

#### 2. 创建 SyncRimeManager

新建文件 `app/src/main/java/com/osfans/trime/ime/syncrime/SyncRimeManager.kt`:

```kotlin
package com.osfans.trime.ime.syncrime

import android.content.Context
import android.util.Log
import com.osfans.trime.data.db.AppDatabase
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

/**
 * SyncRime 管理器
 * 负责输入采集、存储和同步
 */
class SyncRimeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SyncRimeManager"
        
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
    private val database = AppDatabase.getDatabase(context)
    private val inputDao = database.inputDao()
    
    private val sessionId = AtomicLong(System.currentTimeMillis())
    private var isCapturing = false
    
    /**
     * 初始化
     */
    fun initialize() {
        Log.i(TAG, "SyncRime 初始化")
        isCapturing = true
    }
    
    /**
     * 采集输入
     */
    fun captureInput(
        text: String,
        packageName: String,
        isPassword: Boolean = false
    ) {
        if (!isCapturing) return
        
        // 过滤密码等敏感输入
        if (isPassword || text.isBlank()) return
        
        scope.launch {
            try {
                val inputRecord = InputRecordEntity(
                    id = System.currentTimeMillis(),
                    sessionId = sessionId.get(),
                    content = text,
                    application = packageName,
                    timestamp = System.currentTimeMillis(),
                    isSensitive = false
                )
                
                inputDao.insert(inputRecord)
                Log.d(TAG, "采集输入：$text (${text.length} 字符)")
                
            } catch (e: Exception) {
                Log.e(TAG, "采集失败", e)
            }
        }
    }
    
    /**
     * 开始新会话
     */
    fun startSession(packageName: String) {
        scope.launch {
            sessionId.set(System.currentTimeMillis())
            Log.d(TAG, "新会话开始：$packageName")
        }
    }
    
    /**
     * 结束会话
     */
    fun endSession() {
        scope.launch {
            Log.d(TAG, "会话结束")
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        isCapturing = false
        scope.cancel()
        Log.i(TAG, "SyncRime 清理完成")
    }
}
```

#### 3. 集成到 TrimeInputMethodService

修改 `TrimeInputMethodService.kt`:

```kotlin
// 在类开头添加
import com.osfans.trime.ime.syncrime.SyncRimeManager

// 在类中添加
class TrimeInputMethodService : LifecycleInputMethodService() {
    
    // 添加 SyncRime 管理器
    private val syncRimeManager by lazy { 
        SyncRimeManager.getInstance(this) 
    }
    
    override fun onCreate() {
        super.onCreate()
        // 初始化 SyncRime
        syncRimeManager.initialize()
    }
    
    override fun onDestroy() {
        // 清理 SyncRime
        syncRimeManager.cleanup()
        super.onDestroy()
    }
    
    // 在输入提交时采集
    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        val result = super.commitText(text, newCursorPosition)
        
        // 采集输入
        val packageName = packageName
        val isPassword = isSensitiveInput
        
        syncRimeManager.captureInput(
            text = text.toString(),
            packageName = packageName,
            isPassword = isPassword
        )
        
        return result
    }
    
    // 在焦点变化时开始/结束会话
    override fun onStartInputView(
        attribute: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(attribute, restarting)
        
        // 开始新会话
        syncRimeManager.startSession(packageName)
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        // 结束会话
        syncRimeManager.endSession()
        super.onFinishInputView(finishingInput)
    }
    
    // 判断是否敏感输入（密码等）
    private val isSensitiveInput: Boolean
        get() {
            val inputType = currentInputEditorInfo?.inputType ?: InputType.TYPE_NULL
            return inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
                   inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0 ||
                   inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0
        }
}
```

#### 4. 添加数据实体

新建 `app/src/main/java/com/osfans/trime/data/db/InputRecordEntity.kt`:

```kotlin
package com.osfans.trime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "syncrime_input_records")
data class InputRecordEntity(
    @PrimaryKey
    val id: Long,
    val sessionId: Long,
    val content: String,
    val application: String,
    val timestamp: Long,
    val isSensitive: Boolean = false,
    val metadata: String? = null
)
```

#### 5. 添加 DAO

新建 `app/src/main/java/com/osfans/trime/data/db/InputRecordDao.kt`:

```kotlin
package com.osfans.trime.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InputRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InputRecordEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InputRecordEntity>)
    
    @Query("SELECT * FROM syncrime_input_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 100): Flow<List<InputRecordEntity>>
    
    @Query("SELECT COUNT(*) FROM syncrime_input_records WHERE date(timestamp/1000) = date('now')")
    fun getTodayCount(): Flow<Int>
    
    @Query("SELECT application, COUNT(*) as count FROM syncrime_input_records WHERE date(timestamp/1000) = date('now') GROUP BY application")
    fun getTodayAppStats(): Flow<List<AppStat>>
    
    @Query("DELETE FROM syncrime_input_records WHERE timestamp < :timestamp")
    suspend fun deleteBefore(timestamp: Long): Int
    
    data class AppStat(
        val application: String,
        val count: Int
    )
}
```

#### 6. 更新数据库

修改 `AppDatabase.kt`:

```kotlin
@Database(
    entities = [
        // 现有实体...
        InputRecordEntity::class  // 新增
    ],
    version = 2  // 升级版本号
)
abstract class AppDatabase : RoomDatabase() {
    
    // 新增 DAO
    abstract fun inputDao(): InputRecordDao
    
    // 现有 DAO...
}
```

#### 7. 添加设置界面

在设置中添加 SyncRime 开关：

```kotlin
// 在设置界面添加
@Composable
fun SyncRimeSettings() {
    var enabled by remember { mutableStateOf(true) }
    
    Switch(
        checked = enabled,
        onCheckedChange = { 
            enabled = it
            // 保存设置
        }
    ) {
        Text("启用输入采集")
    }
}
```

---

### 方案 B: 深度集成（完整功能）

**在方案 A 基础上增加**：

1. **完整的 Room 数据库**
2. **后台同步服务**
3. **统计界面**
4. **云端同步**

#### 额外工作量：
- 复制 SyncRime Android 客户端的 data 层
- 添加 WorkManager 后台任务
- 创建独立的统计 Activity
- 实现网络同步模块

---

## 📝 修改步骤

### 步骤 1: Fork Trime 仓库

```bash
# 1. 在 GitHub 上 Fork
# https://github.com/osfans/trime → 点击 Fork

# 2. 克隆到本地
git clone https://github.com/YOUR_USERNAME/trime.git
cd trime

# 3. 创建分支
git checkout -b feature/syncrime-integration
```

### 步骤 2: 添加 SyncRime 代码

```bash
# 复制 SyncRime 的 data 层
cp -r ~/claw_workspaces/tim/syncrime/android-client/app/src/main/java/com/syncrime/android/data \
      app/src/main/java/com/osfans/trime/data/syncrime

# 创建 SyncRimeManager
mkdir -p app/src/main/java/com/osfans/trime/ime/syncrime
# 编辑 SyncRimeManager.kt
```

### 步骤 3: 修改 TrimeInputMethodService

```bash
# 编辑核心服务文件
vim app/src/main/java/com/osfans/trime/ime/core/TrimeInputMethodService.kt
```

### 步骤 4: 更新数据库 Schema

```bash
# 添加新的 Entity 和 DAO
# 升级数据库版本
```

### 步骤 5: 编译测试

```bash
# 编译 Debug 版本
./gradlew assembleDebug

# 安装到手机
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 测试输入采集
adb logcat -s SyncRimeManager
```

### 步骤 6: 打包发布

```bash
# 编译 Release 版本
./gradlew assembleRelease

# APK 位置
app/build/outputs/apk/release/app-release.apk
```

---

## ⚖️ 优缺点分析

### 优势

1. **无需额外权限** - 输入法原生功能
2. **用户体验好** - 安装即用
3. **性能最优** - 无中间层
4. **功能完整** - 可访问输入法内部状态
5. **稳定可靠** - 不受系统限制

### 劣势

1. **需要维护 Fork** - Trime 更新需要合并
2. **编译复杂** - 需要 Android NDK 环境
3. **包体积增大** - 增加数据库和同步功能
4. **仅支持 Trime** - 不支持其他输入法

### 风险

1. **Trime 更新** - 需要定期合并上游代码
2. **代码冲突** - 核心文件修改可能冲突
3. **维护成本** - 需要持续跟进 Trime 开发

---

## 📊 工作量估算

| 任务 | 工作量 | 说明 |
|------|--------|------|
| 基础集成（方案 A） | 1-2 天 | 采集 + 存储 |
| 完整功能（方案 B） | 3-5 天 | + 同步 + 统计 |
| 测试调优 | 1-2 天 | 功能测试 + 性能优化 |
| 文档编写 | 0.5 天 | 安装和使用文档 |
| **总计** | **5.5-9.5 天** | |

---

## 🚀 推荐实施路径

### Phase 1: 快速验证（1-2 天）

1. Fork Trime
2. 实现方案 A（基础采集）
3. 编译测试
4. 验证功能

### Phase 2: 完整功能（3-5 天）

1. 实现方案 B（完整功能）
2. 添加统计界面
3. 实现后台同步
4. 性能优化

### Phase 3: 发布测试（1-2 天）

1. 打包 Release
2. 用户测试
3. 收集反馈
4. 修复问题

### Phase 4: 持续维护

1. 定期合并 Trime 上游
2. 功能迭代
3. Bug 修复

---

## 📦 交付物

1. **Fork 的 Trime 仓库** - GitHub 公开
2. **修改说明文档** - 详细的修改点
3. **编译好的 APK** - 可直接安装
4. **使用文档** - 安装和配置指南

---

## 💡 建议

### 短期（1 周内）
- 实现方案 A（基础采集）
- 验证可行性
- 发布测试版

### 中期（1 个月内）
- 实现方案 B（完整功能）
- 优化性能
- 正式发布

### 长期
- 维护 Fork 版本
- 跟进 Trime 更新
- 持续优化

---

## 🔗 相关文件

- [Trime 源码](https://github.com/osfans/trime)
- [Trime 开发文档](https://github.com/osfans/trime/wiki)
- [SyncRime Android 客户端](~/claw_workspaces/tim/syncrime/android-client)
- [贡献指南](https://github.com/osfans/trime/blob/develop/CONTRIBUTING.md)

---

*方案制定时间：2026-03-18*
*版本：v1.0*
