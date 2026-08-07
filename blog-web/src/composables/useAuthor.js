import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'

/**
 * 全站统一解析作者信息:
 *   1) 文章对象自带的 author 字段优先（后端 ArticleService.decorate 注入 admin 昵称）
 *   2) 后台已登录 admin → useUserStore.userInfo.nickname
 *   3) 未登录前台 → site store 中的 authorName(由 site store 统一加载,避免重复请求)
 *   4) 兜底 '博主'
 *
 * 返回 { authorName, authorAvatar, initial }
 */
export function useAuthor(article = null) {
  const userStore = useUserStore()
  const siteStore = useSiteStore()

  // 触发 store 加载(若已加载则为 no-op),避免重复请求 /api/v1/site
  siteStore.load()

  const authorName = computed(() => {
    // 1) 文章对象自带 author 字段(后端在 ArticleService.decorate 中注入 admin.nickname)
    // 这是最准的源,因为每次 article 加载时后端都会从 user 表拿最新 nickname
    if (article?.value?.author) return article.value.author
    if (article?.author) return article.author
    // 2) 后台已登录 admin
    if (userStore.userInfo?.nickname && userStore.userInfo?.role === 'ADMIN') {
      return userStore.userInfo.nickname
    }
    // 3) 站点配置中的 authorName
    return siteStore.info?.authorName || '博主'
  })

  const authorAvatar = computed(() => {
    if (article?.value?.authorAvatar) return article.value.authorAvatar
    if (article?.authorAvatar) return article.authorAvatar
    return userStore.userInfo?.avatar || ''
  })

  const initial = computed(() => (authorName.value || '博')[0])

  return { authorName, authorAvatar, initial }
}
