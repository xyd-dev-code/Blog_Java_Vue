import { onMounted, onBeforeUnmount } from 'vue'

/**
 * 全局滚动揭示 — 任何带 .reveal class 的元素进入视口时淡入
 * 在 FrontLayout 中调用一次即可覆盖整站动态内容
 */
export function useScrollReveal(selector = '.reveal', threshold = 0.1) {
  let observer = null
  let fallbackTimer = null
  const seen = new WeakSet()

  const show = (el) => {
    if (!seen.has(el)) {
      el.classList.add('visible')
      seen.add(el)
      observer?.unobserve(el)
    }
  }

  const observe = () => {
    if (observer) observer.disconnect()
    observer = new IntersectionObserver((entries) => {
      entries.forEach(e => e.isIntersecting && show(e.target))
    }, { threshold })
    document.querySelectorAll(selector).forEach(el => {
      if (!seen.has(el)) observer.observe(el)
    })
  }

  const fallback = () => {
    document.querySelectorAll(`${selector}:not(.visible)`).forEach(el => show(el))
  }

  onMounted(() => {
    if (typeof IntersectionObserver === 'undefined') {
      fallback()
      return
    }
    observe()
    // 监听 DOM 变化，处理动态加载内容
    const mutationObserver = new MutationObserver(() => {
      document.querySelectorAll(selector).forEach(el => {
        if (!seen.has(el)) observer.observe(el)
      })
    })
    mutationObserver.observe(document.body, { childList: true, subtree: true })
    fallbackTimer = setTimeout(fallback, 2000)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    if (fallbackTimer) clearTimeout(fallbackTimer)
  })
}
