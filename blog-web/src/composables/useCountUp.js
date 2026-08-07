/**
 * useCountUp — 数字滚动计数（easeOutCubic）
 *
 * 仅当目标值是数字时才做动画；非数字（如 '2.4k'）直接原样显示。
 * 尊重系统「减弱动效」：直接落到终值，不播放动画。
 * 支持 restart(newTarget) 供外部在 props 变化时重新触发动画。
 *
 * 用法（在组件 setup 中）：
 *   const { display, restart } = useCountUp(87, { duration: 1400, decimals: 0 })
 *   // 模板：<span>{{ display }}</span>
 *   // 值变化时调用 restart(100) 重新滚动到新值
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

function formatNumber(n, decimals) {
  const fixed = Number(n).toFixed(decimals)
  const [int, dec] = fixed.split('.')
  // 千分位分隔
  const withSep = int.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return dec ? `${withSep}.${dec}` : withSep
}

export function useCountUp(target, options = {}) {
  const duration = options.duration ?? 1400
  const decimals = options.decimals ?? 0
  const display = ref(formatNumber(0, decimals))
  let raf = null

  let to = Number(target)
  let isNumeric = !Number.isNaN(to)

  const run = (startFromValue) => {
    // 取消上一轮动画
    if (raf) cancelAnimationFrame(raf)

    if (startFromValue !== undefined) {
      to = Number(startFromValue)
      isNumeric = !Number.isNaN(to)
    }

    // 非数字：直接显示原值
    if (!isNumeric) {
      display.value = String(to)
      return
    }
    // 系统偏好：减弱动效
    const reduced =
      typeof window !== 'undefined' &&
      (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false)
    if (reduced) {
      display.value = formatNumber(to, decimals)
      return
    }
    const start = performance.now()
    const from = 0
    const tick = (now) => {
      const p = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - p, 3) // easeOutCubic
      display.value = formatNumber(from + (to - from) * eased, decimals)
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
  }

  onMounted(() => run())
  onBeforeUnmount(() => {
    if (raf) cancelAnimationFrame(raf)
  })

  // 外部可在 props 变化时调用 restart(newTarget) 重启动画
  const restart = (newTarget) => run(newTarget)

  return { display, restart }
}
