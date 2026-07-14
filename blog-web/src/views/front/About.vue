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
            <div class="stat-item" v-for="s in statsList" :key="s.label">
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
      <section class="section container-narrow reveal" v-if="rendered">
        <h3 class="sec-title"><span class="sec-line"></span>我的故事<span class="sec-line"></span></h3>
        <div class="story-card markdown-body" v-html="rendered"></div>
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
          <div class="contact-item card">
            <div class="ci-icon ci-rss">RSS</div>
            <h4>订阅</h4>
            <p>追踪最新文章</p>
            <a href="#" class="ci-link">订阅 RSS &rarr;</a>
          </div>
        </div>
      </section>
    </div>

    <div v-else class="loading-page container-narrow">
      <el-skeleton :rows="8" animated />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { pageBySlug, home } from '@/api/front'
import { renderMarkdown } from '@/utils/markdown'
import HeroAbout from '@/components/HeroAbout.vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const page = ref(null)
const rendered = ref('')
const loading = ref(true)
const homeData = ref({})

// 站点配置
const siteEmail = computed(() => siteStore.info?.email || 'author@example.com')
const siteGithub = computed(() => siteStore.info?.github || 'https://github.com/DemoAuthor')
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

// 技能标签
const skills = [
  'Java', 'Spring Boot', 'Vue 3', 'TypeScript', 'MySQL', 'Redis',
  'MyBatis-Plus', 'Element Plus', 'Nginx', 'Docker', 'Git', 'Linux',
  'Kotlin', 'Rust', 'Python', 'Markdown', 'Figma', 'VS Code'
]

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
    const [pageResp, homeResp] = await Promise.all([
      pageBySlug('about'),
      home().catch(() => ({ data: {} }))
    ])
    page.value = pageResp.data
    const rawMd = pageResp.data?.contentMd || pageResp.data?.content || ''
    const greeting = siteStore.info?.greeting
    // 用后端 greeting 动态替换 CMS 页面中的硬编码问候语
    let processedMd = rawMd
    if (greeting) {
      processedMd = processedMd.replace(/^#\s*你好，我是[^\n]*/m, '# ' + greeting)
    }
    // 其余"站长"字面统一替为 admin 昵称
    processedMd = processedMd.replace(/站长/g, authorName.value)
    rendered.value = renderMarkdown(processedMd)
      .replace(/you@example\.com/g, siteEmail.value)
      .replace(/admin@blog\.local/g, siteEmail.value)
      .replace(/\[EMAIL\]/g, siteEmail.value)
      .replace(/\[GITHUB_URL\]/g, siteGithub.value)
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
  padding: 44px 44px 32px;
  position: relative;
  overflow: hidden;
}
.profile-card::before {
  content: '';
  position: absolute; top: 0; left: 0; right: 0; height: 4px;
  background: linear-gradient(90deg, var(--c-botany-500), var(--c-autumn-500), var(--c-botany-300));
  background-size: 200% 100%;
  animation: shimmer-bar 3s ease-in-out infinite;
}
@keyframes shimmer-bar {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.profile-top {
  display: flex; gap: 36px; align-items: center; margin-bottom: 30px;
}

.avatar-wrap { flex-shrink: 0; width: 130px; height: 130px; }
.avatar {
  width: 100%; height: 100%; border-radius: 50%; object-fit: cover;
  border: 4px solid var(--c-paper);
  box-shadow: 0 4px 24px rgba(14, 165, 233, 0.12), 0 0 0 3px var(--c-botany-100);
}

.profile-info { flex: 1; }
.name-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.name-row h2 {
  font-family: var(--font-serif); font-size: 30px; font-weight: 600; margin: 0; color: var(--c-ink);
}
.role-badge {
  padding: 4px 14px; background: linear-gradient(135deg, var(--c-botany-50), var(--c-autumn-50));
  color: var(--c-botany-700); border-radius: 999px; font-size: 13px;
  border: 1px solid var(--c-botany-100); font-weight: 500;
}
.bio { font-size: 15px; color: var(--c-ink-soft); line-height: 1.8; margin: 0 0 14px; max-width: 500px; }
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
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  padding-top: 24px; border-top: 1px solid var(--c-line-soft);
}
.stat-item { text-align: center; padding: 14px 8px; border-radius: var(--radius); transition: all .3s ease; }
.stat-item:hover { background: var(--c-botany-50); transform: translateY(-3px); }
.stat-icon {
  width: 42px; height: 42px; margin: 0 auto 8px; display: grid; place-items: center;
  border-radius: 10px; background: var(--c-botany-50); border: 1px solid var(--c-botany-100);
  color: var(--c-botany-700);
  svg { width: 20px; height: 20px; }
}
.stat-num { font-family: var(--font-serif); font-size: 26px; font-weight: 600; color: var(--c-botany-700); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--c-ink-soft); margin-top: 4px; }

/* ③ 技能标签云 */
.sec-title {
  font-family: var(--font-serif); font-size: 20px; font-weight: 600; color: var(--c-ink-700);
  display: flex; align-items: center; justify-content: center; gap: 14px; margin: 0 0 24px;
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
  padding: 36px 40px; font-size: 15px; line-height: 1.9;
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
.ci-rss { background: linear-gradient(135deg, #e0f7ff, #fbbf24); color: #0369a1; }
.contact-item h4 { font-family: var(--font-serif); font-size: 17px; font-weight: 600; margin: 0 0 4px; color: var(--c-ink); }
.contact-item p { font-size: 13px; color: var(--c-ink-soft); margin: 0 0 14px; }
.ci-link { font-size: 13px; color: var(--c-botany-700); font-weight: 500; }
.ci-link:hover { color: var(--c-botany-500); }

/* 滚动揭示 */
.reveal { opacity: 0; transform: translateY(30px); transition: opacity .7s ease, transform .7s cubic-bezier(.25,.8,.25,1); }
.reveal.visible { opacity: 1; transform: translateY(0); }

.loading-page { padding: 80px 24px; }
.section { padding: 16px 0 50px; }

@media (max-width: 900px) {
  .profile-top { flex-direction: column; text-align: center; }
  .avatar-wrap { width: 100px; height: 100px; }
  .name-row { justify-content: center; }
  .social-row { justify-content: center; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .contact-grid { grid-template-columns: 1fr; }
  .profile-card { padding: 32px 24px 28px; }
  .story-card { padding: 24px; }
}
@media (max-width: 600px) {
  .name-row h2 { font-size: 24px; }
  .timeline { padding-left: 16px; }
  .tl-item { padding-left: 28px; }
  .tl-dot { left: -14px; width: 14px; height: 14px; }
  .sec-title { font-size: 17px; }
  .sec-line { width: 32px; }
}
</style>
