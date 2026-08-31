<template>
  <div class="hero-guestbook">
    <GodRays :origin-y="70" />
    <div class="container hg-inner">
      <div class="hg-text">
        <h1 class="hg-title"><WuxiaHeadingLettering :text="title" /></h1>
        <p class="hg-sub" v-if="subtitle">{{ subtitle }}</p>
      </div>
      <button class="hg-publish" @click="$emit('publish')">
        <el-icon><EditPen /></el-icon>
        <span>{{ wx('发表留言') }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import WuxiaHeadingLettering from '@/components/WuxiaHeadingLettering.vue'
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { EditPen } from '@element-plus/icons-vue'
import GodRays from '@/components/effects/GodRays.vue'
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
defineEmits(['publish'])
</script>

<style scoped lang="scss">
.hero-guestbook {
  position: relative;
  /* 不裁切——装饰元素自然向外 bleed */
  /* 不铺不透明底色——让全局天空透上来 */
  background:
    radial-gradient(ellipse at 18% 50%, rgba(var(--theme-primary-soft-rgb), 0.12) 0%, transparent 55%),
    radial-gradient(ellipse at 82% 50%, rgba(var(--theme-primary-light-rgb), 0.08) 0%, transparent 55%);
  padding: 38px 0 60px;
  margin-bottom: 0;
}
.hg-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  position: relative;
  z-index: 2;
}
.hg-text {
  flex: none;
  text-align: center;
}
.hg-title {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 6px;
  background: linear-gradient(135deg, var(--c-botany-800) 0%, var(--c-botany-700) 50%, var(--c-botany-500) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.02em;
}
.hg-sub {
  font-size: 14px;
  color: var(--c-ink-500);
  margin: 0;
  font-weight: 300;
  letter-spacing: 0.05em;
}
.hg-publish {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 11px 26px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--c-botany-700) 0%, var(--c-botany-800) 100%);
  color: var(--theme-on-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(var(--theme-primary-strong-rgb), 0.32);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  flex-shrink: 0;
  letter-spacing: 0.05em;
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
}
.hg-publish:hover {
  transform: translateY(calc(-50% - 2px));
  box-shadow: 0 10px 24px rgba(var(--theme-primary-strong-rgb), 0.4);
}
.hg-publish:active { transform: translateY(-50%); }
.hg-publish .el-icon { font-size: 14px; }

/* 波浪已移除——不再画分隔线，hero 与内容区自然共融 */

@media (max-width: 768px) {
  .hero-guestbook { padding: 28px 0 48px; }
  .hg-inner { flex-direction: column; align-items: center; gap: 18px; }
  .hg-title { font-size: 28px; }
  .hg-publish {
    padding: 9px 20px;
    font-size: 13px;
    position: static;
    transform: none;
  }
  .hg-publish:hover { transform: translateY(-2px); }
  .hg-publish:active { transform: translateY(0); }
}
</style>
