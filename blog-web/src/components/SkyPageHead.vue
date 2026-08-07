<template>
  <div class="sky-page-head">
    <HeroLight />
    <GodRays :origin-y="60" />
    <!-- 浮动云 -->
    <svg class="cloud cloud-1 cloud-float" viewBox="0 0 200 80" preserveAspectRatio="none">
      <path fill="rgba(255,255,255,.15)" d="M40,60 Q50,30 70,35 Q80,10 110,15 Q130,0 160,25 Q185,20 190,45 Q200,60 180,65 L30,65 Z"/>
    </svg>
    <svg class="cloud cloud-2 cloud-float" viewBox="0 0 160 60" preserveAspectRatio="none">
      <path fill="rgba(255,255,255,.1)" d="M20,45 Q35,20 55,28 Q65,8 90,12 Q105,0 130,18 Q150,15 155,35 Q160,45 140,48 L15,48 Z"/>
    </svg>
    <div class="container">
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup>
import GodRays from '@/components/effects/GodRays.vue'
import HeroLight from '@/components/effects/HeroLight.vue'
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>
