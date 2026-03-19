# Trime 同文输入法下载指南

## 📱 官方下载渠道

### 1. GitHub Releases（推荐）
**项目地址**: https://github.com/osfans/trime/releases

**最新版本**: 3.4.2 (2024)

**下载链接**:
- **ARM64 架构** (大多数现代手机): 
  https://github.com/osfans/trime/releases/download/3.4.2/trime-3.4.2-arm64-v8a.apk

- **ARMv7 架构** (旧款手机):
  https://github.com/osfans/trime/releases/download/3.4.2/trime-3.4.2-armeabi-v7a.apk

- **通用版本**:
  https://github.com/osfans/trime/releases/download/3.4.2/trime-3.4.2.apk

### 2. 酷安市场
**下载链接**: https://www.coolapk.com/apk/com.osfans.trime

### 3. F-Droid (开源应用商店)
**下载链接**: https://f-droid.org/packages/com.osfans.trime/

---

## 🔍 如何查看手机架构

```bash
# 通过 ADB 查看
adb shell getprop ro.product.cpu.abi

# 常见结果:
# - arm64-v8a  → 下载 ARM64 版本 (大多数手机)
# - armeabi-v7a → 下载 ARMv7 版本 (旧手机)
# - x86_64     → 下载 x86_64 版本 (模拟器/平板)
```

---

## 📦 安装步骤

### 方法 1: 直接安装 APK
1. 下载 APK 文件
2. 传输到手机
3. 点击安装
4. 启用 Trime 输入法

### 方法 2: ADB 安装
```bash
adb install trime-3.4.2-arm64-v8a.apk
```

---

## ⚙️ 启用 Trime 输入法

1. **设置 → 系统 → 语言和输入法**
2. **虚拟键盘 → 管理键盘**
3. **开启 Trime**
4. **切换输入法**: 下拉通知栏 → 选择输入法 → Trime

---

## 🔧 SyncRime 插件配置

安装 Trime 后，需要配置 SyncRime 插件：

1. 打开 SyncRime Android 客户端
2. 完成初始设置
3. 启用输入采集权限
4. Trime 会自动加载 SyncRime 插件

**配置文件位置**: 
```
/sdcard/rime/syncrime/
├── trime_syncrime.yaml
└── default.yaml
```

---

## 📊 版本对比

| 版本 | 发布日期 | 大小 | 特性 |
|------|---------|------|------|
| 3.4.2 | 2024 | ~15MB | 最新稳定版 |
| 3.4.1 | 2023 | ~15MB | 稳定版 |
| 3.4.0 | 2023 | ~14MB | 重大更新 |

---

## ⚠️ 注意事项

1. **Android 版本要求**: Android 5.0+
2. **权限需求**: 
   - 存储空间 (读取词库)
   - 网络 (同步配置)
3. **首次使用**: 需要下载词库和配置

---

## 🔗 相关链接

- **GitHub 项目**: https://github.com/osfans/trime
- **官方文档**: https://github.com/osfans/trime/wiki
- **配置指南**: https://github.com/osfans/trime/wiki/配置指南
- **常见问题**: https://github.com/osfans/trime/issues

---

*最后更新：2026-03-18*
