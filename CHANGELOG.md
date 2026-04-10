# 更新日志

所有 notable 更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [2.9.27] - 2026-04-10

### 新增

- 移动端原生应用支持（Android APK）
- 自动识别 HTTP/HTTPS 协议，简化服务器配置
- 多语言支持（简体中文、繁体中文、英文）
- 后台任务智能管理（SSE 自动重连、定时器暂停）
- 离线状态检测和提示

### 修改

- 登录页服务器地址输入框优化，只需输入域名/IP+端口
- i18n 翻译完善，输入框标题支持多语言切换
- 键盘弹出时自动调整布局

### 技术

- 基于 Capacitor 8 构建 Android 应用
- Vue 3 + Vuetify 3 + TypeScript 技术栈
- 添加 GitHub Actions 自动构建工作流

## [2.9.0] - 2024-XX-XX

### 基础版本

- 基于 MoviePilot-Frontend v2 分支
- 完整的影视媒体库管理功能
- 插件系统支持
