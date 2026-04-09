# MoviePilot-Mobile 项目记忆

## 项目基础信息
- **项目名**：MoviePilot-Mobile
- **版本**：v2.9.27
- **定位**：MoviePilot 智能影视媒体库管理工具的移动端前端
- **主分支**：v2
- **包管理器**：yarn 1.22.18

## 技术栈
- **框架**：Vue 3 (Composition API) + TypeScript
- **UI 库**：Vuetify 3.7.3
- **状态管理**：Pinia + pinia-plugin-persistedstate（持久化到 localStorage）
- **路由**：Vue Router 4（Hash 模式）
- **构建**：Vite 5 + terser 压缩（生产移除 console）
- **样式**：SCSS + TailwindCSS 3
- **跨平台**：Capacitor 8（Android APK/AAB + PWA）
- **HTTP**：Axios（含请求/响应拦截器，自动处理 token 和离线状态）
- **实时**：SSEManager 单例（后台自动断开重连）
- **国际化**：vue-i18n，支持 zh-CN / zh-TW / en-US

## 目录结构关键点
- `src/pages/` - 24 个路由页面（.vue 文件）
- `src/views/` - 11 个功能模块子视图（dashboard/discover/plugin/setting 等）
- `src/components/cards/` - 28 个业务卡片组件
- `src/composables/` - 16 个组合式函数
- `src/utils/` - 13 个工具函数（capacitor/themeManager/sseManager/backgroundManager 等）
- `src/stores/` - 3 个 Pinia Store（auth/user/globalSettings）
- `src/api/` - Axios 实例 + types.ts（完整类型定义）+ constants.ts
- `src/layouts/components/` - 布局组件（Footer/HeaderTab/ShortcutBar/UserProfile 等）

## 路径别名
- `@` → `src/`
- `@core` → `src/@core/`
- `@layouts` → `src/@layouts/`
- `@images` → `src/assets/images/`
- `@styles` → `src/styles/`

## 移动端特性
- Capacitor 双模式适配（原生/Web 懒加载判断）
- BackgroundManager：后台自动暂停所有定时器
- SSEManager：后台 5 秒后关闭 SSE，前台恢复重连
- 离线状态：连续 3 次网络错误才触发离线模式
- 键盘弹出：自动注入 --keyboard-height CSS 变量
- Android 返回键：无历史则退出 App

## 已有修改文件（截至 2026-04-09）
git 状态下 modified 的文件包括：
- src/api/index.ts（已自定义服务器 URL 恢复逻辑）
- src/pages/login.vue（登录页有自定义改动）
- src/stores/global.ts
- src/utils/loadingStateManager.ts
- src/composables/useVersionChecker.ts
- src/main.ts, vite.config.ts, package.json 等
