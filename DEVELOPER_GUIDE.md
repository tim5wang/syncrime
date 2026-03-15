# SyncRime 开发者指南

本指南帮助开发者快速上手 SyncRime 项目开发。

---

## 📋 目录

1. [项目结构](#项目结构)
2. [开发环境](#开发环境)
3. [构建系统](#构建系统)
4. [代码组织](#代码组织)
5. [调试技巧](#调试技巧)
6. [测试指南](#测试指南)

---

## 项目结构

### 根目录
```
syncrime/
├── android-client/          # Android 客户端应用
├── trime-plugin/            # Trime 输入法插件
├── docs/                    # 项目文档
├── README.md                # 项目说明
├── CONTRIBUTING.md          # 贡献指南
└── DEVELOPER_GUIDE.md       # 开发者指南（本文件）
```

### Android 客户端详细结构
```
android-client/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/syncrime/android/
│   │   │   │   ├── SyncRimeApplication.kt      # 应用入口
│   │   │   │   │
│   │   │   │   ├── presentation/               # UI 层
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── navigation/             # 导航
│   │   │   │   │   ├── ui/
│   │   │   │   │   │   ├── main/               # 主界面
│   │   │   │   │   │   ├── settings/           # 设置界面
│   │   │   │   │   │   ├── statistics/         # 统计界面
│   │   │   │   │   │   ├── profile/            # 个人界面
│   │   │   │   │   │   └── intelligence/       # 智能设置
│   │   │   │   │   └── theme/                  # 主题
│   │   │   │   │
│   │   │   │   ├── domain/                     # 业务逻辑层
│   │   │   │   │   ├── model/                  # 数据模型
│   │   │   │   │   ├── usecase/                # 用例
│   │   │   │   │   └── repository/             # 仓库接口
│   │   │   │   │
│   │   │   │   ├── data/                       # 数据层
│   │   │   │   │   ├── local/                  # 本地数据 (Room)
│   │   │   │   │   ├── remote/                 # 远程 API (Retrofit)
│   │   │   │   │   └── repository/             # 仓库实现
│   │   │   │   │
│   │   │   │   ├── intelligence/               # 智能引擎
│   │   │   │   │   ├── AndroidIntelligenceEngineManager.kt
│   │   │   │   │   └── engines/                # 各引擎实现
│   │   │   │   │
│   │   │   │   ├── sync/                       # 同步服务
│   │   │   │   │   ├── SyncWorker.kt
│   │   │   │   │   └── SyncScheduler.kt
│   │   │   │   │
│   │   │   │   ├── notification/               # 通知系统
│   │   │   │   │   └── SyncRimeNotificationManager.kt
│   │   │   │   │
│   │   │   │   └── permission/                 # 权限管理
│   │   │   │       └── PermissionManager.kt
│   │   │   │
│   │   │   ├── res/                            # 资源文件
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/                               # 单元测试
│   │   └── androidTest/                        # 仪器测试
│   │
│   └── build.gradle                            # 模块构建配置
│
├── gradle/                                     # Gradle 包装器
├── build.gradle                                # 项目构建配置
├── settings.gradle                             # 项目设置
└── gradle.properties                           # Gradle 属性
```

### Trime 插件详细结构
```
trime-plugin/
├── app/
│   ├── src/main/
│   │   ├── java/com/syncrime/trime/plugin/
│   │   │   ├── SyncRimePlugin.kt              # 插件主类
│   │   │   ├── InputSessionManager.kt         # 输入会话管理
│   │   │   ├── SyncManager.kt                 # 同步管理
│   │   │   ├── PluginConfigManager.kt         # 配置管理
│   │   │   │
│   │   │   └── intelligence/                  # 智能引擎
│   │   │       ├── IntelligentRecommendationEngine.kt
│   │   │       ├── ContextAwareInputEngine.kt
│   │   │       ├── PersonalizedLearningEngine.kt
│   │   │       ├── IntelligentCorrectionEngine.kt
│   │   │       ├── SemanticAnalysisEngine.kt
│   │   │       └── MultilingualIntelligentEngine.kt
│   │   │
│   │   ├── cpp/                               # C++ 原生代码
│   │   │   ├── src/
│   │   │   │   ├── syncrime_plugin.cpp
│   │   │   │   ├── jni_bridge.cpp
│   │   │   │   └── data_collector.cpp
│   │   │   └── include/
│   │   │       ├── syncrime_plugin.h
│   │   │       └── data_collector.h
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── test/                                  # 测试
│
└── config/                                    # 配置文件
```

---

## 开发环境

### 必需软件

| 软件 | 版本 | 下载链接 |
|------|------|----------|
| Android Studio | Arctic Fox+ | [官网](https://developer.android.com/studio) |
| JDK | 11+ | [Adoptium](https://adoptium.net/) |
| Android SDK | 31+ | Android Studio 内置 |
| Git | 2.x+ | [官网](https://git-scm.com/) |

### Android Studio 配置

1. **安装必要插件**
   - Kotlin
   - Android Support

2. **配置 SDK**
   ```
   Tools → SDK Manager
   - Android SDK Platform 31+
   - Android SDK Build-Tools 31+
   - Android Emulator (可选)
   ```

3. **配置 Gradle**
   ```
   File → Settings → Build, Execution, Deployment → Build Tools → Gradle
   - Gradle JDK: JDK 11+
   ```

---

## 构建系统

### Gradle 任务

```bash
# 清理构建
./gradlew clean

# 编译 Debug 版本
./gradlew assembleDebug

# 编译 Release 版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew :app:testDebugUnitTest

# 查看依赖
./gradlew :app:dependencies

# 检查代码格式
./gradlew :app:ktlintCheck

# 格式化代码
./gradlew :app:ktlintFormat
```

### 构建变体

| 变体 | 描述 | 用途 |
|------|------|------|
| `debug` | Debug 版本 | 开发调试 |
| `release` | Release 版本 | 生产发布 |
| `benchmark` | 性能测试版本 | 性能分析 |

---

## 代码组织

### 包命名规范

```
com.syncrime.android.<layer>.<feature>

示例:
com.syncrime.android.presentation.ui.main
com.syncrime.android.data.local.dao
com.syncrime.android.domain.usecase
```

### 文件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| Activity | `*Activity.kt` | `MainActivity.kt` |
| Fragment | `*Fragment.kt` | `SettingsFragment.kt` |
| ViewModel | `*ViewModel.kt` | `MainViewModel.kt` |
| UseCase | `*UseCase.kt` | `GetInputDataUseCase.kt` |
| Repository | `*Repository.kt` | `InputRepository.kt` |
| DAO | `*Dao.kt` | `InputDataDao.kt` |
| Entity | `*Entity.kt` | `InputDataEntity.kt` |
| Model | `*Model.kt` | `InputData.kt` |

### 依赖注入 (Hilt)

```kotlin
// Application
@HiltAndroidApp
class SyncRimeApplication : Application()

// ViewModel
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel()

// Repository
@Singleton
class MainRepository @Inject constructor(
    private val dao: MainDao,
    private val api: MainApi
)

// UseCase
class GetInputDataUseCase @Inject constructor(
    private val repository: MainRepository
) {
    operator fun invoke(): Flow<List<InputData>> {
        return repository.getInputData()
    }
}
```

---

## 调试技巧

### Logcat 过滤

```bash
# 按标签过滤
adb logcat -s SyncRimePlugin

# 按优先级过滤
adb logcat *:E

# 保存日志
adb logcat -d > log.txt
```

### Android Studio Debug

1. **设置断点**
   - 点击行号左侧
   - 条件断点：右键断点 → More

2. **调试操作**
   - `F7` - Step Into
   - `F8` - Step Over
   - `F9` - Resume
   - `Shift+F8` - Step Out

3. **查看变量**
   - Variables 窗口
   - Evaluate Expression (Alt+F8)

### 网络调试

```kotlin
// OkHttp 日志拦截器
val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .addInterceptor(logging)
    .build()
```

### 数据库调试

```kotlin
// Room 允许主线程查询（仅调试）
Room.databaseBuilder(context, AppDatabase::class.java, "db")
    .allowMainThreadQueries() // 仅调试使用！
    .build()
```

---

## 测试指南

### 测试类型

| 类型 | 位置 | 工具 |
|------|------|------|
| 单元测试 | `src/test/` | JUnit, MockK |
| 仪器测试 | `src/androidTest/` | Espresso, Hilt |
| UI 测试 | `src/androidTest/` | Compose Testing |

### 单元测试示例

```kotlin
class MainViewModelTest {
    
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: MockMainRepository
    
    @Before
    fun setup() {
        repository = MockMainRepository()
        viewModel = MainViewModel(repository)
    }
    
    @Test
    fun `loadData should emit success state`() = runTest {
        // Given
        val expectedData = listOf(InputData("test"))
        repository inputData = expectedData
        
        // When
        viewModel.loadData()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        assertEquals(expectedData, (state as UiState.Success).data)
    }
}
```

### Compose UI 测试

```kotlin
@Test
fun mainScreen_displaysCorrectTitle() {
    composeTestRule.setContent {
        MainScreen()
    }
    
    composeTestRule
        .onNodeWithText("SyncRime")
        .assertIsDisplayed()
}
```

### 运行测试

```bash
# 所有测试
./gradlew test

# 单元测试
./gradlew testDebugUnitTest

# 仪器测试
./gradlew connectedDebugAndroidTest

# 特定测试类
./gradlew testDebugUnitTest --tests MainViewModelTest
```

---

## 性能优化

### 内存优化

```kotlin
// 使用 StateFlow 替代 LiveData
private val _state = MutableStateFlow(UiState())
val state: StateFlow<UiState> = _state.asStateFlow()

// 避免内存泄漏
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state ->
            // 更新 UI
        }
    }
}
```

### 网络优化

```kotlin
// 使用缓存
@GET("input-data")
suspend fun getInputData(
    @Header("If-None-Match") etag: String? = null
): Response<List<InputData>>

// 批量请求
@POST("batch")
suspend fun batchRequest(@Body requests: List<Request>): List<Response>
```

### 数据库优化

```kotlin
// 使用索引
@Entity(indices = [Index(value = ["userId"])])
data class InputDataEntity(
    val userId: String,
    // ...
)

// 使用事务
@Transaction
suspend fun insertDataWithRelations(data: InputData) {
    // 原子操作
}
```

---

## 常见问题

### Q: 如何调试 JNI 代码？
A: 使用 Android Studio 的 Native Debug 功能，需要在 build.gradle 中启用：
```gradle
android {
    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments "-DANDROID_STL=c++_shared"
            }
        }
    }
}
```

### Q: 如何处理数据库迁移？
A: 使用 Room 的 Migration：
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE input_data ADD COLUMN new_column TEXT")
    }
}

Room.databaseBuilder(context, AppDatabase::class.java, "db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

### Q: 如何测试同步功能？
A: 使用 WorkManager 的 TestWorkerBuilder：
```kotlin
@Test
fun syncWorker_executesSuccessfully() {
    val worker = TestWorkerBuilder<SyncWorker>(
        context = ApplicationProvider.getApplicationContext()
    ).build()
    
    val result = worker.doWork()
    
    assertEquals(Result.success(), result)
}
```

---

## 资源

- [Android 开发者文档](https://developer.android.com/)
- [Kotlin 文档](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose 指南](https://developer.android.com/jetpack/compose)
- [Hilt 文档](https://dagger.dev/hilt/)
- [Room 文档](https://developer.android.com/training/data-storage/room)
- [Coroutines 指南](https://kotlinlang.org/docs/coroutines-overview.html)

---

*最后更新：2026-03-16*
