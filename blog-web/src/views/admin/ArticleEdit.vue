<template>
  <div class="article-edit">
    <el-card>
      <template #header>
        <div class="edit-header">
          <span>{{ isEdit ? '编辑文章' : '新建文章' }}</span>
          <div>
            <el-button @click="$router.push('/admin/articles')">返回</el-button>
            <el-button type="warning" plain @click="save(0)" :loading="saving">保存草稿</el-button>
            <el-button type="primary" @click="save(1)" :loading="saving">发布</el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="给文章起个名字" size="large" />
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
              <el-select v-model="form.tagIds" multiple placeholder="选择标签" style="width: 100%" filterable allow-create>
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
          <div v-if="editor" class="editor-wrapper">
            <!-- 工具栏 -->
          <div class="tiptap-toolbar">
            <!-- 撤销/重做 -->
            <el-button-group class="toolbar-group">
              <el-button size="small" text @click="editor.chain().focus().undo().run()" title="撤销 (Ctrl+Z)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
              </el-button>
              <el-button size="small" text @click="editor.chain().focus().redo().run()" title="重做 (Ctrl+Shift+Z)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
              </el-button>
            </el-button-group>

            <span class="toolbar-divider" />

            <!-- 文本样式 -->
            <el-button-group class="toolbar-group">
              <el-button size="small" text :type="editor.isActive('bold') ? 'primary' : ''" @click="editor.chain().focus().toggleBold().run()" title="加粗 (Ctrl+B)"><b>B</b></el-button>
              <el-button size="small" text :type="editor.isActive('italic') ? 'primary' : ''" @click="editor.chain().focus().toggleItalic().run()" title="斜体 (Ctrl+I)"><i>I</i></el-button>
              <el-button size="small" text :type="editor.isActive('underline') ? 'primary' : ''" @click="editor.chain().focus().toggleUnderline().run()" title="下划线 (Ctrl+U)"><u>U</u></el-button>
              <el-button size="small" text :type="editor.isActive('strike') ? 'primary' : ''" @click="editor.chain().focus().toggleStrike().run()" title="删除线 (Ctrl+Shift+X)"><s>S</s></el-button>
            </el-button-group>

            <span class="toolbar-divider" />

            <!-- 标题 -->
            <el-select
              :model-value="headingLevel"
              size="small"
              style="width: 90px"
              placeholder="正文"
              title="段落 / 标题 (Ctrl+Alt+1~6)"
              @change="setHeading"
            >
              <el-option :value="0" label="正文" />
              <el-option :value="1" label="标题 1" />
              <el-option :value="2" label="标题 2" />
              <el-option :value="3" label="标题 3" />
              <el-option :value="4" label="标题 4" />
              <el-option :value="5" label="标题 5" />
              <el-option :value="6" label="标题 6" />
            </el-select>

            <span class="toolbar-divider" />

            <!-- 字体 -->
            <el-select
              :model-value="currentFontFamily"
              size="small"
              style="width: 110px"
              placeholder="字体"
              @change="setFontFamily"
            >
              <el-option value="" label="默认字体" />
              <el-option v-for="f in fontFamilies" :key="f.value" :value="f.value" :label="f.label"
                :style="{ fontFamily: f.value || 'inherit' }" />
            </el-select>

            <!-- 文字颜色 -->
            <el-popover
              :visible="colorPickerVisible"
              placement="bottom"
              :width="198"
              trigger="click"
            >
              <template #reference>
                <el-button size="small" text @click="colorPickerVisible = !colorPickerVisible" title="文字颜色" class="color-trigger-btn">
                  <span class="color-btn-label">A</span>
                  <span class="color-btn-bar" :style="{ backgroundColor: currentTextColor || '#333' }" />
                </el-button>
              </template>
              <div class="tiptap-color-panel">
                <div class="tiptap-color-grid">
                  <div v-for="c in colorPalette" :key="c" class="tiptap-color-cell"
                    :style="{ backgroundColor: c }" :title="c"
                    @click="applyColor(c)" />
                </div>
                <div class="tiptap-color-footer" @click="applyColor('')">恢复默认</div>
              </div>
            </el-popover>

            <!-- 高亮 -->
            <el-popover
              :visible="highlightPickerVisible"
              placement="bottom"
              :width="198"
              trigger="click"
            >
              <template #reference>
                <el-button size="small" text @click="highlightPickerVisible = !highlightPickerVisible" title="高亮" class="highlight-trigger-btn">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="2" fill="#FAAD14" stroke="#D48806" stroke-width="1"/><text x="12" y="16" text-anchor="middle" font-size="12" fill="#fff" font-weight="bold">H</text></svg>
                </el-button>
              </template>
              <div class="tiptap-color-panel">
                <div class="tiptap-color-grid">
                  <div v-for="c in highlightPalette" :key="c" class="tiptap-color-cell"
                    :style="{ backgroundColor: c }" :title="c"
                    @click="applyHighlight(c)" />
                </div>
                <div class="tiptap-color-footer" @click="applyHighlight('')">清除高亮</div>
              </div>
            </el-popover>

            <span class="toolbar-divider" />

            <!-- 引用 / 代码 -->
            <el-button-group class="toolbar-group">
              <el-button size="small" text :type="editor.isActive('blockquote') ? 'primary' : ''" @click="editor.chain().focus().toggleBlockquote().run()" title="引用 (Ctrl+Shift+B)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/></svg>
              </el-button>
              <el-button size="small" text :type="editor.isActive('code') ? 'primary' : ''" @click="editor.chain().focus().toggleCode().run()" title="行内代码 (Ctrl+E)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
              </el-button>
              <el-button size="small" text :type="editor.isActive('codeBlock') ? 'primary' : ''" @click="editor.chain().focus().toggleCodeBlock().run()" title="代码块 (Ctrl+Alt+C)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="20" rx="2"/><line x1="6" y1="6" x2="6" y2="18"/></svg>
              </el-button>
            </el-button-group>

            <span class="toolbar-divider" />

            <!-- 列表 -->
            <el-button-group class="toolbar-group">
              <el-button size="small" text :type="editor.isActive('bulletList') ? 'primary' : ''" @click="editor.chain().focus().toggleBulletList().run()" title="无序列表 (Ctrl+Shift+8)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="4" cy="6" r="1.5" fill="currentColor" stroke="none"/><circle cx="4" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="4" cy="18" r="1.5" fill="currentColor" stroke="none"/></svg>
              </el-button>
              <el-button size="small" text :type="editor.isActive('orderedList') ? 'primary' : ''" @click="editor.chain().focus().toggleOrderedList().run()" title="有序列表 (Ctrl+Shift+7)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><text x="2" y="15" font-size="10" fill="currentColor" stroke="none">1.</text></svg>
              </el-button>
              <el-button size="small" text :type="editor.isActive('taskList') ? 'primary' : ''" @click="editor.chain().focus().toggleTaskList().run()" title="任务列表 (Ctrl+Shift+9)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="5" width="5" height="5" rx="1"/><line x1="11" y1="7.5" x2="21" y2="7.5"/><rect x="3" y="14" width="5" height="5" rx="1"/><line x1="11" y1="16.5" x2="21" y2="16.5"/></svg>
              </el-button>
            </el-button-group>

            <span class="toolbar-divider" />

            <!-- 插入 -->
            <el-button-group class="toolbar-group">
              <el-button size="small" text @click="insertLink" title="插入链接 (Ctrl+K)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
              </el-button>
              <el-button size="small" text @click="insertImage" title="插入图片">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5" fill="currentColor" stroke="none"/><polyline points="21 15 16 10 5 21"/></svg>
              </el-button>
              <el-button size="small" text @click="insertTable" title="插入表格">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>
              </el-button>
              <el-button size="small" text @click="editor.chain().focus().setHorizontalRule().run()" title="分隔线">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="12" x2="20" y2="12"/></svg>
              </el-button>
            </el-button-group>
          </div>

          <!-- 编辑器 -->
          <editor-content :editor="editor" class="tiptap-editor" />
          </div>
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
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { TextStyle, Color, FontFamily } from '@tiptap/extension-text-style'
import Highlight from '@tiptap/extension-highlight'
import Underline from '@tiptap/extension-underline'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import { Table, TableRow, TableHeader, TableCell } from '@tiptap/extension-table'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import TurndownService from 'turndown'
import { gfm } from 'turndown-plugin-gfm'
import { renderMarkdown } from '@/utils/markdown'
import { adminArticleById, adminCreateArticle, adminUpdateArticle, adminUpload, adminUploadBase64 } from '@/api/admin'
import { categoriesAll, tagsAll } from '@/api/front'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const saving = ref(false)

const categories = ref([])
const tags = ref([])

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
// HTML ↔ Markdown 转换
// =============================

const turndown = new TurndownService({
  headingStyle: 'atx',
  bulletListMarker: '-',
  codeBlockStyle: 'fenced'
})
turndown.use(gfm)

// 保持 <span style="..."> 原样（颜色/字体/高亮）
turndown.addRule('styledSpan', {
  filter: (node) => {
    if (node.nodeName !== 'SPAN') return false
    try {
      return node.hasAttribute('style') &&
        (node.getAttribute('style') || '').includes('color') ||
        (node.getAttribute('style') || '').includes('font-family') ||
        (node.getAttribute('style') || '').includes('background')
    } catch (_) { return false }
  },
  replacement: (_content, node) => {
    try {
      return (node).outerHTML || ''
    } catch (_) { return '' }
  }
})

// =============================
// 工具栏交互状态（强制追踪）
// =============================

const toolbarTick = ref(0)
const bump = () => toolbarTick.value++

// =============================
// 颜色
// =============================

const colorPickerVisible = ref(false)
const colorPalette = [
  '#000000', '#434343', '#666666', '#999999', '#BFBFBF', '#D9D9D9',
  '#FF0000', '#FF4D4F', '#FA541C', '#FA8C16', '#FADB14', '#FAAD14',
  '#52C41A', '#A0D911', '#13C2C2', '#1890FF', '#2F54EB', '#722ED1',
  '#EB2F96', '#F5222D', '#D4380D', '#AD6800', '#5B8C00', '#135200'
]

const currentTextColor = computed(() => {
  toolbarTick.value
  return editor.value?.getAttributes('textStyle').color || ''
})

const applyColor = (color) => {
  colorPickerVisible.value = false
  if (!color) {
    editor.value?.chain().focus().unsetColor().run()
  } else {
    editor.value?.chain().focus().setColor(color).run()
  }
}

// =============================
// 高亮
// =============================

const highlightPickerVisible = ref(false)
const highlightPalette = [
  '#FFF566', '#FFD666', '#FFB366', '#FFA39E', '#FFBB96',
  '#B7EB8F', '#87E8DE', '#91D5FF', '#ADC6FF', '#D3ADF7',
  '#FFADD2', '#FFD591', '#FFE7BA', '#F4FFB8', '#D9F7BE'
]

const applyHighlight = (color) => {
  highlightPickerVisible.value = false
  if (!color) {
    editor.value?.chain().focus().unsetHighlight().run()
  } else {
    editor.value?.chain().focus().toggleHighlight({ color }).run()
  }
}

// =============================
// 字体
// =============================

const fontFamilies = [
  { label: '宋体', value: 'SimSun, 宋体, serif' },
  { label: '黑体', value: 'SimHei, 黑体, sans-serif' },
  { label: '微软雅黑', value: 'Microsoft YaHei, 微软雅黑, sans-serif' },
  { label: '楷体', value: 'KaiTi, 楷体, serif' },
  { label: '仿宋', value: 'FangSong, 仿宋, serif' },
  { label: 'Arial', value: 'Arial, sans-serif' },
  { label: 'Georgia', value: 'Georgia, serif' },
  { label: 'Verdana', value: 'Verdana, sans-serif' },
  { label: 'Consolas', value: 'Consolas, Courier New, monospace' },
  { label: 'Trebuchet MS', value: 'Trebuchet MS, sans-serif' }
]

const currentFontFamily = computed(() => {
  toolbarTick.value
  return editor.value?.getAttributes('textStyle').fontFamily || ''
})

const setFontFamily = (font) => {
  if (!font) {
    editor.value?.chain().focus().unsetFontFamily().run()
  } else {
    editor.value?.chain().focus().setFontFamily(font).run()
  }
}

// =============================
// 标题
// =============================

const headingLevel = computed(() => {
  toolbarTick.value
  for (let i = 1; i <= 6; i++) {
    if (editor.value?.isActive('heading', { level: i })) return i
  }
  return 0
})

const setHeading = (level) => {
  if (level === 0) {
    editor.value?.chain().focus().setParagraph().run()
  } else {
    editor.value?.chain().focus().toggleHeading({ level }).run()
  }
}

// =============================
// 链接 / 图片 / 表格 插入
// =============================

const insertLink = async () => {
  try {
    const { value: url } = await ElMessageBox.prompt('请输入链接地址', '插入链接', {
      inputValue: 'https://',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (url) {
      const prevUrl = editor.value?.getAttributes('link').href
      if (prevUrl) {
        editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
      }
      editor.value?.chain().focus().setLink({ href: url }).run()
    }
  } catch (_) {}
}

const insertImage = async () => {
  try {
    const { value: url } = await ElMessageBox.prompt('请输入图片地址', '插入图片', {
      inputValue: 'https://',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (url) {
      editor.value?.chain().focus().setImage({ src: url }).run()
    }
  } catch (_) {}
}

const insertTable = () => {
  editor.value?.chain().focus()
    .insertTable({ rows: 3, cols: 3, withHeaderRow: true })
    .run()
}

// =============================
// Tiptap 编辑器
// =============================

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      codeBlock: false,
      heading: { levels: [1, 2, 3, 4, 5, 6] },
    }),
    TextStyle,
    Color,
    FontFamily,
    Highlight.configure({ multicolor: true }),
    Underline,
    Link.configure({ openOnClick: false }),
    Image.configure({ inline: true }),
    Table.configure({ resizable: true }),
    TableRow,
    TableHeader,
    TableCell,
    TaskList,
    TaskItem.configure({ nested: true })
  ],
  editorProps: {
    handlePaste: (view, event) => {
      const items = event.clipboardData?.items
      if (!items) return false

      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        if (item.type.startsWith('image/')) {
          event.preventDefault()
          const file = item.getAsFile()
          if (!file) continue

          // 读到 base64，调后端接口上传到七牛云
          const reader = new FileReader()
          reader.onload = async () => {
            try {
              const resp = await adminUploadBase64(reader.result)
              const url = resp.data?.url || resp.url || ''
              if (url) {
                editor.value?.chain().focus().setImage({ src: url }).run()
                ElMessage.success('图片已上传')
              }
            } catch (_) {
              ElMessage.error('图片上传失败')
            }
          }
          reader.readAsDataURL(file)
          return true
        }
      }
      return false
    }
  },
  content: '',
  onUpdate: bump,
  onSelectionUpdate: bump
})

// =============================
// 保存
// =============================

const save = async (status) => {
  if (!form.title.trim()) return ElMessage.warning('请填写标题')

  const html = editor.value?.getHTML() || ''
  if (!html || html === '<p></p>') return ElMessage.warning('请填写正文')

  const markdown = turndown.turndown(html)

  saving.value = true
  try {
    const payload = { ...form, content: markdown, status }
    if (isEdit.value) {
      await adminUpdateArticle(route.params.id, payload)
      ElMessage.success('已更新')
    } else {
      await adminCreateArticle(payload)
      ElMessage.success('已创建')
    }
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
    const resp = await adminUpload(file)
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
    try {
      const resp = await adminArticleById(route.params.id)
      const article = resp.data
      Object.assign(form, {
        ...article,
        tagIds: (article.tags || []).map(t => t.id)
      })
      // Markdown → HTML → 编辑器
      if (article.content) {
        const html = renderMarkdown(article.content)
        editor.value?.commands.setContent(html)
        bump()
      }
    } catch (_) {}
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
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

/* ---- 工具栏 ---- */

.tiptap-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px 4px 0 0;
  background: #fafafa;
  margin-bottom: -1px;
  z-index: 10;
  position: sticky;
  top: 56px;
  width: 100%;
  box-sizing: border-box;
  box-shadow: 0 1px 0 #dcdfe6;
}

.toolbar-group {
  display: flex;
  align-items: center;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: #dcdfe6;
  margin: 0 4px;
  display: inline-block;
}

.color-trigger-btn {
  position: relative;
  width: auto;
  min-width: 28px;

  .color-btn-label {
    font-weight: bold;
    font-size: 13px;
  }

  .color-btn-bar {
    display: block;
    width: 14px;
    height: 3px;
    border-radius: 1px;
    margin-top: 1px;
    transition: background 0.2s;
  }
}

.highlight-trigger-btn {
  min-width: 28px;
}

/* ---- 编辑器内容区 ---- */

.tiptap-editor {
  width: 100%;
  display: block;
}

:deep(.tiptap-editor .ProseMirror) {
  width: 100%;
  min-height: calc(100vh - 360px);
  max-height: none;
  overflow-y: visible;
  padding: 16px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 0 0 4px 4px;
  outline: none;
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  box-sizing: border-box;

  &:focus {
      border-color: #409eff;
    }

    /* 标题 */
    h1 { font-size: 26px; margin: 16px 0 10px; }
    h2 { font-size: 22px; margin: 14px 0 8px; }
    h3 { font-size: 18px; margin: 12px 0 6px; }
    h4 { font-size: 16px; margin: 10px 0 6px; }
    h5 { font-size: 15px; margin: 8px 0 4px; }
    h6 { font-size: 14px; margin: 8px 0 4px; }

    /* 段落 */
    p { margin: 8px 0; }

    /* 引用 */
    blockquote {
      border-left: 3px solid #ddd;
      padding-left: 16px;
      margin: 12px 0;
      color: #666;
    }

    /* 代码 */
    code {
      background: #f5f5f5;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: Consolas, monospace;
      font-size: 0.9em;
    }

    pre {
      background: #2d2d2d;
      color: #ccc;
      padding: 16px;
      border-radius: 6px;
      overflow-x: auto;
      margin: 12px 0;

      code {
        background: none;
        padding: 0;
        color: inherit;
      }
    }

    /* 列表 */
    ul, ol {
      padding-left: 24px;
      margin: 8px 0;
    }

    li { margin: 4px 0; }

    /* 任务列表 */
    ul[data-type="taskList"] {
      list-style: none;
      padding-left: 0;

      li {
        display: flex;
        align-items: flex-start;
        gap: 8px;

        label {
          margin-top: 3px;
          input { cursor: pointer; }
        }
      }
    }

    /* 表格 */
    table {
      border-collapse: collapse;
      margin: 12px 0;
      width: 100%;

      th, td {
        border: 1px solid #dcdfe6;
        padding: 8px 12px;
        text-align: left;
        min-width: 80px;
      }

      th {
        background: #f5f7fa;
        font-weight: 600;
      }
    }

    /* 图片 */
    img {
      max-width: 100%;
      height: auto;
      border-radius: 4px;
      margin: 8px 0;
    }

    /* 链接 */
    a {
      color: #1890ff;
      text-decoration: underline;
      cursor: pointer;
    }

    /* 分割线 */
    hr {
      border: none;
      border-top: 1px solid #dcdfe6;
      margin: 20px 0;
    }

    /* 高亮默认色 */
    mark {
      background: #FFF566;
      padding: 1px 4px;
      border-radius: 2px;
    }
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

.tiptap-color-panel {
  padding: 4px 0;
}

.tiptap-color-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
  padding: 8px 12px;
}

.tiptap-color-cell {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    transform: scale(1.2);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }
}

.tiptap-color-footer {
  padding: 8px 12px;
  font-size: 12px;
  color: #999;
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
  cursor: pointer;
  text-align: center;

  &:hover { color: #1890ff; }
}
</style>
