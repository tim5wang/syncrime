cmake_minimum_required(VERSION 3.18.1)

# 第三方库配置

# JSON 库 (nlohmann/json)
option(SYNC_RIME_USE_SYSTEM_JSON "Use system nlohmann/json library" OFF)
if(SYNC_RIME_USE_SYSTEM_JSON)
    find_package(nlohmann_json REQUIRED)
else()
    # 下载并构建 nlohmann/json
    include(FetchContent)
    FetchContent_Declare(
        nlohmann_json
        GIT_REPOSITORY https://github.com/nlohmann/json.git
        GIT_TAG v3.11.2
    )
    FetchContent_MakeAvailable(nlohmann_json)
    set(SYNC_RIME_JSON_LIB nlohmann_json::nlohmann_json)
endif()

# OpenSSL (可选，用于加密功能)
option(SYNC_RIME_ENABLE_ENCRYPTION "Enable encryption features" ON)
if(SYNC_RIME_ENABLE_ENCRYPTION)
    find_package(OpenSSL REQUIRED)
    if(OpenSSL_FOUND)
        set(SYNC_RIME_CRYPTO_LIB OpenSSL::SSL OpenSSL::Crypto)
        add_definitions(-DSYNC_RIME_ENABLE_ENCRYPTION)
    else()
        message(WARNING "OpenSSL not found, encryption features will be disabled")
        set(SYNC_RIME_CRYPTO_LIB "")
    endif()
else()
    set(SYNC_RIME_CRYPTO_LIB "")
endif()

# Zlib (用于数据压缩)
find_package(ZLIB REQUIRED)
if(ZLIB_FOUND)
    set(SYNC_RIME_ZLIB_LIB ZLIB::ZLIB)
    add_definitions(-DSYNC_RIME_ENABLE_COMPRESSION)
else()
    message(WARNING "Zlib not found, compression features will be disabled")
    set(SYNC_RIME_ZLIB_LIB "")
endif()

# 线程库
find_package(Threads REQUIRED)

# 日志库 (Android)
if(ANDROID)
    find_library(log-lib log)
    set(SYNC_RIME_LOG_LIB ${log-lib})
else()
    # 对于其他平台，可以使用 spdlog
    option(SYNC_RIME_USE_SPDLOG "Use spdlog for logging" OFF)
    if(SYNC_RIME_USE_SPDLOG)
        find_package(spdlog REQUIRED)
        set(SYNC_RIME_LOG_LIB spdlog::spdlog)
    else()
        set(SYNC_RIME_LOG_LIB "")
    endif()
endif()

# 设置第三方库变量
set(SYNC_RIME_THIRD_PARTY_LIBS
    ${SYNC_RIME_JSON_LIB}
    ${SYNC_RIME_CRYPTO_LIB}
    ${SYNC_RIME_ZLIB_LIB}
    Threads::Threads
    ${SYNC_RIME_LOG_LIB}
)

# 编译选项
if(MSVC)
    # Visual Studio 编译选项
    add_compile_options(/W4)
    if(CMAKE_BUILD_TYPE STREQUAL "Release")
        add_compile_options(/O2 /DNDEBUG)
    else()
        add_compile_options(/Od /DDEBUG /Zi)
    endif()
else()
    # GCC/Clang 编译选项
    add_compile_options(-Wall -Wextra -Wpedantic)
    if(CMAKE_BUILD_TYPE STREQUAL "Release")
        add_compile_options(-O3 -DNDEBUG)
    else()
        add_compile_options(-O0 -g -DDEBUG)
    endif()
    
    # C++17 特性
    add_compile_options(-std=c++17)
endif()

# 预处理器定义
add_definitions(
    -DSYNC_RIME_VERSION="1.0.0"
    -DSYNC_RIME_BUILD="${CMAKE_BUILD_TYPE}"
    -DSYNC_RIME_PLATFORM="${CMAKE_SYSTEM_NAME}"
)

# Android 特定配置
if(ANDROID)
    add_definitions(-DANDROID_PLATFORM)
    add_definitions(-DANDROID_NDK)
    
    # Android API 级别
    add_definitions(-DANDROID_API_LEVEL=${ANDROID_NATIVE_API_LEVEL})
    
    # 架构信息
    add_definitions(-DANDROID_ARCH="${CMAKE_ANDROID_ARCH_ABI}")
endif()

# 测试支持
option(SYNC_RIME_BUILD_TESTS "Build tests" OFF)
if(SYNC_RIME_BUILD_TESTS)
    enable_testing()
    find_package(GTest REQUIRED)
    add_subdirectory(tests)
endif()

# 示例程序支持
option(SYNC_RIME_BUILD_EXAMPLES "Build examples" OFF)
if(SYNC_RIME_BUILD_EXAMPLES)
    add_subdirectory(examples)
endif()

# 文档生成
option(SYNC_RIME_BUILD_DOCS "Build documentation" OFF)
if(SYNC_RIME_BUILD_DOCS)
    find_package(Doxygen)
    if(DOXYGEN_FOUND)
        add_subdirectory(docs)
    endif()
endif()

# 包安装配置
include(GNUInstallDirs)

install(
    DIRECTORY include/
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/syncrime
    FILES_MATCHING PATTERN "*.h" PATTERN "*.hpp"
)

# 生成 pkg-config 文件
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/syncrime.pc.in"
    "${CMAKE_CURRENT_BINARY_DIR}/syncrime.pc"
    @ONLY
)

install(
    FILES "${CMAKE_CURRENT_BINARY_DIR}/syncrime.pc"
    DESTINATION ${CMAKE_INSTALL_LIBDIR}/pkgconfig
)