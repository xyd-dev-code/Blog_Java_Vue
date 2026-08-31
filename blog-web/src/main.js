import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
// Element Plus 命令式 API 由使用它们的组件自行导入；这里保留共享样式。
// unplugin-auto-import 不会自动接管这些命令式 API 的样式。
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-notification.css'
import router from './router'
import './styles/index.scss'
import './styles/effects.scss'
import './styles/themes.scss'
import { DEFAULT_THEME_ID, getStoredThemeId, initializeTheme } from '@/themes/registry'
import { useThemeStore } from '@/stores/theme'
import vTilt from '@/directives/vTilt'
import vMagnetic from '@/directives/vMagnetic'

// ── 动态 favicon：直接让浏览器加载站点端点；查询参数负责刷新缓存。 ──
// 端点可能重定向到图床，使用 fetch 读取 Blob 会触发跨域拦截并污染控制台。
;(function refreshFavicon() {
  const apiUrl = '/api/v1/site/favicon'
  const link = document.querySelector('#site-favicon')
  if (!link) return
  link.href = apiUrl + '?t=' + Date.now()
})()

const mountApp = () => {
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
  return useThemeStore(pinia)
}

const bootstrap = async () => {
  // 先用最近一次服务端主题立即挂载，避免为了等待站点接口让整个页面白屏。
  const initialThemeId = getStoredThemeId() || DEFAULT_THEME_ID
  initializeTheme(initialThemeId)

  if (window.__BLOG_THEME_REVEAL_TIMER__) {
    window.clearTimeout(window.__BLOG_THEME_REVEAL_TIMER__)
    delete window.__BLOG_THEME_REVEAL_TIMER__
  }
  document.documentElement.removeAttribute('data-theme-pending')
  const themeStore = mountApp()

  // 后台配置异步到达后再校准主题；不阻塞 Vue 首屏挂载。
  try {
    const siteConfig = await window.__BLOG_SITE_CONFIG_PROMISE__
    if (!siteConfig) return
    window.__BLOG_SITE_CONFIG__ = siteConfig
    const siteThemeId = siteConfig.siteTheme || DEFAULT_THEME_ID
    // Keep reactive content and CSS in sync when the server corrects a cached/default theme.
    if (siteThemeId !== themeStore.activeThemeId) themeStore.setTheme(siteThemeId)
  } catch (_) {
    // 接口不可用时继续使用最近一次缓存主题。
  }
}

bootstrap().catch((error) => {
  document.documentElement.removeAttribute('data-theme-pending')
  console.error('[bootstrap] 应用启动失败:', error)
})
