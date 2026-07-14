<template>
  <article
    class="sticky-note"
    :class="[`tone-${tone}`, { 'is-hover': hovering }]"
    :style="noteStyle"
    @mouseenter="hovering = true"
    @mouseleave="hovering = false"
  >
    <div v-if="$slots.pin" class="note-pin">
      <slot name="pin" />
    </div>
    <slot />
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  // 颜色 tone：sky / cyan / sun / paper
  tone: {
    type: String,
    default: 'sky',
    validator: (v) => ['sky', 'cyan', 'sun', 'paper'].includes(v)
  },
  // 随机旋转角度（-3 ~ +3）
  rotate: { type: Number, default: null },
  // 是否显示顶部红条（"便签纸"效果）
  pinned: { type: Boolean, default: false }
})

const hovering = ref(false)

const noteStyle = computed(() => {
  if (props.rotate == null) return {}
  return { transform: `rotate(${props.rotate}deg)` }
})
</script>

<style scoped lang="scss">
.sticky-note {
  position: relative;
  padding: 20px 22px;
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06),
              0 12px 32px rgba(15, 23, 42, 0.04);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1),
              box-shadow 0.35s ease,
              z-index 0s;
  border: 1px solid rgba(255, 255, 255, 0.6);
  z-index: 1;
}
.sticky-note.is-hover {
  transform: rotate(0deg) translateY(-6px) scale(1.02) !important;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.10),
              0 20px 48px rgba(15, 23, 42, 0.08);
  z-index: 10;
}

// 颜色调
.tone-sky {
  background: linear-gradient(180deg, #f0f9ff 0%, #ffffff 100%);
  border-color: rgba(125, 211, 252, 0.4);
}
.tone-cyan {
  background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
  border-color: rgba(34, 211, 238, 0.35);
}
.tone-sun {
  background: linear-gradient(180deg, #fffbeb 0%, #ffffff 100%);
  border-color: rgba(251, 191, 36, 0.35);
}
.tone-paper {
  background: linear-gradient(180deg, #fafaf9 0%, #ffffff 100%);
  border-color: rgba(226, 232, 240, 0.6);
}

// 顶部红条（便签纸效果）
.note-pin {
  position: absolute;
  top: 0; left: 50%;
  transform: translate(-50%, -50%);
  width: 38px;
  height: 12px;
  border-radius: 3px;
  background: linear-gradient(180deg, rgba(220, 38, 38, 0.55), rgba(220, 38, 38, 0.4));
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
</style>