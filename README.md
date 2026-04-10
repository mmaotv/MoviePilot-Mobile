# MoviePilot Mobile

MoviePilot 智能影视媒体库管理工具的移动端应用。

## 简介

本项目是基于 [MoviePilot](https://github.com/jxxghp/MoviePilot) 前端项目的移动端适配版本，使用 Capacitor 构建 Android APK，同时支持 PWA 和 Web 访问。

## 特性

- 📱 **移动端原生体验** - 基于 Capacitor 8 构建 Android 应用
- 🌐 **多平台支持** - Android APK / PWA / Web 三端统一
- 🎨 **现代化界面** - Vue 3 + Vuetify 3 构建的响应式 UI
- 🌍 **多语言支持** - 简体中文 / 繁体中文 / 英文
- ⚡ **智能服务器识别** - 自动识别 HTTP/HTTPS 协议，简化服务器配置
- 🔄 **后台任务管理** - 智能处理后台 SSE 连接和定时器
- 💾 **离线状态检测** - 自动检测网络状态并提示

## 技术栈

| 技术 | 版本 |
|------|------|
| Vue | 3.x |
| Vuetify | 3.7.3 |
| TypeScript | 5.x |
| Vite | 5.x |
| Capacitor | 8.x |
| Pinia | 2.x |
| TailwindCSS | 3.x |

## 快速开始

### 环境要求

- Node.js >= v20.12.1
- Yarn 或 npm
- Android Studio（用于构建 APK）
- Java JDK 17+

### 安装依赖

```sh
yarn install
```

### 开发运行

```sh
# Web 开发模式
yarn dev

# 同步到 Android 项目
npx cap sync android

# 打开 Android Studio
npx cap open android
```

### 构建 APK

```sh
# 构建 Web 资源
yarn build

# 同步到 Android
npx cap sync android

# 构建 Release APK
cd android
./gradlew assembleRelease
```

构建完成的 APK 位于：`android/app/build/outputs/apk/release/app-release.apk`

## 移动端适配说明

### 服务器地址输入优化

- 输入框只需填写域名或 IP + 端口（如 `192.168.1.100:3001`）
- 自动识别协议：443/8443 端口使用 HTTPS，其他使用 HTTP
- 协议前缀实时显示在输入框前，无需手动输入

### 移动端特性

- **后台管理**：应用进入后台 5 秒后自动断开 SSE，返回前台自动重连
- **键盘适配**：自动调整布局避免键盘遮挡输入框
- **返回键处理**：Android 返回键智能处理页面导航
- **离线检测**：连续 3 次网络错误触发离线模式提示

## 项目结构

```
MoviePilot-Mobile/
├── android/              # Capacitor Android 项目
├── src/
│   ├── api/             # API 接口和 Axios 配置
│   ├── components/      # Vue 组件
│   │   └── cards/       # 卡片组件
│   ├── composables/     # 组合式函数
│   ├── layouts/         # 布局组件
│   ├── locales/         # i18n 语言文件
│   ├── pages/           # 路由页面
│   ├── stores/          # Pinia 状态管理
│   ├── styles/          # 全局样式
│   ├── utils/           # 工具函数
│   │   ├── capacitor.ts       # Capacitor 桥接
│   │   ├── backgroundManager.ts  # 后台管理
│   │   ├── sseManager.ts    # SSE 连接管理
│   │   └── themeManager.ts  # 主题管理
│   └── views/           # 功能模块视图
├── capacitor.config.json # Capacitor 配置
├── vite.config.ts       # Vite 配置
└── package.json         # 项目依赖
```

## 下载安装

从 [Releases](https://github.com/mmaotv/MoviePilot-Mobile/releases) 页面下载最新版 APK。

### 安装步骤

1. 下载 `MoviePilot-v2.9.27.apk`
2. 在 Android 设备上允许安装未知来源应用
3. 安装 APK
4. 首次打开输入 MoviePilot 服务器地址即可使用

## 相关项目

- [MoviePilot](https://github.com/jxxghp/MoviePilot) - 后端服务
- [MoviePilot-Frontend](https://github.com/jxxghp/MoviePilot-Frontend) - 原前端项目

## 许可证

MIT License
