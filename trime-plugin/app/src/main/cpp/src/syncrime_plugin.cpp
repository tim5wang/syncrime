#include "syncrime_plugin.h"
#include "data_collector.h"
#include "sync_manager.h"
#include "config_manager.h"
#include "input_analyzer.h"
#include <android/log.h>
#include <chrono>
#include <thread>

#define LOG_TAG "SyncRimePlugin"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace syncrime {
namespace trime {

// 单例实现
SyncRimePlugin& SyncRimePlugin::getInstance() {
    static SyncRimePlugin instance;
    return instance;
}

// 初始化
bool SyncRimePlugin::initialize(JavaVM* vm, JNIEnv* env, jobject context) {
    std::lock_guard<std::mutex> lock(state_mutex_);
    
    if (state_ != PluginState::UNINITIALIZED) {
        LOGW("Plugin already initialized");
        return false;
    }
    
    setState(PluginState::INITIALIZING);
    
    try {
        java_vm_ = vm;
        app_context_ = env->NewGlobalRef(context);
        
        if (!initializeComponents()) {
            setError(ErrorCode::INITIALIZATION_FAILED);
            setState(PluginState::ERROR);
            return false;
        }
        
        setState(PluginState::READY);
        LOGI("SyncRime plugin initialized successfully");
        return true;
        
    } catch (const std::exception& e) {
        LOGE("Initialization failed: %s", e.what());
        setError(ErrorCode::INITIALIZATION_FAILED);
        setState(PluginState::ERROR);
        return false;
    }
}

// 清理
void SyncRimePlugin::cleanup() {
    std::lock_guard<std::mutex> lock(state_mutex_);
    
    if (state_ == PluginState::UNINITIALIZED) {
        return;
    }
    
    cleanupComponents();
    
    if (app_context_) {
        JNIEnv* env = getJniEnv();
        if (env) {
            env->DeleteGlobalRef(app_context_);
        }
        app_context_ = nullptr;
    }
    
    if (jni_callbacks_) {
        JNIEnv* env = getJniEnv();
        if (env) {
            env->DeleteGlobalRef(jni_callbacks_);
        }
        jni_callbacks_ = nullptr;
    }
    
    java_vm_ = nullptr;
    setState(PluginState::UNINITIALIZED);
    LOGI("SyncRime plugin cleaned up");
}

// 开始采集
bool SyncRimePlugin::startCapture() {
    if (!validateState(PluginState::READY)) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(state_mutex_);
    
    if (state_ == PluginState::CAPTURING) {
        setError(ErrorCode::CAPTURE_ALREADY_STARTED);
        return false;
    }
    
    try {
        if (data_collector_ && data_collector_->start()) {
            setState(PluginState::CAPTURING);
            LOGI("Input capture started");
            return true;
        } else {
            setError(ErrorCode::UNKNOWN_ERROR);
            return false;
        }
    } catch (const std::exception& e) {
        LOGE("Failed to start capture: %s", e.what());
        setError(ErrorCode::UNKNOWN_ERROR);
        return false;
    }
}

// 停止采集
bool SyncRimePlugin::stopCapture() {
    if (!validateState(PluginState::CAPTURING)) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(state_mutex_);
    
    try {
        if (data_collector_ && data_collector_->stop()) {
            setState(PluginState::READY);
            LOGI("Input capture stopped");
            return true;
        } else {
            setError(ErrorCode::UNKNOWN_ERROR);
            return false;
        }
    } catch (const std::exception& e) {
        LOGE("Failed to stop capture: %s", e.what());
        setError(ErrorCode::UNKNOWN_ERROR);
        return false;
    }
}

// 同步数据
bool SyncRimePlugin::syncData() {
    if (!validateState(PluginState::READY) && !validateState(PluginState::CAPTURING)) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(state_mutex_);
    
    if (state_ == PluginState::SYNCING) {
        setError(ErrorCode::SYNC_IN_PROGRESS);
        return false;
    }
    
    try {
        setState(PluginState::SYNCING);
        notifySyncStarted();
        
        bool success = false;
        if (sync_manager_) {
            success = sync_manager_->sync();
        }
        
        setState(success ? PluginState::READY : PluginState::ERROR);
        notifySyncCompleted(success);
        
        LOGI("Data sync %s", success ? "completed" : "failed");
        return success;
        
    } catch (const std::exception& e) {
        LOGE("Sync failed: %s", e.what());
        setState(PluginState::ERROR);
        notifySyncCompleted(false);
        setError(ErrorCode::UNKNOWN_ERROR);
        return false;
    }
}

// 检查是否正在采集
bool SyncRimePlugin::isCapturing() const {
    return state_.load() == PluginState::CAPTURING;
}

// 获取状态字符串
const char* SyncRimePlugin::getStateString() const {
    switch (state_.load()) {
        case PluginState::UNINITIALIZED: return "UNINITIALIZED";
        case PluginState::INITIALIZING: return "INITIALIZING";
        case PluginState::READY: return "READY";
        case PluginState::CAPTURING: return "CAPTURING";
        case PluginState::SYNCING: return "SYNCING";
        case PluginState::ERROR: return "ERROR";
        default: return "UNKNOWN";
    }
}

// 加载配置
bool SyncRimePlugin::loadConfig(const std::string& config_path) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    
    if (!config_manager_) {
        setError(ErrorCode::NOT_INITIALIZED);
        return false;
    }
    
    try {
        bool success = config_manager_->loadFromFile(config_path);
        if (!success) {
            setError(ErrorCode::CONFIG_LOAD_FAILED);
        }
        return success;
    } catch (const std::exception& e) {
        LOGE("Failed to load config: %s", e.what());
        setError(ErrorCode::CONFIG_LOAD_FAILED);
        return false;
    }
}

// 保存配置
bool SyncRimePlugin::saveConfig(const std::string& config_path) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    
    if (!config_manager_) {
        setError(ErrorCode::NOT_INITIALIZED);
        return false;
    }
    
    try {
        bool success = config_manager_->saveToFile(config_path);
        if (!success) {
            setError(ErrorCode::CONFIG_SAVE_FAILED);
        }
        return success;
    } catch (const std::exception& e) {
        LOGE("Failed to save config: %s", e.what());
        setError(ErrorCode::CONFIG_SAVE_FAILED);
        return false;
    }
}

// 更新配置
void SyncRimePlugin::updateConfig(const std::string& key, const std::string& value) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    
    if (config_manager_) {
        config_manager_->set(key, value);
    }
}

// 采集输入
bool SyncRimePlugin::captureInput(const std::string& text, const std::string& metadata) {
    if (!validateState(PluginState::CAPTURING)) {
        return false;
    }
    
    try {
        if (data_collector_) {
            bool success = data_collector_->captureInput(text, metadata);
            if (success) {
                notifyInputCaptured(text);
            }
            return success;
        }
        return false;
    } catch (const std::exception& e) {
        LOGE("Failed to capture input: %s", e.what());
        return false;
    }
}

// 采集按键事件
bool SyncRimePlugin::captureKeyEvent(int keycode, int action) {
    if (!validateState(PluginState::CAPTURING)) {
        return false;
    }
    
    try {
        return data_collector_ && data_collector_->captureKeyEvent(keycode, action);
    } catch (const std::exception& e) {
        LOGE("Failed to capture key event: %s", e.what());
        return false;
    }
}

// 采集手势
bool SyncRimePlugin::captureGesture(int gesture_type, const std::vector<float>& points) {
    if (!validateState(PluginState::CAPTURING)) {
        return false;
    }
    
    try {
        return data_collector_ && data_collector_->captureGesture(gesture_type, points);
    } catch (const std::exception& e) {
        LOGE("Failed to capture gesture: %s", e.what());
        return false;
    }
}

// 启用自动同步
bool SyncRimePlugin::enableAutoSync(bool enable) {
    if (!sync_manager_) {
        return false;
    }
    
    try {
        return sync_manager_->enableAutoSync(enable);
    } catch (const std::exception& e) {
        LOGE("Failed to enable auto sync: %s", e.what());
        return false;
    }
}

// 设置同步间隔
bool SyncRimePlugin::setSyncInterval(int seconds) {
    if (!sync_manager_) {
        return false;
    }
    
    try {
        return sync_manager_->setSyncInterval(seconds);
    } catch (const std::exception& e) {
        LOGE("Failed to set sync interval: %s", e.what());
        return false;
    }
}

// 获取同步间隔
int SyncRimePlugin::getSyncInterval() const {
    if (!sync_manager_) {
        return -1;
    }
    
    try {
        return sync_manager_->getSyncInterval();
    } catch (const std::exception& e) {
        LOGE("Failed to get sync interval: %s", e.what());
        return -1;
    }
}

// 强制同步
bool SyncRimePlugin::forceSync() {
    return syncData();
}

// 获取统计信息
SyncRimePlugin::Statistics SyncRimePlugin::getStatistics() const {
    Statistics stats = {};
    
    if (data_collector_) {
        auto collector_stats = data_collector_->getStatistics();
        stats.total_inputs = collector_stats.total_inputs;
    }
    
    if (sync_manager_) {
        auto sync_stats = sync_manager_->getStatistics();
        stats.total_syncs = sync_stats.total_syncs;
        stats.successful_syncs = sync_stats.successful_syncs;
        stats.failed_syncs = sync_stats.failed_syncs;
        stats.last_sync_time = sync_stats.last_sync_time;
        stats.average_sync_time = sync_stats.average_sync_time;
    }
    
    return stats;
}

// 重置统计信息
void SyncRimePlugin::resetStatistics() {
    if (data_collector_) {
        data_collector_->resetStatistics();
    }
    
    if (sync_manager_) {
        sync_manager_->resetStatistics();
    }
}

// 获取错误字符串
const char* SyncRimePlugin::getLastErrorString() const {
    switch (last_error_.load()) {
        case ErrorCode::SUCCESS: return "SUCCESS";
        case ErrorCode::INITIALIZATION_FAILED: return "INITIALIZATION_FAILED";
        case ErrorCode::ALREADY_INITIALIZED: return "ALREADY_INITIALIZED";
        case ErrorCode::NOT_INITIALIZED: return "NOT_INITIALIZED";
        case ErrorCode::CAPTURE_ALREADY_STARTED: return "CAPTURE_ALREADY_STARTED";
        case ErrorCode::CAPTURE_NOT_STARTED: return "CAPTURE_NOT_STARTED";
        case ErrorCode::SYNC_IN_PROGRESS: return "SYNC_IN_PROGRESS";
        case ErrorCode::CONFIG_LOAD_FAILED: return "CONFIG_LOAD_FAILED";
        case ErrorCode::CONFIG_SAVE_FAILED: return "CONFIG_SAVE_FAILED";
        case ErrorCode::NETWORK_ERROR: return "NETWORK_ERROR";
        case ErrorCode::PERMISSION_DENIED: return "PERMISSION_DENIED";
        case ErrorCode::UNKNOWN_ERROR: return "UNKNOWN_ERROR";
        default: return "UNKNOWN";
    }
}

// 设置 JNI 回调
void SyncRimePlugin::setJniCallbacks(JNIEnv* env, jobject callbacks) {
    std::lock_guard<std::mutex> lock(callback_mutex_);
    
    if (jni_callbacks_) {
        env->DeleteGlobalRef(jni_callbacks_);
    }
    
    jni_callbacks_ = env->NewGlobalRef(callbacks);
}

// 通知输入已采集
void SyncRimePlugin::notifyInputCaptured(const std::string& text) {
    JNIEnv* env = getJniEnv();
    if (env && jni_callbacks_) {
        jstring j_text = env->NewStringUTF(text.c_str());
        callJniVoidMethod("onInputCaptured", "(Ljava/lang/String;)V", j_text);
        env->DeleteLocalRef(j_text);
    }
}

// 通知同步开始
void SyncRimePlugin::notifySyncStarted() {
    JNIEnv* env = getJniEnv();
    if (env && jni_callbacks_) {
        callJniVoidMethod("onSyncStarted", "()V");
    }
}

// 通知同步完成
void SyncRimePlugin::notifySyncCompleted(bool success) {
    JNIEnv* env = getJniEnv();
    if (env && jni_callbacks_) {
        callJniVoidMethod("onSyncCompleted", "(Z)V", static_cast<jboolean>(success));
    }
}

// 通知错误
void SyncRimePlugin::notifyError(ErrorCode error, const std::string& message) {
    JNIEnv* env = getJniEnv();
    if (env && jni_callbacks_) {
        jstring j_message = env->NewStringUTF(message.c_str());
        callJniVoidMethod("onError", "(ILjava/lang/String;)V", 
                          static_cast<jint>(error), j_message);
        env->DeleteLocalRef(j_message);
    }
}

// 私有方法实现

bool SyncRimePlugin::initializeComponents() {
    try {
        data_collector_ = std::make_unique<DataCollector>();
        sync_manager_ = std::make_unique<SyncManager>();
        config_manager_ = std::make_unique<ConfigManager>();
        input_analyzer_ = std::make_unique<InputAnalyzer>();
        
        return data_collector_ && sync_manager_ && 
               config_manager_ && input_analyzer_;
    } catch (const std::exception& e) {
        LOGE("Failed to initialize components: %s", e.what());
        return false;
    }
}

void SyncRimePlugin::cleanupComponents() {
    data_collector_.reset();
    sync_manager_.reset();
    config_manager_.reset();
    input_analyzer_.reset();
}

bool SyncRimePlugin::validateState(PluginState required_state) const {
    PluginState current = state_.load();
    if (current != required_state) {
        LOGW("Invalid state transition: current=%s, required=%s",
             getStateString(), 
             required_state == PluginState::READY ? "READY" :
             required_state == PluginState::CAPTURING ? "CAPTURING" :
             required_state == PluginState::SYNCING ? "SYNCING" : "UNKNOWN");
        return false;
    }
    return true;
}

void SyncRimePlugin::setState(PluginState new_state) {
    PluginState old_state = state_.exchange(new_state);
    LOGD("State changed: %s -> %s", 
         old_state == PluginState::UNINITIALIZED ? "UNINITIALIZED" :
         old_state == PluginState::INITIALIZING ? "INITIALIZING" :
         old_state == PluginState::READY ? "READY" :
         old_state == PluginState::CAPTURING ? "CAPTURING" :
         old_state == PluginState::SYNCING ? "SYNCING" :
         old_state == PluginState::ERROR ? "ERROR" : "UNKNOWN",
         getStateString());
}

void SyncRimePlugin::setError(ErrorCode error) {
    last_error_ = error;
    LOGE("Error set: %s", getLastErrorString());
}

JNIEnv* SyncRimePlugin::getJniEnv() {
    if (!java_vm_) {
        return nullptr;
    }
    
    JNIEnv* env = nullptr;
    jint result = java_vm_->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    
    if (result == JNI_EDETACHED) {
        result = java_vm_->AttachCurrentThread(&env, nullptr);
        if (result != JNI_OK) {
            LOGE("Failed to attach current thread");
            return nullptr;
        }
    } else if (result != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return nullptr;
    }
    
    return env;
}

void SyncRimePlugin::callJniMethod(const char* method_name, const char* signature, ...) {
    JNIEnv* env = getJniEnv();
    if (!env || !jni_callbacks_) {
        return;
    }
    
    jclass clazz = env->GetObjectClass(jni_callbacks_);
    if (!clazz) {
        LOGE("Failed to get callback class");
        return;
    }
    
    jmethodID method = env->GetMethodID(clazz, method_name, signature);
    if (!method) {
        LOGE("Failed to get method ID: %s", method_name);
        env->DeleteLocalRef(clazz);
        return;
    }
    
    va_list args;
    va_start(args, signature);
    env->CallVoidMethodV(jni_callbacks_, method, args);
    va_end(args);
    
    env->DeleteLocalRef(clazz);
}

void SyncRimePlugin::callJniVoidMethod(const char* method_name, const char* signature, ...) {
    JNIEnv* env = getJniEnv();
    if (!env || !jni_callbacks_) {
        return;
    }
    
    jclass clazz = env->GetObjectClass(jni_callbacks_);
    if (!clazz) {
        LOGE("Failed to get callback class");
        return;
    }
    
    jmethodID method = env->GetMethodID(clazz, method_name, signature);
    if (!method) {
        LOGE("Failed to get method ID: %s", method_name);
        env->DeleteLocalRef(clazz);
        return;
    }
    
    va_list args;
    va_start(args, signature);
    env->CallVoidMethodV(jni_callbacks_, method, args);
    va_end(args);
    
    env->DeleteLocalRef(clazz);
}

} // namespace trime
} // namespace syncrime