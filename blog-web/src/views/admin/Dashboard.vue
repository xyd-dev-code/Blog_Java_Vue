<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :xs="12" :md="6" v-for="(s, i) in statsCards" :key="i">
        <div class="stat-card" :style="{ background: s.bg }">
          <el-icon class="stat-icon"><component :is="s.icon" /></el-icon>
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="dashboard-row" style="margin-top: 20px;">
      <el-col :xs="24" :md="16">
        <el-card class="dashboard-card">
          <template #header>
            <div class="card-head">
              <span>{{ wx('文章') }}</span>
              <router-link to="/admin/articles" class="head-link">查看更多 →</router-link>
            </div>
          </template>
          <div class="card-section">
            <div class="section-title">{{ wx('最近发布') }}</div>
            <div class="table-scroll">
              <el-table :data="recentArticles" stripe>
                <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                      {{ row.status === 1 ? '已发布' : '草稿' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="viewCount" label="阅读" width="80" />
                <el-table-column prop="createTime" label="创建时间" width="120">
                  <template #default="{ row }">{{ fmtDate(row.createTime, 'MM-DD') }}</template>
                </el-table-column>
              </el-table>
            </div>
            <div class="pager" v-if="articleTotal > articleSize">
              <el-pagination small background layout="prev, pager, next, total"
                :current-page="articlePage" :page-size="articleSize" :total="articleTotal"
                @current-change="(p) => { articlePage = p; load() }" />
            </div>
          </div>

          <el-divider class="card-divider" />

          <div class="card-section">
            <div class="section-title">{{ wx('最热阅读') }}</div>
            <div class="table-scroll">
              <el-table :data="topArticles" stripe>
                <el-table-column label="#" width="48" align="center">
                  <template #default="{ $index }">
                    <span class="rank-badge" :class="`rank-${$index + 1}`">{{ $index + 1 }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
                <el-table-column prop="viewCount" label="阅读" width="80" />
                <el-table-column prop="createTime" label="发布时间" width="100">
                  <template #default="{ row }">{{ fmtDate(row.createTime, 'MM-DD') }}</template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="dashboard-card">
          <template #header>
            <div class="card-head">
              <span>{{ wx('待审核评论') }}</span>
              <router-link to="/admin/comments?tab=comment" class="head-link">查看更多 →</router-link>
            </div>
          </template>
          <el-empty v-if="!pendingComments.length && !loadingComments" :description="wx('无待审评论')" :image-size="80" />
          <div v-loading="loadingComments" v-else class="pc-list">
            <div v-for="c in pendingComments" :key="c.id" class="pc-item">
              <div class="pc-meta">
                <el-avatar :src="c.avatar" :size="32">{{ c.nickname?.[0] }}</el-avatar>
                <div class="pc-meta-text">
                  <b>{{ c.nickname }}</b>
                  <span class="text-soft">{{ fmtDate(c.createTime) }}</span>
                </div>
              </div>
              <div class="pc-content">{{ c.content }}</div>
              <div class="pc-actions">
                <el-button size="small" type="success" @click="approve(c)">通过</el-button>
                <el-button size="small" type="danger" plain @click="spam(c)">垃圾</el-button>
              </div>
            </div>
          </div>
          <div class="pager" v-if="commentTotal > commentSize">
            <el-pagination small background layout="prev, pager, next, total"
              :current-page="commentPage" :page-size="commentSize" :total="commentTotal"
              @current-change="(p) => { commentPage = p; load() }" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, Folder, ChatDotRound, View
} from '@element-plus/icons-vue'
import { adminDashboard, adminApproveComment, adminSpamComment } from '@/api/admin'
import { fmtDate } from '@/utils/format'

const PAGE_SIZE = 5

const stats = ref({})
const recentArticles = ref([])
const topArticles = ref([])
const pendingComments = ref([])
const articlePage = ref(1)
const articleSize = ref(PAGE_SIZE)
const articleTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(PAGE_SIZE)
const commentTotal = ref(0)
const loadingComments = ref(false)

const statsCards = computed(() => [
  { label: '文章总数', value: stats.value.articleCount || 0, icon: Document, bg: 'linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700))' },
  { label: '分类数', value: stats.value.categoryCount || 0, icon: Folder, bg: 'linear-gradient(135deg, var(--c-cyan-500), var(--c-cyan-700))' },
  { label: '评论数', value: stats.value.commentCount || 0, icon: ChatDotRound, bg: 'linear-gradient(135deg, var(--c-autumn-500), var(--c-autumn-700))' },
  { label: '总阅读量', value: stats.value.viewCount || 0, icon: View, bg: 'linear-gradient(135deg, var(--c-botany-500), var(--c-cyan-500))' }
])

const load = async () => {
  loadingComments.value = true
  try {
    const resp = await adminDashboard({
      articlePage: articlePage.value,
      articleSize: articleSize.value,
      commentPage: commentPage.value,
      commentSize: commentSize.value,
    })
    stats.value = resp.data?.stats || {}
    recentArticles.value = resp.data?.recentArticles || []
    articleTotal.value = resp.data?.recentArticlesTotal || 0
    topArticles.value = resp.data?.topArticles || []
    pendingComments.value = resp.data?.pendingComments || []
    commentTotal.value = resp.data?.pendingCommentsTotal || 0
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载仪表盘失败')
  }
  loadingComments.value = false
}

const approve = async (c) => {
  try {
    await adminApproveComment(c.id)
    // 重新拉当前页,让总数 / 分页跟后端真实数据一致
    await load()
    // 当前页空了但还有数据 → 翻到下一页,继续审核
    if (pendingComments.value.length === 0 && commentTotal.value > 0) {
      const maxPage = Math.max(1, Math.ceil(commentTotal.value / commentSize.value))
      commentPage.value = Math.min(commentPage.value + 1, maxPage)
      await load()
      ElMessage.info('本页已审完，已切到下一页')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}
const spam = async (c) => {
  try {
    await adminSpamComment(c.id)
    await load()
    if (pendingComments.value.length === 0 && commentTotal.value > 0) {
      const maxPage = Math.max(1, Math.ceil(commentTotal.value / commentSize.value))
      commentPage.value = Math.min(commentPage.value + 1, maxPage)
      await load()
      ElMessage.info('本页已处理完，已切到下一页')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.stat-card {
  border-radius: 12px;
  padding: 22px 24px;
  color: var(--theme-on-primary);
  position: relative;
  overflow: hidden;
  min-height: 110px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-icon {
  position: absolute;
  right: 16px;
  top: 16px;
  font-size: 40px;
  opacity: 0.3;
}
.stat-num { font-family: var(--font-serif); font-size: 32px; font-weight: 600; }
.stat-label { font-size: 13px; opacity: 0.9; margin-top: 4px; }

.dashboard-row {
  display: flex;
  align-items: stretch;
}
.dashboard-row > .el-col {
  display: flex;
}
.dashboard-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.dashboard-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.head-link {
  font-size: 12px;
  color: var(--el-color-primary);
  text-decoration: none;
}
.head-link:hover { text-decoration: underline; }

.pc-list { display: flex; flex-direction: column; gap: 14px; }
.pc-item {
  padding: 12px;
  background: var(--c-botany-50);
  border-radius: 8px;
}
.pc-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  margin-bottom: 6px;
}
.pc-meta-text {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.pc-content {
  font-size: 13px;
  color: var(--c-ink-soft);
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pc-actions { display: flex; gap: 8px; }
.text-soft { color: var(--c-ink-soft); font-size: 12px; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.card-section { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  letter-spacing: 0.05em;
  margin-bottom: 8px;
  text-transform: uppercase;
}
.card-divider {
  margin: 18px 0 14px;
  grid-column: 1 / -1;
}
.card-divider :deep(.el-divider__text) {
  background: var(--el-card-bg-color);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.05em;
}
.rank-badge {
  display: inline-block;
  width: 22px; height: 22px; line-height: 22px;
  border-radius: 50%;
  font-size: 12px; font-weight: 600;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  text-align: center;
}
.rank-badge.rank-1 { background: linear-gradient(135deg, var(--c-autumn-500), var(--c-autumn-700)); color: var(--theme-on-primary); }
.rank-badge.rank-2 { background: linear-gradient(135deg, var(--c-ink-200), var(--c-ink-300)); color: var(--theme-on-primary); }
.rank-badge.rank-3 { background: linear-gradient(135deg, var(--c-bronze-light), var(--c-bronze)); color: var(--theme-on-primary); }
</style>
