import { onMounted, onBeforeUnmount } from 'vue'

/**
 * 全局滚动揭示 — 任何匹配 selector 的元素进入视口时加 .visible
 * 在 FrontLayout 中调用一次即可覆盖整站动态内容
 *
 * @param {string} selector 观察目标，支持逗号分隔多个
 * @param {number} threshold 进入视口的比例阈值
 */
export function useScrollReveal(selector = '.reveal', threshold = 0.1) {
  let observer = null
  let mutationObserver = null
  let rafId = null
  const seen = new WeakSet()

  const show = (el) => {
    if (seen.has(el)) return
    el.classList.add('visible')
    seen.add(el)
    observer?.unobserve(el)
  }

  // 收集尚未观察的新节点（动态列表加载后调用）
  const scan = () => {
    document.querySelectorAll(selector).forEach((el) => {
      if (!seen.has(el)) observer.observe(el)
    })
  }

  // 无 IntersectionObserver 的老浏览器：直接全部显形，保证内容可读
  const revealAll = () => {
    document.querySelectorAll(selector).forEach((el) => {
      el.classList.add('visible')
      seen.add(el)
    })
  }

  onMounted(() => {
    if (typeof IntersectionObserver === 'undefined') {
      revealAll()
      return
    }

    observer = new IntersectionObserver(
      (entries) => entries.forEach((e) => e.isIntersecting && show(e.target)),
      { threshold, rootMargin: '0px 0px -8% 0px' }
    )
    scan()

    // 动态内容（异步列表、路由切换）出现后补观察；用 rAF 合并同一帧内的多次变更
    mutationObserver = new MutationObserver(() => {
      if (rafId) return
      rafId = requestAnimationFrame(() => {
        rafId = null
        scan()
      })
    })
    mutationObserver.observe(document.body, { childList: true, subtree: true })
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    mutationObserver?.disconnect()
    if (rafId) cancelAnimationFrame(rafId)
  })
}
