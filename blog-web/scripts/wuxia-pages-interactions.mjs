import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import assert from 'node:assert/strict'

const require = createRequire(import.meta.url)
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const origin = process.env.LAYOUT_QA_ORIGIN || 'http://127.0.0.1:5173'
const output = resolve('design-qa-assets/wuxia-pages/states')
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' })
const exceptions = [], checks = [], blockedWrites = []
page.on('pageerror', error => exceptions.push(error.message))
// Fully intercepted fixtures; a dummy token never reaches a real server.
await page.addInitScript(() => localStorage.setItem('token', 'fixture-local-only'))
await page.route(url => url.pathname.startsWith('/api/'), async route => {
  const req = route.request(), path = new URL(req.url()).pathname.replace('/api/v1','')
  if (!['GET','HEAD'].includes(req.method())) blockedWrites.push({ path, method: req.method() })
  let data = []
  if (path === '/site' || path === '/admin/site') data = { siteTheme: 'ink', siteName: '拾光小筑', authorName: '站长' }
  else if (path === '/home') data = { stats: {}, featured: [], latest: [] }
  else if (path === '/archives') data = { articles: [] }
  else if (path === '/admin/tool-categories') data = []
  else if (path.startsWith('/admin/')) data = path.endsWith('/all') ? [] : { records: [], total: 0 }
  else if (path.endsWith('/captcha')) data = { enabled: false }
  else if (['/articles','/articles/search','/projects'].includes(path) || path.endsWith('/articles')) data = { records: [], total: 0 }
  await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data }) })
})
// Avatar/CDN/weather/font latency must not decide local UI test outcomes.
await page.route(url => url.origin !== origin, route => route.fulfill({ status: 200, contentType: 'text/plain', body: '' }))
const theme = async id => {
  await page.evaluate(async id => { const { useThemeStore } = await import('/src/stores/theme.js'); useThemeStore().setTheme(id) }, id)
  await page.evaluate(() => document.fonts.ready)
}
const ready = async path => {
  await page.goto(origin + path, { waitUntil: 'networkidle' })
  await theme('ink')
  await page.addStyleTag({ content: '*,*::before,*::after{animation:none!important;transition:none!important}.reveal,.ef-rise{opacity:1!important;transform:none!important}' })
}
const check = (name, value) => { checks.push({ name, pass: !!value }); assert.ok(value, name) }
try {
  for (const path of ['/articles','/projects','/tools','/friends','/guestbook','/archives','/search?kw=missing','/missing-page']) {
    await ready(path)
    check(`empty state ${path}`, await page.locator('.el-empty,.empty-state,.empty-title,.notfound').count() > 0)
    check(`no overflow ${path}`, await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  }
  await ready('/friends')
  await page.getByRole('button', { name: '递交拜帖', exact: true }).first().click()
  check('friend application remains accessible', await page.getByRole('dialog', { name: '递交拜帖' }).isVisible())
  await page.getByRole('button', { name: '呈上拜帖', exact: true }).click()
  await page.getByText('请输入站点名称', { exact: true }).waitFor()
  check('validation is explicit', await page.getByText('请输入站点名称', { exact: true }).isVisible())
  await page.screenshot({ path: resolve(output, 'friend-dialog.png') })

  await ready('/guestbook')
  await page.locator('.hg-publish').click()
  check('guestbook form opens', await page.getByRole('dialog').isVisible())
  check('themed submit action', await page.getByRole('button', { name: '落笔留书', exact: true }).count() > 0)
  await page.locator('.gb-input input').first().focus()
  await page.screenshot({ path: resolve(output, 'guestbook-dialog.png') })

  await ready('/about')
  await page.locator('.sub-trigger').click()
  check('subscription copy explains confirmation', await page.getByRole('dialog').innerText().then(t => t.includes('确认链接')))

  await ready('/admin/login')
  await page.locator('.login-btn').click()
  await page.getByText('请输入用户名', { exact: true }).waitFor()
  check('login validation preserved', await page.getByText('请输入用户名', { exact: true }).isVisible())
  await page.screenshot({ path: resolve(output, 'login.png') })

  for (const [path, labels] of [
    ['/admin/articles',['秘籍名录','秘籍门派','标签']],
    ['/admin/comments',['论剑','举报']],
    ['/admin/projects',['兵谱名录','兵谱门类']],
    ['/admin/tools',['兵器名录','兵器门类']],
  ]) {
    await ready(path)
    for (const label of labels) {
      await page.getByRole('tab', { name: label, exact: true }).click()
      check(`tab ${path} ${label}`, await page.getByRole('tab', { name: label, exact: true }).getAttribute('aria-selected') === 'true')
    }
  }

  await page.setViewportSize({ width: 390, height: 844 })
  await ready('/tools')
  await page.getByRole('button', { name: '菜单', exact: true }).click()
  await page.locator('.drawer-panel').waitFor({ state: 'visible' })
  await page.waitForFunction(() => document.querySelector('.drawer-link[href="/articles"]')?.textContent.trim() === '秘籍')
  check('mobile themed menu', (await page.locator('.drawer-link[href="/articles"]').innerText()).trim() === '秘籍')
  await page.screenshot({ path: resolve(output, 'mobile-menu.png') })
  await page.locator('.drawer-link[href="/articles"]').click()
  await page.waitForURL('**/articles')
  check('mobile menu closes on navigation', await page.locator('.mobile-drawer').count() === 0)
  await theme('sunny')
  check('sunny copy restored', await page.locator('.ha-title').innerText().then(t => t.includes('所有文章')))
  check('sunny title restored', (await page.title()).startsWith('文章'))
  check('sunny decorations removed', await page.locator('.app-root').getAttribute('data-wuxia-page') === null)
  await theme('ink')
  check('ink title restored', (await page.title()).startsWith('秘籍'))
  await page.locator('.brand').click()
  await page.waitForURL(origin + '/')
  check('homepage scope stays untouched', await page.locator('.app-root').getAttribute('data-wuxia-page') === null)

  check('no runtime errors', exceptions.length === 0)
} finally {
  await writeFile(resolve(output, 'report.json'), JSON.stringify({ checks, exceptions, blockedWrites, fixtures: true }, null, 2))
  await browser.close()
}
console.log(JSON.stringify({ pass: checks.every(c => c.pass), checks: checks.length, exceptions, blockedWrites }))
