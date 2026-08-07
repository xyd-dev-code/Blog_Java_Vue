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
      try {
        const resp = await siteConfig()
        this.info = resp.data
        this.loaded = true
      } catch (e) {
        // 请求失败时保持 loaded=false,下次可以重试
      }
    },
    /** 强制重新拉取 */
    async reload() {
      return this.load(true)
    }
  }
})

/**
 * 模块加载即触发一次拉取。store 是 pinia 单例,此副作用只执行一次,
 * 早于任何组件渲染,避免首屏渲染陈旧的 authorName 闪现。
 */
const _site = useSiteStore()
_site.load()
