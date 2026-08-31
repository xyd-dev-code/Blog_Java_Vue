<template>
  <div class="theme-management">
    <header class="theme-intro">
      <div class="theme-intro-copy">
        <span class="theme-eyebrow">Appearance</span>
        <h1>{{ wx('主题管理') }}</h1>
        <p>网站主题由管理员统一设置，保存后所有访客使用同一套主题。</p>
      </div>

      <div class="current-theme" aria-live="polite">
        <span class="current-label">{{ wx('当前主题') }}</span>
        <strong>
          <i :style="{ background: themeStore.activeTheme.swatches[0] }" aria-hidden="true" />
          {{ themeStore.activeTheme.name }}
        </strong>
        <small>{{ saveStatus || '站点级配置，仅管理员可修改' }}</small>
      </div>
    </header>

    <section class="theme-section" aria-labelledby="theme-list-title">
      <div class="section-heading">
        <div>
          <h2 id="theme-list-title">{{ wx('已注册主题') }}</h2>
          <p>系统缺省保持为「晴天」，新设置会写入服务端站点配置。</p>
        </div>
        <span class="theme-count">{{ themeStore.themes.length }} 套主题</span>
      </div>

      <div class="theme-grid">
        <button
          v-for="theme in themeStore.themes"
          :key="theme.id"
          class="theme-card"
          :class="{ active: theme.id === themeStore.activeThemeId }"
          :aria-pressed="theme.id === themeStore.activeThemeId"
          :aria-busy="theme.id === savingThemeId"
          :disabled="Boolean(savingThemeId) || theme.id === themeStore.activeThemeId"
          type="button"
          @click="selectTheme(theme.id)"
        >
          <span class="theme-preview" :class="{ 'preview-wuxia': theme.id === 'ink' }" :style="previewStyle(theme)" aria-hidden="true">
            <img v-if="theme.id === 'ink'" class="preview-wuxia-art" src="/images/wuxia-jianghu-border.webp" alt="" loading="lazy" />
            <span class="preview-topbar">
              <i /><i /><i />
            </span>
            <span class="preview-sun" />
            <span class="preview-mountain preview-mountain-far" />
            <span class="preview-mountain preview-mountain-near" />
            <span class="preview-panel">
              <i class="preview-title" />
              <i class="preview-line" />
              <i class="preview-line preview-line-short" />
              <i class="preview-action" />
            </span>
          </span>

          <span class="theme-card-body">
            <span class="theme-card-heading">
              <span>
                <strong>{{ theme.name }}</strong>
                <small>{{ theme.description }}</small>
              </span>
              <span v-if="theme.id === 'sunny'" class="default-badge">系统默认</span>
            </span>

            <span class="theme-palette" aria-hidden="true">
              <i
                v-for="color in theme.swatches"
                :key="color"
                :style="{ background: color }"
                aria-hidden="true"
              />
            </span>

            <span class="theme-action" :class="{ selected: theme.id === themeStore.activeThemeId }">
              <el-icon v-if="theme.id === savingThemeId" class="saving-icon" aria-hidden="true"><Loading /></el-icon>
              <el-icon v-else-if="theme.id === themeStore.activeThemeId" aria-hidden="true"><Check /></el-icon>
              {{ theme.id === savingThemeId ? '正在保存' : (theme.id === themeStore.activeThemeId ? '正在使用' : '设为网站主题') }}
            </span>
          </span>
        </button>
      </div>
    </section>

    <aside class="theme-note">
      <el-icon aria-hidden="true"><Lock /></el-icon>
      <div>
        <strong>{{ wx('管理员专属设置') }}</strong>
        <p>前台不提供主题切换入口。访客刷新或下次访问时会读取这里保存的主题；该设置不会修改文章、页面结构或其他业务数据。</p>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref } from 'vue'
import { Check, Loading, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { adminSaveSiteConfig } from '@/api/admin'
import { useSiteStore } from '@/stores/site'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const siteStore = useSiteStore()
const savingThemeId = ref('')
const saveStatus = ref('')

const previewStyle = (theme) => ({
  '--preview-primary': theme.swatches[0],
  '--preview-paper': theme.swatches[1],
  '--preview-accent': theme.swatches[2],
})

const selectTheme = async (themeId) => {
  if (themeId === themeStore.activeThemeId || savingThemeId.value) return
  const targetTheme = themeStore.themes.find((theme) => theme.id === themeId)
  savingThemeId.value = themeId
  saveStatus.value = `正在保存「${targetTheme?.name || themeId}」…`
  try {
    await adminSaveSiteConfig({ siteTheme: themeId })
    siteStore.setSiteTheme(themeId)
    themeStore.setTheme(themeId)
    saveStatus.value = `「${targetTheme?.name || themeId}」已设为网站主题`
    ElMessage.success('网站主题已更新')
  } catch (_) {
    saveStatus.value = '保存失败，网站主题未发生变化'
  } finally {
    savingThemeId.value = ''
  }
}
</script>

<style scoped lang="scss">
.theme-management {
  width: min(1120px, 100%);
  min-width: 0;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

.theme-management > *,
.theme-intro-copy,
.theme-section,
.theme-grid,
.theme-card-body {
  min-width: 0;
  max-width: 100%;
}

.theme-intro {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--c-line);
  border-radius: var(--radius);
  background: var(--c-paper);
  box-shadow: var(--shadow-soft);
}

.theme-intro-copy {
  min-width: 0;

  h1 {
    margin: 4px 0 8px;
    color: var(--c-ink);
    font-family: var(--font-serif);
    font-size: clamp(24px, 3vw, 32px);
    line-height: 1.2;
  }

  p {
    margin: 0;
    color: var(--c-ink-soft);
    line-height: 1.6;
    overflow-wrap: anywhere;
  }
}

.theme-eyebrow {
  color: var(--c-botany-700);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.current-theme {
  min-width: 220px;
  padding: 16px 18px;
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius-sm);
  background: var(--c-paper-soft);
  display: grid;
  gap: 5px;

  strong {
    color: var(--c-ink);
    font-family: var(--font-serif);
    font-size: 18px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  strong i {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    box-shadow: 0 0 0 3px var(--c-line-soft);
  }

  small { color: var(--c-ink-soft); }
}

.current-label {
  color: var(--c-ink-soft);
  font-size: 12px;
}

.theme-section {
  display: grid;
  gap: 16px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 0 0 4px;
    color: var(--c-ink);
    font-family: var(--font-serif);
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--c-ink-soft);
    font-size: 13px;
    line-height: 1.5;
  }
}

.theme-count,
.default-badge {
  flex-shrink: 0;
  padding: 4px 9px;
  border-radius: 999px;
  background: var(--c-botany-50);
  color: var(--c-botany-800);
  font-size: 12px;
  font-weight: 600;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: 16px;
}

.theme-card {
  min-width: 0;
  padding: 0;
  overflow: hidden;
  border: 2px solid var(--c-line-soft);
  border-radius: var(--radius);
  background: var(--c-paper);
  color: var(--c-ink);
  box-shadow: var(--shadow-soft);
  font: inherit;
  text-align: left;
  cursor: pointer;
  touch-action: manipulation;
  transition:
    border-color var(--theme-motion-fast) var(--theme-motion-ease),
    box-shadow var(--theme-motion-base) var(--theme-motion-ease),
    transform var(--theme-motion-base) var(--theme-motion-ease);

  &.active {
    border-color: var(--c-botany-500);
    box-shadow: var(--shadow-pop);
  }

  &:focus-visible {
    outline: 3px solid var(--theme-focus-ring);
    outline-offset: 3px;
  }

  &:active { transform: scale(0.99); }

  &:disabled {
    cursor: default;
  }

  &[aria-busy='true'] { cursor: wait; }
}

.theme-preview {
  position: relative;
  height: 190px;
  overflow: hidden;
  background: var(--preview-paper);
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--preview-paper) 88%, var(--preview-primary)),
    var(--preview-paper)
  );
  display: block;
}

.preview-topbar {
  position: absolute;
  z-index: 4;
  inset: 0 0 auto;
  height: 30px;
  padding: 0 12px;
  border-bottom: 1px solid color-mix(in srgb, var(--preview-primary) 20%, transparent);
  background: color-mix(in srgb, var(--preview-paper) 82%, transparent);
  display: flex;
  align-items: center;
  gap: 5px;

  i {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--preview-primary);
    opacity: 0.6;
  }
}

.preview-wuxia-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: .8;
}
.preview-wuxia :is(.preview-sun, .preview-mountain) { opacity: 0; }

.preview-sun {
  position: absolute;
  top: 48px;
  right: 42px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--preview-accent);
  opacity: 0.78;
}

.preview-mountain {
  position: absolute;
  inset: auto -4% 0;
  height: 58%;
  background: var(--preview-primary);
  clip-path: polygon(0 78%, 13% 61%, 23% 70%, 39% 34%, 49% 60%, 62% 46%, 74% 64%, 87% 42%, 100% 63%, 100% 100%, 0 100%);
}

.preview-mountain-far {
  bottom: 24px;
  opacity: 0.18;
  transform: scaleX(1.05);
}

.preview-mountain-near {
  opacity: 0.34;
  transform: scaleX(1.12) translateY(22px);
}

.preview-panel {
  position: absolute;
  z-index: 3;
  left: 24px;
  top: 52px;
  width: 44%;
  min-width: 126px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--preview-primary) 18%, transparent);
  border-radius: 6px;
  background: color-mix(in srgb, var(--preview-paper) 88%, transparent);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--preview-primary) 12%, transparent);
  display: grid;
  gap: 8px;

  i { display: block; }
}

.preview-title {
  width: 68%;
  height: 9px;
  border-radius: 99px;
  background: var(--preview-primary);
}

.preview-line {
  width: 92%;
  height: 4px;
  border-radius: 99px;
  background: var(--preview-primary);
  opacity: 0.26;
}

.preview-line-short { width: 62%; }

.preview-action {
  width: 42px;
  height: 15px;
  margin-top: 3px;
  border-radius: 99px;
  background: var(--preview-accent);
}

.theme-card-body {
  padding: 18px;
  display: grid;
  gap: 14px;
}

.theme-card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  > span:first-child {
    min-width: 0;
    display: grid;
    gap: 3px;
  }

  strong {
    font-family: var(--font-serif);
    font-size: 18px;
  }

  small {
    color: var(--c-ink-soft);
    line-height: 1.45;
  }
}

.theme-palette {
  display: flex;
  gap: 8px;

  i {
    width: 24px;
    height: 24px;
    border: 1px solid var(--c-line);
    border-radius: 50%;
    box-shadow: inset 0 0 0 2px var(--c-paper);
  }
}

.theme-action {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid var(--c-botany-300);
  border-radius: var(--radius-sm);
  color: var(--c-botany-800);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 700;

  &.selected {
    border-color: var(--c-botany-500);
    background: var(--c-botany-500);
    color: var(--theme-on-primary);
  }
}

.saving-icon {
  animation: theme-saving-spin 0.9s linear infinite;
}

@keyframes theme-saving-spin {
  to { transform: rotate(360deg); }
}

.theme-note {
  padding: 16px 18px;
  border: 1px solid var(--c-line);
  border-radius: var(--radius-sm);
  background: var(--c-paper-soft);
  color: var(--c-ink-soft);
  display: flex;
  align-items: flex-start;
  gap: 12px;

  > .el-icon {
    flex-shrink: 0;
    margin-top: 2px;
    color: var(--c-botany-700);
    font-size: 18px;
  }

  strong { color: var(--c-ink); }

  p {
    margin: 4px 0 0;
    line-height: 1.6;
  }
}

@media (hover: hover) and (pointer: fine) {
  .theme-card:hover:not(.active):not(:disabled) {
    border-color: var(--c-botany-300);
    box-shadow: var(--shadow-pop);
    transform: translateY(-3px);
  }
}

@media (max-width: 700px) {
  .theme-intro {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .current-theme { min-width: 0; }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .theme-grid { grid-template-columns: minmax(0, 1fr); }

  .theme-preview { height: 168px; }
}

@media (prefers-reduced-motion: reduce) {
  .theme-card { transition: none; }
  .saving-icon { animation: none; }
}
</style>
