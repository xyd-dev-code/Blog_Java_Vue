import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/index.scss'
import vTilt from '@/directives/vTilt'

// ── 动态 favicon：绕过 Chrome 顽固缓存，每次加载时从 API 刷新 ──
;(function refreshFavicon() {
  const apiUrl = '/api/v1/site/favicon'
  const selector = "link[rel='icon'], link[rel='shortcut icon']"
  const link = document.querySelector(selector)
  if (!link) return
  // 用 fetch 拿一次，让浏览器知道这是新鲜资源
  fetch(apiUrl, { cache: 'no-cache' })
    .then(() => {
      // 加上时间戳确保 URL 唯一，强制 Chrome 刷新 favicon 缓存
      link.href = apiUrl + '?t=' + Date.now()
    })
    .catch(() => {})
})()

const app = createApp(App)
app.directive('tilt', vTilt)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')