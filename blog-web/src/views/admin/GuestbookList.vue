<template>
  <div class="gb-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>留言管理</span>
          <el-radio-group v-model="filter" @change="reload">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pending">待审核 ({{ counts.pending || 0 }})</el-radio-button>
            <el-radio-button value="approved">已通过</el-radio-button>
            <el-radio-button value="spam">垃圾</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="留言人" width="200">
            <template #default="{ row }">
              <div class="gb-author">
                <el-avatar :src="row.avatar" :size="32" />
                <div class="gb-author-info">
                  <div class="gb-name">{{ row.nickname }}</div>
                  <div v-if="row.parentName" class="gb-reply-to">
                    ↳ 回复 <b>@{{ row.parentName }}</b>
                  </div>
                  <div v-else class="gb-email">{{ row.email || '-' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="300">
            <template #default="{ row }">
              <div class="gb-content">{{ row.content }}</div>
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
            <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button v-if="row.status !== 1" size="small" link type="success" @click="approve(row)">通过</el-button>
              <el-button v-if="row.status !== 2" size="small" link type="warning" @click="spam(row)">垃圾</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="footer-bar" v-if="total > query.size">
        <el-pagination background layout="prev, pager, next, total"
          :current-page="query.page" :page-size="query.size" :total="total"
          @current-change="(p) => { query.page = p; reload() }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminGuestbook, adminApproveGuestbook, adminSpamGuestbook, adminDeleteGuestbook, adminGuestbookStats
} from '@/api/admin'
import { fmtDate } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const filter = ref('all')
const counts = ref({})

const query = reactive({ page: 1, size: 15, status: '' })

const reload = async () => {
  loading.value = true
  query.status = filter.value === 'all' ? '' :
                 filter.value === 'pending' ? 0 :
                 filter.value === 'approved' ? 1 : 2
  query.page = 1
  try {
    const resp = await adminGuestbook(query)
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (_) {}
  loading.value = false
  try {
    counts.value = (await adminGuestbookStats()).data || {}
  } catch (_) {}
}

const approve = async (row) => { await adminApproveGuestbook(row.id); ElMessage.success('已通过'); reload() }
const spam = async (row) => { await adminSpamGuestbook(row.id); ElMessage.success('已标记'); reload() }
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
.footer-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
</style>
