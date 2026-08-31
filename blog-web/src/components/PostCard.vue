<template>
  <article
    class="post-card reveal"
    v-tilt
    @click="$router.push(`/articles/${article.slug}`)"
    @mousemove="onGlowMove"
    ref="cardRef"
  >
    <!-- 鼠标跟随径向光晕（真实 DOM，避免 glow-card/border-glow 伪元素冲突） -->
    <div class="card-radial-glow" aria-hidden="true"></div>
    <div class="cover" v-if="article.coverImage">
      <img :src="article.coverImage" :alt="article.title" loading="lazy" />
    </div>
    <div class="cover cover-placeholder" v-else>
      <span class="cover-char">{{ firstChar }}</span>
    </div>
    <div class="meta">
      <div class="tags" v-if="article.tags && article.tags.length">
        <span v-for="t in article.tags.slice(0, 2)" :key="t.id" class="tag-pill">{{ t.name }}</span>
      </div>
      <h3 class="title">{{ article.title }}</h3>
      <p class="excerpt">{{ excerpt(article.summary || article.content, 100) }}</p>
      <div class="footer">
        <span class="date">{{ fmtDate(article.createTime) }}</span>
        <span class="dot">·</span>
        <span class="views">{{ article.viewCount || 0 }} 阅读</span>
        <span class="dot">·</span>
        <span class="comments">{{ article.commentCount || 0 }} {{ wx('评论') }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { computed, ref } from 'vue'
import { fmtDate, excerpt } from '@/utils/format'

const props = defineProps({
  article: { type: Object, required: true }
})

const cardRef = ref(null)
const firstChar = computed(() => (props.article.title || '山')[0])

const onGlowMove = (e) => {
  if (!cardRef.value) return
  const rect = cardRef.value.getBoundingClientRect()
  cardRef.value.style.setProperty('--mx', `${((e.clientX - rect.left) / rect.width) * 100}%`)
  cardRef.value.style.setProperty('--my', `${((e.clientY - rect.top) / rect.height) * 100}%`)
}
</script>

<style scoped lang="scss">
.post-card {
  background: var(--c-paper);
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--c-line-soft);
  transition: transform 0.35s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.35s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}
.post-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--c-botany-500), var(--c-autumn-500));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 2;
}
.post-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: var(--shadow-pop), var(--shadow-glow);
}
.post-card:hover::before { opacity: 1; }

/* 鼠标跟随径向光晕（真实 DOM，不占用 ::before/::after） */
.card-radial-glow {
  position: absolute;
  inset: 0;
  top: var(--my, 50%);
  left: var(--mx, 50%);
  width: 300px;
  height: 300px;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(var(--theme-accent-rgb), 0.15) 0%,
    rgba(var(--theme-primary-rgb), 0.10) 30%,
    transparent 65%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
  z-index: 0;
}
.post-card:hover .card-radial-glow { opacity: 1; }
.post-card > :not(.card-radial-glow) {
  position: relative;
  z-index: 1;
}
.cover {
  aspect-ratio: 16/9;
  background: var(--c-line-soft);
  overflow: hidden;
}
.cover img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}
.post-card:hover .cover img { transform: scale(1.06); }

.cover-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-ink-500));
  position: relative;
  overflow: hidden;
}
.cover-placeholder::after {
  content: '';
  position: absolute;
  width: 120%; height: 120%;
  background: radial-gradient(circle, rgba(var(--theme-paper-rgb), 0.15) 0%, transparent 60%);
  animation: shimmer 4s ease-in-out infinite;
}
.cover-char {
  font-family: var(--font-serif);
  font-size: 80px;
  color: rgba(var(--theme-paper-rgb), 0.8);
  font-weight: 500;
  position: relative;
  z-index: 1;
}

@keyframes shimmer {
  0%, 100% { transform: translate(-10%, -10%); }
  50% { transform: translate(10%, 10%); }
}

.meta { padding: 22px 24px 20px; flex: 1; display: flex; flex-direction: column; }
.tags { margin-bottom: 8px; min-height: 22px; }
.title {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 10px;
  color: var(--c-ink);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.excerpt {
  font-size: 14px;
  color: var(--c-ink-soft);
  line-height: 1.7;
  margin: 0 0 16px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.footer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--c-legacy-muted);
  margin-top: auto;
}
.dot { color: var(--c-legacy-line); }

/* mobile */
@media (max-width: 480px) {
  .cover-char { font-size: 48px; }
  .meta { padding: 16px 16px 14px; }
  .title { font-size: 17px; }
  .excerpt { font-size: 13px; }
}
</style>