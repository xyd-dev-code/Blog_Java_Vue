<template>
  <div class="app-root" :class="{ 'cursor-click': cursorClicked }">
    <div v-if="showCursor" class="cursor-dot" :style="dotStyle"></div>
    <div v-if="showCursor" class="cursor-ring" :style="ringStyle"></div>
    <el-config-provider :locale="zhCn">
      <router-view />
    </el-config-provider>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const route = useRoute()
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