<template>
  <div class="tool-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工具管理</span>
          <div class="toolbar">
            <el-select v-model="filters.category" placeholder="分类" clearable style="width: 130px" @change="reload">
              <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px" @change="reload">
              <el-option label="正常" :value="1" />
              <el-option label="维护中" :value="2" />
              <el-option label="预告" :value="3" />
              <el-option label="下线" :value="0" />
            </el-select>
            <el-input v-model="filters.keyword" placeholder="搜索名称 / slug" clearable inputmode="search" style="width: 200px"
              @keyup.enter="reload" @clear="reload" />
            <el-button @click="reload">查询</el-button>
            <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> 新建工具</el-button>
          </div>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" row-key="id"
          @selection-change="onSel" :cell-style="{ verticalAlign: 'middle' }">
          <el-table-column type="selection" width="44" />
          <el-table-column label="序号" width="70" align="center">
            <template #default="{ $index }">
              <span class="sort-num">{{ (page - 1) * size + $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="130" />
          <el-table-column prop="slug" label="slug" width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <code class="slug">{{ row.slug }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ catLabel(row.category) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.type === 1" class="type-ext">外链</span>
              <span v-else class="type-int">内嵌</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-switch :model-value="row.status === 1" @change="(v) => onStatus(row, v ? 1 : 2)"
                inline-prompt active-text="正常" inactive-text="维护" />
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="70" align="center">
            <template #default="{ row }"><span class="muted">{{ row.viewCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="clickCount" label="点击" width="70" align="center">
            <template #default="{ row }"><span class="click-num">{{ row.clickCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button size="small" link @click="openForm(row)">编辑</el-button>
                <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="footer-bar">
        <div>
          <el-button size="small" type="danger" plain :disabled="!selected.length" @click="batchDelete">
            批量删除 ({{ selected.length }})
          </el-button>
        </div>
        <el-pagination
          v-if="total > size"
          background
          layout="prev, pager, next, total"
          :current-page="page" :page-size="size" :total="total"
          @current-change="(p) => { page = p; load() }"
        />
      </div>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dlg" :title="form.id ? '编辑工具' : '新建工具'" width="min(640px, 92vw)" destroy-on-close
      @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如 JSON 格式化" />
        </el-form-item>
        <el-form-item label="slug" prop="slug">
          <el-input v-model="form.slug" maxlength="50" placeholder="字母/数字/连字符,如 json-format / DeepSeek-AI" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button :value="0">同页内嵌</el-radio-button>
            <el-radio-button :value="1">外链</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="跳转URL" prop="url">
          <el-input v-model="form.url" maxlength="500"
            :placeholder="form.type === 1 ? 'https://example.com' : '/tools/<slug>'" />
        </el-form-item>
        <el-form-item label="图标">
          <div class="icon-field">
            <el-input v-model="form.icon" maxlength="1000" clearable
              placeholder="图标图片 URL（可上传或粘贴外链，如 https://.../xx.jpg）" />
            <el-upload
              class="icon-uploader"
              :show-file-list="false"
              accept="image/*"
              :before-upload="beforeIconUpload"
              :http-request="uploadIcon">
              <el-button :loading="iconUploading" type="primary" plain>上传图片</el-button>
            </el-upload>
          </div>
          <div v-if="isIconUrl(form.icon)" class="icon-preview">
            <img :src="form.icon" alt="图标预览" />
            <el-button size="small" text type="danger" @click="form.icon = ''">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200"
            placeholder="一两句话说明功能,卡片上展示" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="状态" style="width: 160px">
            <el-option label="正常" :value="1" />
            <el-option label="维护中" :value="2" />
            <el-option label="预告" :value="3" />
            <el-option label="下线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" controls-position="right" />
          <span class="muted" style="margin-left: 10px; font-size: 12px;">数值越小越靠前；拖拽排序已下线，请手动调整</span>
        </el-form-item>
        <el-form-item label="公告">
          <el-input v-model="form.announcement" maxlength="200"
            placeholder="可选,出现在 Hero 下方横幅;维护中状态自动带该字段" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminTools, adminTool, adminCreateTool, adminUpdateTool, adminDeleteTool, adminUpdateToolStatus,
  adminToolCategories, adminUpload, adminNextToolSort
} from '@/api/admin'

// 写死兜底：接口不可用时仍能保证表单/筛选可用
const defaultCategories = [
  { value: 'develop',   label: '开发工具' },
  { value: 'text',      label: '文本处理' },
  { value: 'image',     label: '图片处理' },
  { value: 'time',      label: '时间转换' },
  { value: 'crypto',    label: '加密解密' },
  { value: 'generator', label: '生成工具' },
  { value: 'daily',     label: '日常计算' }
]
const categoryOptions = ref([...defaultCategories])
const catLabel = (v) => (categoryOptions.value.find((c) => c.value === v) || {}).label || v || '-'

const loadCategories = async () => {
  try {
    const resp = await adminToolCategories()
    const arr = resp.data || []
    if (arr.length) {
      categoryOptions.value = arr.map(c => ({ value: c.code, label: c.name }))
    }
  } catch (_) {
    // 保持兜底列表
  }
}

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const selected = ref([])

const filters = reactive({ category: '', status: null, keyword: '' })

const onSel = (rows) => { selected.value = rows }

const load = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.category) params.category = filters.category
    if (filters.status != null && filters.status !== '') params.status = filters.status
    if (filters.keyword) params.keyword = filters.keyword
    const resp = await adminTools(params)
    list.value = resp.data?.records || []
    total.value = resp.data?.total || 0
  } catch (_) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}
const reload = () => { page.value = 1; load() }

const onStatus = async (row, status) => {
  try {
    await adminUpdateToolStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 1 ? '已上线' : '已转为维护中')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '更新失败')
  }
}

const remove = async (row) => {
  try { await ElMessageBox.confirm(`确定删除工具 “${row.name}”？`, '提示', { type: 'warning' }) }
  catch (_) { return }
  try { await adminDeleteTool(row.id); ElMessage.success('已删除'); reload() }
  catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '删除失败') }
}

const batchDelete = async () => {
  if (!selected.value.length) return
  try { await ElMessageBox.confirm(`确定批量删除 ${selected.value.length} 个工具？`, '提示', { type: 'warning' }) }
  catch (_) { return }
  let ok = 0
  for (const r of selected.value) {
    try { await adminDeleteTool(r.id); ok++ } catch (_) { /* 单条失败忽略 */ }
  }
  ElMessage.success(`已删除 ${ok} 个`)
  selected.value = []
  reload()
}

const dlg = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({
  id: null, name: '', slug: '', icon: 'Tools', category: 'develop',
  description: '', url: '', type: 0, status: 1, sortOrder: 0, announcement: ''
})
const form = reactive(emptyForm())
const rules = {
  name: [{ required: true, message: '请填写名称', trigger: 'blur' }],
  slug: [
    { required: true, message: '请填写 slug', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9-]+$/, message: '仅允许字母/数字/连字符', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const resetForm = () => { Object.assign(form, emptyForm()) }

// 图标：支持上传图片或填写链接。icon 字段存图片 URL
const iconUploading = ref(false)
const isIconUrl = (v) => /^(https?:\/\/|\/|data:)/.test(v || '')
const beforeIconUpload = (file) => {
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图标图片不能超过 2MB')
    return false
  }
  return true
}
const uploadIcon = async ({ file }) => {
  iconUploading.value = true
  try {
    const resp = await adminUpload(file)
    form.icon = resp.data?.url || resp.url || ''
    ElMessage.success('图标上传成功')
  } catch (_) {
    ElMessage.error('图标上传失败')
  } finally {
    iconUploading.value = false
  }
}
const openForm = async (row) => {
  resetForm()
  if (row) {
    try {
      const resp = await adminTool(row.id)
      Object.assign(form, resp.data || emptyForm())
    } catch (_) { /* 走 reload 已有的 list 数据 */ }
  } else {
    // 新建:默认序号接在现有最大序号之后,而非写死 0
    try {
      const resp = await adminNextToolSort()
      if (resp?.data != null) form.sortOrder = resp.data
    } catch (_) { /* 接口不可用时沿用 emptyForm 的兜底值 */ }
  }
  dlg.value = true
}
const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) await adminUpdateTool(form.id, form)
      else await adminCreateTool(form)
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

onMounted(() => { loadCategories(); load() })
</script>

<style scoped lang="scss">
.icon-field {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}
.icon-field .el-input {
  flex: 1;
}
.icon-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
.icon-preview img {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid var(--c-line, #e2e8f0);
  background: #f8fafc;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.card-header > span {
  font-weight: 600;
  font-size: 15px;
  color: var(--c-ink, #1e293b);
}
.toolbar {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  align-items: center;
}
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }

.sort-num { font-family: 'Consolas', monospace; color: #475569; font-size: 12px; }
.slug { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; color: #0369a1; background: rgba(56,189,248,0.08); padding: 1px 6px; border-radius: 4px; }
.type-ext {
  font-size: 12px; padding: 2px 8px; border-radius: 999px;
  background: rgba(251, 191, 36, 0.15); color: #854f0b;
}
.type-int {
  font-size: 12px; padding: 2px 8px; border-radius: 999px;
  background: rgba(56, 189, 248, 0.15); color: #0c4a6e;
}
.muted { color: #94a3b8; font-size: 12px; }
.click-num { color: #0ea5e9; font-weight: 500; font-size: 12px; }
.footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
</style>