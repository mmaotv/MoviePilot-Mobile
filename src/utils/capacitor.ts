/**
 * Capacitor 初始化工具
 * 用于在移动应用中初始化原生功能
 * 注意：此文件仅在 Capacitor 环境中使用，Web 构建时会被排除
 */

// 动态导入 Capacitor 模块，避免 Web 构建时报错
let Capacitor: any = null
let StatusBar: any = null
let SplashScreen: any = null
let App: any = null
let Keyboard: any = null
let Preferences: any = null
let Style: any = null

// 懒加载 Capacitor 模块
const loadCapacitorModules = async () => {
  if (Capacitor) return

  try {
    const core = await import('@capacitor/core')
    const statusBar = await import('@capacitor/status-bar')
    const splashScreen = await import('@capacitor/splash-screen')
    const app = await import('@capacitor/app')
    const keyboard = await import('@capacitor/keyboard')
    const preferences = await import('@capacitor/preferences')

    Capacitor = core.Capacitor
    StatusBar = statusBar.StatusBar
    SplashScreen = splashScreen.SplashScreen
    App = app.App
    Keyboard = keyboard.Keyboard
    Preferences = preferences.Preferences
    Style = statusBar.Style
  } catch (e) {
    console.warn('Capacitor modules not available (running in web mode)')
  }
}

// 检查是否在 Capacitor 环境中
export const isNativePlatform = (): boolean => {
  return Capacitor?.isNativePlatform() ?? false
}

// 获取平台信息
export const getPlatform = (): 'ios' | 'android' | 'web' => {
  return Capacitor?.getPlatform() ?? 'web'
}

// 初始化状态栏
const initStatusBar = async () => {
  if (!isNativePlatform() || !StatusBar) return

  try {
    await StatusBar.setStyle({ style: Style.Dark })
    await StatusBar.setBackgroundColor({ color: '#0E1116' })
    await StatusBar.setOverlaysWebView({ overlay: false })
  } catch (error) {
    console.error('Failed to initialize status bar:', error)
  }
}

// 初始化键盘行为
const initKeyboard = async () => {
  if (!isNativePlatform() || !Keyboard) return

  try {
    Keyboard.addListener('keyboardWillShow', (info: any) => {
      document.body.style.setProperty('--keyboard-height', `${info.keyboardHeight}px`)
    })

    Keyboard.addListener('keyboardWillHide', () => {
      document.body.style.removeProperty('--keyboard-height')
    })
  } catch (error) {
    console.error('Failed to initialize keyboard:', error)
  }
}

// 初始化应用生命周期管理
const initAppLifecycle = () => {
  if (!isNativePlatform() || !App) return

  App.addListener('appStateChange', ({ isActive }: any) => {
    console.log('App state changed:', isActive ? 'active' : 'inactive')

    if (isActive) {
      window.dispatchEvent(new CustomEvent('app-resumed'))
    } else {
      window.dispatchEvent(new CustomEvent('app-paused'))
    }
  })

  App.addListener('backButton', ({ canGoBack }: any) => {
    if (!canGoBack) {
      App.exitApp()
    } else {
      window.history.back()
    }
  })
}

// 隐藏启动屏
const hideSplashScreen = async () => {
  if (!isNativePlatform() || !SplashScreen) return

  try {
    setTimeout(async () => {
      await SplashScreen.hide()
    }, 1500)
  } catch (error) {
    console.error('Failed to hide splash screen:', error)
  }
}

// 首选项存储封装
export const storage = {
  async set(key: string, value: string): Promise<void> {
    await loadCapacitorModules()
    if (isNativePlatform() && Preferences) {
      await Preferences.set({ key, value })
    } else {
      localStorage.setItem(key, value)
    }
  },

  async get(key: string): Promise<string | null> {
    await loadCapacitorModules()
    if (isNativePlatform() && Preferences) {
      const result = await Preferences.get({ key })
      return result.value
    } else {
      return localStorage.getItem(key)
    }
  },

  async remove(key: string): Promise<void> {
    await loadCapacitorModules()
    if (isNativePlatform() && Preferences) {
      await Preferences.remove({ key })
    } else {
      localStorage.removeItem(key)
    }
  },

  async clear(): Promise<void> {
    await loadCapacitorModules()
    if (isNativePlatform() && Preferences) {
      await Preferences.clear()
    } else {
      localStorage.clear()
    }
  },
}

// 初始化所有 Capacitor 功能
export const initCapacitor = async () => {
  await loadCapacitorModules()

  if (!isNativePlatform()) {
    console.log('Running in web mode, skipping Capacitor initialization')
    return
  }

  console.log(`Initializing Capacitor on ${getPlatform()} platform`)

  try {
    await Promise.all([
      initStatusBar(),
      initKeyboard(),
    ])

    initAppLifecycle()
    hideSplashScreen()

    console.log('Capacitor initialized successfully')
  } catch (error) {
    console.error('Error during Capacitor initialization:', error)
  }
}
