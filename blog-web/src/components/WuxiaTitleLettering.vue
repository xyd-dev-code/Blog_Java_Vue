<template>
  <span class="wuxia-title-lettering" :class="{ 'is-ready': ready }">
    <!-- Keep real heading text available for assistive technology and asset failures. -->
    <span :class="{ 'lettering-text': ready }">{{ lettering.text }}</span>
    <svg
      v-if="ready"
      class="lettering-art"
      :viewBox="lettering.viewBox"
      preserveAspectRatio="xMinYMid meet"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <!-- Render the approved RGB proof as ink, without a white rectangle or white edge. -->
        <filter :id="filterId" x="0" y="0" width="1536" height="1024" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
          <feColorMatrix type="matrix" values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 -1 0 0 1" />
          <feComponentTransfer>
            <feFuncA type="linear" slope="1.05" intercept="-0.05" />
          </feComponentTransfer>
        </filter>
        <mask :id="maskId" x="0" y="0" width="1536" height="1024" maskUnits="userSpaceOnUse" style="mask-type: alpha">
          <image :href="letteringUrl" width="1536" height="1024" :filter="`url(#${filterId})`" @error="ready = false" />
        </mask>
      </defs>
      <rect width="1536" height="1024" fill="currentColor" :mask="`url(#${maskId})`" />
    </svg>
  </span>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, useId } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'main', validator: (value) => ['main', 'accent'].includes(value) },
})

const variants = {
  main: { text: '拾光小筑', viewBox: '64 132 1428 404' },
  accent: { text: '仗剑天涯', viewBox: '45 538 1444 416' },
}
const lettering = computed(() => variants[props.variant] || variants.main)
const letteringUrl = '/images/wuxia-hero-lettering-v1.png'
const id = useId()
const filterId = `lettering-ink-${id}`
const maskId = `lettering-mask-${id}`
// Reserve the final lettering from the first render; show text only on asset failure.
const ready = ref(true)
let preloader

onMounted(() => {
  preloader = new Image()
  preloader.decoding = 'async'
  preloader.onload = () => { ready.value = true }
  preloader.onerror = () => { ready.value = false }
  preloader.src = letteringUrl
})

onBeforeUnmount(() => {
  if (preloader) preloader.onload = preloader.onerror = null
})
</script>

<style scoped>
.wuxia-title-lettering {
  display: block;
}
.is-ready {
  position: relative;
}
.lettering-art {
  display: block;
  width: 4.1em;
  max-width: 100%;
  height: 1.2em;
}
.lettering-text {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
  border: 0;
}
</style>
