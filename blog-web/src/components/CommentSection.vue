<template>
  <section class="comment-section">
    <h3 class="cs-title">评论 <span class="cs-count">({{ total }})</span></h3>

    <div class="comment-form">
      <div class="cs-avatar-row">
        <el-avatar :src="avatarPreview" :size="48" />
        <div class="cs-avatar-actions">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="uploadAvatar"
            accept="image/*"
          >
            <el-button size="small" :loading="avatarUploading">
              <el-icon><Plus /></el-icon> 上传头像
            </el-button>
          </el-upload>
          <span class="text-soft" style="font-size: 12px;">不传则用 Gravatar 邮箱头像</span>
        </div>
      </div>
      <el-input v-model="form.nickname" placeholder="昵称 *" maxlength="20" />
      <el-input v-model="form.email" placeholder="邮箱 (选填, 不会公开)" />
      <el-input v-model="form.website" placeholder="网站 (选填)" />
      <el-input v-model="form.content" type="textarea" :rows="4" placeholder="说点什么… 支持 Markdown" maxlength="1000" show-word-limit />
      <div class="form-actions">
        <span class="text-soft" v-if="replyTo">回复 <b>{{ replyTo.nickname }}</b>
          <el-icon class="clear-reply" @click="replyTo = null"><Close /></el-icon>
        </span>
        <el-button type="primary" @click="submit" :loading="submitting">发表评论</el-button>
      </div>
    </div>

    <div class="comment-list" v-if="tree.length">
      <CommentItem v-for="c in tree" :key="c.id" :comment="c" @reply="onReply" />
    </div>
    <el-empty v-else description="还没有评论，来抢沙发吧～" />
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Close, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitComment, commentsByArticle } from '@/api/front'
import { uploadAvatar as uploadAvatarApi } from '@/api/admin'
import CommentItem from '@/components/CommentItem.vue'

const props = defineProps({ articleId: { type: Number, required: true } })

const tree = ref([])
const total = ref(0)
const submitting = ref(false)
const replyTo = ref(null)
const avatarUploading = ref(false)
const form = reactive({ nickname: '', email: '', website: '', avatar: '', content: '', parentId: null })

// 头像预览：优先用上传的，否则 gravatar identicon
const avatarPreview = computed(() => {
  if (form.avatar) return form.avatar
  return ''
})

const beforeAvatarUpload = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return false
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return false
  }
  return true
}

const uploadAvatar = async (options) => {
  avatarUploading.value = true
  try {
    const resp = await uploadAvatarApi(options.file)
    form.avatar = resp.data?.url || resp.url || ''
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const load = async () => {
  if (!props.articleId) {
    tree.value = []
    total.value = 0
    return
  }
  try {
    const resp = await commentsByArticle(props.articleId)
    tree.value = resp.data || []
    const count = (arr) => arr.reduce((n, c) => n + 1 + (c.replies ? count(c.replies) : 0), 0)
    total.value = count(tree.value)
  } catch (_) {}
}

const onReply = (c) => {
  replyTo.value = c
  form.parentId = c.id
}

const submit = async () => {
  if (!form.nickname.trim()) return ElMessage.warning('请填写昵称')
  if (!form.content.trim()) return ElMessage.warning('请填写评论内容')
  submitting.value = true
  try {
    await submitComment({
      articleId: props.articleId,
      nickname: form.nickname,
      email: form.email,
      website: form.website,
      avatar: form.avatar,
      content: form.content,
      parentId: form.parentId
    })
    ElMessage.success('评论成功，等待审核')
    form.content = ''
    form.parentId = null
    form.avatar = ''
    replyTo.value = null
    load()
  } catch (_) {}
  submitting.value = false
}

onMounted(load)
</script>

<style scoped lang="scss">
.comment-section { padding: 40px 0; }
.cs-title {
  font-family: var(--font-serif);
  font-size: 22px;
  margin: 0 0 24px;
  padding-left: 12px;
  border-left: 3px solid var(--c-autumn-500);
}
.cs-count { color: var(--c-ink-soft); font-size: 16px; font-weight: 400; }

.comment-form {
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius);
  padding: 24px;
  margin-bottom: 30px;
}
.comment-form .el-input { margin-bottom: 12px; }
.cs-avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
  padding: 12px;
  background: var(--c-paper-soft);
  border-radius: 8px;
}
.cs-avatar-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.clear-reply {
  cursor: pointer;
  margin-left: 6px;
  color: var(--c-ink-soft);
}
.clear-reply:hover { color: var(--c-autumn-500); }
</style>