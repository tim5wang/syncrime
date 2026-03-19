#!/bin/bash
# SyncRime 快速配置脚本

set -e

echo "======================================"
echo "  SyncRime 快速配置工具"
echo "======================================"
echo ""

# 检查 ADB 连接
echo "📱 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 未检测到设备，请连接手机后重试"
    exit 1
fi
echo "✅ 设备已连接"
echo ""

# 检查 SyncRime 是否安装
echo "🔍 检查 SyncRime 安装状态..."
if adb shell pm list packages | grep -q "com.syncrime.android"; then
    echo "✅ SyncRime 已安装"
    
    # 检查无障碍服务
    echo ""
    echo "🔧 检查无障碍服务状态..."
    ENABLED_SERVICES=$(adb shell settings get secure enabled_accessibility_services)
    
    if echo "$ENABLED_SERVICES" | grep -q "InputCaptureService"; then
        echo "✅ 无障碍服务已启用"
    else
        echo "⚠️  无障碍服务未启用"
        echo ""
        echo "请手动启用："
        echo "1. 设置 → 无障碍 → 找到 'SyncRime 输入采集'"
        echo "2. 开启服务"
        echo ""
        echo "或者执行以下 ADB 命令："
        echo "adb shell settings put secure enabled_accessibility_services \$(adb shell settings get secure enabled_accessibility_services):com.syncrime.android.debug/.accessibility.InputCaptureService"
    fi
else
    echo "❌ SyncRime 未安装"
    echo ""
    echo "请安装 SyncRime APK："
    echo "adb install ~/claw_workspaces/tim/syncrime/SyncRime-v1.0-debug.apk"
    exit 1
fi

# 检查 Trime 输入法
echo ""
echo "⌨️  检查 Trime 输入法..."
if adb shell pm list packages | grep -q "com.osfans.trime"; then
    echo "✅ Trime 输入法已安装"
else
    echo "⚠️  Trime 输入法未安装"
    echo ""
    echo "请下载并安装 Trime："
    echo "https://github.com/osfans/trime/releases"
fi

# 查看 SyncRime 数据库
echo ""
echo "💾 检查 SyncRime 数据库..."
DB_EXISTS=$(adb shell "ls /data/data/com.syncrime.android*/databases/syncrime.db 2>/dev/null" || echo "")

if [ -n "$DB_EXISTS" ]; then
    echo "✅ 数据库存在"
    
    # 尝试读取统计数据
    echo ""
    echo "📊 当前统计数据："
    adb shell "dumpsys activity service com.syncrime.android.debug/.accessibility.InputCaptureService" 2>/dev/null || echo "（需要 root 权限查看详细统计）"
else
    echo "⚠️  数据库不存在（可能还未开始采集）"
fi

# 查看日志
echo ""
echo "📝 最近日志（最后 10 条）："
adb logcat -s SyncRime --pid=$(adb shell pidof -s com.syncrime.android.debug 2>/dev/null || echo 0) -d | tail -10 || echo "（无法获取日志）"

echo ""
echo "======================================"
echo "  配置完成！"
echo "======================================"
echo ""
echo "📱 下一步："
echo "1. 打开 SyncRime App"
echo "2. 点击 '开始采集'"
echo "3. 开始输入测试"
echo ""
echo "📚 查看文档："
echo "- docs/getting-started/QUICK_START.md"
echo "- docs/getting-started/TRIME_INTEGRATION.md"
echo ""
