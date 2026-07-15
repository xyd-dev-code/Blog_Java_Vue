<template>
  <header class="app-header" :class="{ scrolled }">
    <div class="container nav-inner">
      <router-link to="/" class="brand">
        <img v-if="siteLogo" :src="siteLogo" class="brand-mark brand-mark-img" alt="logo" />
        <span v-else-if="siteName" class="brand-mark">{{ brandMark }}</span>
        <span class="brand-text">
          <span class="brand-name">{{ siteName }}</span>
          <span class="brand-motto">{{ siteMotto }}</span>
        </span>
      </router-link>

      <nav class="nav-menu">
        <router-link to="/" class="nav-item" exact-active-class="active">首页</router-link>
        <router-link to="/articles" class="nav-item" active-class="active">文章</router-link>
        <router-link to="/projects" class="nav-item" active-class="active">项目</router-link>
        <router-link to="/friends" class="nav-item" active-class="active">友链</router-link>
        <router-link to="/guestbook" class="nav-item" active-class="active">留言</router-link>
        <router-link to="/about" class="nav-item" active-class="active">关于</router-link>
      </nav>

      <div class="nav-tools">
        <el-input v-model="kw" placeholder="搜点什么…" size="default" clearable class="search-box"
          @keyup.enter="goSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <button class="hamburger" :class="{ active: drawerOpen }" aria-label="菜单"
          @click="toggleDrawer">
          <span /><span /><span />
        </button>
      </div>
    </div>

    <!-- 手机抽屉 -->
    <!-- Teleport to body 让抽屉脱离 AppHeader 的 stacking context,
         避免被页面里 transform/filter 元素(如 SkyHero / ParticleBg)盖住 -->
    <Teleport to="body">
      <transition name="drawer">
        <div v-if="drawerOpen" class="mobile-drawer" @click.self="toggleDrawer">
        <div class="drawer-panel">
          <div class="drawer-head">
            <span class="drawer-title">{{ siteName }}</span>
            <button class="drawer-close" aria-label="关闭" @click="toggleDrawer">
              <svg viewBox="0 0 24 24" width="24" height="24">
                <path d="M6 6L18 18 M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" fill="none"/>
              </svg>
            </button>
          </div>
          <div class="drawer-search">
            <el-input v-model="kw" placeholder="搜点什么…" clearable @keyup.enter="goSearchAndClose">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <nav class="drawer-nav">
            <router-link to="/" class="drawer-link" @click="toggleDrawer">首页</router-link>
            <router-link to="/articles" class="drawer-link" @click="toggleDrawer">文章</router-link>
            <router-link to="/projects" class="drawer-link" @click="toggleDrawer">项目</router-link>
            <router-link to="/friends" class="drawer-link" @click="toggleDrawer">友链</router-link>
            <router-link to="/guestbook" class="drawer-link" @click="toggleDrawer">留言</router-link>
            <router-link to="/about" class="drawer-link" @click="toggleDrawer">关于</router-link>
          </nav>
          <div class="drawer-foot">
            <router-link to="/admin/login" class="drawer-admin" @click="toggleDrawer">
              <svg viewBox="0 0 24 24" width="16" height="16"><path d="M12 12c2.7 0 5-2.3 5-5s-2.3-5-5-5-5 2.3-5 5 2.3 5 5 5zm0 2c-3.3 0-10 1.7-10 5v3h20v-3c0-3.3-6.7-5-10-5z" fill="currentColor"/></svg>
              管理后台
            </router-link>
          </div>
        </div>
        </div>
      </transition>
    </Teleport>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useSiteStore } from '@/stores/site'

const router = useRouter()
const route = useRoute()
const siteStore = useSiteStore()
const kw = ref('')
const scrolled = ref(false)
const drawerOpen = ref(false)

const siteName = computed(() => siteStore.info?.siteName || '个人博客')
const siteMotto = computed(() => siteStore.info?.motto || '')
const siteLogo = computed(() => siteStore.info?.siteLogo || '')
// 站点 mark 默认字符:取站点名第一个汉字或字母
const brandMark = computed(() => {
  const s = siteName.value || ''
  return s ? s.charAt(0).toUpperCase() : '·'
})

const onScroll = () => { scrolled.value = window.scrollY > 24 }

const goSearch = () => {
  if (!kw.value.trim()) return
  router.push({ name: 'search', query: { kw: kw.value.trim() } })
}
const goSearchAndClose = () => {
  if (!kw.value.trim()) return
  drawerOpen.value = false
  goSearch()
}
const toggleDrawer = () => { drawerOpen.value = !drawerOpen.value }

// 路由变化时关闭抽屉,并清空搜索词防止误触
watch(() => route.fullPath, () => {
  drawerOpen.value = false
  kw.value = ''
})

// 抽屉打开时锁 body 滚动
watch(drawerOpen, (v) => {
  if (typeof document !== 'undefined') {
    document.body.style.overflow = v ? 'hidden' : ''
  }
})

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (typeof document !== 'undefined') document.body.style.overflow = ''
})
</script>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid transparent;
  transition: all 0.3s ease;
}
.app-header.scrolled {
  background: rgba(255, 255, 255, 0.96);
  border-bottom-color: var(--c-line);
  box-shadow: 0 2px 16px rgba(14, 165, 233, 0.08);
}
.nav-inner {
  display: flex;
  align-items: center;
  height: 72px;
}
.brand {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: transform 0.2s ease;
}
.brand:hover { transform: translateY(-1px); }
.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-autumn-500));
  color: #fff;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.2);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.brand:hover .brand-mark {
  transform: scale(1.05) rotate(-2deg);
  box-shadow: 0 6px 18px rgba(14, 165, 233, 0.3);
}
.brand-mark-img {
  object-fit: cover;
  padding: 0;
  background: transparent;
}
.brand-text { display: flex; flex-direction: column; line-height: 1.1; }
.brand-name { font-family: var(--font-serif); font-size: 20px; font-weight: 600; color: var(--c-ink); }
.brand-motto { font-size: 11px; color: var(--c-ink-soft); letter-spacing: 0.05em; margin-top: 3px; }

.nav-menu {
  display: flex;
  gap: 4px;
}
.nav-item {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 15px;
  color: var(--c-ink-soft);
  transition: all 0.2s ease;
  position: relative;
}
.nav-item:hover { color: var(--c-botany-500); background: var(--c-botany-50); }
.nav-item.active {
  color: var(--c-botany-700);
  font-weight: 500;
}
.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px; left: 50%;
  transform: translateX(-50%);
  width: 20px; height: 2px;
  background: var(--c-autumn-500);
  border-radius: 1px;
}

.nav-tools { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: flex-end; gap: 8px; }
.search-box {
  width: 200px;
  :deep(.el-input__wrapper) {
    background: var(--c-line-soft);
    box-shadow: none;
    border-radius: 999px;
  }
  :deep(.el-input__wrapper):hover, :deep(.el-input__wrapper.is-focus) {
    background: #fff;
    box-shadow: 0 0 0 1px var(--c-botany-300);
  }
}

// 汉堡按钮 — 移动端显示
.hamburger {
  display: none;
  width: 40px; height: 40px;
  border: none; background: transparent;
  flex-direction: column; justify-content: center; align-items: center;
  gap: 5px; cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  &:hover { background: var(--c-botany-50); }
  span {
    display: block;
    width: 22px; height: 2px;
    background: var(--c-ink);
    border-radius: 2px;
    transition: transform 0.25s ease, opacity 0.2s;
  }
  &.active span:nth-child(1) { transform: translateY(7px) rotate(45deg); }
  &.active span:nth-child(2) { opacity: 0; }
  &.active span:nth-child(3) { transform: translateY(-7px) rotate(-45deg); }
}

// 抽屉遮罩与面板
.mobile-drawer {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
  overflow: hidden;
}
.drawer-panel {
  width: min(86vw, 320px);
  background: #fff;
  height: 100%;
  max-width: 100vw;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(15, 23, 42, 0.18);
}
.drawer-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 22px 12px;
  border-bottom: 1px solid var(--c-line-soft);
}
.drawer-title {
  font-family: var(--font-serif);
  font-size: 18px; font-weight: 600;
  color: var(--c-ink);
}
.drawer-close {
  width: 36px; height: 36px;
  border: none; background: transparent;
  border-radius: 8px;
  display: grid; place-items: center;
  color: var(--c-ink-soft);
  cursor: pointer;
  &:hover { background: var(--c-botany-50); color: var(--c-ink); }
}
.drawer-search { padding: 16px 22px 8px; }
.drawer-nav {
  flex: 1;
  display: flex; flex-direction: column;
  padding: 8px 12px;
  overflow-y: auto;
}
.drawer-link {
  padding: 14px 16px;
  font-size: 16px;
  color: var(--c-ink-700);
  border-radius: 10px;
  transition: all 0.18s;
  &:hover { background: var(--c-botany-50); color: var(--c-botany-700); }
  &.router-link-active {
    background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
    color: #fff;
    font-weight: 500;
  }
}
.drawer-foot {
  padding: 14px 22px 28px;
  border-top: 1px solid var(--c-line-soft);
}
.drawer-admin {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 16px;
  border-radius: 999px;
  background: var(--c-ink-50);
  color: var(--c-ink-700);
  font-size: 14px;
  transition: all 0.18s;
  &:hover { background: var(--c-botany-500); color: #fff; }
}

.drawer-enter-active, .drawer-leave-active { transition: opacity 0.25s; overflow: hidden; }
.drawer-enter-active .drawer-panel, .drawer-leave-active .drawer-panel { transition: transform 0.3s cubic-bezier(.4,0,.2,1); }
.drawer-enter-from { opacity: 0; visibility: hidden; }
.drawer-enter-from .drawer-panel { transform: translateX(100%); }
.drawer-leave-to { opacity: 0; visibility: hidden; }
.drawer-leave-to .drawer-panel { transform: translateX(100%); }

@media (max-width: 900px) {
  .nav-menu { display: none; }
  .search-box { width: 160px; }
  .brand-motto { display: none; }
  .hamburger { display: flex; }
}
@media (max-width: 480px) {
  .brand-text .brand-name { font-size: 17px; }
  .brand-mark { width: 36px; height: 36px; font-size: 19px; }
  .search-box { display: none; }   // 桌面端搜索框;移动端用抽屉里的
  .hamburger { width: 44px; height: 44px; }
  .nav-inner { height: 60px; }
}
</style>
