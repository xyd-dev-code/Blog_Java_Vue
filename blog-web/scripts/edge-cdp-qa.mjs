import { writeFile } from 'node:fs/promises'

const [, , websocketUrl, screenshotPath, viewportWidth, viewportHeight] = process.argv
if (!websocketUrl || !screenshotPath) {
  throw new Error('Usage: node edge-cdp-qa.mjs <websocket-url> <screenshot-path>')
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
if (viewportWidth && viewportHeight) {
  await call('Emulation.setDeviceMetricsOverride', {
    width: Number(viewportWidth),
    height: Number(viewportHeight),
    deviceScaleFactor: 1,
    mobile: Number(viewportWidth) <= 600,
  })
}
await call('Page.navigate', { url: 'http://localhost:5173/' })
await call('Runtime.evaluate', {
  expression: `new Promise((resolve) => {
    const startedAt = Date.now()
    const check = () => {
      if (document.querySelector('.sky-hero')) return resolve(true)
      if (Date.now() - startedAt > 45000) return resolve(false)
      setTimeout(check, 500)
    }
    check()
  })`,
  awaitPromise: true,
  returnByValue: true,
})
await call('Runtime.evaluate', {
  expression: `Promise.all(
    [...document.querySelectorAll('.ink-hero-scene img')].map((image) =>
      image.decode ? image.decode().catch(() => undefined) : Promise.resolve()
    )
  )`,
  awaitPromise: true,
})
await new Promise((resolve) => setTimeout(resolve, 1600))

const expression = `(() => {
  const root = document.documentElement
  const body = document.body
  const hero = document.querySelector('.sky-hero')
  const grid = document.querySelector('.hero-grid')
  const titleCard = document.querySelector('.hero-text')
  const weatherCard = document.querySelector('.weather-card')
  const nav = document.querySelector('.nav-inner')
  const style = getComputedStyle(root)
  const rect = root.getBoundingClientRect()
  const images = [...document.querySelectorAll('.ink-hero-scene img')]
  return {
    theme: root.dataset.theme,
    viewport: {
      innerWidth: window.innerWidth,
      innerHeight: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio,
    },
    html: {
      display: style.display,
      visibility: style.visibility,
      opacity: style.opacity,
      width: rect.width,
      height: rect.height,
    },
    body: { width: body.scrollWidth, height: body.scrollHeight },
    hero: hero ? hero.getBoundingClientRect().toJSON() : null,
    layout: {
      nav: nav ? nav.getBoundingClientRect().toJSON() : null,
      grid: grid ? grid.getBoundingClientRect().toJSON() : null,
      titleCard: titleCard ? titleCard.getBoundingClientRect().toJSON() : null,
      weatherCard: weatherCard ? weatherCard.getBoundingClientRect().toJSON() : null,
      horizontalOverflow: body.scrollWidth > window.innerWidth,
    },
    images: images.map((image) => ({
      src: image.getAttribute('src'),
      complete: image.complete,
      naturalWidth: image.naturalWidth,
      naturalHeight: image.naturalHeight,
      display: getComputedStyle(image).display,
      opacity: getComputedStyle(image).opacity,
    })),
  }
})()`

const evaluated = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
const capture = await call('Page.captureScreenshot', {
  format: 'png',
  fromSurface: true,
  captureBeyondViewport: false,
})
await writeFile(screenshotPath, Buffer.from(capture.data, 'base64'))

await call('Runtime.evaluate', {
  expression: "document.querySelector('.hero-actions .el-button--primary')?.click()",
})
const interaction = await call('Runtime.evaluate', {
  expression: `new Promise((resolve) => {
    const startedAt = Date.now()
    const check = () => {
      if (location.pathname === '/articles' || Date.now() - startedAt > 20000) {
        return resolve({ pathname: location.pathname, title: document.title })
      }
      setTimeout(check, 300)
    }
    check()
  })`,
  awaitPromise: true,
  returnByValue: true,
})
await call('Page.navigate', { url: 'http://localhost:5173/' })

console.log(JSON.stringify({
  state: evaluated.result.value,
  interaction: interaction.result.value,
  events,
  screenshotPath,
}, null, 2))
socket.close()
