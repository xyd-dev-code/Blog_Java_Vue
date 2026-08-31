<template>
  <div class="friends-page">
    <HeroFriends title="友链" subtitle="志同道合的朋友们 · 交换友链请留言" />

    <div class="container-narrow page-body">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-tip">
        <span class="loader-dot"></span>
        <span class="loader-dot"></span>
        <span class="loader-dot"></span>
        <span>{{ wx('正在召唤远方的朋友…') }}</span>
      </div>

      <!-- 召唤卡（晴空 v2 渐变描边） -->
      <GradientBorderCard
        v-if="!loading"
        variant="mix"
        class="friends-hero reveal"
        :class="{ 'is-empty': !published.length }"
      >
        <div class="fh-decor" aria-hidden="true">
          <span class="fh-cloud fh-cloud-1"></span>
          <span class="fh-cloud fh-cloud-2"></span>
          <span class="fh-cloud fh-cloud-3"></span>
        </div>
        <div class="fh-left">
          <span class="section-eyebrow">friends wanted</span>
          <h2 class="fh-title">{{ wx('欢迎互换友链') }} <span class="fh-title-spark">✦</span></h2>
          <p class="fh-desc">
            {{ wx('如果你的博客也写点代码、记点生活，欢迎留言交换。') }}
            <br />{{ wx('愿这条风，把有趣的灵魂串在一起。') }}
          </p>
        </div>
        <div class="fh-right">
          <div class="fh-stat">
            <span class="fh-stat-num">{{ published.length }}</span>
            <span class="fh-stat-label">{{ wx('位朋友') }}</span>
          </div>
          <button class="fh-apply-btn" v-magnetic="{ strength: 5 }" @click="openApplyDialog">
            <el-icon><Link /></el-icon>
            <span>{{ wx('申请友链') }}</span>
          </button>
        </div>
      </GradientBorderCard>

      <!-- 分组列表 -->
      <div v-if="!loading && groups.length" class="friend-groups">
        <section
          v-for="(g, gi) in groups"
          :key="g.name"
          class="fg-section reveal"
          :style="{ transitionDelay: `${100 + gi * 90}ms` }"
        >
          <header class="fg-head">
            <div class="fg-head-left">
              <span class="fg-icon">{{ groupIcon(g.name) }}</span>
              <h3 class="fg-title">{{ g.name }}</h3>
              <span class="fg-count">{{ g.items.length }}</span>
            </div>
            <p class="fg-sub" v-if="groupSub(g.name)">{{ groupSub(g.name) }}</p>
          </header>

          <div class="friend-grid">
            <a
              v-for="(f, fi) in g.items"
              :key="f.id"
              :href="f.url"
              target="_blank"
              rel="noopener"
              class="friend-card-wrap reveal"
              :style="{ transitionDelay: `${150 + (gi * 90) + (fi * 55)}ms` }"
            >
              <GradientBorderCard
                :variant="variantFor(g.name, fi)"
                hoverable
                v-tilt
                class="friend-card"
                :class="`group-${groupClass(g.name)}`"
              >
                <div class="fc-top">
                  <div class="fc-avatar-wrap">
                    <img v-if="f.avatar" :src="f.avatar" class="avatar avatar-img" :alt="f.name" loading="lazy" />
                    <div v-else class="avatar avatar-placeholder">{{ (f.name || '?')[0] }}</div>
                    <span class="fc-avatar-ring" aria-hidden="true"></span>
                  </div>
                  <div class="meta">
                    <h4>
                      {{ f.name }}
                      <span class="meta-badge" v-if="f.recommended === 1">推荐</span>
                    </h4>
                    <p class="url">
                      <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/>
                        <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/>
                      </svg>
                      <span>{{ friendlyUrl(f.url) }}</span>
                    </p>
                  </div>
                </div>
                <p v-if="f.description" class="desc">{{ f.description }}</p>
                <p v-else class="desc desc-empty">{{ wx('这位朋友比较低调，暂无简介～') }}</p>

                <div class="fc-foot">
                  <span class="fc-tag">
                    <span class="fc-tag-dot" :class="`tag-${groupClass(g.name)}`"></span>
                    {{ groupFootTag(g.name) }}
                  </span>
                  <span class="fc-arrow" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="7" y1="17" x2="17" y2="7"/>
                      <polyline points="7 7 17 7 17 17"/>
                    </svg>
                  </span>
                </div>
              </GradientBorderCard>
            </a>

            <!-- 占位卡：补齐到 3 的倍数，让网格视觉完整 -->
            <div
              v-for="n in placeholdersFor(g.items.length)"
              :key="`ph-${g.name}-${n}`"
              class="friend-card-wrap friend-placeholder-wrap reveal"
              :style="{ transitionDelay: `${150 + (gi * 90) + (g.items.length + n) * 55}ms` }"
            >
              <div class="friend-placeholder">
                <span class="ph-cloud" aria-hidden="true">
                  <svg viewBox="0 0 64 40" width="64" height="40" fill="none">
                    <path d="M14 32c-5 0-9-4-9-9 0-4.4 3.2-8.1 7.4-8.9C13.6 7.6 19 4 25 4c6 0 11 4.4 11.6 10.1c.5-.1 1-.1 1.4-.1c6 0 11 4.9 11 11c0 6-5 11-11 11H14z"
                      fill="currentColor" opacity="0.85"/>
                  </svg>
                </span>
                <p class="ph-text">虚位以待</p>
                <p class="ph-sub">{{ wx('下一位朋友，会是你吗？') }}</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 真的没数据时的整页空状态 -->
      <div v-if="!loading && !published.length" class="empty-state">
        <div class="empty-art" aria-hidden="true">
          <div class="es-sun"></div>
          <span class="es-cloud es-cloud-1"></span>
          <span class="es-cloud es-cloud-2"></span>
          <div class="es-hill"></div>
        </div>
        <h3 class="empty-title">{{ wx('这片晴空下还没有友链') }}</h3>
        <p class="empty-sub">{{ wx('换种方式遇见：发起一次申请，把有趣的灵魂串起来。') }}</p>
        <button class="fh-apply-btn fh-apply-btn-lg" v-magnetic="{ strength: 6 }" @click="openApplyDialog">
          <el-icon><Link /></el-icon>
          <span>{{ wx('申请成为第一位朋友') }}</span>
        </button>
      </div>

      <!-- 底部邀请条（无按钮 — 召唤卡已有一个，避免重复） -->
      <div v-if="!loading && published.length" class="friends-invite reveal">
        <div class="cta-art" aria-hidden="true">
          <span class="cta-orb cta-orb-1"></span>
          <span class="cta-orb cta-orb-2"></span>
        </div>
        <div class="invite-quote">「</div>
        <p class="invite-text">
          {{ wx('愿这条风，把有趣的灵魂串在一起。') }}<br />
          <span class="invite-hint">{{ wx('点击召唤卡里的「申请友链」即可加入') }}</span>
        </p>
        <div class="invite-quote invite-quote-r">」</div>
      </div>
    </div>

    <!-- 申请友链弹窗 -->
    <el-dialog
      v-model="applyDialogVisible"
      :title="wx('申请友链')"
      width="min(520px, 92vw)"
      :close-on-click-modal="false"
      :append-to-body="false"
      class="apply-dialog"
    >
      <el-form
        ref="applyFormRef"
        :model="applyForm"
        :rules="applyRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="applyForm.name" :placeholder="wx('你的博客名称')" maxlength="50" />
        </el-form-item>
        <el-form-item label="站点链接" prop="url">
          <el-input v-model="applyForm.url" placeholder="https://" maxlength="200" />
        </el-form-item>
        <el-form-item label="头像链接" prop="avatar">
          <el-input v-model="applyForm.avatar" placeholder="可选，头像图片 URL" maxlength="500" />
        </el-form-item>
        <el-form-item label="站点简介" prop="description">
          <el-input
            v-model="applyForm.description"
            type="textarea"
            :rows="3"
            :placeholder="wx('简单介绍一下你的博客（最多 200 字）')"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="email">
          <el-input v-model="applyForm.email" type="email" inputmode="email" placeholder="可选，审核结果将发送到此邮箱" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">{{ wx('提交申请') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { Link } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { friendLinks as fetchFriendLinks, applyFriendLink } from '@/api/front'
import HeroFriends from '@/components/HeroFriends.vue'
import GradientBorderCard from '@/components/GradientBorderCard.vue'
import { useFoldFit } from '@/composables/useFoldFit'

const published = ref([])
const loading = ref(false)

let observer = null

// 首屏折痕对齐：撑高 hero，让第一行友链卡完整可见、第二行藏到折痕下
// pad 需小于卡片行间距（.friend-grid gap: 18px）
const { refit } = useFoldFit({
  hero: '.friends-page .hero-friends',
  item: '.friends-page .friend-card-wrap',
  min: 140,
  max: 520,
  pad: 10
})

const refresh = async () => {
  loading.value = true
  try {
    published.value = (await fetchFriendLinks()).data || []
  } catch (_) {
    published.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await refresh()
  // 入场动画：观察 .reveal 元素，进入视口时挂上 .visible
  await new Promise(r => requestAnimationFrame(r))
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible')
          observer.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
  )
  document.querySelectorAll('.friends-page .reveal').forEach((el) => observer.observe(el))
  refit()
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})

// 分组：好友 → 网友 → 其他
const GROUP_ORDER = ['好友', '网友']
const groups = computed(() => {
  const map = {}
  published.value.forEach((f) => {
    const g = (f.linkGroup && f.linkGroup.trim()) || '网友'
    if (!map[g]) map[g] = { name: g, items: [] }
    map[g].items.push(f)
  })
  const result = []
  GROUP_ORDER.forEach((k) => { if (map[k]) result.push(map[k]) })
  Object.keys(map).forEach((k) => { if (!GROUP_ORDER.includes(k)) result.push(map[k]) })
  return result
})

const groupIcon = (name) => {
  if (name === '好友') return '♥'
  if (name === '网友') return '🌐'
  if (name === '推荐' || name === '置顶') return '✦'
  return '◆'
}

const groupClass = (name) => {
  if (name === '好友') return 'friend'
  if (name === '网友') return 'net'
  if (name === '推荐' || name === '置顶') return 'star'
  return 'misc'
}

const groupSub = (name) => {
  if (name === '好友') return '那些曾经、现在、一直同行的人'
  if (name === '网友') return '风里雨里，从远方寄来的回响'
  if (name === '推荐' || name === '置顶') return '值得一逛的精选'
  return ''
}

const groupFootTag = (name) => {
  if (name === '好友') return '挚友'
  if (name === '网友') return '网友'
  if (name === '推荐' || name === '置顶') return '推荐'
  return '友链'
}

// 渐变描边卡变体：好友用 sun，荐用 mix，其它 sky
const variantFor = (groupName, idx) => {
  if (groupName === '好友') return 'sun'
  if (groupName === '推荐' || groupName === '置顶') return 'mix'
  return idx % 2 === 0 ? 'sky' : 'mix'
}

// 「推荐」徽章：仅依据后台显式标记 — f.recommended === 1 时显示
// 后台在「友链管理」列表和编辑表单里可控；未勾选时不会出现

const friendlyUrl = (url) => {
  if (!url) return ''
  return url.replace(/^https?:\/\//, '').replace(/\/$/, '')
}

// 占位卡：补齐到 3 的倍数（仅当整页有友链时显示）
const placeholdersFor = (n) => {
  if (n === 0) return 0
  const rem = n % 3
  return rem === 0 ? 0 : 3 - rem
}

// ---------- 申请友链 ----------
const applyDialogVisible = ref(false)
const applying = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({
  name: '',
  url: '',
  avatar: '',
  description: '',
  email: ''
})

const applyRules = {
  name: [
    { required: true, message: '请输入站点名称', trigger: 'blur' },
    { max: 50, message: '站点名称不能超过 50 个字符', trigger: 'blur' }
  ],
  url: [
    { required: true, message: '请输入站点链接', trigger: 'blur' },
    { pattern: /^https?:\/\/.+/, message: '链接需以 http:// 或 https:// 开头', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入站点简介', trigger: 'blur' },
    { max: 200, message: '简介不能超过 200 字', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const openApplyDialog = () => {
  Object.assign(applyForm, { name: '', url: '', avatar: '', description: '', email: '' })
  applyFormRef.value?.resetFields()
  applyDialogVisible.value = true
}

const submitApply = async () => {
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return

  applying.value = true
  try {
    await applyFriendLink({
      name: applyForm.name,
      url: applyForm.url,
      avatar: applyForm.avatar || undefined,
      description: applyForm.description,
      email: applyForm.email || undefined
    })
    ElMessage.success(wx('友链申请已提交，审核通过后将展示在友链页面'))
    applyDialogVisible.value = false
  } catch (e) {
    // 错误已在拦截器中统一处理
  } finally {
    applying.value = false
  }
}
</script>

<style scoped lang="scss">
.page-body { padding: 0 0 80px; }

/* 加载态 */
.loading-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--c-ink-soft);
  padding: 80px 0;
  font-size: 14px;
}
.loader-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--c-botany-500);
  animation: loader-bounce 1.2s ease-in-out infinite;
}
.loader-dot:nth-child(2) { animation-delay: 0.15s; }
.loader-dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes loader-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.45; }
  40%           { transform: scale(1.2); opacity: 1; }
}

/* ============ Hero 召唤卡 ============ */
.friends-hero {
  position: relative;
  padding: 16px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  background:
    radial-gradient(ellipse 60% 70% at 0% 50%, rgba(var(--theme-primary-rgb), 0.12) 0%, transparent 60%),
    radial-gradient(ellipse 50% 70% at 100% 0%, rgba(var(--theme-accent-rgb), 0.10) 0%, transparent 60%),
    linear-gradient(135deg, var(--c-hero-cool) 0%, var(--c-hero-warm) 100%);
  margin-bottom: 14px;
  overflow: hidden;
}
.friends-hero.is-empty { margin-bottom: 0; }

.fh-decor { position: absolute; inset: 0; pointer-events: none; }
.fh-cloud {
  position: absolute;
  background: rgba(var(--theme-paper-rgb), 0.6);
  border-radius: 100px;
  filter: blur(2px);
  animation: cloud-drift 18s linear infinite;
}
.fh-cloud-1 { width: 80px; height: 18px; top: 18%; right: 36%; opacity: 0.55; animation-duration: 22s; }
.fh-cloud-2 { width: 50px; height: 12px; top: 60%; right: 18%; opacity: 0.45; animation-duration: 26s; animation-direction: reverse; }
.fh-cloud-3 { width: 30px; height: 8px; top: 30%; right: 12%; opacity: 0.35; animation-duration: 30s; }
@keyframes cloud-drift {
  0%   { transform: translateX(40px); }
  50%  { transform: translateX(-30px); }
  100% { transform: translateX(40px); }
}

.fh-left { flex: 1; min-width: 220px; position: relative; z-index: 1; }
.section-eyebrow {
  display: inline-block;
  font-size: 12px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--c-cyan-700);
  font-weight: 600;
  margin-bottom: 10px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(var(--theme-cyan-strong-rgb), 0.08);
  border: 1px solid rgba(var(--theme-cyan-strong-rgb), 0.18);
}
.fh-title {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  margin: 0 0 8px;
  background: linear-gradient(135deg, var(--c-botany-900) 0%, var(--c-botany-500) 55%, var(--c-autumn-700) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
  display: flex;
  align-items: center;
  gap: 8px;
}
.fh-title-spark {
  font-size: 22px;
  -webkit-text-fill-color: initial;
  background: none;
  color: var(--c-autumn-500);
  filter: drop-shadow(0 0 6px rgba(var(--theme-accent-rgb), 0.45));
  animation: twinkle 3s ease-in-out infinite;
}
@keyframes twinkle {
  0%, 100% { transform: rotate(0deg) scale(1); opacity: 1; }
  50%      { transform: rotate(20deg) scale(1.2); opacity: 0.75; }
}
.fh-desc {
  font-size: 14px;
  color: var(--c-ink-soft);
  margin: 0;
  line-height: 1.7;
}
.fh-right {
  text-align: right;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}
.fh-stat { text-align: right; }
.fh-stat-num {
  display: inline-block;
  font-family: var(--font-serif);
  font-size: 42px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-autumn-500));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.fh-stat-label {
  display: block;
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 4px;
  letter-spacing: 0.1em;
}

/* 申请友链按钮 */
.fh-apply-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  color: var(--theme-on-primary);
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  border: none;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(var(--theme-primary-strong-rgb), 0.3);
  transition: all 0.3s ease;
  font-family: inherit;
}
.fh-apply-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(var(--theme-primary-strong-rgb), 0.4);
}
.fh-apply-btn:active { transform: translateY(0); }
.fh-apply-btn-lg { padding: 12px 28px; font-size: 14px; }

/* ============ 整页空状态 ============ */
.empty-state {
  text-align: center;
  padding: 30px 24px 50px;
  position: relative;
}
.empty-art {
  position: relative;
  width: 320px;
  max-width: 80%;
  height: 200px;
  margin: 0 auto 30px;
}
.es-sun {
  position: absolute;
  top: 18px; right: 30px;
  width: 70px; height: 70px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--c-autumn-200) 0%, var(--c-autumn-500) 50%, var(--c-autumn-700) 100%);
  box-shadow: 0 0 30px rgba(var(--theme-accent-rgb), 0.45);
  animation: sun-pulse 4s ease-in-out infinite;
}
@keyframes sun-pulse {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.05); }
}
.es-cloud {
  position: absolute;
  background: rgba(var(--theme-paper-rgb), 0.85);
  border-radius: 100px;
  box-shadow: 0 4px 12px rgba(var(--theme-primary-rgb), 0.08);
}
.es-cloud-1 {
  width: 90px; height: 22px;
  top: 30px; left: 30px;
  animation: cloud-drift 12s linear infinite;
}
.es-cloud-2 {
  width: 60px; height: 16px;
  top: 70px; left: 130px;
  animation: cloud-drift 16s linear infinite reverse;
}
.es-hill {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 60px;
  background: linear-gradient(180deg, rgba(var(--theme-primary-rgb), 0.10), rgba(var(--theme-primary-strong-rgb), 0.18));
  border-radius: 50% 50% 0 0 / 80% 80% 0 0;
  transform: scaleX(1.4);
}
.empty-title {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--c-ink);
  margin: 0 0 8px;
}
.empty-sub {
  font-size: 14px;
  color: var(--c-ink-soft);
  margin: 0 0 24px;
}

/* ============ 分组 ============ */
.friend-groups {
  display: flex;
  flex-direction: column;
  gap: 36px;
}
.fg-section { position: relative; }
.fg-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 22px;
  padding-left: 16px;
  position: relative;
}
.fg-head::before {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 4px; height: 22px;
  background: linear-gradient(180deg, var(--c-botany-500), var(--c-autumn-500));
  border-radius: 2px;
}
.fg-head-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.fg-icon {
  font-size: 20px;
  color: var(--c-autumn-700);
  line-height: 1;
}
.fg-title {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--c-ink);
  margin: 0;
}
.fg-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 7px;
  font-size: 12px;
  color: var(--c-botany-900);
  background: rgba(var(--theme-primary-rgb), 0.10);
  border: 1px solid rgba(var(--theme-primary-rgb), 0.22);
  border-radius: 999px;
  font-weight: 500;
}
.fg-sub {
  font-size: 13px;
  color: var(--c-ink-soft);
  margin: 0;
  font-style: italic;
  letter-spacing: 0.02em;
}

/* ============ 网格 ============ */
.friend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.friend-card-wrap {
  display: block;
  text-decoration: none;
  color: inherit;
}

/* ============ 卡片 ============ */
.friend-card {
  padding: 20px 22px 16px;
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  /* 让卡片整体在 hover 时不溢出 */
  overflow: hidden;
}
/* 卡片左下角小色块标识组别 */
.friend-card::after {
  content: '';
  position: absolute;
  left: 0; bottom: 0;
  width: 0; height: 3px;
  background: linear-gradient(90deg, var(--c-botany-500), var(--c-autumn-500));
  transition: width 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 0 3px 0 0;
}
.friend-card.group-friend::after { background: linear-gradient(90deg, var(--c-autumn-500), var(--c-autumn-700)); }
.friend-card.group-net::after   { background: linear-gradient(90deg, var(--c-botany-500), var(--c-cyan-500)); }
.friend-card.group-star::after  { background: linear-gradient(90deg, var(--c-plum-500), var(--c-rouge-500)); }
.friend-card:hover::after { width: 100%; }

.fc-top {
  display: flex;
  align-items: center;
  gap: 14px;
}
.fc-avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 52px; height: 52px;
}
.avatar {
  width: 100%; height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background: var(--c-ink-100);
  position: relative;
  z-index: 2;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.avatar-img { display: block; }
.avatar-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-cyan-500));
  color: var(--theme-on-primary);
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
}
.friend-card.group-friend .avatar-placeholder {
  background: linear-gradient(135deg, var(--c-autumn-500), var(--c-autumn-700));
}
.friend-card.group-star .avatar-placeholder {
  background: linear-gradient(135deg, var(--c-plum-500), var(--c-rouge-500));
}
.fc-avatar-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(var(--theme-primary-rgb), 0.6), rgba(var(--theme-accent-rgb), 0.6));
  z-index: 1;
  opacity: 0.55;
  transition: opacity 0.3s ease, transform 0.4s ease;
}
.friend-card.group-friend .fc-avatar-ring { background: linear-gradient(135deg, rgba(var(--theme-accent-rgb), 0.6), rgba(var(--theme-accent-strong-rgb), 0.6)); }
.friend-card.group-star .fc-avatar-ring  { background: linear-gradient(135deg, rgba(var(--theme-plum-rgb), 0.6), rgba(var(--theme-rouge-rgb), 0.6)); }
.friend-card:hover .avatar { transform: scale(1.05) rotate(-3deg); }
.friend-card:hover .fc-avatar-ring { opacity: 1; transform: scale(1.08); }

.friend-card .meta { min-width: 0; flex: 1; }
.friend-card .meta h4 {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--c-ink);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}
.friend-card:hover .meta h4 { color: var(--c-botany-900); }
.meta-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 500;
  color: var(--c-autumn-900);
  background: linear-gradient(135deg, rgba(var(--theme-accent-rgb), 0.18), rgba(var(--theme-accent-strong-rgb), 0.10));
  border: 1px solid rgba(var(--theme-accent-strong-rgb), 0.30);
  border-radius: 999px;
  letter-spacing: 0.04em;
  flex-shrink: 0;
  transform: translateY(-1px);
}
.friend-card .meta .url {
  font-size: 12px;
  color: var(--c-ink-300);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s ease;
}
.friend-card:hover .meta .url { color: var(--c-botany-700); }

.desc {
  font-size: 13px;
  color: var(--c-ink-soft);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}
.desc-empty { color: var(--c-ink-200); font-style: italic; }

.fc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 4px;
}
.fc-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--c-ink-soft);
  letter-spacing: 0.04em;
}
.fc-tag-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--c-botany-500);
  box-shadow: 0 0 6px currentColor;
}
.tag-friend { background: var(--c-autumn-500); color: var(--c-autumn-700); }
.tag-net    { background: var(--c-botany-500); color: var(--c-botany-700); }
.tag-star   { background: var(--c-plum-500); color: var(--c-plum-500); }
.tag-misc   { background: var(--c-ink-300); color: var(--c-ink-400); }

.fc-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px; height: 28px;
  border-radius: 50%;
  color: var(--c-ink-300);
  background: rgba(var(--theme-neutral-muted-rgb), 0.08);
  transition: all 0.3s ease;
}
.friend-card:hover .fc-arrow {
  color: var(--theme-on-primary);
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  transform: translateX(2px) rotate(0deg);
  box-shadow: 0 4px 12px rgba(var(--theme-primary-strong-rgb), 0.3);
}

/* ============ 占位卡 ============ */
.friend-placeholder-wrap {
  text-decoration: none;
  color: inherit;
  pointer-events: none;
}
.friend-placeholder {
  position: relative;
  height: 100%;
  min-height: 168px;
  border: 2px dashed rgba(var(--theme-primary-rgb), 0.30);
  border-radius: 18px;
  background:
    repeating-linear-gradient(45deg,
      rgba(var(--theme-primary-rgb), 0.03) 0 6px,
      transparent 6px 12px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 18px;
  color: var(--c-ink-soft);
  transition: all 0.3s ease;
}
.friend-placeholder:hover {
  border-color: rgba(var(--theme-primary-rgb), 0.55);
  background:
    repeating-linear-gradient(45deg,
      rgba(var(--theme-primary-rgb), 0.06) 0 6px,
      transparent 6px 12px);
}
.ph-cloud {
  color: rgba(var(--theme-primary-rgb), 0.35);
  animation: float 6s ease-in-out infinite;
}
.ph-text {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
  color: var(--c-ink-soft);
  margin: 6px 0 0;
}
.ph-sub {
  font-size: 12px;
  color: var(--c-ink-300);
  margin: 0;
  text-align: center;
}

/* ============ 底部 CTA ============ */
/* ============ 底部邀请条（无按钮，避免与召唤卡重复） ============ */
.friends-invite {
  margin-top: 36px;
  padding: 22px 28px;
  border-radius: 18px;
  background:
    radial-gradient(ellipse 60% 100% at 0% 50%, rgba(var(--theme-primary-rgb), 0.10) 0%, transparent 70%),
    radial-gradient(ellipse 60% 100% at 100% 50%, rgba(var(--theme-accent-rgb), 0.10) 0%, transparent 70%),
    linear-gradient(135deg, rgba(var(--theme-paper-rgb), 0.9), rgba(var(--theme-paper-rgb), 0.7));
  border: 1px solid rgba(var(--theme-primary-rgb), 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  text-align: center;
}
.cta-art {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.cta-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
}
.cta-orb-1 {
  width: 160px; height: 160px;
  top: -60px; right: 30%;
  background: rgba(var(--theme-primary-rgb), 0.12);
  animation: float 8s ease-in-out infinite;
}
.cta-orb-2 {
  width: 120px; height: 120px;
  bottom: -50px; right: 8%;
  background: rgba(var(--theme-accent-rgb), 0.12);
  animation: float 10s ease-in-out infinite -3s;
}
.invite-quote {
  font-family: var(--font-serif);
  font-size: 32px;
  line-height: 1;
  color: rgba(var(--theme-primary-rgb), 0.5);
  font-weight: 300;
  flex-shrink: 0;
  position: relative;
}
.invite-quote-r { transform: translateY(8px); }
.invite-text {
  font-family: var(--font-serif);
  font-size: 14px;
  line-height: 1.7;
  color: var(--c-ink-soft);
  margin: 0;
  position: relative;
  letter-spacing: 0.04em;
}
.invite-hint {
  display: inline-block;
  margin-top: 4px;
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--c-botany-700);
  letter-spacing: 0.06em;
}

/* ============ 申请弹窗 ============ */
:deep(.apply-dialog) { border-radius: 14px; }
:deep(.apply-dialog .el-dialog__header) { padding: 22px 28px 0; }
:deep(.apply-dialog .el-dialog__title) {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--c-ink);
}
:deep(.apply-dialog .el-dialog__body) { padding: 16px 28px 0; }
:deep(.apply-dialog .el-dialog__footer) { padding: 16px 28px 24px; }

/* ============ reveal 入场动画 ============ */
.reveal {
  opacity: 0;
  transform: translateY(28px);
  transition:
    opacity 0.7s ease,
    transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .friend-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .friends-hero { padding: 26px 22px; }
  .fh-title { font-size: 22px; }
  .fh-right {
    text-align: left;
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }
  .friends-invite { padding: 18px 18px; flex-direction: row; flex-wrap: wrap; }
}
@media (max-width: 640px) {
  .friend-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  :deep(.apply-dialog) { width: 92vw !important; max-width: 380px; }
}

@media (prefers-reduced-motion: reduce) {
  .reveal { opacity: 1; transform: none; transition: none; }
  .fc-cloud, .es-cloud, .fh-cloud, .cta-orb, .es-sun, .ph-cloud, .fh-title-spark {
    animation: none !important;
  }
}
</style>
