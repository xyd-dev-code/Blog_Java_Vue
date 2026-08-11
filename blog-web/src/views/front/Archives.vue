<template>
  <div class="archives-page">
    <SkyPageHead title="文章归档" :subtitle="`共 ${articles.length} 篇 · 按月份整理`" />

    <div class="container-narrow page-body">
      <div class="timeline">
        <div v-for="(group, ym) in grouped" :key="ym" class="timeline-group reveal">
          <div class="tl-marker">
            <div class="tl-year">{{ ym.split('-')[0] }}</div>
            <div class="tl-month">{{ Number(ym.split('-')[1]) }}月</div>
            <div class="tl-count">{{ group.length }} 篇</div>
          </div>
          <ul class="tl-list">
            <li v-for="a in group" :key="a.id" @click="$router.push(`/articles/${a.slug}`)">
              <span class="tl-day">{{ fmtDate(a.createTime, 'DD') }}</span>
              <span class="tl-title">{{ a.title }}</span>
              <span v-if="a.categoryName" class="tl-cat">· {{ a.categoryName }}</span>
              <span class="tl-arrow">→</span>
            </li>
          </ul>
        </div>
        <el-empty v-if="!articles.length" description="还没有文章" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { archives } from '@/api/front'
import { fmtDate } from '@/utils/format'
import SkyPageHead from '@/components/SkyPageHead.vue'

const articles = ref([])

onMounted(async () => {
  try {
    const resp = await archives()
    // 后端返回 { months, articles }，兼容直接数组或 { records }
    const raw = resp.data
    articles.value = raw?.articles || raw?.records || (Array.isArray(raw) ? raw : [])
  } catch (_) {}
})

const grouped = computed(() => {
  const map = {}
  for (const a of articles.value) {
    const ym = (a.createTime || '').slice(0, 7)
    if (!map[ym]) map[ym] = []
    map[ym].push(a)
  }
  return map
})
</script>

<style scoped lang="scss">
.page-body { padding: 40px 0 60px; }

/* 时间线：晴空 v2 */
.timeline-group {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 28px;
  margin-bottom: 40px;
  position: relative;
}
.tl-marker {
  text-align: right;
  padding-right: 28px;
  border-right: 2px solid transparent;
  background-image: linear-gradient(180deg, #38bdf8, #fbbf24);
  background-size: 2px 100%;
  background-repeat: no-repeat;
  background-position: right 0;
  position: relative;
}
.tl-marker::after {
  content: '';
  position: absolute;
  right: -8px; top: 4px;
  width: 14px; height: 14px;
  border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #fbbf24);
  border: 3px solid #fff;
  box-shadow:
    0 0 0 2px rgba(56, 189, 248, 0.3),
    0 4px 12px rgba(56, 189, 248, 0.25);
}
.tl-year {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 700;
  background: linear-gradient(135deg, #0369a1, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
  line-height: 1;
}
.tl-month {
  font-size: 13px;
  color: #0369a1;
  font-weight: 500;
  margin-top: 4px;
}
.tl-count {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 6px;
  padding: 2px 8px;
  background: rgba(56, 189, 248, 0.1);
  border-radius: 999px;
  display: inline-block;
}

.tl-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.tl-list li {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(125, 211, 252, 0.4);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  margin-bottom: 10px;
  backdrop-filter: blur(6px);
}
.tl-list li:hover {
  background: #fff;
  border-color: #38bdf8;
  transform: translateX(4px);
  box-shadow:
    0 6px 20px rgba(56, 189, 248, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}
.tl-day {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
  flex-shrink: 0;
  width: 36px;
  text-align: center;
}
.tl-title {
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--c-ink);
  flex: 1;
  transition: color 0.2s ease;
}
.tl-list li:hover .tl-title { color: #0369a1; }
.tl-cat {
  font-size: 12px;
  color: var(--c-ink-soft);
  background: rgba(56, 189, 248, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
}
.tl-arrow {
  color: #38bdf8;
  font-weight: 600;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.25s ease;
}
.tl-list li:hover .tl-arrow {
  opacity: 1;
  transform: translateX(0);
}

.reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible { opacity: 1; transform: translateY(0); }

@media (max-width: 768px) {
  .timeline-group { grid-template-columns: 90px 1fr; gap: 16px; }
  .tl-year { font-size: 22px; }
  .tl-marker { padding-right: 16px; }
  .tl-list li { padding: 10px 12px; gap: 8px; }
  .tl-cat { display: none; }
}
</style>