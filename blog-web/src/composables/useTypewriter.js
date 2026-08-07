/**
 * useTypewriter — 打字机效果：逐字打出文本，打完回调。
 *
 * 用法：
 *   const { displayText } = useTypewriter('在代码与文字之间', { speed: 80, delay: 400 })
 *   <span>{{ displayText }}</span>
 */

import { ref, onMounted, onBeforeUnmount } from 'vue'

export function useTypewriter(text, options = {}) {
  const speed = options.speed ?? 60       // 每字间隔 ms
  const delay = options.delay ?? 300      // 开打前延时 ms
  const cursor = options.cursor ?? true   // 是否显示闪烁光标
  const displayText = ref('')
  if (cursor) displayText.value = '\u200b'  // 零宽空格占位

  let timer = null, idx = 0, chars = []
  let blinkTimer = null, showCursor = false

  const start = () => {
    chars = [...text]
    idx = 0
    if (delay > 0) {
      timer = setTimeout(tick, delay)
    } else {
      tick()
    }
    if (cursor) {
      blinkTimer = setInterval(() => {
        showCursor = !showCursor
        displayText.value = chars.slice(0, idx).join('') + (showCursor ? '|' : '\u200b')
      }, 530)
    }
  }

  const tick = () => {
    if (idx < chars.length) {
      const chunk = chars[idx]
      displayText.value = chars.slice(0, idx + 1).join('') + (cursor && showCursor ? '|' : '')
      idx++
      timer = setTimeout(tick, speed)
    } else {
      // 打完：去掉光标，保持文本
      if (blinkTimer) { clearInterval(blinkTimer); blinkTimer = null }
      displayText.value = text
    }
  }

  onMounted(() => start())
  onBeforeUnmount(() => {
    clearTimeout(timer)
    if (blinkTimer) clearInterval(blinkTimer)
  })

  return { displayText }
}
