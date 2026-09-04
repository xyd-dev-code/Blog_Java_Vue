const DIRECT_TOKEN_KEY = 'token'
const PERSISTED_USER_KEY = 'user'
const AUTH_CHECK_TIMEOUT_MS = 10000

export const readStoredToken = () => {
  try {
    const directToken = localStorage.getItem(DIRECT_TOKEN_KEY)?.trim()
    if (directToken) return directToken

    const persistedUser = JSON.parse(localStorage.getItem(PERSISTED_USER_KEY) || 'null')
    return typeof persistedUser?.token === 'string' ? persistedUser.token.trim() : ''
  } catch (_) {
    return ''
  }
}

export const clearStoredAuthSession = () => {
  try {
    localStorage.removeItem(DIRECT_TOKEN_KEY)
    localStorage.removeItem(PERSISTED_USER_KEY)
  } catch (_) {}
}

/**
 * 后台路由不能凭“本地存在一个字符串”放行，必须由服务端确认 token 和 ADMIN 角色。
 * 网络或服务异常时同样关闭后台入口，但只在明确未授权时清除本地会话。
 */
export const verifyAdminSession = async (token) => {
  if (!token) return { authenticated: false, invalid: true }

  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), AUTH_CHECK_TIMEOUT_MS)
  try {
    const response = await fetch('/api/v1/auth/me', {
      method: 'GET',
      cache: 'no-store',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token}`
      },
      signal: controller.signal
    })

    if (response.status === 401 || response.status === 403) {
      return { authenticated: false, invalid: true }
    }
    if (!response.ok) return { authenticated: false, invalid: false }

    const payload = await response.json()
    const succeeded = payload?.code === 0 || payload?.code === 200
    const isAdmin = succeeded && payload?.data?.role === 'ADMIN'
    return { authenticated: isAdmin, invalid: !isAdmin }
  } catch (_) {
    return { authenticated: false, invalid: false }
  } finally {
    window.clearTimeout(timeout)
  }
}

export const ensureAdminSession = async () => {
  const checkedToken = readStoredToken()
  const verification = await verifyAdminSession(checkedToken)
  if (verification.invalid && readStoredToken() === checkedToken) clearStoredAuthSession()
  return verification.authenticated
}

export const isSessionAuthFailure = (status, url = '') => {
  if (status !== 401) return false
  return url.startsWith('/admin/')
    || url.startsWith('/auth/me')
    || url.startsWith('/auth/logout')
    || url.startsWith('/auth/change-password')
}
