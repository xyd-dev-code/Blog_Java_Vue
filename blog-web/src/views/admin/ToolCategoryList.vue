<template>
  <div class="tool-cat-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('工具分类管理') }}</span>
          <div class="header-actions">
            <div class="toolbar">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索分类键 / 名称"
                clearable
                inputmode="search"
                @keyup.enter="reload"
                @clear="reload"
              />
              <el-button @click="reload">查询</el-button>
            </div>
            <el-button plain @click="openLogs"><el-icon><Tickets /></el-icon> 操作日志</el-button>
            <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> {{ wx('新建分类') }}</el-button>
          </div>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="displayed" v-loading="loading" row-key="id" style="width: 100%">
          <el-table-column label="序号" width="70" align="center">
            <template #default="{ $index }">
              <span class="sort-num">{{ (page - 1) * size + $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="分类键" width="180">
            <template #default="{ row }">
              <code class="cat-code">{{ row.code }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="显示名称" min-width="160" />
          <el-table-column label="工具数" width="90" align="center">
            <template #default="{ row }">
              <el-tag type="info" effect="plain" round>
                {{ row.toolCount ?? 0 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status !== 0"
                inline-prompt
                active-text="正常"
                inactive-text="下线"
                @change="(v) => toggleStatus(row, v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button size="small" link @click="openForm(row)">编辑</el-button>
                <el-tooltip v-if="row.toolCount > 0" :content="`该分类下还有 ${row.toolCount} 个工具,无法删除`" placement="top">
                  <span>
                    <el-button size="small" link type="danger" disabled>删除</el-button>
                  </span>
                </el-tooltip>
                <el-button v-else size="small" link type="danger" @click="remove(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager" v-if="filtered.length > size">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="page"
          :page-size="size"
          :total="filtered.length"
          @current-change="(p) => { page = p }"
        />
      </div>

    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? wx('编辑分类') : wx('新建分类')" width="min(480px, 92vw)" destroy-on-close @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="显示名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如 开发工具、AI 工具" />
        </el-form-item>
        <el-form-item label="分类键" prop="code">
          <el-input v-model="form.code" maxlength="30" placeholder="字母/数字/连字符,如 develop / ai-tool" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 操作日志抽屉 -->
    <el-drawer v-model="logVisible" title="操作日志 · 工具分类" size="420px" direction="rtl">
      <div v-loading="logLoading" class="log-list">
        <el-empty v-if="!logLoading && logs.length === 0" :description="wx('暂无操作记录')" />
        <div v-for="log in logs" :key="log.id" class="log-item">
          <div class="log-head">
            <el-tag size="small" :type="actionType(log.action)" effect="light">
              {{ actionLabel(log.action) }}
            </el-tag>
            <span class="log-time">{{ formatTime(log.createTime) }}</span>
          </div>
          <div class="log-target">{{ log.target }}</div>
          <div class="log-detail">{{ log.detail }}</div>
          <div class="log-meta">操作人:{{ log.operator }} · IP:{{ log.ip }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Tickets } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminToolCategories,
  adminToolCategory,
  adminCreateToolCategory,
  adminUpdateToolCategory,
  adminDeleteToolCategory,
  adminSetToolCategoryStatus,
  adminToolCategoryLogs
} from '@/api/admin'

const all = ref([])
const filtered = computed(() => {
  const kw = (filters.keyword || '').trim().toLowerCase()
  if (!kw) return all.value
  return all.value.filter(
    (it) => (it.name || '').toLowerCase().includes(kw) || (it.code || '').toLowerCase().includes(kw)
  )
})
const page = ref(1)
const size = ref(10)
const displayed = computed(() => {
  const start = (page.value - 1) * size.value
  return filtered.value.slice(start, start + size.value)
})
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const formRef = ref(null)
const filters = reactive({ keyword: '' })
const form = reactive({ id: null, code: '', name: '', sortOrder: 0 })

// 操作日志抽屉
const logVisible = ref(false)
const logLoading = ref(false)
const logs = ref([])

const rules = {
  code: [
    { required: true, message: '请填写分类键', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9-]+$/, message: '仅允许字母、数字、连字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请填写显示名称', trigger: 'blur' }]
}

const resetForm = () => {
  Object.assign(form, { id: null, code: '', name: '', sortOrder: 0 })
  formRef.value?.resetFields?.()
}

const reload = async () => {
  loading.value = true
  page.value = 1
  try {
    const resp = await adminToolCategories()
    all.value = resp.data || []
  } catch (e) {
    ElMessage.error('分类列表加载失败')
  } finally {
    loading.value = false
  }
}

const openForm = async (row) => {
  resetForm()
  if (row) {
    try {
      const resp = await adminToolCategory(row.id)
      Object.assign(form, resp.data || {})
    } catch (_) {
      Object.assign(form, row)
    }
  }
  dlg.value = true
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) await adminUpdateToolCategory(form.id, form)
      else await adminCreateToolCategory(form)
      ElMessage.success('已保存')
      dlg.value = false
      reload()
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const remove = async (row) => {
  try { await ElMessageBox.confirm(`确定删除分类 “${row.name}”？`, '提示', { type: 'warning' }) }
  catch (_) { return }
  try {
    await adminDeleteToolCategory(row.id)
    ElMessage.success('已删除')
    reload()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

// 切换状态(正常/下线)
const toggleStatus = async (row, val) => {
  const next = val ? 1 : 0
  try {
    await adminSetToolCategoryStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '已设为正常' : '已下线')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '状态切换失败')
  }
}

// 打开操作日志抽屉
const openLogs = async () => {
  logVisible.value = true
  await loadLogs()
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const resp = await adminToolCategoryLogs(20)
    logs.value = resp.data || []
  } catch (_) {
    logs.value = []
  } finally {
    logLoading.value = false
  }
}

const actionLabel = (a) => ({ create: '新建', update: '编辑', delete: '删除', status: '状态' }[a] || a)
const actionType = (a) => ({ create: 'success', update: 'warning', delete: 'danger', status: 'info' }[a] || 'info')
const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 290px;
  max-width: 100%;
}
.toolbar .el-input { flex: 1; min-width: 0; }
.header-actions > .el-button + .el-button { margin-left: 0; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.sort-num { font-family: 'Consolas', 'Monaco', monospace; color: var(--c-ink-soft); font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.cat-code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: var(--c-botany-700);
  background: var(--c-botany-50);
  border: 1px solid var(--c-botany-100, rgba(var(--theme-primary-rgb), 0.25));
  padding: 1px 6px;
  border-radius: 4px;
}
.log-list { display: flex; flex-direction: column; gap: 12px; }
.log-item {
  padding: 12px 14px;
  border: 1px solid var(--c-line-soft, var(--c-line));
  border-radius: 10px;
  background: var(--c-paper, var(--c-white));
}
.log-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.log-time { font-size: 12px; color: var(--c-ink-soft, var(--c-ink-300)); font-family: 'Consolas', 'Monaco', monospace; }
.log-target { font-weight: 600; color: var(--c-ink, var(--c-ink-800)); font-size: 14px; margin-bottom: 2px; }
.log-detail { font-size: 13px; color: var(--c-ink-soft, var(--c-ink-400)); line-height: 1.6; }
.log-meta { font-size: 12px; color: var(--c-ink-soft, var(--c-ink-300)); margin-top: 6px; }
@media (max-width: 480px) { :deep(.el-dialog) { width: 92vw !important; } }
</style>
