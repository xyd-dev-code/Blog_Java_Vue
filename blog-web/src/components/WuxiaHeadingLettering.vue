<template>
  <span v-if="lettering" class="wuxia-heading-lettering" :class="{ 'is-ready': ready }">
    <!-- The real text determines size and remains selectable and accessible. -->
    <span :class="{ 'lettering-source': ready }">{{ text }}</span>
    <svg v-if="ready" class="heading-lettering-art" :viewBox="lettering" preserveAspectRatio="xMidYMid meet" aria-hidden="true" focusable="false">
      <defs>
        <filter :id="filterId" x="0" y="0" width="1254" height="1254" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
          <feColorMatrix type="matrix" values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 -1 0 0 1" />
          <feComponentTransfer><feFuncA type="linear" slope="1.05" intercept="-0.05" /></feComponentTransfer>
        </filter>
        <mask :id="maskId" x="0" y="0" width="1254" height="1254" maskUnits="userSpaceOnUse" style="mask-type: alpha">
          <image :href="letteringUrl" width="1254" height="1254" :filter="`url(#${filterId})`" @error="ready = false" />
        </mask>
      </defs>
      <rect width="1254" height="1254" fill="currentColor" :mask="`url(#${maskId})`" />
    </svg>
  </span>
  <template v-else>{{ text }}</template>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'

const props = defineProps({ text: { type: String, required: true } })
const theme = useThemeStore()
// Only exact, static copy has artwork. Dynamic names never display a stale title.
const headings = {
  '所有文章': '67 52 507 189',
  '我的项目': '674 67 510 160',
  '在线工具箱': '46 278 539 146',
  '友链': '750 261 388 171',
  '留言板': '103 479 466 160',
  '关于我': '712 479 430 170',
  '文章归档': '65 694 518 176',
  '搜索结果': '668 699 546 150',
  '精选文章': '71 897 489 165',
  '最新文章': '686 901 489 164',
  '常常会写到的话题': '54 1096 576 120',
  '页面找不到了': '702 1108 484 105',
}
const lettering = computed(() => theme.activeThemeId === 'ink' ? headings[props.text] : undefined)
const letteringUrl = '/images/wuxia-page-lettering-v2.png'
const id = useId()
const filterId = `heading-ink-${id}`
const maskId = `heading-mask-${id}`
const ready = ref(false)
let preloader
let stopWatching

onMounted(() => {
  stopWatching = watch(lettering, (value) => {
    if (!value || preloader) return
    preloader = new Image()
    preloader.decoding = 'async'
    preloader.onload = () => { ready.value = true }
    preloader.onerror = () => { ready.value = false }
    preloader.src = letteringUrl
  }, { immediate: true })
})
onBeforeUnmount(() => {
  stopWatching?.()
  if (preloader) preloader.onload = preloader.onerror = null
})
</script>

<style scoped>
.wuxia-heading-lettering {
  display: inline-block;
  max-width: 100%;
}
.is-ready { position: relative; }
.lettering-source { opacity: 0; }
.heading-lettering-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
