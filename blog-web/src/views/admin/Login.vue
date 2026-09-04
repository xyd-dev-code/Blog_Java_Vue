<template>
  <div class="login-page">
    <div class="bg-shape s1"></div>
    <div class="bg-shape s2"></div>
    <div class="login-card">
      <div class="login-brand">
        <img v-if="siteLogo" :src="siteLogo" class="brand-img" alt="logo" />
        <span v-else class="mark">{{ brandInitial }}</span>
        <h1>{{ siteTitle }}</h1>
        <p>{{ wx('欢迎回来') }}</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
            prefix-icon="Lock" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'

const route = useRoute()
const userStore = useUserStore()
const siteStore = useSiteStore()
const formRef = ref(null)
const loading = ref(false)

const siteTitle = computed(() => {
  const name = siteStore.info?.siteName?.trim() || ''
  return name ? `${name} · ${wx('后台管理')}` : wx('后台管理')
})
const siteLogo = computed(() => siteStore.info?.siteLogo || '')
const brandInitial = computed(() => {
  const name = siteStore.info?.siteName || 'Blog'
  return name[0].toUpperCase()
})

onMounted(() => {
  siteStore.load()
})

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const safeAdminRedirect = (value) => {
  if (typeof value !== 'string') return '/admin/dashboard'
  const isAdminPath = value === '/admin' || value.startsWith('/admin/') || value.startsWith('/admin?')
  return isAdminPath && !value.startsWith('//')
    ? value
    : '/admin/dashboard'
}

const submit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    await userStore.doLogin(form)
    ElMessage.success('登录成功')
    const target = safeAdminRedirect(route.query.redirect)
    // 强制整页跳转，避免 SPA 路由 + 守卫 + HMR 之间状态错位
    setTimeout(() => { window.location.assign(target) }, 300)
  } catch (_) {} finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--c-primary-mist) 0%, var(--c-botany-50) 50%, var(--c-white) 100%);
}
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: drift 10s ease-in-out infinite;
}
.s1 {
  width: 360px; height: 360px;
  background: rgba(var(--theme-primary-rgb), 0.35);
  top: -120px; left: -100px;
  animation-delay: 0s;
}
.s2 {
  width: 300px; height: 300px;
  background: rgba(var(--theme-cyan-rgb), 0.3);
  bottom: -80px; right: -80px;
  animation-delay: -5s;
}
@keyframes drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}
.login-card {
  position: relative;
  z-index: 1;
  background: rgba(var(--theme-paper-rgb), 0.92);
  border-radius: 16px;
  padding: 40px 36px;
  width: 380px;
  box-shadow: 0 20px 50px rgba(var(--theme-primary-strong-rgb), 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid var(--c-line-soft);
}
@media (max-width: 480px) {
  .login-card { width: 92vw; max-width: 380px; padding: 28px 22px; }
  .login-page { padding: 16px; }
}
.login-brand {
  text-align: center;
  margin-bottom: 28px;
}
.login-brand .mark {
  width: 56px; height: 56px;
  border-radius: 14px;
  display: inline-grid;
  place-items: center;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-autumn-500));
  color: var(--theme-on-primary);
  font-family: var(--font-serif);
  font-size: 28px;
  margin-bottom: 12px;
}
.login-brand .brand-img {
  width: 56px; height: 56px;
  border-radius: 14px;
  object-fit: cover;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(var(--theme-black-rgb), 0.08);
}
.login-brand h1 {
  font-family: var(--font-serif);
  font-size: 22px;
  margin: 0 0 4px;
}
.login-brand p { color: var(--c-ink-soft); font-size: 14px; margin: 0; }

.login-btn {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  font-size: 15px;
}
</style>
