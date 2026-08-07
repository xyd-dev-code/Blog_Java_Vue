import http from '@/utils/request'

/** 记录一次分享点击(每日同 IP+UA+渠道去重)。返回最新 shareCount + 是否被计数。 */
export const trackShare = (articleId, channel) =>
  http.post('/share/click', { articleId, channel })