import { onMounted, onBeforeUnmount, nextTick } from 'vue'

/**
 * 首屏折痕对齐（fold fit）
 *
 * 目标：进入页面时，内容区「第一行卡片」完整落在折痕（视口下沿）之上，
 *      而第二行被推到折痕之下、不露头。
 *
 * 做法：测量第一行最后一张卡片的文档坐标底边，与「视口高度 - 安全余量」比较，
 *      把差值补到 hero 的 min-height 上。写死 padding 只对某一种屏高成立，
 *      这里改成运行时一次线性求解，随屏高 / 字号 / 内容自适应。
 *
 * @param {object} opts
 * @param {string} opts.hero      hero 容器选择器（会被撑高/压扁的那一块）
 * @param {string} opts.item      卡片选择器（取第一行的若干张）
 * @param {number} opts.min       hero 最小高度（默认 140）
 * @param {number} opts.max       hero 最大高度（默认 560，防止超高屏上 hero 空得离谱）
 * @param {number} opts.pad       折痕安全余量，需小于卡片行间距（默认 10）
 * @returns {{ refit: () => void }} 数据异步就绪后手动调用 refit() 重算
 */
export function useFoldFit(opts = {}) {
  const {
    hero: heroSel,
    item: itemSel,
    min = 140,
    max = 560,
    pad = 10,
    minViewport = 640,
    minWidth = 769
  } = opts

  let raf = 0
  let disposed = false

  // 文档坐标下的 top（不受 transform 影响）
  const docTop = (el) => {
    let y = 0
    let node = el
    while (node) {
      y += node.offsetTop
      node = node.offsetParent
    }
    return y
  }

  const apply = () => {
    if (disposed) return
    const heroEl = document.querySelector(heroSel)
    if (!heroEl) return

    // 窄屏 / 矮屏不参与：移动端按正常文档流排版更合理
    if (window.innerWidth < minWidth || window.innerHeight < minViewport) {
      heroEl.style.minHeight = ''
      return
    }

    const items = document.querySelectorAll(itemSel)
    if (!items.length) {
      heroEl.style.minHeight = ''
      return
    }

    // 先还原自然高度再测量，避免上一次的结果参与本次计算
    heroEl.style.minHeight = ''
    const heroH = heroEl.offsetHeight

    // 卡片带 .reveal 入场动画（translateY），getBoundingClientRect 会量到位移后的位置，
    // 因此一律走 offsetTop/offsetHeight —— 它们不受 transform 影响
    const firstTop = docTop(items[0])
    let rowBottom = firstTop + items[0].offsetHeight
    for (const el of items) {
      const top = docTop(el)
      if (Math.abs(top - firstTop) > 2) break   // 已经换行，第一行到此为止
      const bottom = top + el.offsetHeight
      if (bottom > rowBottom) rowBottom = bottom
    }

    // 折痕位于文档 y = innerHeight（滚动到顶时）
    const delta = (window.innerHeight - pad) - rowBottom
    const target = Math.round(Math.min(max, Math.max(min, heroH + delta)))

    // 差距太小就别改，省一次重排
    if (Math.abs(target - heroH) < 2) return
    heroEl.style.minHeight = `${target}px`
  }

  const schedule = () => {
    cancelAnimationFrame(raf)
    raf = requestAnimationFrame(() => nextTick(apply))
  }

  onMounted(() => {
    schedule()
    // 字体加载完成后行高会变，重算一次
    if (document.fonts?.ready) document.fonts.ready.then(schedule).catch(() => {})
    window.addEventListener('resize', schedule)
  })

  onBeforeUnmount(() => {
    disposed = true
    cancelAnimationFrame(raf)
    window.removeEventListener('resize', schedule)
    const heroEl = document.querySelector(heroSel)
    if (heroEl) heroEl.style.minHeight = ''
  })

  return { refit: schedule }
}
