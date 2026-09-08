<template>
  <div class="report-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('举报管理') }}</span>
          <div class="header-filters">
          <el-radio-group v-model="filter" @change="reload">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pending">待处理 ({{ counts.pending || 0 }})</el-radio-button>
            <el-radio-button value="resolved">已处理</el-radio-button>
            <el-radio-button value="dismissed">已驳回</el-radio-button>
          </el-radio-group>
          <form class="management-search" role="search" @submit.prevent="search">
            <el-input v-model="keyword" placeholder="搜索被举报人、内容、详情或邮箱" aria-label="搜索被举报人、内容、详情或邮箱" maxlength="100" clearable @clear="search">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button native-type="submit" type="primary">搜索</el-button>
          </form>
          </div>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" :row-key="(row) => row.id">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (query.page - 1) * query.size + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="被举报留言" min-width="220">
            <template #default="{ row }">
              <div class="cell-flex">
                <div class="rp-target">
                  <b>@{{ row.commentNickname || '(已删除)' }}</b>
                  <span class="rp-excerpt">{{ row.commentExcerpt || '—' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="举报原因" width="120">
            <template #default="{ row }"><span class="cell-text">{{ reasonLabel(row.reason) }}</span></template>
          </el-table-column>
          <el-table-column label="详情" min-width="200">
            <template #default="{ row }">
              <div class="cell-flex">
                <span class="rp-detail">{{ row.detail || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="联系邮箱" width="200">
            <template #default="{ row }">
              <a v-if="row.email" :href="`mailto:${row.email}`">{{ row.email }}</a>
              <span v-else class="rp-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'warning' : (row.status === 1 ? 'success' : 'info')" size="small">
                {{ row.status === 0 ? '待处理' : (row.status === 1 ? '已处理' : '已驳回') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }"><span class="cell-text">{{ fmtDate(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="340">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button v-if="row.status !== 1" size="small" link type="success" @click="updateStatus(row, 1)">已处理</el-button>
                <el-button v-if="row.status !== 2" size="small" link type="info" @click="updateStatus(row, 2)">驳回</el-button>
                <el-button size="small" link type="warning" @click="spamComment(row)">标垃圾</el-button>
                <el-button size="small" link type="danger" @click="removeComment(row)">删评论</el-button>
                <el-button size="small" link type="danger" @click="remove(row)">删记录</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="footer-bar" v-if="total > query.size">
        <el-pagination background layout="prev, pager, next, total"
          :current-page="query.page" :page-size="query.size" :total="total"
          @current-change="(p) => { query.page = p; reload(false) }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminReports, adminReportStats, adminUpdateReportStatus, adminDeleteReport,
  adminSpamGuestbook, adminDeleteGuestbook
} from '@/api/admin'
import { fmtDate } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const filter = ref('all')
const counts = ref({})
const keyword = ref('')

const query = reactive({ page: 1, size: 15, keyword: '', status: '' })

const reasonLabels = {
  spam: '广告/垃圾', abuse: '辱骂/不友善', porn: '色情/违规', plagiarism: '抄袭',
  illegal: '违法/敏感', attack: '人身攻击', offtopic: '跑题', other: '其他'
}
const reasonLabel = (v) => reasonLabels[v] || v || '—'

const search = () => {
  query.keyword = keyword.value.trim()
  reload()
}
let requestId = 0
const reload = async (resetPage = true) => {
  const currentRequest = ++requestId
  loading.value = true
  query.status = filter.value === 'all' ? '' :
                 filter.value === 'pending' ? 0 :
                 filter.value === 'resolved' ? 1 : 2
  // 分页器回调里传 false,避免「点第 2 页 → 立刻被重置回第 1 页」的递归 bug
  if (resetPage) query.page = 1
  try {
    const resp = await adminReports({ ...query })
    if (currentRequest !== requestId) return
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (_) {}
  if (currentRequest !== requestId) return
  loading.value = false
  try {
    counts.value = (await adminReportStats()).data || {}
  } catch (_) {}
}

const updateStatus = async (row, status) => {
  await adminUpdateReportStatus(row.id, status)
  ElMessage.success(status === 1 ? '已标记为已处理' : '已驳回')
  reload()
}
const remove = async (row) => {
  await ElMessageBox.confirm('确定删除该举报记录?', '提示', { type: 'warning' })
  await adminDeleteReport(row.id); ElMessage.success('已删除'); reload()
}

// 对被举报的评论直接处理(标垃圾/删除),同时把举报状态改为已处理
const spamComment = async (row) => {
  await ElMessageBox.confirm('确定将该评论标记为垃圾?', '提示', { type: 'warning' })
  await adminSpamGuestbook(row.commentId)
  await adminUpdateReportStatus(row.id, 1)
  ElMessage.success('已标记垃圾 + 举报已处理'); reload()
}
const removeComment = async (row) => {
  await ElMessageBox.confirm('确定永久删除该评论?(含全部回复) 操作不可逆', '警告', { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' })
  await adminDeleteGuestbook(row.commentId)
  await adminUpdateReportStatus(row.id, 1)
  ElMessage.success('评论已删除 + 举报已处理'); reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-filters { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.management-search { display: flex; align-items: center; gap: 8px; width: 350px; max-width: 100%; }
.management-search .el-input { flex: 1; min-width: 0; }
@media (max-width: 600px) {
  .header-filters, .management-search { width: 100%; }
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.rp-target { display: flex; flex-direction: column; gap: 4px; }
.rp-excerpt {
  font-size: 12px; color: var(--c-ink-soft);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.rp-detail {
  font-size: 12px; color: var(--c-ink-500);
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.rp-muted { color: var(--c-ink-soft); font-size: 12px; }
.footer-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
</style>