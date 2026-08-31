import { defineStore } from 'pinia'
import { siteConfig } from '@/api/front'

export const useSiteStore = defineStore('site', {
  state: () => ({
    info: null,
    loaded: false
  }),
  actions: {
    /**
     * 拉取站点配置。
     * 默认只在未加载过时请求(force=false),避免一次页面渲染触发多次 /api/v1/site。
     * 需要强制刷新(如 admin 修改站点信息后)请传 force=true 或调用 reload()。
     */
    async load(force = false) {
      if (!force && this.loaded) return this.info
      if (!force && typeof window !== 'undefined' && window.__BLOG_SITE_CONFIG__) {
        this.info = window.__BLOG_SITE_CONFIG__
        this.loaded = true
        return this.info
      }
      try {
        const resp = await siteConfig()
        this.info = resp.data
        this.loaded = true
        if (typeof window !== 'undefined') window.__BLOG_SITE_CONFIG__ = resp.data
        return this.info
      } catch (e) {
        // 请求失败时保持 loaded=false,下次可以重试
        return null
      }
    },
    /** 强制重新拉取 */
    async reload() {
      return this.load(true)
    },
    /** 管理员保存站点主题成功后，同步当前页面使用的公开站点配置快照。 */
    setSiteTheme(themeId) {
      this.info = { ...(this.info || {}), siteTheme: themeId }
      this.loaded = true
      if (typeof window !== 'undefined') window.__BLOG_SITE_CONFIG__ = this.info
    }
  }
})
