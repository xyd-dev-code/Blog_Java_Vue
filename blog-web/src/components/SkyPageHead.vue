<template>
  <div class="sky-page-head">
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
    <svg class="wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
      <path fill="currentColor" style="color:var(--c-bg); opacity:.95"
        d="M0,40 C180,100 360,0 540,50 C720,100 900,20 1080,60 C1260,100 1380,30 1440,50 L1440,120 L0,120 Z" />
      <path fill="currentColor" style="color:#fff; opacity:.3"
        d="M0,60 C200,10 400,70 600,40 C800,10 1000,60 1200,35 C1320,20 1400,55 1440,45 L1440,120 L0,120 Z" />
    </svg>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>
