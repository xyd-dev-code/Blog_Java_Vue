import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
// ── Element Plus 命令式 API（ElMessage / ElMessageBox / ElNotification）按需从深路径 import ──
// 仅打包 ElMessage 函数自身的依赖链（不会拉到 el-table / el-form 等业务组件），
// 并手动注入对应 CSS —— unplugin-auto-import 不会自动接管这些命令式 API 的样式。
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElNotification } from 'element-plus/es/components/notification/index.mjs'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-notification.css'
import router from './router'
import './styles/index.scss'
import './styles/effects.scss'
import vTilt from '@/directives/vTilt'
import vMagnetic from '@/directives/vMagnetic'

// ── 动态 favicon：读取圆形 PNG 后改用 Blob URL，彻底绕过 Chrome 独立的 favicon 缓存 ──
;(async function refreshFavicon() {
  const apiUrl = '/api/v1/site/favicon'
  const link = document.querySelector('#site-favicon')
  if (!link) return
  const cacheBuster = apiUrl + '?t=' + Date.now()
  try {
    const response = await fetch(cacheBuster, { cache: 'no-store', credentials: 'same-origin' })
    if (!response.ok) throw new Error('favicon request failed')
    const blobUrl = URL.createObjectURL(await response.blob())
    link.href = blobUrl
    window.addEventListener('pagehide', () => URL.revokeObjectURL(blobUrl), { once: true })
  } catch (_) {
    // 兼容尚未升级、仍返回外部重定向的后端。
    link.href = cacheBuster
  }
})()

const app = createApp(App)
app.directive('tilt', vTilt)
app.directive('magnetic', vMagnetic)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// Element Plus 改为按需引入（见 vite.config.js 的 AutoImport / Components + ElementPlusResolver），
// 不再在此整包注册；中文语言由 App.vue 的 <el-config-provider> 承接。
app.use(pinia)
app.use(router)
app.mount('#app')
