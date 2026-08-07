<template>
  <div class="admin-archives-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <div class="title-area">
            <span class="title">归档管理</span>
            <span class="sub">按月份聚合的所有文章，共 <b>{{ articles.length }}</b> 篇</span>
          </div>
          <div class="actions">
            <el-input v-model="kw" placeholder="搜索标题…" clearable style="width: 240px;" @keyup.enter="reload" />
            <el-button @click="reload"><el-icon><Refresh /></el-icon>&nbsp;刷新</el-button>
          </div>
        </div>
      </template>

      <div class="archive-layout">
        <!-- 左：月份聚合 -->
        <aside class="month-aside">
          <div class="aside-eyebrow">月份</div>
          <div class="month-list" v-if="months.length">
            <div
              v-for="m in months"
              :key="m.ym"
              class="month-item"
              :class="{ active: activeYm === m.ym }"
              @click="activeYm = m.ym"
            >
              <div class="m-year">{{ m.ym.split('-')[0] }}</div>
              <div class="m-month">{{ Number(m.ym.split('-')[1]) }}月</div>
              <div class="m-count">{{ m.cnt }} 篇</div>
            </div>
          </div>
          <el-empty v-else description="暂无归档" :image-size="60" />
        </aside>

        <!-- 右：当前月份文章 -->
        <main class="article-pane">
          <div class="pane-head">
            <div>
              <div class="pane-eyebrow">{{ activeYm ? `${activeYm.split('-')[0]} 年 ${Number(activeYm.split('-')[1])} 月` : '所有文章' }}</div>
              <div class="pane-title">{{ filtered.length }} 篇</div>
            </div>
            <div class="pane-stats">
              <span class="stat-chip"><el-icon><View /></el-icon>&nbsp;{{ totalViews }} 阅读</span>
              <span class="stat-chip stat-chip-sun"><el-icon><Star /></el-icon>&nbsp;{{ totalFeatured }} 推荐</span>
            </div>
          </div>

          <div class="art-table table-scroll" v-loading="loading">
            <el-table :data="filtered" stripe>
              <el-table-column type="index" label="#" width="60" align="center" />
              <el-table-column label="日期" width="100">
                <template #default="{ row }"><span class="cell-text">{{ fmtDate(row.createTime, 'MM-DD') }}</span></template>
              </el-table-column>
              <el-table-column label="标题" min-width="280">
                <template #default="{ row }">
                  <div class="cell-flex">
                    <a class="art-title" @click="goEdit(row)" :title="row.title">{{ row.title }}</a>
                    <div class="art-meta-row">
                      <el-tag v-if="row.categoryName" size="small" effect="plain" type="warning">
                        {{ row.categoryName }}
                      </el-tag>
                      <el-tag v-for="t in (row.tags || []).slice(0, 3)" :key="t.id"
                        size="small" effect="plain"># {{ t.name }}</el-tag>
                      <el-tag v-if="(row.tags || []).length > 3" size="small" effect="plain" type="info"
                        :title="(row.tags || []).slice(3).map(t => '#' + t.name).join(' ')">
                        +{{ (row.tags || []).length - 3 }}
                      </el-tag>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 1" type="success" size="small">已发布</el-tag>
                  <el-tag v-else type="info" size="small">草稿</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="数据" width="160" align="center">
                <template #default="{ row }">
                  <span class="mini-stat"><el-icon><View /></el-icon>&nbsp;{{ row.viewCount || 0 }}</span>
                  <span class="mini-stat"><el-icon><ChatDotRound /></el-icon>&nbsp;{{ row.commentCount || 0 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <div class="cell-actions">
                    <el-button size="small" link type="primary" @click="goEdit(row)">编辑</el-button>
                    <el-button size="small" link @click="preview(row)" v-if="row.status === 1">前台</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loading && !filtered.length" description="该月份下没有文章" :image-size="80" />
          </div>
        </main>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Refresh, View, ChatDotRound, Star
} from '@element-plus/icons-vue'
import { archives } from '@/api/front'
import { fmtDate } from '@/utils/format'

const router = useRouter()
const articles = ref([])
const months = ref([])
const activeYm = ref('')
const kw = ref('')
const loading = ref(false)

const filtered = computed(() => {
  let arr = articles.value
  if (activeYm.value) arr = arr.filter(a => (a.createTime || '').slice(0, 7) === activeYm.value)
  if (kw.value.trim()) {
    const k = kw.value.trim().toLowerCase()
    arr = arr.filter(a => (a.title || '').toLowerCase().includes(k))
  }
  return arr
})

const totalViews = computed(() => filtered.value.reduce((s, a) => s + (a.viewCount || 0), 0))
const totalFeatured = computed(() => filtered.value.filter(a => a.isFeatured === 1).length)

const reload = async () => {
  loading.value = true
  try {
    const resp = await archives()
    const raw = resp.data
    articles.value = raw?.articles || raw?.records || (Array.isArray(raw) ? raw : [])
    months.value = raw?.months || buildMonths(articles.value)
    if (!activeYm.value && months.value.length) activeYm.value = months.value[0].ym
  } catch (_) {}
  loading.value = false
}

// 兜底：若后端没返回 months，自行按 articles 聚合
const buildMonths = (arr) => {
  const map = new Map()
  for (const a of arr) {
    const ym = (a.createTime || '').slice(0, 7)
    if (!ym) continue
    map.set(ym, (map.get(ym) || 0) + 1)
  }
  return [...map.entries()]
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([ym, cnt]) => ({ ym, cnt }))
}

const goEdit = (row) => router.push(`/admin/articles/${row.id}/edit`)
const preview = (row) => window.open(`/articles/${row.slug}`, '_blank')

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.title-area { display: flex; flex-direction: column; gap: 4px; }
.title { font-weight: 600; font-size: 16px; color: var(--c-ink); }
.sub { font-size: 12px; color: var(--c-ink-soft); }
.sub b { color: var(--c-botany-500); font-weight: 600; }
.actions { display: flex; gap: 8px; }

.archive-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 18px;
  /* 不再固定 min-height — 让两列各自按内容撑开 + overflow-y: auto 独立滚动 */
  align-items: stretch;
}
.archive-layout > * { min-height: 0; }  /* grid 子项允许内容溢出,不会把父 grid 撑开 */

/* 左：月份 */
.month-aside {
  background: linear-gradient(180deg, #f0f9ff 0%, #ffffff 100%);
  border: 1px solid #e0f2fe;
  border-radius: 10px;
  padding: 14px;
  /* 关键:固定高度 + 独立滚动 */
  max-height: calc(100vh - 200px);  /* 减去 header + 边距,可视区高度 */
  overflow-y: auto;
  overflow-x: hidden;
  position: sticky;
  top: 80px;  /* 顶部 admin-topbar 高度 */
}
.aside-eyebrow {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #06b6d4;
  font-weight: 600;
  margin-bottom: 12px;
}
.month-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.month-item {
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  background: #fff;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  position: relative;
}
.month-item:hover {
  border-color: #bae6fd;
  background: #f0f9ff;
}
.month-item.active {
  background: linear-gradient(135deg, #38bdf8, #22d3ee);
  color: #fff;
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.3);
}
.month-item.active .m-year,
.month-item.active .m-count { color: rgba(255, 255, 255, 0.95); }
.month-item.active .m-month { color: rgba(255, 255, 255, 0.85); }

.m-year {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.1;
  color: var(--c-ink);
}
.m-month {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 2px;
}
.m-count {
  font-size: 11px;
  color: var(--c-ink-soft);
  margin-top: 6px;
}

/* 右：当前月份 */
.article-pane {
  background: #fff;
  border: 1px solid #e0f2fe;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  /* 关键:独立滚动(不依赖整页 scroll) */
  max-height: calc(100vh - 200px);
  overflow: hidden;  /* 表格区域自己滚动,header 不滚 */
}
.pane-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px dashed #e0f2fe;
}
.pane-eyebrow {
  font-size: 11px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #06b6d4;
  font-weight: 600;
}
.pane-title {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  margin-top: 4px;
  color: var(--c-ink);
}
.pane-stats {
  display: flex;
  gap: 8px;
}
.stat-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: rgba(56, 189, 248, 0.08);
  color: #0369a1;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.stat-chip-sun {
  background: rgba(251, 191, 36, 0.12);
  color: #b45309;
}

.art-table {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  /* 让 el-table 能撑开 + 内部滚动,而不是撑爆父 pane */
  min-height: 0;
}
.art-title {
  color: var(--c-ink);
  font-weight: 500;
  cursor: pointer;
  border-bottom: 1px dashed transparent;
  transition: all 0.2s ease;
}
.art-title:hover {
  color: #0369a1;
  border-bottom-color: #7dd3fc;
}
.art-meta-row {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.mini-stat {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-right: 12px;
}
.mini-stat .el-icon { font-size: 13px; }

@media (max-width: 900px) {
  .archive-layout { grid-template-columns: 1fr; }
  .month-aside, .article-pane {
    max-height: none;
    position: static;
  }
  .month-aside { padding: 10px; }
  .month-list {
    flex-direction: row;
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  .month-item {
    min-width: 110px;
    flex-shrink: 0;
  }
  .art-table { overflow-y: visible; }
}
@media (max-width: 480px) {
  .header-bar { gap: 8px; }
  .pane-head { flex-direction: column; align-items: flex-start; gap: 8px; }
  .pane-title { font-size: 18px; }
  .art-table.table-scroll { margin: 0 -10px; }
}
</style>