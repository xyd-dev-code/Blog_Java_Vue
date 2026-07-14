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
            <span>最近文章</span>
          </template>
          <div class="table-scroll">
            <el-table :data="recentArticles" stripe>
              <el-table-column prop="title" label="标题" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '已发布' : '草稿' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="viewCount" label="阅读" width="80" />
              <el-table-column prop="createTime" label="创建时间" width="160">
                <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="dashboard-card">
          <template #header><span>待审核评论</span></template>
          <el-empty v-if="!pendingComments.length" description="无待审评论" :image-size="80" />
          <div v-else class="pc-list">
            <div v-for="c in pendingComments" :key="c.id" class="pc-item">
              <div class="pc-meta">
                <b>{{ c.nickname }}</b>
                <span class="text-soft">{{ fmtDate(c.createTime) }}</span>
              </div>
              <div class="pc-content">{{ c.content }}</div>
              <div class="pc-actions">
                <el-button size="small" type="success" @click="approve(c)">通过</el-button>
                <el-button size="small" type="danger" plain @click="spam(c)">垃圾</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Document, Folder, ChatDotRound, View
} from '@element-plus/icons-vue'
import { adminDashboard, adminApproveComment, adminSpamComment } from '@/api/admin'
import { fmtDate } from '@/utils/format'

const stats = ref({})
const recentArticles = ref([])
const pendingComments = ref([])

const statsCards = computed(() => [
  { label: '文章总数', value: stats.value.articleCount || 0, icon: Document, bg: 'linear-gradient(135deg, #38bdf8, #0ea5e9)' },
  { label: '分类数', value: stats.value.categoryCount || 0, icon: Folder, bg: 'linear-gradient(135deg, #22d3ee, #06b6d4)' },
  { label: '评论数', value: stats.value.commentCount || 0, icon: ChatDotRound, bg: 'linear-gradient(135deg, #fbbf24, #f59e0b)' },
  { label: '总阅读量', value: stats.value.viewCount || 0, icon: View, bg: 'linear-gradient(135deg, #38bdf8, #22d3ee)' }
])

const load = async () => {
  try {
    const resp = await adminDashboard()
    stats.value = resp.data?.stats || {}
    recentArticles.value = resp.data?.recentArticles || []
    pendingComments.value = resp.data?.pendingComments || []
  } catch (_) {}
}

const approve = async (c) => {
  await adminApproveComment(c.id)
  pendingComments.value = pendingComments.value.filter(x => x.id !== c.id)
}
const spam = async (c) => {
  await adminSpamComment(c.id)
  pendingComments.value = pendingComments.value.filter(x => x.id !== c.id)
}

onMounted(load)
</script>

<style scoped lang="scss">
.stat-card {
  border-radius: 12px;
  padding: 22px 24px;
  color: #fff;
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
}

.pc-list { display: flex; flex-direction: column; gap: 14px; }
.pc-item {
  padding: 12px;
  background: var(--c-botany-50);
  border-radius: 8px;
}
.pc-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
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
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
</style>