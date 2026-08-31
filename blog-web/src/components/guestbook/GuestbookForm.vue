<template>
  <div class="gb-form">
    <!-- 回复引用块：明确锚定被回复的留言（仅回复场景显示） -->
    <div class="reply-quote" v-if="replyTo">
      <el-avatar :size="36" :src="replyAvatar || undefined" class="rq-avatar">{{ replyInitial }}</el-avatar>
      <div class="rq-body">
        <div class="rq-name">@{{ replyTo.nickname }}</div>
        <div class="rq-content">{{ excerpt(replyTo.content, 80) }}</div>
      </div>
      <el-icon class="rq-close" @click="$emit('cancel-reply')"><Close /></el-icon>
    </div>

    <!-- 资料区：始终以完整表单展示，每次进入表单为空，用户重新填写 -->
    <div class="form-row">
      <div class="form-col">
        <label class="form-label">昵称 <span class="required">*</span></label>
        <el-input v-model="form.nickname" :placeholder="wx('你的昵称')" maxlength="20" class="gb-input" />
      </div>
      <div class="form-col">
        <label class="form-label">邮箱 <span class="required">*</span> <span class="label-hint">（不会公开）</span></label>
        <el-input v-model="form.email" type="email" inputmode="email" placeholder="user@example.com" class="gb-input" />
      </div>
    </div>

    <div class="form-row-full">
      <label class="form-label">网站 <span class="label-hint">（可选）</span></label>
      <el-input v-model="form.website" placeholder="https://" class="gb-input" />
    </div>

    <div class="form-row-full">
      <label class="form-label">头像 <span class="label-hint">（可选，不传则用 Gravatar）</span></label>
      <div class="gb-avatar-row">
        <div class="gb-avatar-wrap">
          <el-avatar :src="avatarPreview || undefined" :size="44" />
          <span v-if="avatarUploading" class="gb-avatar-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </span>
        </div>
        <div class="gb-avatar-actions">
          <el-upload :show-file-list="false" :before-upload="beforeAvatarUpload" :http-request="uploadAvatar" accept="image/png,image/jpeg,image/gif">
            <el-button size="small" :loading="avatarUploading" class="gb-avatar-upload">
              <el-icon><Plus /></el-icon>
              <span style="margin-left:4px">{{ avatarUploading ? '上传中…' : '上传头像' }}</span>
            </el-button>
          </el-upload>
          <span class="gb-avatar-or">或</span>
          <el-input
            v-model="form.avatar"
            placeholder="粘贴图片 URL（https://…）"
            size="small"
            class="gb-input gb-avatar-url"
            clearable
          />
          <el-button v-if="form.avatar || avatarLocalPreview" size="small" link type="info" @click="clearAvatar">清除</el-button>
        </div>
      </div>
    </div>

    <!-- 内容区（两场景共有） -->
    <div class="form-row-full">
      <label class="form-label">{{ isReply ? '回复内容' : '留言内容' }} <span class="required">*</span></label>
      <div class="gb-editor">
        <el-input
          ref="contentInput"
          v-model="form.content"
          type="textarea"
          :rows="isReply ? 4 : 5"
          :placeholder="isReply ? ('回复 @' + (replyTo?.nickname || '') + ' 说点什么…（最多 1000 字，支持 @提及）') : '写下你想说的话…（最多 1000 字，支持网址自动识别与 @提及）'"
          maxlength="1000"
          show-word-limit
          class="gb-textarea"
          @input="onContentInput"
          @keydown="onContentKeydown"
        />
        <!-- 工具栏 -->
        <div class="gb-toolbar">
          <el-button size="small" text :class="{ 'is-active': showEmoji }" @click="showEmoji = !showEmoji">
            <el-icon><PictureFilled /></el-icon><span>表情</span>
          </el-button>
        </div>

        <!-- 表情面板 -->
        <transition name="pop">
          <div class="emoji-panel" v-if="showEmoji">
            <span v-for="e in emojis" :key="e" class="emoji-item" @click="insertEmoji(e)">{{ e }}</span>
          </div>
        </transition>

        <!-- @提及自动补全 -->
        <transition name="pop">
          <div class="mention-panel" v-if="showMention && mentionList.length">
            <div v-for="m in mentionList" :key="m" class="mention-item" @click="pickMention(m)">@{{ m }}</div>
            <div v-if="!mentionList.length" class="mention-empty">无匹配昵称</div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 验证码（开关由后台控制） -->
    <div class="form-row-full" v-if="captcha.enabled">
      <label class="form-label">验证码 <span class="required">*</span> <span class="label-hint">（防刷留言）</span></label>
      <div class="captcha-row">
        <span class="captcha-q">{{ captcha.question }}</span>
        <el-input v-model="form.captchaAnswer" inputmode="numeric" placeholder="答案" class="captcha-input gb-input" @keyup.enter="submit" />
        <el-button size="small" link @click="refreshCaptcha">换一题</el-button>
      </div>
    </div>

    <div class="form-footer">
      <span class="form-hint">{{ isReply ? '回复经审核后显示，请文明交流。' : '留言经审核后显示，请文明交流。' }}</span>
      <el-button type="primary" @click="submit" :loading="submitting" class="submit-btn">{{ submitText }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { Close, Plus, PictureFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { submitComment, getCaptcha } from '@/api/front'
import { uploadAvatar as uploadAvatarApi } from '@/api/admin'
import { excerpt } from '@/utils/format'

const props = defineProps({
  replyTo: { type: Object, default: null },
  nicknames: { type: Array, default: () => [] },
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['submitted', 'cancel-reply'])

const form = reactive({ nickname: '', email: '', website: '', avatar: '', content: '', captchaAnswer: '' })
const submitting = ref(false)
const avatarUploading = ref(false)
const avatarLocalPreview = ref('')   // Blob URL,选完文件立即显示,上传完回收
const contentInput = ref(null)
const showEmoji = ref(false)
const showMention = ref(false)
const mentionQuery = ref('')

const captcha = reactive({ enabled: false, token: '', question: '' })

// 场景判断：是否回复模式
const isReply = computed(() => !!props.replyTo)
// 引用块头像/首字母
const replyAvatar = computed(() => props.replyTo?.avatar || '')
const replyInitial = computed(() => (props.replyTo?.nickname || '?').trim().charAt(0).toUpperCase())
// 提交按钮文案随场景变化
const submitText = computed(() => wx(isReply.value ? '提交回复' : '提交留言'))

const emojis = ['😀','😁','😂','🤣','😊','😍','😘','🤔','😎','😭','😡','👍','👎','👏','🙏','💪','🎉','❤️','🔥','⭐','🌟','✨','🌈','☀️','🌸','🍃','🍀','🌿','🐱','🐶','🚀','💡','📚','☕','🍰','🎁','💯','✅','❓','💬','🌊','🍻','👀','🤝','✍️','🏔️','🌙','⚡','🍃','🎈','🧡','💙','💚','💜','🤍','😴','🥳','😇','🤩','😉','🙌','💗','🌻']

// 头像预览优先级:已上传 URL > 本地 Blob 预览 > 空(后端不再走 Gravatar 兜底,前端 fallback 接管)
const avatarPreview = computed(() => form.avatar || avatarLocalPreview.value || '')

// ───────── 每次进入弹窗 / 切换回复对象：清空全部字段，满足「每次进入表单为空、重新填写所有资料」 ─────────
const resetFormFields = () => {
  form.nickname = ''
  form.email = ''
  form.website = ''
  form.avatar = ''
  form.content = ''
  form.captchaAnswer = ''
  revokeLocal()
}
watch(() => props.visible, (v) => {
  if (v) { resetFormFields(); refreshCaptcha() }
})
// 回复对象切换（含同弹窗内从一条切到另一条）也清空，避免残留上一对象的资料
watch(() => props.replyTo, () => { resetFormFields() })

// ───────── 头像上传 ─────────
const revokeLocal = () => {
  if (avatarLocalPreview.value) {
    URL.revokeObjectURL(avatarLocalPreview.value)
    avatarLocalPreview.value = ''
  }
}
const beforeAvatarUpload = (file) => {
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('头像不能超过 2MB'); return false }
  if (!['image/png', 'image/jpeg', 'image/gif'].includes(file.type)) { ElMessage.warning('请选择 PNG/JPEG/GIF 图片'); return false }
  // 选完文件立刻给一个本地预览 URL,避免等 1-2 秒后端压缩上传完才显示
  revokeLocal()
  avatarLocalPreview.value = URL.createObjectURL(file)
  return true
}
const uploadAvatar = async (options) => {
  avatarUploading.value = true
  try {
    const resp = await uploadAvatarApi(options.file)
    const url = resp.data?.url || resp.url || ''
    form.avatar = url
    revokeLocal()    // 上传成功,丢掉本地预览,改用正式 URL
    ElMessage.success('头像上传成功')
  } catch (e) {
    revokeLocal()    // 上传失败,本地预览也清掉,避免误导
    ElMessage.error(e?.response?.data?.msg || e?.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}
const clearAvatar = () => {
  form.avatar = ''
  revokeLocal()
}

// ───────── 表情插入 ─────────
const insertEmoji = (e) => {
  insertAtCursor(e)
  showEmoji.value = false
}
const insertAtCursor = (text) => {
  const ta = contentInput.value?.textarea
  if (!ta) { form.content += text; return }
  const start = ta.selectionStart ?? form.content.length
  const end = ta.selectionEnd ?? form.content.length
  form.content = form.content.slice(0, start) + text + form.content.slice(end)
  nextTick(() => {
    ta.focus()
    const pos = start + text.length
    ta.setSelectionRange(pos, pos)
  })
}

// ───────── @提及自动补全 ─────────
const onContentInput = () => {
  const ta = contentInput.value?.textarea
  if (!ta) return
  const pos = ta.selectionStart ?? form.content.length
  const left = form.content.slice(0, pos)
  const m = left.match(/@([^\s@]*)$/)
  if (m) {
    mentionQuery.value = m[1]
    showMention.value = true
  } else {
    showMention.value = false
  }
}
const mentionList = computed(() => {
  const q = mentionQuery.value.toLowerCase()
  return props.nicknames
    .filter(n => n && n.toLowerCase().includes(q))
    .filter((n, i, arr) => arr.indexOf(n) === i)
    .slice(0, 6)
})
const pickMention = (name) => {
  const ta = contentInput.value?.textarea
  const pos = ta ? (ta.selectionStart ?? form.content.length) : form.content.length
  const left = form.content.slice(0, pos)
  const right = form.content.slice(pos)
  const replaced = left.replace(/@([^\s@]*)$/, '@' + name + ' ')
  form.content = replaced + right
  showMention.value = false
  nextTick(() => {
    ta?.focus()
    const np = replaced.length
    ta?.setSelectionRange(np, np)
  })
}
const onContentKeydown = (e) => {
  if (showMention.value && (e.key === 'Escape')) showMention.value = false
}

// ───────── 验证码 ─────────
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

// ───────── 提交（校验 + 提交流程保持不变） ─────────
const submit = async () => {
  if (!form.nickname.trim()) return ElMessage.warning(wx('请填写昵称'))
  if (!form.email.trim()) return ElMessage.warning('请填写邮箱')
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) return ElMessage.warning('邮箱格式不正确')
  if (!form.content.trim()) return ElMessage.warning('请填写留言内容')
  if (captcha.enabled && !form.captchaAnswer.trim()) return ElMessage.warning('请填写验证码')

  submitting.value = true
  try {
    const resp = await submitComment({
      targetType: 'GUESTBOOK',
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      website: form.website.trim(),
      avatar: form.avatar,
      content: form.content.trim(),
      parentId: props.replyTo ? props.replyTo.id : 0,
      contentType: 0,
      captchaToken: captcha.enabled ? captcha.token : '',
      captchaAnswer: captcha.enabled ? form.captchaAnswer.trim() : ''
    })
    const saved = resp?.data || resp
    ElMessage.success(isReply.value ? wx('回复成功，等待审核', '回书已送达，等待审核') : wx('留言成功，等待审核', '留书已送达，等待审核'))
    // 重置（下次打开弹窗时 watch(visible) 会再次清空，这里顺手清一遍）
    form.content = ''
    form.captchaAnswer = ''
    showEmoji.value = false
    showMention.value = false
    refreshCaptcha()
    emit('submitted', saved)
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.response?.data?.message || e?.message || '提交失败'
    ElMessage.error(msg)
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

// ───────── 首次挂载兜底 ─────────
// el-dialog 默认懒加载：首次打开弹窗、GuestbookForm 挂载时 props.visible 已为 true，
// 但上方 watch(props.visible) 无 immediate，不会在挂载初值上触发，导致 refreshCaptcha 不调用、
// captcha.enabled 一直 false、验证码整块 v-if 不渲染（首次打开表单必现，移动端首点 FAB 尤甚）。
// 此处 onMounted 主动拉一次，与 watch 的「二次打开」逻辑互补。
onMounted(() => {
  if (props.visible) refreshCaptcha()
})

// 关闭弹窗时回收 Blob URL,避免内存泄漏
onBeforeUnmount(() => revokeLocal())
</script>

<style scoped lang="scss">
.gb-form { display: flex; flex-direction: column; gap: 14px; }

/* 回复引用块：锚定被回复的留言 */
.reply-quote {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px; background: var(--c-botany-50);
  border-left: 3px solid var(--c-botany-500); border-radius: 8px;
}
.rq-avatar {
  flex-shrink: 0; background: var(--c-botany-200); color: var(--c-botany-700);
  font-size: 14px; font-weight: 600;
}
.rq-body { flex: 1; min-width: 0; }
.rq-name { font-size: 13px; font-weight: 600; color: var(--c-botany-700); }
.rq-content {
  font-size: 13px; color: var(--c-ink-500); margin-top: 2px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.rq-close { cursor: pointer; color: var(--c-ink-soft); margin-top: 2px; flex-shrink: 0; }
.rq-close:hover { color: var(--c-ink); }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-col { display: flex; flex-direction: column; gap: 6px; }
.form-row-full { display: flex; flex-direction: column; gap: 6px; }

.form-label { font-size: 13px; color: var(--c-ink-500); font-weight: 500; }
.required { color: var(--c-autumn-500); }
.label-hint { font-size: 12px; color: var(--c-ink-300); font-weight: 400; }

.gb-avatar-row { display: flex; align-items: center; gap: 12px; }
.gb-avatar-wrap { position: relative; flex-shrink: 0; }
.gb-avatar-mask {
  position: absolute; inset: 0; border-radius: 50%;
  background: rgba(var(--theme-paper-rgb), 0.7);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: var(--c-botany-500);
}
.gb-avatar-mask .is-loading { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.gb-avatar-actions { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; flex-wrap: wrap; }
.gb-avatar-upload { flex-shrink: 0; }
.gb-avatar-or { font-size: 12px; color: var(--c-ink-300); flex-shrink: 0; }
.gb-avatar-url { flex: 1; min-width: 180px; }
.gb-avatar-url :deep(.el-input__wrapper) { border-radius: 8px; }

.gb-editor { position: relative; }
.gb-toolbar { display: flex; align-items: center; gap: 10px; margin-top: 6px; }
.gb-toolbar .el-button.is-active { color: var(--c-botany-500); }

.emoji-panel {
  position: absolute; z-index: 20; top: 100%; left: 0; margin-top: 6px;
  width: min(280px, calc(100vw - 32px)); max-height: 180px; overflow-y: auto;
  background: var(--c-paper); border: 1px solid var(--c-line); border-radius: 12px;
  box-shadow: var(--shadow-pop); padding: 10px;
  display: grid; grid-template-columns: repeat(8, 1fr); gap: 4px;
}
@media (max-width: 480px) {
  .emoji-panel { grid-template-columns: repeat(6, 1fr); width: min(260px, calc(100vw - 32px)); }
}
.emoji-item { text-align: center; font-size: 18px; cursor: pointer; border-radius: 6px; padding: 2px; }
.emoji-item:hover { background: var(--c-botany-50); }

.mention-panel {
  position: absolute; z-index: 20; top: 100%; left: 0; margin-top: 6px;
  min-width: 180px; background: var(--c-paper); border: 1px solid var(--c-line);
  border-radius: 10px; box-shadow: var(--shadow-pop); padding: 6px; overflow: hidden;
}
.mention-item { padding: 6px 10px; font-size: 13px; cursor: pointer; border-radius: 6px; color: var(--c-ink-500); }
.mention-item:hover { background: var(--c-botany-50); color: var(--c-botany-700); }
.mention-empty { padding: 8px 10px; font-size: 12px; color: var(--c-ink-300); }

.captcha-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.captcha-q { font-size: 14px; font-weight: 600; color: var(--c-ink); background: var(--c-botany-50); padding: 6px 12px; border-radius: 8px; }
.captcha-input { width: 120px; }

.form-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; gap: 12px; }
.form-hint { font-size: 12px; color: var(--c-ink-300); }
.submit-btn { border-radius: 999px; padding: 0 24px; height: 38px; font-weight: 500; }

/* 输入框样式覆盖 */
.gb-input :deep(.el-input__wrapper) { border-radius: 10px; box-shadow: 0 0 0 1px var(--c-line) inset; padding: 0 14px; }
.gb-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--c-ink-300) inset; }
.gb-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--c-botany-500) inset; }
.gb-textarea :deep(.el-textarea__inner) { border-radius: 10px; border: 1px solid var(--c-line); padding: 12px 14px; resize: vertical; }
.gb-textarea :deep(.el-textarea__inner:focus) { border-color: var(--c-botany-500); outline: none; }
.gb-textarea :deep(.el-input__count) { background: transparent; color: var(--c-ink-300); font-size: 12px; }

.pop-enter-active, .pop-leave-active { transition: all 0.18s ease; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(-4px); }

@media (max-width: 640px) {
  .form-row { grid-template-columns: 1fr; gap: 12px; }
  .form-footer { flex-direction: column; align-items: stretch; gap: 10px; }
  .submit-btn { width: 100%; }
}
</style>
