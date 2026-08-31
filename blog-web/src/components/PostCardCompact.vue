<template>
  <router-link :to="`/articles/${article.slug}`" class="pc-compact reveal">
    <div class="pc-thumb" :class="{ 'is-placeholder': !article.coverImage }">
      <img v-if="article.coverImage" :src="article.coverImage" :alt="article.title" loading="lazy" />
      <span v-else class="pc-char">{{ firstChar }}</span>
    </div>
    <div class="pc-body">
      <h3 class="pc-title">{{ article.title }}</h3>
      <p class="pc-excerpt" v-if="excerptText">{{ excerptText }}</p>
      <div class="pc-meta">
        <span v-if="date" class="pc-date">{{ date }}</span>
        <span v-if="tagName" class="pc-tag">{{ tagName }}</span>
        <span class="pc-views">👁 {{ article.viewCount || 0 }}</span>
      </div>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { fmtDate, excerpt } from '@/utils/format'

const props = defineProps({
  article: { type: Object, required: true }
})

const firstChar = computed(() => (props.article.title || '山')[0])
const date = computed(() => fmtDate(props.article.createTime))
const excerptText = computed(() => {
  const s = props.article.summary || props.article.content || ''
  return s ? excerpt(s, 70) : ''
})
const tagName = computed(() => props.article.tags?.[0]?.name || props.article.categoryName || '')
</script>

<style scoped lang="scss">
.pc-compact {
  display: flex;
  align-items: stretch;
  gap: 14px;
  padding: 12px;
  background: rgba(var(--theme-paper-rgb), 0.66);
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.35);
  border-radius: 14px;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  text-decoration: none;
  color: inherit;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.pc-compact:hover {
  border-color: var(--c-botany-500);
  box-shadow: 0 6px 18px rgba(var(--theme-primary-rgb), 0.14);
  transform: translateY(-2px);
}
.pc-thumb {
  flex: 0 0 120px;
  width: 120px;
  height: 84px;
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(135deg, var(--c-botany-200) 0%, var(--c-cyan-300) 60%, var(--c-autumn-200) 100%);
  display: grid;
  place-items: center;
}
.pc-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pc-char {
  font-family: var(--font-serif);
  font-size: 34px;
  color: rgba(var(--theme-paper-rgb), 0.85);
  font-weight: 500;
}
.pc-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 2px 0;
}
.pc-title {
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--c-ink, var(--c-ink-800));
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
}
.pc-compact:hover .pc-title { color: var(--c-botany-900); }
.pc-excerpt {
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--c-ink-soft, var(--c-ink-400));
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pc-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  font-size: 11.5px;
  color: var(--c-ink-soft, var(--c-ink-300));
}
.pc-tag {
  padding: 1px 8px;
  background: var(--c-botany-50);
  color: var(--c-botany-900);
  border-radius: 999px;
  border: 1px solid var(--c-botany-100);
}
@media (max-width: 480px) {
  .pc-thumb { flex-basis: 92px; width: 92px; height: 70px; }
  .pc-title { font-size: 15px; }
  .pc-excerpt { font-size: 12px; }
}
</style>
