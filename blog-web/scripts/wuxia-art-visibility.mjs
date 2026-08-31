// Runs in the browser. The figure has empty background inside its rectangular
// canvas, so collisions must use visible ink rather than the canvas rectangle.
export function hasWuxiaForegroundOverlap() {
  const image = document.querySelector('.wuxia-duelist img')
  if (!image?.complete || !image.naturalWidth) return false
  const box = image.getBoundingClientRect()
  const scale = Math.min(box.width / image.naturalWidth, box.height / image.naturalHeight)
  const left = box.left + (box.width - image.naturalWidth * scale) / 2
  const top = box.top + (box.height - image.naturalHeight * scale) / 2
  const canvas = document.createElement('canvas')
  canvas.width = image.naturalWidth
  canvas.height = image.naturalHeight
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  ctx.drawImage(image, 0, 0)
  const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height).data
  const foreground = [...document.querySelectorAll('.hero-eyebrow,.title-main,.title-accent,.hero-desc,.hero-actions button')].flatMap(el => {
    const lettering = el.querySelector('.lettering-art')
    if (lettering) return [lettering.getBoundingClientRect()]
    const rects = [...el.querySelectorAll('svg')].map(icon => icon.getBoundingClientRect())
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT)
    while (walker.nextNode()) {
      if (!walker.currentNode.textContent.trim()) continue
      const range = document.createRange()
      range.selectNodeContents(walker.currentNode)
      rects.push(...range.getClientRects())
    }
    return rects
  })
  foreground.push(document.querySelector('.weather-card').getBoundingClientRect())
  return foreground.some(rect => {
    const x0 = Math.max(0, Math.floor((rect.left - left) / scale))
    const x1 = Math.min(canvas.width, Math.ceil((rect.right - left) / scale))
    const y0 = Math.max(0, Math.floor((rect.top - top) / scale))
    const y1 = Math.min(canvas.height, Math.ceil((rect.bottom - top) / scale))
    for (let y = y0; y < y1; y++) {
      for (let x = x0; x < x1; x++) {
        const i = (y * canvas.width + x) * 4
        if (pixels[i + 3] > 128 && pixels[i] + pixels[i + 1] + pixels[i + 2] < 735) return true
      }
    }
    return false
  })
}
