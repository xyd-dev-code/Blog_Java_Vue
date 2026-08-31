<template>
  <div class="guestbook-page">
    <!-- Hero -->
    <HeroGuestbook
      title="留言板"
      subtitle="写下你的想法，让这里多一点温度"
      @publish="openForm"
    />

    <div class="container page-body">
      <!-- 骨架 -->
      <div v-if="loading && !allList.length" class="gb-skeleton">
        <div class="sk-featured"></div>
        <div class="sk-grid">
          <div class="sk-card" v-for="i in 6" :key="i"></div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="!allList.length" :description="wx('还没有留言，来抢沙发吧～')" />

      <!-- 内容 -->
      <template v-else>
        <!-- 精选留言（点赞最多的一条） -->
        <section v-if="featuredComment" class="gb-featured">
          <CommentItem
            :key="featuredComment.id"
            :comment="featuredComment"
            variant="card"
            :featured="true"
            @like="onLike"
            @reply="onReply"
            @report="onReport"
            @mention="onMention"
          />
        </section>

        <!-- 留言列表 -->
        <section class="gb-list">
          <div class="gb-grid">
            <CommentItem
              v-for="c in visibleGridList"
              :key="c.id"
              :comment="c"
              variant="card"
              @like="onLike"
              @reply="onReply"
              @report="onReport"
              @mention="onMention"
            />
          </div>
        </section>

        <!-- 无限滚动哨兵 -->
        <div ref="sentinel" class="gb-sentinel" v-if="hasMore">
          <el-icon class="gb-spin"><Loading /></el-icon>
          <span>展开更多 {{ gridList.length - visibleCount }} 条回复</span>
        </div>

        <!-- 底部：到底 + 总数 -->
        <div class="gb-footer" v-if="allList.length">
          <span class="gb-overline">{{ wx('— 已经到底啦 —') }}</span>
          <span class="gb-total">共 {{ totalCount }} 条回复</span>
        </div>
      </template>
    </div>

    <!-- 移动端：悬浮写留言按钮 -->
    <button class="gb-fab" v-if="isMobile" @click="openForm">
      <el-icon><EditPen /></el-icon>
    </button>

    <!-- 写留言弹窗（桌面 + 移动端共用） -->
    <el-dialog v-model="formVisible" :title="replyTo ? '回复 @' + replyTo.nickname : wx('写下你的留言')"
      :width="replyTo ? 'min(480px, 92vw)' : 'min(560px, 92vw)'" :top="'84px'" class="gb-form-dialog" :close-on-click-modal="false"
      @close="replyTo = null">
      <GuestbookForm
        :reply-to="replyTo"
        :nicknames="nicknames"
        :visible="formVisible"
        @submitted="onSubmitted"
        @cancel-reply="replyTo = null"
      />
    </el-dialog>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportVisible" title="举报留言" width="min(500px, 92vw)" :top="'84px'" class="gb-form-dialog">
      <div class="report-body" v-if="reportTarget">
        <p class="report-target">
          举报 <b>@{{ reportTarget.nickname }}</b> 的留言：
          <span class="report-quote">{{ excerpt(reportTarget.content, 60) }}</span>
        </p>
        <label class="form-label">举报原因 <span class="required">*</span></label>
        <el-radio-group v-model="reportForm.reason" class="report-reasons">
          <el-radio v-for="r in reportReasons" :key="r.value" :value="r.value" :label="r.value" border>{{ r.label }}</el-radio>
        </el-radio-group>
        <el-input
          v-model="reportForm.detail"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :placeholder="detailRequired ? '请补充具体内容（必填）' : '补充说明（选填）'"
          class="report-detail" />
        <label class="form-label">联系邮箱 <span class="required">*</span> <span class="label-hint">（便于回复处理进展）</span></label>
        <el-input v-model="reportForm.email" type="email" inputmode="email" placeholder="user@example.com" class="report-email" />
      </div>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { EditPen, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import CommentItem from '@/components/CommentItem.vue'
import HeroGuestbook from '@/components/HeroGuestbook.vue'
import GuestbookForm from '@/components/guestbook/GuestbookForm.vue'
import { guestbookComments, likeComment, reportComment } from '@/api/front'
import { excerpt } from '@/utils/format'

const allList = ref([])
const visibleCount = ref(12)
const loading = ref(false)
const replyTo = ref(null)
const isMobile = ref(false)
const formVisible = ref(false)
const sentinel = ref(null)
let observer = null

const reportVisible = ref(false)
const reportTarget = ref(null)
const reportSubmitting = ref(false)
const reportReasons = [
  { value: 'spam',       label: '广告或垃圾信息' },
  { value: 'abuse',      label: '辱骂或不友善' },
  { value: 'porn',       label: '色情或违规内容' },
  { value: 'plagiarism', label: '抄袭/未授权转载' },
  { value: 'illegal',    label: '违法或政治敏感' },
  { value: 'attack',     label: '人身攻击或造谣' },
  { value: 'offtopic',   label: '与博客主题无关' },
  { value: 'other',      label: '其他', needDetail: true }
]
const reportForm = reactive({ reason: 'spam', detail: '', email: '' })

// 选了"其他"必须补充说明
const detailRequired = computed(() =>
  reportReasons.find(r => r.value === reportForm.reason)?.needDetail === true
)

const PAGE_BATCH = 12

// 仅顶层留言参与精选/网格判断
const topLevelList = computed(() => allList.value.filter(c => !c.parentId))

// 精选 = 仅人工置顶（featured=1），没人设就不显示精选区
const featuredComment = computed(() => {
  const manual = topLevelList.value.filter(c => c.featured === 1 || c.featured === true)
  return manual.length ? manual[0] : null
})

// 网格列表 = 排除精选的所有顶层留言
const gridList = computed(() => {
  if (!featuredComment.value) return topLevelList.value
  return topLevelList.value.filter(c => c.id !== featuredComment.value.id)
})

const visibleGridList = computed(() => gridList.value.slice(0, visibleCount.value))
const hasMore = computed(() => visibleCount.value < gridList.value.length)

const countAll = (arr) => arr.reduce((n, c) => n + 1 + (c.replies ? countAll(c.replies) : 0), 0)
const totalCount = computed(() => countAll(allList.value))

const nicknames = computed(() => {
  const set = new Set()
  const walk = (arr) => arr.forEach(c => { if (c.nickname) set.add(c.nickname); if (c.replies) walk(c.replies) })
  walk(allList.value)
  return [...set]
})

const load = async () => {
  loading.value = true
  try {
    const resp = await guestbookComments()
    allList.value = resp.data || []
    visibleCount.value = Math.min(PAGE_BATCH, gridList.value.length) || 12
  } catch (e) {
    ElMessage.error(wx('留言加载失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (visibleCount.value < gridList.value.length) {
    visibleCount.value = Math.min(visibleCount.value + PAGE_BATCH, gridList.value.length)
  }
}

const setupObserver = () => {
  if (!sentinel.value) return
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) loadMore()
  }, { rootMargin: '120px' })
  observer.observe(sentinel.value)
}

const findNode = (arr, id) => {
  for (const c of arr) {
    if (c.id === id) return c
    if (c.replies) { const f = findNode(c.replies, id); if (f) return f }
  }
  return null
}

const onLike = async (comment) => {
  try {
    const resp = await likeComment(comment.id)
    const d = resp.data || {}
    comment.likeCount = d.likeCount
    comment._liked = d.liked
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.response?.data?.message || e?.message || '操作失败'
    ElMessage.error(msg)
  }
}

// 回复：直接打开表单弹窗(桌面和移动端共用)
const onReply = (comment) => {
  replyTo.value = comment
  formVisible.value = true
}

// @提及 → 滚动定位到对应留言
const onMention = (name) => {
  let targetId = null
  const walk = (arr) => arr.forEach(c => {
    if (!targetId && c.nickname === name) targetId = c.id
    if (c.replies) walk(c.replies)
  })
  walk(allList.value)
  if (targetId) {
    nextTick(() => {
      const el = document.getElementById(`comment-${targetId}`)
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }
}

// 提交成功 → 乐观插入
const onSubmitted = (saved) => {
  if (!saved || !saved.id) { replyTo.value = null; return }
  const node = { ...saved, _pending: true, replies: saved.replies || [] }
  if (replyTo.value && saved.parentId && saved.parentId > 0) {
    const parent = findNode(allList.value, saved.parentId)
    if (parent) {
      if (!parent.replies) parent.replies = []
      parent.replies.unshift(node)
    } else {
      allList.value.unshift(node)
    }
  } else {
    allList.value.unshift(node)
    visibleCount.value = Math.max(visibleCount.value, 1)
  }
  replyTo.value = null
  formVisible.value = false
}

const openForm = () => { replyTo.value = null; formVisible.value = true }

// 举报
const onReport = (comment) => {
  reportTarget.value = comment
  reportForm.reason = 'spam'
  reportForm.detail = ''
  reportForm.email = ''
  reportVisible.value = true
}
const submitReport = async () => {
  if (!reportTarget.value) return
  const email = reportForm.email.trim()
  if (!email) { ElMessage.warning('请填写联系邮箱'); return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { ElMessage.warning('邮箱格式不正确'); return }
  if (detailRequired.value && !reportForm.detail.trim()) {
    ElMessage.warning('选择了「其他」必须补充说明'); return
  }
  reportSubmitting.value = true
  try {
    await reportComment(reportTarget.value.id, {
      reason: reportForm.reason,
      detail: reportForm.detail.trim(),
      email
    })
    ElMessage.success('举报已提交，感谢你的反馈')
    reportVisible.value = false
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.response?.data?.message || e?.message || '举报失败'
    ElMessage.error(msg)
  } finally {
    reportSubmitting.value = false
  }
}

const checkMobile = () => { isMobile.value = window.matchMedia('(max-width: 899px)').matches }
const onResize = () => checkMobile()

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', onResize)
  load().then(() => nextTick(setupObserver))
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (observer) observer.disconnect()
})
</script>

<style scoped lang="scss">
.page-body { position: relative; padding: 30px 0 80px; }

/* 精选留言 */
.gb-featured {
  margin-bottom: 28px;
}
.gb-featured :deep(.ci-card) {
  padding: 22px 26px 18px;
  border: 1px solid var(--c-botany-200);
  background: linear-gradient(135deg, var(--c-bg) 0%, var(--c-botany-50) 100%);
  position: relative;
}

/* 留言网格（瀑布流：CSS columns，列高自适应避免被高卡撑出空白） */
.gb-grid {
  column-count: 3;
  column-gap: 16px;
}
.gb-grid > * {
  break-inside: avoid;
  -webkit-column-break-inside: avoid;
  page-break-inside: avoid;
  margin-bottom: 16px;
}

/* 骨架 */
.gb-skeleton { display: flex; flex-direction: column; gap: 28px; }
.sk-featured {
  height: 160px; border-radius: var(--radius);
  background: linear-gradient(100deg, var(--c-line-soft) 30%, var(--c-botany-50) 50%, var(--c-line-soft) 70%);
  background-size: 200% 100%; animation: sk 1.3s ease-in-out infinite;
}
.sk-grid { column-count: 3; column-gap: 16px; }
.sk-grid > .sk-card {
  break-inside: avoid;
  -webkit-column-break-inside: avoid;
  page-break-inside: avoid;
  margin-bottom: 16px;
  height: 120px; border-radius: var(--radius);
  background: linear-gradient(100deg, var(--c-line-soft) 30%, var(--c-botany-50) 50%, var(--c-line-soft) 70%);
  background-size: 200% 100%; animation: sk 1.3s ease-in-out infinite;
}
@keyframes sk { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* 无限滚动哨兵 */
.gb-sentinel {
  text-align: center; color: var(--c-ink-300); font-size: 13px;
  padding: 32px 0 12px; display: flex; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer;
  transition: color 0.2s;
}
.gb-sentinel:hover { color: var(--c-botany-500); }
.gb-spin { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 底部 */
.gb-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 18px 0; margin-top: 8px;
  border-top: 1px dashed var(--c-line);
  font-size: 12px; color: var(--c-ink-300);
  letter-spacing: 0.05em;
}
.gb-overline { letter-spacing: 0.2em; }
.gb-total { font-weight: 500; color: var(--c-ink-500); }

/* 移动端 FAB */
.gb-fab {
  position: fixed; right: 20px; bottom: 28px; z-index: 50;
  width: 54px; height: 54px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  color: var(--theme-on-primary); font-size: 22px; cursor: pointer;
  box-shadow: 0 8px 24px rgba(var(--theme-primary-strong-rgb), 0.4);
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.2s ease;
}
.gb-fab:active { transform: scale(0.92); }

/* 举报弹窗 */
.report-body { display: flex; flex-direction: column; gap: 12px; }
.report-target { font-size: 13px; color: var(--c-ink-500); margin: 0; }
.report-quote {
  display: block; margin-top: 4px; color: var(--c-ink-300);
  background: var(--c-botany-50); padding: 8px 10px; border-radius: 8px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.report-reasons { display: flex; flex-wrap: wrap; gap: 8px; }
.report-detail, .report-email { margin-top: 2px; }
.report-body .form-label { margin-top: 4px; }

/* 弹窗在窄屏转为底部抽屉（Bottom Sheet）*/
@media (max-width: 640px) {
  :deep(.gb-form-dialog) {
    width: 100% !important;
    margin: 0 !important;
    position: fixed !important;
    bottom: 0 !important;
    left: 0 !important;
    right: 0 !important;
    border-radius: 18px 18px 0 0 !important;
    max-height: 88vh;
    overflow-y: auto;
  }
  /* 遮罩层距离顶部留白 */
  :deep(.el-overlay-dialog) {
    align-items: flex-end !important;
  }
  :deep(.gb-form-dialog .el-dialog__header) {
    border-radius: 18px 18px 0 0;
    padding-top: 14px;
  }
  /* 拖拽手柄视觉提示 */
  :deep(.gb-form-dialog .el-dialog__header)::before {
    content: '';
    display: block;
    width: 36px;
    height: 4px;
    background: var(--c-line);
    border-radius: 2px;
    margin: 0 auto 10px;
  }
}

@media (max-width: 1199px) {
  .gb-grid, .sk-grid { column-count: 2; }
}
@media (max-width: 640px) {
  .gb-grid, .sk-grid { column-count: 1; gap: 12px; }
  .gb-featured { margin-bottom: 20px; }
  .gb-featured :deep(.ci-card) { padding: 18px 18px 14px; }
  .gb-footer { padding: 18px 12px 0; font-size: 12px; }
}
</style>
