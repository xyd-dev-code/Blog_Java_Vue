import dayjs from 'dayjs'

export const fmtDate = (d, p = 'YYYY-MM-DD') => d ? dayjs(d).format(p) : ''
export const fmtDateTime = (d) => fmtDate(d, 'YYYY-MM-DD HH:mm')
export const fromNow = (d) => {
  if (!d) return ''
  const diff = Date.now() - new Date(d).getTime()
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return '刚刚'
  if (sec < 3600) return `${Math.floor(sec / 60)} 分钟前`
  if (sec < 86400) return `${Math.floor(sec / 3600)} 小时前`
  if (sec < 86400 * 30) return `${Math.floor(sec / 86400)} 天前`
  return fmtDate(d)
}

export const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
}

export const excerpt = (html, len = 120) => {
  const s = stripHtml(html)
  return s.length > len ? s.slice(0, len) + '…' : s
}

// 转义 HTML，防止 XSS（评论内容先经后端 Jsoup 清洗，这里再做一次前端兜底）
export const escapeHtml = (s) => {
  if (!s) return ''
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 将评论正文渲染为安全 HTML：
 * - 先转义所有 HTML（杜绝注入）
 * - 识别 http(s) 链接并转为可点击外链
 * - 识别 @昵称 并渲染为可点击的高亮标签（data-name 供父组件定位）
 * - 换行转为 <br>
 */
export const renderComment = (text) => {
  if (!text) return ''
  let s = escapeHtml(text)
  // @提及先于 URL：避免 URL 中的 @（如 https://x.com/@user）被误包一层 <a> 导致链接 HTML 破碎
  s = s.replace(/@([^\s@<]{1,20})/g,
    '<a class="cmt-mention" data-name="$1">@$1</a>')
  // URL 识别（[^\s<]+ 遇到已注入的 < 标签会自然停止，不会侵入 @mention 标签内部）
  s = s.replace(/(https?:\/\/[^\s<]+)/g,
    '<a class="cmt-link" href="$1" target="_blank" rel="noopener noreferrer">$1</a>')
  // 换行
  s = s.replace(/\n/g, '<br>')
  return s
}