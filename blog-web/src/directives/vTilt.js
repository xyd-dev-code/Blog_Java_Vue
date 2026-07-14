/**
 * 卡片 3D 倾斜效果
 * 用法: <div v-tilt class="card">...</div>
 * 或: <div ref="el" @mousemove="onTiltMove" @mouseleave="onTiltLeave">
 */
export default {
  mounted(el) {
    let raf = null
    const handleMove = (e) => {
      const rect = el.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      const midX = rect.width / 2
      const midY = rect.height / 2
      const rotX = ((y - midY) / midY) * -8
      const rotY = ((x - midX) / midX) * 8
      if (raf) cancelAnimationFrame(raf)
      raf = requestAnimationFrame(() => {
        el.style.transform = `perspective(800px) rotateX(${rotX}deg) rotateY(${rotY}deg) scale3d(1.02, 1.02, 1.02)`
      })
    }
    const handleLeave = () => {
      if (raf) cancelAnimationFrame(raf)
      el.style.transform = 'perspective(800px) rotateX(0) rotateY(0) scale3d(1, 1, 1)'
      el.style.transition = 'transform 0.5s cubic-bezier(0.25, 0.8, 0.25, 1)'
    }
    el.style.transition = 'transform 0.1s ease'
    el.addEventListener('mousemove', handleMove)
    el.addEventListener('mouseleave', handleLeave)
    el._tiltCleanup = () => {
      el.removeEventListener('mousemove', handleMove)
      el.removeEventListener('mouseleave', handleLeave)
    }
  },
  unmounted(el) {
    el._tiltCleanup?.()
  }
}
