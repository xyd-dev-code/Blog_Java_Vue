<template>
  <header class="app-header">
    <div class="container nav-inner">
      <router-link to="/" class="brand" @mouseenter="prefetchByName('home')">
        <img v-if="siteLogo" :src="siteLogo" class="brand-mark brand-mark-img" alt="" aria-hidden="true" />
        <span v-else-if="siteName" class="brand-mark">{{ brandMark }}</span>
        <span class="brand-text">
          <span class="brand-name">{{ siteName }}</span>
          <span class="brand-motto">{{ siteMotto }}</span>
        </span>
      </router-link>

      <nav class="nav-menu">
        <router-link to="/" class="nav-item" exact-active-class="active" @mouseenter="prefetchByName('home')">首页</router-link>
        <router-link to="/articles" class="nav-item" active-class="active" @mouseenter="prefetchByName('articles')">文章</router-link>
        <router-link to="/projects" class="nav-item" active-class="active" @mouseenter="prefetchByName('projects')">项目</router-link>
        <router-link to="/tools" class="nav-item" active-class="active" @mouseenter="prefetchByName('tools')">工具</router-link>
        <router-link to="/friends" class="nav-item" active-class="active" @mouseenter="prefetchByName('friends')">友链</router-link>
        <router-link to="/guestbook" class="nav-item" active-class="active" @mouseenter="prefetchByName('guestbook')">留言</router-link>
        <router-link to="/about" class="nav-item" active-class="active" @mouseenter="prefetchByName('about')">关于</router-link>
      </nav>

      <div class="nav-tools">
        <el-input v-model="kw" placeholder="搜点什么…" size="default" clearable inputmode="search"
          class="search-box" :class="{ 'is-focused': searchFocused }"
          @focus="searchFocused = true" @blur="searchFocused = false"
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
         避免被页面里 transform/filter 元素(如 SkyHero)盖住 -->
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
            <el-input v-model="kw" placeholder="搜点什么…" clearable inputmode="search" @keyup.enter="goSearchAndClose">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <nav class="drawer-nav">
            <router-link to="/" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('home')">首页</router-link>
            <router-link to="/articles" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('articles')">文章</router-link>
            <router-link to="/projects" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('projects')">项目</router-link>
            <router-link to="/tools" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('tools')">工具</router-link>
            <router-link to="/friends" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('friends')">友链</router-link>
            <router-link to="/guestbook" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('guestbook')">留言</router-link>
            <router-link to="/about" class="drawer-link" @click="toggleDrawer" @touchstart="prefetchByName('about')">关于</router-link>
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
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { prefetchByName } from '@/router'
import { Search } from '@element-plus/icons-vue'
import { useSiteStore } from '@/stores/site'

const router = useRouter()
const route = useRoute()
const siteStore = useSiteStore()
const kw = ref('')
const drawerOpen = ref(false)
const searchFocused = ref(false)

const siteName = computed(() => siteStore.info?.siteName || '个人博客')
const siteMotto = computed(() => siteStore.info?.motto || '')
const siteLogo = computed(() => siteStore.info?.siteLogo || '')
// 站点 mark 默认字符:取站点名第一个汉字或字母
const brandMark = computed(() => {
  const s = siteName.value || ''
    return s ? s.charAt(0).toUpperCase() : '·'
})

const goSearch = () => {
  if (!kw.value.trim()) return
  router.push({ name: 'search', query: { kw: kw.value.trim() } })
}
const goSearchAndClose = () => {
  if (!kw.value.trim()) return
  drawerOpen.value = false
  goSearch()
}
const toggleDrawer = () => {
  drawerOpen.value = !drawerOpen.value
  document.body.style.overflow = drawerOpen.value ? 'hidden' : ''
}
onMounted(async () => {
  // 不再监听滚动事件 — 用户明确不要滚动收缩/变色
})
onUnmounted(() => {
  if (typeof document !== 'undefined') document.body.style.overflow = ''
})
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

</script>

<style scoped lang="scss">
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding-top: 12px;
  background: transparent;
  /* 提升为独立合成层:背景 8 层特效持续动画时,
     导航不再被每帧重绘,避免点击跳转卡顿 */
  will-change: transform;
  transform: translateZ(0);
  backface-visibility: hidden;
}
.nav-inner {
  display: flex;
  align-items: center;
  height: 64px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 6px 24px rgba(14, 165, 233, 0.12);
  /* 不再有任何 scrolled 态变化 —— 用户明确不要滚动收缩/变色 */
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
  flex: 0 0 40px;
  border-radius: 50%;
  overflow: hidden;
  -webkit-clip-path: circle(50% at 50% 50%);
  clip-path: circle(50% at 50% 50%);
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
  display: block;
  width: 40px;
  height: 40px;
  border-radius: inherit;
  -webkit-clip-path: inherit;
  clip-path: inherit;
  object-fit: cover;
  padding: 0;
  background: transparent;
}
.brand-text { display: flex; flex-direction: column; line-height: 1.1; }
.brand-name { font-family: var(--font-serif); font-size: 20px; font-weight: 600; color: var(--c-ink); }
.brand-motto {
  font-size: 12px;
  color: var(--c-ink-soft);
  letter-spacing: 0.05em;
  margin-top: 3px;
  overflow: hidden;
  max-height: 20px;
  transition: opacity 0.25s ease, max-height 0.3s ease, margin-top 0.3s ease;
}

.nav-menu {
  display: flex;
  gap: 4px;
}
.nav-item {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 15px;
  color: var(--c-ink-soft);
  transition: color 0.2s ease, background 0.2s ease;
  position: relative;
}
/* hover 下划线：从中心向两边展开 */
.nav-item::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 4px;
  height: 2px;
  border-radius: 1px;
  background: linear-gradient(90deg, var(--c-botany-500), var(--c-botany-700));
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  pointer-events: none;
}
.nav-item:hover {
  color: var(--c-botany-700);
  background: var(--c-botany-50);
}
.nav-item:hover::after { transform: scaleX(1); }

.nav-item.active {
  color: var(--c-botany-700);
  font-weight: 500;
}
/* 激活项：暖阳金短横，常驻不参与 hover 动画 */
.nav-item.active::after {
  background: var(--c-autumn-500);
  transform: scaleX(1);
  left: 50%;
  right: auto;
  width: 20px;
  margin-left: -10px;
  bottom: 3px;
}
.nav-item.active:hover::after {
  background: linear-gradient(90deg, var(--c-autumn-500), var(--c-botany-500));
  width: 28px;
  margin-left: -14px;
  transition: width 0.28s cubic-bezier(0.22, 1, 0.36, 1),
              margin-left 0.28s cubic-bezier(0.22, 1, 0.36, 1),
              background 0.28s ease;
}

.nav-tools { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: flex-end; gap: 8px; }
/* 聚焦时平滑横向拓宽，失焦收回 */
.search-box {
  width: 200px;
  transition: width 0.34s cubic-bezier(0.22, 1, 0.36, 1);

  &.is-focused { width: 300px; }

  :deep(.el-input__wrapper) {
    background: var(--c-line-soft);
    box-shadow: none;
    border-radius: 999px;
    transition: background 0.25s ease, box-shadow 0.25s ease;
  }
  :deep(.el-input__wrapper):hover {
    background: #fff;
    box-shadow: 0 0 0 1px var(--c-botany-300);
  }
  :deep(.el-input__wrapper.is-focus) {
    background: #fff;
    box-shadow:
      0 0 0 1px var(--c-botany-500),
      0 4px 16px rgba(56, 189, 248, 0.18);
  }
}

// 汉堡按钮 — 移动端显示
.hamburger {
  display: none;
  width: 44px; height: 44px;
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
  .search-box {
    width: 160px;
    &.is-focused { width: 220px; }
  }
  .brand-motto { display: none; }
  .hamburger { display: flex; }
}
/* 窄屏空间紧张，聚焦拉伸幅度收敛，避免挤压 logo */
@media (max-width: 1100px) and (min-width: 901px) {
  .search-box.is-focused { width: 250px; }
}
@media (max-width: 480px) {
  .brand-text .brand-name { font-size: 18px; }
  /* logo / nav-inner 高度不变 —— 用户明确"不要变小" */
  .search-box { display: none; }   // 桌面端搜索框;移动端用抽屉里的
  .hamburger { width: 44px; height: 44px; }
}
</style>
