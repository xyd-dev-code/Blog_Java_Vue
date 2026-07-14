<template>
  <div class="articles-page">
    <HeroArticles :title="'所有文章'" :subtitle="`共 ${total} 篇 · 慢一点，让灵魂跟上脚步`" />

    <!-- 浮光粒子 -->
    <div class="af-motes" aria-hidden="true">
      <span v-for="i in 12" :key="i" class="af-mote"
        :style="{
          left: (8 + (i * 7.5)) + '%',
          animationDelay: (i * 0.8) + 's',
          animationDuration: (6 + (i % 4) * 2) + 's',
          width: (3 + (i % 3)) + 'px',
          height: (3 + (i % 3)) + 'px'
        }"
      ></span>
    </div>

    <!-- 装饰条：晴空 v2 渐变细线 -->
    <div class="container">
      <div class="sky-divider reveal"></div>
    </div>

    <div class="container page-body">
      <div class="article-list">
        <div v-if="loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <el-empty v-else-if="!list.length" description="暂无文章" />
        <PostCardSky
          v-for="(a, i) in list"
          v-else
          :key="a.id"
          :article="a"
          class="reveal"
          :style="{ transitionDelay: `${(i % 6) * 60}ms` }"
        />
        <div class="pagination" v-if="total > pageSize">
          <el-pagination background layout="prev, pager, next, total"
            :current-page="page" :page-size="pageSize" :total="total"
            @current-change="(p) => { page = p; reload() }" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PostCardSky from '@/components/PostCardSky.vue'
import HeroArticles from '@/components/HeroArticles.vue'
import { articles } from '@/api/front'

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)

const reload = async () => {
  loading.value = true
  try {
    const resp = await articles({ page: page.value, size: pageSize.value })
    list.value = resp.data?.records || resp.data || []
    total.value = resp.data?.total || list.value.length
  } catch (_) {}
  loading.value = false
}

onMounted(reload)
</script>

<style scoped lang="scss">
/* 浮光粒子 */
.af-motes {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.af-mote {
  position: absolute;
  top: -10px;
  border-radius: 50%;
  background: var(--c-botany-500);
  opacity: 0;
  animation: mote-fall ease-in-out infinite;
  box-shadow: 0 0 6px var(--c-botany-500);
}
@keyframes mote-fall {
  0% { transform: translateY(0) scale(0); opacity: 0; }
  15% { opacity: 0.5; transform: scale(1); }
  85% { opacity: 0.15; }
  100% { transform: translateY(100vh) scale(0.5); opacity: 0; }
}

.sky-divider {
  height: 3px;
  margin: 36px auto 0;
  max-width: 320px;
  background: linear-gradient(90deg, transparent, #38bdf8, #fbbf24, #38bdf8, transparent);
  background-size: 200% 100%;
  border-radius: 999px;
  animation: divider-shimmer 4s ease-in-out infinite;
}
@keyframes divider-shimmer {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}

.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 24px;
  padding: 40px 0 60px;
}
.pagination {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}
</style>