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
     * 站点配置为「全局公共信息」,不存在用户级数据,缓存意义不大;
     * 为了避免 admin 修改昵称后前台 store 还停在旧值,默认每次调用都重新请求。
     */
    async load(force = true) {
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
