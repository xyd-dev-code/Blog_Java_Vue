<template>
  <div class="article-edit">
    <el-card>
      <template #header>
        <div class="edit-header">
          <span>{{ isEdit ? '编辑文章' : '新建文章' }}</span>
          <div>
            <el-button @click="goBack">返回</el-button>
            <el-button type="warning" plain @click="save(0)" :loading="saving">保存草稿</el-button>
            <el-button type="primary" @click="save(1)" :loading="saving">发布</el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="给文章起个名字" size="large" maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :xs="24" :md="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%" filterable>
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="16">
            <el-form-item label="标签">
              <el-select v-model="form.tagIds" multiple placeholder="选择已有标签或输入新名称回车创建" style="width: 100%" filterable allow-create default-first-option :loading="tagsCombo.creating.value">
                <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :xs="24" :md="8">
            <el-form-item label="封面">
              <div class="cover-area">
                <div class="cover-row">
                  <div class="cover-tabs">
                    <span
                      :class="['cover-tab', { active: coverMode === 'upload' }]"
                      @click="coverMode = 'upload'"
                    >上传图片</span>
                    <span
                      :class="['cover-tab', { active: coverMode === 'url' }]"
                      @click="coverMode = 'url'"
                    >填写 URL</span>
                  </div>
                  <div v-if="coverMode === 'upload'" class="cover-upload">
                    <input
                      ref="coverInput"
                      type="file"
                      accept="image/*"
                      style="display:none"
                      @change="onCoverFileChange"
                    />
                    <el-button size="small" @click="$refs.coverInput.click()" :loading="coverUploading">
                      选择图片
                    </el-button>
                  </div>
                  <div v-else class="cover-url">
                    <el-input v-model="form.coverImage" placeholder="封面图片 URL" size="small" />
                  </div>
                </div>
                <div v-if="form.coverImage" class="cover-preview">
                  <img :src="form.coverImage" alt="封面预览" />
                  <el-button
                    class="cover-clear"
                    size="small"
                    type="danger"
                    text
                    @click="form.coverImage = ''"
                    title="清除封面"
                  >&times;</el-button>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="200" show-word-limit
            placeholder="可选，留空则自动从正文截取" />
        </el-form-item>
        <el-form-item label="正文">
          <MdEditor
            v-model="markdown"
            v-bind="editorConfig"
            style="min-height: 700px;"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :xs="24" :md="8">
            <el-form-item label="置顶">
              <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="推荐">
              <el-switch v-model="form.isFeatured" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="允许评论">
              <el-switch v-model="form.allowComment" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { adminArticleById, adminCreateArticle, adminUpdateArticle, adminUploadBase64 } from '@/api/admin'
import { categoriesAll, tagsAll } from '@/api/front'
import { useArticleTags } from '@/composables/useArticleTags'

const route = useRoute()
const router = useRouter()

// route.params.id 路由配置上是 String,但后端要 Long。统一在组件入口正规化一次。
// 容错: '/admin/articles//edit'、'/admin/articles/undefined/edit' 这种退化情况直接判为新建。
const editorId = computed(() => {
  const raw = route.params.id
  if (raw === undefined || raw === null || raw === '') return null
  const s = String(raw).trim()
  if (!s || s === 'undefined' || s === 'null') return null
  const n = Number(s)
  return Number.isFinite(n) && n > 0 ? n : null
})
const isEdit = computed(() => editorId.value !== null)
const saving = ref(false)

const categories = ref([])
const tags = ref([])

const tagsCombo = useArticleTags(tags)

const form = reactive({
  title: '',
  categoryId: null,
  tagIds: [],
  coverImage: '',
  summary: '',
  isTop: 0,
  isFeatured: 0,
  allowComment: 1
})

const coverMode = ref('upload')
const coverUploading = ref(false)
const coverInput = ref(null)

// =============================
// md-editor-v3 编辑器（仿 CSDN 风）
// =============================

const markdown = ref('')

// File → base64 dataURL，给 MdEditor 的 onUploadImg 钩子上传七牛
const fileToDataURL = (file) => new Promise((resolve, reject) => {
  const r = new FileReader()
  r.onload = () => resolve(r.result)
  r.onerror = reject
  r.readAsDataURL(file)
})

// "是否有未保存改动":任一字段相对初值变动即视为脏。
// 不再用定时器静默写草稿——避免打开页面就被无声改成草稿态。
const dirty = ref(false)
const baselineSnapshot = ref('')

const currentSnapshot = () => JSON.stringify({
  title: form.title,
  categoryId: form.categoryId,
  tagIds: [...(form.tagIds || [])].sort((a, b) => Number(a) - Number(b)),
  coverImage: form.coverImage,
  summary: form.summary,
  content: markdown.value
})

const recomputeDirty = () => {
  if (!baselineSnapshot.value) return
  dirty.value = currentSnapshot() !== baselineSnapshot.value
}

watch(markdown, recomputeDirty)
watch(() => form.title, recomputeDirty)
watch(() => form.categoryId, recomputeDirty)
watch(() => form.tagIds, recomputeDirty, { deep: true })
watch(() => form.coverImage, recomputeDirty)
watch(() => form.summary, recomputeDirty)

// 浏览器 beforeunload 提示(关闭/刷新页时)
const beforeUnloadHandler = (e) => {
  if (!dirty.value) return undefined
  e.preventDefault()
  e.returnValue = '您有未保存的修改,确认离开吗?'
  return e.returnValue
}

// 应用内导航:点击"返回"或 router 跳转时提示
const confirmLeave = async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('您有未保存的修改,确认离开吗?', '提示', { type: 'warning' })
    return true
  } catch (_) {
    return false
  }
}
const goBack = async () => {
  if (await confirmLeave()) router.push('/admin/articles')
}

const editorConfig = {
  theme: 'light',
  preview: true,
  showTableOfContents: true,
  tableOfContents: { scrollElement: 'html' },
  previewTheme: 'default',
  codeTheme: 'atom-one-light',
  showToolbarName: true,
  toolbars: [
    'bold', 'underline', 'italic', 'strikeThrough', '-',
    'title', 'sub', 'sup',
    'quote', 'unorderedList', 'orderedList', 'task', '-',
    'code', 'codeRow', 'link', 'image', 'table', '-',
    'katex', '-',
    'preview', 'htmlPreview', 'catalog'
  ],
  onUploadImg: async (files, callback) => {
    try {
      const urls = await Promise.all(files.map(async (file) => {
        const resp = await adminUploadBase64(await fileToDataURL(file))
        return resp.data?.url || resp.url || ''
      }))
      callback(urls.filter(Boolean))
    } catch (_) {
      ElMessage.error('图片上传失败')
    }
  }
}

// =============================
// 保存
// =============================

const save = async (status) => {
  if (!form.title.trim()) return ElMessage.warning('请填写标题')
  if (!markdown.value.trim()) return ElMessage.warning('请填写正文')

  saving.value = true
  try {
    const resolvedTagIds = await tagsCombo.resolve(form.tagIds)
    const cleanTagIds = resolvedTagIds.filter(v => v !== null && v !== '' && !Number.isNaN(Number(v)))
    const payload = { ...form, tagIds: cleanTagIds, content: markdown.value, status }
    if (isEdit.value) {
      if (editorId.value === null) {
        ElMessage.error('无效的文章 id,无法保存。请返回列表重试。')
        return
      }
      await adminUpdateArticle(editorId.value, payload)
      ElMessage.success('已更新')
    } else {
      await adminCreateArticle(payload)
      ElMessage.success('已创建')
    }
    dirty.value = false
    router.push('/admin/articles')
  } catch (_) {}
  saving.value = false
}

// =============================
// 封面上传
// =============================

const onCoverFileChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    const resp = await adminUploadBase64(await fileToDataURL(file))
    form.coverImage = resp.data?.url || resp.url || ''
    ElMessage.success('封面上传成功')
  } catch (_) {
    ElMessage.error('上传失败')
  } finally {
    coverUploading.value = false
    // 重置 file input，允许重复选择同一文件
    if (coverInput.value) coverInput.value.value = ''
  }
}

// =============================
// 生命周期
// =============================

onMounted(async () => {
  try {
    categories.value = (await categoriesAll()).data || []
    tags.value = (await tagsAll()).data || []
  } catch (_) {}

  if (isEdit.value) {
    // 编辑模式下也校验一次 id,防止 /articles//edit 这种退化路径进入分支后崩
    if (editorId.value === null) {
      ElMessage.error('无效的文章 id')
      router.replace('/admin/articles')
      return
    }
    try {
      const resp = await adminArticleById(editorId.value)
      const article = resp.data
      Object.assign(form, {
        ...article,
        tagIds: (article.tags || []).map(t => t.id)
      })
      if (article.content) {
        markdown.value = article.content
      }
    } catch (_) {}
  }

  // 装载完成后再记录 baseline,避免组件初始化触发的写入误判为"脏"
  await nextTick()
  baselineSnapshot.value = currentSnapshot()
})

window.addEventListener('beforeunload', beforeUnloadHandler)
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnloadHandler)
})
</script>

<style scoped lang="scss">
.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ---- 编辑器容器 ---- */

.editor-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

</style>

<!-- 非 scoped 样式（弹出层等） -->
<style lang="scss">
/* ---- 封面区域 ---- */

.cover-area {
  width: 100%;
}

.cover-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.cover-tabs {
  display: flex;
  gap: 0;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  width: fit-content;
}

.cover-tab {
  padding: 2px 10px;
  font-size: 12px;
  cursor: pointer;
  color: #909399;
  background: #f5f7fa;
  transition: all 0.2s;
  user-select: none;
  line-height: 24px;

  &:first-child { border-right: 1px solid #dcdfe6; }

  &.active {
    color: #409eff;
    background: #ecf5ff;
    font-weight: 500;
  }

  &:hover:not(.active) { color: #606266; }
}

.cover-upload {
  display: flex;
  align-items: center;
}

.cover-url {
  display: flex;
  align-items: center;
  width: 220px;
}

.cover-preview {
  position: relative;
  margin-top: 8px;
  width: 120px;
  height: 68px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--c-line-soft);
  background: var(--c-line-soft);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .cover-clear {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 18px;
    height: 18px;
    padding: 0;
    font-size: 16px;
    line-height: 1;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    display: none;
    align-items: center;
    justify-content: center;

    &:hover { background: rgba(0, 0, 0, 0.7); }
  }

  &:hover .cover-clear { display: flex; }
}

</style>
