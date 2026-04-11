import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores'
import { initializeRequestOptimizer } from '@/utils/requestOptimizer'
import { useGlobalOfflineStatus } from '@/composables/useOfflineStatus'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

// 初始化时恢复上次使用的服务器地址
function restoreServerUrl() {
  try {
    const saved = localStorage.getItem('moviepilot_servers')
    if (saved) {
      const servers = JSON.parse(saved)
      if (servers && servers.length > 0) {
        let serverUrl = servers[servers.length - 1]
        // 规范化 URL
        serverUrl = serverUrl.replace(/\/$/, '')
        if (!serverUrl.startsWith('http://') && !serverUrl.startsWith('https://')) {
          serverUrl = 'http://' + serverUrl
        }
        if (!serverUrl.endsWith('/api/v1')) {
          serverUrl = serverUrl + '/api/v1'
        }
        api.defaults.baseURL = serverUrl
        console.log('[API] Restored server URL:', serverUrl)
      }
    }
  } catch (e) {
    console.warn('[API] Failed to restore server URL:', e)
  }
}

// 执行恢复
restoreServerUrl()

// 声明全局变量类型
declare global {
  interface Window {
    MoviePilotAPI: typeof api
  }
}

// 将 API 实例暴露到全局，供插件使用
window.MoviePilotAPI = api

// 初始化请求优化器（必须在其他拦截器之前）
initializeRequestOptimizer(api)

// 添加请求拦截器
api.interceptors.request.use(config => {
  // 认证 Store
  const authStore = useAuthStore()
  // 在请求头中添加token
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

// 离线状态管理
const globalOfflineStatus = useGlobalOfflineStatus()

// 添加响应拦截器
api.interceptors.response.use(
  response => {
    // 成功响应时，清除应用离线状态并重置连续错误计数
    globalOfflineStatus.setAppOffline(false)
    globalOfflineStatus.resetConsecutiveErrors()
    return response.data
  },
  error => {
    if (!error.response) {
      // 网络错误或请求超时 - 通知离线状态管理系统
      const isNetworkError =
        error.code === 'NETWORK_ERROR' ||
        error.code === 'ERR_NETWORK' ||
        error.code === 'ECONNABORTED' ||
        error.name === 'NetworkError'

      if (isNetworkError) {
        let reason = 'Network connection failed'
        if (error.code === 'ECONNABORTED') {
          reason = 'Request timeout'
        }
        // 记录网络错误，只有连续三次才会设置为离线模式
        globalOfflineStatus.recordNetworkError(reason)

        // 检查是否是服务器连接问题（首次加载时）
        const authStore = useAuthStore()
        if (authStore.token && !authStore.isServerChecked) {
          // 标记服务器检查失败，避免无限循环
          authStore.isServerChecked = true
          // 清除登录状态
          authStore.logout()
          // 跳转到登录页并提示用户
          router.push('/login')
          return Promise.reject(new Error('无法连接到服务器，请检查服务器地址'))
        }
      }

      if (error.code === 'NETWORK_ERROR' || error.code === 'ERR_NETWORK') {
        // 网络连接问题
        return Promise.reject(new Error('Network connection failed, please check your network status'))
      } else if (error.code === 'ECONNABORTED') {
        // 请求超时
        return Promise.reject(new Error('Request timeout, please try again later'))
      } else if (error.name === 'AbortError') {
        // 请求被中止（路由切换等）
        return Promise.reject(new Error('Request cancelled'))
      }
      // 其他网络错误
      return Promise.reject(new Error(error.message || 'Network error'))
    } else if (error.response.status === 403) {
      // 认证 Store
      const authStore = useAuthStore()
      // 清除登录状态信息
      authStore.logout()
      // token验证失败，跳转到登录页面
      router.push('/login')
    }

    return Promise.reject(error)
  },
)

export default api
