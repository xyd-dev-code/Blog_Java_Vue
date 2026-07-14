<template>
  <div class="home-sky">
    <SkyHero
      :title-main="heroTitle.main"
      :title-accent="heroTitle.accent"
      :subtitle="heroSubtitle"
      :stats="heroStats"
      @cta="goArticles"
      @secondary="goAbout"
    />

    <!-- 精选 -->
    <section class="section" v-if="featured.length">
      <div class="container">
        <div class="section-head reveal">
          <div>
            <span class="section-eyebrow">featured</span>
            <h2 class="section-title">精选文章</h2>
            <p class="section-sub">这一阵子最想推荐的几篇</p>
          </div>
          <router-link to="/articles" class="more-link">查看全部 →</router-link>
        </div>
        <div class="post-grid">
          <PostCardSky v-for="a in featured" :key="a.id" :article="a" />
        </div>
      </div>
    </section>

    <!-- 最新 + 标签云（不对称布局） -->
    <section class="section">
      <div class="container">
        <div class="section-head reveal">
          <div>
            <span class="section-eyebrow">latest</span>
            <h2 class="section-title">最新文章</h2>
            <p class="section-sub">最近写的几篇</p>
          </div>
          <router-link to="/articles" class="more-link">查看全部 →</router-link>
        </div>
        <div class="post-grid">
          <PostCardSky v-for="a in latest" :key="a.id" :article="a" class="reveal" />
        </div>
      </div>
    </section>

    <!-- 标签云 -->
    <section class="section">
      <div class="container">
        <GradientBorderCard variant="mix" class="tag-cloud-card reveal">
          <div class="tc-head">
            <span class="section-eyebrow">tags</span>
            <h2 class="section-title">常常会写到的话题</h2>
          </div>
          <div class="tc-list" v-if="tags.length">
            <router-link
              v-for="t in tags"
              :key="t.id"
              :to="`/tags/${t.slug || t.name}`"
              class="tc-chip"
              :class="{ 'is-big': (t.articleCount || 0) > 8, 'is-med': (t.articleCount || 0) > 4 && (t.articleCount || 0) <= 8 }"
            >
              # {{ t.name }}
              <span v-if="t.articleCount" class="tc-count">{{ t.articleCount }}</span>
            </router-link>
          </div>
          <div class="tc-list" v-else>
            <span class="tc-chip is-big"># Vue 3</span>
            <span class="tc-chip"># 草木</span>
            <span class="tc-chip is-med"># Java</span>
            <span class="tc-chip"># 设计</span>
            <span class="tc-chip"># 茶事</span>
            <span class="tc-chip"># 立秋</span>
          </div>
        </GradientBorderCard>
      </div>
    </section>

    <!-- 留言 + 关于 CTA -->
    <section class="section">
      <div class="container cta-grid">
        <router-link to="/about" class="cta-card reveal">
          <span class="section-eyebrow">about</span>
          <div class="cta-title">关于我 · {{ authorName }}</div>
          <p class="cta-desc">前端工程师，写代码也写散文。常在山里，偶尔进城。</p>
          <div class="cta-more">了解更多 →</div>
        </router-link>
        <router-link to="/guestbook" class="cta-card cta-sun reveal">
          <span class="section-eyebrow">guestbook</span>
          <div class="cta-title">留言板</div>
          <p class="cta-desc">来过就留个脚印吧。每一则留言都会被认真地读。</p>
          <div class="cta-more">去留言 →</div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSiteStore } from '@/stores/site'
import { useAuthor } from '@/composables/useAuthor'
import SkyHero from '@/components/SkyHero.vue'
import PostCardSky from '@/components/PostCardSky.vue'
import GradientBorderCard from '@/components/GradientBorderCard.vue'
import { home, tagsAll } from '@/api/front'

const router = useRouter()
const siteStore = useSiteStore()
const { authorName } = useAuthor()

const homeData = ref(null)
const featured = ref([])
const latest = ref([])
const tags = ref([])

const heroTitle = computed(() => {
  const name = siteStore.info?.siteName || 'DemoAuthor'
  const motto = siteStore.info?.motto || '春山可望'
  return { main: name, accent: motto }
})

const heroSubtitle = computed(() =>
  siteStore.info?.description || '把博客做成一片晴天。喝口茶，看看今天的天空。'
)

const heroStats = computed(() => {
  const s = homeData.value?.stats || {}
  return [
    { label: '文章', value: s.articleCount || 87 },
    { label: '分类', value: s.categoryCount || 12 },
    { label: '标签', value: s.tagCount || 12 },
    { label: '阅读', value: s.viewCount || 0 }
  ]
})

const goArticles = () => router.push('/articles')
const goAbout = () => router.push('/about')

onMounted(async () => {
  try {
    const [homeResp, tagsResp] = await Promise.all([home(), tagsAll().catch(() => ({ data: [] }))])
    homeData.value = homeResp.data
    featured.value = (homeResp.data.featured || []).slice(0, 3)
    latest.value = (homeResp.data.latest || []).slice(0, 4)
    tags.value = (tagsResp.data || []).slice(0, 14)
  } catch (_) {
    // 接口失败时 fallback 数据由模板里的 v-else 处理
  }
})
</script>

<style scoped lang="scss">
.section {
  padding: 80px 0;
}
.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 36px;
}
.section-eyebrow {
  display: block;
  font-size: 11px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #06b6d4;
  font-weight: 600;
  margin-bottom: 8px;
}
.section-title {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--c-ink, #1e293b);
  position: relative;
  display: inline-block;
}
.section-title::after {
  content: '';
  display: block;
  width: 50px;
  height: 3px;
  margin-top: 8px;
  background: linear-gradient(90deg, #38bdf8, #fbbf24, #38bdf8);
  background-size: 200% 100%;
  border-radius: 2px;
  animation: shimmer-bar 3s ease-in-out infinite;
}
@keyframes shimmer-bar {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}
.section-sub {
  color: var(--c-ink-soft, #64748b);
  font-size: 14px;
  margin: 0;
}
.more-link {
  color: #0ea5e9;
  font-size: 14px;
  font-weight: 500;
  transition: gap 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.more-link:hover { color: #0369a1; gap: 8px; }

// 文章网格
.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 24px;
}

// 标签云
.tag-cloud-card { padding: 36px; text-align: center; }
.tc-head { margin-bottom: 24px; }
.tc-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 760px;
  margin: 0 auto;
}
.tc-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #f0f9ff;
  color: #0369a1;
  border: 1px solid #e0f2fe;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.25s ease, background 0.25s ease;
}
.tc-chip:hover {
  background: #38bdf8;
  color: #fff;
  border-color: #38bdf8;
  transform: translateY(-2px);
}
.tc-chip.is-med {
  padding: 8px 18px;
  font-size: 14px;
}
.tc-chip.is-big {
  padding: 10px 22px;
  font-size: 16px;
  background: #38bdf8;
  color: #fff;
  border-color: #38bdf8;
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.3);
}
.tc-chip.is-big:hover { background: #0369a1; border-color: #0369a1; }
.tc-count {
  font-size: 11px;
  padding: 1px 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 999px;
}

// CTA
.cta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.cta-card {
  display: block;
  padding: 36px;
  background: #fff;
  border-radius: 18px;
  border: 1px solid #e0f2fe;
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1),
              box-shadow 0.35s ease;
}
.cta-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(14, 165, 233, 0.1);
  border-color: #7dd3fc;
}
.cta-sun {
  background: linear-gradient(135deg, #fffbeb 0%, #fff 100%);
  border-color: #fde68a;
}
.cta-sun:hover { border-color: #fbbf24; }
.cta-title {
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  margin: 4px 0 8px;
  color: var(--c-ink, #1e293b);
}
.cta-desc {
  font-size: 14px;
  line-height: 1.8;
  color: var(--c-ink-soft, #64748b);
  margin: 0 0 16px;
}
.cta-more {
  font-size: 13px;
  color: #0ea5e9;
  font-weight: 500;
}
.cta-sun .cta-more { color: #f59e0b; }

// reveal
.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 900px) {
  .cta-grid { grid-template-columns: 1fr; }
}
</style>