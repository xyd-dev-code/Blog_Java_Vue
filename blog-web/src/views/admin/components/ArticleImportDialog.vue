<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="导入 .md 文章"
    width="min(720px, 96vw)"
    destroy-on-close
    append-to-body
    @close="onClose"
  >
    <div class="imp-intro">
      <el-alert type="info" :closable="false" show-icon>
        <p>
          支持同时选择多个 <code>.md</code> 文件。解析后按 slug 去重:
          <b>命中已有 slug</b> → 更新该篇;<b>无 slug</b> → 视为新建。
          不存在的分类/标签会自动创建。
        </p>
      </el-alert>
      <div class="imp-tpl-row">
        <el-link type="primary" :underline="false" @click="downloadTemplate">
          <el-icon><Download /></el-icon>
          <span style="margin-left: 4px;">下载 .docx 模板 (Word 友好,字段含义见文末注释)</span>
        </el-link>
        <el-link type="primary" :underline="false" style="margin-left: 12px;" @click="downloadMdTemplate">
          <el-icon><Download /></el-icon>
          <span style="margin-left: 4px;">下载 .md 模板</span>
        </el-link>
      </div>
    </div>

    <el-upload
      ref="uploaderRef"
      drag
      multiple
      accept=".md,.markdown,.docx,.zip"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="onPickFile"
      class="imp-drop"
    >
      <el-icon class="imp-drop-icon"><UploadFilled /></el-icon>
      <div class="imp-drop-text">点击或拖拽 .md / .docx / .zip 到此处</div>
      <div class="imp-drop-hint">支持多文件,单文件 ≤ 50MB;docx 走 pandoc 服务端转换</div>
    </el-upload>

    <div v-if="previews.length" class="imp-list">
      <div class="imp-list-head">
        已解析 <b>{{ previews.length }}</b> 个文件,共 <b>{{ summary.toImport }}</b> 篇待导入
        <span v-if="summary.update > 0" class="imp-pill imp-pill-up">更新 {{ summary.update }}</span>
        <span v-if="summary.create > 0" class="imp-pill imp-pill-new">新建 {{ summary.create }}</span>
        <span v-if="summary.invalid > 0" class="imp-pill imp-pill-err">无效 {{ summary.invalid }}</span>
      </div>
      <el-table :data="previews" size="small" max-height="280">
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ok'" :type="row.existed ? 'warning' : 'success'" size="small">
              {{ row.existed ? '更新' : '新建' }}
            </el-tag>
            <el-tag v-else-if="row.status === 'docx-pending'" type="info" size="small">待解析</el-tag>
            <el-tag v-else type="danger" size="small">错误</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="160">
          <template #default="{ row }">{{ row.title || row.filename }}</template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="标签" width="160">
          <template #default="{ row }">
            <span v-if="row.tags?.length">{{ row.tags.join('、') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="正文" width="80" align="right">
          <template #default="{ row }">{{ row.bodyLength }} 字符</template>
        </el-table-column>
        <el-table-column v-if="hasError" label="" width="60">
          <template #default="{ row }">
            <el-tooltip v-if="row.error" :content="row.error" placement="top">
              <el-icon style="color: var(--c-element-danger)"><WarningFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="onClose">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="!summary.toImport"
        @click="submit"
      >
        开始导入 ({{ summary.toImport }})
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { UploadFilled, WarningFilled, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { parseMarkdownInWorker } from '@/utils/markdownImportWorker'
import { adminImportArticles, adminDownloadMarkdownTemplate, adminDownloadDocxTemplate, triggerDownload } from '@/api/admin'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'imported'])

const uploaderRef = ref(null)
const previews = ref([])
const submitting = ref(false)
// Element Plus Upload 组件的 setup 只 expose 了 abort/submit/clearFiles 等,
// uploadFiles 没暴露 → 取不到原 File 对象。所以 dialog 自己维护一份 raw 映射。
const rawByName = new Map()

// 兼容后端在 .zip 中嵌套 .md 文件:通过 JSZip 解压后再喂进来。
// 但为简化首版,只支持直接选 .md;若选 .zip 则给出明确提示。
async function onPickFile(file) {
  if (!file?.raw) return
  const f = file.raw
  const name = f.name || ''
  if (f.size > 5 * 1024 * 1024 || rawByName.size >= 20) {
    ElMessage.warning('每个文件最多 5MB，一次最多 20 个文件')
    return
  }
  rawByName.set(name, f)
  if (name.toLowerCase().endsWith('.zip')) {
    ElMessage.warning('暂不支持直接上传 zip,请先解压后选择其中的 .md / .docx')
    return
  }
  const lower = name.toLowerCase()
  if (!lower.endsWith('.md') && !lower.endsWith('.markdown') && !lower.endsWith('.docx')) {
    ElMessage.warning('仅支持 .md / .markdown / .docx 文件')
    return
  }

  if (lower.endsWith('.docx')) {
    // 提交前是占位,真正解析在后端 import 时通过 pandoc 完成。
    previews.value.push({
      filename: name,
      status: 'docx-pending',
      title: name.replace(/\.docx$/i, ''),
      category: '',
      tags: [],
      bodyLength: 0,
      existed: false,
      error: ''
    })
    return
  }

  let text
  try {
    text = await f.text()
  } catch (e) {
    previews.value.push(mkErr(name, `读取失败: ${e.message || e}`))
    return
  }

  try {
    const p = await parseMarkdownInWorker(name, text)
    previews.value.push(p)
  } catch (error) {
    previews.value.push(mkErr(name, error.message || '解析失败'))
  }
}

function mkErr(filename, msg) {
  return {
    filename,
    status: 'err',
    error: msg,
    existed: false,
    title: filename,
    category: '',
    tags: [],
    bodyLength: 0
  }
}


const hasError = computed(() => previews.value.some(p => p.status === 'err'))

const summary = computed(() => {
  // docx-pending / ok 都算"待导入";只有 parseMd 失败(status==='err')才不计
  const pending = previews.value.filter(p => p.status === 'docx-pending').length
  const ok = previews.value.filter(p => p.status === 'ok')
  const invalid = previews.value.filter(p => p.status === 'err').length
  return {
    total: previews.value.length,
    create: pending + ok.filter(p => !p.existed).length,
    update: ok.filter(p => p.existed).length,
    invalid,
    toImport: pending + ok.length
  }
})

watch(() => props.modelValue, (v) => {
  if (v) {
    previews.value = []
  } else {
    rawByName.clear()
  }
})

async function submit() {
  const valid = previews.value.filter(p => p.status === 'ok' || p.status === 'docx-pending')
  if (!valid.length) return
  submitting.value = true
  try {
    const form = new FormData()
    for (const p of valid) {
      const raw = rawByName.get(p.filename)
      if (raw) form.append('files', raw, p.filename)
      else form.append('files', new Blob([''], { type: 'text/markdown' }), p.filename)
    }
    const resp = await adminImportArticles(form)
    const r = resp.data || {}
    emit('imported', r)
    ElMessage.success(`导入完成:新建 ${r.created || 0} 篇,更新 ${r.updated || 0} 篇,失败 ${r.failed || 0} 篇`)
    emit('update:modelValue', false)
  } catch (e) {
    // 拦截器已 toast
  } finally {
    submitting.value = false
  }
}

function onClose() {
  emit('update:modelValue', false)
}

async function downloadTemplate() {
  try {
    const blob = await adminDownloadDocxTemplate()
    triggerDownload(blob, 'blog-article-template.docx')
  } catch (e) {
    ElMessage.error(e?.message || '下载模板失败')
  }
}

async function downloadMdTemplate() {
  try {
    const blob = await adminDownloadMarkdownTemplate()
    triggerDownload(blob, 'blog-article-template.md')
  } catch (e) {
    ElMessage.error(e?.message || '下载模板失败')
  }
}
</script>

<style scoped lang="scss">
.imp-intro { margin-bottom: 16px; }
.imp-intro p { margin: 0; line-height: 1.7; font-size: 13px; }
.imp-intro code {
  background: var(--c-editor-fill); border: 1px solid var(--c-line-soft);
  padding: 0 4px; border-radius: 3px; font-size: 12px;
}
.imp-tpl-row {
  margin-top: 8px;
  text-align: right;
}

.imp-drop {
  display: flex; justify-content: center;
  :deep(.el-upload) { width: 100%; }
  :deep(.el-upload-dragger) {
    padding: 36px 20px;
    border-radius: 10px;
  }
}
.imp-drop-icon {
  font-size: 38px; color: var(--c-botany-500);
  margin-bottom: 8px;
}
.imp-drop-text {
  font-size: 14px; color: var(--c-ink); font-weight: 500;
}
.imp-drop-hint {
  font-size: 12px; color: var(--c-ink-soft); margin-top: 4px;
}

.imp-list {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed var(--c-line-soft);
}
.imp-list-head {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  font-size: 13px; color: var(--c-ink-soft); margin-bottom: 8px;
  b { color: var(--c-ink); margin: 0 2px; }
}
.imp-pill {
  font-size: 12px; padding: 2px 8px; border-radius: 999px;
  background: var(--c-neutral-fill); color: var(--c-ink-500);
}
.imp-pill-new { background: var(--c-success-soft); color: var(--c-success); }
.imp-pill-up  { background: var(--c-warning-soft); color: var(--c-warning-text); }
.imp-pill-err { background: var(--c-danger-soft); color: var(--c-danger-strong); }
</style>
