#include "syncrime_plugin.h"
#include <android/log.h>

#define LOG_TAG "SyncRimeJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace syncrime::trime;

// JNI 方法实现

JNIEXPORT jlong JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeInitialize(
    JNIEnv* env, jobject thiz, jobject context) {
    try {
        JavaVM* vm = nullptr;
        if (env->GetJavaVM(&vm) != JNI_OK) {
            LOGE("Failed to get JavaVM");
            return 0;
        }
        
        SyncRimePlugin& plugin = SyncRimePlugin::getInstance();
        if (plugin.initialize(vm, env, context)) {
            LOGI("SyncRime plugin initialized successfully");
            return reinterpret_cast<jlong>(&plugin);
        } else {
            LOGE("Failed to initialize SyncRime plugin");
            return 0;
        }
    } catch (const std::exception& e) {
        LOGE("Exception in nativeInitialize: %s", e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCleanup(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            plugin->cleanup();
            LOGI("SyncRime plugin cleaned up");
        }
    } catch (const std::exception& e) {
        LOGE("Exception in nativeCleanup: %s", e.what());
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeStartCapture(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->startCapture();
            LOGD("Start capture result: %s", result ? "success" : "failed");
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeStartCapture: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeStopCapture(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->stopCapture();
            LOGD("Stop capture result: %s", result ? "success" : "failed");
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeStopCapture: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSyncData(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->syncData();
            LOGD("Sync data result: %s", result ? "success" : "failed");
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeSyncData: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jint JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetState(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            return static_cast<jint>(plugin->getState());
        }
        return static_cast<jint>(SyncRimePlugin::PluginState::UNINITIALIZED);
    } catch (const std::exception& e) {
        LOGE("Exception in nativeGetState: %s", e.what());
        return static_cast<jint>(SyncRimePlugin::PluginState::ERROR);
    }
}

JNIEXPORT jstring JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetStateString(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            const char* state_str = plugin->getStateString();
            return env->NewStringUTF(state_str);
        }
        return env->NewStringUTF("UNKNOWN");
    } catch (const std::exception& e) {
        LOGE("Exception in nativeGetStateString: %s", e.what());
        return env->NewStringUTF("ERROR");
    }
}

JNIEXPORT jint JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetLastError(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            return static_cast<jint>(plugin->getLastError());
        }
        return static_cast<jint>(SyncRimePlugin::ErrorCode::UNKNOWN_ERROR);
    } catch (const std::exception& e) {
        LOGE("Exception in nativeGetLastError: %s", e.what());
        return static_cast<jint>(SyncRimePlugin::ErrorCode::UNKNOWN_ERROR);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeLoadConfig(
    JNIEnv* env, jobject thiz, jlong instance, jstring config_path) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (!plugin || !config_path) {
            return JNI_FALSE;
        }
        
        const char* config_path_c = env->GetStringUTFChars(config_path, nullptr);
        if (!config_path_c) {
            return JNI_FALSE;
        }
        
        std::string path(config_path_c);
        env->ReleaseStringUTFChars(config_path, config_path_c);
        
        bool result = plugin->loadConfig(path);
        LOGD("Load config result: %s", result ? "success" : "failed");
        return static_cast<jboolean>(result);
    } catch (const std::exception& e) {
        LOGE("Exception in nativeLoadConfig: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSaveConfig(
    JNIEnv* env, jobject thiz, jlong instance, jstring config_path) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (!plugin || !config_path) {
            return JNI_FALSE;
        }
        
        const char* config_path_c = env->GetStringUTFChars(config_path, nullptr);
        if (!config_path_c) {
            return JNI_FALSE;
        }
        
        std::string path(config_path_c);
        env->ReleaseStringUTFChars(config_path, config_path_c);
        
        bool result = plugin->saveConfig(path);
        LOGD("Save config result: %s", result ? "success" : "failed");
        return static_cast<jboolean>(result);
    } catch (const std::exception& e) {
        LOGE("Exception in nativeSaveConfig: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeUpdateConfig(
    JNIEnv* env, jobject thiz, jlong instance, jstring key, jstring value) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (!plugin || !key || !value) {
            return;
        }
        
        const char* key_c = env->GetStringUTFChars(key, nullptr);
        const char* value_c = env->GetStringUTFChars(value, nullptr);
        
        if (!key_c || !value_c) {
            if (key_c) env->ReleaseStringUTFChars(key, key_c);
            if (value_c) env->ReleaseStringUTFChars(value, value_c);
            return;
        }
        
        std::string key_str(key_c);
        std::string value_str(value_c);
        
        env->ReleaseStringUTFChars(key, key_c);
        env->ReleaseStringUTFChars(value, value_c);
        
        plugin->updateConfig(key_str, value_str);
        LOGD("Config updated: %s = %s", key_str.c_str(), value_str.c_str());
    } catch (const std::exception& e) {
        LOGE("Exception in nativeUpdateConfig: %s", e.what());
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCaptureInput(
    JNIEnv* env, jobject thiz, jlong instance, jstring text, jstring metadata) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (!plugin || !text) {
            return JNI_FALSE;
        }
        
        const char* text_c = env->GetStringUTFChars(text, nullptr);
        const char* metadata_c = metadata ? env->GetStringUTFChars(metadata, nullptr) : nullptr;
        
        if (!text_c) {
            if (metadata_c) env->ReleaseStringUTFChars(metadata, metadata_c);
            return JNI_FALSE;
        }
        
        std::string text_str(text_c);
        std::string metadata_str = metadata_c ? std::string(metadata_c) : "";
        
        env->ReleaseStringUTFChars(text, text_c);
        if (metadata_c) env->ReleaseStringUTFChars(metadata, metadata_c);
        
        bool result = plugin->captureInput(text_str, metadata_str);
        LOGD("Capture input result: %s", result ? "success" : "failed");
        return static_cast<jboolean>(result);
    } catch (const std::exception& e) {
        LOGE("Exception in nativeCaptureInput: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCaptureKeyEvent(
    JNIEnv* env, jobject thiz, jlong instance, jint keycode, jint action) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->captureKeyEvent(keycode, action);
            LOGD("Capture key event result: %s", result ? "success" : "failed");
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeCaptureKeyEvent: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeCaptureGesture(
    JNIEnv* env, jobject thiz, jlong instance, jint gesture_type, jfloatArray points) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (!plugin || !points) {
            return JNI_FALSE;
        }
        
        jsize points_length = env->GetArrayLength(points);
        std::vector<float> points_vector(points_length);
        env->GetFloatArrayRegion(points, 0, points_length, points_vector.data());
        
        bool result = plugin->captureGesture(gesture_type, points_vector);
        LOGD("Capture gesture result: %s", result ? "success" : "failed");
        return static_cast<jboolean>(result);
    } catch (const std::exception& e) {
        LOGE("Exception in nativeCaptureGesture: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeEnableAutoSync(
    JNIEnv* env, jobject thiz, jlong instance, jboolean enable) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->enableAutoSync(enable);
            LOGD("Enable auto sync result: %s", result ? "success" : "failed");
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeEnableAutoSync: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSetSyncInterval(
    JNIEnv* env, jobject thiz, jlong instance, jint seconds) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->setSyncInterval(seconds);
            LOGD("Set sync interval result: %s", result ? "success" : "failed");
        }
    } catch (const std::exception& e) {
        LOGE("Exception in nativeSetSyncInterval: %s", e.what());
    }
}

JNIEXPORT jint JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetSyncInterval(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            return plugin->getSyncInterval();
        }
        return -1;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeGetSyncInterval: %s", e.what());
        return -1;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeForceSync(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            bool result = plugin->forceSync();
            LOGD("Force sync result: %s", result ? "success" : "failed");
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeForceSync: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT jobject JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeGetStatistics(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (!plugin) {
            return nullptr;
        }
        
        auto stats = plugin->getStatistics();
        
        // 创建 Statistics 对象
        jclass stats_class = env->FindClass("com/syncrime/trime/plugin/SyncRimePlugin$Statistics");
        if (!stats_class) {
            LOGE("Failed to find Statistics class");
            return nullptr;
        }
        
        jmethodID constructor = env->GetMethodID(stats_class, "<init>", "(JJJJDD)V");
        if (!constructor) {
            LOGE("Failed to find Statistics constructor");
            return nullptr;
        }
        
        jobject stats_obj = env->NewObject(stats_class, constructor,
                                          static_cast<jlong>(stats.total_inputs),
                                          static_cast<jlong>(stats.total_syncs),
                                          static_cast<jlong>(stats.successful_syncs),
                                          static_cast<jlong>(stats.failed_syncs),
                                          static_cast<jlong>(stats.last_sync_time),
                                          static_cast<jdouble>(stats.average_sync_time));
        
        return stats_obj;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeGetStatistics: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeResetStatistics(
    JNIEnv* env, jobject thiz, jlong instance) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin) {
            plugin->resetStatistics();
            LOGD("Statistics reset");
        }
    } catch (const std::exception& e) {
        LOGE("Exception in nativeResetStatistics: %s", e.what());
    }
}

JNIEXPORT void JNICALL
Java_com_syncrime_trime_plugin_SyncRimePlugin_nativeSetJniCallbacks(
    JNIEnv* env, jobject thiz, jlong instance, jobject callbacks) {
    try {
        SyncRimePlugin* plugin = reinterpret_cast<SyncRimePlugin*>(instance);
        if (plugin && callbacks) {
            plugin->setJniCallbacks(env, callbacks);
            LOGD("JNI callbacks set");
        }
    } catch (const std::exception& e) {
        LOGE("Exception in nativeSetJniCallbacks: %s", e.what());
    }
}

// JNI_OnLoad 函数
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called");
    
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return JNI_ERR;
    }
    
    LOGI("JNI_OnLoad completed successfully");
    return JNI_VERSION_1_6;
}

// JNI_OnUnload 函数
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnUnload called");
    
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        // 清理资源
        try {
            SyncRimePlugin::getInstance().cleanup();
        } catch (const std::exception& e) {
            LOGE("Exception in JNI_OnUnload: %s", e.what());
        }
    }
    
    LOGI("JNI_OnUnload completed");
}