import { computed, readonly, ref } from 'vue'
import { defineStore } from 'pinia'
import { applyTheme, DEFAULT_THEME_ID, getTheme, themeRegistry } from '@/themes/registry'

export const useThemeStore = defineStore('theme', () => {
  const initialId = typeof document === 'undefined'
    ? DEFAULT_THEME_ID
    : (document.documentElement.dataset.theme || DEFAULT_THEME_ID)
  const activeThemeId = ref(getTheme(initialId).id)

  const themes = readonly(themeRegistry.map(({ tokens, ...meta }) => meta))
  const activeTheme = computed(() => getTheme(activeThemeId.value))

  const setTheme = (themeId) => {
    const theme = applyTheme(themeId)
    activeThemeId.value = theme.id
  }

  return { activeThemeId, activeTheme, themes, setTheme }
})

