<template>
  <div class="gb-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('留言管理') }}</span>
          <div class="header-filters">
          <el-radio-group v-model="filter" @change="reload">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pending">待审核 ({{ counts.pending || 0 }})</el-radio-button>
            <el-radio-button value="approved">已通过</el-radio-button>
            <el-radio-button value="spam">垃圾</el-radio-button>
          </el-radio-group>
          <form class="guestbook-search" role="search" @submit.prevent="search">
            <el-input v-model="keyword" placeholder="搜索留言人或内容" aria-label="搜索留言人或内容" maxlength="100" clearable @clear="search">
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
          <el-table-column label="留言人" width="200">
            <template #default="{ row }">
              <div class="cell-flex">
                <div class="gb-author">
                  <el-avatar :src="row.avatar" :size="32" :key="row.avatar">{{ row.nickname?.[0] }}</el-avatar>
                  <div class="gb-author-info">
                    <div class="gb-name" :title="row.nickname">{{ row.nickname }}</div>
                    <div v-if="row.parentName" class="gb-reply-to">
                      ↳ 回复 <b>@{{ row.parentName }}</b>
                    </div>
                    <div v-else class="gb-email" :title="row.email">{{ row.email || '-' }}</div>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="300">
            <template #default="{ row }">
              <div class="cell-flex">
                <div class="gb-content" :title="row.content">{{ row.content }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : (row.status === 2 ? 'danger' : 'warning')" size="small">
                {{ row.status === 1 ? '已通过' : (row.status === 2 ? '垃圾' : '待审核') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="精选" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.featured === 1" type="warning" size="small" effect="dark">
                <el-icon><Star /></el-icon> 精选
              </el-tag>
              <span v-else class="gb-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }"><span class="cell-text">{{ fmtDate(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="点赞" width="70" align="center">
            <template #default="{ row }">
              <span :class="row.likeCount ? 'gb-like' : 'gb-muted'">{{ row.likeCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="被举报" width="80" align="center">
            <template #default="{ row }">
              <span :class="row.reportCount ? 'gb-reported' : 'gb-muted'">{{ row.reportCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button size="small" link type="primary" @click="openReply(row)">回复</el-button>
                <el-button v-if="row.featured !== 1" size="small" link type="warning" @click="setFeatured(row, true)">设精选</el-button>
                <el-button v-else size="small" link type="info" @click="setFeatured(row, false)">取消精选</el-button>
                <el-button v-if="row.status !== 1" size="small" link type="success" @click="approve(row)">通过</el-button>
                <el-button v-if="row.status !== 2" size="small" link type="warning" @click="spam(row)">垃圾</el-button>
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

      <el-dialog v-model="replyVisible" :title="wx('回复留言')" width="min(520px, 92vw)" append-to-body>
        <div v-if="replyTarget" class="reply-quote">
          ↳ 回复 <b>@{{ replyTarget.nickname }}</b>：{{ clipText(replyTarget.content) }}
        </div>
        <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="1000" show-word-limit
          :placeholder="wx('输入回复内容…')" />
        <template #footer>
          <el-button @click="replyVisible = false">取消</el-button>
          <el-button type="primary" :loading="replyLoading" @click="submitReply">发送回复</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, Search } from '@element-plus/icons-vue'
import {
  adminGuestbook, adminApproveGuestbook, adminSpamGuestbook, adminDeleteGuestbook, adminGuestbookStats, adminReplyGuestbook, adminSetFeaturedGuestbook
} from '@/api/admin'
import { fmtDate } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const filter = ref('all')
const counts = ref({})
const keyword = ref('')

const replyVisible = ref(false)
const replyLoading = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const openReply = (row) => { replyTarget.value = row; replyContent.value = ''; replyVisible.value = true }
const clipText = (s) => (s && s.length > 60 ? s.slice(0, 60) + '…' : (s || ''))
const submitReply = async () => {
  if (!replyContent.value.trim()) { ElMessage.warning('回复内容不能为空'); return }
  replyLoading.value = true
  try {
    await adminReplyGuestbook(replyTarget.value.id, { content: replyContent.value })
    ElMessage.success('回复已发送')
    replyVisible.value = false
    reload()
  } catch (_) {
  } finally {
    replyLoading.value = false
  }
}

const query = reactive({ page: 1, size: 15, status: '', keyword: '' })
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
                 filter.value === 'approved' ? 1 : 2
  // 分页器回调里传 false,避免「点第 2 页 → 立刻被重置回第 1 页」的递归 bug
  if (resetPage) query.page = 1
  try {
    const resp = await adminGuestbook({ ...query })
    if (currentRequest !== requestId) return
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (e) {
    if (currentRequest !== requestId) return
    ElMessage.error(e?.response?.data?.message || e?.message || '加载留言失败')
  }
  loading.value = false
  try {
    counts.value = (await adminGuestbookStats()).data || {}
  } catch (_) {}
}

const approve = async (row) => { await adminApproveGuestbook(row.id); ElMessage.success('已通过'); reload() }
const spam = async (row) => { await adminSpamGuestbook(row.id); ElMessage.success('已标记'); reload() }
const setFeatured = async (row, featured) => {
  await adminSetFeaturedGuestbook(row.id, featured)
  ElMessage.success(featured ? '已设为精选留言' : '已取消精选')
  reload()
}
const remove = async (row) => {
  await ElMessageBox.confirm('确定删除该留言?', '提示', { type: 'warning' })
  await adminDeleteGuestbook(row.id); ElMessage.success('已删除'); reload()
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
.header-filters { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.guestbook-search { display: flex; align-items: center; gap: 8px; width: 310px; max-width: 100%; }
.guestbook-search .el-input { flex: 1; min-width: 0; }
@media (max-width: 600px) {
  .header-filters, .guestbook-search { width: 100%; }
}
.gb-author { display: flex; align-items: center; gap: 10px; }
.gb-author-info { flex: 1; min-width: 0; }
.gb-name { font-weight: 500; }
.gb-reply-to {
  font-size: 12px;
  color: var(--c-botany-600);
  margin-top: 2px;
  background: var(--c-botany-50);
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  b { color: var(--c-botany-700); font-weight: 600; }
}
.gb-email {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 2px;
}
.gb-content {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.gb-muted { color: var(--c-ink-soft); font-size: 12px; }
.gb-like { color: var(--c-botany-600); font-weight: 500; }
.gb-reported { color: var(--c-autumn-600); font-weight: 500; }
.footer-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
</style>
