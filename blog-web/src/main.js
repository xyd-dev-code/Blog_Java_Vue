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

// ── 动态 favicon：绕过 Chrome 顽固缓存，每次加载时从 API 刷新 ──
;(function refreshFavicon() {
  const apiUrl = '/api/v1/site/favicon'
  const selector = "link[rel='icon'], link[rel='shortcut icon']"
  const link = document.querySelector(selector)
  if (!link) return
  // 直接给 link 设置带时间戳的 URL 即可强制刷新缓存。
  // 不要先用 fetch 预取：/favicon 接口会 302 跳转到外部图床，
  // 外部图床没有 CORS 头，fetch 跨域会被浏览器拦截并报错。
  // 而 <link rel="icon"> 加载图片不受 CORS 限制，可直接设置。
  link.href = apiUrl + '?t=' + Date.now()
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