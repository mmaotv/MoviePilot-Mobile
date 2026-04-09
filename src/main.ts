// 0. Capacitor 初始化（必须在最前面）
import { initCapacitor } from './utils/capacitor'

// 1. 配置与兼容性
import './ace-config'
import '@/@core/utils/compatibility'
import '@/@iconify/icons-bundle'
import '@/plugins/webfontloader'

// 2. 核心插件和 UI 框架
import { createApp } from 'vue'
import vuetify from '@/plugins/vuetify'
import router from '@/router'
import pinia from '@/stores/index'
import i18n from '@/plugins/i18n'

// 3. 全局组件
import App from '@/App.vue'
import { VAceEditor } from 'vue3-ace-editor'
import { PerfectScrollbarPlugin } from 'vue3-perfect-scrollbar'
import { CronVuetify } from '@vue-js-cron/vuetify'

// 4. 工具函数和其他辅助模块
import { loadRemoteComponents } from './utils/federationLoader'

// 5. 其他插件和功能模块
import Toast from 'vue-toastification'
import ConfirmDialog from '@/composables/useConfirm'
import VueApexCharts from 'vue3-apexcharts'

// 6. 注册自定义组件
import DialogCloseBtn from '@/@core/components/DialogCloseBtn.vue'
import ScrollToTopBtn from '@/@core/components/ScrollToTopBtn.vue'
import PageContentTitle from './@core/components/PageContentTitle.vue'
import MediaCard from './components/cards/MediaCard.vue'
import PosterCard from './components/cards/PosterCard.vue'
import BackdropCard from './components/cards/BackdropCard.vue'
import PersonCard from './components/cards/PersonCard.vue'
import MediaInfoCard from './components/cards/MediaInfoCard.vue'
import TorrentCard from './components/cards/TorrentCard.vue'
import MediaIdSelector from './components/misc/MediaIdSelector.vue'
import CronField from './components/field/CronField.vue'
import PathField from './components/field/PathField.vue'
import HeaderTab from './layouts/components/HeaderTab.vue'

// 7. 样式文件 - 合并为单一导入
import '@/styles/main.scss'

// 8. 状态恢复插件
import stateRestorePlugin from '@/plugins/stateRestore'

// 9. 后台优化工具
import { backgroundManager } from '@/utils/backgroundManager'
import { sseManagerSingleton } from '@/utils/sseManager'

// 创建Vue实例
const app = createApp(App)

// 初始化 Capacitor（非阻塞）
initCapacitor().catch(error => {
  console.error('Failed to initialize Capacitor:', error)
})

// 1. 注册pinia
app.use(pinia)

// 异步加载远程组件（不阻塞启动）
loadRemoteComponents().catch(error => {
  console.error('Failed to load remote components', error)
})

// 2. 注册 UI 框架
app.use(vuetify)

// 3. 注册路由
app.use(router)

// 4. 注册状态恢复插件
app.use(stateRestorePlugin)

// 5. 注册全局组件
app
  .component('VAceEditor', VAceEditor)
  .component('VApexChart', VueApexCharts)
  .component('VCronVuetify', CronVuetify)
  .component('VDialogCloseBtn', DialogCloseBtn)
  .component('VScrollToTopBtn', ScrollToTopBtn)
  .component('VMediaCard', MediaCard)
  .component('VPosterCard', PosterCard)
  .component('VBackdropCard', BackdropCard)
  .component('VPersonCard', PersonCard)
  .component('VMediaInfoCard', MediaInfoCard)
  .component('VTorrentCard', TorrentCard)
  .component('VMediaIdSelector', MediaIdSelector)
  .component('VCronField', CronField)
  .component('VPathField', PathField)
  .component('VHeaderTab', HeaderTab)
  .component('VPageContentTitle', PageContentTitle)

// 6. 注册其他插件
app
  .use(PerfectScrollbarPlugin)
  .use(Toast, {
    position: 'bottom-right',
    hideProgressBar: true,
  })
  .use(ConfirmDialog)
  .use(i18n)
  .mount('#app')

// 页面卸载时清理后台管理器
window.addEventListener('beforeunload', () => {
  backgroundManager.destroy()
  sseManagerSingleton.closeAllManagers()
})
