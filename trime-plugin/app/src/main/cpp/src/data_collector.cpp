#include "data_collector.h"
#include "syncrime_plugin.h"
#include <algorithm>
#include <regex>
#include <sstream>
#include <fstream>
#include <ctime>
#include <iomanip>

namespace syncrime {
namespace trime {

// 敏感信息关键词
static const std::vector<std::string> SENSITIVE_KEYWORDS = {
    "密码", "password", "账号", "account", "用户名", "username",
    "手机号", "phone", "邮箱", "email", "地址", "address",
    "身份证", "id", "银行卡", "bank", "信用卡", "credit",
    "支付", "payment", "转账", "transfer", "验证码", "code"
};

// 敏感信息正则表达式
static const std::regex PASSWORD_REGEX(R"((?:password|pwd|密码)[:：\s]*[\w!@#$%^&*()_+={}\[\]:;"'<>?,.\\/`~|-]+)", std::regex_constants::icase);
static const std::regex EMAIL_REGEX(R"([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})");
static const std::regex PHONE_REGEX(R"(1[3-9]\d{9})");
static const std::regex ID_CARD_REGEX(R"(\d{17}[\dXx]|\d{15})");
static const std::regex BANK_CARD_REGEX(R"(\d{16,19})");

DataCollector::DataCollector() : next_session_id_("1") {
    loadStatistics();
}

DataCollector::~DataCollector() {
    cleanup();
    saveStatistics();
}

bool DataCollector::initialize() {
    std::lock_guard<std::mutex> lock(config_mutex_);
    
    if (is_initialized_.load()) {
        return true;
    }
    
    try {
        // 加载历史会话数据
        loadSessions();
        
        // 清理过期会话
        cleanupOldSessions();
        
        is_initialized_ = true;
        return true;
        
    } catch (const std::exception& e) {
        // 错误处理在主类中实现
        return false;
    }
}

void DataCollector::cleanup() {
    if (is_capturing_.load()) {
        stop();
    }
    
    // 结束当前会话
    endCurrentSession();
    
    // 保存统计数据
    saveStatistics();
    
    is_initialized_ = false;
}

bool DataCollector::start() {
    if (!is_initialized_.load()) {
        return false;
    }
    
    if (is_capturing_.load()) {
        return true;
    }
    
    try {
        // 创建新会话
        current_session_ = createNewSession();
        
        is_capturing_ = true;
        is_paused_ = false;
        
        triggerSessionStartedCallback(current_session_);
        return true;
        
    } catch (const std::exception& e) {
        return false;
    }
}

bool DataCollector::stop() {
    if (!is_capturing_.load()) {
        return true;
    }
    
    try {
        // 结束当前会话
        endCurrentSession();
        
        is_capturing_ = false;
        is_paused_ = false;
        
        return true;
        
    } catch (const std::exception& e) {
        return false;
    }
}

bool DataCollector::pause() {
    if (!is_capturing_.load() || is_paused_.load()) {
        return false;
    }
    
    is_paused_ = true;
    return true;
}

bool DataCollector::resume() {
    if (!is_capturing_.load() || !is_paused_.load()) {
        return false;
    }
    
    is_paused_ = false;
    return true;
}

bool DataCollector::captureInput(const std::string& text, const std::string& metadata) {
    if (!is_capturing_.load() || is_paused_.load()) {
        return false;
    }
    
    if (text.empty()) {
        return false;
    }
    
    try {
        // 检测输入类型
        InputType input_type = detectInputType(text);
        
        // 检查是否应该采集此输入
        if (!shouldCaptureInput(text, input_type)) {
            return false;
        }
        
        // 清理和敏感化输入
        std::string sanitized_text = sanitizeInput(text);
        
        // 分析重要性
        Importance importance = analyzeInputImportance(sanitized_text, input_type);
        
        // 创建输入记录
        auto record = std::make_shared<InputRecord>(
            sanitized_text,
            metadata,
            inputTypeToString(input_type),
            categorizeInput(sanitized_text),
            static_cast<int>(importance),
            getCurrentTime()
        );
        
        // 添加到当前会话
        {
            std::lock_guard<std::mutex> lock(sessions_mutex_);
            
            if (!current_session_) {
                current_session_ = createNewSession();
                triggerSessionStartedCallback(current_session_);
            }
            
            current_session_->inputs.push_back(record);
            
            // 更新会话重要性
            if (importance > static_cast<Importance>(current_session_->importance)) {
                current_session_->importance = static_cast<int>(importance);
            }
            
            // 更新会话活动时间
            updateSessionActivity();
        }
        
        // 更新统计信息
        updateStatistics(record);
        
        // 触发回调
        triggerInputCapturedCallback(record);
        
        return true;
        
    } catch (const std::exception& e) {
        return false;
    }
}

bool DataCollector::captureKeyEvent(int keycode, int action) {
    if (!is_capturing_.load() || is_paused_.load()) {
        return false;
    }
    
    if (!getCaptureGestures()) {
        return false;
    }
    
    try {
        std::string metadata = "key_event:" + std::to_string(keycode) + ":" + std::to_string(action);
        std::string text = "";
        
        // 对于特殊按键，可以记录按键信息
        if (keycode == 66) { // Enter键
            text = "[ENTER]";
        } else if (keycode == 67) { // Backspace键
            text = "[BACKSPACE]";
        } else if (keycode == 62) { // Space键
            text = " ";
        }
        
        return captureInput(text, metadata);
        
    } catch (const std::exception& e) {
        return false;
    }
}

bool DataCollector::captureGesture(int gesture_type, const std::vector<float>& points) {
    if (!is_capturing_.load() || is_paused_.load()) {
        return false;
    }
    
    if (!getCaptureGestures()) {
        return false;
    }
    
    try {
        std::stringstream ss;
        ss << "[GESTURE:" << gesture_type << ":" << points.size() << "pts]";
        
        std::string metadata = "gesture:" + std::to_string(gesture_type);
        return captureInput(ss.str(), metadata);
        
    } catch (const std::exception& e) {
        return false;
    }
}

std::shared_ptr<InputSession> DataCollector::getCurrentSession() const {
    std::lock_guard<std::mutex> lock(sessions_mutex_);
    return current_session_;
}

std::vector<std::shared_ptr<InputSession>> DataCollector::getRecentSessions(int limit) const {
    std::lock_guard<std::mutex> lock(sessions_mutex_);
    
    std::vector<std::shared_ptr<InputSession>> recent_sessions = sessions_;
    
    // 按开始时间排序
    std::sort(recent_sessions.begin(), recent_sessions.end(),
              [](const std::shared_ptr<InputSession>& a, const std::shared_ptr<InputSession>& b) {
                  return a->start_time > b->start_time;
              });
    
    // 限制数量
    if (recent_sessions.size() > static_cast<size_t>(limit)) {
        recent_sessions.resize(limit);
    }
    
    return recent_sessions;
}

std::vector<std::shared_ptr<InputRecord>> DataCollector::getHighImportanceInputs(Importance min_importance) const {
    std::lock_guard<std::mutex> lock(sessions_mutex_);
    
    std::vector<std::shared_ptr<InputRecord>> high_importance_inputs;
    
    for (const auto& session : sessions_) {
        for (const auto& record : session->inputs) {
            if (record->importance >= static_cast<int>(min_importance)) {
                high_importance_inputs.push_back(record);
            }
        }
    }
    
    // 按时间排序
    std::sort(high_importance_inputs.begin(), high_importance_inputs.end(),
              [](const std::shared_ptr<InputRecord>& a, const std::shared_ptr<InputRecord>& b) {
                  return a->timestamp > b->timestamp;
              });
    
    return high_importance_inputs;
}

std::vector<std::shared_ptr<InputRecord>> DataCollector::searchInputs(const std::string& query, int limit) const {
    std::lock_guard<std::mutex> lock(sessions_mutex_);
    
    std::vector<std::shared_ptr<InputRecord>> search_results;
    
    // 简单的文本搜索
    for (const auto& session : sessions_) {
        for (const auto& record : session->inputs) {
            if (record->text.find(query) != std::string::npos) {
                search_results.push_back(record);
            }
        }
    }
    
    // 按时间排序
    std::sort(search_results.begin(), search_results.end(),
              [](const std::shared_ptr<InputRecord>& a, const std::shared_ptr<InputRecord>& b) {
                  return a->timestamp > b->timestamp;
              });
    
    // 限制数量
    if (search_results.size() > static_cast<size_t>(limit)) {
        search_results.resize(limit);
    }
    
    return search_results;
}

Statistics DataCollector::getStatistics() const {
    std::lock_guard<std::mutex> lock(stats_mutex_);
    return statistics_;
}

void DataCollector::resetStatistics() {
    std::lock_guard<std::mutex> lock(stats_mutex_);
    statistics_ = Statistics();
    saveStatistics();
}

void DataCollector::clearOldSessions(int64_t max_age_ms) {
    std::lock_guard<std::mutex> lock(sessions_mutex_);
    
    int64_t current_time = getCurrentTime();
    int64_t cutoff_time = current_time - max_age_ms;
    
    auto it = sessions_.begin();
    while (it != sessions_.end()) {
        if (!(*it)->is_active && (*it)->end_time < cutoff_time) {
            it = sessions_.erase(it);
        } else {
            ++it;
        }
    }
    
    // 更新统计信息
    updateStatistics(nullptr);
}

// 私有方法实现

std::string DataCollector::generateSessionId() {
    std::stringstream ss;
    ss << "session_" << next_session_id_ << "_" << getCurrentTime();
    next_session_id_ = std::to_string(std::stoi(next_session_id_) + 1);
    return ss.str();
}

std::shared_ptr<InputSession> DataCollector::createNewSession() {
    auto session = std::make_shared<InputSession>(generateSessionId());
    session->start_time = getCurrentTime();
    session->is_active = true;
    session->importance = static_cast<int>(Importance::LOW);
    
    return session;
}

void DataCollector::endCurrentSession() {
    if (!current_session_) {
        return;
    }
    
    current_session_->end_time = getCurrentTime();
    current_session_->is_active = false;
    
    // 分析会话重要性
    analyzeSessionImportance(current_session_);
    
    // 添加到会话列表
    {
        std::lock_guard<std::mutex> lock(sessions_mutex_);
        sessions_.push_back(current_session_);
    }
    
    // 更新统计信息
    updateStatistics(current_session_);
    
    // 触发回调
    triggerSessionEndedCallback(current_session_);
    
    // 保存会话
    saveSession(current_session_);
    
    current_session_.reset();
}

void DataCollector::cleanupOldSessions() {
    clearOldSessions();
}

DataCollector::InputType DataCollector::detectInputType(const std::string& text) const {
    if (std::regex_search(text, PASSWORD_REGEX)) {
        return InputType::PASSWORD;
    }
    
    if (std::regex_search(text, EMAIL_REGEX)) {
        return InputType::EMAIL;
    }
    
    if (std::regex_search(text, PHONE_REGEX)) {
        return InputType::PHONE;
    }
    
    if (text.find("http://") == 0 || text.find("https://") == 0) {
        return InputType::URL;
    }
    
    if (text.find("*") != std::string::npos && text.length() > 6) {
        return InputType::PASSWORD;
    }
    
    if (std::all_of(text.begin(), text.end(), ::isdigit)) {
        return InputType::NUMBER;
    }
    
    if (std::all_of(text.begin(), text.end(), ::ispunct) || 
        std::all_of(text.begin(), text.end(), ::isspace)) {
        return InputType::SYMBOL;
    }
    
    if (text.find("/") == 0) {
        return InputType::COMMAND;
    }
    
    // 简单的表情符号检测
    if (text.length() <= 2 && (text.find("😀") != std::string::npos || 
                               text.find("😊") != std::string::npos ||
                               text.find("❤") != std::string::npos)) {
        return InputType::EMOJI;
    }
    
    return InputType::TEXT;
}

std::string DataCollector::categorizeInput(const std::string& text) const {
    if (isSensitiveInput(text)) {
        return "sensitive";
    }
    
    if (std::regex_search(text, EMAIL_REGEX) || std::regex_search(text, PHONE_REGEX)) {
        return "contact";
    }
    
    if (text.find("http") == 0) {
        return "url";
    }
    
    if (std::all_of(text.begin(), text.end(), ::isdigit)) {
        return "number";
    }
    
    if (text.length() > 100) {
        return "long_text";
    }
    
    if (text.length() < 3) {
        return "short_text";
    }
    
    return "general";
}

DataCollector::Importance DataCollector::analyzeInputImportance(const std::string& text, InputType type) const {
    // 敏感信息检测
    if (isSensitiveInput(text)) {
        return Importance::CRITICAL;
    }
    
    // 输入类型重要性
    switch (type) {
        case InputType::PASSWORD:
            return Importance::CRITICAL;
        case InputType::EMAIL:
        case InputType::PHONE:
            return Importance::HIGH;
        case InputType::URL:
            return Importance::NORMAL;
        case InputType::COMMAND:
            return Importance::HIGH;
        default:
            return Importance::NORMAL;
    }
}

bool DataCollector::isSensitiveInput(const std::string& text) const {
    std::string lower_text = text;
    std::transform(lower_text.begin(), lower_text.end(), lower_text.begin(), ::tolower);
    
    // 检查敏感关键词
    for (const auto& keyword : SENSITIVE_KEYWORDS) {
        if (lower_text.find(keyword) != std::string::npos) {
            return true;
        }
    }
    
    // 检查敏感信息模式
    return (std::regex_search(text, PASSWORD_REGEX) ||
            std::regex_search(text, EMAIL_REGEX) ||
            std::regex_search(text, PHONE_REGEX) ||
            std::regex_search(text, ID_CARD_REGEX) ||
            std::regex_search(text, BANK_CARD_REGEX));
}

bool DataCollector::shouldCaptureInput(const std::string& text, InputType type) const {
    switch (capture_mode_) {
        case CaptureMode::ALL:
            return true;
            
        case CaptureMode::FILTERED:
            // 过滤模式：只采集非敏感信息
            return !isSensitiveInput(text);
            
        case CaptureMode::SMART:
        default:
            // 智能模式：基于类型和配置决定
            if (type == InputType::PASSWORD) {
                return capture_sensitive_data_;
            }
            
            if (type == InputType::EMOJI) {
                return capture_emoji_;
            }
            
            return true;
    }
}

std::string DataCollector::sanitizeInput(const std::string& text) const {
    if (!capture_sensitive_data_ && isSensitiveInput(text)) {
        return "[FILTERED]";
    }
    
    // 对于密码类型，部分屏蔽
    if (detectInputType(text) == InputType::PASSWORD && text.length() > 4) {
        return text.substr(0, 2) + "***" + text.substr(text.length() - 2);
    }
    
    return text;
}

void DataCollector::updateSessionActivity() {
    if (current_session_) {
        current_session_->end_time = getCurrentTime();
    }
}

void DataCollector::analyzeSessionImportance(std::shared_ptr<InputSession> session) {
    int max_importance = static_cast<int>(Importance::LOW);
    
    for (const auto& record : session->inputs) {
        if (record->importance > max_importance) {
            max_importance = record->importance;
        }
    }
    
    session->importance = max_importance;
    
    // 长会话可能更重要
    if (session->inputs.size() > 20) {
        session->importance = std::min(session->importance + 1, static_cast<int>(Importance::CRITICAL));
    }
}

void DataCollector::updateStatistics(const std::shared_ptr<InputRecord>& record) {
    if (!record) return;
    
    std::lock_guard<std::mutex> lock(stats_mutex_);
    statistics_.total_inputs++;
    statistics_.last_capture_time = record->timestamp;
    
    if (record->importance >= static_cast<int>(Importance::HIGH)) {
        statistics_.high_importance_sessions++;
    }
    
    if (record->importance >= static_cast<int>(Importance::CRITICAL)) {
        statistics_.critical_inputs++;
    }
}

void DataCollector::updateStatistics(const std::shared_ptr<InputSession>& session) {
    if (!session) return;
    
    std::lock_guard<std::mutex> lock(stats_mutex_);
    
    statistics_.total_sessions++;
    
    if (session->is_active) {
        statistics_.active_sessions++;
    }
    
    // 更新平均会话长度
    if (statistics_.total_sessions > 0) {
        statistics_.average_session_length = 
            (statistics_.average_session_length * (statistics_.total_sessions - 1) + session->inputs.size()) 
            / statistics_.total_sessions;
    }
    
    // 更新平均会话持续时间
    if (session->end_time > session->start_time) {
        int64_t duration = session->end_time - session->start_time;
        if (statistics_.total_sessions > 0) {
            statistics_.average_session_duration = 
                (statistics_.average_session_duration * (statistics_.total_sessions - 1) + duration) 
                / statistics_.total_sessions;
        }
    }
}

int64_t DataCollector::getCurrentTime() const {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
}

std::string DataCollector::inputTypeToString(InputType type) const {
    switch (type) {
        case InputType::TEXT: return "text";
        case InputType::PASSWORD: return "password";
        case InputType::EMAIL: return "email";
        case InputType::PHONE: return "phone";
        case InputType::URL: return "url";
        case InputType::NUMBER: return "number";
        case InputType::SYMBOL: return "symbol";
        case InputType::EMOJI: return "emoji";
        case InputType::COMMAND: return "command";
        default: return "unknown";
    }
}

// 回调方法
void DataCollector::setInputCapturedCallback(InputCapturedCallback callback) {
    input_captured_callback_ = callback;
}

void DataCollector::setSessionStartedCallback(SessionStartedCallback callback) {
    session_started_callback_ = callback;
}

void DataCollector::setSessionEndedCallback(SessionEndedCallback callback) {
    session_ended_callback_ = callback;
}

void DataCollector::triggerInputCapturedCallback(const std::shared_ptr<InputRecord>& record) {
    if (input_captured_callback_) {
        try {
            input_captured_callback_(record);
        } catch (const std::exception& e) {
            // 忽略回调异常
        }
    }
}

void DataCollector::triggerSessionStartedCallback(const std::shared_ptr<InputSession>& session) {
    if (session_started_callback_) {
        try {
            session_started_callback_(session);
        } catch (const std::exception& e) {
            // 忽略回调异常
        }
    }
}

void DataCollector::triggerSessionEndedCallback(const std::shared_ptr<InputSession>& session) {
    if (session_ended_callback_) {
        try {
            session_ended_callback_(session);
        } catch (const std::exception& e) {
            // 忽略回调异常
        }
    }
}

// 配置方法
void DataCollector::setCaptureMode(CaptureMode mode) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    capture_mode_ = mode;
}

void DataCollector::setSessionTimeout(int64_t timeout_ms) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    session_timeout_ms_ = timeout_ms;
}

void DataCollector::setMinSessionLength(int min_length) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    min_session_length_ = min_length;
}

void DataCollector::setCaptureSensitiveData(bool enable) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    capture_sensitive_data_ = enable;
}

void DataCollector::setCaptureEmoji(bool enable) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    capture_emoji_ = enable;
}

void DataCollector::setCaptureGestures(bool enable) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    capture_gestures_ = enable;
}

// 持久化方法（简单实现）
void DataCollector::saveSession(const std::shared_ptr<InputSession>& session) const {
    // TODO: 实现会话持久化
}

void DataCollector::loadSessions() {
    // TODO: 实现会话加载
}

void DataCollector::saveStatistics() const {
    // TODO: 实现统计信息持久化
}

void DataCollector::loadStatistics() {
    // TODO: 实现统计信息加载
}

void DataCollector::exportData(const std::string& file_path) const {
    // TODO: 实现数据导出
}

void DataCollector::importData(const std::string& file_path) {
    // TODO: 实现数据导入
}

} // namespace trime
} // namespace syncrime