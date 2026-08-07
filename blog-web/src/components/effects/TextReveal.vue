<template>
  <!--
    逐字浮现 → 柔光扫过（串行播放，避免样式冲突）

    为什么分两个分支渲染：
    .ef-title-shine 依赖 background-clip: text + -webkit-text-fill-color: transparent，
    而后者是可继承属性。若与逐字拆分的 <span> 同时存在，子 span 会继承 transparent
    却没有自己的背景，导致文字整体不可见。因此逐字动画播完后切换为纯文本再上扫光。
  -->
  <component :is="tag" v-if="!done" :class="['ef-title-chars', rootClass]" :aria-label="text">
    <span
      v-for="(ch, i) in chars"
      :key="i"
      :class="{ 'is-space': ch === ' ' }"
      :style="{ animationDelay: `${delay + i * stagger}s` }"
      aria-hidden="true"
    >{{ ch === ' ' ? '\u00A0' : ch }}</span>
  </component>

  <component :is="tag" v-else :class="[rootClass, shine && !reduced ? 'ef-title-shine' : null]">{{ text }}</component>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  text: { type: String, required: true },
  tag: { type: String, default: 'h1' },
  // 附加在根元素上的类名（用于沿用页面原有标题样式）
  rootClass: { type: String, default: '' },
  // 首字延迟（秒）
  delay: { type: Number, default: 0.04 },
  // 每字间隔（秒）
  stagger: { type: Number, default: 0.025 },
  // 逐字播完后是否接柔光扫过
  shine: { type: Boolean, default: true }
})

const done = ref(false)
const reduced = ref(false)
let timer = null

const chars = computed(() => Array.from(props.text))

// 单字动画时长 0.5s，与 effects.scss 的 ef-char-in 保持一致（淡入 + 上移 0.5em + 轻微缩放）
const CHAR_DURATION = 0.5

const start = () => {
  if (timer) clearTimeout(timer)
  done.value = false

  // 尊重系统「减弱动效」：直接落到终态，逐字与扫光都不播放
  reduced.value =
    typeof window !== 'undefined' &&
    (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false)
  if (reduced.value) {
    done.value = true
    return
  }

  const total = (props.delay + chars.value.length * props.stagger + CHAR_DURATION) * 1000
  timer = setTimeout(() => { done.value = true }, total)
}

onMounted(start)
watch(() => props.text, start)
onBeforeUnmount(() => { if (timer) clearTimeout(timer) })
</script>
