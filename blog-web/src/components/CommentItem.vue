<template>
  <!-- variant: sticky=留言板便签 | card=留言板卡片 | 默认=文章评论 -->
  <div
    :id="`comment-${comment.id}`"
    class="comment-item"
    :class="[variantClass, { 'is-reply': depth > 0, 'is-pending': comment._pending }]"
    :style="{ scrollMarginTop: '80px' }"
  >
    <div class="ci-card" :style="cardStyle">
      <!-- 图钉（仅便签模式） -->
      <div v-if="variant === 'sticky'" class="ci-pin" :style="{ borderColor: pinColor }"></div>

      <!-- 精选留言徽章 -->
      <div v-if="featured" class="ci-featured-tag">
        <el-icon><Star /></el-icon>
        <span>精选留言</span>
      </div>

      <!-- 头部 -->
      <div class="ci-head">
        <div class="ci-avatar">
          <!-- 首字母 fallback 仅在没头像 / 头像加载失败时显示,避免遮住真实头像 -->
          <span v-if="!hasAvatar || imgError" class="ci-av-fallback" :style="{ backgroundColor: avatarColor }">
            {{ firstLetter(comment.nickname) }}
          </span>
          <img v-if="hasAvatar && !imgError"
            :src="comment.avatar" alt="avatar"
            loading="lazy" decoding="async"
            class="ci-av-img" :class="{ 'is-loaded': imgLoaded }"
            @load="imgLoaded = true" @error="onImgError" />
        </div>
        <div class="ci-meta">
          <div class="ci-name">
            {{ comment.nickname }}
            <span v-if="comment.isAdmin" class="ci-admin">博主</span>
          </div>
          <div class="ci-time">{{ fromNow(comment.createTime) }}</div>
        </div>
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
        <span v-else class="ci-reply-btn" @click="$emit('reply', comment)">
          <el-icon><TopLeft /></el-icon> 回复
        </span>
      </div>
    </div>

    <!-- 子回复（默认折叠,避免某一卡片撑高导致整行留白） -->
    <div v-if="comment.replies && comment.replies.length" class="ci-children">
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
import { computed, ref, watch } from 'vue'
import { TopLeft, Loading, Pointer, Star, WarnTriangleFilled, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { fromNow, renderComment } from '@/utils/format'

const props = defineProps({
  comment: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  index: { type: Number, default: 999 },
  variant: { type: String, default: '' },   // "sticky"=便签, "card"=卡片, ""=文章评论
  featured: { type: Boolean, default: false },  // 精选留言
  maxReplies: { type: Number, default: 3 }      // 卡片/便签模式默认折叠子回复,只露前 N 条
})
const emit = defineEmits(['reply', 'like', 'report', 'mention'])

const imgLoaded = ref(false)
const imgError = ref(false)
const allRepliesShown = ref(false)

// 头像是否存在:有 URL 且 trim 后非空。决定 fallback 是否渲染
const hasAvatar = computed(() => {
  const a = props.comment.avatar
  return !!a && a.trim() !== ''
})

// 头像 URL 变更或重渲染时,重置加载状态,避免新旧头像错乱
watch(hasAvatar, () => {
  imgLoaded.value = false
  imgError.value = false
})

// 头像加载失败 → 触发 fallback 接管
const onImgError = () => { imgError.value = true }

// 子回复折叠:同一卡片内最多展示 maxReplies 条,其余通过"展开 X 条回复"按钮点开
const visibleReplies = computed(() => {
  const list = props.comment.replies || []
  if (allRepliesShown.value || list.length <= props.maxReplies) return list
  return list.slice(0, props.maxReplies)
})
const hiddenRepliesCount = computed(() => {
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
  const avColors = ['#38bdf8', '#0ea5e9', '#22d3ee', '#a78bfa', '#34d399', '#fb7185', '#818cf8']
  return avColors[Math.abs(hashStr(props.comment.nickname)) % avColors.length]
})

const pinColor = computed(() => {
  const c = ['#fbbf24', '#f59e0b', '#0ea5e9', '#38bdf8', '#22d3ee', '#a78bfa']
  return c[Math.abs(hashStr(props.comment.nickname)) % c.length]
})

const cardStyle = computed(() => {
  if (props.variant !== 'sticky') return {}
  const pastelColors = [
    '#fef9e7', '#fef3c7', '#f0f9ff', '#e0f2fe',
    '#f0fdf4', '#dcfce7', '#fef2f2', '#fee2e2',
    '#f5f3ff', '#ede9fe', '#fff7ed', '#ffedd5',
    '#ecfeff', '#cffafe', '#fdf2f8', '#fce7f3',
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
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.05em;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
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
  font-size: 12px; color: var(--c-autumn-500, #c87d3e); font-style: italic;
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
  width: 12px; height: 12px; border-radius: 50%; border: 2px solid; background: #fff;
  z-index: 2; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
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
    display: flex; align-items: center; justify-content: center; color: #fff;
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
.ci-admin { font-size: 12px; padding: 1px 6px; background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700)); color: #fff; border-radius: 999px; font-weight: 500; }
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
  font-size: 12px; color: var(--c-ink-300); cursor: pointer; display: inline-flex; align-items: center; gap: 3px;
  user-select: none; transition: color 0.2s; margin-top: 10px; padding-top: 8px; border-top: 1px dashed rgba(0,0,0,0.08);
  &:hover { color: var(--c-botany-500); }
  .el-icon { font-size: 12px; }
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
.comment-item.is-default:not(.is-reply) > .ci-card { border-bottom: 1px solid var(--c-line-soft); }

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
