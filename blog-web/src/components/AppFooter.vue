<template>
  <footer class="app-footer">
    <div class="container footer-inner">
      <div class="footer-col brand-col">
        <div class="f-brand">{{ siteName }}</div>
        <p class="f-motto">{{ siteMotto }}</p>
        <p v-if="siteDesc" class="f-desc">{{ siteDesc }}</p>
      </div>
      <div class="footer-col">
        <h4>导航</h4>
        <router-link to="/">首页</router-link>
        <router-link to="/articles">文章</router-link>
        <router-link to="/guestbook">留言</router-link>
      </div>
      <div class="footer-col">
        <h4>关注</h4>
        <a :href="githubUrl || 'javascript:;'" target="_blank">GitHub</a>
        <a href="javascript:;">微信公众号</a>
        <a href="javascript:;">RSS 订阅</a>
      </div>
      <div class="footer-col">
        <h4>友链</h4>
        <a href="javascript:;">Vue.js</a>
        <a href="javascript:;">Element Plus</a>
        <a href="javascript:;">MyBatis-Plus</a>
      </div>
    </div>
    <div class="footer-bottom">
      <div class="container">
        <span>© {{ year }} {{ siteName }} · Powered by Spring Boot & Vue 3</span>
        <span v-if="siteBeian" class="icp">{{ siteBeian }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const siteName = computed(() => siteStore.info?.siteName || 'DemoAuthor')
const siteMotto = computed(() => siteStore.info?.motto || '草木蔓发，春山可望')
const siteDesc = computed(() => siteStore.info?.description || '')
const siteBeian = computed(() => siteStore.info?.beian || '')
const githubUrl = computed(() => siteStore.info?.github || '')
const year = new Date().getFullYear()
</script>

<style scoped lang="scss">
.app-footer {
  background: linear-gradient(135deg, #0c4a6e 0%, #075985 100%);
  color: #e0f2fe;
  margin-top: 80px;
  position: relative;
  overflow: hidden;
}
.app-footer::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
}
.footer-inner {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr;
  gap: 40px;
  padding: 64px 24px 40px;
}
.footer-col h4 {
  font-family: var(--font-serif);
  color: #fff;
  font-size: 16px;
  margin: 0 0 18px;
}
.footer-col a {
  display: block;
  margin: 8px 0;
  color: #93c5fd;
  font-size: 14px;
  transition: color 0.2s;
}
.footer-col a:hover { color: var(--c-autumn-300); }

.f-brand {
  font-family: var(--font-serif);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}
.f-motto { color: var(--c-autumn-300); font-size: 14px; margin: 0 0 14px; }
.f-desc { color: #bae6fd; font-size: 13px; line-height: 1.7; margin: 0; }

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 22px 0;
  font-size: 13px;
  color: #7dd3fc;
}
.footer-bottom .container {
  display: flex;
  justify-content: space-between;
}

@media (max-width: 768px) {
  .footer-inner {
    grid-template-columns: 1fr 1fr;
    padding: 40px 24px 24px;
  }
}
@media (max-width: 480px) {
  .footer-inner { grid-template-columns: 1fr; padding: 36px 16px 20px; gap: 24px; }
  .footer-bottom { padding: 18px 0; }
  .footer-bottom .container { flex-direction: column; gap: 6px; text-align: center; }
  .f-brand { font-size: 20px; }
}
</style>