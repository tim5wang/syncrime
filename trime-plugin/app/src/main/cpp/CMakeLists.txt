cmake_minimum_required(VERSION 3.18.1)

project("syncrime-trime-plugin")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 编译选项
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -O2")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -DDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

# 包含目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party
)

# 源文件
file(GLOB_RECURSE SYNC_RIME_SOURCES
    "src/*.cpp"
    "src/*.c"
    "src/*.cc"
)

# 头文件
file(GLOB_RECURSE SYNC_RIME_HEADERS
    "include/*.h"
    "include/*.hpp"
    "src/*.h"
    "src/*.hpp"
)

# 创建共享库
add_library(
    syncrime_plugin
    SHARED
    ${SYNC_RIME_SOURCES}
    ${SYNC_RIME_HEADERS}
)

# 查找依赖库
find_library(
    log-lib
    log
)

find_library(
    jnigraphics-lib
    jnigraphics
)

# 链接库
target_link_libraries(
    syncrime_plugin
    ${log-lib}
    ${jnigraphics-lib}
    android
)

# 编译定义
target_compile_definitions(
    syncrime_plugin
    PRIVATE
        SYNC_RIME_VERSION="1.0.0"
        SYNC_RIME_BUILD="${CMAKE_BUILD_TYPE}"
        SYNC_RIME_PLATFORM="Android"
)

# 第三方库
add_subdirectory(third_party)

# 导出符号
set_target_properties(
    syncrime_plugin
    PROPERTIES
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
)

# 安装规则
install(
    TARGETS syncrime_plugin
    LIBRARY
        DESTINATION lib
)