<template>
  <section class="sky-hero" ref="rootRef">
    <div class="ink-hero-scene" aria-hidden="true">
      <img class="ink-paper-layer" src="/images/ink-paper-texture-v2.png" alt="" decoding="async" fetchpriority="high" />
      <WuxiaHeroArt />
    </div>
    <!-- 装饰层：视差光影 + 太阳 + 云朵 -->
    <HeroLight />
    <div class="deco">
      <div class="sun" :style="sunStyle"></div>
      <svg class="cloud cloud-a" viewBox="0 0 64 24" fill="var(--theme-on-primary)" aria-hidden="true">
        <ellipse cx="20" cy="14" rx="14" ry="8" />
        <ellipse cx="38" cy="12" rx="18" ry="10" />
        <ellipse cx="52" cy="14" rx="10" ry="6" />
      </svg>
      <svg class="cloud cloud-b" viewBox="0 0 64 24" fill="var(--theme-on-primary)" aria-hidden="true">
        <ellipse cx="32" cy="12" rx="22" ry="11" />
      </svg>
      <svg class="cloud cloud-c" viewBox="0 0 64 24" fill="var(--theme-on-primary)" aria-hidden="true">
        <ellipse cx="32" cy="12" rx="18" ry="9" />
      </svg>
    </div>

    <div class="container hero-grid">
      <!-- 左：标题与按钮 -->
      <div class="hero-text reveal">
        <div class="hero-eyebrow">
          <span class="dot"></span>
          <span>{{ displayGreeting }} · {{ todayStr }}</span>
        </div>
        <h1 class="hero-title">
          <span class="title-main">
            <WuxiaTitleLettering v-if="showMainLettering" variant="main" />
            <template v-else>{{ titleMain }}</template>
          </span>
          <span class="title-accent">
            <WuxiaTitleLettering v-if="showAccentLettering" variant="accent" />
            <template v-else>{{ titleAccent }}</template>
          </span>
        </h1>
        <p class="hero-desc">{{ subtitle }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="btn-pulse ef-neon" @click="$emit('cta')">
            <el-icon><Reading /></el-icon>&nbsp;{{ ctaText }}
          </el-button>
          <el-button size="large" plain @click="$emit('secondary')">
            {{ secondaryText }}
          </el-button>
        </div>
      </div>

      <!-- 右：天气卡（玻璃态 + 渐变描边） -->
      <GradientBorderCard variant="mix" floating class="weather-card reveal">
        <div class="weather-head">
          <div>
            <div class="weather-eyebrow">
              <span class="we-text">{{ (status === 'ready' || status === 'error') ? (location || '本地') : '今日天气' }}</span>
              <button class="weather-loc-btn" type="button" :title="location ? '切换城市' : '选择城市'" @click="togglePicker">📍</button>
            </div>
            <div class="weather-temp">
                  <template v-if="status === 'ready'">
                    {{ desc }} · {{ temp ?? '--' }}°
                    <span v-if="feelsLike != null && feelsLike !== temp" class="weather-feels">（体感 {{ feelsLike }}°）</span>
                  </template>
                  <template v-else-if="status === 'error'">
                    <span class="weather-fail">{{ desc }}</span>
                    <button class="wd-btn" type="button" @click="reAuthorize">重试</button>
                  </template>
                  <template v-else-if="status === 'denied'">
                    <span class="weather-fail">位置信息暂未授权</span>
                    <button class="wd-btn" type="button" @click="reAuthorize">重新授权</button>
                  </template>
              <template v-else>
                正在获取天气…
              </template>
            </div>
                <div v-if="status === 'ready' && (humidity != null || windSpeed != null || pressure != null)" class="weather-detail">
                  <span v-if="humidity != null">湿度 {{ humidity }}%</span>
                  <span v-if="humidity != null && (windSpeed != null || pressure != null)" class="wd-sep">·</span>
                  <span v-if="windSpeed != null">{{ windDir }}{{ windSpeed }}km/h</span>
                  <span v-if="windSpeed != null && pressure != null" class="wd-sep">·</span>
                  <span v-if="pressure != null">气压 {{ pressure }}hPa</span>
                </div>
            <div class="weather-date">{{ todayStr }} · {{ siteNameLabel }}</div>
          </div>
          <div class="weather-icon">{{ icon }}</div>
        </div>

        <!-- 城市切换面板：访客搜索并切换到自己真实的城市 -->
        <div v-if="showPicker" class="city-picker">
          <div class="cp-input-wrap">
            <input
              v-model="cityKeyword"
              class="cp-input"
              type="text"
              inputmode="search"
              placeholder="搜索城市，如 深圳 / 北京 / 上海"
              @input="onCityInput"
              @keyup.enter="searchCities(cityKeyword)"
            />
            <button v-if="location" class="cp-reset" type="button" @click="resetCity" title="回到默认位置">默认</button>
          </div>
          <div v-if="searching" class="cp-tip">搜索中…</div>
          <ul v-else-if="searchResults.length" class="cp-list">
            <li
              v-for="c in searchResults"
              :key="c.name + c.lat + c.lon"
              class="cp-item"
              @click="pickCity(c)"
            >
              <span class="cp-name">{{ c.name }}</span>
              <span class="cp-admin">{{ c.admin }}</span>
            </li>
          </ul>
          <div v-else-if="cityKeyword.trim()" class="cp-tip">未找到匹配的城市</div>
        </div>
        <div class="weather-divider"></div>
        <div class="weather-grid">
          <div v-for="s in stats" :key="s.label">
            <div class="wg-num"><CountUp :value="s.value" /></div>
            <div class="wg-label">{{ s.label }}</div>
          </div>
        </div>
        <div class="weather-quote">{{ displayQuote }}</div>
      </GradientBorderCard>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Reading } from '@element-plus/icons-vue'
import GradientBorderCard from './GradientBorderCard.vue'
import HeroLight from '@/components/effects/HeroLight.vue'
import CountUp from '@/components/CountUp.vue'
import { useWeather } from '@/composables/useWeather'
import { useSiteStore } from '@/stores/site'
import { useThemeStore } from '@/stores/theme'
import WuxiaHeroArt from '@/components/effects/WuxiaHeroArt.vue'
import WuxiaTitleLettering from './WuxiaTitleLettering.vue'

// 天气卡展示访客所选城市的天气。默认按 IP 定位到城市，访客可手动搜索切换城市。
const {
  temp, desc, icon, location, status,
  humidity, windSpeed, windDir, pressure, feelsLike,
  showPicker, cityKeyword, searchResults, searching,
  togglePicker, onCityInput, searchCities, pickCity, resetCity, reAuthorize
} = useWeather()

const siteStore = useSiteStore()
const themeStore = useThemeStore()
// 天气卡底部"日期·站点名"——不再硬编码博主笔名，跟随后台站点配置走，
// 改名后也能即时同步。
const siteNameLabel = computed(() =>
  (siteStore.info?.siteName && siteStore.info.siteName.trim())
    ? siteStore.info.siteName
    : '拾光小筑'
)

// 按当前时段返回招呼语：深夜（含凌晨 0-5 点）归为「晚上好」，其余分段
function timeGreeting() {
  const h = new Date().getHours()
  if (h >= 18 || h < 6) return '晚上好' // 18:00–次日 05:59 深夜 / 凌晨
  if (h < 9) return '早上好' // 06:00–08:59
  if (h < 12) return '上午好' // 09:00–11:59
  if (h < 14) return '中午好' // 12:00–13:59
  return '下午好' // 14:00–17:59
}

const props = defineProps({
  titleMain: { type: String, default: '草木蔓发' },
  titleAccent: { type: String, default: '春山可望' },
  subtitle: { type: String, default: '把博客做成一片晴天，慢一点，让灵魂跟上脚步。' },
  greeting: { type: String, default: '' },
  ctaText: { type: String, default: '开始阅读' },
  secondaryText: { type: String, default: '关于我' },
  stats: { type: Array, default: () => [
    { label: '文章', value: 87 },
    { label: '标签', value: 12 },
    { label: '友人', value: 24 },
    { label: '读者', value: '2.4k' }
  ]},
  quote: { type: String, default: '「天气好的时候，写字也会明亮些。」' }
})

defineEmits(['cta', 'secondary'])

// Fixed lettering must never override a changed site name/motto or the sunny theme.
const showMainLettering = computed(() => themeStore.activeThemeId === 'ink' && props.titleMain === '拾光小筑')
const showAccentLettering = computed(() => themeStore.activeThemeId === 'ink' && props.titleAccent === '仗剑天涯')

const displayQuote = computed(() => themeStore.activeThemeId === 'ink'
  ? '「一卷在手，江湖在心。」'
  : props.quote)

// 优先用外部传入的 greeting；未传入时按当前时段动态生成
const displayGreeting = computed(() =>
  (props.greeting && props.greeting.trim()) ? props.greeting : timeGreeting()
)

const rootRef = ref(null)
const sunX = ref(50)
const sunY = ref(50)

const onMouseMove = (e) => {
  if (!rootRef.value) return
  const rect = rootRef.value.getBoundingClientRect()
  sunX.value = ((e.clientX - rect.left) / rect.width) * 100
  sunY.value = ((e.clientY - rect.top) / rect.height) * 100
}

const sunStyle = computed(() => ({
  '--sx': `${sunX.value}%`,
  '--sy': `${sunY.value}%`
}))

const todayStr = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit'
}).replace(/\//g, ' · ')

onMounted(() => {
  rootRef.value?.addEventListener('mousemove', onMouseMove, { passive: true })
})
</script>

<style scoped lang="scss">
.sky-hero {
  position: relative;
  /* 不在根元素裁切——太阳 box-shadow(0 0 120px) 的光晕会远超容器，
     overflow:hidden 会在右侧/底部形成硬截断线 */
  padding: 104px 0 120px;
  /* 不设底色——全局天空渐变透上来，太阳光晕自然向外 bleed，与下方内容区共融 */
}

// 装饰：太阳 + 云
.deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.sun {
  position: absolute;
  right: 8%;
  top: 12%;
  width: 140px; height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, var(--c-autumn-300) 0%, var(--c-autumn-500) 55%, var(--c-autumn-700) 100%);
  box-shadow: 0 0 60px rgba(var(--theme-accent-rgb), 0.5),
              0 0 120px rgba(var(--theme-accent-rgb), 0.25);
  animation: sun-pulse 5s ease-in-out infinite;
  transform: translate(calc(var(--sx, 50%) - 50% - 0px), calc(var(--sy, 50%) - 50% - 0px));
  transition: transform 1.2s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes sun-pulse {
  0%, 100% { box-shadow: 0 0 50px rgba(var(--theme-accent-rgb), .4), 0 0 100px rgba(var(--theme-accent-rgb), .2); }
  50%      { box-shadow: 0 0 80px rgba(var(--theme-accent-rgb), .65), 0 0 140px rgba(var(--theme-accent-rgb), .35); }
}
.cloud {
  position: absolute;
  opacity: 0.85;
  filter: drop-shadow(0 6px 14px rgba(var(--theme-primary-strong-rgb), .12));
}
.cloud-a { top: 18%; right: 18%; width: 180px; animation: cloud-drift 14s ease-in-out infinite; }
.cloud-b { top: 56%; left:  4%;  width: 220px; animation: cloud-drift 18s ease-in-out infinite -4s; }
.cloud-c { top: 28%; left: 18%; width: 140px; animation: cloud-drift 11s ease-in-out infinite -2s; opacity: 0.6; }
@keyframes cloud-drift {
  0%, 100% { transform: translateX(0); }
  50%      { transform: translateX(30px); }
}

// 内容栅格
.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 72px;
  align-items: center;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(var(--theme-paper-rgb), 0.7);
  color: var(--c-botany-900);
  border-radius: 999px;
  font-size: 13px;
  margin-bottom: 34px;
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.5);
  backdrop-filter: blur(6px);
}
.hero-eyebrow .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--c-autumn-500);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(0.85); }
}

.hero-title {
  font-family: var(--font-serif);
  font-size: 52px;
  line-height: 1.2;
  margin: 0 0 36px;
  font-weight: 600;
  color: var(--c-ink, var(--c-ink-800));
}
.title-main {
  display: block;
  background: linear-gradient(135deg, var(--c-botany-900) 0%, var(--c-botany-500) 60%, var(--c-cyan-500) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.title-accent {
  display: block;
  margin-top: 12px;
  font-family: 'Caveat', 'Noto Serif SC', cursive;
  font-size: 60px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--c-autumn-500) 0%, var(--c-autumn-700) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-desc {
  font-size: 16px;
  color: var(--c-ink-soft, var(--c-ink-400));
  line-height: 1.9;
  max-width: 520px;
  margin: 0 0 52px;
}
.hero-actions {
  display: flex;
  gap: 22px;
  margin-bottom: 44px;
  flex-wrap: wrap;
}
// 天气卡
.weather-card { padding: 26px 26px 22px; }
.weather-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  // 左右各让出 8px，让天气信息和图标都"往中间靠一点"，
  // 不再贴着 weather-card 的内边缘
  padding: 0 8px;
}
.weather-eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--c-cyan-700);
  font-weight: 600;
}
.weather-temp {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 600;
  color: var(--c-ink, var(--c-ink-800));
  margin-top: 4px;
}
.weather-feels {
  font-family: inherit;
  font-size: 14px;
  font-weight: 400;
  color: var(--c-ink-soft, var(--c-ink-400));
  margin-left: 4px;
}
.weather-date {
  font-size: 12px;
  color: var(--c-ink-soft, var(--c-ink-400));
  margin-top: 4px;
}

// 城市切换按钮
.weather-loc-btn {
  border: none;
  background: rgba(var(--theme-primary-rgb), 0.12);
  color: var(--c-cyan-700);
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  padding: 3px 6px;
  border-radius: 8px;
  transition: background 0.2s ease;
}
.weather-loc-btn:hover { background: rgba(var(--theme-primary-rgb), 0.24); }
.weather-fail { color: var(--c-ink-soft, var(--c-ink-400)); }

// 天气详情（湿度 / 风力）
.weather-detail {
  margin-top: 4px;
  font-size: 12px;
  color: var(--c-ink-soft, var(--c-ink-300));
  letter-spacing: 0.02em;
}
.wd-sep { margin: 0 4px; opacity: 0.5; }

// 城市搜索面板
.city-picker {
  margin-top: 14px;
  padding: 12px;
  background: rgba(var(--theme-paper-rgb), 0.62);
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.5);
  border-radius: 12px;
  backdrop-filter: blur(6px);
}
.cp-input-wrap {
  display: flex;
  gap: 8px;
}
.cp-input {
  flex: 1;
  min-width: 0;
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.6);
  border-radius: 8px;
  padding: 7px 10px;
  font-size: 13px;
  outline: none;
  background: var(--c-paper);
  color: var(--c-ink, var(--c-ink-800));
  transition: border-color 0.2s ease;
}
.cp-input:focus { border-color: var(--c-botany-500); }
.cp-reset {
  flex-shrink: 0;
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.6);
  background: var(--c-paper);
  color: var(--c-botany-800);
  border-radius: 8px;
  padding: 0 10px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.cp-reset:hover { background: rgba(var(--theme-primary-rgb), 0.12); }
.cp-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  max-height: 184px;
  overflow-y: auto;
}
.cp-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}
.cp-item:hover { background: rgba(var(--theme-primary-rgb), 0.12); }
.cp-name {
  font-weight: 600;
  color: var(--c-ink, var(--c-ink-800));
}
.cp-admin {
  font-size: 12px;
  color: var(--c-ink-soft, var(--c-ink-400));
  flex-shrink: 0;
}
.cp-tip {
  margin-top: 10px;
  font-size: 12px;
  color: var(--c-ink-soft, var(--c-ink-400));
}
.wd-btn {
  align-self: flex-start;
  border: 1px solid var(--c-botany-500, var(--c-botany-500));
  background: rgba(var(--theme-primary-rgb), 0.08);
  color: var(--c-botany-600, var(--c-botany-800));
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.wd-btn:hover {
  background: var(--c-botany-500, var(--c-botany-500));
  color: var(--theme-on-primary);
}
.wd-btn:active {
  transform: scale(0.96);
}
.weather-icon {
  font-size: 56px;
  animation: icon-spin 12s linear infinite;
  filter: drop-shadow(0 4px 12px rgba(var(--theme-accent-rgb), 0.4));
}
@keyframes icon-spin {
  0%, 100% { transform: rotate(-6deg) scale(1); }
  50%      { transform: rotate(6deg) scale(1.08); }
}
.weather-divider {
  height: 1px;
  margin: 18px 0;
  background: linear-gradient(90deg, transparent, rgba(var(--theme-primary-rgb), .3), transparent);
}
.weather-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  text-align: center;
}
.wg-num {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--c-botany-900);
}
.wg-label {
  font-size: 12px;
  color: var(--c-ink-soft, var(--c-ink-400));
  margin-top: 2px;
}
// 第 4 项「阅读」固定放在第二列（即「分类」正下方）居中，
// 避免 3 列 grid + 4 项时最后一项被推到第二行最左。
.weather-grid > div:nth-child(4) { grid-column: 2; }
.weather-quote {
  margin-top: 16px;
  font-size: 12px;
  color: var(--c-ink-soft, var(--c-ink-400));
  font-style: italic;
  line-height: 1.6;
  padding: 10px 14px;
  // 左右对称渐变：两端透明、中间暖色，呼应居中显示
  background: linear-gradient(90deg, transparent, rgba(var(--theme-accent-rgb), 0.18), transparent);
  border-radius: 8px;
  text-align: center;
}

// reveal
.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.8s ease, transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}

/* 水墨主题：真实画作占据首屏，界面信息退为题签。 */
.ink-hero-scene {
  display: block;
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.22s ease;
}
.ink-hero-scene img {
  position: absolute;
  display: block;
  user-select: none;
}
.ink-paper-layer {
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.ink-mountain-layer {
  left: 50%;
  top: 9%;
  width: min(126vw, 2440px);
  height: 76%;
  object-fit: contain;
  object-position: 50% 45%;
  transform: translateX(-50%);
}
.ink-foliage-layer {
  bottom: -7%;
  width: min(42vw, 820px);
  height: 78%;
  object-fit: contain;
}
.ink-foliage-left {
  left: -8%;
  object-position: 0 100%;
}
.ink-foliage-right {
  right: -8%;
  object-position: 100% 100%;
}

@media (max-width: 900px) {
  .hero-grid { grid-template-columns: 1fr; }
  /* 移动端不再隐藏天气卡——原 display:none 导致移动端完全看不到天气、
     也无法切换城市/重试授权。改为单列下全宽堆叠在标题下方。 */
  .weather-card {
    display: block;
    width: 100%;
    margin-top: 8px;
    padding: 22px 20px 18px;
  }
  .weather-temp { font-size: 28px; }
  .hero-title { font-size: 38px; }
  .title-accent { font-size: 44px; }

  .ink-mountain-layer {
    top: 6%;
    width: 174vw;
    height: 56%;
    object-position: 50% 35%;
  }
  .ink-foliage-layer {
    bottom: 11%;
    width: 58vw;
    height: 54%;
  }
  .ink-foliage-left { left: -18%; }
  .ink-foliage-right { right: -20%; }
}
@media (max-width: 480px) {
  .weather-grid { gap: 6px; }
  .wg-num { font-size: 19px; }
  .weather-temp { font-size: 26px; }

  .ink-mountain-layer {
    width: 220vw;
    height: 46%;
  }
  .ink-foliage-layer {
    width: 82vw;
    opacity: .58;
  }
  .ink-foliage-left { left: -34%; }
  .ink-foliage-right { opacity: 0; }
}
</style>
