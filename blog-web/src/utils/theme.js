import { DEFAULT_CONTENT_ACCENT } from '@/themes/registry'

/** 读取当前主题 token 的最终值，供 Canvas / ECharts 等不能解析 CSS var 的 API 使用。 */
export const resolveThemeToken = (token) => {
  if (typeof document === 'undefined') return ''
  return getComputedStyle(document.documentElement).getPropertyValue(token).trim()
}

export const resolveThemeTokens = (tokens) => tokens.map(resolveThemeToken)

/** 历史默认蓝跟随主题；用户明确选择的自定义内容色保持原值。 */
export const toThemeColor = (color, fallbackToken = '--c-botany-500') => {
  if (!color || String(color).toLowerCase() === DEFAULT_CONTENT_ACCENT) {
    return `var(${fallbackToken})`
  }
  return color
}

export const subscribeThemeChange = (handler) => {
  window.addEventListener('blog:theme-change', handler)
  return () => window.removeEventListener('blog:theme-change', handler)
}
