import { ref, h } from 'vue'
import { useToast } from 'vue-toastification'
import { Workbox } from 'workbox-window'
import i18n from '@/plugins/i18n'
import VersionUpdateToast from '@/components/toast/VersionUpdateToast.vue'

// 全局状态
const currentVersion = ref(__APP_VERSION__)
let isUpdateToastShown = false
let wb: Workbox | null = null

// 标记是否正在自动刷新（防止无限循环）
let isAutoReloading = false

/**
 * 普通刷新页面
 */
export const reloadPage = (): void => {
  window.location.reload()
}

/**
 * 刷新页面并添加时间戳
 */
export const reloadWithTimestamp = (): void => {
  const url = new URL(window.location.href)
  url.searchParams.set('_t', Date.now().toString())
  window.location.replace(url.pathname + url.search + url.hash)
}

/**
 * 清除所有缓存和 Service Worker
 */
export const clearCachesAndServiceWorker = async (): Promise<void> => {
  try {
    // 1. 清除所有缓存
    if ('caches' in window) {
      const cacheNames = await caches.keys()
      await Promise.all(cacheNames.map(name => caches.delete(name)))
      console.log('[VersionChecker] 已清除所有缓存')
    }

    // 2. 注销 Service Worker
    if ('serviceWorker' in navigator) {
      const registrations = await navigator.serviceWorker.getRegistrations()
      await Promise.all(registrations.map(registration => registration.unregister()))
      console.log('[VersionChecker] 已注销所有 Service Worker')
    }
  } catch (error) {
    console.error('[VersionChecker] 清除缓存失败:', error)
  }
}

/**
 * 清除缓存并刷新
 */
const clearCacheAndReload = async (): Promise<void> => {
  await clearCachesAndServiceWorker()
  reloadWithTimestamp()
}

/**
 * 版本检查 Composable
 */
export function useVersionChecker() {
  const toast = useToast()

  /**
   * 显示版本更新通知
   */
  const showUpdateNotification = (message: string, refreshText?: string, onRefresh?: () => void): void => {
    if (isUpdateToastShown) return
    isUpdateToastShown = true
    const component = h(VersionUpdateToast, {
      message,
      refreshText,
      onRefresh,
    })

    toast.info(component, {
      timeout: false,
      closeButton: false,
      closeOnClick: false,
      draggable: false,
    })
  }

  // 初始化 Workbox
  if (!wb && 'serviceWorker' in navigator) {
    wb = new Workbox('/service-worker.js')

    // Service Worker 激活事件 (install -> activate)
    wb.addEventListener('activated', event => {
      if (event.isUpdate) {
        console.log('[VersionChecker] Service Worker 更新已激活，自动刷新页面')
        reloadPage()
      }
    })

    // 注册 Service Worker
    wb.register()
  }

  /**
   * 检查版本并在需要时自动处理
   */
  const checkVersion = async (latestVersion: string): Promise<void> => {
    // 如果已经在自动刷新流程中，跳过
    if (isAutoReloading) return

    // 版本一致，无需操作
    if (latestVersion === currentVersion.value) {
      console.log('[VersionChecker] 版本号一致，无需操作')
      return
    }

    console.log(`[VersionChecker] 检测到版本不一致: ${currentVersion.value} -> ${latestVersion}`)

    // 检查 URL 中是否有 _t 参数（说明是自动刷新回来的页面）
    const urlParams = new URLSearchParams(window.location.search)
    const hasTimestamp = urlParams.has('_t')

    // 如果是刷新回来的页面，不再次触发刷新，避免无限循环
    if (hasTimestamp) {
      console.log('[VersionChecker] 刷新回来的页面，跳过版本检查')
      return
    }

    // 尝试触发 Service Worker 更新检查
    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
      try {
        const registration = await navigator.serviceWorker.getRegistration()
        if (registration) {
          console.log('[VersionChecker] 触发 Service Worker 更新检查...')

          let updateFound = false
          const onUpdateFound = () => {
            updateFound = true
          }

          registration.addEventListener('updatefound', onUpdateFound, { once: true })
          await registration.update()

          // 如果发现更新，交由 SW activated 事件处理
          if (updateFound || registration.installing || registration.waiting) {
            console.log('[VersionChecker] Service Worker 更新中...')
            return
          }

          console.log('[VersionChecker] SW 无更新，但版本号不一致，可能是缓存问题')
        }
      } catch (error) {
        console.log('[VersionChecker] Service Worker 更新检查失败:', error)
      }
    } else {
      console.log('[VersionChecker] 无 Service Worker')
    }

    // 最终兜底：标记并自动清除缓存刷新
    isAutoReloading = true
    console.log('[VersionChecker] 版本不一致，自动清除缓存并刷新')
    await clearCacheAndReload()
  }

  return {
    checkVersion,
  }
}
