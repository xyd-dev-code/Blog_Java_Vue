<template>
  <div class="article-list-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>文章列表</span>
          <div class="actions">
            <el-input v-model="query.keyword" placeholder="搜索标题" clearable
              class="search-input" @keyup.enter="reload" />
            <el-select v-model="query.status" placeholder="状态" clearable
              class="status-select" @change="reload">
              <el-option label="已发布" :value="1" />
              <el-option label="草稿" :value="0" />
            </el-select>
            <el-button type="primary" @click="$router.push('/admin/articles/new')">
              <el-icon><Plus /></el-icon>
              <span class="btn-text">新建文章</span>
            </el-button>
          </div>
        </div>
      </template>

      <!-- 桌面表格 -->
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" @selection-change="onSel" class="desktop-table">
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="标题" min-width="280">
            <template #default="{ row }">
              <a class="art-title" @click="$router.push(`/admin/articles/${row.id}/edit`)">{{ row.title }}</a>
              <div class="art-tags">
                <el-tag v-for="t in row.tags" :key="t.id" size="small" effect="plain" style="margin-right: 4px;">{{ t.name }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="category.name" label="分类" width="120">
            <template #default="{ row }">{{ row.category?.name || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)"
                inline-prompt active-text="发布" inactive-text="草稿" />
            </template>
          </el-table-column>
          <el-table-column label="置顶" width="80">
            <template #default="{ row }">
              <el-switch :model-value="row.isTop === 1" @change="(v) => toggleTop(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="推荐" width="80">
            <template #default="{ row }">
              <el-switch :model-value="row.isFeatured === 1" @change="(v) => toggleFeatured(row, v)" />
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="阅读" width="80" />
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link @click="$router.push(`/admin/articles/${row.id}/edit`)">编辑</el-button>
              <el-button size="small" link @click="preview(row)" v-if="row.status === 1">预览</el-button>
              <el-button size="small" link @click="openAdjustView(row)">阅读量</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 手机卡片 -->
      <div class="mobile-cards" v-loading="loading">
        <div v-for="row in list" :key="row.id" class="art-card">
          <div class="art-card-head" @click="$router.push(`/admin/articles/${row.id}/edit`)">
            <h3 class="art-card-title">{{ row.title }}</h3>
            <div class="art-card-tags">
              <el-tag v-for="t in row.tags" :key="t.id" size="small" effect="plain">{{ t.name }}</el-tag>
              <el-tag v-if="!row.tags?.length" size="small" type="info" effect="plain">{{ row.category?.name || '未分类' }}</el-tag>
            </div>
          </div>
          <div class="art-card-meta">
            <span class="meta-item">分类: {{ row.category?.name || '-' }}</span>
            <span class="meta-item">阅读: {{ row.viewCount || 0 }}</span>
            <span class="meta-item">{{ fmtDate(row.createTime) }}</span>
          </div>
          <div class="art-card-toggles">
            <div class="tog">
              <span>发布</span>
              <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)"
                inline-prompt active-text="是" inactive-text="否" />
            </div>
            <div class="tog">
              <span>置顶</span>
              <el-switch :model-value="row.isTop === 1" @change="(v) => toggleTop(row, v)" />
            </div>
            <div class="tog">
              <span>推荐</span>
              <el-switch :model-value="row.isFeatured === 1" @change="(v) => toggleFeatured(row, v)" />
            </div>
          </div>
          <div class="art-card-actions">
            <el-button size="small" type="primary" @click="$router.push(`/admin/articles/${row.id}/edit`)">编辑</el-button>
            <el-button size="small" v-if="row.status === 1" @click="preview(row)">预览</el-button>
            <el-button size="small" @click="openAdjustView(row)">阅读量</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" description="暂无文章" />
      </div>

      <div class="footer-bar">
        <div>
          <el-button size="small" type="danger" :disabled="!selected.length" @click="batchDelete">
            批量删除 ({{ selected.length }})
          </el-button>
        </div>
        <el-pagination background layout="prev, pager, next, total"
          :current-page="query.page" :page-size="query.size" :total="total"
          @current-change="(p) => { query.page = p; reload() }" />
      </div>
    </el-card>

    <el-dialog v-model="adjustDialog.open" title="调整阅读量" width="420px" destroy-on-close
      append-to-body>
      <div class="adjust-meta" v-if="adjustDialog.target">
        《{{ adjustDialog.target.title }}》当前阅读量：
        <b>{{ adjustDialog.target.viewCount || 0 }}</b>
      </div>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="调整方式">
          <el-radio-group v-model="adjustDialog.mode">
            <el-radio-button value="delta">按增量(可正可负)</el-radio-button>
            <el-radio-button value="set">直接设为</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="adjustDialog.mode === 'set' ? '目标阅读量' : '调整量 (负数表示减少)'">
          <el-input-number v-model="adjustDialog.value" :min="adjustDialog.mode === 'set' ? 0 : null"
            :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="adjustDialog.loading" @click="submitAdjust">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminArticles, adminDeleteArticle, adminBatchDeleteArticles, adminUpdateArticleStatus, adminUpdateArticleTop, adminUpdateArticleFeatured, adminSetArticleViewCount, adminAdjustArticleViewCount } from '@/api/admin'
import { fmtDate } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])

const adjustDialog = reactive({
  open: false,
  loading: false,
  target: null,
  mode: 'delta',
  value: 0
})

const openAdjustView = (row) => {
  adjustDialog.target = row
  adjustDialog.mode = 'delta'
  adjustDialog.value = 0
  adjustDialog.open = true
}

const submitAdjust = async () => {
  const row = adjustDialog.target
  if (!row) return
  const n = Number(adjustDialog.value)
  if (!Number.isFinite(n)) {
    ElMessage.error('请输入有效数字')
    return
  }
  adjustDialog.loading = true
  try {
    if (adjustDialog.mode === 'set') {
      await adminSetArticleViewCount(row.id, n)
      row.viewCount = n
      ElMessage.success(`已设阅读量为 ${n}`)
    } else {
      await adminAdjustArticleViewCount(row.id, n)
      row.viewCount = Math.max(0, (row.viewCount || 0) + n)
      ElMessage.success(`已按 ${n > 0 ? '+' : ''}${n} 调整阅读量`)
    }
    adjustDialog.open = false
  } catch (_) {
    // request.js 拦截器已经 toast 了
  } finally {
    adjustDialog.loading = false
  }
}

const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const reload = async () => {
  loading.value = true
  try {
    const resp = await adminArticles(query)
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (_) {}
  loading.value = false
}

const onSel = (rows) => { selected.value = rows }

const toggleStatus = async (row, v) => {
  await adminUpdateArticleStatus(row.id, v ? 1 : 0)
  row.status = v ? 1 : 0
  ElMessage.success('已更新')
}

const toggleTop = async (row, v) => {
  await adminUpdateArticleTop(row.id, v ? 1 : 0)
  row.isTop = v ? 1 : 0
  ElMessage.success('置顶状态已更新')
}

const toggleFeatured = async (row, v) => {
  await adminUpdateArticleFeatured(row.id, v ? 1 : 0)
  row.isFeatured = v ? 1 : 0
  ElMessage.success('推荐状态已更新')
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除《${row.title}》？`, '提示', { type: 'warning' })
  await adminDeleteArticle(row.id)
  ElMessage.success('已删除')
  reload()
}

const batchDelete = async () => {
  await ElMessageBox.confirm(`确定批量删除 ${selected.value.length} 篇文章？`, '提示', { type: 'warning' })
  await adminBatchDeleteArticles(selected.value.map(s => s.id))
  ElMessage.success('已删除')
  reload()
}

const preview = (row) => {
  window.open(`/articles/${row.slug}`, '_blank')
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
.actions { display: flex; gap: 12px; flex-wrap: wrap; }
.search-input { width: 240px; }
.status-select { width: 120px; }

.art-title { font-weight: 500; color: var(--c-ink); cursor: pointer; }
.art-title:hover { color: var(--c-botany-500); }
.art-tags { margin-top: 4px; }
.footer-bar {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  flex-wrap: wrap; gap: 12px;
}

.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.mobile-cards { display: none; }

.art-card {
  border: 1px solid var(--c-line-soft);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fff;
}
.art-card-head { cursor: pointer; }
.art-card-title {
  font-family: var(--font-serif); font-size: 17px; font-weight: 600;
  margin: 0 0 6px; color: var(--c-ink);
}
.art-card-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.art-card-meta {
  display: flex; flex-wrap: wrap; gap: 4px 12px;
  font-size: 12px; color: var(--c-ink-soft);
  padding: 8px 0; border-top: 1px dashed var(--c-line-soft); border-bottom: 1px dashed var(--c-line-soft);
  margin: 10px 0;
}
.art-card-toggles {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 8px; margin-bottom: 10px;
}
.tog {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  font-size: 12px; color: var(--c-ink-soft);
}
.art-card-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.art-card-actions .el-button { flex: 1; }

.adjust-meta {
  font-size: 13px;
  color: var(--c-ink-soft);
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--c-botany-50);
  border-radius: 8px;
}
.adjust-meta b { color: var(--c-ink); font-weight: 600; }

@media (max-width: 900px) {
  .desktop-table { display: none; }
  .mobile-cards { display: block; }
  .search-input { width: 100%; }
  .status-select { width: 110px; }
}
@media (max-width: 480px) {
  .art-card-toggles { grid-template-columns: 1fr 1fr 1fr; }
}
</style>
