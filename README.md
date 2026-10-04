# MoviePilot Android

一个基于 Kotlin + Jetpack Compose 开发的 MoviePilot 安卓客户端应用。

## 功能特性

- 用户登录/登出（支持自定义服务器地址）
- 仪表盘（存储空间、媒体统计、系统状态）
- 资源搜索和浏览
- 订阅管理（查看、删除）
- 下载任务管理
- 传输历史记录
- 媒体详情（原生实现）
- Bangumi 日历
- 插件管理、应用中心、站点管理、系统设置

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp
- **图片加载**: Coil
- **导航**: Navigation Compose
- **异步**: Coroutines + Flow

## 项目结构

```
app/src/main/java/com/moviepilot/app/
├── data/
│   ├── model/           # 数据模型
│   ├── network/         # API客户端
│   ├── repository/      # 数据仓库
│   └── local/           # 本地存储
├── di/                  # 依赖注入模块
├── ui/
│   ├── screens/         # UI屏幕组件
│   ├── theme/           # 主题配置
│   └── viewmodel/       # ViewModel
├── MainActivity.kt      # 主Activity
└── MoviePilotApp.kt     # Application类
```

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK API 35
- Gradle 8.9

## 构建步骤

### 1. 配置 Android SDK 路径

复制 `local.properties.template` 为 `local.properties`，并修改为你的实际 SDK 路径：

```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

### 2. 使用 Android Studio 构建

1. 用 Android Studio 打开项目文件夹
2. 等待 Gradle 同步完成
3. 点击 "Run" 按钮或使用快捷键 `Shift+F10`

### 3. 使用命令行构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要配置签名）
./gradlew assembleRelease
```

构建成功后，APK 文件位于：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 首次使用

1. **安装 APK** 到你的 Android 设备
2. **启动应用**，在登录界面输入你的 MoviePilot 服务器地址
3. **输入账号密码**，点击登录
4. 服务器地址会自动保存，下次启动无需重新输入

## 注意事项

1. **网络权限**: 应用需要网络访问权限，已在 AndroidManifest.xml 中声明
2. **HTTP明文传输**: 当前服务器使用 HTTP，已在 manifest 中启用 `usesCleartextTraffic="true"`
3. **最低Android版本**: Android 8.0 (API 26)

## 许可证

本项目基于 MIT 许可证开源。
