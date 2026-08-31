<template>
  <div class="front-layout">
    <!-- 视差云层：铺满整个布局高度，随滚动分层位移 -->
    <CloudParallax class="layout-clouds" />
    <!-- 动态背景层：光点星场 + 天光流影 + 枫叶飘落 + 浮光上升 + 极光天幕 + 光尘 + 底部光池，皆在内容之下 -->
    <SparkleField />
    <AuroraSky />
    <SkySheen />
    <FloatingLeaves />
    <BokehFloat />
    <LightDust />
    <LightPool />

    <AppHeader />
    <main class="front-main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" appear mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter />

    <!-- 交互特效层：星星拖尾 + 进度条 + 回顶 -->
    <ScrollBar />
    <StarTrail />
    <BackToTop />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ScrollBar from '@/components/effects/ScrollBar.vue'
import StarTrail from '@/components/effects/StarTrail.vue'
import BackToTop from '@/components/effects/BackToTop.vue'
import SparkleField from '@/components/effects/SparkleField.vue'
import CloudParallax from '@/components/effects/CloudParallax.vue'
import AuroraSky from '@/components/effects/AuroraSky.vue'
import SkySheen from '@/components/effects/SkySheen.vue'
import FloatingLeaves from '@/components/effects/FloatingLeaves.vue'
import BokehFloat from '@/components/effects/BokehFloat.vue'
import LightDust from '@/components/effects/LightDust.vue'
import LightPool from '@/components/effects/LightPool.vue'
import { useSiteStore } from '@/stores/site'
import { useScrollReveal } from '@/composables/useScrollReveal'

const siteStore = useSiteStore()
// .reveal 为既有约定；.ef-rise 为新的卡片入场类，一并观察
useScrollReveal('.reveal, .ef-rise')
// 站点配置无缓存,每次进前台都重新拉取,保证 admin 改名后立即生效
onMounted(() => siteStore.load())
</script>

<style scoped lang="scss">
.front-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  /* AppHeader 已改为 fixed，顶部留出药丸导航高度：12px padding-top + 64px nav-inner */
  padding-top: 76px;
}
/* 云层垫在最底，不拦截任何交互 */
.layout-clouds {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
.front-main {
  flex: 1;
  /* 抬到云层之上 */
  position: relative;
  z-index: 1;
}
@media (max-width: 768px) {
  .front-layout {
    /* 移动端 nav-inner 高度为 60px */
    padding-top: 72px;
  }
}
.page-fade-enter-active {
  transition: opacity 0.13s ease, transform 0.18s cubic-bezier(0.22, 1, 0.36, 1);
}
.page-fade-leave-active {
  transition: opacity 0.08s ease, transform 0.1s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.99);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.995);
}
</style>
