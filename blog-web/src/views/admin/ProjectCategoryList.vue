<template>
  <div class="cat-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>项目分类</span>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon> 新建分类</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="搜索名称 / slug" clearable style="width: 220px" @keyup.enter="reload" @clear="reload" />
        <el-button @click="reload">查询</el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" row-key="id">
          <el-table-column label="序号" width="64" align="center">
            <template #default="{ $index }">{{ (currentPage - 1) * pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="颜色" width="70" align="center">
            <template #default="{ row }">
              <span class="color-dot" :style="{ background: row.color || '#94a3b8' }"></span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="130" />
          <el-table-column prop="slug" label="slug" min-width="130">
            <template #default="{ row }"><code class="slug">{{ row.slug || '—' }}</code></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="项目数" width="90" align="center">
            <template #default="{ row }">{{ row.projectCount != null ? row.projectCount : 0 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '已下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <div class="cell-actions">
                <el-button size="small" link @click="openForm(row)">编辑</el-button>
                <el-button size="small" link @click="toggle(row)">{{ row.status === 1 ? '下架' : '发布' }}</el-button>
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

    <el-dialog v-model="dlg" :title="form.id ? '编辑分类' : '新建分类'" width="min(520px, 92vw)">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：前端 / AI / 后端" />
        </el-form-item>
        <el-form-item label="slug">
          <el-input v-model="form.slug" placeholder="留空则按名称自动生成，建议填英文" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker
            :model-value="form.color"
            color-format="hex"
            @update:model-value="(v) => form.color = v"
            @active-change="(v) => form.color = v"
            @change="(v) => form.color = v"
          />
          <span class="color-hint">{{ form.color || '#38bdf8' }}</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="分类的一句话说明" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusOn" active-text="已发布" inactive-text="已下架" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminProjectCategories, adminCreateProjectCategory, adminUpdateProjectCategory,
  adminUpdateProjectCategoryStatus, adminDeleteProjectCategory,
} from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '' })

const emptyForm = () => ({ id: null, name: '', slug: '', color: '#38bdf8', description: '', sortOrder: 0, status: 1 })
const form = reactive(emptyForm())
const statusOn = computed({
  get: () => form.status === 1,
  set: (v) => { form.status = v ? 1 : 0 },
})

const reload = async () => {
  loading.value = true
  try {
    // 加时间戳破浏览器 GET 缓存，否则保存后 reload 拿到的还是旧颜色
    const params = { page: currentPage.value, size: pageSize.value, _t: Date.now() }
    if (filters.keyword) params.keyword = filters.keyword
    const resp = await adminProjectCategories(params)
    const data = resp.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载分类失败')
  }
  loading.value = false
}

const openForm = (row) => {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, {
      id: row.id, name: row.name, slug: row.slug,
      color: row.color || '#38bdf8',
      description: row.description, sortOrder: row.sortOrder, status: row.status ?? 1,
    })
  }
  dlg.value = true
}

const submit = async () => {
  if (!form.name || !form.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  try {
    // 深拷贝，避免 Vue reactive Proxy 在 axios 序列化时丢失字段
    const payload = JSON.parse(JSON.stringify(form))
    console.log('[project-category] submit color =', payload.color)
    if (form.id) await adminUpdateProjectCategory(form.id, payload)
    else await adminCreateProjectCategory(payload)
    ElMessage.success('已保存')
    dlg.value = false
    reload()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
  saving.value = false
}

const toggle = async (row) => {
  const next = row.status === 1 ? 0 : 1
  try {
    // 走专用状态接口：只改 status，不会带上其它字段触发校验或误覆盖
    await adminUpdateProjectCategoryStatus(row.id, next)
    ElMessage.success(next === 1 ? '已发布' : '已下架')
    reload()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除分类 “${row.name}”? 该分类下的项目不会被删除，但会变为未分类。`,
      '提示',
      { type: 'warning' },
    )
  } catch (_) {
    return // 用户取消
  }
  try {
    await adminDeleteProjectCategory(row.id)
    ElMessage.success('已删除')
    reload()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

onMounted(reload)
</script>

<style scoped lang="scss">
.header-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.pager { margin-top: 12px; justify-content: flex-end; display: flex; }
.color-dot { display: inline-block; width: 18px; height: 18px; border-radius: 6px; box-shadow: 0 0 0 1px rgba(0,0,0,0.06); }
.slug { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; color: #0369a1; background: rgba(56,189,248,0.08); padding: 1px 6px; border-radius: 4px; }
.color-hint { margin-left: 10px; font-size: 12px; color: var(--c-ink-300); font-family: 'Consolas', monospace; }
</style>
