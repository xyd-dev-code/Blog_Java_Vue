import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdir, mkdtemp, readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { hasWuxiaForegroundOverlap } from './wuxia-art-visibility.mjs'

// Deterministic, read-only UI regression: no backend writes or real user profile.
const origin = process.env.LAYOUT_QA_ORIGIN || 'http://127.0.0.1:5173'
const output = resolve('design-qa-assets/theme-layout-regression')
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
  const timeout = setTimeout(() => { pending.delete(requestId); reject(new Error(`${method} timed out`)) }, 20000)
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
  siteTheme: 'sunny', siteName: '拾光小筑', motto: 'Hello World', authorName: '站长',
  description: '一个工程师与写作者的小角落，记录代码、设计、生活与思考。',
  siteLogo: '', userAvatar: '', github: 'https://example.com', email: 'qa@example.com',
}
const pageData = (url) => {
  const path = new URL(url).pathname.replace('/api/v1', '')
  if (path === '/site') return site
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
    } catch (error) { exceptions.push({ route: currentRoute, detail: error.message }) }
  }
})
const evaluate = async (expression) => {
  const response = await call('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (response.exceptionDetails) throw new Error(response.exceptionDetails.exception?.description || response.exceptionDetails.text)
  return response.result.value
}
const waitFor = async (expression) => {
  for (let attempt = 0; attempt < 120; attempt++) {
    try { if (await evaluate(expression)) return }
    catch { /* navigation may replace the execution context */ }
    await delay(100)
  }
  throw new Error(`Page did not become ready: ${currentRoute}`)
}
const properties = [
  'display', 'position', 'width', 'height', 'minWidth', 'maxWidth', 'minHeight', 'maxHeight',
  'marginTop', 'marginRight', 'marginBottom', 'marginLeft', 'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft',
  'borderTopWidth', 'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth',
  'borderTopLeftRadius', 'borderTopRightRadius', 'borderBottomLeftRadius', 'borderBottomRightRadius',
  'gap', 'rowGap', 'columnGap', 'gridTemplateColumns', 'gridTemplateRows', 'gridAutoFlow',
  'alignItems', 'alignContent', 'justifyItems', 'justifyContent', 'flexBasis', 'flexGrow', 'flexShrink', 'flexDirection', 'flexWrap',
  'overflowX', 'overflowY', 'fontFamily', 'fontSize', 'fontWeight', 'lineHeight', 'letterSpacing', 'whiteSpace', 'textAlign', 'clipPath',
]
const snapshot = () => evaluate(`(() => {
  const skip = '[aria-hidden="true"],.deco,.hero-light,.god-rays,.sunny-decor,.ink-hero-scene,.cursor-dot,.cursor-ring'
  const nodes = [...document.querySelectorAll('html,body,#app,.app-root,.front-layout,.app-header,.app-header *,.front-main,.front-main *,.app-footer,.app-footer *')]
    .filter(el => !el.closest(skip) && !['SCRIPT','STYLE','SVG','PATH','G','USE','CIRCLE','RECT','ELLIPSE','DEFS','LINE'].includes(el.tagName))
  const elements = nodes.map((el, index) => {
    const css = getComputedStyle(el)
    const rect = el.getBoundingClientRect()
    return { key: index + ':' + el.tagName + '.' + [...el.classList].filter(c => c !== 'visible').join('.'),
      rect: ['x','y','width','height'].map(key => Math.round(rect[key] * 1000) / 1000),
      styles: Object.fromEntries(${JSON.stringify(properties)}.map(key => {
        // CSSOM may serialize an auto margin as either 0px or its resolved width after paint invalidation.
        // Keep its actual computed value; the used position is independently checked by rect above.
        const property = key.replace(/[A-Z]/g, char => '-' + char.toLowerCase())
        const typed = key.startsWith('margin') ? el.computedStyleMap?.().get(property)?.toString() : null
        // The user requested a calligraphy face on display titles. Their size,
        // line-height, spacing and actual rectangles remain independently checked.
        const displayTitle = el.closest('.hero-title,.title-accent,.section-title,.sky-page-head h1,.ha-title,.hf-title,.hg-title,.hs-title,.cta-title') || (el.closest('.home-sky') && el.closest('.hero-desc'))
        return [key, key === 'fontFamily' && displayTitle ? 'display-typeface' : typed === 'auto' ? 'auto' : css[key]]
      })) }
  })
  const hero = document.querySelector('.hero-text')
  const header = document.querySelector('.app-header')
  const comments = document.querySelector('.comment-list:not(.skeleton-list)')
  const home = document.querySelector('.home-sky')
  const layout = document.querySelector('.front-layout')
  const scene = document.querySelector('.ink-hero-scene')
  const paper = document.querySelector('.ink-paper-layer')
  const homeBackground = home ? {
    image: getComputedStyle(layout).backgroundImage,
    color: getComputedStyle(layout).backgroundColor,
    top: layout.getBoundingClientRect().top,
    left: layout.getBoundingClientRect().left,
    right: layout.getBoundingClientRect().right,
    viewportWidth: document.documentElement.clientWidth,
    contentBackground: getComputedStyle(home).backgroundColor,
    contentImage: getComputedStyle(home).backgroundImage,
    sceneBackground: getComputedStyle(scene).backgroundColor,
    paperOpacity: getComputedStyle(paper).opacity,
    paperLoaded: paper.complete && paper.naturalWidth > 0,
    artRects: [...scene.querySelectorAll('img')].map(img => {
      const rect = img.getBoundingClientRect()
      return ['x','y','width','height'].map(key => Math.round(rect[key] * 1000) / 1000)
    }),
  } : null
  const heroSection = document.querySelector('.sky-hero')
  const rectOf = el => ['x','y','width','height'].map(key => Math.round(el.getBoundingClientRect()[key] * 1000) / 1000)
  const homeLayout = home ? {
    nav: rectOf(document.querySelector('.nav-inner')),
    grid: getComputedStyle(document.querySelector('.hero-grid')).gridTemplateColumns,
    weather: (() => { const r = document.querySelector('.weather-card').getBoundingClientRect(); return [r.x,r.width] })(),
    sections: [...home.querySelectorAll(':scope > .section')].map(el => {
      const r = el.getBoundingClientRect()
      return [r.x,r.y-heroSection.getBoundingClientRect().bottom,r.width,r.height]
    }),
    overlap: (${hasWuxiaForegroundOverlap.toString()})(),
    noOverflow: document.documentElement.scrollWidth <= innerWidth,
    copyLeftMargin: getComputedStyle(hero).marginLeft,
    copyTopMargin: getComputedStyle(hero).marginTop,
  } : null
  return { elements, scrollHeight: document.documentElement.scrollHeight, scrollWidth: document.documentElement.scrollWidth, homeLayout,
    homeBackground,
    surfaces: { heroPseudo: hero ? getComputedStyle(hero, '::before').content : null,
      headerBackground: header ? getComputedStyle(header).backgroundColor : null,
      commentsBackground: comments ? getComputedStyle(comments).backgroundColor : null,
      commentsShadow: comments ? getComputedStyle(comments).boxShadow : null } }
})()`)

const waitForStableLayout = async () => {
  let previous = ''
  let stableFrames = 0
  for (let attempt = 0; attempt < 32; attempt++) {
    const current = JSON.stringify(await snapshot())
    stableFrames = current === previous ? stableFrames + 1 : 0
    if (stableFrames >= 3) return
    previous = current
    await delay(150)
  }
  throw new Error(`Layout did not settle (including count-up animation): ${currentRoute}`)
}

// This test isolates skin geometry, so use identical copy for both theme
// measurements. Wuxia now intentionally changes labels and prompts. The separate
// wuxia-pages-qa/verify-wuxia-pages regression compares real before/after copy,
// including its wrapping and page geometry, without this normalization.
const readCopy = () => evaluate(`(() => {
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
  const texts = []
  while (walker.nextNode()) {
    if (!walker.currentNode.parentElement.closest('script,style') && walker.currentNode.data.trim()) texts.push(walker.currentNode.data)
  }
  return texts
})()`)
const normalizeCopy = (copy) => evaluate(`(() => {
  const copy = ${JSON.stringify(copy)}
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
  const nodes = []
  while (walker.nextNode()) {
    if (!walker.currentNode.parentElement.closest('script,style') && walker.currentNode.data.trim()) nodes.push(walker.currentNode)
  }
  if (nodes.length !== copy.length) throw new Error('Theme changed text node structure')
  nodes.forEach((node,index) => { node.data = copy[index] })
})()`)

// Validate the loaded face without modifying the rendered geometry.
const checkTypeface = () => evaluate(`(() => {
  const home = !!document.querySelector('.home-sky')
  const family = home ? 'Zhi Mang Xing' : 'Ma Shan Zheng'
  const selectors = home
    ? '.home-sky :is(.hero-title,.title-accent,.hero-desc)'
    : '.app-root[data-wuxia-page] :is(.hero-title,.title-accent,.section-title,.sky-page-head h1,.ha-title,.hf-title,.hg-title,.hs-title,.cta-title)'
  const copy = [...document.querySelectorAll(selectors)]
  const fontLoaded = !copy.length || [...document.fonts].some(font => font.family === family && font.status === 'loaded')
  const fontPass = copy.every(el => getComputedStyle(el).fontFamily.includes(family))
  return { pass: fontLoaded && fontPass, fontLoaded, fontPass, family, checkedCopy: copy.length }
})()`)

const routes = ['/', '/articles', '/articles/layout-1', '/categories/dev', '/tags/vue', '/projects', '/tools', '/friends', '/guestbook', '/about', '/archives', '/search?kw=布局']
const viewports = [[1440, 900], [768, 1024], [390, 844]]
const results = []
try {
  await call('Page.enable')
  await call('Runtime.enable')
  await call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] })
  await call('Fetch.enable', { patterns: [{ urlPattern: '*' }] })
  await call('Page.addScriptToEvaluateOnNewDocument', { source: `
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition: (ok, fail) => fail({code: 1}) } });
  ` })
  for (const [width, height] of viewports) {
    await call('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 600 })
    for (const route of routes) {
      currentRoute = route
      await call('Page.navigate', { url: origin + route })
      await waitFor(`!!document.querySelector('.front-main > div') && !!document.querySelector('.nav-inner')`)
      await evaluate(`(() => { const style = document.createElement('style'); style.textContent = '* ,*::before,*::after{animation:none!important;transition:none!important}.reveal,.ef-rise{opacity:1!important;transform:none!important}'; document.head.appendChild(style); })()`)
      await delay(600)
      await evaluate('document.fonts.ready.then(() => true)')
      await waitForStableLayout()
      const states = {}
      let sunnyCopy
      let inkTypography
      for (const theme of ['sunny', 'ink']) {
        await evaluate(`import('/src/stores/theme.js').then(({useThemeStore}) => {useThemeStore().setTheme('${theme}'); return new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))})`)
        await evaluate('document.fonts.ready.then(() => true)')
        await waitForStableLayout()
        if (theme === 'sunny') {
          sunnyCopy = await readCopy()
        } else {
          inkTypography = await checkTypeface()
        }
        // Screenshots always show the actual requested font sizes and wrapping.
        if (route === '/') {
          const capture = await call('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
          await writeFile(join(output, `home-${width}-${theme}.png`), Buffer.from(capture.data, 'base64'))
        }
        if (theme === 'ink') {
          if (route !== '/') await normalizeCopy(sunnyCopy)
        }
        // Compare the real layout; never normalize CSS metrics to hide reflow.
        await waitForStableLayout()
        states[theme] = await snapshot()
      }
      const diffs = []
      if (states.sunny.elements.length !== states.ink.elements.length) diffs.push({ property: 'elementCount', sunny: states.sunny.elements.length, ink: states.ink.elements.length })
      states.sunny.elements.forEach((element, index) => {
        const after = states.ink.elements[index]
        if (!after) return
        if (JSON.stringify(element) !== JSON.stringify(after)) diffs.push({ key: element.key, sunny: element, ink: after })
      })
      for (const property of ['scrollHeight','scrollWidth']) {
        if (states.sunny[property] !== states.ink[property]) diffs.push({ property, sunny: states.sunny[property], ink: states.ink[property] })
      }
      const surfaces = states.ink.surfaces
      const surfacePass = (!surfaces.heroPseudo || surfaces.heroPseudo === 'none') && surfaces.headerBackground === 'rgba(0, 0, 0, 0)' && (!surfaces.commentsBackground || surfaces.commentsBackground === 'rgba(0, 0, 0, 0)') && (!surfaces.commentsShadow || surfaces.commentsShadow === 'none')
      const paper = states.ink.homeBackground
      const transparent = 'rgba(0, 0, 0, 0)'
      const backgroundPass = !paper || (
        paper.image.includes('/images/ink-paper-texture-v2.png') && paper.color !== transparent &&
        paper.top === 0 && paper.left === 0 && paper.right === paper.viewportWidth &&
        paper.contentBackground === transparent && paper.contentImage === 'none' &&
        paper.sceneBackground === transparent && paper.paperOpacity === '0' && paper.paperLoaded &&
        states.sunny.homeBackground.image === 'none' &&
        JSON.stringify(paper.artRects) === JSON.stringify(states.sunny.homeBackground.artRects)
      )
      const homeBefore = states.sunny.homeLayout, homeAfter = states.ink.homeLayout
      const homeCompositionPass = !homeAfter || (
        !homeAfter.overlap && homeAfter.noOverflow && homeBefore.noOverflow &&
        homeBefore.copyLeftMargin === '0px' && homeBefore.copyTopMargin === '0px' &&
        homeAfter.copyLeftMargin === '0px' && homeAfter.copyTopMargin === '0px'
      )
      const result = { route, width, comparedElements: states.sunny.elements.length, pass: diffs.length === 0 && inkTypography.pass && homeCompositionPass && surfacePass && backgroundPass, typography: inkTypography, surfacePass, backgroundPass, homeCompositionPass, homeBefore, homeAfter, homeBackground: paper, diffs }
      results.push(result)
      console.log(`${result.pass ? 'PASS' : 'FAIL'} ${width} ${route} (${result.comparedElements} elements, ${diffs.length} diffs)`)
      if (process.env.LAYOUT_QA_DEBUG && diffs.length) console.log(JSON.stringify(diffs.slice(0, 3)))
    }
  }
  const report = { pass: results.every(result => result.pass) && exceptions.length === 0, dataMode: 'deterministic fixtures; no backend writes; actual rendered geometry; only non-home copy matched to sunny; no CSS metric normalization', results, exceptions }
  await writeFile(join(output, 'report.json'), JSON.stringify(report, null, 2))
  console.log(JSON.stringify({ pass: report.pass, cases: results.length, comparedElements: results.reduce((sum, result) => sum + result.comparedElements, 0), exceptions: exceptions.length, output }))
  if (!report.pass) process.exitCode = 1
} finally {
  await call('Browser.close').catch(() => {})
  socket.close()
  for (const item of pending.values()) clearTimeout(item.timeout)
}
