<template>
  <!-- 晴天主题装饰插画：复用的 SVG 组件，太阳/云朵/电脑/光柱 组合 -->
  <svg
    :class="['sunny-decor', `sunny-decor--${variant}`]"
    :viewBox="viewBox"
    :width="size"
    :height="height"
    preserveAspectRatio="xMidYMid meet"
    aria-hidden="true"
  >
    <!-- 渐变定义 -->
    <defs>
      <linearGradient :id="`sky-${uid}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#dbeafe" />
        <stop offset="100%" stop-color="#f0f9ff" />
      </linearGradient>
      <radialGradient :id="`sun-${uid}`" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0%" stop-color="#fefce8" />
        <stop offset="60%" stop-color="#fde68a" />
        <stop offset="100%" stop-color="#fcd34d" />
      </radialGradient>
      <linearGradient :id="`cloud-${uid}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#ffffff" />
        <stop offset="100%" stop-color="#e0f2fe" />
      </linearGradient>
      <linearGradient :id="`laptop-${uid}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#f8fafc" />
        <stop offset="100%" stop-color="#cbd5e1" />
      </linearGradient>
      <linearGradient :id="`ray-${uid}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#dbeafe" stop-opacity="0.85" />
        <stop offset="100%" stop-color="#7dd3fc" stop-opacity="0.1" />
      </linearGradient>
    </defs>

    <!-- ===================== Hero 用：电脑 + 太阳 + 大云朵 ===================== -->
    <g v-if="variant === 'hero'">
      <!-- 背景光晕 -->
      <ellipse cx="380" cy="200" rx="280" ry="160" :fill="`url(#sky-${uid})`" opacity="0.55" />

      <!-- 远云 -->
      <g :fill="`url(#cloud-${uid})`">
        <ellipse cx="100" cy="70" rx="55" ry="16" opacity="0.85" />
        <ellipse cx="85" cy="60" rx="22" ry="20" opacity="0.85" />
        <ellipse cx="120" cy="55" rx="18" ry="16" opacity="0.85" />
      </g>

      <!-- 太阳（右上角，带光晕） -->
      <g>
        <circle cx="540" cy="90" r="68" :fill="`url(#sun-${uid})`" opacity="0.18" />
        <circle cx="540" cy="90" r="46" :fill="`url(#sun-${uid})`" />
        <!-- 光芒 -->
        <g stroke="#fcd34d" stroke-width="4" stroke-linecap="round" opacity="0.55">
          <line x1="540" y1="22" x2="540" y2="6" />
          <line x1="540" y1="158" x2="540" y2="174" />
          <line x1="472" y1="90" x2="456" y2="90" />
          <line x1="608" y1="90" x2="624" y2="90" />
          <line x1="490" y1="40" x2="478" y2="28" />
          <line x1="590" y1="140" x2="602" y2="152" />
          <line x1="490" y1="140" x2="478" y2="152" />
          <line x1="590" y1="40" x2="602" y2="28" />
        </g>
      </g>

      <!-- 笔记本（中心偏下） -->
      <g transform="translate(220 140)">
        <!-- 屏幕 -->
        <rect x="20" y="10" width="320" height="190" rx="14" :fill="`url(#laptop-${uid})`" stroke="#94a3b8" stroke-width="2" />
        <!-- 屏幕内容（窗口 UI） -->
        <rect x="32" y="22" width="296" height="166" rx="6" fill="#ffffff" />
        <!-- 顶部窗口栏 -->
        <rect x="32" y="22" width="296" height="22" rx="6" fill="#f1f5f9" />
        <circle cx="44" cy="33" r="4" fill="#f87171" />
        <circle cx="58" cy="33" r="4" fill="#fcd34d" />
        <circle cx="72" cy="33" r="4" fill="#86efac" />
        <!-- 侧边栏 -->
        <rect x="32" y="44" width="64" height="144" fill="#f8fafc" />
        <rect x="42" y="58" width="44" height="6" rx="3" fill="#cbd5e1" />
        <rect x="42" y="72" width="34" height="6" rx="3" fill="#cbd5e1" />
        <rect x="42" y="86" width="40" height="6" rx="3" fill="#cbd5e1" />
        <rect x="42" y="100" width="30" height="6" rx="3" fill="#cbd5e1" />
        <!-- 主区域内容（卡片） -->
        <rect x="106" y="58" width="100" height="50" rx="6" fill="#e0f2fe" />
        <rect x="214" y="58" width="100" height="50" rx="6" fill="#bae6fd" />
        <rect x="106" y="116" width="208" height="14" rx="3" fill="#e2e8f0" />
        <rect x="106" y="138" width="170" height="14" rx="3" fill="#e2e8f0" />
        <rect x="106" y="160" width="140" height="14" rx="3" fill="#e2e8f0" />
        <!-- 笔记本底座 -->
        <path d="M 0 200 L 360 200 L 348 218 L 12 218 Z" fill="#94a3b8" />
        <rect x="160" y="200" width="40" height="4" rx="2" fill="#64748b" />
      </g>

      <!-- 底部大云：贴电脑脚,呼应设计图"云在电脑两侧" -->
      <g :fill="`url(#cloud-${uid})`" opacity="0.95">
        <ellipse cx="200" cy="350" rx="100" ry="26" />
        <ellipse cx="175" cy="338" rx="36" ry="32" />
        <ellipse cx="225" cy="332" rx="28" ry="24" />
        <!-- 右下小云 -->
        <ellipse cx="560" cy="348" rx="60" ry="18" />
        <ellipse cx="545" cy="340" rx="22" ry="18" />
        <ellipse cx="580" cy="334" rx="18" ry="15" />
      </g>

      <!-- 装饰星点 -->
      <circle cx="60" cy="40" r="3" fill="#bae6fd" opacity="0.5" />
      <circle cx="600" cy="220" r="2.5" fill="#38bdf8" opacity="0.6" />
      <circle cx="80" cy="240" r="2" fill="#bae6fd" opacity="0.45" />
    </g>

    <!-- ===================== 「精选项目」标题旁：去掉太阳避免与 Hero 重复，只留光柱+云朵 ===================== -->
    <g v-else-if="variant === 'ray'">
      <!-- 光柱（淡蓝色柱） -->
      <g :fill="`url(#ray-${uid})`">
        <polygon points="50,90 90,90 130,250 10,250" />
      </g>
      <!-- 多云组合 -->
      <g :fill="`url(#cloud-${uid})`">
        <ellipse cx="70" cy="95" rx="42" ry="14" />
        <ellipse cx="56" cy="88" rx="18" ry="14" />
        <ellipse cx="86" cy="84" rx="14" ry="12" />
        <ellipse cx="36" cy="105" rx="20" ry="10" />
        <ellipse cx="106" cy="102" rx="16" ry="9" />
      </g>
    </g>

    <!-- ================ 卡片占位（中性灰，不抢真实图视觉，6 种互不重复）================ -->
    <g v-else-if="variant === 'card-notebook'">
      <rect x="0" y="0" width="240" height="160" fill="#f8fafc" />
      <rect x="40" y="36" width="160" height="100" rx="8" fill="#fff" stroke="#cbd5e1" stroke-width="1.5" />
      <rect x="50" y="46" width="140" height="80" rx="4" fill="#f1f5f9" />
      <rect x="58" y="56" width="60" height="6" rx="3" fill="#94a3b8" />
      <rect x="58" y="70" width="124" height="4" rx="2" fill="#cbd5e1" />
      <rect x="58" y="80" width="110" height="4" rx="2" fill="#cbd5e1" />
      <rect x="58" y="90" width="118" height="4" rx="2" fill="#cbd5e1" />
      <rect x="58" y="100" width="80" height="4" rx="2" fill="#cbd5e1" />
      <circle cx="55" cy="46" r="3" fill="#94a3b8" opacity="0.5" />
    </g>

    <g v-else-if="variant === 'card-toolbox'">
      <rect x="0" y="0" width="240" height="160" fill="#f8fafc" />
      <rect x="50" y="60" width="140" height="70" rx="8" fill="#e2e8f0" />
      <rect x="50" y="50" width="140" height="20" rx="6" fill="#cbd5e1" />
      <rect x="115" y="40" width="10" height="14" rx="2" fill="#64748b" />
      <g transform="translate(78 76) rotate(-15)">
        <rect x="0" y="10" width="60" height="6" rx="3" fill="#fff" />
        <circle cx="0" cy="13" r="8" fill="#fff" />
        <circle cx="0" cy="13" r="3" fill="#cbd5e1" />
      </g>
    </g>

    <g v-else-if="variant === 'card-flask'">
      <rect x="0" y="0" width="240" height="160" fill="#f8fafc" />
      <path d="M 95 36 L 95 70 L 65 130 Q 65 138 75 138 L 165 138 Q 175 138 175 130 L 145 70 L 145 36 Z" fill="#e2e8f0" stroke="#94a3b8" stroke-width="2" />
      <path d="M 72 110 L 168 110 L 175 130 Q 175 138 165 138 L 75 138 Q 65 138 65 130 Z" fill="#cbd5e1" />
      <rect x="92" y="30" width="56" height="10" rx="2" fill="#cbd5e1" />
      <circle cx="100" cy="118" r="3" fill="#fff" opacity="0.8" />
      <circle cx="130" cy="122" r="2" fill="#fff" opacity="0.8" />
      <circle cx="145" cy="116" r="2.5" fill="#fff" opacity="0.8" />
    </g>

    <!-- 仪表板 -->
    <g v-else-if="variant === 'card-dashboard'">
      <rect x="0" y="0" width="240" height="160" fill="#f8fafc" />
      <!-- 顶栏 -->
      <rect x="24" y="32" width="192" height="22" rx="4" fill="#f1f5f9" stroke="#e2e8f0" stroke-width="1" />
      <rect x="36" y="39" width="40" height="8" rx="4" fill="#94a3b8" />
      <!-- 统计卡片 ×3 -->
      <rect x="24" y="64" width="56" height="34" rx="5" fill="#fff" stroke="#e2e8f0" stroke-width="1" />
      <rect x="40" y="72" width="24" height="8" rx="4" fill="#94a3b8" />
      <rect x="40" y="84" width="32" height="4" rx="2" fill="#cbd5e1" />
      <rect x="92" y="64" width="56" height="34" rx="5" fill="#fff" stroke="#e2e8f0" stroke-width="1" />
      <rect x="108" y="72" width="24" height="8" rx="4" fill="#cbd5e1" />
      <rect x="108" y="84" width="20" height="4" rx="2" fill="#cbd5e1" />
      <rect x="160" y="64" width="56" height="34" rx="5" fill="#fff" stroke="#e2e8f0" stroke-width="1" />
      <rect x="176" y="72" width="24" height="8" rx="4" fill="#cbd5e1" />
      <rect x="176" y="84" width="26" height="4" rx="2" fill="#cbd5e1" />
      <!-- 折线 -->
      <path d="M 40 130 L 70 118 L 100 124 L 130 108 L 160 114 L 190 100" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" opacity="0.6" />
      <circle cx="130" cy="108" r="3" fill="#94a3b8" />
    </g>

    <!-- 代码编辑器 -->
    <g v-else-if="variant === 'card-code'">
      <rect x="0" y="0" width="240" height="160" fill="#f8fafc" />
      <rect x="20" y="24" width="200" height="120" rx="8" fill="#fff" stroke="#e2e8f0" stroke-width="1.5" />
      <rect x="26" y="30" width="188" height="16" rx="3" fill="#f1f5f9" />
      <circle cx="34" cy="38" r="4" fill="#94a3b8" />
      <circle cx="46" cy="38" r="4" fill="#cbd5e1" />
      <circle cx="58" cy="38" r="4" fill="#cbd5e1" />
      <!-- 行号 + 代码行 -->
      <rect x="26" y="54" width="14" height="38" rx="2" fill="#f8fafc" />
      <rect x="34" y="58" width="6" height="6" rx="3" fill="#cbd5e1" />
      <rect x="34" y="72" width="6" height="6" rx="3" fill="#cbd5e1" />
      <rect x="34" y="86" width="6" height="6" rx="3" fill="#cbd5e1" />
      <!-- 彩色代码条 -->
      <rect x="46" y="58" width="40" height="6" rx="3" fill="#94a3b8" />
      <rect x="60" y="72" width="120" height="6" rx="3" fill="#cbd5e1" />
      <rect x="60" y="86" width="100" height="6" rx="3" fill="#cbd5e1" />
      <rect x="46" y="100" width="90" height="6" rx="3" fill="#cbd5e1" />
      <rect x="46" y="114" width="140" height="6" rx="3" fill="#cbd5e1" />
      <rect x="46" y="128" width="70" height="6" rx="3" fill="#cbd5e1" />
    </g>

    <!-- 文档 -->
    <g v-else-if="variant === 'card-document'">
      <rect x="0" y="0" width="240" height="160" fill="#f8fafc" />
      <rect x="28" y="28" width="184" height="116" rx="6" fill="#fff" stroke="#e2e8f0" stroke-width="1.5" />
      <!-- 大标题 -->
      <rect x="44" y="44" width="100" height="8" rx="4" fill="#64748b" />
      <rect x="44" y="58" width="152" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="68" width="138" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="78" width="146" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="88" width="120" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="98" width="132" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="108" width="100" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="118" width="80" height="4" rx="2" fill="#cbd5e1" />
      <rect x="44" y="128" width="144" height="4" rx="2" fill="#cbd5e1" />
      <!-- 小图标 -->
      <circle cx="44" cy="44" r="5" fill="#94a3b8" opacity="0.4" />
    </g>
  </svg>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'hero' }, // hero / ray / card-notebook / card-toolbox / card-flask / card-dashboard / card-code / card-document
  size: { type: [String, Number], default: 560 },
})

// 唯一 id 防止同一页面多实例渐变冲突
const uid = ref(`s${Math.random().toString(36).slice(2, 8)}`)

const viewBox = computed(() => {
  switch (props.variant) {
    case 'hero': return '0 0 640 380'
    case 'ray': return '0 0 140 260'
    case 'card-notebook':
    case 'card-toolbox':
    case 'card-flask':
    case 'card-dashboard':
    case 'card-code':
    case 'card-document':
      return '0 0 240 160'
    default: return '0 0 640 380'
  }
})

const height = computed(() => {
  switch (props.variant) {
    case 'hero': return Math.round(Number(props.size) * 380 / 640)
    case 'ray': return Math.round(Number(props.size) * 260 / 140)
    case 'card-notebook':
    case 'card-toolbox':
    case 'card-flask':
    case 'card-dashboard':
    case 'card-code':
    case 'card-document':
      return Math.round(Number(props.size) * 160 / 240)
    default: return Number(props.size)
  }
})
</script>

<style scoped lang="scss">
.sunny-decor {
  display: block;
  pointer-events: none;
  user-select: none;
}
</style>