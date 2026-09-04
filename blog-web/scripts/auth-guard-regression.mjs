import assert from 'node:assert/strict'
import { chromium } from 'playwright'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { readFile } from 'node:fs/promises'

const root = fileURLToPath(new URL('..', import.meta.url))
const server = await createServer({
  root,
  configFile: false,
  plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  appType: 'custom',
  optimizeDeps: {
    noDiscovery: true,
    include: ['vue', 'vue-router', 'pinia', 'pinia-plugin-persistedstate', 'element-plus', 'axios']
  },
  server: { host: '127.0.0.1', port: 5199, strictPort: true },
  logLevel: 'error'
})
server.middlewares.use('/__auth-guard', async (_request, response) => {
  response.setHeader('Content-Type', 'text/html')
  response.end(await server.transformIndexHtml('/__auth-guard', `<!doctype html>
    <html><body><script type="module">
      import { createPinia, setActivePinia } from 'pinia'
      import { ensureAdminSession, isSessionAuthFailure } from '/src/utils/authSession.js'
      import { useUserStore } from '/src/stores/user.js'
      import http from '/src/utils/request.js'
      setActivePinia(createPinia())
      window.ensureAdminSession = ensureAdminSession
      window.isSessionAuthFailure = isSessionAuthFailure
      window.testRequestAuthHandling = async () => {
        const store = useUserStore()
        store.token = 'fixture-fixture-expired-admin-token'
        store.userInfo = { role: 'ADMIN' }
        localStorage.setItem('token', store.token)
        localStorage.setItem('user', JSON.stringify({ token: store.token, userInfo: store.userInfo }))
        await http.get('/admin/tools', { silent: true }).catch(() => {})
        const expiredResult = {
          storeToken: store.token,
          directToken: localStorage.getItem('token'),
          persistedUser: localStorage.getItem('user')
        }

        store.token = 'fixture-fixture-existing-valid-session'
        store.userInfo = { role: 'ADMIN' }
        localStorage.setItem('token', store.token)
        localStorage.setItem('user', JSON.stringify({ token: store.token, userInfo: store.userInfo }))
        await http.post('/auth/login', { username: 'bad', password: 'fixture-invalid' }).catch(() => {})
        return {
          expiredResult,
          loginFailureResult: {
            storeToken: store.token,
            directToken: localStorage.getItem('token'),
            persistedUser: localStorage.getItem('user')
          }
        }
      }
      window.authGuardReady = true
    </script></body></html>`))
})
const appHtml = await readFile(new URL('../index.html', import.meta.url), 'utf8')
server.middlewares.use(async (request, response, next) => {
  if (request.method !== 'GET' || !request.headers.accept?.includes('text/html')) return next()
  response.setHeader('Content-Type', 'text/html')
  response.end(await server.transformIndexHtml(request.url, appHtml))
})

let browser
try {
  await server.listen()
  browser = await chromium.launch({
    headless: true,
    ...(process.env.REVIEW_BROWSER_CHANNEL
      ? { channel: process.env.REVIEW_BROWSER_CHANNEL }
      : {})
  })

  const invalidContext = await browser.newContext()
  await invalidContext.addInitScript(() => {
    localStorage.setItem('token', 'fixture-local-only')
    localStorage.setItem('user', JSON.stringify({
      token: 'fixture-local-only',
      userInfo: { role: 'ADMIN' }
    }))
  })
  const invalidPage = await invalidContext.newPage()
  let invalidChecks = 0
  await invalidPage.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/v1/auth/me') {
      invalidChecks++
      return route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ code: 401, message: '登录已过期，请重新登录', data: null })
      })
    }
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: {} })
    })
  })
  await invalidPage.goto('http://127.0.0.1:5199/__auth-guard')
  await invalidPage.waitForFunction(() => window.authGuardReady)
  assert.equal(await invalidPage.evaluate(() => window.ensureAdminSession()), false)
  assert.deepEqual(await invalidPage.evaluate(() => ({
    token: localStorage.getItem('token'),
    user: localStorage.getItem('user')
  })), { token: null, user: null })
  await invalidPage.evaluate(() => {
    localStorage.setItem('user', JSON.stringify({
      token: 'fixture-persisted-only',
      userInfo: { role: 'ADMIN' }
    }))
  })
  assert.equal(await invalidPage.evaluate(() => window.ensureAdminSession()), false)
  assert.equal(invalidChecks, 2)
  assert.equal(await invalidPage.evaluate(() => localStorage.getItem('user')), null)
  await invalidContext.close()

  const raceContext = await browser.newContext()
  await raceContext.addInitScript(() => localStorage.setItem('token', 'old-token'))
  const racePage = await raceContext.newPage()
  await racePage.route('**/api/v1/auth/me', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 100))
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ code: 401, data: null })
    })
  })
  await racePage.goto('http://127.0.0.1:5199/__auth-guard')
  await racePage.waitForFunction(() => window.authGuardReady)
  const oldRequest = racePage.waitForRequest('**/api/v1/auth/me')
  const oldCheck = racePage.evaluate(() => window.ensureAdminSession())
  await oldRequest
  await racePage.evaluate(() => localStorage.setItem('token', 'new-token'))
  assert.equal(await oldCheck, false)
  assert.equal(await racePage.evaluate(() => localStorage.getItem('token')), 'new-token')
  await raceContext.close()

  const validContext = await browser.newContext()
  await validContext.addInitScript(() => localStorage.setItem('token', 'valid-admin-token'))
  const validPage = await validContext.newPage()
  let validChecks = 0
  await validPage.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/v1/auth/me') {
      validChecks++
      assert.equal(route.request().headers().authorization, 'Bearer valid-admin-token')
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { id: 1, role: 'ADMIN' } })
      })
    }
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: {} })
    })
  })
  await validPage.goto('http://127.0.0.1:5199/__auth-guard')
  await validPage.waitForFunction(() => window.authGuardReady)
  assert.equal(await validPage.evaluate(() => window.ensureAdminSession()), true)
  assert.equal(validChecks, 1)
  await validContext.close()

  const routeContext = await browser.newContext()
  await routeContext.addInitScript(() => localStorage.setItem('token', 'route-fixture-only'))
  const routePage = await routeContext.newPage()
  let routeChecks = 0
  await routePage.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/v1/auth/me') {
      routeChecks++
      return route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ code: 401, message: '登录已过期，请重新登录', data: null })
      })
    }
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: {} })
    })
  })
  await routePage.goto('http://127.0.0.1:5199/admin')
  await routePage.waitForURL(/\/admin\/login\?redirect=/)
  await routePage.locator('.login-card').waitFor()
  assert.equal(routeChecks, 1)
  assert.equal(await routePage.locator('.admin-layout').count(), 0)
  await routeContext.close()

  const requestContext = await browser.newContext()
  const requestPage = await requestContext.newPage()
  requestPage.on('pageerror', (error) => console.error('Browser error:', error.message))
  await requestPage.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/v1/admin/tools' || path === '/api/v1/auth/login') {
      return route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ code: 401, message: '认证失败', data: null })
      })
    }
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 200, data: {} }) })
  })
  await requestPage.goto('http://127.0.0.1:5199/__auth-guard')
  await requestPage.waitForFunction(() => window.authGuardReady)
  assert.equal(await requestPage.evaluate(() => window.isSessionAuthFailure(401, '/auth/login')), false)
  assert.equal(await requestPage.evaluate(() => window.isSessionAuthFailure(401, '/admin/tools')), true)
  const requestResult = await requestPage.evaluate(() => window.testRequestAuthHandling())
  assert.deepEqual(requestResult.expiredResult, {
    storeToken: '', directToken: null, persistedUser: null
  })
  assert.deepEqual(requestResult.loginFailureResult, {
    storeToken: 'fixture-fixture-existing-valid-session',
    directToken: 'fixture-fixture-existing-valid-session',
    persistedUser: JSON.stringify({ token: 'fixture-fixture-existing-valid-session', userInfo: { role: 'ADMIN' } })
  })
  await requestContext.close()

  console.log('PASS: route guard, silent 401 cleanup, and login 401 classification')
} finally {
  await browser?.close()
  await server.close()
}
