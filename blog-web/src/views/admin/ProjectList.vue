<template>
  <div class="proj-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>项目管理</span>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> 新建项目</el-button>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" row-key="id">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="kind" label="分类" width="90" />
          <el-table-column prop="description" label="简介" show-overflow-tooltip min-width="200" />
          <el-table-column label="技术栈" min-width="180">
            <template #default="{ row }">
              <el-tag v-for="t in (row.stack || [])" :key="t" size="small" class="m-r">{{ t }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '已下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link @click="openForm(row)">编辑</el-button>
              <el-button size="small" link @click="toggle(row)">{{ row.status === 1 ? '下架' : '发布' }}</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? '编辑项目' : '新建项目'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.kind" placeholder="选择分类">
            <el-option label="开源" value="开源" />
            <el-option label="工具" value="工具" />
            <el-option label="实验" value="实验" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-select v-model="form.stack" multiple filterable allow-create default-first-option placeholder="输入后回车添加">
          </el-select>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Folder" />
        </el-form-item>
        <el-form-item label="主题色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="form.githubUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="Demo">
          <el-input v-model="form.demoUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import {
  adminProjects, adminCreateProject, adminUpdateProject, adminDeleteProject, adminUpdateProjectStatus
} from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const emptyForm = () => ({ id: null, name: '', kind: '工具', description: '', stack: [], icon: 'Folder', color: '#38bdf8', githubUrl: '', demoUrl: '', sortOrder: 0 })
const form = reactive(emptyForm())

const reload = async () => {
  loading.value = true
  try {
    const resp = await adminProjects({ size: 100 })
    list.value = resp.data?.records || []
  } catch (_) {}
  loading.value = false
}

const openForm = (row) => {
  Object.assign(form, emptyForm())
  if (row) Object.assign(form, row, { stack: Array.isArray(row.stack) ? [...row.stack] : [] })
  dlg.value = true
}

const submit = async () => {
  if (!form.name || !form.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  try {
    if (form.id) await adminUpdateProject(form.id, form)
    else await adminCreateProject(form)
    ElMessage.success('已保存')
    dlg.value = false
    reload()
  } catch (_) {}
  saving.value = false
}

const toggle = async (row) => {
  const next = row.status === 1 ? 0 : 1
  try {
    await adminUpdateProjectStatus(row.id, next)
    ElMessage.success(next === 1 ? '已发布' : '已下架')
    reload()
  } catch (_) {}
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除项目 “${row.name}”?`, '提示', { type: 'warning' })
  await adminDeleteProject(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.m-r { margin-right: 4px; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>
