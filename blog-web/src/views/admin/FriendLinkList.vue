<template>
  <div class="fl-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>友链管理</span>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> 新建友链</el-button>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" row-key="id">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="站点" min-width="220">
            <template #default="{ row }">
              <div class="site-cell">
                <el-avatar :size="32" :src="row.avatar" shape="square">{{ row.name ? row.name[0] : '?' }}</el-avatar>
                <div>
                  <div class="site-name">{{ row.name }}</div>
                  <div class="site-url">{{ row.url }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="linkGroup" label="分组" width="90" />
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link type="success" v-if="row.status !== 1" @click="review(row, 1)">通过</el-button>
              <el-button size="small" link type="warning" v-if="row.status !== 2" @click="review(row, 2)">拒绝</el-button>
              <el-button size="small" link @click="openForm(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? '编辑友链' : '新建友链'" width="min(520px, 92vw)">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="链接" required>
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="form.avatar" placeholder="图片 URL" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.linkGroup" placeholder="选择分组">
            <el-option label="好友" value="好友" />
            <el-option label="网友" value="网友" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="选择状态">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
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
  adminFriendLinks, adminCreateFriendLink, adminUpdateFriendLink, adminDeleteFriendLink, adminReviewFriendLink
} from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const emptyForm = () => ({ id: null, name: '', url: '', avatar: '', description: '', email: '', linkGroup: '网友', status: 0, sortOrder: 0 })
const form = reactive(emptyForm())

const statusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[s] ?? '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] ?? 'info')

const reload = async () => {
  loading.value = true
  try {
    const resp = await adminFriendLinks({ size: 100 })
    list.value = resp.data?.records || []
  } catch (_) {}
  loading.value = false
}

const openForm = (row) => {
  Object.assign(form, emptyForm())
  if (row) Object.assign(form, row)
  dlg.value = true
}

const submit = async () => {
  if (!form.name || !form.name.trim()) return ElMessage.warning('请填写名称')
  if (!form.url || !form.url.trim()) return ElMessage.warning('请填写链接')
  saving.value = true
  try {
    if (form.id) await adminUpdateFriendLink(form.id, form)
    else await adminCreateFriendLink(form)
    ElMessage.success('已保存')
    dlg.value = false
    reload()
  } catch (_) {}
  saving.value = false
}

const review = async (row, status) => {
  try {
    await adminReviewFriendLink(row.id, status)
    ElMessage.success(status === 1 ? '已通过' : '已拒绝')
    reload()
  } catch (_) {}
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除友链 “${row.name}”?`, '提示', { type: 'warning' })
  await adminDeleteFriendLink(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.site-cell { display: flex; align-items: center; gap: 10px; }
.site-name { font-weight: 600; }
.site-url { font-size: 12px; color: var(--c-ink-soft); word-break: break-all; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>
