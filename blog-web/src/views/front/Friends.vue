<template>
  <div class="friends-page">
    <HeroFriends title="友链" subtitle="志同道合的朋友们 · 交换友链请留言" />

    <div class="container-narrow page-body">
      <div v-if="loading" class="loading-tip">加载中…</div>
      <div v-else-if="!published.length" class="empty-tip">暂无友链</div>

      <div v-else class="friend-groups">
        <!-- 顶部召唤卡（晴空 v2 渐变描边） -->
        <GradientBorderCard variant="mix" class="friends-hero reveal">
          <div class="fh-left">
            <span class="section-eyebrow">friends wanted</span>
            <h2 class="fh-title">欢迎互换友链 ✦</h2>
            <p class="fh-desc">如果你的博客也写点代码、记点生活，欢迎留言交换。</p>
          </div>
          <div class="fh-right">
            <span class="fh-stat-num">{{ published.length }}</span>
            <span class="fh-stat-label">位朋友</span>
            <button class="fh-apply-btn" @click="openApplyDialog">
              <el-icon><Link /></el-icon>
              <span>申请友链</span>
            </button>
          </div>
        </GradientBorderCard>

        <section v-for="(g, gi) in groups" :key="g.name" class="fg-section reveal" :style="{ transitionDelay: `${gi * 80}ms` }">
          <h3 class="fg-title">
            <span class="fg-icon">{{ groupIcon(g.name) }}</span>
            <span>{{ g.name }}</span>
            <span class="fg-count">({{ g.items.length }})</span>
          </h3>
          <div class="friend-grid">
            <a
              v-for="(f, fi) in g.items"
              :key="f.id"
              :href="f.url"
              target="_blank"
              class="friend-card-wrap reveal"
              :style="{ transitionDelay: `${(gi * 80) + (fi * 50)}ms` }"
            >
              <GradientBorderCard
                :variant="variantFor(g.name, fi)"
                hoverable
                class="friend-card"
              >
                <div class="fc-top">
                  <img v-if="f.avatar" :src="f.avatar" class="avatar avatar-img" :alt="f.name" />
                  <div v-else class="avatar avatar-placeholder">{{ (f.name || '?')[0] }}</div>
                  <div class="meta">
                    <h4>{{ f.name }}</h4>
                    <p class="url">{{ friendlyUrl(f.url) }}</p>
                  </div>
                </div>
                <p v-if="f.description" class="desc">{{ f.description }}</p>
                <div class="fc-arrow">→</div>
              </GradientBorderCard>
            </a>
          </div>
        </section>
      </div>
    </div>

    <!-- 申请友链弹窗 -->
    <el-dialog
      v-model="applyDialogVisible"
      title="申请友链"
      width="520px"
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
          <el-input v-model="applyForm.name" placeholder="你的博客名称" maxlength="50" />
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
            placeholder="简单介绍一下你的博客（最多 200 字）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="email">
          <el-input v-model="applyForm.email" placeholder="可选，审核结果将发送到此邮箱" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Link } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { friendLinks as fetchFriendLinks, applyFriendLink } from '@/api/front'
import HeroFriends from '@/components/HeroFriends.vue'
import GradientBorderCard from '@/components/GradientBorderCard.vue'

const published = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { published.value = (await fetchFriendLinks()).data || [] } catch (_) {}
  loading.value = false
})

const groups = computed(() => {
  const map = {}
  published.value.forEach(f => {
    const g = f.linkGroup || '网友'
    if (!map[g]) map[g] = { name: g, items: [] }
    map[g].items.push(f)
  })
  const order = ['好友', '网友']
  const result = []
  order.forEach(k => { if (map[k]) result.push(map[k]) })
  Object.keys(map).forEach(k => { if (!order.includes(k)) result.push(map[k]) })
  return result
})

const groupIcon = (name) => {
  if (name === '好友') return '♥'
  if (name === '网友') return '🌐'
  return '✦'
}

// 渐变描边卡变体：好友组用 sun，其它组用 sky/mix 交替
const variantFor = (groupName, idx) => {
  if (groupName === '好友') return 'sun'
  return idx % 2 === 0 ? 'sky' : 'mix'
}

const friendlyUrl = (url) => {
  if (!url) return ''
  return url.replace(/^https?:\/\//, '').replace(/\/$/, '')
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
    { max: 200, message: '简介不能超过 200 个字符', trigger: 'blur' }
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
    ElMessage.success('友链申请已提交，审核通过后将展示在友链页面')
    applyDialogVisible.value = false
  } catch (e) {
    // 错误已在拦截器中统一处理
  } finally {
    applying.value = false
  }
}
</script>

<style scoped lang="scss">
.page-body { padding: 30px 0 60px; }
.loading-tip, .empty-tip {
  text-align: center;
  color: var(--c-ink-soft);
  padding: 60px 0;
}

.friend-groups {
  display: flex;
  flex-direction: column;
  gap: 36px;
}

/* Hero 卡 */
.friends-hero {
  padding: 28px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  background: linear-gradient(135deg, #f0f9ff 0%, #fffbeb 100%);
}
.fh-left { flex: 1; min-width: 220px; }
.section-eyebrow {
  display: block;
  font-size: 11px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #06b6d4;
  font-weight: 600;
  margin-bottom: 8px;
}
.fh-title {
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 6px;
  background: linear-gradient(135deg, #0369a1 0%, #38bdf8 60%, #f59e0b 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.fh-desc {
  font-size: 14px;
  color: var(--c-ink-soft);
  margin: 0;
}
.fh-right {
  text-align: right;
}
.fh-stat-num {
  display: block;
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, #38bdf8, #fbbf24);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.fh-stat-label {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 4px;
  margin-bottom: 14px;
  display: block;
}

/* 申请友链按钮 */
.fh-apply-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  background: linear-gradient(135deg, #38bdf8, #0ea5e9);
  border: none;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(14, 165, 233, 0.3);
  transition: all 0.3s ease;
}
.fh-apply-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.4);
}
.fh-apply-btn:active {
  transform: translateY(0);
}

/* 申请弹窗 */
:deep(.apply-dialog) {
  border-radius: 14px;
}
:deep(.apply-dialog .el-dialog__header) {
  padding: 22px 28px 0;
}
:deep(.apply-dialog .el-dialog__title) {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--c-ink);
}
:deep(.apply-dialog .el-dialog__body) {
  padding: 16px 28px 0;
}
:deep(.apply-dialog .el-dialog__footer) {
  padding: 16px 28px 24px;
}

/* 分组 */
.fg-section { margin-top: 4px; }
.fg-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-serif);
  font-size: 22px;
  margin: 0 0 22px;
  color: var(--c-ink);
  position: relative;
  padding-left: 14px;
}
.fg-title::before {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 4px; height: 22px;
  background: linear-gradient(180deg, #38bdf8, #fbbf24);
  border-radius: 2px;
}
.fg-icon {
  font-size: 18px;
  color: #fbbf24;
}
.fg-count {
  color: var(--c-ink-soft);
  font-size: 14px;
  font-weight: 400;
}

/* 友链网格 */
.friend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.friend-card-wrap {
  display: block;
  text-decoration: none;
}
.friend-card {
  padding: 18px 20px 16px;
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.fc-top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar {
  width: 48px; height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.15);
}
.avatar-img { object-fit: cover; }
.avatar-placeholder {
  display: grid; place-items: center;
  background: linear-gradient(135deg, #38bdf8, #22d3ee);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
}
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
}
.friend-card:hover .meta h4 { color: #0369a1; }
.friend-card .meta .url {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.desc {
  font-size: 13px;
  color: var(--c-ink-soft);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fc-arrow {
  align-self: flex-end;
  color: #94a3b8;
  font-size: 16px;
  transition: all 0.25s ease;
}
.friend-card:hover .fc-arrow {
  color: #0ea5e9;
  transform: translateX(4px);
}

/* reveal */
.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible { opacity: 1; transform: translateY(0); }

@media (max-width: 1024px) {
  .friend-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .friend-grid { grid-template-columns: 1fr; }
  .fh-right { text-align: left; }
}
@media (max-width: 480px) {
  :deep(.apply-dialog) { width: 92vw !important; max-width: 380px; }
  .friend-item { padding: 18px 16px; }
}
</style>