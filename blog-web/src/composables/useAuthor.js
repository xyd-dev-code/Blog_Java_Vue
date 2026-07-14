import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { siteConfig } from '@/api/front'

/**
 * 全站统一解析作者信息（最终方案）:
 *   1) 文章对象自带的 author 字段优先（后端 ArticleService.decorate 注入 admin 昵称）
 *   2) 后台已登录 admin → useUserStore.userInfo.nickname
 *   3) 未登录前台 → 实时从后端 site config 取 authorName
 *   4) 兜底 '博主'
 *
 * 返回 { authorName, authorAvatar, initial }
 */
export function useAuthor(article = null) {
  const userStore = useUserStore()
  const siteStore = useSiteStore()

  // 本地 ref,组件挂载时再拉一次,绕开任何 store 缓存
  const liveAuthorName = ref('')

  // 触发 store 刷新
  siteStore.load()

  onMounted(async () => {
    try {
      const resp = await siteConfig()
      if (resp?.data?.authorName) {
        liveAuthorName.value = resp.data.authorName
      }
    } catch (_) {}
  })

  // watch store 实时同步
  watch(
    () => siteStore.info?.authorName,
    (v) => { if (v) liveAuthorName.value = v },
    { immediate: true }
  )

  const authorName = computed(() => {
    // 1) 文章对象自带 author 字段(后端在 ArticleService.decorate 中注入 admin.nickname)
    // 这是最准的源,因为每次 article 加载时后端都会从 user 表拿最新 nickname
    if (article?.value?.author) return article.value.author
    if (article?.author) return article.author
    // 2) 后台已登录 admin
    if (userStore.userInfo?.nickname && userStore.userInfo?.role === 'ADMIN') {
      return userStore.userInfo.nickname
    }
    // 3) 实时拉取的 authorName
    return liveAuthorName.value || '博主'
  })

  const authorAvatar = computed(() => {
    if (article?.value?.authorAvatar) return article.value.authorAvatar
    if (article?.authorAvatar) return article.authorAvatar
    return userStore.userInfo?.avatar || ''
  })

  const initial = computed(() => (authorName.value || '博')[0])

  return { authorName, authorAvatar, initial }
}
