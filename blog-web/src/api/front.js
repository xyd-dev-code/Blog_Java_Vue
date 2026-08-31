import http from '@/utils/request'

export const home = () => http.get('/home')
export const articles = (params) => http.get('/articles', { params })
export const articleBySlug = (slug) => http.get(`/articles/${slug}`)
export const articlesByCategory = (slug, params) => http.get(`/categories/${slug}/articles`, { params })
export const articlesByTag = (slug, params) => http.get(`/tags/${slug}/articles`, { params })
export const archives = (params) => http.get('/archives', { params })
export const search = (params) => http.get('/articles/search', { params: { q: params.kw, limit: params.limit || 20 } })

export const categoriesAll = () => http.get('/categories')
export const tagsAll = () => http.get('/tags')
export const siteConfig = () => http.get('/site')

export const projects = (params, config = {}) => http.get('/projects', { params, ...config })
export const projectCategories = (params = {}, config = {}) => http.get('/project-categories', { params, ...config })
export const friendLinks = () => http.get('/friend-links')
export const applyFriendLink = (data) => http.post('/friend-links/apply', data)

export const submitComment = (data) => http.post('/comments', data)
export const commentsByArticle = (id, params) => http.get(`/comments/article/${id}`, { params })
export const guestbookComments = (params) => http.get('/comments/guestbook', { params })
export const getCaptcha = () => http.get('/comments/captcha')
export const likeComment = (id) => http.post(`/comments/${id}/like`)
export const reportComment = (id, data) => http.post(`/comments/${id}/report`, data)

// 工具箱(前台公开) — 失败时静默(不弹 toast,由组件自行展示空状态)
// 注意:axios 查询参数必须放在 config.params 里,不能直接展开到 config 根
export const publicTools = (params) => http.get('/tools', { params, silent: true })
export const publicClickTool = (id) => http.post(`/tools/${id}/click`, null, { silent: true })
export const publicToolCategories = () => http.get('/tool-categories', { silent: true })

export const login = (data) => http.post('/auth/login', data)

// 邮箱订阅（替代原 RSS 订阅）— 双重确认：提交后发确认邮件，点击链接完成订阅
export const subscribeEmail = (data) => http.post('/subscribe', data)

// Direct replies use an ID cursor so capped initial trees never hide older replies.
export const commentReplies = (id, params) => http.get(`/comments/${id}/replies`, { params })
