/**
 * v-magnetic — 按钮/链接磁吸：鼠标靠近时该元素略微跟向光标，离开时弹回原位。
 *
 * 指令会在元素被插入 DOM 时（含 v-if 切换显隐）自动
 * 挂载监听，因此适配「数据加载后条件渲染」的按钮。
 *
 * 用法：
 *   <button v-magnetic>申请友链</button>
 *   <button v-magnetic="{ strength: 6 }">强磁吸</button>
 *
 * 注意：该指令通过 inline style 写入 transform: translate(...)，会覆盖元素上
 * 由 CSS :hover 设置的 transform（如 translateY(-1px)）。若需叠加 hover 位移，
 * 请在元素上改用 box-shadow / 缩放类，避免与 translate 类冲突。
 */
export default {
  mounted(el, binding) {
    const strength = binding.value?.strength ?? 4 // 最大位移 px
    let raf = null

    const onMove = (e) => {
      const rect = el.getBoundingClientRect()
      const cx = rect.left + rect.width / 2
      const cy = rect.top + rect.height / 2
      const dx = ((e.clientX - cx) / (rect.width / 2)) * strength
      const dy = ((e.clientY - cy) / (rect.height / 2)) * strength
      if (raf) cancelAnimationFrame(raf)
      raf = requestAnimationFrame(() => {
        el.style.transform = `translate(${dx.toFixed(2)}px, ${dy.toFixed(2)}px)`
      })
    }
    const onLeave = () => {
      if (raf) cancelAnimationFrame(raf)
      el.style.transform = 'translate(0, 0)'
    }

    // 让 hover 过渡更顺滑
    el.style.willChange = 'transform'
    if (!el.style.transition.includes('transform')) {
      el.style.transition = 'transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), ' +
        (el.style.transition || '')
    }

    el.addEventListener('mousemove', onMove, { passive: true })
    el.addEventListener('mouseleave', onLeave)
    el._magneticCleanup = () => {
      el.removeEventListener('mousemove', onMove)
      el.removeEventListener('mouseleave', onLeave)
    }
  },
  unmounted(el) {
    el._magneticCleanup?.()
  }
}
