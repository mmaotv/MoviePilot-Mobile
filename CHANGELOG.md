# MoviePilot Android - 更新日志

## v3.0.1 (2026-10-25) - 最新

修复部分悬浮菜单点开后**看不到内容**的问题。

#### 现象

- 订阅页「电视剧订阅」，点任意订阅卡片上的 `…` 菜单，屏幕上看不到任何反应；
- 搜索结果页点「排序」，排序下拉同样不显示；
- 站点管理页点站点卡片的菜单，也是空的。

而「站点 / 编码 / 制作组 / 促销状态」这些筛选菜单一直正常 —— 分界线非常明确。

#### 根因

Vuetify 弹层有两种定位方式，两种都被我们之前注入的一条样式命中：

```css
.v-overlay__content { max-height: 100% !important; }
```

| 弹层定位 | 父容器高度 | `max-height: 100%` | 实际效果 |
|---|---|---|---|
| `position: fixed`（铺满视口） | 778px | 778px | 正常 —— 站点 / 编码 / 制作组筛选 |
| `position: absolute`（吸附页面内） | **0** | **0px** | 内容被裁成 0 高 —— 订阅卡片菜单 / 排序 / 站点管理菜单 |

Vuetify 自己写在行内的 `max-height: 754px` 本来是正确的，被这条 `!important` 覆盖后才出问题。
菜单其实**已经弹出、DOM 里文字都在**，只是高度被压成 0，所以屏幕上完全看不到。

#### 修复

- 不再写死百分比，改为由 JS 实测 `window.innerHeight - 16` 写入 CSS 变量 `--mp-overlay-max`，CSS 只读变量；
- 测不到值时退回 `none`，**不可能再产生 0 高裁剪**。

> 附带发现：这个 WebView 里 `vh` / `dvh` / `svh` 全部解析成 0（实测 `100vh == 0px`，而 `innerHeight` 是 778），
> 所以上限也不能改用视口单位，必须用 JS 算出的像素值。

#### 验证（真机 Android 14 / WebView 137）

| 场景 | 修复前 | 修复后 |
|---|---|---|
| 订阅卡片 `…` 菜单 | h = 0 | h = 402 ✔ |
| 搜索「排序」下拉 | h = 0 | h = 307 ✔ |
| 站点管理卡片菜单 | h = 0 | h = 114 ✔ |
| 站点 / 编码 / 制作组筛选 | 正常 | 仍正常 ✔ |

<p align="center">
  <img src="https://raw.githubusercontent.com/mmaotv/MoviePilot-Mobile/master/docs/screenshots/fix-sub-menu.jpg" width="240" alt="订阅卡片菜单">
  <img src="https://raw.githubusercontent.com/mmaotv/MoviePilot-Mobile/master/docs/screenshots/fix-sort-menu.jpg" width="240" alt="排序菜单">
  <img src="https://raw.githubusercontent.com/mmaotv/MoviePilot-Mobile/master/docs/screenshots/fix-site-menu.jpg" width="240" alt="站点管理菜单">
</p>

> 升级方式：直接覆盖安装即可，账号与已保存的服务器配置不受影响。

---

## v3.0.0 (2026-10-19)

全面转向 PWA 外壳：原生只保留「登录 / 切换服务器 / 切换账号」，其余功能全部由网页版承担。

### 移除原生业务层（约 1.2 万行）

| 类别 | 删除数量 |
|---|---|
| 侧边栏入口（仪表盘/推荐/发现/资源/订阅/日历/下载/整理/文件/插件/应用/站点/用户/设置） | 15 项 |
| 原生页面 | 14 个 |
| ViewModel | 15 个 |
| Repository | 9 个 |
| 数据模型 | 17 个 |
| ApiService 接口 | 1,036 行 → 2 个接口 |

不再维护「网页有什么页面」的映射表，从根上消除了侧边栏入口与实际网页入口不一致的问题。

### 界面

- 登录后直接全屏显示服务器网页版，去掉 48dp 原生顶栏与侧边栏
- 移除全部转场动画：登录页入场/呼吸/展开、按钮切换动画、抽屉动画、页面切换动画
- 唯一原生控件：可拖动悬浮球 → 底部面板（服务器 / 账号 / 退出登录）
- 深色配色固定为网页版的 `#0E1116` + `#8D51F9`，状态栏/导航栏透明

### 多账号

- 服务器与账号拆成两份数据：同一服务器可保存多个账号
- 账号以「服务器地址 + 用户名」为唯一键，不再互相覆盖
- 首次启动自动迁移 v2 的 `server_profiles`（含加密密码）
- 切换服务器 / 切换账号 / 退出登录统一回到登录页，并自动预填

### 服务器地址：完全由用户输入

- **去掉一切内置/预置服务器地址**（不再首次安装就塞一个固定 IP），列表只作为登录过的历史
- 悬浮球面板顶部常驻地址输入框 + 「连接」，输入任意地址即可切过去，不限于已保存列表
- 地址解析支持：`IP:端口`、域名、显式 `http://` / `https://`、反向代理子路径（`domain/moviepilot`）、从浏览器直接粘贴带 `#/` 或 `?` 的整条地址
- 没写协议时自动尝试 http / https：内网地址（IP、localhost、单段主机名、`.local`/`.lan`）优先 http，公网域名优先 https
- 界面展示保留协议，能看出当前连的是 http 还是 https
- 自签名 HTTPS 证书不再白屏，弹窗确认后可继续访问
- 本地图标加速只作用于当前服务器域名，不会把别的服务器的图标换成本地的

### 浅色 / 深色主题

- 原生界面（登录页、切换面板）跟随系统深浅色自动切换，无需重启
- 配色直接对齐网页版 V3：浅色 `#F4F5FA` 底 / `#3A3541` 文字 / `#8D51F9` 主色 / `#FFFFFF` 卡片；深色 `#0E1116` / `#E6EDF3`
- 状态栏、导航栏图标颜色跟随主题；进入网页版后改为读取网页顶部的实际背景色再校正（见「网页承载」），深色网页不再配浅色图标
- 启动页背景与状态栏图标按系统深浅色分别适配（新增 `values-night`）

### 登录页与图标

- 登录页重做：渐变背景 + 顶部柔光、白色圆角卡片 + 轻阴影、输入框聚焦时图标与描边变主色、主按钮渐变
- Logo 换成 V3 的透明底品牌图（`logo.png`），不再用深色方图裁圆角
- 启动图标改为「浅色底 #F4F5FA + 紫色 M」的自适应图标，与 V3 浅色主题一致；同时提供 Android 13 单色层

### 网页承载

- WebView 的 UA 改为 iOS Safari（`iPhone; CPU iPhone OS 18_5`），拿到与设计稿一致的移动端布局
- **登录态还原改成写两个 Pinia store**：`auth`（token）+ `user`（`superUser` / `userID` / `permissions` 等）。
  只写 `auth` 时网页版根路由读到 `superUser=false`，会把超管也重定向到「应用中心 `#/apps`」，
  表现就是「只有订阅等少数入口、没有真正的主页」。
- **注入后整体重载到站点根 `#/`**（带一次性 `?__mp_r=1` 参数触发真正的文档重载），
  让 Pinia 重新 hydrate、由网页版自己把 `/` 解析成 `/dashboard`；
  以前是对当前 hash（`#/login`）做 `location.reload()`，所以一直停在登录页或应用中心。
- 兜底：万一仍落在 `#/apps` 而当前账号是超管，自动纠正到 `#/dashboard` 一次（不影响之后手动进应用中心）
- 布局永远避让系统栏（`WindowInsets.systemBars`），**不再有内容被状态栏压住的情况**；
  预留出来的状态栏/导航栏区域用网页顶栏与页面底色填充，看起来仍是通栏
- 状态栏/导航栏图标明暗改为**读取网页顶部实际背景色**后取反（每 600ms 检测一次，跟随网页切主题、切页面），
  不再只看 `<html>` 的 `color-scheme`，深色网页不会再配白色图标
- 登录页自己按 App 主题设置状态栏图标，退出网页版回到登录页时不会残留网页那边的设置
- 键盘弹出时网页内容会跟随上移（`imePadding`），输入框不会被输入法盖住
- 网页颜色通过 `console.log('MP_THEME:…')` 单向回报，不注入 `addJavascriptInterface`（服务器地址可由用户任意填写，避免 JS 桥成为攻击面）

### 图标

- 从 V3 服务器（`192.168.10.130:3000`）取最新 `pwa-1024x1024.png`
- 改为标准 adaptive-icon（前景 + 背景 + Android 13 单色层），修复桌面白底方块
- 启动页改用透明 Logo，去掉已废弃的 branding image
- 本地加速资源从 3.0MB 精简到 376KB（只保留根目录图标，删掉 48 张旧前端图）

### 兼容性

| 项 | 值 |
|---|---|
| 服务器端 | 任意 MoviePilot 部署（V2 / V3 均可），地址由用户输入 |
| 最低系统 | Android 8.0 (API 26) |
| 目标 API | 35 |

---

## v2.2.2 (2026-10-04)

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
