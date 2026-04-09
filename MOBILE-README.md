# MoviePilot Android App 开发指南

基于 Capacitor 的 Android 移动应用。

## 项目结构

```
MoviePilot-Mobile/
├── android/              # Android 原生项目
├── src/
│   ├── utils/
│   │   └── capacitor.ts  # Capacitor 初始化工具
│   └── styles/
│       └── mobile.scss   # 移动端适配样式
├── capacitor.config.json # Capacitor 配置
└── package.json
```

## 环境要求

### 基础环境
- Node.js >= 20.12.1
- npm >= 10.0

### Android 开发
- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **Android SDK**: API Level 34 (Android 14)
- **JDK**: 17
- **Gradle**: 8.0+

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 构建 Web 资源并同步到 Android

```bash
npm run mobile:sync
```

### 3. 在 Android Studio 中打开

```bash
npm run mobile:open
```

### 4. 运行到模拟器或真机

在 Android Studio 中点击 Run 按钮，或使用命令行：

```bash
npm run mobile:run
```

## 开发工作流

### 开发模式（带实时重载）

```bash
# 启动带 live reload 的应用
npm run mobile:livereload
```

这会：
1. 启动 Vite 开发服务器
2. 将应用部署到设备
3. 代码更改时自动刷新

### 生产构建

```bash
# 构建 APK
npm run mobile:build:apk

# 构建 AAB（用于 Google Play）
npm run mobile:build:aab
```

生成的文件位置：
- APK: `android/app/build/outputs/apk/release/app-release.apk`
- AAB: `android/app/build/outputs/bundle/release/app-release.aab`

## 原生功能

### 已集成的插件

| 插件 | 功能 |
|------|------|
| `@capacitor/status-bar` | 状态栏控制 |
| `@capacitor/splash-screen` | 启动屏管理 |
| `@capacitor/app` | 应用生命周期、返回按钮 |
| `@capacitor/keyboard` | 键盘事件 |
| `@capacitor/preferences` | 数据存储 |
| `@capacitor/push-notifications` | 推送通知 |
| `@capacitor/app-launcher` | 应用启动器 |
| `@capacitor/haptics` | 触觉反馈 |

### 使用示例

```typescript
import { isNativePlatform, storage } from '@/utils/capacitor'

// 检查是否在 Android 原生环境
if (isNativePlatform()) {
  // 使用原生功能
}

// 统一存储 API（自动适配 Web/原生）
await storage.set('token', 'xxx')
const token = await storage.get('token')
```

## 调试

### Chrome 远程调试

1. 在 Android 设备上启用 USB 调试
2. Chrome 访问 `chrome://inspect`
3. 找到你的设备并点击 "inspect"

### Android Studio Logcat

1. 打开 Logcat 窗口
2. 过滤 "Capacitor" 标签查看原生日志

## 常见问题

### 构建失败

```bash
# 清理并重新构建
cd android
gradlew.bat clean
cd ..
npm run mobile:sync
```

### 找不到 gradlew

确保在 `android` 目录下执行命令时使用 `gradlew.bat`（Windows）。

### 同步失败

```bash
npx cap sync android
```

## 下一步

1. 在 Android Studio 中配置签名
2. 测试不同 Android 版本的兼容性
3. 优化性能和用户体验
4. 准备发布到应用商店

## 参考

- [Capacitor Android 文档](https://capacitorjs.com/docs/android)
- [Android 开发文档](https://developer.android.com/docs)
