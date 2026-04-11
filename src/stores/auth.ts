import { defineStore } from 'pinia'
import type { authState } from '@/stores/types'

export const useAuthStore = defineStore('auth', {
  state: (): authState => ({
    token: null,
    remember: false,
    originalPath: null,
    isServerChecked: false,
  }),

  // 全局持久化
  persist: true,

  actions: {
    setToken(token: string | null) {
      this.token = token
    },
    clearToken() {
      this.token = null
    },
    setRemember(remember: boolean) {
      this.remember = remember
    },
    setOriginalPath(originalPath: string | null) {
      this.originalPath = originalPath
    },
    login(payload: authState) {
      this.setToken(payload.token)
      this.setRemember(payload.remember)
    },
    logout() {
      this.clearToken()
      this.setOriginalPath(null)
      this.isServerChecked = false
    },
  },

  getters: {
    getToken: state => state.token,
    getRemember: state => state.remember,
    getOriginalPath: state => state.originalPath,
  },
})
