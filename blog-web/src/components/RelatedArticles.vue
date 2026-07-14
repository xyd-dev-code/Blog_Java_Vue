<template>
  <section class="related-section" v-if="list.length">
    <h3 class="rs-title">相关推荐</h3>
    <div class="rs-grid">
      <div v-for="a in list" :key="a.id" class="rs-card" @click="$router.push(`/articles/${a.slug}`)">
        <h4>{{ a.title }}</h4>
        <p>{{ excerpt(a.summary || a.content, 60) }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { excerpt } from '@/utils/format'
defineProps({ list: { type: Array, default: () => [] } })
</script>

<style scoped lang="scss">
.related-section { margin: 30px 0; }
.rs-title {
  font-family: var(--font-serif);
  font-size: 18px;
  margin: 0 0 16px;
  padding-left: 12px;
  border-left: 3px solid var(--c-autumn-500);
}
.rs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.rs-card {
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.rs-card:hover { border-color: var(--c-botany-300); transform: translateY(-2px); }
.rs-card h4 {
  font-family: var(--font-serif);
  font-size: 14px;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rs-card p {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>