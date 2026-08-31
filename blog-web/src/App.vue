<template>
  <div class="app-root" :class="{ 'cursor-click': cursorClicked }" :data-wuxia-page="isWuxiaPage || undefined">
    <InkWashBackdrop />
    <div v-if="showCursor" class="cursor-dot" :style="dotStyle"></div>
    <div v-if="showCursor" class="cursor-ring" :style="ringStyle"></div>
    <el-config-provider :locale="uiLocale">
      <router-view />
    </el-config-provider>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import InkWashBackdrop from '@/components/effects/InkWashBackdrop.vue'
import { useInkInteractions } from '@/composables/useInkInteractions'
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'

const route = useRoute()
const { isWuxiaPage } = useWuxiaCopy()
const uiLocale = computed(() => isWuxiaPage.value ? {
  ...zhCn,
  el: { ...zhCn.el, table: { ...zhCn.el.table, emptyText: '暂无卷宗' } },
} : zhCn)
useInkInteractions()
const cursorClicked = ref(false)
const showCursor = ref(false)

const dotStyle = ref({})
const ringStyle = ref({})

let raf = null

const onMove = (e) => {
  dotStyle.value = { left: `${e.clientX - 6}px`, top: `${e.clientY - 6}px` }
  if (!raf) {
    raf = requestAnimationFrame(() => {
      ringStyle.value = { left: `${e.clientX - 18}px`, top: `${e.clientY - 18}px` }
      raf = null
    })
  }
}

const onDown = () => { cursorClicked.value = true }
const onUp = () => { cursorClicked.value = false }

watch(() => route.path, (path) => {
  showCursor.value = !path.startsWith('/admin') && window.innerWidth > 768
}, { immediate: true })

onMounted(() => {
  if (window.innerWidth > 768 && !route.path.startsWith('/admin')) {
    showCursor.value = true
    document.addEventListener('mousemove', onMove)
    document.addEventListener('mousedown', onDown)
    document.addEventListener('mouseup', onUp)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMove)
  document.removeEventListener('mousedown', onDown)
  document.removeEventListener('mouseup', onUp)
})
</script>
