import { writeFile } from 'node:fs/promises'

const [, , websocketUrl, outputPrefix, viewportWidth = '1894', viewportHeight = '953'] = process.argv
if (!websocketUrl || !outputPrefix) {
  throw new Error('Usage: node edge-theme-layout-qa.mjs <websocket-url> <output-prefix> [width] [height]')
}

const socket = new WebSocket(websocketUrl)
const pending = new Map()
const events = []
let nextId = 1

const call = (method, params = {}) => new Promise((resolve, reject) => {
  const id = nextId++
  pending.set(id, { resolve, reject })
  socket.send(JSON.stringify({ id, method, params }))
})

socket.addEventListener('message', (event) => {
  const message = JSON.parse(event.data)
  if (message.id) {
    const request = pending.get(message.id)
    if (!request) return
    pending.delete(message.id)
    if (message.error) request.reject(new Error(message.error.message))
    else request.resolve(message.result)
    return
  }

  if (message.method === 'Runtime.exceptionThrown') {
    events.push({ type: 'exception', text: message.params?.exceptionDetails?.text || 'Unknown exception' })
  }
  if (message.method === 'Log.entryAdded' && ['error', 'warning'].includes(message.params?.entry?.level)) {
    events.push({ type: message.params.entry.level, text: message.params.entry.text })
  }
})

await new Promise((resolve, reject) => {
  socket.addEventListener('open', resolve, { once: true })
  socket.addEventListener('error', reject, { once: true })
})

await call('Page.enable')
await call('Runtime.enable')
await call('Log.enable')
await call('Emulation.setDeviceMetricsOverride', {
  width: Number(viewportWidth),
  height: Number(viewportHeight),
  deviceScaleFactor: 1,
  mobile: Number(viewportWidth) <= 600,
})
await call('Page.navigate', { url: 'http://localhost:5173/' })
await call('Runtime.evaluate', {
  expression: `new Promise((resolve) => {
    const startedAt = Date.now()
    const check = () => {
      if (document.querySelector('.sky-hero') && document.querySelector('.nav-inner')) return resolve(true)
      if (Date.now() - startedAt > 45000) return resolve(false)
      setTimeout(check, 250)
    }
    check()
  })`,
  awaitPromise: true,
  returnByValue: true,
})

await call('Runtime.evaluate', {
  expression: `Promise.all([
    document.fonts?.ready,
    ...[...document.images].map((image) => image.decode?.().catch(() => undefined))
  ])`,
  awaitPromise: true,
})

await call('Runtime.evaluate', {
  expression: `(() => {
    const style = document.createElement('style')
    style.dataset.layoutQa = 'true'
    style.textContent = '*,*::before,*::after{animation:none!important;transition:none!important}'
    document.head.appendChild(style)
  })()`,
})

const settle = async () => {
  await call('Runtime.evaluate', {
    expression: `new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))`,
    awaitPromise: true,
  })
}

const applyTheme = async (themeId) => {
  await call('Runtime.evaluate', {
    expression: `import('/src/themes/registry.js').then(({ applyTheme }) => {
      applyTheme(${JSON.stringify(themeId)}, { persist: false, announce: false })
      return document.fonts?.ready
    })`,
    awaitPromise: true,
  })
  await settle()
}

const selectors = [
  'html', 'body', '#app', '.front-layout',
  '.app-header', '.nav-inner', '.brand', '.brand-mark', '.brand-text',
  '.nav-menu', '.nav-item', '.nav-tools', '.search-box',
  '.sky-hero', '.hero-grid', '.hero-text', '.hero-eyebrow', '.hero-title',
  '.title-main', '.title-accent', '.hero-desc', '.hero-actions',
  '.weather-card', '.weather-head', '.weather-temp', '.weather-divider',
  '.weather-grid', '.weather-quote',
  '.home-sky', '.home-sky .section', '.section-head', '.section-title',
  '.post-grid', '.post-card-sky', '.tag-cloud-card', '.cta-grid', '.cta-card',
]

const snapshotExpression = `(() => {
  const selectors = ${JSON.stringify(selectors)}
  const properties = [
    'display', 'position', 'boxSizing', 'width', 'height', 'minWidth', 'maxWidth',
    'minHeight', 'maxHeight', 'marginTop', 'marginRight', 'marginBottom', 'marginLeft',
    'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft',
    'borderTopWidth', 'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth',
    'gap', 'rowGap', 'columnGap', 'gridTemplateColumns', 'gridTemplateRows',
    'gridAutoFlow', 'alignItems', 'alignContent', 'justifyItems', 'justifyContent',
    'flexBasis', 'flexGrow', 'flexShrink', 'flexDirection', 'flexWrap',
    'overflowX', 'overflowY', 'transform', 'fontFamily', 'fontSize', 'fontWeight',
    'lineHeight', 'letterSpacing', 'whiteSpace', 'textAlign'
  ]
  const elements = {}
  for (const selector of selectors) {
    document.querySelectorAll(selector).forEach((element, index) => {
      const rect = element.getBoundingClientRect()
      const style = getComputedStyle(element)
      const computed = Object.fromEntries(properties.map((property) => [property, style[property]]))
      elements[selector + '::' + index] = {
        rect: Object.fromEntries(['x', 'y', 'width', 'height', 'top', 'right', 'bottom', 'left']
          .map((key) => [key, Number(rect[key].toFixed(3))])),
        clientWidth: element.clientWidth,
        clientHeight: element.clientHeight,
        scrollWidth: element.scrollWidth,
        scrollHeight: element.scrollHeight,
        computed,
      }
    })
  }
  return {
    theme: document.documentElement.dataset.theme,
    viewport: { width: innerWidth, height: innerHeight },
    page: {
      scrollWidth: document.documentElement.scrollWidth,
      scrollHeight: document.documentElement.scrollHeight,
      horizontalOverflow: document.documentElement.scrollWidth > innerWidth,
    },
    elements,
  }
})()`

const snapshot = async () => {
  const result = await call('Runtime.evaluate', { expression: snapshotExpression, returnByValue: true })
  return result.result.value
}

const screenshot = async (themeId) => {
  const capture = await call('Page.captureScreenshot', {
    format: 'png',
    fromSurface: true,
    captureBeyondViewport: false,
  })
  const path = `${outputPrefix}-${themeId}.png`
  await writeFile(path, Buffer.from(capture.data, 'base64'))
  return path
}

await applyTheme('sunny')
const sunny = await snapshot()
const sunnyScreenshot = await screenshot('sunny')

await applyTheme('ink')
const ink = await snapshot()
const inkScreenshot = await screenshot('ink')

const numericDiffs = []
const computedDiffs = []
const allKeys = new Set([...Object.keys(sunny.elements), ...Object.keys(ink.elements)])
for (const key of allKeys) {
  const before = sunny.elements[key]
  const after = ink.elements[key]
  if (!before || !after) {
    computedDiffs.push({ key, property: 'existence', sunny: Boolean(before), ink: Boolean(after) })
    continue
  }
  for (const property of Object.keys(before.rect)) {
    const delta = Number(Math.abs(before.rect[property] - after.rect[property]).toFixed(3))
    if (delta !== 0) numericDiffs.push({ key, property: `rect.${property}`, sunny: before.rect[property], ink: after.rect[property], delta })
  }
  for (const property of ['clientWidth', 'clientHeight', 'scrollWidth', 'scrollHeight']) {
    const delta = Math.abs(before[property] - after[property])
    if (delta !== 0) numericDiffs.push({ key, property, sunny: before[property], ink: after[property], delta })
  }
  for (const [property, value] of Object.entries(before.computed)) {
    if (value !== after.computed[property]) {
      computedDiffs.push({ key, property, sunny: value, ink: after.computed[property] })
    }
  }
}

const pageDiffs = Object.fromEntries(
  ['scrollWidth', 'scrollHeight', 'horizontalOverflow']
    .filter((property) => sunny.page[property] !== ink.page[property])
    .map((property) => [property, { sunny: sunny.page[property], ink: ink.page[property] }]),
)
const maxRectDelta = numericDiffs.reduce((max, item) => Math.max(max, item.delta), 0)
const report = {
  pass: numericDiffs.length === 0 && computedDiffs.length === 0 && Object.keys(pageDiffs).length === 0,
  viewport: sunny.viewport,
  comparedElements: allKeys.size,
  maxRectDelta,
  numericDiffs,
  computedDiffs,
  pageDiffs,
  page: { sunny: sunny.page, ink: ink.page },
  events,
  screenshots: { sunny: sunnyScreenshot, ink: inkScreenshot },
}

await writeFile(`${outputPrefix}-report.json`, JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
socket.close()

if (!report.pass) process.exitCode = 1
