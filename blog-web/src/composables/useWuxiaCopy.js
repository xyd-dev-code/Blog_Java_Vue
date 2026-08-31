import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { wuxiaCopy } from '@/utils/wuxiaCopy'

// The route guard also protects shared header/cards when rendered on Home.
// Derive from the store so an in-place theme change restores all original copy.
export function useWuxiaCopy() {
  const route = useRoute()
  const theme = useThemeStore()
  const isWuxiaPage = computed(() => theme.activeThemeId === 'ink' && route.path !== '/')
  // Admin keeps the theme's visuals, but always uses the original UI wording.
  const isWuxiaCopyEnabled = computed(() => isWuxiaPage.value && !/^\/admin(?:\/|$)/i.test(route.path))
  const wx = (original, themed = wuxiaCopy[original] ?? original) => (
    isWuxiaCopyEnabled.value ? themed : original
  )
  return { wx, isWuxiaPage, isWuxiaCopyEnabled }
}
