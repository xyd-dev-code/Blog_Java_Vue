import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { chromium } from 'playwright'
import { fileURLToPath } from 'node:url'
const root = fileURLToPath(new URL('..', import.meta.url))
const server = await createServer({ root, configFile: false, plugins: [vue()], resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, optimizeDeps: { noDiscovery: true, include: ['vue', 'vue-router', 'pinia', 'element-plus', 'md-editor-v3', 'dompurify', 'js-yaml', 'axios', 'dayjs', '@element-plus/icons-vue'] }, appType: 'custom', server: { host: '127.0.0.1', port: 5198, strictPort: true }, logLevel: 'error' })
server.middlewares.use('/__review', async (request, response) => {
  response.setHeader('Content-Type', 'text/html')
  response.end(await server.transformIndexHtml('/__review', '<!doctype html><html><body><div id="app"></div><script type="module" src="/scripts/review-harness.js"></script></body></html>'))
})
let browser
let logoutRequests = 0
try {
  await server.listen()
  browser = await chromium.launch({ headless: true, ...(process.env.REVIEW_BROWSER_CHANNEL ? { channel: process.env.REVIEW_BROWSER_CHANNEL } : {}) })
  const page = await browser.newPage()
  page.on('pageerror', error => console.error('Browser error:',error.message))
  await page.route('**/*', async route => {
    const url = new URL(route.request().url())
    if (url.hostname !== '127.0.0.1') return route.abort()
    if (!url.pathname.startsWith('/api/v1/')) return route.continue()
    let data = []
    if(url.pathname === '/api/v1/admin/articles/1') data = { id:1,title:'Review article',content:'Original body',isTop:0,isFeatured:0,allowComment:1,tags:[] }
    if(url.pathname === '/api/v1/admin/articles/2') data = { id:2,title:'Second article',content:'Second body',isTop:0,isFeatured:0,allowComment:1,tags:[] }
    if(url.pathname === '/api/v1/comments/100/replies') {
      const first = url.searchParams.get('afterId') === '0'
      data = { records: [{ id:first ? 101 : 102,parentId:100,nickname:'Reply',content:first ? 'First reply' : 'Second reply',replies:[],hasMoreReplies:false }], hasMore:first }
    }
    if(url.pathname === '/api/v1/site') data = { siteTheme:'sky' }
    if(url.pathname.endsWith('/captcha')) data = { enabled:false }
    if(url.pathname === '/api/v1/auth/logout') logoutRequests++
    return route.fulfill({ contentType:'application/json',body:JSON.stringify({code:200,data}) })
  })
  await page.goto('http://127.0.0.1:5198/__review')
  await page.waitForFunction(() => window.reviewReady, { timeout: 60000 })
  await page.evaluate(() => window.mountMarkdown('```x"><details/open/ontoggle=globalThis.__reviewXss=1>\nSAFE\n```\n<img src=x onerror="globalThis.__reviewXss=2">'))
  await page.locator('.md-editor-preview').waitFor()
  await page.waitForTimeout(400)
  assert.equal(await page.evaluate(() => window.__reviewXss), undefined)
  assert.equal(await page.locator('[onerror],[ontoggle]').count(),0)
  const parsed = await page.evaluate(() => window.parseImport('demo.md','---\ntitle: demo\n---\nbody'))
  assert.equal(parsed.title,'demo')
  const malicious = await page.evaluate(() => window.parseImport('bad.md','---\ntitle: !!omap [a: 1]\n---\nbody'))
  assert.equal(malicious.status,'err')
  await page.evaluate(() => window.mountClosedComments())
  await page.getByText('文章已关闭评论', { exact:true }).waitFor()
  assert.equal(await page.locator('.comment-form,.trigger-btn').count(),0)
  await page.evaluate(() => window.mountReplyTree())
  await page.getByRole('button', { name: '加载更多回复' }).click()
  await page.getByText('First reply', { exact: true }).waitFor()
  await page.getByRole('button', { name: '加载更多回复' }).click()
  await page.getByText('Second reply', { exact: true }).waitFor()
  assert.equal(await page.getByRole('button', { name: '加载更多回复' }).count(), 0)
  await page.evaluate(() => window.mountEditor())
  await page.waitForFunction(() => document.querySelector('.article-edit input')?.value === 'Review article')
  await page.evaluate(() => window.navigateToArticle(2))
  await page.waitForFunction(() => document.querySelector('.article-edit input')?.value === 'Second article')
  await page.waitForTimeout(200)
  await page.locator('.el-switch__core').first().click()
  await page.evaluate(() => { window.reviewNavDone = false; window.navigateAway().then(() => { window.reviewNavDone = true }) })
  await page.locator('.el-message-box').waitFor()
  await page.locator('.el-message-box__btns button').first().click()
  await page.waitForFunction(() => window.reviewNavDone)
  await page.locator('.el-message-box').waitFor({ state: 'hidden' })
  assert.equal(await page.locator('#left-editor').count(),0)
  await page.evaluate(() => { window.reviewNavDone = false; window.navigateAway().then(() => { window.reviewNavDone = true }) })
  await page.locator('.el-message-box').waitFor()
  await page.locator('.el-message-box__btns button').last().click()
  await page.waitForFunction(() => window.reviewNavDone)
  await page.locator('#left-editor').waitFor()
  assert.equal(await page.evaluate(() => window.testLogout()),'')
  assert.equal(logoutRequests,1)
  console.log('PASS: Markdown XSS, YAML Worker, closed comments, reply pagination, reused editor route, unsaved switches/navigation, server logout')
} finally {
  await browser?.close()
  await server.close()
}
