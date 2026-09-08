import { onBeforeUnmount, onMounted } from 'vue'

const INTERACTIVE_SELECTOR = [
  'button',
  '.el-button',
  'a.btn',
  '.fh-apply-btn',
  '.share-btn',
  '.use-btn',
].join(',')

/**
 * 在水墨主题下为可点击元素增加一次性墨点反馈。
 * 事件委托避免给每个业务组件绑定监听，也不会改变现有点击逻辑。
 */
export const useInkInteractions = () => {
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)')

  const onPointerDown = (event) => {
    if (document.documentElement.dataset.theme !== 'ink' || reduceMotion.matches) return

    const host = event.target.closest?.(INTERACTIVE_SELECTOR)
    if (!host || host.matches(':disabled, [aria-disabled="true"]')) return
    if (host.closest('[data-ink-feedback="off"]')) return

    const rect = host.getBoundingClientRect()
    const size = Math.max(rect.width, rect.height) * 2.2
    const drop = document.createElement('span')
    drop.className = 'ink-feedback-drop'
    drop.style.width = `${size}px`
    drop.style.height = `${size}px`
    drop.style.left = `${event.clientX - size / 2}px`
    drop.style.top = `${event.clientY - size / 2}px`

    // 固定在视口层播放，避免为了墨点反馈去改宿主元素的
    // position / overflow，从根源上杜绝主题交互造成布局或裁切差异。
    document.body.appendChild(drop)
    drop.addEventListener('animationend', () => drop.remove(), { once: true })
  }

  onMounted(() => document.addEventListener('pointerdown', onPointerDown, { passive: true }))
  onBeforeUnmount(() => document.removeEventListener('pointerdown', onPointerDown))
}
