<template>
  <article
    class="tool-card ef-rise ef-card-glow"
    :class="cardClass"
    v-tilt
    @mousemove="onGlowMove"
  >
    <!-- 鼠标跟随径向光晕（真实 DOM，避免伪元素冲突） -->
    <div class="card-radial-glow" aria-hidden="true"></div>

    <!-- 左上：分类标签 -->
    <span class="cat-tag">{{ catLabel(tool.category) }}</span>

    <!-- 右上：圆形 + 按钮（收藏） -->
    <button
      class="add-btn"
      :class="{ active: isFav }"
      :title="isFav ? '取消收藏' : '收藏'"
      @click.stop="emit('fav', tool)"
    >
      <el-icon>
        <component :is="isFav ? 'StarFilled' : 'Plus'" />
      </el-icon>
    </button>

    <!-- 标题 + 描述 -->
    <h3 class="card-title">{{ tool.name }}</h3>
    <p class="card-desc">{{ tool.description }}</p>

    <!-- 右上图标：图片 URL 优先渲染，否则蓝线稿文档图标兜底 -->
    <img v-if="isIconUrl(tool.icon)" :src="tool.icon" class="card-icon card-icon-img" alt="" aria-hidden="true" loading="lazy" decoding="async" />
    <svg v-else class="card-icon" width="60" height="60" viewBox="0 0 60 60"
      fill="none" stroke="#38bdf8" stroke-width="2" stroke-linejoin="round"
      aria-hidden="true">
      <rect x="14" y="8" width="32" height="44" rx="3" />
      <line x1="20" y1="18" x2="40" y2="18" />
      <line x1="20" y1="26" x2="40" y2="26" />
      <line x1="20" y1="34" x2="34" y2="34" />
    </svg>

    <!-- 底部按钮区 -->
    <div class="card-foot">
      <span class="cat-pill" :class="{ muted: tool.status === 2 }">
        {{ catLabel(tool.category) }}
      </span>
      <template v-if="tool.status === 1">
        <span class="use-btn ef-neon" @click.stop="emit('use', tool)">开始使用</span>
      </template>
      <template v-else-if="tool.status === 2">
        <span class="use-btn muted">维护中</span>
      </template>
      <template v-else-if="tool.status === 3">
        <span class="use-btn upcoming">即将上线</span>
      </template>
      <template v-else>
        <span class="use-btn muted">已下线</span>
      </template>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tool: { type: Object, required: true },
  categories: { type: Array, default: () => [{ value: 'all', label: '全部' }] },
  isFav: { type: Boolean, default: false }
})
const emit = defineEmits(['fav', 'use'])

const catLabel = (v) => (props.categories.find((c) => c.value === v) || {}).label || '其他'

const cardClass = computed(() => ({
  'is-maintain': props.tool.status === 2,
  'is-upcoming': props.tool.status === 3,
  'is-offline': props.tool.status === 0
}))

// 图标：URL（http/https/ /data:）才渲染成 <img>，否则回退蓝线稿文档图标
const isIconUrl = (v) => /^(https?:\/\/|\/|data:)/.test(v || '')

// 卡片鼠标跟随高光：把光标位置写成 --mx/--my
const onGlowMove = (e) => {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${((e.clientX - rect.left) / rect.width) * 100}%`)
  el.style.setProperty('--my', `${((e.clientY - rect.top) / rect.height) * 100}%`)
}
</script>

<style scoped lang="scss">
.tool-card {
  position: relative;
  background: #fff;
  border: 1px solid rgba(186, 230, 253, 0.6);
  border-radius: 14px;
  padding: 22px 18px 18px;
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto auto;
  grid-template-areas:
    "tag add"
    "title icon"
    "desc  desc"
    "foot  foot";
  gap: 10px 14px;
  box-shadow: 0 4px 16px rgba(56, 189, 248, 0.08);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  cursor: pointer;
  overflow: hidden;

  &:hover {
    box-shadow: 0 10px 28px rgba(56, 189, 248, 0.18);
    border-color: rgba(56, 189, 248, 0.4);
  }

  &.is-maintain {
    background: #f8fafc;
    border-style: dashed;
    border-color: #cbd5e1;
    opacity: 0.7;
    .card-title { color: #64748b; }
  }
  &.is-upcoming {
    background: #fff;
    border-style: dashed;
    border-color: rgba(56, 189, 248, 0.4);
  }
  &.is-offline { opacity: 0.5; }
}

.card-radial-glow {
  position: absolute;
  inset: 0;
  top: var(--my, 50%);
  left: var(--mx, 50%);
  width: 320px;
  height: 320px;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(251, 191, 36, 0.14) 0%,
    rgba(56, 189, 248, 0.10) 30%,
    transparent 65%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
  z-index: 0;
}
.tool-card:hover .card-radial-glow { opacity: 1; }
.tool-card > :not(.card-radial-glow) {
  position: relative;
  z-index: 1;
}

.cat-tag {
  grid-area: tag;
  display: inline-block;
  padding: 3px 10px;
  font-size: 12px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 6px;
  align-self: flex-start;
}

.add-btn {
  grid-area: add;
  width: 22px; height: 22px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #94a3b8;
  transition: all 0.2s ease;
  .el-icon { font-size: 12px; }
  &:hover {
    color: #0ea5e9;
    border-color: #0ea5e9;
    transform: scale(1.05);
  }
  &.active {
    color: #fbbf24;
    border-color: #fbbf24;
    background: rgba(251, 191, 36, 0.08);
  }
}

.card-title {
  grid-area: title;
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  color: #0c4a6e;
  margin: 0;
  line-height: 1.3;
  padding-right: 50px;
  align-self: center;
}
.card-desc {
  grid-area: desc;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
.card-icon {
  grid-area: icon;
  width: 56px; height: 56px;
  align-self: center;
  justify-self: end;
  opacity: 0.85;
}
.card-icon-img {
  object-fit: contain;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.55);
  padding: 4px;
}

.card-foot {
  grid-area: foot;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed rgba(186, 230, 253, 0.6);
}
.cat-pill {
  display: inline-block;
  padding: 5px 14px;
  font-size: 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #38bdf8, #0ea5e9);
  color: #fff;
  &.muted { background: #f1f5f9; color: #94a3b8; }
}
.use-btn {
  display: inline-block;
  padding: 6px 18px;
  font-size: 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid rgba(56, 189, 248, 0.5);
  color: #0ea5e9;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    background: linear-gradient(135deg, #38bdf8, #0ea5e9);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(56, 189, 248, 0.3);
  }
  &.muted {
    border-color: #cbd5e1;
    color: #94a3b8;
    cursor: not-allowed;
    &:hover { background: #fff; color: #94a3b8; box-shadow: none; }
  }
  &.upcoming {
    background: rgba(56, 189, 248, 0.1);
    border-color: rgba(56, 189, 248, 0.3);
    color: #0284c7;
    cursor: default;
    &:hover {
      background: rgba(56, 189, 248, 0.18);
      color: #0369a1;
      box-shadow: none;
    }
  }
}

/* 移动端：放大工具卡「开始使用」与收藏按钮到 ≥44px 触控目标（P0 触控尺寸修复） */
@media (max-width: 768px) {
  .use-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 44px;
    padding: 10px 22px;
    font-size: 13px;
  }
  .add-btn {
    width: 40px;
    height: 40px;
    .el-icon { font-size: 16px; }
  }
}
</style>
