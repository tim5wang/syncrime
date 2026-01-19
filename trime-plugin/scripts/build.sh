# SyncRime Trime 插件构建脚本

#!/bin/bash

# SyncRime Trime 插件构建脚本
# 支持 Linux、macOS 和 Windows (WSL)

set -e

# 配置变量
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PLUGIN_ROOT="$PROJECT_ROOT/trime-plugin"
BUILD_DIR="$PLUGIN_ROOT/build"
ANDROID_BUILD_DIR="$PLUGIN_ROOT/app/build"
NDK_VERSION="25.2.9519653"
BUILD_TYPE="Release"
CLEAN_BUILD=false

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
SyncRime Trime 插件构建脚本

用法: $0 [选项]

选项:
    -h, --help              显示此帮助信息
    -c, --clean             清理构建目录
    -d, --debug             构建调试版本
    -r, --release           构建发布版本 (默认)
    -t, --test              运行测试
    -p, --package           打包 APK
    -a, --analyze           分析代码
    -f, --format            格式化代码
    -l, --lint              运行代码检查
    --ndk-version VER       指定 NDK 版本 (默认: $NDK_VERSION)
    --build-dir DIR         指定构建目录 (默认: $BUILD_DIR)
    
示例:
    $0                      # 构建发布版本
    $0 -c -r                # 清理并构建发布版本
    $0 -d -t                # 构建调试版本并运行测试
    $0 -p                   # 打包 APK
    
EOF
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--clean)
                CLEAN_BUILD=true
                shift
                ;;
            -d|--debug)
                BUILD_TYPE="Debug"
                shift
                ;;
            -r|--release)
                BUILD_TYPE="Release"
                shift
                ;;
            -t|--test)
                RUN_TESTS=true
                shift
                ;;
            -p|--package)
                PACKAGE_APK=true
                shift
                ;;
            -a|--analyze)
                ANALYZE_CODE=true
                shift
                ;;
            -f|--format)
                FORMAT_CODE=true
                shift
                ;;
            -l|--lint)
                LINT_CODE=true
                shift
                ;;
            --ndk-version)
                NDK_VERSION="$2"
                shift 2
                ;;
            --build-dir)
                BUILD_DIR="$2"
                shift 2
                ;;
            *)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# 检查依赖
check_dependencies() {
    log_info "检查构建依赖..."
    
    # 检查 Android SDK
    if [[ -z "$ANDROID_HOME" ]]; then
        log_error "未设置 ANDROID_HOME 环境变量"
        exit 1
    fi
    
    # 检查 Android NDK
    if [[ -z "$ANDROID_NDK_HOME" ]]; then
        log_error "未设置 ANDROID_NDK_HOME 环境变量"
        exit 1
    fi
    
    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "未找到 Java"
        exit 1
    fi
    
    # 检查 Gradle
    if ! command -v gradle &> /dev/null; then
        log_warning "未找到 Gradle，将使用 Gradle Wrapper"
    fi
    
    # 检查 CMake
    if ! command -v cmake &> /dev/null; then
        log_error "未找到 CMake"
        exit 1
    fi
    
    # 检查 Git
    if ! command -v git &> /dev/null; then
        log_error "未找到 Git"
        exit 1
    fi
    
    log_success "依赖检查完成"
}

# 清理构建目录
clean_build() {
    log_info "清理构建目录..."
    
    if [[ -d "$BUILD_DIR" ]]; then
        rm -rf "$BUILD_DIR"
    fi
    
    if [[ -d "$ANDROID_BUILD_DIR" ]]; then
        rm -rf "$ANDROID_BUILD_DIR"
    fi
    
    # 清理 Gradle 缓存
    if [[ -f "$PLUGIN_ROOT/gradlew" ]]; then
        (cd "$PLUGIN_ROOT" && ./gradlew clean) || {
            log_warning "Gradle 清理失败"
        }
    fi
    
    log_success "清理完成"
}

# 设置环境变量
setup_environment() {
    log_info "设置构建环境..."
    
    # 设置 NDK 版本
    export ANDROID_NDK_VERSION="$NDK_VERSION"
    
    # 设置构建类型
    export SYNC_RIME_BUILD_TYPE="$BUILD_TYPE"
    
    # 设置架构
    export SYNC_RIME_ARCHS="arm64-v8a,armeabi-v7a,x86_64,x86"
    
    log_success "环境设置完成"
}

# 构建 C++ 库
build_native() {
    log_info "构建原生 C++ 库..."
    
    # 进入插件目录
    cd "$PLUGIN_ROOT"
    
    # 运行 Gradle 构建
    if [[ -f "gradlew" ]]; then
        ./gradlew assemble$BUILD_TYPE
    else
        log_error "未找到 Gradle Wrapper"
        exit 1
    fi
    
    log_success "C++ 库构建完成"
}

# 构建插件 APK
build_apk() {
    log_info "构建插件 APK..."
    
    cd "$PLUGIN_ROOT"
    
    # 构建 APK
    ./gradlew assemble$BUILD_TYPE
    
    # 查找生成的 APK
    local apk_path="$ANDROID_BUILD_DIR/outputs/apk/$BUILD_TYPE"
    if [[ -d "$apk_path" ]]; then
        local apk_files=("$apk_path"/*.apk)
        if [[ ${#apk_files[@]} -gt 0 ]]; then
            log_success "APK 构建完成: ${apk_files[0]}"
        else
            log_warning "未找到 APK 文件"
        fi
    else
        log_warning "APK 构建目录不存在"
    fi
}

# 运行测试
run_tests() {
    log_info "运行测试..."
    
    cd "$PLUGIN_ROOT"
    
    # 运行单元测试
    ./gradlew test$BUILD_TYPE
    
    # 运行 Android 测试
    if command -v adb &> /dev/null; then
        ./gradlew connected$BUILD_TYPE
    else
        log_warning "未找到 adb，跳过连接测试"
    fi
    
    log_success "测试完成"
}

# 分析代码
analyze_code() {
    log_info "分析代码..."
    
    cd "$PLUGIN_ROOT"
    
    # 运行静态分析
    ./gradlew lint$BUILD_TYPE
    
    # 检查代码覆盖率
    if [[ "$BUILD_TYPE" == "Debug" ]]; then
        ./gradlew jacocoTestReport
    fi
    
    log_success "代码分析完成"
}

# 格式化代码
format_code() {
    log_info "格式化代码..."
    
    cd "$PLUGIN_ROOT"
    
    # 格式化 Kotlin 代码
    if command -v ktlint &> /dev/null; then
        ktlint -F "app/src/**/*.kt"
    fi
    
    # 格式化 C++ 代码
    if command -v clang-format &> /dev/null; then
        find "app/src/main/cpp" -name "*.cpp" -o -name "*.h" -o -name "*.hpp" | xargs clang-format -i
    fi
    
    log_success "代码格式化完成"
}

# 运行代码检查
lint_code() {
    log_info "运行代码检查..."
    
    cd "$PLUGIN_ROOT"
    
    # 检查 Kotlin 代码
    if command -v ktlint &> /dev/null; then
        ktlint "app/src/**/*.kt"
    fi
    
    # 检查 C++ 代码
    if command -v cppcheck &> /dev/null; then
        cppcheck --enable=all --std=c++17 "app/src/main/cpp/"
    fi
    
    log_success "代码检查完成"
}

# 打包 APK
package_apk() {
    log_info "打包 APK..."
    
    cd "$PLUGIN_ROOT"
    
    # 签名配置
    if [[ ! -f "keystore.properties" ]]; then
        log_warning "未找到签名配置，将使用调试签名"
    fi
    
    # 构建发布版本
    if [[ "$BUILD_TYPE" == "Release" ]]; then
        ./gradlew assembleRelease
    else
        ./gradlew assembleDebug
    fi
    
    # 创建输出目录
    local output_dir="$BUILD_DIR/distributions"
    mkdir -p "$output_dir"
    
    # 复制 APK 文件
    local apk_path="$ANDROID_BUILD_DIR/outputs/apk/$BUILD_TYPE"
    if [[ -d "$apk_path" ]]; then
        cp "$apk_path"/*.apk "$output_dir/"
        log_success "APK 打包完成: $output_dir/"
    fi
    
    # 创建发布包
    local version=$(grep "versionCode" "$PLUGIN_ROOT/app/build.gradle" | awk '{print $2}' | tr -d "'\"")
    local package_name="syncrime-trime-plugin-v$version.apk"
    
    if [[ -f "$output_dir/app-$BUILD_TYPE.apk" ]]; then
        cp "$output_dir/app-$BUILD_TYPE.apk" "$output_dir/$package_name"
        log_success "发布包创建完成: $output_dir/$package_name"
    fi
}

# 显示构建信息
show_build_info() {
    log_info "构建信息:"
    echo "  项目目录: $PROJECT_ROOT"
    echo "  插件目录: $PLUGIN_ROOT"
    echo "  构建目录: $BUILD_DIR"
    echo "  构建类型: $BUILD_TYPE"
    echo "  NDK 版本: $NDK_VERSION"
    echo "  Android SDK: $ANDROID_HOME"
    echo "  Android NDK: $ANDROID_NDK_HOME"
    echo ""
}

# 主函数
main() {
    log_info "SyncRime Trime 插件构建开始"
    
    # 解析参数
    parse_args "$@"
    
    # 显示构建信息
    show_build_info
    
    # 检查依赖
    check_dependencies
    
    # 清理构建目录
    if [[ "$CLEAN_BUILD" == true ]]; then
        clean_build
    fi
    
    # 设置环境
    setup_environment
    
    # 构建原生库
    build_native
    
    # 构建 APK
    build_apk
    
    # 运行测试
    if [[ "$RUN_TESTS" == true ]]; then
        run_tests
    fi
    
    # 分析代码
    if [[ "$ANALYZE_CODE" == true ]]; then
        analyze_code
    fi
    
    # 格式化代码
    if [[ "$FORMAT_CODE" == true ]]; then
        format_code
    fi
    
    # 运行代码检查
    if [[ "$LINT_CODE" == true ]]; then
        lint_code
    fi
    
    # 打包 APK
    if [[ "$PACKAGE_APK" == true ]]; then
        package_apk
    fi
    
    log_success "SyncRime Trime 插件构建完成"
}

# 运行主函数
main "$@"