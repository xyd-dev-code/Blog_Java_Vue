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