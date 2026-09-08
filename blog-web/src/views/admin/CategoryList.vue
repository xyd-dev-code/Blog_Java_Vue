<template>
  <div class="cat-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('分类管理') }}</span>
          <div class="toolbar">
            <el-input v-model="filters.keyword" placeholder="搜索" clearable inputmode="search" class="search-input" @keyup.enter="reload" @clear="reload" />
            <el-button @click="reload">查询</el-button>
            <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> {{ wx('新建分类') }}</el-button>
          </div>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (currentPage - 1) * pageSize + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="slug" label="Slug" width="180" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="articleCount" label="文章数" width="100" align="center" />
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button size="small" link @click="openForm(row)">编辑</el-button>
                <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-pagination
        v-if="total > pageSize"
        class="pager"
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="(p) => { currentPage = p; reload() }"
      />
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? wx('编辑分类') : wx('新建分类')" width="min(480px, 92vw)">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Slug">
          <el-input v-model="form.slug" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" placeholder="留空自动续号" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminCategories, adminCreateCategory, adminUpdateCategory, adminDeleteCategory } from '@/api/admin'
import { DEFAULT_CONTENT_ACCENT } from '@/themes/registry'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '' })
const form = reactive({ id: null, name: '', slug: '', description: '', color: DEFAULT_CONTENT_ACCENT, sortOrder: 0 })

const reload = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    const resp = await adminCategories(params)
    const data = resp.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('分类列表加载失败')
  } finally {
    loading.value = false
  }
}

const openForm = (row) => {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, name: '', slug: '', description: '', color: DEFAULT_CONTENT_ACCENT, sortOrder: 0 })
  dlg.value = true
}

const submit = async () => {
  if (!form.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  try {
    if (form.id) await adminUpdateCategory(form.id, form)
    else await adminCreateCategory(form)
    ElMessage.success('已保存')
    dlg.value = false
    reload()
  } catch (_) {}
  saving.value = false
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除分类 “${row.name}”?`, '提示', { type: 'warning' })
  await adminDeleteCategory(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; max-width: 100%; }
.toolbar .search-input { width: 200px; max-width: 100%; }
.toolbar .el-button + .el-button { margin-left: 0; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.pager { margin-top: 12px; justify-content: flex-end; display: flex; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>
