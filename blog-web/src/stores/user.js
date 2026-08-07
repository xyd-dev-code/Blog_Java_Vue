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
      // 拦截器 return data 后,await 拿到的是 R 对象本身:{code, message, data:{token, user, tokenInfo}}
      const payload = (resp && resp.data) || {}
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