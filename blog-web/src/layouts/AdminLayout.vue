<template>
  <div class="admin-layout">
    <!-- 移动端遮罩 -->
    <transition name="fade">
      <div v-if="drawerOpen && isMobile" class="aside-mask" @click="drawerOpen = false" />
    </transition>

    <aside class="admin-aside" :class="{ collapsed, 'mobile-open': drawerOpen && isMobile }">
      <router-link to="/" class="aside-brand">
        <img v-if="siteLogo" :src="siteLogo" class="mark mark-img" alt="" aria-hidden="true" />
        <span v-else class="mark">·</span>
        <span v-if="!collapsed" class="text">{{ wx('Admin') }}</span>
      </router-link>
      <nav class="aside-nav" @click="drawerOpen = false">
        <router-link to="/admin/dashboard" class="nav-item">
          <el-icon><DataLine /></el-icon><span v-if="!collapsed">{{ wx('仪表盘') }}</span>
        </router-link>
        <router-link to="/admin/articles" class="nav-item">
          <el-icon><Document /></el-icon><span v-if="!collapsed">{{ wx('文章管理') }}</span>
        </router-link>
        <router-link to="/admin/archives" class="nav-item">
          <el-icon><Clock /></el-icon><span v-if="!collapsed">{{ wx('归档管理') }}</span>
        </router-link>
        <router-link to="/admin/comments" class="nav-item">
          <el-icon><ChatDotRound /></el-icon><span v-if="!collapsed">{{ wx('评论管理') }}</span>
        </router-link>
        <router-link to="/admin/guestbook" class="nav-item">
          <el-icon><ChatLineSquare /></el-icon><span v-if="!collapsed">{{ wx('留言管理') }}</span>
        </router-link>
        <router-link to="/admin/projects" class="nav-item">
          <el-icon><Box /></el-icon><span v-if="!collapsed">{{ wx('项目管理') }}</span>
        </router-link>
        <router-link to="/admin/tools" class="nav-item">
          <el-icon><Tools /></el-icon><span v-if="!collapsed">{{ wx('工具管理') }}</span>
        </router-link>
        <router-link to="/admin/friend-links" class="nav-item">
          <el-icon><Link /></el-icon><span v-if="!collapsed">{{ wx('友链管理') }}</span>
        </router-link>
        <router-link to="/admin/subscriptions" class="nav-item">
          <el-icon><Bell /></el-icon><span v-if="!collapsed">{{ wx('订阅管理') }}</span>
        </router-link>
        <router-link to="/admin/stats" class="nav-item">
          <el-icon><DataAnalysis /></el-icon><span v-if="!collapsed">{{ wx('访问统计') }}</span>
        </router-link>
        <div class="nav-sep" />
        <router-link to="/admin/themes" class="nav-item" :aria-label="wx('主题管理')">
          <el-icon aria-hidden="true"><Brush /></el-icon><span v-if="!collapsed">{{ wx('主题管理') }}</span>
        </router-link>
        <router-link to="/admin/profile" class="nav-item">
          <el-icon><User /></el-icon><span v-if="!collapsed">{{ wx('个人中心') }}</span>
        </router-link>
      </nav>
      <div class="aside-foot" v-if="!collapsed">
        <a href="/" target="_blank"><el-icon><View /></el-icon> {{ wx('查看前台') }}</a>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <!-- 移动端汉堡 -->
        <button v-if="isMobile" class="admin-hamburger" aria-label="菜单" @click="drawerOpen = !drawerOpen">
          <span /><span /><span />
        </button>
        <!-- 桌面端折叠按钮 -->
        <el-button v-else text @click="collapsed = !collapsed">
          <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        </el-button>
        <div class="topbar-title">{{ pageTitle }}</div>
        <div class="topbar-right">
          <el-dropdown @command="onCmd">
            <span class="user-trigger">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" style="background: var(--c-botany-500); color: var(--theme-on-primary);">
                {{ userInitial }}
              </el-avatar>
              <span v-if="!isMobile" class="user-name">{{ userStore.userInfo?.username || wx('Admin') }}</span>
              <el-icon v-if="!isMobile"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">{{ wx('返回前台') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Expand, Fold, DataLine, DataAnalysis, Document,
  ChatDotRound, ChatLineSquare, Files, View, ArrowDown, Box, Link, User, Clock, Tools, Bell, Brush,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const collapsed = ref(false)
const drawerOpen = ref(false)
const isMobile = ref(false)
const isToolsActive = computed(() => route.path.startsWith('/admin/tools') || route.path.startsWith('/admin/tool-categories'))
const userInitial = computed(() => (userStore.userInfo?.username || 'A')[0].toUpperCase())
const siteLogo = computed(() => siteStore.info?.siteLogo || '')

const checkMobile = () => {
  const before = isMobile.value
  isMobile.value = window.innerWidth < 900
  if (isMobile.value) {
    drawerOpen.value = false
  } else if (before && !isMobile.value) {
    drawerOpen.value = false
  }
}

watch(() => route.fullPath, () => {
  drawerOpen.value = false
})

const titles = {
  'admin-dashboard': '仪表盘',
  'admin-articles': '文章管理',
  'admin-archives': '归档管理',
  'admin-article-new': '写文章',
  'admin-article-edit': '编辑文章',
  'admin-comments': '评论管理',
  'admin-guestbook': '留言管理',
  'admin-pages': '页面管理',
  'admin-projects': '项目管理',
  'admin-tools': '工具管理',
  'admin-friend-links': '友链管理',
  'admin-subscriptions': '订阅管理',
  'admin-stats': '访问统计',
  'admin-themes': '主题管理',
  'admin-profile': '个人中心'
}
const pageTitle = computed(() => wx(titles[route.name] || '后台'))

const onCmd = (cmd) => {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/admin/login')
  } else if (cmd === 'home') {
    window.open('/', '_blank')
  }
}

// resize 防抖：连续 resize 事件合并到 150ms 后只执行一次，避免频繁重渲染
let resizeTimer = null
const onResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(checkMobile, 150)
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', onResize, { passive: true })
  siteStore.load()
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: linear-gradient(180deg, var(--c-bg) 0%, var(--c-paper-soft) 100%);
}
.admin-aside {
  width: 220px;
  height: 100%;
  background: var(--c-paper);
  border-right: 1px solid var(--c-line);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
}
.admin-aside.collapsed { width: 64px; }
.admin-aside.collapsed .nav-group-items { display: none; }
.aside-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--c-line-soft);
}
.aside-brand .mark {
  width: 32px; height: 32px;
  border-radius: 50%;
  overflow: hidden;
  -webkit-clip-path: circle(50% at 50% 50%);
  clip-path: circle(50% at 50% 50%);
  display: grid; place-items: center;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-autumn-500));
  color: var(--theme-on-primary);
  font-family: var(--font-serif);
  font-size: 18px;
  flex-shrink: 0;
}
.aside-brand .mark-img {
  display: block;
  border-radius: 50%;
  -webkit-clip-path: circle(50% at 50% 50%);
  clip-path: circle(50% at 50% 50%);
  object-fit: cover;
  padding: 0;
  background: transparent;
}
.aside-brand .text {
  font-family: var(--font-serif);
  font-weight: 600;
  font-size: 16px;
  color: var(--c-ink);
}

.aside-nav { flex: 1; padding: 12px; overflow-y: auto; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  color: var(--c-ink-soft);
  margin-bottom: 4px;
  font-size: 14px;
  transition: all 0.2s;
}
.nav-item:hover { background: var(--c-botany-50); color: var(--c-botany-700); }
.nav-item.router-link-active {
  background: var(--c-botany-500);
  color: var(--theme-on-primary);
}
.nav-item .el-icon { font-size: 16px; flex-shrink: 0; }

.nav-sep {
  height: 1px;
  background: var(--c-line-soft);
  margin: 8px 12px;
}

.aside-foot {
  padding: 12px 20px;
  border-top: 1px solid var(--c-line-soft);
  font-size: 13px;
}
.aside-foot a {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-ink-soft);
}

.admin-main { flex: 1; display: flex; flex-direction: column; min-width: 0; height: 100%; }
.admin-topbar {
  background: var(--c-paper);
  height: 56px;
  border-bottom: 1px solid var(--c-line);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
}
.topbar-title {
  font-family: var(--font-serif);
  font-weight: 600;
  font-size: 16px;
  color: var(--c-ink);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  flex: 1; min-width: 0;
}
.topbar-right { margin-left: auto; }
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
}
.user-trigger:hover { background: var(--c-botany-50); }
.admin-content { flex: 1; padding: 24px; overflow-y: auto; overflow-x: hidden; }

.admin-hamburger {
  display: none;
  width: 40px; height: 40px;
  border: none; background: transparent;
  flex-direction: column; justify-content: center; align-items: center;
  gap: 5px; cursor: pointer; padding: 0; margin: 0;
  border-radius: 8px;
  &:hover { background: var(--c-botany-50); }
  span {
    display: block; width: 22px; height: 2px;
    background: var(--c-ink); border-radius: 2px;
  }
}
.aside-mask {
  position: fixed; inset: 0;
  background: var(--theme-overlay);
  z-index: 99;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 900px) {
  .admin-layout > .admin-aside {
    position: fixed;
    top: 0; left: 0; bottom: 0;
    width: 240px;
    z-index: 100;
    box-shadow: 4px 0 24px rgba(var(--theme-ink-rgb), 0.15);
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(.4,0,.2,1);
  }
  .admin-aside.mobile-open { transform: translateX(0); }
  .admin-aside.mobile-open ~ .admin-main { /* noop */ }
  .admin-main { width: 100%; }
  .admin-topbar { padding: 0 14px; gap: 10px; }
  .topbar-title { font-size: 15px; }
  .admin-hamburger { display: flex; }
  .admin-content { padding: 14px; }
}

@media (max-width: 480px) {
  .admin-content { padding: 10px; }
  .topbar-title { font-size: 14px; }
  .user-trigger { padding: 4px 6px; }
}
</style>
