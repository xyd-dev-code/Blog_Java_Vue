import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdir, mkdtemp, readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

// Deterministic, read-only UI regression: no backend writes or real user profile.
const origin = process.env.LAYOUT_QA_ORIGIN || 'http://127.0.0.1:5173'
const output = resolve(process.env.WUXIA_QA_OUTPUT || 'design-qa-assets/wuxia-pages')
const phase = process.argv.includes('--before') ? 'before' : 'after'
await mkdir(output, { recursive: true })
const profile = await mkdtemp(join(tmpdir(), 'blog-theme-layout-'))
const edge = spawn(process.env.EDGE_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', [
  '--headless=new', '--disable-gpu', '--disable-extensions', '--disable-sync',
  '--disable-background-networking', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' })
edge.on('error', (error) => { throw error })
const delay = (ms) => new Promise((resolveDelay) => setTimeout(resolveDelay, ms))
let port
for (let attempt = 0; attempt < 120; attempt++) {
  try { port = (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; break }
  catch { await delay(250) }
}
assert.ok(port, 'Headless Edge did not start within 30 seconds')
const target = await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' }).then((r) => r.json())
const socket = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolveOpen, reject) => {
  socket.addEventListener('open', resolveOpen, { once: true })
  socket.addEventListener('error', reject, { once: true })
})
const pending = new Map()
const exceptions = []
let id = 0
let currentRoute = ''
const call = (method, params = {}) => new Promise((resolveCall, reject) => {
  const requestId = ++id
  const timeout = setTimeout(() => { pending.delete(requestId); reject(new Error(`${method} timed out`)) }, 60000)
  pending.set(requestId, { resolve: resolveCall, reject, timeout })
  socket.send(JSON.stringify({ id: requestId, method, params }))
})
const articles = Array.from({ length: 6 }, (_, index) => ({
  id: index + 1, slug: `layout-${index + 1}`, title: `主题布局回归文章 ${index + 1}`,
  summary: '记录代码、设计、生活与思考。主题切换时保留原来的容器、间距与换行。',
  content: '## 保留原始设计\n\n只替换颜色和纸纹，所有布局共用同一份组件规则。\n\n### 布局检查\n\n- 容器宽度\n- 字号与间距\n\n> 主题不是重新设计。',
  categoryName: '开发记录', categoryId: 1, createTime: '2026-08-30T12:00:00',
  viewCount: 125, commentCount: 2, tags: [{ id: 1, name: 'Vue', slug: 'vue' }],
}))
const categories = [{ id: 1, name: '开发记录', slug: 'dev', code: 'dev', articleCount: 6 }]
const tags = [{ id: 1, name: 'Vue', slug: 'vue', articleCount: 6 }, { id: 2, name: '主题', slug: 'theme', articleCount: 2 }]
const comments = Array.from({ length: 5 }, (_, index) => ({
  id: index + 1, nickname: `布局测试 ${index + 1}`, content: '保留原来的阅读区和留言布局，主题只负责换色。',
  createTime: '2026-08-30T12:00:00', likeCount: 3, featured: index === 0, isFeatured: index === 0, replies: [],
}))
const projects = Array.from({ length: 4 }, (_, index) => ({
  id: index + 1, name: `布局项目 ${index + 1}`, description: '主题与布局分离的示例项目',
  techStack: 'Vue,Java', categoryId: 1, color: '#38bdf8', githubUrl: 'https://example.com/repo', demoUrl: 'https://example.com/demo',
}))
const tools = Array.from({ length: 4 }, (_, index) => ({
  id: index + 1, name: `布局工具 ${index + 1}`, slug: `tool-${index + 1}`, description: '用于验证工具卡片的原始结构与排版',
  category: 'dev', categoryCode: 'dev', type: 'external', url: 'https://example.com/tool', tags: '开发,布局', clickCount: 10,
}))
const friends = Array.from({ length: 6 }, (_, index) => ({
  id: index + 1, name: `布局友邻 ${index + 1}`, description: '代码与生活的记录', url: 'https://example.com', status: 1,
}))
const site = {
  siteTheme: 'ink', siteName: '拾光小筑', motto: 'Hello World', authorName: '站长',
  description: '一个工程师与写作者的小角落，记录代码、设计、生活与思考。',
  siteLogo: '', userAvatar: '', github: 'https://example.com', email: 'qa@example.com',
}
const pageData = (url) => {
  const path = new URL(url).pathname.replace('/api/v1', '')
  if (path === '/site' || path === '/admin/site') return site
  if (path === '/admin/tool-categories') return []
  if (path === '/admin/profile') return { username: 'qa', nickname: 'QA', email: 'qa@example.com' }
  if (path === '/admin/dashboard') return { stats: { articleCount: 6, categoryCount: 1, commentCount: 5, viewCount: 125 }, recentArticles: articles, topArticles: articles.slice(0, 3), pendingComments: comments.slice(0, 2) }
  if (path === '/admin/articles/1') return articles[0]
  if (path.startsWith('/admin/')) {
    if (path.includes('/stats') || path.endsWith('/today')) return {}
    if (path.endsWith('/all')) return categories
    return { records: [], total: 0 }
  }
  if (path === '/home') return { featured: articles.slice(0, 3), latest: articles.slice(0, 4), stats: { articleCount: 8, categoryCount: 7, tagCount: 23, viewCount: 3755 } }
  if (path === '/categories' || path === '/project-categories' || path === '/tool-categories') return categories
  if (path === '/tags') return tags
  if (path === '/archives') return { articles }
  if (path.startsWith('/comments/')) return path.endsWith('/captcha') ? { id: 'qa', question: '1 + 1' } : comments
  if (path === '/friend-links') return friends
  if (path === '/projects') return { records: projects, total: projects.length }
  if (path === '/tools' || path === '/tools/hot') return tools
  if (/^\/articles\/layout-/.test(path)) return { article: articles[0], prev: articles[1], next: articles[2], related: articles.slice(3) }
  if (path === '/articles' || path === '/articles/search' || /\/(?:categories|tags)\/.+\/articles/.test(path)) return { records: articles, total: articles.length }
  return []
}
socket.addEventListener('message', async (event) => {
  const message = JSON.parse(event.data)
  if (message.id) {
    const item = pending.get(message.id)
    if (!item) return
    clearTimeout(item.timeout)
    pending.delete(message.id)
    if (message.error) item.reject(new Error(message.error.message))
    else item.resolve(message.result)
    return
  }
  if (message.method === 'Runtime.exceptionThrown') exceptions.push({ route: currentRoute, detail: message.params.exceptionDetails.exception?.description || message.params.exceptionDetails.text })
  if (message.method === 'Fetch.requestPaused') {
    const { requestId, request } = message.params
    const url = new URL(request.url)
    try {
      if (url.pathname.startsWith('/api/')) {
        await call('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ code: 200, data: pageData(request.url) })).toString('base64') })
      } else if (url.origin !== origin) {
        // The same local font fallback is used for both themes; no network timing or personal data.
        await call('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'text/css' }], body: '' })
      } else await call('Fetch.continueRequest', { requestId })
    } catch (error) {
      // Navigation can cancel an already paused request before CDP fulfills it.
      // This protocol cancellation is not a Runtime.exceptionThrown event.
      if (error.message !== 'Invalid InterceptionId.') exceptions.push({ route: currentRoute, detail: error.message })
    }
  }
})
const evaluate = async (expression) => {
  const response = await call('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (response.exceptionDetails) throw new Error(response.exceptionDetails.exception?.description || response.exceptionDetails.text)
  return response.result.value
}
const waitFor = async (expression) => {
  for (let attempt = 0; attempt < 450; attempt++) {
    try { if (await evaluate(expression)) return }
    catch { /* navigation may replace the execution context */ }
    await delay(100)
  }
  throw new Error(`Page did not become ready: ${currentRoute}`)
}

const allRoutes = ['/', '/articles', '/articles/layout-1', '/categories/dev', '/tags/vue', '/projects', '/tools', '/friends', '/guestbook', '/about', '/archives', '/search?kw=布局', '/search', '/missing-page', '/admin/login', '/admin/dashboard', '/admin/articles', '/admin/archives', '/admin/articles/new', '/admin/articles/1/edit', '/admin/comments', '/admin/guestbook', '/admin/projects', '/admin/tools', '/admin/friend-links', '/admin/subscriptions', '/admin/stats', '/admin/themes', '/admin/profile']
const routes = (process.env.WUXIA_QA_ROUTES ? allRoutes.filter(route=>process.env.WUXIA_QA_ROUTES.split(',').includes(route)) : allRoutes).filter(route => !process.argv.includes('--non-home') || route !== '/')
const viewports = [[1440,900],[768,1024],[390,844]].filter(([width]) => !process.env.WUXIA_QA_WIDTH || width === Number(process.env.WUXIA_QA_WIDTH))
const records = []
const snapshot = () => evaluate(`(() => {
  const geometry = ['display','position','padding','margin','gap','gridTemplateColumns','flexDirection','flexWrap','fontSize','lineHeight','letterSpacing','borderRadius','borderWidth']
  const nodes = [...document.querySelectorAll('.front-layout,.front-main,.front-main > div,.nav-inner,.container,.container-narrow,section,.page-body,.post-grid,.grid,.post-card-sky,.tool-card,.proj-card,.profile-card,.contact-grid,.hero-inner,.art-body,.art-main,.art-toc,.timeline,.login-card,.notfound,.admin-layout,.admin-aside,.admin-topbar,.admin-content,.el-card,.el-tabs,.el-table,.el-form,.el-row,.el-col,.stats-row')].filter(el=>el.getBoundingClientRect().height>0)
  return {title:document.title,text:document.body.innerText,overflow:document.documentElement.scrollWidth>innerWidth,
    boxes:nodes.map((el,i)=>{const c=getComputedStyle(el),r=el.getBoundingClientRect();return {key:i+':'+el.tagName+'.'+el.className,rect:[r.x,r.y,r.width,r.height].map(v=>Math.round(v*100)/100),styles:Object.fromEntries(geometry.map(k=>[k,c[k]]))}})}
})()`)
try {
 await call('Page.enable'); await call('Runtime.enable'); await call('Fetch.enable',{patterns:[{urlPattern:'*'}]})
 await call('Page.addScriptToEvaluateOnNewDocument',{source:`localStorage.setItem('token','fixture-local-only'); Object.defineProperty(navigator,'geolocation',{value:{getCurrentPosition:(ok,fail)=>fail({code:1})}})`})
 await call('Emulation.setEmulatedMedia',{features:[{name:'prefers-reduced-motion',value:'reduce'}]})
 for(const [width,height] of viewports) {
  await call('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:width<600})
  for(const route of routes) {
   currentRoute=route
   await call('Page.navigate',{url:origin+route})
   await waitFor(`document.querySelector('.front-main > div,.login-card,.notfound,.admin-content > div')`)
   await evaluate(`(()=>{const s=document.createElement('style');s.textContent='*,*::before,*::after{animation:none!important;transition:none!important}.reveal,.ef-rise{opacity:1!important;transform:none!important}';document.head.appendChild(s)})()`)
   await delay(850)
   await evaluate('document.fonts.ready.then(()=>true)')
   for(const theme of ['ink','sunny']) {
    await evaluate(`import('/src/stores/theme.js').then(({useThemeStore})=>useThemeStore().setTheme('${theme}'))`)
    await evaluate('document.fonts.ready.then(()=>true)')
    await waitFor(`document.querySelector('.front-main > div,.login-card,.notfound,.admin-content > div')`)
    await waitFor(`!document.querySelector('.page-fade-enter-from,.page-fade-enter-active,.page-fade-leave-active')`)
    await delay(250)
    const record={route,width,theme,...await snapshot()};records.push(record)
    if(theme==='ink' && [1440,390].includes(width) && ['/','/articles','/projects','/tools','/guestbook','/admin/login','/admin/dashboard','/missing-page'].includes(route)) {
     const shot=await call('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});await writeFile(join(output,`${phase}-${width}-${route.replace(/[^a-z0-9]/gi,'_')||'home'}.png`),Buffer.from(shot.data,'base64'))
    }
   }
   // Preserve completed measurements if a later cold module load interrupts CDP.
   if (!process.env.WUXIA_QA_ROUTES) {
    await writeFile(join(output,`${phase}.json`),JSON.stringify({records,exceptions},null,2))
   }
   console.log(`${phase} ${width} ${route}`)
  }
 }
 if (process.env.WUXIA_QA_ROUTES) {
  const previous = JSON.parse(await readFile(join(output,`${phase}.json`),'utf8'))
  records.push(...previous.records.filter(r=>!routes.includes(r.route) || (process.env.WUXIA_QA_WIDTH && r.width !== Number(process.env.WUXIA_QA_WIDTH))))
  exceptions.push(...previous.exceptions.filter(r=>!routes.includes(r.route)))
 }
 await writeFile(join(output,`${phase}.json`),JSON.stringify({records,exceptions},null,2))
 console.log(JSON.stringify({phase,cases:records.length,exceptions}))
} finally {
 await call('Browser.close').catch(()=>{});socket.close();for(const item of pending.values()) clearTimeout(item.timeout)
}
