<template>
  <div class="sub-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>订阅者管理</span>
          <div class="header-actions">
            <el-button :loading="exporting" @click="onExport">
              <el-icon><Download /></el-icon> 导出 CSV
            </el-button>
            <el-button type="primary" @click="openCreate">
              <el-icon><Plus /></el-icon> 新增订阅
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <div class="stat-grid">
        <div class="stat-card" v-for="(s, i) in statsCards" :key="i" :style="{ background: s.bg }">
          <el-icon class="stat-icon"><component :is="s.icon" /></el-icon>
          <div class="stat-num">{{ stats[s.key] ?? 0 }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>

      <!-- 过滤栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索邮箱…"
            clearable
            inputmode="search"
            class="kw-input"
            @keyup.enter="reload"
            @clear="reload"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button @click="reload">搜索</el-button>
        </div>
        <el-radio-group v-model="filter" @change="reload">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="confirmed">已确认 ({{ stats.confirmed || 0 }})</el-radio-button>
          <el-radio-button value="pending">待确认 ({{ stats.pending || 0 }})</el-radio-button>
          <el-radio-button value="unsubscribed">已退订 ({{ stats.unsubscribed || 0 }})</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 表格 -->
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" :row-key="(row) => row.id" empty-text="暂无订阅者">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (query.page - 1) * query.size + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-text">{{ row.email }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'" size="small">
                {{ row.status === 1 ? '已确认' : row.status === 2 ? '已退订' : '待确认' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <span class="cell-text">{{ sourceLabel(row.source) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="订阅时间" width="170">
            <template #default="{ row }"><span class="cell-text">{{ fmtDateTime(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column prop="confirmTime" label="确认时间" width="170">
            <template #default="{ row }">
              <span class="cell-text">{{ row.confirmTime ? fmtDateTime(row.confirmTime) : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="center">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button v-if="row.status === 0" size="small" link type="success" @click="confirmRow(row)">确认</el-button>
                <el-button v-if="row.status === 1" size="small" link type="warning" @click="unsub(row)">退订</el-button>
                <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 新增订阅弹窗 -->
    <el-dialog v-model="createVisible" title="新增订阅" width="min(440px, 92vw)" append-to-body @closed="resetCreate">
      <el-form :model="createForm" @submit.prevent="submitCreate">
        <el-form-item label="邮箱" :error="createError">
          <el-input v-model="createForm.email" type="email" inputmode="email" placeholder="subscriber@example.com" @keyup.enter="submitCreate" />
        </el-form-item>
        <p class="create-tip">后台添加视为可信来源，将直接标记为「已确认」，不会发送确认邮件。</p>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, CircleCheck, Clock, TrendCharts, Calendar, Download, Plus, Search, CircleClose } from '@element-plus/icons-vue'
import {
  adminSubscriptions, adminSubscriptionStats, adminCreateSubscription,
  adminConfirmSubscription, adminUnsubscribeSubscription,
  adminDeleteSubscription, adminExportSubscriptions, triggerDownload
} from '@/api/admin'
import { fmtDateTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const keyword = ref('')
const filter = ref('all')

const statsCards = [
  { key: 'total', label: '总订阅数', bg: 'linear-gradient(135deg,#0ea5e9,#38bdf8)', icon: Bell },
  { key: 'confirmed', label: '已确认', bg: 'linear-gradient(135deg,#10b981,#34d399)', icon: CircleCheck },
  { key: 'pending', label: '待确认', bg: 'linear-gradient(135deg,#f59e0b,#fbbf24)', icon: Clock },
  { key: 'unsubscribed', label: '已退订', bg: 'linear-gradient(135deg,#64748b,#94a3b8)', icon: CircleClose },
  { key: 'todayNew', label: '今日新增', bg: 'linear-gradient(135deg,#6366f1,#818cf8)', icon: Calendar },
  { key: 'weekNew', label: '近 7 天新增', bg: 'linear-gradient(135deg,#ec4899,#f472b6)', icon: TrendCharts }
]

const sourceLabel = (s) => {
  if (s === 'admin') return '后台'
  if (s === 'web') return '网页'
  return s || '—'
}

const query = reactive({ page: 1, size: 15, status: '' })

const loadStats = async () => {
  try {
    stats.value = (await adminSubscriptionStats()).data || {}
  } catch (_) {}
}

const reload = async (resetPage = true) => {
  loading.value = true
  query.status = filter.value === 'all' ? ''
    : filter.value === 'confirmed' ? 1
    : filter.value === 'pending' ? 0 : 2
  if (resetPage) query.page = 1
  try {
    const resp = await adminSubscriptions({ page: query.page, size: query.size, keyword: keyword.value, status: query.status })
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载订阅者失败')
  }
  loading.value = false
  loadStats()
}

// ── 删除 / 确认 / 退订 ──
// 统一兜底：catch 里区分「用户取消 confirm」(字符串 'cancel')与真实错误；
// 没有兜底会导致 ReferenceError/网络异常被 promise rejection 静默吞掉。
const runOp = async (row, fn, okMsg) => {
  try {
    await fn(row.id)
    ElMessage.success(okMsg)
    reload(false)
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}
const remove = (row) =>
  ElMessageBox.confirm(`确定彻底删除订阅「${row.email}」? 记录将被移除且无法恢复。`, '提示', { type: 'warning' })
    .then(() => runOp(row, adminDeleteSubscription, '已删除'))
    .catch(() => {})
const confirmRow = (row) => runOp(row, adminConfirmSubscription, '已确认为已订阅')
const unsub = (row) =>
  ElMessageBox.confirm(`确定退订「${row.email}」? 记录将保留并标记为已退订，不再推送邮件。`, '提示', { type: 'warning' })
    .then(() => runOp(row, adminUnsubscribeSubscription, '已退订'))
    .catch(() => {})

// ── 新增订阅 ──
const createVisible = ref(false)
const createLoading = ref(false)
const createError = ref('')
const createForm = reactive({ email: '' })
const openCreate = () => { createVisible.value = true }
const resetCreate = () => { createForm.email = ''; createError.value = ''; createLoading.value = false }
const submitCreate = async () => {
  const email = createForm.email.trim()
  if (!email) { createError.value = '请输入邮箱'; return }
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
    createError.value = '邮箱格式不正确'; return
  }
  createLoading.value = true
  createError.value = ''
  try {
    await adminCreateSubscription({ email })
    ElMessage.success('已添加订阅')
    createVisible.value = false
    reload(false)
  } catch (e) {
    createError.value = e?.response?.data?.message || e?.message || '添加失败'
  } finally {
    createLoading.value = false
  }
}

// ── 导出 CSV ──
const exporting = ref(false)
const onExport = async () => {
  exporting.value = true
  try {
    const blob = await adminExportSubscriptions()
    triggerDownload(blob, `subscriptions-${new Date().toISOString().slice(0, 10)}.csv`)
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(err?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.header-actions { display: flex; gap: 8px; }
.header-actions .el-icon { margin-right: 4px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 12px;
  padding: 18px 20px;
  color: #fff;
  position: relative;
  overflow: hidden;
  min-height: 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-icon {
  position: absolute;
  right: 14px;
  top: 14px;
  font-size: 36px;
  opacity: 0.28;
}
.stat-num { font-family: var(--font-serif); font-size: 30px; font-weight: 600; line-height: 1.1; }
.stat-label { font-size: 13px; opacity: 0.92; margin-top: 4px; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}
.toolbar-left { display: flex; gap: 8px; }
.kw-input { width: 240px; }

.footer-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.cell-actions { display: flex; gap: 4px; justify-content: center; }
.create-tip { font-size: 12px; color: var(--c-ink-soft); margin: 0; }

@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .kw-input { width: 160px; }
  .toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-left { width: 100%; }
  .kw-input { flex: 1; }
}
</style>
