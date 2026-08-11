<template>
  <footer class="app-footer">
    <div class="container footer-inner">
      <div class="footer-col brand-col">
        <div class="f-brand">{{ siteName }}</div>
        <p class="f-motto">{{ siteMotto }}</p>
        <p v-if="siteDesc" class="f-desc">{{ siteDesc }}</p>
      </div>
      <div class="footer-col">
        <h4>导航</h4>
        <router-link to="/">首页</router-link>
        <router-link to="/articles">文章</router-link>
        <router-link to="/projects">项目</router-link>
        <router-link to="/friends">友链</router-link>
        <router-link to="/guestbook">留言</router-link>
        <router-link to="/about">关于</router-link>
      </div>
      <div class="footer-col">
        <h4>关注</h4>
        <a :href="githubUrl || 'javascript:;'" target="_blank">GitHub</a>
        <a href="javascript:;">微信公众号</a>
        <form class="f-sub" @submit.prevent="onSubscribe">
          <input
            v-model.trim="subEmail"
            type="email"
            inputmode="email"
            placeholder="输入邮箱订阅更新"
            :disabled="subLoading"
            aria-label="邮箱订阅"
          />
          <button type="submit" :disabled="subLoading || !subEmail">{{ subLoading ? '…' : '订阅' }}</button>
        </form>
        <p v-if="subMsg" class="f-sub-msg" :class="{ ok: subOk }">{{ subMsg }}</p>

        <el-dialog
          v-model="subDialogVisible"
          class="sub-dialog"
          width="420px"
          align-center
          :close-on-click-modal="true"
          :show-close="true"
        >
          <template #header>
            <span class="sub-dlg-title">订阅成功</span>
          </template>
          <div class="sub-dlg-body">
            <div class="sub-dlg-icon">✉</div>
            <p class="sub-dlg-text">确认邮件已重新发送，请查收邮箱完成订阅</p>
          </div>
          <template #footer>
            <el-button type="primary" @click="subDialogVisible = false">我知道了</el-button>
          </template>
        </el-dialog>
      </div>
      <div class="footer-col">
        <h4>友链</h4>
        <a v-if="friendLinks.length === 0" href="javascript:;" class="f-empty">暂无友链,去申请 →</a>
        <template v-else>
          <a
            v-for="link in friendLinksTop6"
            :key="link.id"
            :href="link.url"
            target="_blank"
            rel="noopener"
          >{{ link.name }}</a>
          <router-link v-if="hasMoreFriends" to="/friends" class="f-more" title="查看全部友链">......</router-link>
        </template>
      </div>
    </div>
    <div class="footer-bottom">
      <div class="container">
        <span>© {{ year }} {{ siteName }} · Powered by Spring Boot & Vue 3</span>
        <span v-if="siteBeian" class="icp">{{ siteBeian }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useSiteStore } from '@/stores/site'
import { friendLinks as fetchFriendLinks, subscribeEmail } from '@/api/front'
import { ElMessage } from 'element-plus'

const siteStore = useSiteStore()
const siteName = computed(() => siteStore.info?.siteName || 'MyBlog')
const siteMotto = computed(() => siteStore.info?.motto || '草木蔓发，春山可望')
const siteDesc = computed(() => siteStore.info?.description || '')
const siteBeian = computed(() => siteStore.info?.beian || '')
const githubUrl = computed(() => siteStore.info?.github || '')
const year = new Date().getFullYear()

const friendLinks = ref([])
onMounted(async () => {
  try {
    const res = await fetchFriendLinks()
    friendLinks.value = res?.data ?? res ?? []
  } catch {
    friendLinks.value = []
  }
})

// 页脚"友链"列：超过 6 个时只露前 6 个，其余用 "......" 代替并跳到 /friends 全部页
const friendLinksTop6 = computed(() => friendLinks.value.slice(0, 6))
const hasMoreFriends = computed(() => friendLinks.value.length > 6)

// 邮箱订阅（替代原 RSS 订阅）
const subEmail = ref('')
const subLoading = ref(false)
const subMsg = ref('')
const subOk = ref(false)
const subDialogVisible = ref(false)

async function onSubscribe() {
  const email = subEmail.value
  if (!email) {
    subMsg.value = '请输入邮箱'
    subOk.value = false
    return
  }
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
    subMsg.value = '邮箱格式不正确'
    subOk.value = false
    return
  }
  subLoading.value = true
  subMsg.value = ''
  try {
    const res = await subscribeEmail({ email })
    const data = res?.data ?? {}
    // 后端如实反馈确认邮件是否真的发出：emailSent===false 时不应伪装成成功
    const sent = data.emailSent !== false
    if (sent) {
      subEmail.value = ''
      subDialogVisible.value = true
    } else {
      subOk.value = false
      subMsg.value = data.message || '确认邮件发送失败，请稍后重试'
      ElMessage.warning(data.message || '确认邮件发送失败，请稍后重试')
    }
  } catch (e) {
    subOk.value = false
    subMsg.value = e?.response?.data?.message || e?.message || '订阅失败，请稍后重试'
  } finally {
    subLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.app-footer {
  background: linear-gradient(135deg, #0c4a6e 0%, #075985 100%);
  color: #e0f2fe;
  margin-top: 80px;
  position: relative;
  overflow: hidden;
}
.app-footer::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
}
.footer-inner {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr;
  gap: 40px;
  padding: 64px 24px 40px;
}
.footer-col h4 {
  font-family: var(--font-serif);
  color: #fff;
  font-size: 16px;
  margin: 0 0 18px;
}
.footer-col a {
  display: block;
  margin: 8px 0;
  color: #93c5fd;
  font-size: 14px;
  transition: color 0.2s;
}
.footer-col a:hover { color: var(--c-autumn-300); }

/* 邮箱订阅表单（替代原 RSS 订阅） */
.f-sub {
  display: flex;
  gap: 6px;
  margin: 12px 0 4px;
}
.f-sub input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border: 1px solid rgba(147, 197, 253, 0.4);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #e0f2fe;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s, background 0.2s;
}
.f-sub input::placeholder { color: #7dd3fc; opacity: 0.7; }
.f-sub input:focus {
  border-color: var(--c-autumn-300);
  background: rgba(255, 255, 255, 0.14);
}
.f-sub button {
  flex: 0 0 auto;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: var(--c-autumn-300);
  color: #0c4a6e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.2s, opacity 0.2s;
}
.f-sub button:hover:not(:disabled) { filter: brightness(1.08); }
.f-sub button:disabled { opacity: 0.5; cursor: not-allowed; }
.f-sub-msg {
  margin: 2px 0 0;
  font-size: 12px;
  color: #fca5a5;
  line-height: 1.5;
}
.f-sub-msg.ok { color: #86efac; }

.f-brand {
  font-family: var(--font-serif);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}
.f-motto { color: var(--c-autumn-300); font-size: 14px; margin: 0 0 14px; }
.f-desc { color: #bae6fd; font-size: 13px; line-height: 1.7; margin: 0; }

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 22px 0;
  font-size: 13px;
  color: #7dd3fc;
}
.footer-bottom .container {
  display: flex;
  justify-content: space-between;
}

@media (max-width: 768px) {
  .footer-inner {
    grid-template-columns: 1fr 1fr;
    padding: 40px 24px 24px;
  }
}
@media (max-width: 480px) {
  .footer-inner { grid-template-columns: 1fr; padding: 36px 16px 20px; gap: 24px; }
  .footer-bottom { padding: 18px 0; }
  .footer-bottom .container { flex-direction: column; gap: 6px; text-align: center; }
  .f-brand { font-size: 20px; }
}
</style>

<!-- 弹窗被 portal 到 body，scoped 样式无法命中，故此处用非 scoped 块并限定 .sub-dialog 类 -->
<style lang="scss">
.sub-dialog {
  max-width: 90vw;
  border-radius: 16px;
  overflow: hidden;
  .el-dialog__header {
    padding: 22px 22px 0;
  }
  .sub-dlg-title {
    font-family: var(--font-serif);
    font-size: 18px;
    font-weight: 600;
    color: #0f172a;
  }
  .sub-dlg-body {
    text-align: center;
    padding: 8px 8px 4px;
  }
  .sub-dlg-icon {
    width: 52px;
    height: 52px;
    margin: 4px auto 16px;
    border-radius: 50%;
    background: rgba(56, 189, 248, 0.14);
    color: #38bdf8;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    line-height: 1;
  }
  .sub-dlg-text {
    margin: 0;
    font-size: 15px;
    line-height: 1.7;
    color: #334155;
  }
  .el-dialog__footer {
    text-align: center;
    padding: 0 0 22px;
  }
}
</style>