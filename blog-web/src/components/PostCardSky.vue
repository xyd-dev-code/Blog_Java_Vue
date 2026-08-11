<template>
  <GradientBorderCard
    variant="sky"
    hoverable
    v-tilt
    class="post-card-sky"
    @click="$router.push(`/articles/${article.slug}`)"
  >
    <!-- 封面 -->
    <div class="cover" :class="{ 'is-placeholder': !article.coverImage }">
      <img v-if="article.coverImage" :src="article.coverImage" :alt="article.title" loading="lazy" />
      <span v-else class="cover-char">{{ firstChar }}</span>
      <span v-if="readMinutes" class="cover-badge">{{ readMinutes }} 分钟</span>
    </div>

    <!-- 内容 -->
    <div class="body">
      <div class="meta-top">
        <span v-for="t in (article.tags || []).slice(0, 2)" :key="t.id" class="chip">{{ t.name }}</span>
        <span class="dot">·</span>
        <span class="date">{{ fmtDate(article.createTime) }}</span>
      </div>
      <h3 class="title">{{ article.title }}</h3>
      <p class="excerpt">{{ excerpt(article.summary || article.content, 100) }}</p>
      <div class="meta-bottom">
        <div class="author">
          <img v-if="authorAvatar" :src="authorAvatar" class="avatar avatar-img" alt="" loading="lazy" decoding="async" />
          <div v-else class="avatar"></div>
          <span>{{ authorName }}</span>
        </div>
        <div class="stats">
          <span>👁 {{ article.viewCount || 0 }}</span>
          <span>💬 {{ article.commentCount || 0 }}</span>
        </div>
      </div>
    </div>
  </GradientBorderCard>
</template>

<script setup>
import { computed } from 'vue'
import { fmtDate, excerpt } from '@/utils/format'
import { useAuthor } from '@/composables/useAuthor'
import GradientBorderCard from './GradientBorderCard.vue'

const props = defineProps({
  article: { type: Object, required: true }
})

const { authorName, authorAvatar } = useAuthor(props.article)
const firstChar = computed(() => (props.article.title || '山')[0])

const readMinutes = computed(() => {
  const words = (props.article.content || '').length
  if (!words) return null
  return Math.max(1, Math.round(words / 400))
})
</script>

<style scoped lang="scss">
.post-card-sky {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1),
              box-shadow 0.35s ease;
  position: relative;

  /* 悬浮上移 + 淡蓝光晕 */
  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(56,189,248,0.15), 0 4px 12px rgba(14,165,233,0.1);
  }
}

.cover {
  position: relative;
  aspect-ratio: 16/9;
  background: linear-gradient(135deg, #bae6fd 0%, #67e8f9 60%, #fde68a 100%);
  overflow: hidden;
}
.cover img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}
.post-card-sky:hover .cover img { transform: scale(1.06); }

.cover.is-placeholder {
  display: grid;
  place-items: center;
}
.cover-char {
  font-family: var(--font-serif);
  font-size: 72px;
  color: rgba(255,255,255,0.85);
  font-weight: 500;
}
.cover-badge {
  position: absolute;
  right: 12px; top: 12px;
  padding: 4px 10px;
  background: rgba(15, 23, 42, 0.55);
  color: #fff;
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(6px);
}

.body {
  padding: 18px 20px 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.meta-top {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--c-ink-soft, #64748b);
  margin-bottom: 10px;
}
.chip {
  padding: 2px 10px;
  background: #f0f9ff;
  color: #0369a1;
  border-radius: 999px;
  border: 1px solid #e0f2fe;
  font-size: 12px;
}
.chip:nth-child(2) {
  background: #ecfeff;
  color: #0e7490;
  border-color: #cffafe;
}
.dot { color: #cbd5e1; }
.date { color: var(--c-ink-soft, #64748b); }

.title {
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--c-ink, #1e293b);
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
}
.post-card-sky:hover .title { color: #0369a1; }

.excerpt {
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--c-ink-soft, #64748b);
  margin: 0 0 14px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
  font-size: 12px;
  color: var(--c-ink-soft, #64748b);
}
.author {
  display: flex;
  align-items: center;
  gap: 8px;
}
.avatar {
  width: 24px; height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #22d3ee);
}
.avatar-img { object-fit: cover; }
.stats {
  display: flex;
  gap: 10px;
}

/* mobile */
@media (max-width: 480px) {
  .cover-char { font-size: 48px; }
  .body { padding: 14px 16px 12px; }
  .title { font-size: 16px; }
  .meta-top { gap: 6px; font-size: 12px; }
  .excerpt { font-size: 13px; }
  .stats { font-size: 12px; gap: 6px; }
}
</style>