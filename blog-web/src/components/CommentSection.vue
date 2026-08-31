<template>
  <section class="comment-section">
    <h3 class="cs-title">{{ wx('评论') }} <span class="cs-count">({{ total }})</span></h3>

    <!-- 首次加载骨架 -->
    <div class="comment-list skeleton-list" v-if="loading && !tree.length">
      <div class="skeleton-item" v-for="i in 3" :key="i">
        <el-skeleton-item variant="circle" style="width:40px;height:40px" />
        <div class="skeleton-body">
          <el-skeleton-item variant="text" style="width: 30%; height: 16px;" />
          <el-skeleton-item variant="text" style="width: 90%; height: 14px; margin-top: 8px;" />
          <el-skeleton-item variant="text" style="width: 60%; height: 14px; margin-top: 6px;" />
        </div>
      </div>
    </div>

    <div class="comment-list" v-else-if="tree.length">
      <CommentItem
        v-for="c in tree"
        :key="c.id"
        :comment="c"
        @reply="onReply"
      />
    </div>
    <div class="comment-empty" v-else>
      <el-empty :description="wx('还没有评论，来抢沙发吧～')" />
    </div>

    <!-- 触发式写评论：默认只显示「我来说一句」按钮 -->
    <div class="comment-trigger" v-if="!formExpanded">
      <el-button type="primary" round class="trigger-btn" @click="openForm">
        <el-icon><EditPen /></el-icon>
        <span>{{ wx('我来说一句') }}</span>
      </el-button>
      <span class="trigger-hint">已有 {{ total }} 条评论，期待你的声音</span>
    </div>

    <!-- 展开后的写评论表单 -->
    <div class="comment-form" ref="formRef" v-else>
      <div class="form-header">
        <span class="form-title">{{ replyTo ? '回复 @' + replyTo.nickname : wx('写下你的评论') }}</span>
        <el-button link type="info" size="small" @click="closeForm" class="form-close">
          <el-icon><Close /></el-icon> 收起
        </el-button>
      </div>

      <div class="cs-avatar-row">
        <div class="cs-avatar-wrap">
          <el-avatar :src="avatarPreview" :size="48" />
          <span v-if="avatarUploading" class="cs-avatar-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </span>
        </div>
        <div class="cs-avatar-actions">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="uploadAvatar"
            accept="image/png,image/jpeg,image/webp,image/gif"
          >
            <el-button size="small" :loading="avatarUploading">
              <el-icon><Plus /></el-icon>
              <span style="margin-left:4px">{{ avatarUploading ? '上传中…' : '上传头像' }}</span>
            </el-button>
          </el-upload>
          <span class="cs-avatar-or">或</span>
          <el-input
            v-model="form.avatar"
            placeholder="粘贴图片 URL（https://…）"
            size="small"
            class="cs-avatar-url"
            clearable
          />
          <el-button v-if="form.avatar || avatarLocalPreview" size="small" link type="info" @click="clearAvatar">清除</el-button>
          <span class="text-soft cs-avatar-tip">不传则用 Gravatar 邮箱头像</span>
        </div>
      </div>
      <el-input v-model="form.nickname" :placeholder="wx('昵称 *')" maxlength="20" />
      <el-input v-model="form.email" type="email" inputmode="email" placeholder="邮箱 (选填, 不会公开)" />
      <el-input v-model="form.website" placeholder="网站 (选填)" />
      <el-input
        ref="contentInput"
        v-model="form.content"
        type="textarea"
        :rows="4"
        :placeholder="wx('说点什么… 支持 Markdown')"
        maxlength="1000"
        show-word-limit
        @keydown.ctrl.enter.exact.prevent="submit"
      />
      <!-- 验证码（开关由后台控制，与留言板共用同一套 captcha_enabled） -->
      <div class="cs-captcha" v-if="captcha.enabled">
        <label class="form-label">验证码 <span class="required">*</span> <span class="label-hint">（防刷评论）</span></label>
        <div class="captcha-row">
          <span class="captcha-q">{{ captcha.question }}</span>
          <el-input v-model="form.captchaAnswer" inputmode="numeric" placeholder="答案" class="captcha-input cs-input" @keyup.enter="submit" />
          <el-button size="small" link @click="refreshCaptcha">换一题</el-button>
        </div>
      </div>
      <div class="form-actions">
        <span class="text-soft reply-hint" v-if="replyTo">
          <el-icon><ChatLineRound /></el-icon>
          回复 <b>{{ replyTo.nickname }}</b>
          <el-icon class="clear-reply" @click="cancelReply"><Close /></el-icon>
        </span>
        <span class="text-soft hotkey-hint" v-else>Ctrl + Enter 快速发表</span>
        <el-button type="primary" @click="submit" :loading="submitting">{{ wx('发表评论') }}</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { Close, Plus, EditPen, ChatLineRound, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import { submitComment, commentsByArticle, getCaptcha } from '@/api/front'
import { uploadAvatar as uploadAvatarApi } from '@/api/admin'
import CommentItem from '@/components/CommentItem.vue'

const props = defineProps({ articleId: { type: Number, required: true } })

const tree = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const replyTo = ref(null)
const avatarUploading = ref(false)
const avatarLocalPreview = ref('')
const formExpanded = ref(false)        // 默认折叠
const formRef = ref(null)
const contentInput = ref(null)
const form = reactive({ nickname: '', email: '', website: '', avatar: '', content: '', parentId: null, captchaAnswer: '' })

// 验证码：与留言板共用后端 captcha_enabled 开关。CommentSection 常驻挂载（非 dialog 懒加载），
// 故在 onMounted 提前拉一次，表单展开时再补拉（token 一次性消费，提交后也刷新）
const captcha = reactive({ enabled: false, token: '', question: '' })
const refreshCaptcha = async () => {
  try {
    const resp = await getCaptcha()
    const d = resp.data || {}
    captcha.enabled = !!d.enabled
    captcha.token = d.token || ''
    captcha.question = d.question || ''
    form.captchaAnswer = ''
  } catch (e) {}
}

// 头像预览优先级:URL > 本地 Blob 预览 > 空(Gravatar 由后端兜底)
const avatarPreview = computed(() => form.avatar || avatarLocalPreview.value || '')

// 树的节点总数(含嵌套回复)
const countTree = (arr) => arr.reduce((n, c) => n + 1 + (c.replies ? countTree(c.replies) : 0), 0)

// 打开表单(默认展开,或回复某条评论时也展开)
const openForm = (preselect) => {
  formExpanded.value = true
  refreshCaptcha()   // 展开表单即拉取验证码
  // 默认聚焦到内容框
  nextTick(() => {
    setTimeout(() => {
      const ta = document.querySelector('.comment-form textarea')
      ta?.focus()
    }, 60)
  })
}

const closeForm = () => {
  formExpanded.value = false
  cancelReply()
  // 提交后的内容保留在 form.content,以便用户重新打开时编辑
  // 这里仅清掉内容与 parentId,昵称/邮箱/头像保留方便连续评论
  form.content = ''
  form.parentId = null
}

// ───────── 头像(上传 + URL 粘贴,参考留言板 GuestbookForm) ─────────
const revokeLocal = () => {
  if (avatarLocalPreview.value) {
    URL.revokeObjectURL(avatarLocalPreview.value)
    avatarLocalPreview.value = ''
  }
}
const beforeAvatarUpload = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return false
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return false
  }
  // 选完文件立刻给一个本地预览,避免等 1-2 秒后端压缩完才显示
  revokeLocal()
  avatarLocalPreview.value = URL.createObjectURL(file)
  return true
}
const uploadAvatar = async (options) => {
  avatarUploading.value = true
  try {
    const resp = await uploadAvatarApi(options.file)
    form.avatar = resp.data?.url || resp.url || ''
    revokeLocal()
    ElMessage.success('头像上传成功')
  } catch (e) {
    revokeLocal()
    ElMessage.error(e?.response?.data?.msg || e?.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}
const clearAvatar = () => {
  form.avatar = ''
  revokeLocal()
}

const load = async () => {
  if (!props.articleId) {
    tree.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const resp = await commentsByArticle(props.articleId)
    tree.value = resp.data || []
    total.value = countTree(tree.value)
  } catch (e) {
    ElMessage.error(wx('评论加载失败,稍后重试'))
  } finally {
    loading.value = false
  }
}

const onReply = (c) => {
  replyTo.value = c
  form.parentId = c.id
  // 自动展开表单(若未展开)
  if (!formExpanded.value) formExpanded.value = true
  refreshCaptcha()   // 展开回复表单也需验证码
  // 滚动到表单并聚焦输入框
  nextTick(() => {
    formRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setTimeout(() => {
      const ta = document.querySelector('.comment-form textarea')
      ta?.focus()
      if (ta && typeof ta.setSelectionRange === 'function') {
        ta.setSelectionRange(ta.value.length, ta.value.length)
      }
    }, 350)
  })
}

const cancelReply = () => {
  replyTo.value = null
  form.parentId = null
}

const submit = async () => {
  if (!form.nickname.trim()) return ElMessage.warning(wx('请填写昵称'))
  if (captcha.enabled && !form.captchaAnswer.trim()) return ElMessage.warning('请填写验证码')
  if (!form.content.trim()) return ElMessage.warning(wx('请填写评论内容'))
  if (form.content.trim().length < 2) return ElMessage.warning(wx('评论内容太短'))

  submitting.value = true
  const payload = {
    articleId: props.articleId,
    nickname: form.nickname.trim(),
    email: form.email.trim(),
    website: form.website.trim(),
    avatar: form.avatar,
    content: form.content.trim(),
    parentId: form.parentId,
    captchaToken: captcha.enabled ? captcha.token : '',
    captchaAnswer: captcha.enabled ? form.captchaAnswer.trim() : ''
  }
  try {
    const resp = await submitComment(payload)
    const saved = resp?.data || resp
    insertOptimistic(saved)
    form.content = ''
    form.parentId = null
    replyTo.value = null
    refreshCaptcha()   // token 已一次性消费，刷新备用
    // 提交成功后自动收起表单,体验更清爽
    formExpanded.value = false
    ElNotification.success({
      title: wx('评论已提交'),
      message: wx('评论正在等待审核,通过后会自动展示'),
      duration: 2500
    })
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.response?.data?.message || e?.message || '评论提交失败'
    ElMessage.error(msg)
    refreshCaptcha()   // 失败也刷新，避免旧 token 卡死
  } finally {
    submitting.value = false
  }
}

const insertOptimistic = (c) => {
  if (!c || !c.id) return
  const node = { ...c, _pending: true, replies: c.replies || [] }
  if (!c.parentId || c.parentId === 0) {
    tree.value.unshift(node)
  } else {
    const parent = findInTree(tree.value, c.parentId)
    if (parent) {
      if (!parent.replies) parent.replies = []
      parent.replies.unshift(node)
    } else {
      tree.value.unshift(node)
    }
  }
  total.value = countTree(tree.value)
  nextTick(() => {
    const el = document.getElementById(`comment-${c.id}`)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

const findInTree = (arr, id) => {
  for (const c of arr) {
    if (c.id === id) return c
    if (c.replies) {
      const f = findInTree(c.replies, id)
      if (f) return f
    }
  }
  return null
}

onMounted(() => {
  load()
  refreshCaptcha()   // 提前拉取，展开表单即显示
})
onBeforeUnmount(() => revokeLocal())
</script>

<style scoped lang="scss">
.comment-section { padding: 24px 0 28px; }
.cs-title {
  font-family: var(--font-serif);
  font-size: 22px;
  margin: 0 0 16px;
  padding-left: 12px;
  border-left: 3px solid var(--c-autumn-500);
}
.cs-count { color: var(--c-ink-soft); font-size: 16px; font-weight: 400; }

/* 空状态：压缩 el-empty 自带的留白 */
.comment-empty {
  padding: 8px 0 4px;
  :deep(.el-empty) { padding: 8px 0; }
  :deep(.el-empty__image) { width: 80px; height: 80px; }
  :deep(.el-empty__description) { margin-top: 6px; font-size: 13px; }
  :deep(.el-empty__bottom) { margin-top: 0; }
}

/* 触发按钮（默认显示） */
.comment-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  margin-top: 16px;
  background: var(--c-paper);
  border: 1px dashed var(--c-line);
  border-radius: var(--radius);
  transition: border-color 0.2s, background 0.2s;
}
.comment-trigger:hover {
  border-color: var(--c-autumn-500);
  background: var(--c-paper-soft);
}
.trigger-btn {
  min-width: 140px;
  height: 40px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(var(--theme-accent-dark-rgb), 0.18);
}
.trigger-hint {
  font-size: 13px;
  color: var(--c-ink-soft);
}

/* 写评论表单（展开后） */
.comment-form {
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius);
  padding: 20px;
  margin-top: 16px;
  scroll-margin-top: 80px;
  animation: slideDown 0.22s ease-out;
}
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}
.form-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--c-line-soft);
}
.form-title {
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--c-ink);
}
.form-close { font-size: 13px; }
.form-close .el-icon { margin-right: 2px; }

.comment-form .el-input { margin-bottom: 12px; }

/* 头像行:头像 + 上传 + 或 + URL输入 + 清除 + 提示 */
.cs-avatar-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
  padding: 12px;
  background: var(--c-paper-soft);
  border-radius: 8px;
}
.cs-avatar-wrap { position: relative; flex-shrink: 0; }
.cs-avatar-mask {
  position: absolute; inset: 0; border-radius: 50%;
  background: rgba(var(--theme-paper-rgb), 0.7);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: var(--c-autumn-500);
}
.cs-avatar-mask .is-loading { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.cs-avatar-actions {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cs-avatar-or {
  font-size: 12px;
  color: var(--c-ink-300);
  flex-shrink: 0;
}
.cs-avatar-url {
  flex: 1;
  min-width: 180px;
}
.cs-avatar-url :deep(.el-input__wrapper) { border-radius: 8px; }
.cs-avatar-tip {
  width: 100%;
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 2px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  gap: 12px;
}
.reply-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--c-autumn-500);
}
.hotkey-hint {
  font-size: 12px;
  color: var(--c-ink-soft);
}
.clear-reply {
  cursor: pointer;
  margin-left: 6px;
  color: var(--c-ink-soft);
}
.clear-reply:hover { color: var(--c-autumn-500); }

/* 验证码（与留言板共用同一套视觉，开关由后台 captcha_enabled 控制） */
.cs-captcha { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.cs-captcha .form-label { font-size: 13px; color: var(--c-ink-500); font-weight: 500; }
.cs-captcha .required { color: var(--c-autumn-500); }
.cs-captcha .label-hint { font-size: 12px; color: var(--c-ink-300); font-weight: 400; }
.captcha-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.captcha-q { font-size: 14px; font-weight: 600; color: var(--c-ink); background: var(--c-botany-50); padding: 6px 12px; border-radius: 8px; }
.captcha-input { width: 120px; }

/* 骨架 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.skeleton-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius);
}
.skeleton-body { flex: 1; }

/* ───────── mobile ───────── */
@media (max-width: 480px) {
  .cs-avatar-row {
    flex-direction: column;
    gap: 10px;
  }
  .cs-avatar-url {
    min-width: 0;
    flex: 1;
  }
  .cs-avatar-actions {
    flex-wrap: wrap;
    gap: 6px;
  }
  .cs-input :deep(.el-textarea__inner) { font-size: 14px; }
  .cs-submit { width: 100%; }
}
</style>
