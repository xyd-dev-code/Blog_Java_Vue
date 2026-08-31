/**
 * use3DTilt — 给任意卡片加 3D 倾斜 + 光晕效果
 *
 * 用法：在卡片容器上绑定 ref 或 class，然后在组件里
 *   import { use3DTilt } from '@/composables/use3DTilt'
 *   const { el } = use3DTilt()
 *   // 模板里: <div ref="el" class="card tilt-card">...
 *
 * 样式配合（加到该组件的 scoped style）：
 *   .tilt-card { transform-style: preserve-3d; perspective: 800px; }
 *   .tilt-card :deep(.tilt-glow) {
 *     position: absolute; inset: 0; border-radius: inherit; pointer-events: none;
 *     background: radial-gradient(circle at var(--mx,50%) var(--my,50%), rgba(var(--theme-primary-rgb),.12), transparent 70%);
 *     opacity: 0; transition: opacity .3s;
 *   }
 *   .tilt-card:hover :deep(.tilt-glow) { opacity: 1; }
 */

import { ref, onMounted, onBeforeUnmount } from 'vue'

export function use3DTilt(options = {}) {
  const maxTilt = options.maxTilt ?? 12        // 最大倾斜角度（度）
  const speed = options.speed ?? 400           // 回正过渡毫秒
  const el = ref(null)
  let card = null

  const onMove = (e) => {
    if (!card) return
    const rect = card.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const cx = x / rect.width  - 0.5    // -0.5 .. 0.5
    const cy = y / rect.height - 0.5
    card.style.setProperty('--mx', `${(cx + 0.5) * 100}%`)
    card.style.setProperty('--my', `${(cy + 0.5) * 100}%`)
    card.style.setProperty('--rx', `${(-cy * maxTilt).toFixed(2)}deg`)
    card.style.setProperty('--ry', `${(cx  * maxTilt).toFixed(2)}deg`)
    card.style.setProperty('--tz', '4px')
  }

  const onLeave = () => {
    if (!card) return
    card.style.setProperty('--rx', '0deg')
    card.style.setProperty('--ry', '0deg')
    card.style.setProperty('--tz', '0px')
  }

  onMounted(() => {
    card = el.value
    if (!card) return
    card.style.transition = `transform ${speed}ms ease-out, box-shadow ${speed}ms ease-out`
    card.addEventListener('mousemove', onMove, { passive: true })
    card.addEventListener('mouseleave', onLeave)
  })

  onBeforeUnmount(() => {
    card?.removeEventListener('mousemove', onMove)
    card?.removeEventListener('mouseleave', onLeave)
  })

  return { el }
}
