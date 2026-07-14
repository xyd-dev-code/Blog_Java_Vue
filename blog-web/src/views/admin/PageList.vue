<template>
  <div class="page-admin-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>页面管理 (如: 关于、留言板模板)</span>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> 新建页面</el-button>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="slug" label="Slug" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link @click="openForm(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? '编辑页面' : '新建页面'" width="720px" top="6vh">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="留空自动生成" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="正文">
          <el-input v-model="form.contentMd" type="textarea" :rows="14" placeholder="支持 Markdown" />
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
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminPages, adminCreatePage, adminUpdatePage, adminDeletePage } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const form = reactive({ id: null, title: '', slug: '', contentMd: '', sortOrder: 0, status: 1 })

const reload = async () => {
  loading.value = true
  try {
    const resp = await adminPages({ size: 100 })
    list.value = resp.data?.records || []
  } catch (_) {}
  loading.value = false
}

const openForm = (row) => {
  if (row) Object.assign(form, { ...row, contentMd: row.contentMd || row.content || '' })
  else Object.assign(form, { id: null, title: '', slug: '', contentMd: '', sortOrder: 0, status: 1 })
  dlg.value = true
}

const submit = async () => {
  if (!form.title.trim()) return ElMessage.warning('请填写标题')
  saving.value = true
  try {
    if (form.id) await adminUpdatePage(form.id, form)
    else await adminCreatePage(form)
    ElMessage.success('已保存')
    dlg.value = false
    reload()
  } catch (_) {}
  saving.value = false
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除 “${row.title}”?`, '提示', { type: 'warning' })
  await adminDeletePage(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>