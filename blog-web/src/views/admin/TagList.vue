<template>
  <div class="tag-admin-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>标签管理</span>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> 新建标签</el-button>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="slug" label="Slug" />
          <el-table-column prop="articleCount" label="文章数" width="100" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link @click="openForm(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? '编辑标签' : '新建标签'" width="420px">
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
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminTags, adminCreateTag, adminUpdateTag, adminDeleteTag } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const form = reactive({ id: null, name: '', slug: '' })

const reload = async () => {
  loading.value = true
  try {
    const resp = await adminTags({ size: 200 })
    list.value = resp.data?.records || []
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
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>