#pragma once

#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <atomic>

namespace syncrime {
namespace trime {

// 前向声明
class DataCollector;
class SyncManager;
class ConfigManager;
class InputAnalyzer;

/**
 * SyncRime 插件主类
 * 负责插件的生命周期管理和核心功能协调
 */
class SyncRimePlugin {
public:
    // 单例模式
    static SyncRimePlugin& getInstance();
    
    // 禁用拷贝和赋值
    SyncRimePlugin(const SyncRimePlugin&) = delete;
    SyncRimePlugin& operator=(const SyncRimePlugin&) = delete;
    
    // 初始化和清理
    bool initialize(JavaVM* vm, JNIEnv* env, jobject context);
    void cleanup();
    
    // 核心功能
    bool startCapture();
    bool stopCapture();
    bool syncData();
    bool isCapturing() const;
    
    // 状态管理
    enum class PluginState {
        UNINITIALIZED,
        INITIALIZING,
        READY,
        CAPTURING,
        SYNCING,
        ERROR
    };
    
    PluginState getState() const { return state_.load(); }
    const char* getStateString() const;
    
    // 配置管理
    bool loadConfig(const std::string& config_path);
    bool saveConfig(const std::string& config_path);
    void updateConfig(const std::string& key, const std::string& value);
    
    // 数据收集
    bool captureInput(const std::string& text, const std::string& metadata);
    bool captureKeyEvent(int keycode, int action);
    bool captureGesture(int gesture_type, const std::vector<float>& points);
    
    // 同步功能
    bool enableAutoSync(bool enable);
    bool setSyncInterval(int seconds);
    int getSyncInterval() const;
    bool forceSync();
    
    // 统计信息
    struct Statistics {
        uint64_t total_inputs;
        uint64_t total_syncs;
        uint64_t successful_syncs;
        uint64_t failed_syncs;
        uint64_t last_sync_time;
        double average_sync_time;
    };
    
    Statistics getStatistics() const;
    void resetStatistics();
    
    // 错误处理
    enum class ErrorCode {
        SUCCESS = 0,
        INITIALIZATION_FAILED,
        ALREADY_INITIALIZED,
        NOT_INITIALIZED,
        CAPTURE_ALREADY_STARTED,
        CAPTURE_NOT_STARTED,
        SYNC_IN_PROGRESS,
        CONFIG_LOAD_FAILED,
        CONFIG_SAVE_FAILED,
        NETWORK_ERROR,
        PERMISSION_DENIED,
        UNKNOWN_ERROR
    };
    
    ErrorCode getLastError() const { return last_error_.load(); }
    const char* getLastErrorString() const;
    
    // JNI 回调
    void setJniCallbacks(JNIEnv* env, jobject callbacks);
    void notifyInputCaptured(const std::string& text);
    void notifySyncStarted();
    void notifySyncCompleted(bool success);
    void notifyError(ErrorCode error, const std::string& message);

private:
    SyncRimePlugin() = default;
    ~SyncRimePlugin() = default;
    
    // 内部状态
    std::atomic<PluginState> state_{PluginState::UNINITIALIZED};
    std::atomic<ErrorCode> last_error_{ErrorCode::SUCCESS};
    
    // JNI 相关
    JavaVM* java_vm_{nullptr};
    jobject app_context_{nullptr};
    jobject jni_callbacks_{nullptr};
    
    // 核心组件
    std::unique_ptr<DataCollector> data_collector_;
    std::unique_ptr<SyncManager> sync_manager_;
    std::unique_ptr<ConfigManager> config_manager_;
    std::unique_ptr<InputAnalyzer> input_analyzer_;
    
    // 同步原语
    mutable std::mutex state_mutex_;
    mutable std::mutex config_mutex_;
    mutable std::mutex callback_mutex_;
    
    // 内部方法
    bool initializeComponents();
    void cleanupComponents();
    bool validateState(PluginState required_state) const;
    void setState(PluginState new_state);
    void setError(ErrorCode error);
    
    // JNI 辅助方法
    JNIEnv* getJniEnv();
    void callJniMethod(const char* method_name, const char* signature, ...);
    void callJniVoidMethod(const char* method_name, const char* signature, ...);
};

/**
 * JNI 入口点
 */
extern "C" {

// 插件生命周期
JNIEXPORT jlong JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeInitialize(
    JNIEnv* env, jobject thiz, jobject context);

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCleanup(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeStartCapture(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeStopCapture(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSyncData(
    JNIEnv* env, jobject thiz, jlong instance);

// 状态查询
JNIEXPORT jint JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetState(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT jstring JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetStateString(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT jint JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetLastError(
    JNIEnv* env, jobject thiz, jlong instance);

// 配置管理
JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeLoadConfig(
    JNIEnv* env, jobject thiz, jlong instance, jstring config_path);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSaveConfig(
    JNIEnv* env, jobject thiz, jlong instance, jstring config_path);

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeUpdateConfig(
    JNIEnv* env, jobject thiz, jlong instance, jstring key, jstring value);

// 数据收集
JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCaptureInput(
    JNIEnv* env, jobject thiz, jlong instance, jstring text, jstring metadata);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCaptureKeyEvent(
    JNIEnv* env, jobject thiz, jlong instance, jint keycode, jint action);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCaptureGesture(
    JNIEnv* env, jobject thiz, jlong instance, jint gesture_type, jfloatArray points);

// 同步功能
JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeEnableAutoSync(
    JNIEnv* env, jobject thiz, jlong instance, jboolean enable);

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSetSyncInterval(
    JNIEnv* env, jobject thiz, jlong instance, jint seconds);

JNIEXPORT jint JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetSyncInterval(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeForceSync(
    JNIEnv* env, jobject thiz, jlong instance);

// 统计信息
JNIEXPORT jobject JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetStatistics(
    JNIEnv* env, jobject thiz, jlong instance);

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeResetStatistics(
    JNIEnv* env, jobject thiz, jlong instance);

// 回调设置
JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSetJniCallbacks(
    JNIEnv* env, jobject thiz, jlong instance, jobject callbacks);

} // extern "C"

} // namespace trime
} // namespace syncrime