<template>
  <!-- variant: sticky=留言板便签 | card=留言板卡片 | 默认=文章评论 -->
  <div
    :id="`comment-${comment.id}`"
    class="comment-item"
    :class="[variantClass, { 'is-reply': depth > 0, 'is-pending': comment._pending, 'is-author': comment.isAdmin }]"
    :style="{ scrollMarginTop: '80px' }"
  >
    <div class="ci-card" :style="cardStyle">
      <!-- 图钉（仅便签模式） -->
      <div v-if="variant === 'sticky'" class="ci-pin" :style="{ borderColor: pinColor }"></div>

      <!-- 精选留言徽章 -->
      <div v-if="featured" class="ci-featured-tag">
        <el-icon><Star /></el-icon>
        <span>{{ wx('精选留言') }}</span>
      </div>

      <!-- 头部 -->
      <div class="ci-head">
        <div class="ci-avatar">
          <!-- 博主头像失败时先尝试当前站点 Logo，仍失败才显示首字母 -->
          <span v-if="!hasAvatar || imgError" class="ci-av-fallback" :style="{ backgroundColor: avatarColor }">
            {{ firstLetter(comment.nickname) }}
          </span>
          <img v-if="hasAvatar && !imgError"
            :src="avatarSrc" :alt="`${comment.nickname || '用户'}的头像`"
            loading="lazy" decoding="async"
            class="ci-av-img" :class="{ 'is-loaded': imgLoaded }"
            @load="imgLoaded = true" @error="onImgError" />
        </div>
        <div class="ci-meta">
          <div class="ci-name">
            {{ comment.nickname }}
          </div>
          <div class="ci-time">{{ fromNow(comment.createTime) }}</div>
        </div>
        <span v-if="!variant && depth === 0 && index !== 999" class="ci-floor">{{ String(index + 1).padStart(2, '0') }}</span>
      </div>

      <!-- 回复对象提示 -->
      <div v-if="depth > 0 && comment.parentName" class="ci-reply-to">
        ↳ 回复 <b>@{{ comment.parentName }}</b>
      </div>

      <!-- 内容（卡片/文章模式走安全渲染：URL 识别 + @提及 + 换行） -->
      <div class="ci-body" :class="{ 'ci-body-raw': variant === 'sticky' }"
           @click="onBodyClick" v-html="renderedContent"></div>

      <!-- 待审核标记(乐观插入的新评论,status=0) -->
      <div v-if="comment._pending" class="ci-pending-tag">
        <el-icon><Loading /></el-icon> 审核中
      </div>

      <!-- 底部操作（卡片模式显示点赞/举报，便签/文章仅回复） -->
      <div class="ci-foot">
        <template v-if="variant === 'card'">
          <span class="ci-act" :class="{ 'is-liked': comment._liked }" @click="$emit('like', comment)">
            <el-icon><Star v-if="comment._liked" /><Pointer v-else /></el-icon>
            <span>赞</span><i v-if="likeCount > 0">{{ likeCount }}</i>
          </span>
          <span class="ci-act" @click="$emit('reply', comment)">
            <el-icon><TopLeft /></el-icon><span>回复</span>
          </span>
          <span class="ci-act ci-report" @click="$emit('report', comment)">
            <el-icon><WarnTriangleFilled /></el-icon><span>举报</span>
          </span>
        </template>
        <button v-else type="button" class="ci-reply-btn" @click="$emit('reply', comment)">
          <el-icon><TopLeft /></el-icon> 回复
        </button>
      </div>
    </div>

    <!-- 子回复（默认折叠,避免某一卡片撑高导致整行留白） -->
    <div v-if="comment.replies?.length || comment.hasMoreReplies" class="ci-children">
      <CommentItem
        v-for="c in visibleReplies"
        :key="c.id"
        :comment="c"
        :depth="depth + 1"
        :variant="variant"
        :max-replies="maxReplies"
        :index="999"
        @reply="$emit('reply', $event)"
        @like="$emit('like', $event)"
        @report="$emit('report', $event)"
        @mention="$emit('mention', $event)"
      />
      <button v-if="comment.hasMoreReplies" type="button" class="ci-expand" :disabled="loadingReplies" @click="loadMoreReplies">
        {{ loadingReplies ? '加载中…' : '加载更多回复' }}
      </button>
      <!-- 折叠控制 -->
      <div v-if="hiddenRepliesCount > 0" class="ci-expand" @click="allRepliesShown = true">
        <el-icon><ArrowDown /></el-icon>
        <span>展开 {{ hiddenRepliesCount }} 条回复</span>
      </div>
      <div v-else-if="canCollapse" class="ci-expand" @click="allRepliesShown = false">
        <el-icon><ArrowUp /></el-icon>
        <span>收起回复</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { computed, ref, watch } from 'vue'
import { TopLeft, Loading, Pointer, Star, WarnTriangleFilled, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { commentReplies } from '@/api/front'
import { fromNow, renderComment } from '@/utils/format'
import { useSiteStore } from '@/stores/site'

const props = defineProps({
  comment: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  index: { type: Number, default: 999 },
  variant: { type: String, default: '' },   // "sticky"=便签, "card"=卡片, ""=文章评论
  featured: { type: Boolean, default: false },  // 精选留言
  maxReplies: { type: Number, default: 3 }      // 卡片/便签模式默认折叠子回复,只露前 N 条
})
const emit = defineEmits(['reply', 'like', 'report', 'mention'])
const siteStore = useSiteStore()

const imgLoaded = ref(false)
const imgError = ref(false)
const avatarSrc = ref('')
const usingSiteFallback = ref(false)
const allRepliesShown = ref(false)
const loadingReplies = ref(false)
const loadMoreReplies = async () => {
  if (loadingReplies.value) return
  loadingReplies.value = true
  try {
    const existing = props.comment.replies || []
    const afterId = existing.filter(c => !c._pending).reduce((last, c) => Math.max(last, c.id), 0)
    const { data } = await commentReplies(props.comment.id, { afterId, size: 50 })
    const ids = new Set(existing.map(c => c.id))
    props.comment.replies = [...existing, ...(data.records || []).filter(c => !ids.has(c.id))]
    props.comment.hasMoreReplies = !!data.hasMore
    allRepliesShown.value = true
  } catch (_) {
    // The API interceptor reports the error; retain the cursor so the user can retry.
  } finally {
    loadingReplies.value = false
  }
}

// 头像是否存在:有 URL 且 trim 后非空。决定 fallback 是否渲染
const hasAvatar = computed(() => {
  const a = avatarSrc.value
  return !!a && a.trim() !== ''
})

// 头像 URL 变更或重渲染时,重置加载状态,避免新旧头像错乱
watch(
  [() => props.comment.avatar, () => props.comment.isAdmin ? siteStore.info?.siteLogo : ''],
  () => {
    avatarSrc.value = props.comment.avatar?.trim() || ''
    usingSiteFallback.value = false
    imgLoaded.value = false
    imgError.value = false
  },
  { immediate: true }
)

// 博主头像失效时尝试当前站点 Logo；普通访客或 Logo 也失效时才显示首字母。
const onImgError = () => {
  const siteLogo = props.comment.isAdmin ? siteStore.info?.siteLogo?.trim() : ''
  if (!usingSiteFallback.value && siteLogo && siteLogo !== avatarSrc.value) {
    usingSiteFallback.value = true
    avatarSrc.value = siteLogo
    imgLoaded.value = false
    return
  }
  avatarSrc.value = ''
  imgLoaded.value = false
  imgError.value = true
}

// 子回复折叠:同一卡片内最多展示 maxReplies 条,其余通过"展开 X 条回复"按钮点开
const visibleReplies = computed(() => {
  const list = props.comment.replies || []
  if (allRepliesShown.value || list.length <= props.maxReplies) return list
  return list.slice(0, props.maxReplies)
})
const hiddenRepliesCount = computed(() => {
  if (allRepliesShown.value) return 0
  const total = (props.comment.replies || []).length
  return Math.max(0, total - props.maxReplies)
})
const canCollapse = computed(() => allRepliesShown.value && (props.comment.replies || []).length > props.maxReplies)

const renderedContent = computed(() => renderComment(props.comment.content))
const likeCount = computed(() => props.comment.likeCount || 0)

const variantClass = computed(() => {
  if (props.variant === 'sticky') return 'is-sticky'
  if (props.variant === 'card') return 'is-card'
  return 'is-default'
})

// 点击 @提及 → 向上抛出，由父组件(Guestbook)滚动定位
const onBodyClick = (e) => {
  const t = e.target.closest('.cmt-mention')
  if (t && t.dataset.name) {
    e.preventDefault()
    emit('mention', t.dataset.name)
  }
}

// ========== 工具函数 ==========
const hashStr = (s) => {
  if (!s) return 0
  let h = 0
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  return h
}

const avatarColor = computed(() => {
  const avColors = [
    'var(--c-botany-500)', 'var(--c-botany-700)', 'var(--c-cyan-500)',
    'var(--c-plum-300)', 'var(--c-emerald-400)', 'var(--c-rose-400)', 'var(--c-indigo-400)',
  ]
  return avColors[Math.abs(hashStr(props.comment.nickname)) % avColors.length]
})

const pinColor = computed(() => {
  const c = [
    'var(--c-autumn-500)', 'var(--c-autumn-700)', 'var(--c-botany-700)',
    'var(--c-botany-500)', 'var(--c-cyan-500)', 'var(--c-plum-300)',
  ]
  return c[Math.abs(hashStr(props.comment.nickname)) % c.length]
})

const cardStyle = computed(() => {
  if (props.variant !== 'sticky') return {}
  const pastelColors = [
    'var(--c-note-yellow)', 'var(--c-autumn-100)', 'var(--c-botany-50)', 'var(--c-botany-100)',
    'var(--c-note-green)', 'var(--c-note-green-strong)', 'var(--c-danger-soft)', 'var(--c-note-red)',
    'var(--c-note-plum)', 'var(--c-plum-100)', 'var(--c-warning-soft)', 'var(--c-note-orange)',
    'var(--c-cyan-50)', 'var(--c-cyan-100)', 'var(--c-note-pink)', 'var(--c-note-pink-strong)',
  ]
  const idx = Math.abs(hashStr(props.comment.nickname)) % pastelColors.length
  return { backgroundColor: pastelColors[idx] }
})

const firstLetter = (name) => {
  if (!name) return '?'
  const char = name.trim().charAt(0)
  return /[a-zA-Z\u4e00-\u9fa5]/.test(char) ? char.toUpperCase() : name.trim().charAt(0)
}
</script>

<style scoped lang="scss">
/* ============================== 精选留言徽章 ============================== */
.ci-featured-tag {
  position: absolute;
  top: 14px;
  right: 16px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: linear-gradient(135deg, var(--c-autumn-500), var(--c-autumn-700));
  color: var(--theme-on-primary);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.05em;
  box-shadow: 0 2px 6px rgba(var(--theme-accent-strong-rgb), 0.3);
  z-index: 2;
}
.ci-featured-tag .el-icon { font-size: 12px; }

/* ============================== 基础容器 ============================== */
.comment-item { display: flex; flex-direction: column; }
.comment-item.is-reply { width: 100%; }

.comment-item.is-pending { opacity: 0.55; transition: opacity 0.6s ease; }
.comment-item.is-pending:hover { opacity: 0.85; }
.ci-pending-tag {
  display: inline-flex; align-items: center; gap: 4px; margin-top: 8px;
  font-size: 12px; color: var(--c-autumn-500); font-style: italic;
}
.ci-pending-tag .el-icon { animation: spin 1.4s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* ============================== 卡片模式 ============================== */
.comment-item.is-card .ci-card {
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-soft);
  padding: 18px 20px 14px;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
}
.comment-item.is-card:hover > .ci-card {
  transform: translateY(-3px);
  box-shadow: var(--shadow-pop);
  border-color: var(--c-botany-100);
}
.comment-item.is-card.is-reply > .ci-card {
  background: var(--c-botany-50);
  border-color: var(--c-botany-100);
  box-shadow: none;
}
.comment-item.is-card.is-reply:hover > .ci-card { transform: none; box-shadow: none; }

/* ============================== 便签模式 ============================== */
.comment-item.is-sticky .ci-card { width: 260px; min-height: 160px; padding: 20px 20px 16px; border-radius: 3px 3px 8px 3px; }
.comment-item.is-reply.is-sticky .ci-card { width: 100%; min-height: auto; padding: 14px 16px 12px; border-radius: 3px 3px 6px 3px; }

/* ============================== 文章评论模式 ============================== */
.comment-item.is-default .ci-card { padding: 18px 0; }
.comment-item.is-reply.is-default .ci-card { padding: 10px 0 10px 14px; border-radius: 8px; }

/* ============================== 图钉 ============================== */
.ci-pin {
  position: absolute; top: -6px; left: 50%; transform: translateX(-50%);
  width: 12px; height: 12px; border-radius: 50%; border: 2px solid; background: var(--c-paper);
  z-index: 2; box-shadow: 0 1px 2px rgba(var(--theme-black-rgb), 0.15);
  &::after { content: ''; position: absolute; top: 2px; left: 2px; right: 2px; bottom: 2px; border-radius: 50%; background: inherit; opacity: 0.5; }
}
.comment-item.is-reply .ci-pin { display: none; }

/* ============================== 头部 ============================== */
.ci-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.ci-avatar {
  position: relative;
  flex-shrink: 0;
  width: 36px; height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--c-line-soft);  /* img 加载中:浅灰占位 */
  img, .ci-av-fallback {
    width: 36px; height: 36px; border-radius: 50%; object-fit: cover;
    display: flex; align-items: center; justify-content: center; color: var(--theme-on-primary);
    font-size: 13px; font-weight: 600;
  }
  .ci-av-img {
    position: absolute; inset: 0;
    opacity: 0;
    transition: opacity 0.18s ease;
  }
  .ci-av-img.is-loaded { opacity: 1; }
}
.comment-item.is-reply .ci-avatar { width: 28px; height: 28px; }
.comment-item.is-reply .ci-avatar img, .comment-item.is-reply .ci-avatar .ci-av-fallback { width: 28px; height: 28px; font-size: 12px; }
.ci-meta { flex: 1; min-width: 0; }
.ci-name { font-size: 13px; font-weight: 600; color: var(--c-ink); display: flex; align-items: center; gap: 6px; }
.comment-item.is-reply .ci-name { font-size: 12px; }
.ci-time { font-size: 12px; color: var(--c-ink-300); margin-top: 2px; }

/* ============================== 回复对象 ============================== */
.ci-reply-to { font-size: 12px; color: var(--c-ink-300); margin-bottom: 4px; b { color: var(--c-botany-600); } }

/* ============================== 正文 ============================== */
.ci-body { font-size: 13px; color: var(--c-ink-500); line-height: 1.7; white-space: pre-wrap; word-break: break-word; flex: 1; }
.comment-item.is-reply .ci-body { font-size: 12px; line-height: 1.6; }
.ci-body-raw { white-space: pre-wrap; }

.ci-body :deep(.cmt-link) { color: var(--c-botany-700); text-decoration: underline; word-break: break-all; }
.ci-body :deep(.cmt-mention) {
  color: var(--c-botany-700); font-weight: 600; cursor: pointer;
  background: var(--c-botany-50); padding: 0 4px; border-radius: 4px;
}
.ci-body :deep(.cmt-mention:hover) { background: var(--c-botany-100); }

/* ============================== 卡片模式底部操作 ============================== */
.comment-item.is-card .ci-foot {
  display: flex; align-items: center; gap: 18px; margin-top: 12px; padding-top: 10px;
  border-top: 1px dashed var(--c-line); font-size: 12px; color: var(--c-ink-300);
}
.ci-act { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; user-select: none; transition: color 0.2s; }
.ci-act:hover { color: var(--c-botany-500); }
.ci-act .el-icon { font-size: 14px; }
.ci-act i { font-style: normal; }
.ci-act.is-liked { color: var(--c-botany-500); }
.ci-report:hover { color: var(--c-autumn-500); }

/* ============================== 便签/文章模式回复按钮 ============================== */
.ci-reply-btn {
  font-family: inherit; background: transparent; border: 0;
  font-size: 12px; color: var(--c-ink-300); cursor: pointer; display: inline-flex; align-items: center; gap: 3px;
  user-select: none; transition: color 0.2s; margin-top: 10px; padding-top: 8px; border-top: 1px dashed rgba(var(--theme-black-rgb), 0.08);
  &:hover { color: var(--c-botany-500); }
  .el-icon { font-size: 12px; }
}

/* 文章讨论：头像与正文形成同一阅读列，回复以浅底和连接线区分。 */
.comment-item.is-default {
  > .ci-card { padding: 22px 0 18px; }
  > .ci-card > .ci-head { gap: 12px; margin-bottom: 10px; }
  > .ci-card > .ci-head .ci-name { font-size: 14px; flex-wrap: wrap; overflow-wrap: anywhere; }
  > .ci-card > .ci-head .ci-time { color: var(--c-ink-soft); }
  > .ci-card > .ci-body { margin-left: 48px; font-size: 14px; line-height: 1.85; color: var(--c-ink); }
  > .ci-card > .ci-foot { display: flex; justify-content: flex-end; margin-left: 48px; margin-top: 10px; }
  > .ci-card > .ci-foot .ci-reply-btn {
    margin: 0; padding: 5px 12px; min-height: 32px; border: 1px solid var(--c-line);
    border-radius: 6px; color: var(--c-ink-soft); gap: 5px;
    &:hover { color: var(--c-autumn-700); border-color: var(--c-autumn-500); background: var(--c-paper-soft); }
    &:focus-visible { outline: 2px solid var(--c-autumn-500); outline-offset: 3px; }
  }
  > .ci-children { margin: 0 0 18px 48px; padding: 0 16px; background: var(--c-paper-soft); border-left: 2px solid var(--c-line); border-radius: 0 8px 8px 0; }
  &.is-reply > .ci-card { padding: 14px 0; }
  &.is-reply > .ci-card > .ci-body,
  &.is-reply > .ci-card > .ci-foot,
  &.is-reply > .ci-card > .ci-reply-to { margin-left: 40px; }
  &.is-reply > .ci-children { margin-left: 12px; padding-right: 0; }
}
@media (max-width: 600px) {
  .comment-item.is-default {
    > .ci-card > .ci-body, > .ci-card > .ci-foot { margin-left: 0; }
    > .ci-card > .ci-foot .ci-reply-btn { min-height: 44px; }
    > .ci-children { margin-left: 10px; padding: 0 10px; }
    &.is-reply > .ci-card > .ci-body, &.is-reply > .ci-card > .ci-foot, &.is-reply > .ci-card > .ci-reply-to { margin-left: 0; }
  }
}

/* ============================== 子回复区 ============================== */
.ci-children { margin-top: 6px; margin-left: 18px; padding-left: 14px; border-left: 2px dashed var(--c-line); }
.ci-expand {
  display: inline-flex; align-items: center; gap: 4px;
  margin-top: 4px; padding: 4px 10px;
  font-size: 12px; color: var(--c-botany-600);
  cursor: pointer; user-select: none;
  background: transparent; border: none;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
}
.ci-expand:hover { color: var(--c-botany-700); background: var(--c-botany-50); }
.ci-expand .el-icon { font-size: 12px; }

/* ============================== 文章模式分隔线 ============================== */
.comment-item.is-default:not(.is-reply) {
  border: 1px solid var(--c-line); border-radius: 12px; background: var(--c-paper);
  box-shadow: 0 3px 14px rgba(var(--theme-primary-deep-rgb), .035);
  > .ci-card { padding: 22px 24px 18px; }
  > .ci-children { margin: 0 24px 22px 72px; }
}
.ci-floor { align-self: flex-start; font-family: var(--font-serif); font-size: 26px; font-style: italic; color: var(--c-ink-soft); opacity: .45; line-height: 1; }
.comment-item.is-default .ci-avatar { box-shadow: 0 0 0 3px var(--c-paper), 0 0 0 4px var(--c-line); }
.comment-item.is-default.is-author { border-left-color: var(--c-autumn-500); }
.comment-item.is-default > .ci-card > .ci-foot .ci-reply-btn { background: var(--c-paper-soft); border-color: transparent; color: var(--c-ink); }
@media (max-width: 600px) {
  .comment-item.is-default:not(.is-reply) {
    > .ci-card { padding: 18px 14px 12px; }
    > .ci-children { margin: 0 12px 14px; }
  }
}

/* ============================== 响应式 ============================== */
@media (max-width: 640px) {
  .comment-item.is-sticky .ci-card { width: 100%; min-height: auto; }
  .ci-children { margin-left: 10px; padding-left: 10px; }
}
@media (max-width: 480px) {
  .comment-item.is-card .ci-card { padding: 14px 14px 12px; }
  .ci-body { font-size: 14px; }
}
</style>
