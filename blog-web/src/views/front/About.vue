<template>
  <div class="about-page">
    <!-- ① SkyPageHead 横幅 -->
    <HeroAbout title="关于我" subtitle="在代码与文字之间，寻找自己的位置" />

    <div v-if="!loading">
      <!-- ② 个人资料卡片 -->
      <section class="section container-narrow">
        <div class="profile-card reveal">
          <div class="profile-top">
            <div class="avatar-wrap">
              <img class="avatar" :src="siteAvatar" alt="头像" />
            </div>
            <div class="profile-info">
              <div class="name-row">
                <h2>{{ authorName }}</h2>
                <span class="role-badge">全栈工程师</span>
              </div>
              <p class="bio">{{ bio }}</p>
              <div class="social-row">
                <a :href="siteGithub" target="_blank" class="soc-btn" title="GitHub">
                  <svg viewBox="0 0 24 24"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.37 1.23-3.205-.135-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.255 2.88.12 3.18.765.84 1.23 1.905 1.23 3.205 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/></svg>
                </a>
                <a :href="'mailto:' + siteEmail" class="soc-btn" title="Email">
                  <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                </a>
              </div>
            </div>
          </div>

          <!-- 统计面板 -->
          <div class="stats-row">
            <div class="stat-item" v-for="(s, si) in statsList" :key="s.label" :style="{ '--idx': si }">
              <div class="stat-icon">
                <svg v-if="s.key === 'article'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                <svg v-if="s.key === 'category'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                <svg v-if="s.key === 'tag'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
                <svg v-if="s.key === 'view'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </div>
              <div class="stat-num">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- ③ 技能标签云 -->
      <section class="section container-narrow reveal">
        <h3 class="sec-title"><span class="sec-line"></span>技术栈<span class="sec-line"></span></h3>
        <div class="skills-cloud">
          <span class="skill-tag" v-for="sk in skills" :key="sk">{{ sk }}</span>
        </div>
      </section>

      <!-- ④ 我的故事（CMS Markdown） -->
      <section class="section container-narrow reveal" v-if="processedMd">
        <h3 class="sec-title"><span class="sec-line"></span>我的故事<span class="sec-line"></span></h3>
        <div class="story-card">
          <MdPreview
            :model-value="processedMd"
            theme="light"
            preview-theme="default"
            code-theme="atom-one-light"
            :show-outline="false"
          />
        </div>
      </section>

      <!-- ⑤ 成长轨迹时间线 -->
      <section class="section container-narrow reveal">
        <h3 class="sec-title"><span class="sec-line"></span>成长轨迹<span class="sec-line"></span></h3>
        <div class="timeline">
          <div class="tl-item" v-for="(item, idx) in timeline" :key="idx">
            <div class="tl-dot" :class="{ active: idx === 0 }"></div>
            <div class="tl-card card">
              <div class="tl-time">{{ item.time }}</div>
              <div class="tl-title">{{ item.title }}</div>
              <p class="tl-desc">{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- ⑥ 联系卡片 -->
      <section class="section container-narrow reveal" style="padding-bottom: 80px;">
        <h3 class="sec-title"><span class="sec-line"></span>联系我<span class="sec-line"></span></h3>
        <div class="contact-grid">
          <div class="contact-item card">
            <div class="ci-icon ci-email">@</div>
            <h4>邮箱</h4>
            <p>{{ siteEmail }}</p>
            <a :href="'mailto:' + siteEmail" class="ci-link">发送邮件 &rarr;</a>
          </div>
          <div class="contact-item card">
            <div class="ci-icon ci-github">
              <svg viewBox="0 0 24 24"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.37 1.23-3.205-.135-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.255 2.88.12 3.18.765.84 1.23 1.905 1.23 3.205 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/></svg>
            </div>
            <h4>GitHub</h4>
            <p>{{ githubDisplay }}</p>
            <a :href="siteGithub" target="_blank" class="ci-link">访问主页 &rarr;</a>
          </div>
          <div class="contact-item card sub-trigger" @click="openSubDialog" role="button" tabindex="0" @keyup.enter="openSubDialog" @keyup.space="openSubDialog">
            <div class="ci-icon ci-mail">✉</div>
            <h4>邮箱订阅</h4>
            <p>输入邮箱，第一时间收到新文章</p>
            <a href="javascript:;" class="ci-link" @click.stop="openSubDialog">立即订阅 &rarr;</a>
          </div>
        </div>
      </section>
    </div>

    <div v-else class="loading-page container-narrow">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 邮箱订阅弹窗（与上方加载态平级，不进入 v-if 分支） -->
    <el-dialog
      v-model="subDialogVisible"
      title="邮箱订阅"
      width="420px"
      class="sub-dialog"
      :close-on-click-modal="false"
    >
      <p class="sub-dialog-desc">输入你的邮箱，新文章发布当天即可收到通知。无需注册，点确认链接即生效。</p>
      <el-form ref="subFormRef" :model="subForm" :rules="subRules" @submit.prevent="onSubscribe">
        <el-form-item prop="email">
          <el-input
            v-model="subForm.email"
            type="email"
            inputmode="email"
            placeholder="你的邮箱地址，例如 you@example.com"
            clearable
            maxlength="80"
            :disabled="subLoading"
          />
        </el-form-item>
      </el-form>
      <p v-if="subMsg" class="sub-dialog-msg" :class="{ ok: subOk }">{{ subMsg }}</p>
      <template #footer>
        <el-button @click="subDialogVisible = false" :disabled="subLoading">取消</el-button>
        <el-button type="primary" :loading="subLoading" @click="onSubscribe">{{ subLoading ? '提交中…' : '订阅' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, computed, watch } from 'vue'
import { home, subscribeEmail } from '@/api/front'
import { ElMessage } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import HeroAbout from '@/components/HeroAbout.vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const processedMd = ref('')

// 关于页正文：页面管理功能已下线，此处内联静态内容（保留 greeting/昵称/邮箱/github 动态替换）
const ABOUT_MD = `# 你好，我是站长

一个普通的工程师与写作者。
白天写代码，晚上偶尔写点别的。

## 在这里

- 记录日常开发里值得记下来的事
- 整理阅读时划线过的句子
- 偶尔发一些摄影与生活片段

## 怎么联系我

- Email：见页脚
- GitHub：见页脚

如果只是想聊聊天，欢迎在 [留言板](/guestbook) 留下几句话。`
const loading = ref(true)
const homeData = ref({})

// 站点配置
const siteEmail = computed(() => siteStore.info?.email || 'site_email@example.com')
const siteGithub = computed(() => siteStore.info?.github || 'https://github.com/')
const siteAvatar = computed(() => siteStore.info?.siteLogo || 'https://api.dicebear.com/7.x/notionists/svg?seed=author&backgroundColor=e0f2fe')
// About 页展示用的昵称 / 简介:跟随后端 admin 用户的资料,后端未填则用站点名做兜底
const authorName = computed(() => {
  const u = siteStore.info?.userNickname
  const a = siteStore.info?.authorName
  const s = siteStore.info?.siteName
  return u || a || s || '站长'
})
const bio = computed(() => siteStore.info?.userBio || siteStore.info?.motto || '一个在代码与文字之间来回切换的人。')
const githubDisplay = computed(() => {
  const u = siteGithub.value || ''
  return u.replace(/^https?:\/\//, '').replace(/\/$/, '') || 'github.com'
})

// 邮箱订阅（替代原 RSS 订阅）— 弹窗收集 + 后续展示
const subDialogVisible = ref(false)
const subLoading = ref(false)
const subMsg = ref('')
const subOk = ref(false)
const subFormRef = ref(null)
const subForm = reactive({ email: '' })

const subRules = {
  email: [
    {
      validator: (_rule, value, cb) => {
        if (!value) return cb(new Error('请输入邮箱'))
        if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)) return cb(new Error('邮箱格式不正确'))
        cb()
      },
      trigger: 'blur',
    },
  ],
}

function openSubDialog() {
  if (subDialogVisible.value) return
  subMsg.value = ''
  subOk.value = false
  subForm.email = ''
  subDialogVisible.value = true
}

async function onSubscribe() {
  // 触发表单校验后再提交
  let valid = false
  try {
    valid = await (subFormRef.value?.validate?.() ?? Promise.resolve(true))
  } catch (_) {
    valid = false
  }
  if (!valid) return

  const email = subForm.email
  subLoading.value = true
  subMsg.value = ''
  try {
    const res = await subscribeEmail({ email })
    const data = res?.data ?? {}
    // 后端如实反馈确认邮件是否真的发出：emailSent===false 时不应伪装成成功
    const sent = data.emailSent !== false
    subOk.value = sent
    subMsg.value = data.message || '订阅成功，请在邮箱内完成确认'
    if (sent) {
      ElMessage.success(data.message || '订阅成功，请查收邮箱中的确认链接')
    } else {
      ElMessage.warning(data.message || '确认邮件发送失败，请稍后重试')
    }
    // 1.4s 后关闭弹窗，保留 subMsg 静态展示不做重置（避免提前清空提示）
    await nextTick()
    setTimeout(() => {
      subDialogVisible.value = false
      subMsg.value = ''
      subOk.value = false
      subForm.email = ''
    }, 1400)
  } catch (e) {
    subOk.value = false
    subMsg.value = e?.response?.data?.message || e?.message || '订阅失败，请稍后重试'
  } finally {
    subLoading.value = false
  }
}

// 统计数据
const statsList = computed(() => {
  const s = homeData.value?.stats || {}
  return [
    { key: 'article', label: '文章', value: s.articleCount || 0 },
    { key: 'category', label: '分类', value: s.categoryCount || 0 },
    { key: 'tag', label: '标签', value: s.tagCount || 0 },
    { key: 'view', label: '阅读', value: s.viewCount || 0 }
  ]
})

// 技术标签：从站点配置读取（后台 Profile 页可编辑），逗号分隔
// 未配置时 fallback 到默认列表
const DEFAULT_SKILLS = [
  'Java', 'Spring Boot', 'Vue 3', 'TypeScript', 'MySQL', 'Redis',
  'MyBatis-Plus', 'Element Plus', 'Nginx', 'Docker', 'Git', 'Linux',
  'Kotlin', 'Rust', 'Python', 'Markdown', 'Figma', 'VS Code'
]
const skills = computed(() => {
  const raw = siteStore.info?.aboutSkills
  if (!raw || !raw.trim()) return DEFAULT_SKILLS
  return raw.split(/[,，]/).map(s => s.trim()).filter(Boolean)
})

// 时间线
const timeline = [
  {
    time: '2025 至今',
    title: '独立开发者 & 博客站长',
    desc: '搭建并维护个人博客系统，记录技术成长与生活感悟。从设计到部署，全栈独立开发。'
  },
  {
    time: '2024 - 2025',
    title: '软件工程师',
    desc: '深耕 Java 后端开发，参与多个 Spring Boot 项目。同时探索前端技术栈，独立开发 Vue 3 项目。'
  },
  {
    time: '2023 - 2024',
    title: '计算机专业学习',
    desc: '系统学习数据结构与算法、操作系统、计算机网络。开始接触 Java 与 Spring 生态。'
  },
  {
    time: '2022',
    title: '编程入门',
    desc: '写下第一行代码，从此踏上编程之路。从 C 语言到 Python，再到 Java，一步步探索。'
  }
]

onMounted(async () => {
  try {
    if (!siteStore.loaded) await siteStore.load()
    const homeResp = await home().catch(() => ({ data: {} }))
    const greeting = siteStore.info?.greeting
    // 用后端 greeting 动态替换静态正文里的硬编码问候语
    let mdStr = ABOUT_MD
    if (greeting) {
      mdStr = mdStr.replace(/^#\s*你好，我是[^\n]*/m, '# ' + greeting)
    }
    // "站长"字面统一替为 admin 昵称 + 邮箱/github 占位符替换
    mdStr = mdStr
      .replace(/站长/g, authorName.value)
      .replace(/you@example\.com/g, siteEmail.value)
      .replace(/admin@blog\.local/g, siteEmail.value)
      .replace(/\[EMAIL\]/g, siteEmail.value)
      .replace(/\[GITHUB_URL\]/g, siteGithub.value)
    processedMd.value = mdStr
    homeData.value = homeResp.data || {}
  } catch (_) {}
  loading.value = false
  initReveal()
})

function initReveal() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) { e.target.classList.add('visible'); observer.unobserve(e.target) }
    })
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' })
  document.querySelectorAll('.reveal').forEach(el => observer.observe(el))
}
</script>

<style scoped lang="scss">
.container-narrow { max-width: 880px; margin: 0 auto; padding: 0 24px; }

/* ② 个人资料卡片 */
.profile-card {
  background: var(--c-paper);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-pop);
  border: 1px solid var(--c-line-soft);
  padding: 0;
  position: relative;
  overflow: hidden;
}
/* 顶部渐变 banner */
.profile-card::before {
  content: '';
  position: absolute; top: 0; left: 0; right: 0; height: 100px;
  background:
    radial-gradient(ellipse 80% 100% at 20% 0%, rgba(56,189,248,0.15) 0%, transparent 60%),
    radial-gradient(ellipse 60% 100% at 85% 10%, rgba(251,191,36,0.10) 0%, transparent 55%),
    linear-gradient(135deg, rgba(56,189,248,0.06) 0%, rgba(125,211,252,0.04) 50%, rgba(251,191,36,0.05) 100%);
  z-index: 0;
}
/* 底部彩虹描边已移除——用户不需要 */
/* shimmer-bar 动画已移除（底部彩虹条已删） */

.profile-top {
  display: flex; gap: 24px; align-items: center; margin-bottom: 18px;
  position: relative; z-index: 1;
  padding: 20px 32px 0;   /* 顶部留出 banner 空间 */
}

.avatar-wrap {
  flex-shrink: 0; width: 104px; height: 104px;
  position: relative;
}
/* 头像外圈光晕环 */
.avatar-wrap::before {
  content: '';
  position: absolute; inset: -6px;
  border-radius: 50%;
  background: conic-gradient(
    from 180deg,
    rgba(56,189,248,0.35),
    rgba(125,211,252,0.20),
    rgba(251,191,36,0.30),
    rgba(56,189,248,0.35)
  );
  animation: ring-spin 8s linear infinite;
  opacity: 0.7;
}
.avatar-wrap::after {
  content: '';
  position: absolute; inset: -3px;
  border-radius: 50%;
  background: var(--c-paper);
  z-index: 1;
}
@keyframes ring-spin {
  to { transform: rotate(360deg); }
}
.avatar {
  width: 100%; height: 100%; border-radius: 50%; object-fit: cover;
  border: 4px solid var(--c-paper);
  box-shadow: 0 4px 24px rgba(14, 165, 233, 0.12), 0 0 0 3px var(--c-botany-100);
  position: relative; z-index: 2;
}

.profile-info { flex: 1; }
.name-row { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; flex-wrap: wrap; }
.name-row h2 {
  font-family: var(--font-serif); font-size: 24px; font-weight: 600; margin: 0; color: var(--c-ink);
}
.role-badge {
  padding: 4px 14px; background: linear-gradient(135deg, var(--c-botany-50), var(--c-autumn-50));
  color: var(--c-botany-700); border-radius: 999px; font-size: 13px;
  border: 1px solid var(--c-botany-100); font-weight: 500;
}
.bio { font-size: 15px; color: var(--c-ink-soft); line-height: 1.8; margin: 0 0 10px; max-width: 500px; }
.social-row { display: flex; gap: 8px; }

.soc-btn {
  width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center;
  background: var(--c-botany-50); border: 1px solid var(--c-botany-100);
  transition: all .3s ease;
  svg { width: 16px; height: 16px; fill: var(--c-botany-700); }
  &:hover {
    background: var(--c-botany-500); border-color: var(--c-botany-500);
    transform: translateY(-3px); box-shadow: 0 4px 12px rgba(14, 165, 233, .3);
    svg { fill: #fff; }
  }
}

/* 统计面板 */
.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
  padding: 18px 32px 22px;
  border-top: 1px solid var(--c-line-soft);
  position: relative; z-index: 1;
}
.stat-item { text-align: center; padding: 10px 6px; border-radius: var(--radius); transition: all .3s ease; }
.stat-item:hover { background: var(--c-botany-50); transform: translateY(-3px); }
.stat-icon {
  width: 42px; height: 42px; margin: 0 auto 8px; display: grid; place-items: center;
  border-radius: 10px; background: var(--c-botany-50); border: 1px solid var(--c-botany-100);
  color: var(--c-botany-700);
  transition: all .3s ease;
  svg { width: 20px; height: 20px; }
}
.stat-item:hover .stat-icon {
  background: var(--c-botany-500);
  border-color: var(--c-botany-500);
  color: #fff;
  box-shadow: 0 4px 14px rgba(56,189,248,0.25);
}
.stat-num {
  font-family: var(--font-serif); font-size: 26px; font-weight: 700;
  color: var(--c-botany-700); line-height: 1.2;
  /* 数字入场微动效 */
  opacity: 0; transform: translateY(8px);
  animation: stat-in 0.5s ease-out forwards;
  animation-delay: calc(var(--idx, 0) * 0.1s);
}
@keyframes stat-in {
  to { opacity: 1; transform: translateY(0); }
}
.stat-label { font-size: 13px; color: var(--c-ink-soft); margin-top: 4px; }

/* ③ 技能标签云 */
.sec-title {
  font-family: var(--font-serif); font-size: 20px; font-weight: 600; color: var(--c-ink-700);
  display: flex; align-items: center; justify-content: center; gap: 14px; margin: 0 0 18px;
}
.sec-line { width: 44px; height: 1px; background: linear-gradient(90deg, transparent, var(--c-botany-300), transparent); }

.skills-cloud { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
.skill-tag {
  padding: 7px 18px; background: var(--c-paper); border: 1px solid var(--c-line-soft);
  border-radius: 999px; font-size: 14px; color: var(--c-ink-500);
  transition: all .25s ease; cursor: default;
  &:hover {
    background: var(--c-botany-500); color: #fff; border-color: var(--c-botany-500);
    transform: translateY(-2px); box-shadow: 0 4px 12px rgba(14, 165, 233, .25);
  }
}

/* ④ 我的故事 */
.story-card {
  background: var(--c-paper); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft); border: 1px solid var(--c-line-soft);
  padding: 28px 32px; font-size: 15px; line-height: 1.9;
}

/* Markdown 深度样式 */
.story-card :deep(h1) { font-size: 26px; color: var(--c-botany-700); margin: 0 0 16px; }
.story-card :deep(h2) { font-size: 20px; color: var(--c-botany-700); margin: 24px 0 12px; border-bottom: none; }
.story-card :deep(h3) { font-size: 17px; color: var(--c-botany-700); margin: 20px 0 10px; }
.story-card :deep(p) { margin: 12px 0; color: var(--c-ink-500); }
.story-card :deep(ul) { padding-left: 24px; margin: 12px 0; }
.story-card :deep(li) { margin: 6px 0; color: var(--c-ink-500); }
.story-card :deep(a) { color: var(--c-botany-700); text-decoration: underline; text-decoration-color: var(--c-botany-300); }
.story-card :deep(blockquote) {
  border-left: 3px solid var(--c-botany-500); background: var(--c-botany-50);
  padding: 12px 18px; margin: 16px 0; border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--c-ink-700); font-style: italic;
}
.story-card :deep(code) {
  background: var(--c-botany-50); color: var(--c-botany-700); padding: 2px 8px;
  border-radius: 4px; font-size: 13px; border: 1px solid var(--c-botany-100);
}
.story-card :deep(pre) {
  background: var(--c-ink-50); border-radius: var(--radius-sm); padding: 16px;
  overflow-x: auto; border: 1px solid var(--c-line);
}

/* ⑤ 时间线 */
.timeline { position: relative; padding-left: 24px; }
.timeline::before {
  content: ''; position: absolute; left: 8px; top: 8px; bottom: 8px; width: 2px;
  background: linear-gradient(180deg, var(--c-botany-300), var(--c-botany-100), var(--c-botany-300));
  border-radius: 1px;
}
.tl-item { position: relative; padding-left: 36px; padding-bottom: 26px; }
.tl-item:last-child { padding-bottom: 0; }
.tl-dot {
  position: absolute; left: -17px; top: 6px; width: 18px; height: 18px; border-radius: 50%;
  background: var(--c-paper); border: 2px solid var(--c-botany-300); z-index: 1;
  transition: all .3s ease;
  &.active { border-color: var(--c-botany-500); background: var(--c-botany-500); box-shadow: 0 0 0 5px rgba(56, 189, 248, .18); }
}
.tl-item:hover .tl-dot { border-color: var(--c-botany-500); transform: scale(1.15); }
.tl-card { padding: 22px 24px; transition: transform .3s ease, box-shadow .3s ease; }
.tl-item:hover .tl-card { transform: translateX(4px); box-shadow: var(--shadow-pop); }
.tl-time { font-size: 13px; color: var(--c-autumn-700); font-weight: 500; margin-bottom: 4px; }
.tl-title { font-family: var(--font-serif); font-size: 18px; font-weight: 600; color: var(--c-ink); margin: 0 0 6px; }
.tl-desc { font-size: 14px; color: var(--c-ink-soft); line-height: 1.7; margin: 0; }

/* ⑥ 联系卡片 */
.contact-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.contact-item { text-align: center; padding: 30px 24px; transition: all .3s ease; }
.contact-item:hover { transform: translateY(-5px); box-shadow: var(--shadow-pop), var(--shadow-glow); }
.ci-icon {
  width: 54px; height: 54px; border-radius: 50%; display: grid; place-items: center;
  margin: 0 auto 14px; font-size: 17px; font-weight: 700; transition: transform .3s ease;
  svg { width: 22px; height: 22px; }
}
.contact-item:hover .ci-icon { transform: scale(1.08); }
.ci-email { background: linear-gradient(135deg, #e0f7ff, #bae6fd); color: #0369a1; }
.ci-github { background: linear-gradient(135deg, #f1f5f9, #e2e8f0); color: var(--c-ink-700); svg { fill: var(--c-ink-700); } }
.ci-mail { background: linear-gradient(135deg, #e0f7ff, #bae6fd); color: #0369a1; }
.contact-item h4 { font-family: var(--font-serif); font-size: 17px; font-weight: 600; margin: 0 0 4px; color: var(--c-ink); }
.contact-item p { font-size: 13px; color: var(--c-ink-soft); margin: 0 0 14px; }
.ci-link { font-size: 13px; color: var(--c-botany-700); font-weight: 500; }
.ci-link:hover { color: var(--c-botany-500); }

/* 邮箱订阅卡片：点击交互（与同级 ci-link 同色，与 card hover 兼容） */
.sub-trigger { cursor: pointer; outline: none; }
.sub-trigger:focus-visible {
  box-shadow: var(--shadow-pop), 0 0 0 3px rgba(56, 189, 248, 0.25);
}

/* 邮箱订阅弹窗（提示文案 + 行内状态） */
.sub-dialog-desc {
  font-size: 13px; line-height: 1.7; color: var(--c-ink-soft);
  margin: 0 0 14px;
}
.sub-dialog-msg {
  margin: 10px 0 0; font-size: 13px; color: #dc2626; line-height: 1.5;
}
.sub-dialog-msg.ok { color: #16a34a; }

/* dialog 顶部留出 fixed nav 安全距离，避免被遮 */
.sub-dialog :deep(.el-dialog) { margin-top: 84px; }
.sub-dialog :deep(.el-dialog__body) { padding-top: 4px; }

/* 滚动揭示 */
.reveal { opacity: 0; transform: translateY(30px); transition: opacity .7s ease, transform .7s cubic-bezier(.25,.8,.25,1); }
.reveal.visible { opacity: 1; transform: translateY(0); }

.loading-page { padding: 80px 24px; }
.section { padding: 6px 0 36px; }

@media (max-width: 900px) {
  .profile-top { flex-direction: column; text-align: center; }
  .avatar-wrap { width: 90px; height: 90px; }
  .name-row { justify-content: center; }
  .social-row { justify-content: center; }
  .stats-row { grid-template-columns: repeat(2, 1fr); padding: 16px 20px 18px; }
  .contact-grid { grid-template-columns: 1fr; }
  .profile-card .profile-top { padding: 16px 20px 0; }
  .stats-row { padding: 14px 20px 18px; }
  .story-card { padding: 24px; }
}
@media (max-width: 768px) {
  .contact-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .name-row h2 { font-size: 24px; }
  .timeline { padding-left: 16px; }
  .tl-item { padding-left: 28px; }
  .tl-dot { left: -14px; width: 14px; height: 14px; }
  .sec-title { font-size: 17px; }
  .sec-line { width: 32px; }
  .story-card { padding: 20px 16px; }
}
@media (max-width: 480px) {
  .timeline { padding-left: 10px; }
  .tl-item { padding-left: 22px; padding-bottom: 20px; }
  .tl-dot { left: -11px; width: 12px; height: 12px; top: 5px; }
  .tl-card { padding: 14px; }
}
</style>
