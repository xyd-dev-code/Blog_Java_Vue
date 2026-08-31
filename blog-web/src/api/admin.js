import http from '@/utils/request'

// articles
export const adminArticles = (params) => http.get('/admin/articles', { params })
export const adminArticleById = (id) => http.get(`/admin/articles/${id}`)
export const adminCreateArticle = (data) => http.post('/admin/articles', data)
export const adminUpdateArticle = (id, data) => http.put(`/admin/articles/${id}`, data)
export const adminDeleteArticle = (id) => http.delete(`/admin/articles/${id}`)
export const adminBatchDeleteArticles = (ids) => http.delete('/admin/articles', { data: ids })
export const adminUpdateArticleStatus = (id, status) => http.put(`/admin/articles/${id}/status`, null, { params: { status } })
export const adminUpdateArticleTop = (id, top) => http.put(`/admin/articles/${id}/top`, null, { params: { top } })
export const adminUpdateArticleFeatured = (id, featured) => http.put(`/admin/articles/${id}/featured`, null, { params: { featured } })
export const adminSetArticleViewCount = (id, value) => http.put(`/admin/articles/${id}/view-count`, null, { params: { value } })
export const adminAdjustArticleViewCount = (id, delta) => http.put(`/admin/articles/${id}/view-count/delta`, null, { params: { delta } })

// articles — import/export
// PDF/DOCX 走 pandoc + xelatex,首次编译要加载字体,通常 30~90s,给足超时
const EXPORT_TIMEOUT = 120000
export const adminExportArticle = (id, format = 'md') =>
  http.get(`/admin/articles/export/${id}`, { params: { format }, responseType: 'blob', timeout: EXPORT_TIMEOUT })
export const adminExportAllArticles = (format = 'md') =>
  http.get('/admin/articles/export-all', { params: { format }, responseType: 'blob', timeout: EXPORT_TIMEOUT })
export const adminDownloadMarkdownTemplate = () =>
  http.get('/admin/articles/export/template', { responseType: 'blob', timeout: EXPORT_TIMEOUT })
export const adminDownloadDocxTemplate = () =>
  http.get('/admin/articles/export/template.docx', { responseType: 'blob', timeout: EXPORT_TIMEOUT })
export const adminImportArticles = (formData) =>
  http.post('/admin/articles/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/** 浏览器触发下载一个 Blob(从 axios responseType:'blob' 流式接收后) */
export const triggerDownload = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// categories
export const adminCategories = (params) => http.get('/admin/categories', { params })
export const adminCreateCategory = (data) => http.post('/admin/categories', data)
export const adminUpdateCategory = (id, data) => http.put(`/admin/categories/${id}`, data)
export const adminDeleteCategory = (id) => http.delete(`/admin/categories/${id}`)

// tags
export const adminTags = (params) => http.get('/admin/tags', { params })
export const adminCreateTag = (data) => http.post('/admin/tags', data)
export const adminUpdateTag = (id, data) => http.put(`/admin/tags/${id}`, data)
export const adminDeleteTag = (id) => http.delete(`/admin/tags/${id}`)

// comments
export const adminComments = (params) => http.get('/admin/comments', { params })
export const adminApproveComment = (id) => http.put(`/admin/comments/${id}/approve`)
export const adminSpamComment = (id) => http.put(`/admin/comments/${id}/spam`)
export const adminDeleteComment = (id) => http.delete(`/admin/comments/${id}`)
export const adminCommentStats = () => http.get('/admin/comments/stats')
export const adminReplyComment = (id, data) => http.post(`/admin/comments/${id}/reply`, data)

// guestbook
export const adminGuestbook = (params) => http.get('/admin/guestbook', { params })
export const adminApproveGuestbook = (id) => http.put(`/admin/guestbook/${id}/approve`)
export const adminSpamGuestbook = (id) => http.put(`/admin/guestbook/${id}/spam`)
export const adminDeleteGuestbook = (id) => http.delete(`/admin/guestbook/${id}`)
export const adminGuestbookStats = () => http.get('/admin/guestbook/stats')
export const adminReplyGuestbook = (id, data) => http.post(`/admin/guestbook/${id}/reply`, data)
export const adminSetFeaturedGuestbook = (id, featured) => http.put(`/admin/guestbook/${id}/featured`, { featured })

// reports
export const adminReports = (params) => http.get('/admin/reports', { params })
export const adminReportStats = () => http.get('/admin/reports/stats')
export const adminUpdateReportStatus = (id, status) => http.put(`/admin/reports/${id}/status`, { status })
export const adminDeleteReport = (id) => http.delete(`/admin/reports/${id}`)

// projects
export const adminProjects = (params) => http.get('/admin/projects', { params })
export const adminCreateProject = (data) => http.post('/admin/projects', data)
export const adminUpdateProject = (id, data) => http.put(`/admin/projects/${id}`, data)
export const adminDeleteProject = (id) => http.delete(`/admin/projects/${id}`)
export const adminUpdateProjectStatus = (id, status) => http.put(`/admin/projects/${id}/status`, null, { params: { status } })
// project categories
export const adminProjectCategories = (params) => http.get('/admin/project-categories', { params })
export const adminProjectCategoriesAll = () => http.get('/admin/project-categories/all')
export const adminCreateProjectCategory = (data) => http.post('/admin/project-categories', data)
export const adminUpdateProjectCategory = (id, data) => http.put(`/admin/project-categories/${id}`, data)
export const adminUpdateProjectCategoryStatus = (id, status) => http.put(`/admin/project-categories/${id}/status`, null, { params: { status } })
export const adminDeleteProjectCategory = (id) => http.delete(`/admin/project-categories/${id}`)

// friend-links
export const adminFriendLinks = (params) => http.get('/admin/friend-links', { params })
export const adminCreateFriendLink = (data) => http.post('/admin/friend-links', data)
export const adminUpdateFriendLink = (id, data) => http.put(`/admin/friend-links/${id}`, data)
export const adminDeleteFriendLink = (id) => http.delete(`/admin/friend-links/${id}`)
export const adminReviewFriendLink = (id, status) => http.put(`/admin/friend-links/${id}/status`, null, { params: { status } })
export const adminSetFriendLinkRecommended = (id, recommended) => http.put(`/admin/friend-links/${id}/recommended`, null, { params: { recommended } })

// ====== 邮箱订阅订阅者管理 ======
export const adminSubscriptions = (params) => http.get('/admin/subscriptions', { params })
export const adminSubscriptionStats = () => http.get('/admin/subscriptions/stats')
export const adminCreateSubscription = (data) => http.post('/admin/subscriptions', data)
export const adminConfirmSubscription = (id) => http.put(`/admin/subscriptions/${id}/confirm`)
export const adminUnsubscribeSubscription = (id) => http.put(`/admin/subscriptions/${id}/unsubscribe`)
export const adminDeleteSubscription = (id) => http.delete(`/admin/subscriptions/${id}`)
export const adminExportSubscriptions = () =>
  http.get('/admin/subscriptions/export', { responseType: 'blob' })

// dashboard
export const adminDashboard = (params) => http.get('/admin/dashboard', { params })

// profile
export const adminProfile = () => http.get('/admin/profile')
export const adminUpdateProfile = (data) => http.put('/admin/profile', data)
export const adminChangePassword = (data) => http.post('/admin/profile/password', data)

// site config
export const adminSiteConfig = () => http.get('/admin/site')
export const adminSaveSiteConfig = (data) => http.put('/admin/site', data)

// upload (admin)
export const adminUpload = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/admin/upload', fd)
}
export const adminUploadBase64 = (base64) => http.post('/admin/upload/base64', { base64 })

// avatar (public, for visitors)
export const uploadAvatar = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/upload/avatar', fd)
}

// ────────────────────────────────────────────
// 访问统计
// ────────────────────────────────────────────

/** 今日(默认)或指定日期的 PV/UV/分时/分设备 聚合 */
export const adminStatsToday = (params) => http.get('/admin/stats/today', { params })

/** 访问日志分页明细 */
export const adminStatsLogs = (params) => http.get('/admin/stats/logs', { params })

// ====== 工具箱(tool) ======
// 列表接口静默：后端未部署/表未迁移时只展示空状态，不弹「服务暂时不可用」污染管理页
export const adminTools = (params) => http.get('/admin/tools', { params, silent: true })
export const adminTool = (id) => http.get(`/admin/tools/${id}`)
export const adminCreateTool = (data) => http.post('/admin/tools', data)
export const adminUpdateTool = (id, data) => http.put(`/admin/tools/${id}`, data)
export const adminDeleteTool = (id) => http.delete(`/admin/tools/${id}`)
export const adminUpdateToolStatus = (id, status) => http.put(`/admin/tools/${id}/status`, { status })
// 新建工具时预填的下一个序号(当前最大 sort_order + 1)
export const adminNextToolSort = () => http.get('/admin/tools/next-sort', { silent: true })

// ====== 工具分类(tool-category) ======
export const adminToolCategories = () => http.get('/admin/tool-categories')
export const adminToolCategory = (id) => http.get(`/admin/tool-categories/${id}`)
export const adminCreateToolCategory = (data) => http.post('/admin/tool-categories', data)
export const adminUpdateToolCategory = (id, data) => http.put(`/admin/tool-categories/${id}`, data)
export const adminDeleteToolCategory = (id) => http.delete(`/admin/tool-categories/${id}`)
export const adminSetToolCategoryStatus = (id, status) => http.patch(`/admin/tool-categories/${id}/status`, { status })
export const adminToolCategoryLogs = (limit = 20) => http.get('/admin/tool-categories/logs', { params: { limit } })
