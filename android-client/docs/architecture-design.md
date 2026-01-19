# SyncRime Android 客户端架构设计

## 📋 项目概述

SyncRime Android 客户端是智能化输入管理系统的移动端实现，集成了 Phase 2 开发的所有智能化功能，为用户提供完整的输入管理和同步体验。

## 🏗️ 整体架构

### 架构模式
- **MVVM + Clean Architecture** - 清晰的分层架构
- **Repository Pattern** - 数据访问抽象
- **Dependency Injection** - 依赖注入
- **Reactive Programming** - 响应式编程

### 技术栈
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Android Architecture Components
- **Networking**: Retrofit + OkHttp
- **Database**: Room + SQLite
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Testing**: JUnit, Espresso, MockK

## 📁 项目结构

```
app/
├── src/main/java/com/syncrime/android/
│   ├── data/                    # 数据层
│   │   ├── local/              # 本地数据源
│   │   │   ├── database/       # Room 数据库
│   │   │   ├── preferences/    # SharedPreferences
│   │   │   └── cache/          # 缓存管理
│   │   ├── remote/             # 远程数据源
│   │   │   ├── api/            # API 接口
│   │   │   ├── dto/            # 数据传输对象
│   │   │   └── network/        # 网络配置
│   │   ├── repository/         # 数据仓库
│   │   └── mapper/             # 数据映射
│   ├── domain/                  # 业务逻辑层
│   │   ├── model/              # 领域模型
│   │   ├── repository/         # 仓库接口
│   │   ├── usecase/            # 用例
│   │   └── service/            # 领域服务
│   ├── presentation/           # 表现层
│   │   ├── ui/                 # UI 组件
│   │   │   ├── main/           # 主界面
│   │   │   ├── settings/       # 设置界面
│   │   │   ├── statistics/     # 统计界面
│   │   │   ├── profile/        # 个人资料
│   │   │   ├── components/     # 通用组件
│   │   │   └── theme/          # 主题样式
│   │   ├── viewmodel/          # ViewModel
│   │   ├── state/              # UI 状态
│   │   └── navigation/         # 导航
│   ├── intelligence/           # 智能化功能
│   │   ├── recommendation/      # 智能推荐
│   │   ├── context/             # 上下文感知
│   │   ├── learning/            # 个性化学习
│   │   ├── correction/          # 智能纠错
│   │   ├── semantic/            # 语义分析
│   │   └── multilingual/        # 多语言
│   ├── sync/                    # 同步功能
│   │   ├── service/             # 同步服务
│   │   ├── worker/              # 后台任务
│   │   ├── conflict/            # 冲突解决
│   │   └── encryption/          # 加密处理
│   ├── core/                    # 核心功能
│   │   ├── base/                # 基础类
│   │   ├── utils/               # 工具类
│   │   ├── constants/           # 常量定义
│   │   ├── extension/           # 扩展函数
│   │   └── di/                  # 依赖注入
│   └── MainActivity.kt          # 主活动
├── src/test/                    # 单元测试
├── src/androidTest/             # 集成测试
└── build.gradle                 # 构建配置
```

## 🧱 核心架构组件

### 1. 数据层 (Data Layer)

#### 本地数据源
```kotlin
// Room 数据库
@Database(
    entities = [
        InputSession::class,
        InputRecord::class,
        SyncRecord::class,
        UserPreferences::class,
        LanguageStatistics::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SyncRimeDatabase : RoomDatabase() {
    abstract fun inputDao(): InputDao
    abstract fun syncDao(): SyncDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun statisticsDao(): StatisticsDao
}

// 数据访问对象
@Dao
interface InputDao {
    @Query("SELECT * FROM input_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<InputSession>>
    
    @Query("SELECT * FROM input_records WHERE session_id = :sessionId")
    fun getRecordsForSession(sessionId: String): Flow<List<InputRecord>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InputSession)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: InputRecord)
}
```

#### 远程数据源
```kotlin
// API 接口
interface SyncRimeApiService {
    @POST("/api/v1/sync/upload")
    suspend fun uploadData(@Body request: SyncUploadRequest): Response<SyncUploadResponse>
    
    @GET("/api/v1/sync/download")
    suspend fun downloadData(@Query("lastSyncTime") lastSyncTime: Long): Response<SyncDownloadResponse>
    
    @POST("/api/v1/intelligence/recommend")
    suspend fun getRecommendations(@Body request: RecommendationRequest): Response<RecommendationResponse>
    
    @POST("/api/v1/intelligence/analyze")
    suspend fun analyzeInput(@Body request: AnalysisRequest): Response<AnalysisResponse>
}

// 网络配置
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .addInterceptor(AuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }
}
```

### 2. 业务逻辑层 (Domain Layer)

#### 领域模型
```kotlin
// 输入会话模型
data class InputSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val application: String,
    val inputType: String,
    val importance: Importance,
    val metadata: Map<String, Any>
)

// 输入记录模型
data class InputRecord(
    val id: String,
    val sessionId: String,
    val text: String,
    val timestamp: Long,
    val inputType: InputType,
    val category: String,
    val importance: Importance,
    val metadata: Map<String, Any>
)

// 同步记录模型
data class SyncRecord(
    val id: String,
    val timestamp: Long,
    val type: SyncType,
    val status: SyncStatus,
    val dataCount: Int,
    val errorMessage: String?
)
```

#### 用例 (Use Cases)
```kotlin
// 输入管理用例
class ManageInputUseCase @Inject constructor(
    private val inputRepository: InputRepository,
    private val intelligenceEngine: IntelligenceEngine
) {
    suspend fun startInputSession(application: String, inputType: String): Result<InputSession> {
        return try {
            val session = InputSession(
                id = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                endTime = 0,
                application = application,
                inputType = inputType,
                importance = Importance.NORMAL,
                metadata = emptyMap()
            )
            
            inputRepository.insertSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addInputRecord(sessionId: String, text: String): Result<InputRecord> {
        return try {
            // 智能分析
            val analysis = intelligenceEngine.analyzeInput(text)
            
            val record = InputRecord(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                text = text,
                timestamp = System.currentTimeMillis(),
                inputType = analysis.inputType,
                category = analysis.category,
                importance = analysis.importance,
                metadata = analysis.metadata
            )
            
            inputRepository.insertRecord(record)
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// 数据同步用例
class SyncDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val conflictResolver: ConflictResolver
) {
    suspend fun syncData(): Result<SyncResult> {
        return try {
            // 获取本地数据
            val localData = syncRepository.getLocalData()
            
            // 上传到服务器
            val uploadResult = syncRepository.uploadData(localData)
            
            // 下载服务器数据
            val remoteData = syncRepository.downloadData()
            
            // 解决冲突
            val resolvedData = conflictResolver.resolveConflicts(localData, remoteData)
            
            // 更新本地数据
            syncRepository.updateLocalData(resolvedData)
            
            Result.success(SyncResult.Success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3. 表现层 (Presentation Layer)

#### ViewModel
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val manageInputUseCase: ManageInputUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()
    
    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()
    
    init {
        loadStatistics()
        observeCurrentSession()
    }
    
    fun startInputSession(application: String, inputType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            manageInputUseCase.startInputSession(application, inputType)
                .onSuccess { session ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentSession = session,
                            isCapturing = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }
    
    fun addInput(text: String) {
        viewModelScope.launch {
            val sessionId = _uiState.value.currentSession?.id ?: return@launch
            
            manageInputUseCase.addInputRecord(sessionId, text)
                .onSuccess { record ->
                    // 更新推荐
                    updateRecommendations(text)
                    
                    // 更新统计
                    loadStatistics()
                }
        }
    }
    
    private suspend fun updateRecommendations(input: String) {
        getRecommendationsUseCase.getRecommendations(input)
            .onSuccess { recommendations ->
                _recommendations.value = recommendations
            }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            getStatisticsUseCase.getStatistics()
                .onSuccess { stats ->
                    _statistics.value = stats
                }
        }
    }
}
```

#### UI 状态
```kotlin
data class MainUiState(
    val isLoading: Boolean = false,
    val isCapturing: Boolean = false,
    val currentSession: InputSession? = null,
    val error: String? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE
) {
    val hasActiveSession: Boolean
        get() = currentSession != null && isCapturing
}
```

## 🧠 智能化功能集成

### 智能化引擎集成
```kotlin
// 智能化引擎管理器
@Singleton
class IntelligenceEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recommendationEngine: IntelligentRecommendationEngine,
    private val contextEngine: ContextAwareInputEngine,
    private val learningEngine: PersonalizedLearningEngine,
    private val correctionEngine: IntelligentCorrectionEngine,
    private val semanticEngine: SemanticAnalysisEngine,
    private val multilingualEngine: MultilingualIntelligentEngine
) {
    
    suspend fun initialize(): Boolean {
        return try {
            recommendationEngine.initialize()
            contextEngine.initialize()
            learningEngine.initialize()
            correctionEngine.initialize()
            semanticEngine.initialize()
            multilingualEngine.initialize()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun analyzeInput(text: String): InputAnalysis {
        val semanticResult = semanticEngine.analyzeText(text)
        val correctionResult = correctionEngine.checkAndCorrect(text)
        val languageResult = multilingualEngine.detectLanguage(text)
        
        return InputAnalysis(
            text = text,
            semantic = semanticResult,
            corrections = correctionResult,
            language = languageResult,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun getRecommendations(
        input: String,
        context: Map<String, Any>
    ): List<Recommendation> {
        val recommendationResult = recommendationEngine.generateRecommendations(input)
        val contextualSuggestions = contextEngine.getContextualSuggestions(input)
        
        return (recommendationResult + contextualSuggestions.map { 
            Recommendation(
                text = it,
                type = Recommendation.TYPE_CONTEXTUAL,
                confidence = 0.8f
            )
        }).distinctBy { it.text }
    }
}
```

## 🔄 数据同步服务

### WorkManager 集成
```kotlin
// 同步 Worker
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val intelligenceEngine: IntelligenceEngineManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // 同步数据
            val syncResult = syncRepository.syncData()
            
            // 更新智能化引擎
            intelligenceEngine.updateFromSync()
            
            Result.success()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// 同步调度器
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedulePeriodicSync(intervalMinutes: Long = 15) {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes, TimeUnit.MINUTES
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    fun scheduleImmediateSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
```

## 🎨 Jetpack Compose UI

### 主界面组件
```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("SyncRime") },
            actions = {
                IconButton(onClick = { viewModel.syncData() }) {
                    Icon(Icons.Default.Sync, "Sync")
                }
                IconButton(onClick = { viewModel.openSettings() }) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }
        )
        
        // 统计信息卡片
        statistics?.let { stats ->
            StatisticsCard(stats = stats)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 当前会话状态
        if (uiState.hasActiveSession) {
            ActiveSessionCard(
                session = uiState.currentSession!!,
                onStopCapture = { viewModel.stopCapture() }
            )
        } else {
            StartSessionCard(
                onStartCapture = { viewModel.startInputSession(it.first, it.second) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 智能推荐
        if (recommendations.isNotEmpty()) {
            RecommendationList(
                recommendations = recommendations,
                onRecommendationSelected = { recommendation ->
                    viewModel.selectRecommendation(recommendation)
                }
            )
        }
    }
}
```

## 🔧 依赖注入配置

### Hilt 模块
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): SyncRimeDatabase {
        return Room.databaseBuilder(
            context,
            SyncRimeDatabase::class.java,
            "syncrime_database"
        ).build()
    }
    
    @Provides
    fun provideInputDao(database: SyncRimeDatabase): InputDao = database.inputDao()
    
    @Provides
    fun provideSyncDao(database: SyncRimeDatabase): SyncDao = database.syncDao()
    
    @Provides
    fun providePreferencesDao(database: SyncRimeDatabase): PreferencesDao = database.preferencesDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideInputRepository(
        inputDao: InputDao,
        syncApiService: SyncRimeApiService,
        intelligenceEngine: IntelligenceEngineManager
    ): InputRepository {
        return InputRepositoryImpl(inputDao, syncApiService, intelligenceEngine)
    }
    
    @Provides
    @Singleton
    fun provideSyncRepository(
        syncDao: SyncDao,
        syncApiService: SyncRimeApiService,
        conflictResolver: ConflictResolver
    ): SyncRepository {
        return SyncRepositoryImpl(syncDao, syncApiService, conflictResolver)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object IntelligenceModule {
    
    @Provides
    @Singleton
    fun provideIntelligenceEngineManager(
        @ApplicationContext context: Context
    ): IntelligenceEngineManager {
        return IntelligenceEngineManager(context)
    }
}
```

## 🚀 启动和配置

### Application 类
```kotlin
@HiltAndroidApp
class SyncRimeApplication : Application() {
    
    @Inject
    lateinit var intelligenceEngineManager: IntelligenceEngineManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化智能化引擎
        CoroutineScope(Dispatchers.IO).launch {
            intelligenceEngineManager.initialize()
        }
        
        // 调度后台同步
        if (BuildConfig.DEBUG) {
            // 调试模式下不自动同步
        } else {
            schedulePeriodicSync()
        }
    }
    
    private fun schedulePeriodicSync() {
        // 实现 WorkManager 调度
    }
}
```

## 📱 权限和配置

### AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- 存储权限 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- 后台服务权限 -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <!-- 使用情况统计权限 -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />
    
    <application
        android:name=".SyncRimeApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.SyncRime">
        
        <!-- 主活动 -->
        <activity
            android:name=".presentation.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SyncRime.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 设置活动 -->
        <activity
            android:name=".presentation.settings.SettingsActivity"
            android:parentActivityName=".presentation.MainActivity" />
        
        <!-- 后台服务 -->
        <service
            android:name=".sync.SyncService"
            android:enabled="true"
            android:exported="false" />
        
        <!-- WorkManager 初始化 -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
    </application>
</manifest>
```

## 🎯 下一步计划

现在我们已经完成了 Android 客户端架构设计，接下来需要：

1. **实现核心 UI 组件** - Jetpack Compose 界面开发
2. **集成智能化功能** - 将 Phase 2 的智能引擎集成到客户端
3. **实现数据同步服务** - WorkManager 后台同步
4. **开发用户界面和体验** - 完善用户交互体验

这个架构为 SyncRime Android 客户端提供了坚实的基础，支持所有智能化功能的集成，并且具有良好的可扩展性和维护性。

准备好开始实现具体的 UI 组件了吗？