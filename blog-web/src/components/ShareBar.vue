<template>
  <div class="share-bar reveal">
    <GradientBorderCard variant="sky" class="share-card">
      <div class="share-head">
        <span class="share-eyebrow">SHARE · 分享</span>
        <span v-if="shareCount > 0" class="share-count">{{ shareCount }} 次分享</span>
      </div>
      <div class="share-actions">
        <button v-for="c in channels" :key="c.key"
                class="share-btn"
                :class="['share-btn--' + c.key, { 'share-btn--active': lastClicked === c.key }]"
                :title="c.title"
                @click="onClick(c)">
          <span class="share-btn-icon" v-html="c.icon"></span>
          <span class="share-btn-label">{{ c.label }}</span>
        </button>
      </div>
      <div v-if="lastMsg" class="share-msg" :class="{ 'share-msg--ok': lastOk }">
        {{ lastMsg }}
      </div>
    </GradientBorderCard>

    <!-- 微信对话框:展示 URL 让用户复制 → 粘贴到微信 -->
    <el-dialog v-model="wechatOpen" title="分享到微信" width="min(420px, 92vw)" append-to-body>
      <p class="wx-tip">微信扫码分享需备案域名,这里给你最稳妥的方式:</p>
      <ol class="wx-steps">
        <li>长按下方链接 → 复制</li>
        <li>打开微信 → 粘贴给好友 / 文件传输助手 / 朋友圈链接</li>
      </ol>
      <div class="wx-link-box">
        <input ref="linkInputRef" class="wx-link-input" :value="articleUrl" readonly @focus="$event.target.select()" />
        <el-button type="primary" size="small" @click="copyFromInput">复制</el-button>
      </div>
      <p class="wx-meta">{{ article?.title }}</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { trackShare } from '@/api/share'
import GradientBorderCard from '@/components/GradientBorderCard.vue'

const props = defineProps({
  article: { type: Object, required: true }
})

const articleUrl = computed(() => {
  if (typeof window === 'undefined') return ''
  return `${window.location.origin}/articles/${props.article?.slug || ''}`
})

const shareCount = ref(props.article?.shareCount || 0)
watch(() => props.article?.shareCount, v => { if (typeof v === 'number') shareCount.value = v })
watch(() => props.article?.id, () => { shareCount.value = props.article?.shareCount || 0 })

// 内联 SVG icons(避免引入图标库)
// SECURITY: c.icon 是模块级硬编码常量数组,构建期确定,运行时不变。
//   不来自 API/Pinia/用户输入 → v-html="c.icon" 安全(不是 stored XSS sink)。
const channels = [
  { key: 'copy',  label: '复制链接', title: '复制文章链接',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V5a2 2 0 0 1 2-2h10"/></svg>` },
  { key: 'wechat',label: '微信',     title: '分享到微信',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M8.5 4C4.36 4 1 6.69 1 10c0 1.74.93 3.31 2.4 4.36L3 16l1.95-1.1c.55.16 1.13.27 1.74.31-.04-.27-.06-.55-.06-.83 0-2.76 2.69-5 6-5 .18 0 .36.01.53.03C12.74 5.96 10.83 4 8.5 4M6 7.5a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5m5 0a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5"/><path d="M15 10c-3.31 0-6 2.24-6 5s2.69 5 6 5c.61 0 1.19-.08 1.74-.23L18 21l-.4-1.64C19.07 18.31 20 16.74 20 15c0-3.31-2.69-5-5-5m-2 3.75a.6.6 0 1 1 0-1.2.6.6 0 0 1 0 1.2m4 0a.6.6 0 1 1 0-1.2.6.6 0 0 1 0 1.2"/></svg>` },
  { key: 'weibo', label: '微博',     title: '分享到微博',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M10.5 14.5c-2.7 0-5 1.5-5 3.5s2.3 3.5 5 3.5 5-1.5 5-3.5-2.2-3.5-5-3.5m.8 5.5c-1.4.4-2.9.1-3.3-.7s.4-2 1.8-2.4 2.9-.1 3.3.7-.4 2-1.8 2.4m2.3-4.6c-.3-.1-.5-.1-.3-.5.4-.7.4-1.3 0-1.7-.7-.8-2.6-.1-4.7.3 0 0-.6.1-.5-.3.3-.9-.1-2.1-.4-2.4-1-1-3.6.4-3.6.4S.7 9.5 1.3 11.4c.2.6 1.1 2.3 4.3 2.8 0 0 1.4.4 1.4 1.6 0 1.1-1.7 2 0 2.6 0 0 3.4.8 5.6-1.2 1.6-1.5 1.8-3.7 1.6-4.6-.1-.4-.5-.5-.6-.7M21.5 9c-.7-1.4-2-2.2-3.4-2.1-.5 0-.8.4-.8.8 0 .5.4.8.8.9.9.1 1.6.6 2 1.4.4.8.4 1.7-.1 2.4-.3.4-.2.9.2 1.1.1.1.3.1.5.1.3 0 .5-.1.7-.4.8-1.3.9-2.9.1-4.2m-2 2.6c-.4-.6-1-1-1.7-1.1-.4-.1-.6-.4-.6-.8.1-.4.5-.6.9-.6 1.1.2 2 .8 2.5 1.8.5.9.6 2 .2 3-.1.3-.5.5-.8.4-.4-.1-.6-.5-.5-.9.3-.6.3-1.3 0-1.8"/></svg>` },
  { key: 'qq',    label: 'QQ',      title: '分享到 QQ',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 2C7.6 2 4 5.4 4 9.7c0 1.7.6 3.3 1.6 4.6-.3 1-.9 2.6-1.2 3.5 0 0-.5 1.2.6.7 1.1-.5 2.6-1.3 3.5-1.7 1 .4 2.2.6 3.5.6 4.4 0 8-3.4 8-7.7S16.4 2 12 2M8.7 7.5c.6 0 1.2.4 1.6 1 .4.7.5 1.5.3 1.9-.2.4-.8.4-1.4-.2-.6-.7-.9-1.5-.7-1.9.1-.3.4-.5.7-.5zm-.1 4.5c-.5-.1-.7-.6-.4-1.1.2-.5.7-.8 1.2-.7.5.1.7.6.5 1.1-.2.5-.7.8-1.3.7m6.7-3.5c.4-.6 1-1 1.6-1 .3 0 .6.2.7.5.2.4-.1 1.2-.7 1.9-.6.6-1.2.6-1.4.2-.2-.4-.1-1.2.3-1.9zm.7 3.4c.5-.1 1 .2 1.2.7.2.5 0 1-.4 1.1-.5.1-1-.2-1.2-.7-.3-.5-.1-1 .4-1.1"/></svg>` },
  { key: 'douban',label: '豆瓣',     title: '分享到豆瓣',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2m4 6h-3.5l1 4-3.5-1.5L7 12l1-4H4.5V6.5h7v.5l1 1 1-1V6.5h2.5zm-7.5 6.5h7l-3 3.5 3 3.5h-7v-1.5h4.5L8 18l1-1 1 1 1.5 1.5H7v-1.5z"/></svg>` }
]

const wechatOpen = ref(false)
const lastClicked = ref('')
const lastMsg = ref('')
const lastOk = ref(true)
const linkInputRef = ref(null)

const flash = (msg, ok = true) => {
  lastMsg.value = msg
  lastOk.value = ok
  setTimeout(() => { lastMsg.value = '' }, 2200)
}

async function track(channel, delayMs = 1500) {
  if (!props.article?.id) return
  // 延后一点,避免误点
  setTimeout(async () => {
    try {
      const r = await trackShare(props.article.id, channel)
      const data = r.data || {}
      if (typeof data.shareCount === 'number') shareCount.value = data.shareCount
    } catch (_) { /* 静默,不打扰用户 */ }
  }, delayMs)
}

async function onClick(c) {
  lastClicked.value = c.key
  const url = articleUrl.value
  const title = props.article?.title || ''
  const summary = props.article?.summary || title
  const encUrl = encodeURIComponent(url)
  const encTitle = encodeURIComponent(title)
  const encSummary = encodeURIComponent(summary)

  switch (c.key) {
    case 'copy': {
      try {
        await navigator.clipboard.writeText(url)
        flash('链接已复制到剪贴板')
      } catch {
        // 兜底:弹个选区让用户 Ctrl+C
        flash('复制失败,请手动选择链接', false)
      }
      track('copy', 800)
      break
    }
    case 'wechat': {
      wechatOpen.value = true
      await nextTick()
      linkInputRef.value?.focus()
      track('wechat', 1500)
      break
    }
    case 'weibo': {
      window.open(`https://service.weibo.com/share/share.php?url=${encUrl}&title=${encTitle}`, '_blank', 'noopener,noreferrer')
      flash('已打开微博分享页')
      track('weibo', 1500)
      break
    }
    case 'qq': {
      window.open(`https://connect.qq.com/widget/shareqq/index.html?url=${encUrl}&title=${encTitle}&summary=${encSummary}`, '_blank', 'noopener,noreferrer')
      flash('已打开 QQ 分享页')
      track('qq', 1500)
      break
    }
    case 'douban': {
      window.open(`https://www.douban.com/share/service?href=${encUrl}&name=${encTitle}&description=${encSummary}`, '_blank', 'noopener,noreferrer')
      flash('已打开豆瓣分享页')
      track('douban', 1500)
      break
    }
  }
}

async function copyFromInput() {
  try {
    await navigator.clipboard.writeText(articleUrl.value)
    flash('链接已复制')
  } catch {
    linkInputRef.value?.focus()
    linkInputRef.value?.select()
    flash('请按 Ctrl+C / Cmd+C 复制', false)
  }
  track('wechat', 800)
}
</script>

<style scoped lang="scss">
.share-bar { margin: 40px 0 0; }
.share-card { padding: 18px 22px 20px; }
.share-head {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-bottom: 12px;
}
.share-eyebrow {
  font-size: 12px; letter-spacing: 0.22em; font-weight: 600;
  color: #06b6d4; text-transform: uppercase;
}
.share-count {
  font-size: 12px; color: var(--c-ink-soft);
  font-variant-numeric: tabular-nums;
}
.share-actions {
  display: flex; flex-wrap: wrap; gap: 10px;
}
.share-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(125, 211, 252, 0.55);
  border-radius: 999px;
  color: #0369a1;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.share-btn:hover {
  background: #38bdf8; color: #fff;
  border-color: #38bdf8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.25);
}
.share-btn--active {
  background: #22d3ee; color: #fff; border-color: #22d3ee;
}
.share-btn-icon { display: inline-flex; }
.share-btn--wechat:hover  { background: #07c160; border-color: #07c160; }
.share-btn--weibo:hover   { background: #e6162d; border-color: #e6162d; }
.share-btn--qq:hover      { background: #1296db; border-color: #1296db; }
.share-btn--douban:hover  { background: #2e8b57; border-color: #2e8b57; }
.share-btn--copy:hover    { background: #6366f1; border-color: #6366f1; }

.share-msg {
  margin-top: 10px;
  font-size: 12px;
  color: #b91c1c;
}
.share-msg--ok { color: #047857; }

.wx-tip {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--c-ink-soft);
  line-height: 1.7;
}
.wx-steps {
  margin: 0 0 14px;
  padding-left: 20px;
  font-size: 13px;
  color: var(--c-ink);
  line-height: 1.85;
}
.wx-steps li::marker { color: #06b6d4; font-weight: 600; }
.wx-link-box {
  display: flex; gap: 8px;
  background: rgba(240, 249, 255, 0.85);
  border: 1px dashed #7dd3fc;
  border-radius: 10px;
  padding: 8px 10px;
}
.wx-link-input {
  flex: 1;
  border: none; outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--c-ink);
  font-family: var(--font-sans, system-ui);
}
.wx-meta {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--c-ink-soft);
  text-align: center;
}

@media (max-width: 480px) {
  .share-btn {
    padding: 12px 16px;
    font-size: 14px;
  }
  .share-actions {
    flex-direction: column;
    width: 100%;
  }
}
</style>