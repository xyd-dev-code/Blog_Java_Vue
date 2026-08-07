<template>
  <article
    class="proj-card ef-rise ef-card-glow"
    :style="cardVar"
    v-tilt
    @click="$emit('detail', project)"
    @mousemove="onGlowMove"
  >
    <!-- 鼠标跟随径向光晕（真实 DOM，避免伪元素冲突） -->
    <div class="card-radial-glow" aria-hidden="true"></div>

    <!-- 大插画区 -->
    <div class="card-illus">
      <img
        v-if="project.coverUrl"
        :src="project.coverUrl"
        :alt="project.name"
        loading="lazy"
        class="illus-img"
      />
      <SunnyDecor v-else :variant="illusVariant" :size="280" class="illus-decor" />
    </div>

    <!-- 标题区 -->
    <div class="card-head">
      <h3 class="card-title">
        <span class="title-dot"></span>
        {{ project.name }}
      </h3>
      <p class="card-desc" v-if="project.description">{{ project.description }}</p>
    </div>

    <!-- 标签 -->
    <div class="card-tags" v-if="project.stack && project.stack.length">
      <span class="tech-tag" v-for="t in project.stack" :key="t">{{ t }}</span>
    </div>

    <!-- 三按钮 -->
    <div class="card-actions" @click.stop>
      <a
        v-if="isLink(project.githubUrl)"
        :href="project.githubUrl"
        target="_blank"
        rel="noopener"
        class="btn btn-gh"
      >
        <svg class="gh-ic" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true">
          <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
        </svg>
        <span>GitHub</span>
      </a>
      <a
        v-if="isLink(project.demoUrl)"
        :href="project.demoUrl"
        target="_blank"
        rel="noopener"
        class="btn btn-demo"
      >
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
        <span>Demo</span>
      </a>
      <button class="btn btn-more" @click.stop="$emit('detail', project)">
        <span>查看详情</span>
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import SunnyDecor from '@/components/SunnyDecor.vue'

const props = defineProps({
  project: { type: Object, required: true }
})
defineEmits(['detail'])

const isLink = (u) => !!u && u.trim() !== '' && u.trim() !== '#'
const cardVar = computed(() => ({ '--card-color': (props.project.color || '#38bdf8') }))

// 占位插画变体：6 种中性灰按 id 轮询
const illusVariant = computed(() => {
  const pool = ['card-notebook', 'card-dashboard', 'card-code', 'card-toolbox', 'card-flask', 'card-document']
  const idNum = Number(props.project.id) || 0
  return pool[idNum % pool.length]
})

const onGlowMove = (e) => {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${((e.clientX - rect.left) / rect.width) * 100}%`)
  el.style.setProperty('--my', `${((e.clientY - rect.top) / rect.height) * 100}%`)
}
</script>

<style scoped lang="scss">
.proj-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(56, 189, 248, 0.18);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.06);
  cursor: pointer;
  transition:
    transform 0.35s cubic-bezier(0.25, 0.8, 0.25, 1),
    box-shadow 0.35s ease,
    border-color 0.35s ease;
}
.proj-card:hover {
  transform: translateY(-6px);
  border-color: rgba(56, 189, 248, 0.5);
  box-shadow: 0 14px 36px rgba(14, 165, 233, 0.18), 0 20px 50px rgba(251, 191, 36, 0.1);
}
.proj-card.ef-card-glow::before { inset: 0; }

.card-radial-glow {
  position: absolute;
  inset: 0;
  top: var(--my, 50%);
  left: var(--mx, 50%);
  width: 340px;
  height: 340px;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(251, 191, 36, 0.13) 0%,
    rgba(56, 189, 248, 0.09) 28%,
    transparent 60%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
  z-index: 0;
}
.proj-card:hover .card-radial-glow { opacity: 1; }
.proj-card > :not(.card-radial-glow) {
  position: relative;
  z-index: 1;
}

.card-illus {
  position: relative;
  aspect-ratio: 2 / 1;
  overflow: hidden;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  display: grid;
  place-items: center;
}
.illus-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.proj-card:hover .illus-img { transform: scale(1.06); }
.illus-decor {
  width: 100%;
  height: 100%;
  max-width: 280px;
}

.card-head { padding: 10px 14px 2px; }
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 2px;
  color: #0f172a;
}
.title-dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: #fbbf24;
  flex-shrink: 0;
  box-shadow: 0 0 5px rgba(251, 191, 36, 0.5);
}
.card-desc {
  font-family: var(--font-sans);
  font-size: 12px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding: 2px 14px 6px;
}
.tech-tag {
  font-family: var(--font-sans);
  display: inline-block;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px 10px;
  border-top: 1px solid #f1f5f9;
  margin-top: auto;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 500;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
  text-decoration: none;
  white-space: nowrap;
}
.btn-gh {
  background: #374151;
  color: #fff;
  padding: 7px 14px;
}
.btn-gh:hover {
  background: #1f2937;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.3);
}
.btn-demo {
  background: linear-gradient(135deg, #7dd3fc, #38bdf8);
  color: #fff;
  padding: 7px 14px;
}
.btn-demo:hover {
  background: linear-gradient(135deg, #38bdf8, #0ea5e9);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(56, 189, 248, 0.4);
}
.btn-more {
  margin-left: auto;
  background: #ffffff;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 6px 13px;
}
.btn-more:hover {
  background: #f1f5f9;
  color: #0c4a6e;
  border-color: #94a3b8;
  transform: translateY(-1px);
}
</style>
