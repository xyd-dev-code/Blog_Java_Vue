// Apply the last server-confirmed theme before the app mounts to avoid a flash of stale colors.
;(() => {
  const root = document.documentElement
  const fallback = 'ink'
  const cacheKey = 'blog-site-theme-cache'
  const cacheVersion = 7
  const supportedThemes = ['sunny', 'ink']
  root.dataset.themePending = 'true'
  window.__BLOG_THEME_REVEAL_TIMER__ = window.setTimeout(() => {
    root.removeAttribute('data-theme-pending')
  }, 3200)
  try {
    localStorage.removeItem('blog-theme-preference')
    const cached = JSON.parse(localStorage.getItem(cacheKey) || 'null')
    if (!cached || cached.version !== cacheVersion || !supportedThemes.includes(cached.id) || !cached.tokens) {
      root.dataset.theme = fallback
    } else {
      root.dataset.theme = cached.id
      root.style.colorScheme = cached.colorScheme || 'light'
      Object.entries(cached.tokens).forEach(([token, value]) => {
        if (token.startsWith('--') && typeof value === 'string') root.style.setProperty(token, value)
      })
    }
  } catch (_) {
    root.dataset.theme = fallback
  }

  const siteRequest = fetch('/api/v1/site', {
    cache: 'no-store',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  }).then((response) => {
    if (!response.ok) throw new Error('site config unavailable')
    return response.json()
  }).then((payload) => {
    if (!payload || (payload.code !== 0 && payload.code !== 200)) return null
    return payload.data && typeof payload.data === 'object' ? payload.data : null
  })

  window.__BLOG_SITE_CONFIG_PROMISE__ = Promise.race([
    siteRequest,
    new Promise((resolve) => window.setTimeout(() => resolve(null), 2500)),
  ]).catch(() => null)
})()
