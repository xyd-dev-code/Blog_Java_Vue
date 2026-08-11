<template>
  <GradientBorderCard
    :variant="cardVariant"
    hoverable
    v-tilt
    class="friend-card"
    :class="`group-${groupClass}`"
  >
    <div class="fc-top">
      <div class="fc-avatar-wrap">
        <img v-if="friend.avatar" :src="friend.avatar" class="avatar avatar-img" :alt="friend.name" loading="lazy" />
        <div v-else class="avatar avatar-placeholder">{{ (friend.name || '?')[0] }}</div>
        <span class="fc-avatar-ring" aria-hidden="true"></span>
      </div>
      <div class="meta">
        <h4>
          {{ friend.name }}
          <span class="meta-badge" v-if="friend.recommended === 1">推荐</span>
        </h4>
        <p class="url">
          <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/>
          </svg>
          <span>{{ friendlyUrl(friend.url) }}</span>
        </p>
      </div>
    </div>
    <p v-if="friend.description" class="desc">{{ friend.description }}</p>
    <p v-else class="desc desc-empty">这位朋友比较低调，暂无简介～</p>

    <div class="fc-foot">
      <span class="fc-tag">
        <span class="fc-tag-dot" :class="`tag-${groupClass}`"></span>
        {{ groupFootTag }}
      </span>
      <span class="fc-arrow" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="7" y1="17" x2="17" y2="7"/>
          <polyline points="7 7 17 7 17 17"/>
        </svg>
      </span>
    </div>
  </GradientBorderCard>
</template>

<script setup>
import { computed } from 'vue'
import GradientBorderCard from '@/components/GradientBorderCard.vue'

const props = defineProps({
  friend: { type: Object, required: true },
  group: { type: String, default: 'net' }
})

// 与 Friends.vue 的 groupClass 对齐：分组名是中文（好友/网友/推荐/置顶）
const groupClass = computed(() => {
  const g = props.group || ''
  if (g === '好友') return 'friend'
  if (g === '网友') return 'net'
  if (g === '推荐' || g === '置顶') return 'star'
  return 'misc'
})

// 与 Friends.vue 的 variantFor 对齐：好友→sun，推荐/置顶→mix，其它→sky
const cardVariant = computed(() => {
  const g = props.group || ''
  if (g === '好友') return 'sun'
  if (g === '推荐' || g === '置顶') return 'mix'
  return 'sky'
})

// 与 Friends.vue 的 groupFootTag 对齐
const groupFootTag = computed(() => {
  const g = props.group || ''
  if (g === '好友') return '挚友'
  if (g === '网友') return '网友'
  if (g === '推荐' || g === '置顶') return '推荐'
  return '友链'
})

// 与 Friends.vue 的 friendlyUrl 对齐：去协议与尾部斜杠，保留完整 host+path
function friendlyUrl(u = '') {
  return (u || '').replace(/^https?:\/\//, '').replace(/\/$/, '')
}
</script>

<style scoped lang="scss">
.friend-card {
  padding: 20px 22px 16px;
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}
.friend-card::after {
  content: '';
  position: absolute;
  left: 0; bottom: 0;
  width: 0; height: 3px;
  background: linear-gradient(90deg, #38bdf8, #fbbf24);
  transition: width 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 0 3px 0 0;
}
.friend-card.group-friend::after { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
.friend-card.group-net::after   { background: linear-gradient(90deg, #38bdf8, #22d3ee); }
.friend-card.group-star::after  { background: linear-gradient(90deg, #a855f7, #ec4899); }
.friend-card:hover::after { width: 100%; }

.fc-top {
  display: flex;
  align-items: center;
  gap: 14px;
}
.fc-avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 52px; height: 52px;
}
.avatar {
  width: 100%; height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background: #f1f5f9;
  position: relative;
  z-index: 2;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.avatar-img { display: block; }
.avatar-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #38bdf8, #22d3ee);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
}
.friend-card.group-friend .avatar-placeholder {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}
.friend-card.group-star .avatar-placeholder {
  background: linear-gradient(135deg, #a855f7, #ec4899);
}
.fc-avatar-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(56,189,248,0.6), rgba(251,191,36,0.6));
  z-index: 1;
  opacity: 0.55;
  transition: opacity 0.3s ease, transform 0.4s ease;
}
.friend-card.group-friend .fc-avatar-ring { background: linear-gradient(135deg, rgba(251,191,36,0.6), rgba(245,158,11,0.6)); }
.friend-card.group-star .fc-avatar-ring  { background: linear-gradient(135deg, rgba(168,85,247,0.6), rgba(236,72,153,0.6)); }
.friend-card:hover .avatar { transform: scale(1.05) rotate(-3deg); }
.friend-card:hover .fc-avatar-ring { opacity: 1; transform: scale(1.08); }

.friend-card :deep(.meta) { min-width: 0; flex: 1; }
.friend-card :deep(.meta h4) {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--c-ink);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}
.friend-card:hover :deep(.meta h4) { color: #0369a1; }
.meta-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 500;
  color: #b45309;
  background: linear-gradient(135deg, rgba(251,191,36,0.18), rgba(245,158,11,0.10));
  border: 1px solid rgba(245,158,11,0.30);
  border-radius: 999px;
  letter-spacing: 0.04em;
  flex-shrink: 0;
  transform: translateY(-1px);
}
.friend-card :deep(.meta .url) {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s ease;
}
.friend-card:hover :deep(.meta .url) { color: #0ea5e9; }

.desc {
  font-size: 13px;
  color: var(--c-ink-soft);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}
.desc-empty { color: #cbd5e1; font-style: italic; }

.fc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 4px;
}
.fc-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--c-ink-soft);
  letter-spacing: 0.04em;
}
.fc-tag-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #38bdf8;
  box-shadow: 0 0 6px currentColor;
}
.tag-friend { background: #fbbf24; color: #f59e0b; }
.tag-net    { background: #38bdf8; color: #0ea5e9; }
.tag-star   { background: #a855f7; color: #a855f7; }
.tag-misc   { background: #94a3b8; color: #64748b; }

.fc-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px; height: 28px;
  border-radius: 50%;
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(125, 211, 252, 0.3);
  transition: all 0.3s ease;
}
.friend-card:hover .fc-arrow {
  background: linear-gradient(135deg, #38bdf8, #0ea5e9);
  color: #fff;
  border-color: transparent;
  transform: translate(2px, -2px) rotate(-3deg);
}
</style>
