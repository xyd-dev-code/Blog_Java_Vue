import { defineStore } from 'pinia'
import { login } from '@/api/front'
import axios from 'axios'

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
    clearSession() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    },
    async logout() {
      const token = this.token
      this.clearSession()
      if (!token) return
      // Use a separate request so a failed logout never recursively triggers the auth interceptor.
      try {
        await axios.post('/api/v1/auth/logout', null, {
          headers: { Authorization: 'Bearer ' + token }, timeout: 10000
        })
      } catch (error) {
        if (error.response?.status !== 401) {
          const { ElMessage } = await import('element-plus')
          ElMessage.warning('本地会话已清除，服务端退出未确认；如担心令牌泄露，请重新登录并修改密码')
        }
      }
    }
  }
})