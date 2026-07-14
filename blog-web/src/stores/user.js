import { defineStore } from 'pinia'
import { login } from '@/api/front'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null
  }),
  persist: true,
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.userInfo?.role === 'ADMIN'
  },
  actions: {
    async doLogin(form) {
      const resp = await login(form)
      // 拦截器返回的是后端 R 对象；真正的 token / user 在 resp.data 里
      const payload = resp.data || {}
      this.token = payload.token || ''
      this.userInfo = payload.user || null
      if (this.token) localStorage.setItem('token', this.token)
      return resp
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})