<template>
  <div class="tag-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('标签管理') }}</span>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> {{ wx('新建标签') }}</el-button>
        </div>
      </template>
      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="搜索" clearable inputmode="search" style="width: 200px" @keyup.enter="reload" @clear="reload" />
        <el-button @click="reload">查询</el-button>
      </div>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (currentPage - 1) * pageSize + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="slug" label="Slug" width="180" />
          <el-table-column prop="articleCount" label="文章数" width="100" align="center" />
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

    <el-dialog v-model="dlg" :title="form.id ? wx('编辑标签') : wx('新建标签')" width="min(420px, 92vw)">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="留空自动生成" /></el-form-item>
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
import { adminTags, adminCreateTag, adminUpdateTag, adminDeleteTag } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '' })
const form = reactive({ id: null, name: '', slug: '' })

const reload = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    const resp = await adminTags(params)
    const data = resp.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch (_) {}
  loading.value = false
}

const openForm = (row) => {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, name: '', slug: '' })
  dlg.value = true
}

const submit = async () => {
  if (!form.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  try {
    if (form.id) await adminUpdateTag(form.id, form)
    else await adminCreateTag(form)
    ElMessage.success('已保存')
    dlg.value = false
    reload()
  } catch (_) {}
  saving.value = false
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除标签 “${row.name}”?`, '提示', { type: 'warning' })
  await adminDeleteTag(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.pager { margin-top: 12px; justify-content: flex-end; display: flex; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>