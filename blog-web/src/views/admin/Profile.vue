<template>
  <div class="profile-page">
    <!-- 加载失败时顶部 banner + 重试,避免 onMounted 静默吞错导致页面看上去"无数据" -->
    <div v-if="loadError" class="load-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ loadError }}</span>
      <el-button type="primary" size="small" :loading="profileLoading || siteLoading" @click="loadAll">重新加载</el-button>
    </div>
    <div class="profile-grid">
      <!-- 左侧：个人资料 -->
      <el-card class="profile-card" v-loading="profileLoading">
        <template #header>
          <div class="card-header">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </div>
        </template>

        <div class="avatar-section">
          <div class="avatar-upload" @click="triggerUpload">
            <el-avatar :size="88" :src="profileForm.avatar" class="profile-avatar">
              {{ profileForm.nickname?.[0] || 'A' }}
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon><Camera /></el-icon>
              <span>更换头像</span>
            </div>
          </div>
          <input ref="uploadInput" type="file" accept="image/*" style="display:none" @change="onFileSelected" />
        </div>

        <el-form :model="profileForm" label-width="80px" class="profile-form">
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" placeholder="你的昵称" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" type="email" inputmode="email" placeholder="联系邮箱" />
          </el-form-item>
          <el-form-item label="用户名">
            <span class="form-static">{{ profileForm.username || '-' }}</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
            <el-button type="warning" plain @click="pwdVisible = true">
              <el-icon><Lock /></el-icon> 修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右侧：站点信息 -->
      <div class="profile-right">
        <el-card class="profile-card" v-loading="siteLoading">
          <template #header>
            <div class="card-header"><el-icon><Setting /></el-icon><span>站点信息</span></div>
          </template>
          <el-form :model="siteForm" label-width="90px">
            <el-row :gutter="16">
              <el-col :md="12">
                <el-form-item label="站点名称"><el-input v-model="siteForm.siteName" placeholder="MyBlog" /></el-form-item>
              </el-col>
              <el-col :md="12">
                <el-form-item label="副标题"><el-input v-model="siteForm.motto" placeholder="草木蔓发，春山可望" /></el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="站点描述">
              <el-input v-model="siteForm.description" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="SEO 关键词">
              <el-input v-model="siteForm.keywords" placeholder="个人博客,技术,生活" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :md="12">
                <el-form-item label="联系邮箱"><el-input v-model="siteForm.email" type="email" inputmode="email" /></el-form-item>
              </el-col>
              <el-col :md="12">
                <el-form-item label="ICP 备案号"><el-input v-model="siteForm.beian" /></el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :md="12">
                <el-form-item label="GitHub"><el-input v-model="siteForm.github" /></el-form-item>
              </el-col>
              <el-col :md="12">
                <el-form-item label="评论审核">
                  <el-switch v-model="siteForm.comment_audit" active-value="1" inactive-value="0"
                    active-text="需审核" inactive-text="免审核" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="技术栈标签">
              <el-input v-model="siteForm.aboutSkills" type="textarea" :rows="2"
                placeholder="用逗号分隔，如：Java, Spring Boot, Vue 3, TypeScript" />
              <div class="form-tip">显示在「关于我」页面的技术栈区域，留空则使用默认列表</div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="siteSaving" @click="saveSite">保存站点信息</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>

    <!-- 裁剪弹窗 -->
    <el-dialog v-model="cropVisible" title="裁剪头像" width="min(560px, 92vw)" :close-on-click-modal="false" destroy-on-close>
      <div class="crop-container" v-if="cropVisible">
        <Cropper
          ref="cropperRef"
          :src="cropSrc"
          :stencil-props="{ aspectRatio: 1 }"
          :default-boundaries="'fit'"
          image-restriction="stencil"
          class="cropper-instance"
        />
      </div>
      <template #footer>
        <el-button @click="cropVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doCrop">确认裁剪</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="min(420px, 92vw)" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="输入旧密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="warning" :loading="pwdSaving" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Setting, Camera, WarningFilled } from '@element-plus/icons-vue'
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import { useSiteStore } from '@/stores/site'
import { useUserStore } from '@/stores/user'
import {
  adminProfile, adminUpdateProfile, adminChangePassword,
  adminSiteConfig, adminSaveSiteConfig, adminUpload, adminDeleteUpload
} from '@/api/admin'

const siteStore = useSiteStore()
const userStore = useUserStore()

const profileSaving = ref(false)
const pwdSaving = ref(false)
const pwdVisible = ref(false)
const siteSaving = ref(false)
const uploading = ref(false)
const profileLoading = ref(false)
const siteLoading = ref(false)
const loadError = ref('')  // 任一加载失败时显示顶部 banner，并提供手动重试

const uploadInput = ref(null)
const cropperRef = ref(null)
const cropVisible = ref(false)
const cropSrc = ref('')
const cropSrcMime = ref('image/jpeg')

// 记录旧头像 URL，保存后删除
const oldAvatar = ref('')

const triggerUpload = () => uploadInput.value?.click()

const profileForm = reactive({
  avatar: '', nickname: '', email: '', username: ''
})

const pwdForm = reactive({
  oldPassword: '', newPassword: '', confirmPassword: ''
})

const siteForm = reactive({
  siteName: '', motto: '', description: '', keywords: '',
  beian: '', comment_audit: '1', github: '', email: '',
  aboutSkills: ''
})

// ---- 文件选择 → 裁剪 ----

const onFileSelected = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  cropSrcMime.value = file.type || 'image/jpeg'
  cropSrc.value = URL.createObjectURL(file)
  cropVisible.value = true
  uploadInput.value.value = ''
}

// ---- 裁剪并上传 ----

// 将裁剪后的 canvas 缩放为合适尺寸并压缩输出。PNG 输入保留 PNG(透明不丢),其他格式走 JPEG 节省体积。
const canvasToFile = (sourceCanvas, sourceMime, maxSize = 512, quality = 0.92) => {
  return new Promise((resolve, reject) => {
    const { width: sw, height: sh } = sourceCanvas
    let dw = sw
    let dh = sh
    if (sw > sh) {
      if (sw > maxSize) { dh = Math.round(sh * maxSize / sw); dw = maxSize }
    } else {
      if (sh > maxSize) { dw = Math.round(sw * maxSize / sh); dh = maxSize }
    }
    const canvas = document.createElement('canvas')
    canvas.width = dw
    canvas.height = dh
    const ctx = canvas.getContext('2d')
    const isPng = sourceMime === 'image/png'
    if (!isPng) {
      // 仅 JPEG 走白底(避免透明填黑色)
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, dw, dh)
    }
    ctx.drawImage(sourceCanvas, 0, 0, dw, dh)

    const outMime = isPng ? 'image/png' : 'image/jpeg'
    const outName = isPng ? 'avatar.png' : 'avatar.jpg'
    canvas.toBlob((blob) => {
      if (!blob) { reject(new Error('无法生成裁剪图片')); return }
      resolve(new File([blob], outName, { type: outMime }))
    }, outMime, isPng ? undefined : quality)
  })
}

const doCrop = async () => {
  if (!cropperRef.value) return
  uploading.value = true
  try {
    const { canvas } = cropperRef.value.getResult()
    if (!canvas || canvas.width === 0 || canvas.height === 0) {
      throw new Error('裁剪区域无效，请重新选择图片')
    }
    // 缩放到最大 512px 并压缩,防止原图过大导致网络/服务端拒绝;PNG 原样保留(透明不丢)
    const file = await canvasToFile(canvas, cropSrcMime.value, 512, 0.92)
    if (file.size > 5 * 1024 * 1024) {
      throw new Error('裁剪后图片仍超过 5MB，请选择更小的图片')
    }
    console.log('头像上传大小:', (file.size / 1024).toFixed(1), 'KB')
    const resp = await adminUpload(file)
    profileForm.avatar = resp.data?.url || ''
    cropVisible.value = false
    // 上传成功后自动保存到个人资料，避免刷新丢失
    const saved = await saveProfile()
    if (saved) ElMessage.success('头像上传并保存成功')
  } catch (e) {
    console.error('裁剪上传失败:', e)
    ElMessage.error('裁剪失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
  uploading.value = false
}

// ---- 保存资料（含删除旧头像） ----

const saveProfile = async () => {
  profileSaving.value = true
  let ok = false
  try {
    const prev = oldAvatar.value
    await adminUpdateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      avatar: profileForm.avatar
    })
    // 同步头像到站点 logo 与全局用户状态，同时同步邮箱
    if (profileForm.avatar) {
      try { await adminSaveSiteConfig({ siteLogo: profileForm.avatar }) } catch (_) {}
      if (userStore.userInfo) {
        userStore.userInfo = { ...userStore.userInfo, avatar: profileForm.avatar }
      }
      if (siteStore.info) {
        siteStore.info = { ...siteStore.info, siteLogo: profileForm.avatar }
      }
    }
    if (profileForm.email) {
      try { await adminSaveSiteConfig({ email: profileForm.email }) } catch (_) {}
      if (siteStore.info) {
        siteStore.info = { ...siteStore.info, email: profileForm.email }
      }
    }
    // 同步昵称到站点站长名,确保前台关于我模块即时生效
    if (profileForm.nickname) {
      try { await adminSaveSiteConfig({ authorName: profileForm.nickname }) } catch (_) {}
      // 1) 本地 reactive 更新,前台已打开的页面会立刻拿到新 authorName
      if (siteStore.info) {
        siteStore.info = { ...siteStore.info, authorName: profileForm.nickname }
      }
      if (userStore.userInfo) {
        userStore.userInfo = { ...userStore.userInfo, nickname: profileForm.nickname }
      }
      // 2) 强制重新拉一次,避免任何缓存/竞态导致前台仍显示旧值
      try { await siteStore.reload() } catch (_) {}
    }
    ElMessage.success('资料已保存')
    // 如果头像有变化，删除旧文件
    if (prev && prev !== profileForm.avatar) {
      try { await adminDeleteUpload(prev) } catch (_) {}
      oldAvatar.value = profileForm.avatar
    }
    ok = true
  } catch (e) {
    ElMessage.error('保存失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
  profileSaving.value = false
  return ok
}

const savePassword = async () => {
  if (!pwdForm.oldPassword) return ElMessage.warning('请输入旧密码')
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwdForm.newPassword !== pwdForm.confirmPassword) return ElMessage.warning('两次新密码不一致')
  pwdSaving.value = true
  try {
    await adminChangePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已修改，下次登录时生效')
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    pwdVisible.value = false
  } catch (_) {}
  pwdSaving.value = false
}

const saveSite = async () => {
  siteSaving.value = true
  try {
    // 仅提交白名单内的 key,避免把数据库里残留的 site_name 等额外 key 一并发出去被后端拒绝
    const payload = {
      siteName: siteForm.siteName,
      motto: siteForm.motto,
      description: siteForm.description,
      keywords: siteForm.keywords,
      beian: siteForm.beian,
      comment_audit: siteForm.comment_audit,
      github: siteForm.github,
      email: siteForm.email,
      aboutSkills: siteForm.aboutSkills
    }
    await adminSaveSiteConfig(payload)
    ElMessage.success('站点信息已保存')
    // 刷新 front 页面的站点缓存
    siteStore.loaded = false
    await siteStore.load()
  } catch (_) {}
  siteSaving.value = false
}

// 把加载逻辑抽成可重试函数,顶部 banner 上挂重试按钮(替代原 try {} catch (_) {} 完全静默吞错)
const loadAll = async () => {
  loadError.value = ''
  // 串行,避免瞬时打两个 admin 接口给后端压力(后端若慢,两个并发更易 504)
  profileLoading.value = true
  try {
    const resp = await adminProfile()
    const u = resp.data || {}
    Object.assign(profileForm, {
      avatar: u.avatar || '',
      nickname: u.nickname || '',
      email: u.email || '',
      username: u.username
    })
    oldAvatar.value = u.avatar || ''
    if (userStore.userInfo && u.avatar) {
      userStore.userInfo = { ...userStore.userInfo, avatar: u.avatar }
    }
  } catch (e) {
    loadError.value = `个人资料加载失败:${e?.response?.data?.message || e?.message || '未知错误'}`
    ElMessage.error(loadError.value)
  } finally {
    profileLoading.value = false
  }

  siteLoading.value = true
  try {
    const resp = await adminSiteConfig()
    const cfg = resp.data || {}
    Object.assign(siteForm, cfg)
    if (!siteForm.siteName && cfg.site_name) siteForm.siteName = cfg.site_name
    if (cfg.site_name) delete siteForm.site_name
    if (cfg.siteLogo && siteStore.info) {
      siteStore.info = { ...siteStore.info, siteLogo: cfg.siteLogo }
    }
  } catch (e) {
    loadError.value = loadError.value
      ? `${loadError.value}; 站点信息加载失败:${e?.response?.data?.message || e?.message || '未知错误'}`
      : `站点信息加载失败:${e?.response?.data?.message || e?.message || '未知错误'}`
    ElMessage.error(loadError.value)
  } finally {
    siteLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped lang="scss">
.load-error {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px;
  margin-bottom: 16px;
  background: #fef3c7; color: #92400e;
  border: 1px solid #fcd34d; border-radius: 8px;
  font-size: 13px; line-height: 1.5;
}
.load-error .el-icon { font-size: 16px; flex-shrink: 0; }
.load-error > span { flex: 1; word-break: break-all; }

.profile-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  align-items: stretch;
}
@media (max-width: 960px) {
  .profile-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  :deep(.el-dialog) { width: 92vw !important; }
}
.form-tip {
  font-size: 12px; color: var(--c-ink-soft, #94a3b8);
  line-height: 1.6; margin-top: 4px;
}

.profile-card { margin-bottom: 0; }

.card-header {
  display: flex; align-items: center; gap: 8px;
  font-family: var(--font-serif); font-weight: 600; font-size: 15px; color: var(--c-ink);
}

.profile-right {
  display: flex; flex-direction: column; gap: 20px;
}

.avatar-section {
  display: flex; justify-content: center; margin-bottom: 24px;
}

.avatar-upload {
  position: relative; cursor: pointer; border-radius: 50%;
}

.profile-avatar { transition: opacity 0.2s; }

.avatar-overlay {
  position: absolute; inset: 0; border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: #fff; font-size: 12px; opacity: 0; transition: opacity 0.2s;
}
.avatar-upload:hover .avatar-overlay { opacity: 1; }
.avatar-overlay .el-icon { font-size: 22px; }

.profile-form :deep(.el-form-item) { margin-bottom: 18px; }
.form-static { color: var(--c-ink-soft); font-size: 14px; }

/* 裁剪 */
.crop-container { max-height: 400px; }
.cropper-instance { background: #f0f0f0; max-height: 400px; }
</style>
