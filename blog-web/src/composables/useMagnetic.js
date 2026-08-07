/**
 * useMagnetic — 按钮/链接磁吸：鼠标靠近时该元素略微跟过去，离开时弹回原位。
 *
 * 用法：
 *   const { el } = useMagnetic({ strength: 6 })     // stronger pull = larger displacement
 *   <button ref="el" class="btn-mag">Hover me</button>
 *
 * 样式：
 *   .btn-mag { transition: transform 0.3s cubic-bezier(0.25,0.8,0.25,1); will-change: transform; }
 */

import { ref, onMounted, onBeforeUnmount } from 'vue'

export function useMagnetic(options = {}) {
  const strength = options.strength ?? 4    // px max displacement
  const el = ref(null)
  let node = null

  const onMove = (e) => {
    if (!node) return
    const rect = node.getBoundingClientRect()
    const cx = rect.left + rect.width / 2
    const cy = rect.top + rect.height / 2
    const dx = (e.clientX - cx) / (rect.width  / 2) * strength
    const dy = (e.clientY - cy) / (rect.height / 2) * strength
    node.style.transform = `translate(${dx.toFixed(2)}px, ${dy.toFixed(2)}px)`
  }

  const onLeave = () => {
    if (node) node.style.transform = 'translate(0, 0)'
  }

  onMounted(() => {
    node = el.value
    if (!node) return
    node.addEventListener('mousemove', onMove, { passive: true })
    node.addEventListener('mouseleave', onLeave)
  })

  onBeforeUnmount(() => {
    node?.removeEventListener('mousemove', onMove)
    node?.removeEventListener('mouseleave', onLeave)
  })

  return { el }
}
