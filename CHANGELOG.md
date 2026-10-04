# MoviePilot Android - 更新日志

## v2.2.2 (2026-10-04) - 最新

服务器端 v3.1.0 全面适配。主体界面回归服务器 PWA，侧边栏为薄入口层。

### 修复：PWA 页面在 App 内全部 401（关键修复）

**现象**：浏览器中「下载管理 / 媒体整理 / 文件管理」都正常，App 内点击却是空白或
提示「服务器连接失败」。

**根因**：服务器 PWA 用 Pinia store `auth` 持久化登录 token（localStorage 键 `auth`），
业务请求经 axios 拦截器带 `Authorization: Bearer <token>`。App 自己的登录走
`POST /api/v2/login/access-token`，token 只存在 App 侧，**WebView 内的 PWA 拿不到**，
于是所有页面接口返回 401。

实测证据（向 WebView 注入 fetch 探针）：
```
AUTH_LEN=240
PROBE /api/v1/history/transfer -> 200 len=57919
PROBE /api/v1/download/         -> 200 len=2
```

**修复**：App 登录后把 token 直接写入 PWA 的 localStorage，并触发一次 reload
让 store 重新 hydrate。同时**删除**原先模拟填写登录表单的 `attemptAutoLogin`
（无法建立会话，且脆弱）。

### 修复：资源搜索筛选只有灰色遮罩

弹层 `.v-overlay__content` 在 WebView 中被计算为零尺寸（实测 `h=0 w=0`），
遮罩层正常但内容不可见。PWA 的 `<meta viewport>` 锁定了缩放
（`maximum-scale=1.0, user-scalable=no`），与 WebView 缩放设置冲突。

注入最小约束 CSS 放开弹层内容高度后，筛选面板完整显示
（季 / 站点 / 视频编码 / 质量 / 分辨率 / 制作组等维度均可展开选择）。

### 修复：入口名称与实际页面不符

| 路由 | 修正前 | 修正后 |
|---|---|---|
| `/#/downloading` | 正在下载 | **下载管理** |
| `/#/history` | 历史记录 | **媒体整理** |
| `/#/filemanager` | 文件管理 | 文件管理（不变） |

### 修复：登录后闪退

`PreferencesViewModel` 标注 `@Singleton`，原代码用
`hiltViewModel<PreferencesViewModel>()` 获取会尝试无参构造，抛
`NoSuchMethodException`，登录成功进入主界面必崩。
改为由 `MainActivity` 用 `@Inject` 注入后透传。

### 修复：下载与搜索接口适配 v3.1.0

- `POST /api/v1/download/` 请求体字段为 **`torrent_in` + `media_in`**（均必填），
  非旧版的 `torrent_info` / `media_info`
- 弃用 `Map<String, Any>` 作为 `@Body`（Retrofit 会生成带通配符的参数类型），
  改用强类型 data class
- `/api/v1/search/last` 直接返回**数组**（实测 752 条），非 `{success,data}` 包装

### 架构调整：主体回归 PWA

- 撤销此前的原生资源搜索页实现，**主体界面一律由服务器 PWA 渲染**
- 侧边栏仅作入口映射，服务器前端升级时无需重新适配
- 入口补齐至服务器 v3.1.0 实际路由（新增 推荐 / 发现 / 用户管理，共 15 项）
- `loadPath` 改为整页加载：PWA 的 axios baseURL 是相对路径 `api/v1/`，
  只改 hash 会导致部分页面基址解析异常

### 视觉统一

启动页、登录页、侧边栏、WebView 区域配色统一为服务器 PWA 主题
（背景 `#0E1116`、主色 `#8D51F9`），消除原先浅色登录页与深色 PWA 的割裂感。

### 兼容性

| 项 | 值 |
|---|---|
| 服务器端 | MoviePilot **v3.1.0**+ |
| 最低系统 | Android 8.0 (API 26) |
| 目标 API | 35 |

---

## v2.1 (2026-10-04)

## v1.2 (2026-04-11)

### 修复问题

#### 1. 登录持久化修复
- 修复 App 重启后登录状态丢失的问题
- 使用 DataStore 持久化存储登录凭证
- `AuthViewModel` 启动时自动恢复登录状态

#### 2. WebView 自动登录
- 实现 JavaScript 轮询方案自动填充登录表单
- 支持多种登录表单选择器适配
- 最多等待 8 秒让 Vue SPA 初始化完成

#### 3. UI 优化
- 修复 WebView 加载时图标居中显示
- 移除不必要的加载遮罩

### 新增功能

- 登录页密码可见/隐藏切换按钮
- 支持侧边栏滑动手势关闭

---

## v1.1 (2026-04-08)

### 新增功能

#### 用户自定义服务器地址
- 登录界面添加服务器地址输入框
- 支持保存和记忆服务器地址
- 自动添加 `http://` 前缀
- 实时显示当前连接的服务器地址

### 技术改进

- 新增 `PreferencesManager` 管理应用配置
- 修改 `ApiClient` 支持动态设置 baseUrl
- 修复 Hilt Context 注入问题

---

## v1.0 (2026-04-08) - 初始版本

### 核心功能
- 用户登录/登出
- 仪表盘（存储空间、媒体统计、系统状态）
- 资源搜索和浏览
- 订阅管理
- 下载任务管理
- 传输历史记录

### 技术栈
- Kotlin + Jetpack Compose
- Hilt 依赖注入
- Retrofit + OkHttp 网络请求
- Coil 图片加载
- MVVM 架构

### 基础特性
- Material Design 3 深色主题
- 侧边栏导航
- 响应式布局

---

## 使用说明

### 首次使用

1. 安装 APK 到 Android 设备
2. 打开应用，输入服务器地址（例如: `http://192.168.1.100:5000`）
3. 输入用户名和密码
4. 点击登录按钮

### 修改服务器地址

1. 退出当前账号
2. 在登录界面修改服务器地址
3. 点击设置图标保存
4. 重新登录

---

**更新日期**: 2026-10-04
