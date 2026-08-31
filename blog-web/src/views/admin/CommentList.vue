<template>
  <div class="cm-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('评论管理') }}</span>
          <el-radio-group v-model="filter" @change="reload">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pending">待审核 ({{ counts.pending || 0 }})</el-radio-button>
            <el-radio-button value="approved">已通过</el-radio-button>
            <el-radio-button value="spam">垃圾</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" class="desktop-table" :row-key="(row) => row.id">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (query.page - 1) * query.size + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="评论人" width="200">
            <template #default="{ row }">
              <div class="cell-flex">
                <div class="cm-author">
                  <el-avatar :src="row.avatar" :size="32" :key="row.avatar">{{ row.nickname?.[0] }}</el-avatar>
                  <div class="cm-author-info">
                    <div class="cm-name" :title="row.nickname">{{ row.nickname }}</div>
                    <div v-if="row.parentName" class="cm-reply-to">
                      ↳ 回复 <b>@{{ row.parentName }}</b>
                    </div>
                    <div v-else class="cm-email" :title="row.email">{{ row.email || '-' }}</div>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="160">
            <template #default="{ row }">
              <span class="cell-text">{{ row.articleTitle || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="280">
            <template #default="{ row }">
              <div class="cell-flex">
                <div class="cm-content" :title="row.content">{{ row.content }}</div>
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
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }"><span class="cell-text">{{ fmtDate(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button size="small" link type="primary" @click="openReply(row)">回复</el-button>
                <el-button v-if="row.status !== 1" size="small" link type="success" @click="approve(row)">通过</el-button>
                <el-button v-if="row.status !== 2" size="small" link type="warning" @click="spam(row)">垃圾</el-button>
                <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mobile-cards" v-loading="loading">
        <div v-for="row in list" :key="row.id" class="cm-card">
          <div class="cm-card-head">
            <el-avatar :src="row.avatar" :size="36" />
            <div class="cm-card-meta">
              <div class="cm-card-name">{{ row.nickname }}</div>
              <div class="cm-card-time">{{ fmtDate(row.createTime) }}</div>
            </div>
            <el-tag :type="row.status === 1 ? 'success' : (row.status === 2 ? 'danger' : 'warning')" size="small">
              {{ row.status === 1 ? '已通过' : (row.status === 2 ? '垃圾' : '待审核') }}
            </el-tag>
          </div>
          <div v-if="row.parentName" class="cm-card-reply">↳ 回复 <b>@{{ row.parentName }}</b></div>
          <div class="cm-card-content">{{ row.content }}</div>
          <div v-if="row.articleTitle" class="cm-card-source">来源: {{ row.articleTitle }}</div>
          <div class="cm-card-actions">
            <el-button size="small" type="primary" @click="openReply(row)">回复</el-button>
            <el-button v-if="row.status !== 1" size="small" type="success" @click="approve(row)">通过</el-button>
            <el-button v-if="row.status !== 2" size="small" type="warning" @click="spam(row)">垃圾</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" description="暂无评论" />
      </div>

      <div class="footer-bar" v-if="total > query.size">
        <el-pagination background layout="prev, pager, next, total"
          :current-page="query.page" :page-size="query.size" :total="total"
          @current-change="(p) => { query.page = p; reload(false) }" />
      </div>

      <el-dialog v-model="replyVisible" title="回复评论" width="min(520px, 92vw)" append-to-body>
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
import {
  adminComments, adminApproveComment, adminSpamComment, adminDeleteComment, adminCommentStats, adminReplyComment
} from '@/api/admin'
import { fmtDate } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const filter = ref('all')
const counts = ref({})

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
    await adminReplyComment(replyTarget.value.id, { content: replyContent.value })
    ElMessage.success('回复已发送')
    replyVisible.value = false
    reload()
  } catch (_) {
  } finally {
    replyLoading.value = false
  }
}

const query = reactive({ page: 1, size: 15 })

const reload = async (resetPage = true) => {
  loading.value = true
  const statusVal = filter.value === 'all' ? null :
                    filter.value === 'pending' ? 0 :
                    filter.value === 'approved' ? 1 : 2
  if (statusVal != null) query.status = statusVal; else delete query.status
  // 分页器回调里传 false,避免「点第 2 页 → 立刻被重置回第 1 页」的递归 bug
  if (resetPage) query.page = 1
  try {
    const resp = await adminComments(query)
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (e) {
    console.error('[AdminComments] 加载失败', e)
  }
  loading.value = false
  try {
    counts.value = (await adminCommentStats()).data || {}
  } catch (_) {}
}

const approve = async (row) => { await adminApproveComment(row.id); ElMessage.success('已通过'); reload() }
const spam = async (row) => { await adminSpamComment(row.id); ElMessage.success('已标记'); reload() }
const remove = async (row) => {
  await ElMessageBox.confirm('确定删除该评论?', '提示', { type: 'warning' })
  await adminDeleteComment(row.id); ElMessage.success('已删除'); reload()
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
.cm-author { display: flex; align-items: center; gap: 10px; }
.cm-author-info { flex: 1; min-width: 0; }
.cm-name { font-weight: 500; }
.cm-reply-to {
  font-size: 12px;
  color: var(--c-botany-600);
  margin-top: 2px;
  background: var(--c-botany-50);
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  b { color: var(--c-botany-700); font-weight: 600; }
}
.cm-email {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 2px;
}
.cm-content {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.footer-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.mobile-cards { display: none; }

.cm-card {
  border: 1px solid var(--c-line-soft);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: var(--c-paper);
}
.cm-card-head { display: flex; align-items: center; gap: 10px; }
.cm-card-meta { flex: 1; min-width: 0; }
.cm-card-name { font-weight: 500; }
.cm-card-time { font-size: 12px; color: var(--c-ink-soft); }
.cm-card-reply {
  font-size: 12px; color: var(--c-botany-600);
  background: var(--c-botany-50);
  padding: 4px 8px; border-radius: 4px;
  display: inline-block; margin: 8px 0;
  b { color: var(--c-botany-700); }
}
.cm-card-content {
  font-size: 14px; color: var(--c-ink-700); line-height: 1.6;
  margin: 8px 0;
}
.cm-card-source { font-size: 12px; color: var(--c-ink-soft); margin-bottom: 10px; }
.cm-card-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.cm-card-actions .el-button { flex: 1; }

@media (max-width: 900px) {
  .desktop-table { display: none; }
  .mobile-cards { display: block; }
}
</style>
