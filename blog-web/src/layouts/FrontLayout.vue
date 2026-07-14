<template>
  <div class="front-layout">
    <AppHeader />
    <main class="front-main">
      <router-view v-slot="{ Component }">
        <transition name="page-slide" mode="out-in" appear>
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import { useSiteStore } from '@/stores/site'
import { useScrollReveal } from '@/composables/useScrollReveal'

const siteStore = useSiteStore()
useScrollReveal('.reveal')
// 站点配置无缓存,每次进前台都重新拉取,保证 admin 改名后立即生效
onMounted(() => siteStore.load())
</script>

<style scoped lang="scss">
.front-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.front-main {
  flex: 1;
}
.page-slide-enter-active,
.page-slide-leave-active {
  transition: opacity 0.35s ease, transform 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.page-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.page-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>