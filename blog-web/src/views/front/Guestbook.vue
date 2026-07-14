<template>
  <div class="guestbook-page">
    <!-- Hero -->
    <HeroGuestbook
      title="留言板"
      :subtitle="`欢迎留下你的足迹，共 ${totalCount} 条留言`"
      :stats="heroStats"
    />

    <div class="container page-body">
      <!-- 留言列表 · 便签墙 -->
      <div class="gb-list gb-notes" v-if="list.length">
        <StickyNote
          v-for="(c, idx) in list"
          :key="c.id"
          :tone="noteTone(idx)"
          :rotate="noteRotate(idx)"
          class="gb-note"
        >
          <CommentItem :comment="c" :index="idx" variant="sticky" @reply="onReply" />
        </StickyNote>
      </div>
      <el-empty v-else description="还没有留言，来抢沙发吧～" />

      <!-- 写留言按钮 -->
      <div class="gb-write-btn-wrap" v-if="!showForm">
        <button class="gb-write-btn" @click="showForm = true">
          <el-icon><EditPen /></el-icon>
          <span>贴一张</span>
        </button>
      </div>

      <!-- 留言表单 -->
      <Transition name="form-slide">
        <div class="gb-form-card" v-if="showForm">
          <div class="form-cancel" @click="showForm = false">
            <span>取消</span>
          </div>
          <div class="form-body">
            <div v-if="replyTo" class="reply-to-bar">
              <span>回复 <b>{{ replyTo.nickname }}</b></span>
              <el-icon class="clear-reply" @click="replyTo = null"><Close /></el-icon>
            </div>
            <div class="form-row">
              <div class="form-col">
                <label class="form-label">昵称 <span class="required">*</span></label>
                <el-input
                  v-model="form.nickname"
                  placeholder="你的昵称"
                  maxlength="20"
                  class="gb-input"
                />
              </div>
              <div class="form-col">
                <label class="form-label">邮箱 <span class="required">*</span> <span class="label-hint">（不会公开）</span></label>
                <el-input
                  v-model="form.email"
                  placeholder="your@example.com"
                  class="gb-input"
                />
              </div>
            </div>

            <div class="form-row-full">
              <label class="form-label">网站 <span class="label-hint">（可选）</span></label>
              <el-input
                v-model="form.website"
                placeholder="https://"
                class="gb-input"
              />
            </div>

            <div class="form-row-full">
              <label class="form-label">头像 <span class="label-hint">（可选，不传则用 Gravatar）</span></label>
              <div class="gb-avatar-row">
                <el-avatar :src="form.avatar" :size="44" />
                <el-upload
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="uploadAvatar"
                  accept="image/*"
                >
                  <el-button size="small" :loading="avatarUploading">
                    <el-icon><Plus /></el-icon>
                    <span style="margin-left: 4px;">上传头像</span>
                  </el-button>
                </el-upload>
                <el-button v-if="form.avatar" size="small" link @click="form.avatar = ''">清除</el-button>
              </div>
            </div>

            <div class="form-row-full">
              <label class="form-label">留言内容 <span class="required">*</span></label>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="5"
                placeholder="写下你想说的话…（最多 1000 字）"
                maxlength="1000"
                show-word-limit
                class="gb-textarea"
              />
            </div>

            <div class="form-footer">
              <span class="form-hint">留言经审核后显示，请文明交流。</span>
              <el-button type="primary" @click="submit" :loading="submitting" class="submit-btn">
                提交留言
              </el-button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { EditPen, Close, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import CommentItem from '@/components/CommentItem.vue'
import StickyNote from '@/components/StickyNote.vue'
import HeroGuestbook from '@/components/HeroGuestbook.vue'
import { guestbookComments, submitComment } from '@/api/front'
import { uploadAvatar as uploadAvatarApi } from '@/api/admin'

const list = ref([])
const submitting = ref(false)
const showForm = ref(false)
const replyTo = ref(null)
const form = reactive({ nickname: '', email: '', website: '', avatar: '', content: '' })
const avatarUploading = ref(false)

const beforeAvatarUpload = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return false
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return false
  }
  return true
}

const uploadAvatar = async (options) => {
  avatarUploading.value = true
  try {
    const resp = await uploadAvatarApi(options.file)
    form.avatar = resp.data?.url || resp.url || ''
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

// 便签卡随机旋转与色调
const TONES = ['sky', 'cyan', 'sun', 'paper']
const ROTATES = [-2.2, -1, -0.4, 0.6, 1.4, 2.4, -1.8, 0.2]
const noteTone = (i) => TONES[i % TONES.length]
const noteRotate = (i) => ROTATES[i % ROTATES.length]

const totalCount = computed(() => {
  const count = (arr) => arr.reduce((n, c) => n + 1 + (c.replies ? count(c.replies) : 0), 0)
  return count(list.value)
})

const heroStats = computed(() => [
  { label: '条留言', value: totalCount.value },
  { label: '今日新贴', value: todayCount.value },
  { label: '份心情', value: '∞' }
])

const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const walk = (arr) => arr.reduce((n, c) => {
    const d = (c.createTime || '').slice(0, 10)
    return n + (d === today ? 1 : 0) + (c.replies ? walk(c.replies) : 0)
  }, 0)
  return walk(list.value)
})

const load = async () => {
  try {
    const resp = await guestbookComments()
    list.value = resp.data || []
  } catch (_) {}
}

const onReply = (c) => {
  replyTo.value = c
  showForm.value = true
  ElMessage.info(`回复 ${c.nickname}，请填写内容后提交`)
  nextTick(() => {
    const formEl = document.querySelector('.gb-form-card')
    if (formEl) formEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

const submit = async () => {
  if (!form.nickname.trim()) return ElMessage.warning('请填写昵称')
  if (!form.email.trim()) return ElMessage.warning('请填写邮箱')
  if (!form.content.trim()) return ElMessage.warning('请填写留言内容')

  submitting.value = true
  try {
    await submitComment({
      articleId: 1,
      nickname: form.nickname,
      email: form.email,
      website: form.website,
      avatar: form.avatar,
      content: form.content,
      parentId: replyTo.value ? replyTo.value.id : 0
    })
    ElMessage.success('留言成功，等待审核')
    form.content = ''
    form.website = ''
    form.avatar = ''
    replyTo.value = null
    showForm.value = false
    setTimeout(load, 500)
  } catch (_) {}
  submitting.value = false
}

onMounted(load)
</script>

<style scoped lang="scss">
/* 页面头部 — 炫酷晴空光晕 */
.gb-header {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 20%, rgba(255, 255, 255, 0.35) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 70%, rgba(56, 189, 248, 0.4) 0%, transparent 45%),
    radial-gradient(ellipse at 70% 30%, rgba(14, 165, 233, 0.35) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(34, 211, 238, 0.25) 0%, transparent 40%),
    linear-gradient(135deg, #075985 0%, #0369a1 25%, #0ea5e9 55%, #38bdf8 80%, #7dd3fc 100%);
  color: #fff;
  padding: 70px 0 100px;
  text-align: center;
  margin-bottom: 50px;
  box-shadow: 0 10px 40px rgba(14, 165, 233, 0.18);
}

/* 头部背景网格纹理 — 更明显的线条网格 */
.gb-header::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.08) 1px, transparent 1px);
  background-size: 60px 60px;
  z-index: 0;
  pointer-events: none;
}

/* 大型光晕球 */
.gb-header .orb-1,
.gb-header .orb-2,
.gb-header .orb-3 {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
  opacity: 0.7;
}
.gb-header .orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(56,189,248,0.5), transparent 70%);
  top: -80px; right: 5%;
  animation: orb-float 12s ease-in-out infinite;
}
.gb-header .orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(34,211,238,0.4), transparent 70%);
  bottom: 10px; left: 0%;
  animation: orb-float 15s ease-in-out infinite -3s;
}
.gb-header .orb-3 {
  width: 250px; height: 250px;
  background: radial-gradient(circle, rgba(255,255,255,0.3), transparent 70%);
  top: 25%; left: 35%;
  animation: orb-float 18s ease-in-out infinite -6s;
}

@keyframes orb-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(20px, -15px) scale(1.05); }
  50% { transform: translate(-10px, 10px) scale(0.95); }
  75% { transform: translate(15px, 5px) scale(1.03); }
}

/* 头部浮动粒子 — 更多更亮 */
.gb-header .particles {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}
.gb-header .particles span {
  position: absolute;
  width: 5px; height: 5px;
  background: rgba(255,255,255,0.7);
  border-radius: 50%;
  animation: particle-rise 8s linear infinite;
}
@keyframes particle-rise {
  0% { transform: translateY(100%) scale(0); opacity: 0; }
  20% { opacity: 1; transform: translateY(80%) scale(1); }
  80% { opacity: 0.6; }
  100% { transform: translateY(-20%) scale(0); opacity: 0; }
}

.gb-header-inner {
  max-width: 880px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 2;
}

/* 浮动云 */
.cloud {
  position: absolute;
  z-index: 0;
  pointer-events: none;
}
.cloud-1 { top: 10%; right: 8%; width: 200px; animation-delay: 0s; }
.cloud-2 { top: 45%; left: 5%; width: 140px; animation-delay: -3s; }

/* 波浪 */
.wave {
  position: absolute;
  bottom: -2px; left: 0; right: 0;
  z-index: 1;
  pointer-events: none;
}

.gb-pretitle {
  font-size: 13px;
  color: rgba(255,255,255,0.85);
  letter-spacing: 4px;
  margin: 0 0 10px;
  font-weight: 500;
}

.gb-title {
  font-family: var(--font-serif);
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  letter-spacing: 2px;
  text-shadow: 0 0 30px rgba(255,255,255,0.4), 0 2px 12px rgba(2, 132, 199, 0.3);
}

.gb-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.85);
  margin: 0;
}

.gb-count {
  color: #fff;
  font-weight: 600;
}

/* 页面主体 — 星空网格背景 */
.page-body {
  position: relative;
  padding: 20px 0 80px;
}

/* 留言统计卡 — 晴空 v2 渐变描边 */
.gb-stats-card {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 36px;
  padding: 22px 28px;
  margin-bottom: 32px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.7) 0%, rgba(240, 249, 255, 0.5) 100%);
  border: 1px solid rgba(125, 211, 252, 0.5);
  border-radius: 18px;
  backdrop-filter: blur(10px);
  box-shadow:
    0 4px 20px rgba(14, 165, 233, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
.gb-stats-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 18px;
  padding: 1px;
  background: linear-gradient(135deg, #38bdf8, #fbbf24);
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
  pointer-events: none;
}
.gb-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.gb-stat-num {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, #0369a1, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.gb-stat-sun .gb-stat-num {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.gb-stat-label {
  font-size: 12px;
  color: var(--c-ink-soft);
  letter-spacing: 0.05em;
}
.gb-stat-sep {
  width: 1px;
  height: 32px;
  background: linear-gradient(180deg, transparent, rgba(56, 189, 248, 0.3), transparent);
}

/* 便签墙样式 */
.gb-list.gb-notes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 32px 24px;
  padding: 32px 8px;
  align-items: start;
}
.gb-note {
  padding: 8px;
}
.gb-note :deep(.comment-item) {
  background: transparent;
  box-shadow: none;
  border: none;
  padding: 0;
}

.page-body::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle at 25% 25%, rgba(56, 189, 248, 0.04) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(14, 165, 233, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgba(34, 211, 238, 0.03) 0%, transparent 60%);
  background-size: 100% 100%;
  background-attachment: fixed;
}

/* 主体区域浮动光斑 */
.page-body::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle at 15% 30%, rgba(56, 189, 248, 0.06) 0%, transparent 30%),
    radial-gradient(circle at 85% 60%, rgba(14, 165, 233, 0.05) 0%, transparent 35%),
    radial-gradient(circle at 40% 80%, rgba(34, 211, 238, 0.04) 0%, transparent 40%);
  animation: bg-shift 20s ease-in-out infinite;
}

@keyframes bg-shift {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.05); }
}

/* 写留言按钮 */
.gb-write-btn-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.gb-write-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  box-shadow:
    0 4px 14px rgba(14, 165, 233, 0.3),
    0 0 20px rgba(56, 189, 248, 0.15);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.gb-write-btn::before {
  content: '';
  position: absolute;
  top: 0; left: -100%;
  width: 100%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s ease;
}

.gb-write-btn:hover {
  transform: translateY(-2px);
  box-shadow:
    0 6px 24px rgba(14, 165, 233, 0.4),
    0 0 30px rgba(56, 189, 248, 0.2);
}

.gb-write-btn:hover::before {
  left: 100%;
}

.gb-write-btn:active {
  transform: translateY(0);
}

.gb-write-btn .el-icon {
  font-size: 15px;
}

/* 表单卡片 — 高级玻璃拟态 */
.gb-form-card {
  position: relative;
  z-index: 1;
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: 16px;
  padding: 28px 32px 24px;
  margin-top: 36px;
  box-shadow: var(--shadow-soft);
  transition: box-shadow 0.3s ease;
}

.gb-form-card:hover {
  box-shadow:
    0 6px 30px rgba(14, 165, 233, 0.12),
    0 0 80px rgba(56, 189, 248, 0.06),
    inset 0 1px 0 rgba(255,255,255,0.7);
}

.form-cancel {
  text-align: center;
  margin-bottom: 16px;
  cursor: pointer;
}

.form-cancel span {
  font-size: 13px;
  color: var(--c-ink-soft);
  transition: color 0.2s ease;
}

.form-cancel:hover span {
  color: var(--c-ink);
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.reply-to-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--c-botany-50);
  border-radius: 8px;
  font-size: 13px;
  color: var(--c-ink-soft);
}

.reply-to-bar b {
  color: var(--c-botany-700);
}

.clear-reply {
  margin-left: auto;
  cursor: pointer;
  color: var(--c-ink-soft);
  font-size: 14px;
  transition: color 0.2s ease;
}

.clear-reply:hover {
  color: var(--c-ink);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row-full {
  margin-bottom: 18px;
}
.gb-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-label {
  font-size: 13px;
  color: var(--c-ink-500);
  font-weight: 500;
}

.required {
  color: var(--c-autumn-500);
}

.label-hint {
  font-size: 12px;
  color: var(--c-ink-300);
  font-weight: 400;
}

/* 覆盖 Element Plus 输入框样式 */
.gb-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--c-line) inset;
  padding: 0 14px;
  transition: box-shadow 0.2s ease;
}

.gb-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--c-ink-300) inset;
}

.gb-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--c-botany-500) inset;
}

.gb-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  border: 1px solid var(--c-line);
  padding: 12px 14px;
  resize: vertical;
  transition: border-color 0.2s ease;
}

.gb-textarea :deep(.el-textarea__inner:hover) {
  border-color: var(--c-ink-300);
}

.gb-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--c-botany-500);
  outline: none;
}

.gb-textarea :deep(.el-input__count) {
  background: transparent;
  color: var(--c-ink-300);
  font-size: 12px;
}

/* 表单底部 */
.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.form-hint {
  font-size: 12px;
  color: var(--c-ink-300);
}

.submit-btn {
  border-radius: 999px;
  padding: 0 24px;
  height: 38px;
  font-weight: 500;
}

/* 留言列表 — 气泡墙/便利贴布局 */
.gb-list {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  padding: 20px 0 10px;
}

/* 表单展开动画 */
.form-slide-enter-active,
.form-slide-leave-active {
  transition: all 0.35s ease;
}

.form-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.form-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 640px) {
  .gb-header {
    padding: 40px 0 36px;
  }

  .gb-title {
    font-size: 32px;
  }

  .gb-form-card {
    padding: 20px 18px 18px;
    margin-bottom: 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .form-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .submit-btn {
    width: 100%;
  }
}
</style>
