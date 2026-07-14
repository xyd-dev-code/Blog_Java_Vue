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
export const adminAllComments = (params) => http.get('/admin/comments/all', { params })
export const adminApproveComment = (id) => http.put(`/admin/comments/${id}/approve`)
export const adminSpamComment = (id) => http.put(`/admin/comments/${id}/spam`)
export const adminDeleteComment = (id) => http.delete(`/admin/comments/${id}`)
export const adminCommentStats = () => http.get('/admin/comments/stats')

// guestbook
export const adminGuestbook = (params) => http.get('/admin/guestbook', { params })
export const adminApproveGuestbook = (id) => http.put(`/admin/guestbook/${id}/approve`)
export const adminSpamGuestbook = (id) => http.put(`/admin/guestbook/${id}/spam`)
export const adminDeleteGuestbook = (id) => http.delete(`/admin/guestbook/${id}`)
export const adminGuestbookStats = () => http.get('/admin/guestbook/stats')

// pages
export const adminPages = (params) => http.get('/admin/pages', { params })
export const adminCreatePage = (data) => http.post('/admin/pages', data)
export const adminUpdatePage = (id, data) => http.put(`/admin/pages/${id}`, data)
export const adminDeletePage = (id) => http.delete(`/admin/pages/${id}`)

// projects
export const adminProjects = (params) => http.get('/admin/projects', { params })
export const adminProjectById = (id) => http.get(`/admin/projects/${id}`)
export const adminCreateProject = (data) => http.post('/admin/projects', data)
export const adminUpdateProject = (id, data) => http.put(`/admin/projects/${id}`, data)
export const adminDeleteProject = (id) => http.delete(`/admin/projects/${id}`)
export const adminBatchDeleteProjects = (ids) => http.delete('/admin/projects', { data: ids })
export const adminUpdateProjectStatus = (id, status) => http.put(`/admin/projects/${id}/status`, null, { params: { status } })

// friend-links
export const adminFriendLinks = (params) => http.get('/admin/friend-links', { params })
export const adminFriendLinkById = (id) => http.get(`/admin/friend-links/${id}`)
export const adminCreateFriendLink = (data) => http.post('/admin/friend-links', data)
export const adminUpdateFriendLink = (id, data) => http.put(`/admin/friend-links/${id}`, data)
export const adminDeleteFriendLink = (id) => http.delete(`/admin/friend-links/${id}`)
export const adminBatchDeleteFriendLinks = (ids) => http.delete('/admin/friend-links', { data: ids })
export const adminReviewFriendLink = (id, status) => http.put(`/admin/friend-links/${id}/status`, null, { params: { status } })

// dashboard
export const adminDashboard = () => http.get('/admin/dashboard')

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
export const adminDeleteUpload = (url) => http.delete('/admin/upload', { params: { url } })

// avatar (public, for visitors)
export const uploadAvatar = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/upload/avatar', fd)
}