import { readFile, writeFile } from 'node:fs/promises'

const [, , websocketUrl, sourcePath, implementationPath, outputPath] = process.argv
if (!websocketUrl || !sourcePath || !implementationPath || !outputPath) {
  throw new Error('Usage: node edge-cdp-comparison.mjs <websocket-url> <source> <implementation> <output>')
}

const [source, implementation] = await Promise.all([
  readFile(sourcePath),
  readFile(implementationPath),
])

const socket = new WebSocket(websocketUrl)
const pending = new Map()
let nextId = 1

const call = (method, params = {}) => new Promise((resolve, reject) => {
  const id = nextId++
  pending.set(id, { resolve, reject })
  socket.send(JSON.stringify({ id, method, params }))
})

socket.addEventListener('message', (event) => {
  const message = JSON.parse(event.data)
  if (!message.id) return
  const request = pending.get(message.id)
  if (!request) return
  pending.delete(message.id)
  if (message.error) request.reject(new Error(message.error.message))
  else request.resolve(message.result)
})

await new Promise((resolve, reject) => {
  socket.addEventListener('open', resolve, { once: true })
  socket.addEventListener('error', reject, { once: true })
})

await call('Page.enable')
await call('Page.navigate', { url: 'about:blank' })

const html = `<!doctype html>
<html lang="zh-CN">
<meta charset="utf-8">
<style>
  * { box-sizing: border-box; }
  html, body { margin: 0; min-height: 100%; background: #1b1c1d; color: #f4efe6; }
  body { padding: 42px 48px; font-family: "Microsoft YaHei", sans-serif; }
  h1 { margin: 0 0 26px; font: 600 22px/1.2 "Microsoft YaHei", sans-serif; letter-spacing: .08em; }
  .compare { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; }
  figure { margin: 0; padding: 12px; background: #292a2b; border: 1px solid #49494a; border-radius: 10px; }
  figcaption { padding: 2px 4px 12px; font-size: 15px; color: #d9d2c7; }
  img { display: block; width: 100%; aspect-ratio: 16 / 9; object-fit: cover; object-position: center top; border-radius: 5px; background: #eee8dc; }
  .note { margin: 22px 2px 0; color: #aaa49a; font-size: 13px; }
</style>
<body>
  <h1>国风水墨首页 · 视觉语言对照</h1>
  <section class="compare">
    <figure><figcaption>参考图：取色、层次、构图气势</figcaption><img src="data:image/jpeg;base64,${source.toString('base64')}"></figure>
    <figure><figcaption>实现页：拆解为纸纹、远山、枝叶与真实 UI</figcaption><img src="data:image/png;base64,${implementation.toString('base64')}"></figure>
  </section>
  <p class="note">等比例统一至 16:9 画框，用于判断视觉语言；不以逐像素复制参考画面为目标。</p>
</body>
</html>`

const frameTree = await call('Page.getFrameTree')
await call('Page.setDocumentContent', {
  frameId: frameTree.frameTree.frame.id,
  html,
})
await new Promise((resolve) => setTimeout(resolve, 1200))

const capture = await call('Page.captureScreenshot', {
  format: 'png',
  fromSurface: true,
  captureBeyondViewport: false,
})
await writeFile(outputPath, Buffer.from(capture.data, 'base64'))
console.log(outputPath)
socket.close()
