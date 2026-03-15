# 贡献指南

感谢你考虑为 SyncRime 做出贡献！🎉

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发环境设置](#开发环境设置)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)

---

## 行为准则

本项目采用 [Contributor Covenant](https://www.contributor-covenant.org/) 行为准则。请尊重所有贡献者和用户。

---

## 如何贡献

### 报告 Bug
1. 使用 [GitHub Issues](https://github.com/tim5wang/syncrime/issues) 报告
2. 提供详细信息：
   - 问题描述
   - 复现步骤
   - 预期行为
   - 实际行为
   - 环境信息（Android 版本、设备型号等）

### 提出新功能
1. 先在 Issues 中讨论
2. 说明使用场景和价值
3. 等待项目维护者确认

### 提交代码
1. Fork 项目
2. 创建功能分支
3. 提交代码
4. 推送到分支
5. 创建 Pull Request

---

## 开发环境设置

### 前置要求
- Android Studio Arctic Fox 或更高版本
- JDK 11+
- Android SDK 31+
- Git

### 步骤

```bash
# 1. Fork 并克隆
git clone https://github.com/YOUR_USERNAME/syncrime.git
cd syncrime

# 2. 添加上游远程仓库
git remote add upstream https://github.com/tim5wang/syncrime.git

# 3. 使用 Android Studio 打开对应模块
# - Android 客户端：打开 android-client/
# - Trime 插件：打开 trime-plugin/
```

### 编译项目

```bash
# Android 客户端
cd android-client
./gradlew assembleDebug

# Trime 插件
cd ../trime-plugin
./gradlew assembleDebug
```

### 运行测试

```bash
# 单元测试
./gradlew test

# 仪器测试
./gradlew connectedAndroidTest
```

---

## 代码规范

### Kotlin 代码规范

遵循 [Kotlin 官方代码规范](https://kotlinlang.org/docs/coding-conventions.html)

#### 命名约定
```kotlin
// 类名：大驼峰
class MainActivity

// 函数和变量：小驼峰
fun calculateTotal()
val itemCount = 10

// 常量：大写下划线
const val MAX_RETRY_COUNT = 3

// 私有属性：下划线前缀
private val _state = MutableStateFlow(State())
```

#### 代码格式
```kotlin
// 使用 4 个空格缩进
class Example {
    fun method() {
        // 代码内容
    }
}

// 大括号不换行
if (condition) {
    // 正确
}

// 每行最多 120 字符
val longVariableName = "This is a very long string that should be wrapped"

// 导入顺序：Android -> 第三方 -> 项目内部
import android.content.Context
import androidx.compose.runtime.Composable

import kotlinx.coroutines.flow.Flow

import com.syncrime.android.data.repository.MainRepository
```

### 架构规范

遵循 **Clean Architecture** 原则：

```
presentation/  # UI 层
  ├── ui/      # Composable 函数和 ViewModel
  └── theme/   # 主题和样式

domain/        # 业务逻辑层
  ├── model/   # 数据模型
  ├── usecase/ # 用例
  └── repository/ # 仓库接口

data/          # 数据层
  ├── local/   # 本地数据源 (Room, DataStore)
  ├── remote/  # 远程数据源 (Retrofit)
  └── repository/ # 仓库实现
```

### 注释规范

```kotlin
/**
 * 智能推荐引擎
 * 
 * 负责基于用户输入历史和上下文提供个性化推荐
 * 
 * @property context 应用上下文
 * @property model 机器学习模型
 */
class IntelligentRecommendationEngine(
    private val context: Context,
    private val model: MLModel
) {
    /**
     * 获取推荐结果
     * 
     * @param input 用户输入
     * @param context 上下文信息
     * @return 推荐列表
     */
    fun getRecommendations(
        input: String,
        context: InputContext
    ): List<Recommendation> {
        // 实现
    }
}
```

---

## 提交规范

### Commit Message 格式

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具/配置

### 示例

```bash
# 新功能
feat(android): 添加统计页面
feat(intelligence): 实现语义分析引擎

# Bug 修复
fix(sync): 修复同步冲突问题
fix(ui): 修复深色模式下的颜色问题

# 文档
docs(readme): 更新安装说明
docs(api): 添加 API 文档

# 重构
refactor(domain): 重构用例层
refactor(data): 优化数据库查询

# 其他
chore(deps): 更新依赖版本
chore(ci): 添加 GitHub Actions 配置
```

### 提交前检查清单

- [ ] 代码通过编译
- [ ] 单元测试通过
- [ ] 代码符合规范
- [ ] 添加了必要的测试
- [ ] 更新了相关文档
- [ ] Commit message 符合规范

---

## Pull Request 流程

### 1. 创建分支

```bash
# 从 main 分支创建功能分支
git checkout -b feature/your-feature-name
# 或
git checkout -b fix/your-bug-fix
```

### 2. 提交代码

```bash
# 添加更改
git add .

# 提交（遵循提交规范）
git commit -m "feat(module): add new feature"

# 推送到远程
git push origin feature/your-feature-name
```

### 3. 创建 Pull Request

1. 访问你的 Fork 仓库
2. 点击 "Compare & pull request"
3. 填写 PR 描述：
   - 变更内容
   - 相关 Issue
   - 测试说明
   - 截图（如适用）

### 4. Code Review

- 等待维护者 review
- 根据反馈修改代码
- 保持沟通和专业

### 5. 合并

- 通过 review 后，维护者会合并
- 删除功能分支

---

## 开发建议

### Git 工作流

```bash
# 保持与上游同步
git fetch upstream
git rebase upstream/main

# 交互式变基（整理提交历史）
git rebase -i HEAD~3

# 解决冲突后继续
git rebase --continue
```

### 测试建议

- 为关键功能编写单元测试
- 使用 MockK 进行 Mock
- 保持测试独立和可重复
- 测试覆盖率目标：> 70%

### 性能建议

- 避免在主线程进行耗时操作
- 使用 Coroutines 进行异步编程
- 注意内存泄漏（特别是 Flow/StateFlow）
- 使用 Android Profiler 分析性能

---

## 常见问题

### Q: 如何调试 Trime 插件？
A: 使用 Android Studio 的 Debug 功能，附加到 Trime 进程。

### Q: 如何测试同步功能？
A: 可以使用本地测试服务器或 Mock 数据。

### Q: 贡献代码需要签署 CLA 吗？
A: 目前不需要，但请确保代码是你原创的。

---

## 资源

- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose 指南](https://developer.android.com/jetpack/compose)
- [Android 开发者指南](https://developer.android.com/guide)
- [Clean Architecture](https://proandroiddev.com/clean-architecture-on-android-943e39111729)

---

感谢你的贡献！🙏
