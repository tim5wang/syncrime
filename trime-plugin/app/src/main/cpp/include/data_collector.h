#pragma once

#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <atomic>
#include <chrono>

namespace syncrime {
namespace trime {

/**
 * 输入记录结构
 */
struct InputRecord {
    std::string text;
    std::string metadata;
    std::string input_type;
    std::string category;
    int importance;
    int64_t timestamp;
    
    InputRecord() : importance(0), timestamp(0) {}
    
    InputRecord(const std::string& t, const std::string& m = "", 
                const std::string& type = "text", 
                const std::string& cat = "general",
                int imp = 0, int64_t ts = 0)
        : text(t), metadata(m), input_type(type), 
          category(cat), importance(imp), timestamp(ts) {}
};

/**
 * 输入会话结构
 */
struct InputSession {
    std::string id;
    int64_t start_time;
    int64_t end_time;
    std::vector<std::shared_ptr<InputRecord>> inputs;
    bool is_active;
    int importance;
    
    InputSession() : start_time(0), end_time(0), is_active(false), importance(0) {}
    
    InputSession(const std::string& session_id)
        : id(session_id), start_time(0), end_time(0), is_active(true), importance(0) {}
};

/**
 * 统计信息结构
 */
struct Statistics {
    uint64_t total_inputs;
    uint64_t total_sessions;
    uint64_t active_sessions;
    double average_session_length;
    double average_session_duration;
    uint64_t high_importance_sessions;
    uint64_t critical_inputs;
    int64_t last_capture_time;
    
    Statistics() : total_inputs(0), total_sessions(0), active_sessions(0),
                 average_session_length(0.0), average_session_duration(0.0),
                 high_importance_sessions(0), critical_inputs(0), last_capture_time(0) {}
};

/**
 * 数据采集器类
 * 
 * 负责采集用户的输入数据，包括文本、按键和手势信息。
 * 提供智能的内容分类和重要性分析功能。
 */
class DataCollector {
public:
    // 输入类型枚举
    enum class InputType {
        TEXT,
        PASSWORD,
        EMAIL,
        PHONE,
        URL,
        NUMBER,
        SYMBOL,
        EMOJI,
        COMMAND
    };
    
    // 重要性级别枚举
    enum class Importance {
        LOW = 1,
        NORMAL = 2,
        HIGH = 3,
        CRITICAL = 4
    };
    
    // 采集模式枚举
    enum class CaptureMode {
        SMART,      // 智能模式
        ALL,        // 全部采集
        FILTERED    // 过滤模式
    };
    
    DataCollector();
    ~DataCollector();
    
    // 初始化和清理
    bool initialize();
    void cleanup();
    
    // 采集控制
    bool start();
    bool stop();
    bool pause();
    bool resume();
    bool isCapturing() const { return is_capturing_.load(); }
    bool isPaused() const { return is_paused_.load(); }
    
    // 配置管理
    void setCaptureMode(CaptureMode mode);
    CaptureMode getCaptureMode() const { return capture_mode_; }
    
    void setSessionTimeout(int64_t timeout_ms);
    int64_t getSessionTimeout() const { return session_timeout_ms_; }
    
    void setMinSessionLength(int min_length);
    int getMinSessionLength() const { return min_session_length_; }
    
    void setCaptureSensitiveData(bool enable);
    bool getCaptureSensitiveData() const { return capture_sensitive_data_; }
    
    void setCaptureEmoji(bool enable);
    bool getCaptureEmoji() const { return capture_emoji_; }
    
    void setCaptureGestures(bool enable);
    bool getCaptureGestures() const { return capture_gestures_; }
    
    // 数据采集
    bool captureInput(const std::string& text, const std::string& metadata = "");
    bool captureKeyEvent(int keycode, int action);
    bool captureGesture(int gesture_type, const std::vector<float>& points);
    
    // 会话管理
    std::shared_ptr<InputSession> getCurrentSession() const;
    std::vector<std::shared_ptr<InputSession>> getRecentSessions(int limit = 10) const;
    std::vector<std::shared_ptr<InputRecord>> getHighImportanceInputs(Importance min_importance = Importance::HIGH) const;
    std::vector<std::shared_ptr<InputRecord>> searchInputs(const std::string& query, int limit = 50) const;
    
    // 统计信息
    Statistics getStatistics() const;
    void resetStatistics();
    
    // 数据管理
    void clearOldSessions(int64_t max_age_ms = 7 * 24 * 60 * 60 * 1000LL); // 7天
    void exportData(const std::string& file_path) const;
    void importData(const std::string& file_path);
    
    // 事件回调
    typedef std::function<void(const std::shared_ptr<InputRecord>&)> InputCapturedCallback;
    typedef std::function<void(const std::shared_ptr<InputSession>&)> SessionStartedCallback;
    typedef std::function<void(const std::shared_ptr<InputSession>&)> SessionEndedCallback;
    
    void setInputCapturedCallback(InputCapturedCallback callback);
    void setSessionStartedCallback(SessionStartedCallback callback);
    void setSessionEndedCallback(SessionEndedCallback callback);

private:
    // 内部状态
    std::atomic<bool> is_initialized_{false};
    std::atomic<bool> is_capturing_{false};
    std::atomic<bool> is_paused_{false};
    
    // 配置参数
    CaptureMode capture_mode_{CaptureMode::SMART};
    int64_t session_timeout_ms_{5 * 60 * 1000LL}; // 5分钟
    int min_session_length_{3};
    bool capture_sensitive_data_{false};
    bool capture_emoji_{true};
    bool capture_gestures_{true};
    
    // 会话管理
    mutable std::mutex sessions_mutex_;
    std::vector<std::shared_ptr<InputSession>> sessions_;
    std::shared_ptr<InputSession> current_session_;
    std::string next_session_id_;
    
    // 统计信息
    mutable std::mutex stats_mutex_;
    Statistics statistics_;
    
    // 回调函数
    InputCapturedCallback input_captured_callback_;
    SessionStartedCallback session_started_callback_;
    SessionEndedCallback session_ended_callback_;
    
    // 同步原语
    mutable std::mutex config_mutex_;
    
    // 内部方法
    std::string generateSessionId();
    std::shared_ptr<InputSession> createNewSession();
    void endCurrentSession();
    void cleanupOldSessions();
    
    // 输入分析
    InputType detectInputType(const std::string& text) const;
    std::string categorizeInput(const std::string& text) const;
    Importance analyzeInputImportance(const std::string& text, InputType type) const;
    void analyzeSessionImportance(std::shared_ptr<InputSession> session);
    
    // 敏感信息检测
    bool isSensitiveInput(const std::string& text) const;
    bool containsPassword(const std::string& text) const;
    bool containsPersonalInfo(const std::string& text) const;
    
    // 过滤和清理
    bool shouldCaptureInput(const std::string& text, InputType type) const;
    std::string sanitizeInput(const std::string& text) const;
    
    // 会话管理
    void updateSessionActivity();
    bool isSessionExpired(const std::shared_ptr<InputSession>& session) const;
    
    // 统计更新
    void updateStatistics(const std::shared_ptr<InputRecord>& record);
    void updateStatistics(const std::shared_ptr<InputSession>& session);
    
    // 回调触发
    void triggerInputCapturedCallback(const std::shared_ptr<InputRecord>& record);
    void triggerSessionStartedCallback(const std::shared_ptr<InputSession>& session);
    void triggerSessionEndedCallback(const std::shared_ptr<InputSession>& session);
    
    // 数据持久化
    void saveSession(const std::shared_ptr<InputSession>& session) const;
    void loadSessions();
    void saveStatistics() const;
    void loadStatistics();
    
    // 工具方法
    int64_t getCurrentTime() const;
    std::string getCurrentTimeISO() const;
    std::string encryptSensitiveData(const std::string& data) const;
    std::string decryptSensitiveData(const std::string& encrypted_data) const;
};

} // namespace trime
} // namespace syncrime