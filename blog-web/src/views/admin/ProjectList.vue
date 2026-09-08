<template>
  <div class="proj-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>{{ wx('项目管理') }}</span>
          <div class="toolbar">
            <el-input v-model="filters.keyword" placeholder="搜索" clearable inputmode="search" class="search-input" @keyup.enter="reload" @clear="reload" />
            <el-button @click="reload">查询</el-button>
            <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> {{ wx('新建项目') }}</el-button>
          </div>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" row-key="id">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (currentPage - 1) * pageSize + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="封面" width="96">
            <template #default="{ row }">
              <img v-if="row.coverUrl" :src="row.coverUrl" class="cover-thumb" alt="封面" />
              <span v-else class="cover-none">无</span>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="120">
            <template #default="{ row }"><span class="cell-text">{{ row.name }}</span></template>
          </el-table-column>
          <el-table-column label="分类" width="100">
            <template #default="{ row }"><span class="cell-text">{{ catName(row) }}</span></template>
          </el-table-column>
          <el-table-column prop="description" label="简介" show-overflow-tooltip min-width="200">
            <template #default="{ row }">{{ row.description }}</template>
          </el-table-column>
          <el-table-column :label="wx('技术栈')" min-width="200">
            <template #default="{ row }">
              <div class="stack-cell">
                <el-tag v-for="t in (row.stack || []).slice(0, 3)" :key="t" size="small" class="m-r">{{ t }}</el-tag>
                <el-tag v-if="(row.stack || []).length > 3" size="small" type="info" class="m-r"
                  :title="(row.stack || []).slice(3).join(' / ')">
                  +{{ (row.stack || []).length - 3 }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '已下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="70">
            <template #default="{ row }"><span class="cell-text">{{ row.sortOrder }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link @click="openForm(row)">编辑</el-button>
              <el-button size="small" link @click="toggle(row)">{{ row.status === 1 ? '下架' : '发布' }}</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dlg" :title="form.id ? wx('编辑项目') : wx('新建项目')" width="min(560px, 92vw)">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择项目分类" clearable filterable style="width: 100%">
            <el-option v-for="c in categoriesAll" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-uploader">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :before-upload="beforeCover"
              :http-request="uploadCover"
            >
              <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-prev" alt="封面预览" />
              <div v-else class="cover-placeholder">
                <el-icon><Plus /></el-icon>
                <span>上传封面</span>
              </div>
            </el-upload>
            <el-button v-if="form.coverUrl" size="small" text type="danger" class="cover-clear" @click="form.coverUrl = ''">移除</el-button>
          </div>
          <div class="cover-tip">建议 16:9，图片将上传至图床</div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="wx('技术栈')">
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
import { DEFAULT_CONTENT_ACCENT } from '@/themes/registry'
import {
  adminProjects, adminCreateProject, adminUpdateProject, adminDeleteProject, adminUpdateProjectStatus,
  adminProjectCategoriesAll, adminUpload,
} from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '' })

const categoriesAll = ref([])

const emptyForm = () => ({
  id: null, name: '', categoryId: null, description: '', stack: [],
  icon: 'Folder', color: DEFAULT_CONTENT_ACCENT, coverUrl: '', githubUrl: '', demoUrl: '', sortOrder: 0,
})
const form = reactive(emptyForm())

const catName = (row) => {
  if (!row.categoryId) return '未分类'
  const c = categoriesAll.value.find((x) => x.id === row.categoryId)
  return c ? c.name : '未分类'
}

const reload = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    const resp = await adminProjects(params)
    const data = resp.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch (_) {}
  loading.value = false
}

const fetchCategories = async () => {
  try {
    const resp = await adminProjectCategoriesAll()
    categoriesAll.value = resp.data || []
  } catch (_) { categoriesAll.value = [] }
}

const openForm = (row) => {
  Object.assign(form, emptyForm())
  if (row) Object.assign(form, row, { stack: Array.isArray(row.stack) ? [...row.stack] : [] })
  dlg.value = true
}

const beforeCover = (file) => {
  const okType = file.type.startsWith('image/')
  if (!okType) { ElMessage.error('请上传图片文件'); return false }
  const okSize = file.size / 1024 / 1024 < 3
  if (!okSize) { ElMessage.error('封面图不能超过 3MB'); return false }
  return true
}

const uploadCover = async (req) => {
  try {
    const resp = await adminUpload(req.file)
    form.coverUrl = resp.data?.url || resp.data?.link || ''
    if (!form.coverUrl) throw new Error('上传返回缺少 url')
    ElMessage.success('封面上传成功')
  } catch (_) {
    ElMessage.error('封面上传失败')
  }
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
  try {
    await ElMessageBox.confirm(`确定删除项目 “${row.name}”?`, '提示', { type: 'warning' })
  } catch (_) {
    return // 用户取消
  }
  try {
    await adminDeleteProject(row.id)
    ElMessage.success('已删除')
    reload()
  } catch (_) {}
}

onMounted(() => {
  fetchCategories()
  reload()
})
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; max-width: 100%; }
.toolbar .search-input { width: 200px; max-width: 100%; }
.toolbar .el-button + .el-button { margin-left: 0; }
.m-r { margin-right: 4px; }
.stack-cell {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 100%;
}
.stack-cell .el-tag {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.pager { margin-top: 12px; justify-content: flex-end; display: flex; }
.cover-thumb { width: 72px; height: 40px; object-fit: cover; border-radius: 6px; background: var(--c-skeleton-base); }
.cover-none { color: var(--c-ink-300); font-size: 12px; }

.cover-uploader { display: flex; align-items: center; gap: 12px; }
.cover-prev { width: 160px; height: 90px; object-fit: cover; border-radius: 10px; border: 1px solid var(--c-line); display: block; }
.cover-placeholder {
  width: 160px; height: 90px;
  border: 1px dashed var(--c-botany-300);
  border-radius: 10px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: var(--c-ink-300); cursor: pointer; background: var(--c-botany-50);
  transition: all 0.2s ease;
}
.cover-placeholder:hover { border-color: var(--c-botany-500); color: var(--c-botany-700); }
.cover-clear { margin-left: 4px; }
.cover-tip { font-size: 12px; color: var(--c-ink-300); margin-top: 6px; }
</style>
