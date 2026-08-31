import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

http.interceptors.response.use(
  async (resp) => {
    const data = resp.data
    const requestedBlob = resp.config?.responseType === 'blob'
    const silent = !!resp.config?.silent

    // 二进制下载类请求：后端失败时会以 JSON 形式返回错误体，
    // axios 仍然按 blob 解析，这里 peek 一下类型还原成 R 并走统一 toast 路径。
    if (requestedBlob && data instanceof Blob && !data.type.startsWith('application/')) {
      return data
    }
    if (requestedBlob && data instanceof Blob && data.type.includes('json')) {
      try {
        const text = await data.text()
        const payload = JSON.parse(text)
        const msg = payload.message || `导出失败 (HTTP ${resp.status})`
        ElMessage.error(msg)
        return Promise.reject(new Error(msg))
      } catch (_) {
        const msg = `导出失败 (HTTP ${resp.status})`
        ElMessage.error(msg)
        return Promise.reject(new Error(msg))
      }
    }

    if (data && typeof data === 'object' && 'code' in data) {
      // 后端成功码为 200（部分用 0），都视为成功
      if (data.code === 0 || data.code === 200) return data
      // 5xx 的服务端消息通常包含内部细节,这里用通用提示;前端会再把 server message 给 4xx
      const isServerError = data.code >= 500
      // silent 模式：调用方自己有兜底（如空状态），不要让拦截器再弹一次
      if (!silent) {
        ElMessage.error(isServerError ? '服务暂时不可用,请稍后再试' : (data.message || '请求失败'))
      }
      return Promise.reject(data)
    }
    return data
  },
  (err) => {
    const status = err.response?.status
    const silent = !!err.config?.silent
    const url = err.config?.url || ''
    const msg = status >= 500 ? '服务暂时不可用,请稍后再试'
      : (err.response?.data?.message || err.message || '网络错误')
    if (silent) return Promise.reject(err)
    // 只有需要登录的接口返回 401 才清理会话；403 表示权限不足。
    const isAuthFailure = status === 401 && (url.startsWith('/admin/') || url.startsWith('/auth/'))
    if (isAuthFailure) {
      const userStore = useUserStore()
      userStore.clearSession()
      router.push('/admin/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default http