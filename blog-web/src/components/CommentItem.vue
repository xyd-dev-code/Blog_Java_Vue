<template>
  <!-- sticky: 留言板便签模式 | default: 文章评论卡片模式 -->
  <div
    class="comment-item"
    :class="{ 'is-reply': depth > 0, 'is-sticky': variant === 'sticky' }"
  >
    <div class="ci-card" :style="cardStyle">
      <!-- 图钉（仅便签模式） -->
      <div v-if="variant === 'sticky'" class="ci-pin" :style="{ borderColor: pinColor }"></div>

      <!-- 头部 -->
      <div class="ci-head">
        <div class="ci-avatar">
          <img v-if="comment.avatar" :src="comment.avatar" alt="avatar" />
          <span v-else class="ci-av-fallback" :style="{ backgroundColor: avatarColor }">
            {{ firstLetter(comment.nickname) }}
          </span>
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

      <!-- 内容 -->
      <div class="ci-body">{{ comment.content }}</div>

      <!-- 底部操作 -->
      <div class="ci-foot">
        <span class="ci-reply-btn" @click="$emit('reply', comment)">
          <el-icon><TopLeft /></el-icon> 回复
        </span>
      </div>
    </div>

    <!-- 子回复 -->
    <div v-if="comment.replies && comment.replies.length" class="ci-children">
      <CommentItem
        v-for="c in comment.replies"
        :key="c.id"
        :comment="c"
        :depth="depth + 1"
        :variant="variant"
        :index="999"
        @reply="$emit('reply', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { TopLeft } from '@element-plus/icons-vue'
import { fromNow } from '@/utils/format'

const props = defineProps({
  comment: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  index: { type: Number, default: 999 },
  variant: { type: String, default: '' }   // "sticky" = 留言板便签模式
})
defineEmits(['reply'])

// ========== 工具函数 ==========
const hashStr = (s) => {
  if (!s) return 0
  let h = 0
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  return h
}

// ========== 头像色（两种模式共用）==========
const avatarColor = computed(() => {
  const avColors = ['#38bdf8', '#0ea5e9', '#22d3ee', '#a78bfa', '#34d399', '#fb7185', '#818cf8']
  return avColors[Math.abs(hashStr(props.comment.nickname)) % avColors.length]
})

// ========== 便签模式专属 ==========
const pastelColors = [
  '#fef9e7', '#fef3c7', '#f0f9ff', '#e0f2fe',
  '#f0fdf4', '#dcfce7', '#fef2f2', '#fee2e2',
  '#f5f3ff', '#ede9fe', '#fff7ed', '#ffedd5',
  '#ecfeff', '#cffafe', '#fdf2f8', '#fce7f3',
]

const pinColor = computed(() => {
  const c = ['#fbbf24', '#f59e0b', '#0ea5e9', '#38bdf8', '#22d3ee', '#a78bfa']
  return c[Math.abs(hashStr(props.comment.nickname)) % c.length]
})

const cardStyle = computed(() => {
  if (props.variant !== 'sticky') return {}
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
/* ==============================
   基础容器
   ============================== */
.comment-item {
  display: flex;
  flex-direction: column;
}
.comment-item.is-reply {
  width: 100%;
}

/* ==============================
   卡片
   ============================== */
.ci-card {
  position: relative;
  display: flex;
  flex-direction: column;
}
/* 便签模式 */
.comment-item.is-sticky .ci-card {
  width: 260px;
  min-height: 160px;
  padding: 20px 20px 16px;
  border-radius: 3px 3px 8px 3px;
}
.comment-item.is-reply.is-sticky .ci-card {
  width: 100%;
  min-height: auto;
  padding: 14px 16px 12px;
  border-radius: 3px 3px 6px 3px;
}
/* 普通评论模式 */
.comment-item:not(.is-sticky) .ci-card {
  padding: 18px 0;
}
.comment-item.is-reply:not(.is-sticky) .ci-card {
  padding: 10px 0 10px 14px;
  border-radius: 8px;
}

/* ==============================
   图钉（仅便签）
   ============================== */
.ci-pin {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px; height: 12px;
  border-radius: 50%;
  border: 2px solid;
  background: #fff;
  z-index: 2;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
  &::after {
    content: '';
    position: absolute;
    top: 2px; left: 2px; right: 2px; bottom: 2px;
    border-radius: 50%;
    background: inherit;
    opacity: 0.5;
  }
}
.comment-item.is-reply .ci-pin { display: none; }

/* ==============================
   头部
   ============================== */
.ci-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.ci-avatar {
  flex-shrink: 0;
  img, .ci-av-fallback {
    width: 36px; height: 36px;
    border-radius: 50%;
    object-fit: cover;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
  }
}
.comment-item.is-reply {
  .ci-avatar img, .ci-av-fallback {
    width: 28px; height: 28px;
    font-size: 11px;
  }
}
.ci-meta { flex: 1; min-width: 0; }
.ci-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-ink);
  display: flex;
  align-items: center;
  gap: 6px;
}
.comment-item.is-reply .ci-name { font-size: 12px; }

.ci-admin {
  font-size: 10px;
  padding: 1px 6px;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  color: #fff;
  border-radius: 999px;
  font-weight: 500;
}
.ci-time {
  font-size: 11px;
  color: var(--c-ink-300);
  margin-top: 2px;
}

/* ==============================
   回复对象
   ============================== */
.ci-reply-to {
  font-size: 11px;
  color: var(--c-ink-300);
  margin-bottom: 4px;
  b { color: var(--c-botany-600); }
}

/* ==============================
   正文
   ============================== */
.ci-body {
  font-size: 13px;
  color: var(--c-ink-500);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  flex: 1;
}
.comment-item.is-reply .ci-body {
  font-size: 12px;
  line-height: 1.6;
}

/* ==============================
   底部操作
   ============================== */
.ci-foot {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed rgba(0, 0, 0, 0.08);
}
.ci-reply-btn {
  font-size: 11px;
  color: var(--c-ink-300);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  user-select: none;
  transition: color 0.2s;
  &:hover { color: var(--c-botany-500); }
  .el-icon { font-size: 11px; }
}

/* ==============================
   子回复区域
   ============================== */
.ci-children {
  margin-top: 6px;
  margin-left: 20px;
  padding-left: 14px;
  border-left: 2px dashed rgba(0, 0, 0, 0.08);
}

/* ==============================
   普通评论模式的分隔线
   ============================== */
.comment-item:not(.is-sticky):not(.is-reply) > .ci-card {
  border-bottom: 1px solid var(--c-line-soft);
}

/* ==============================
   响应式
   ============================== */
@media (max-width: 640px) {
  .comment-item.is-sticky .ci-card {
    width: 100%;
    min-height: auto;
  }
}
</style>
