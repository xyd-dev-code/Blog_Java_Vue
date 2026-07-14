import http from '@/utils/request'

export const home = () => http.get('/home')
export const articles = (params) => http.get('/articles', { params })
export const articleBySlug = (slug) => http.get(`/articles/${slug}`)
export const articlesByCategory = (slug, params) => http.get(`/categories/${slug}/articles`, { params })
export const articlesByTag = (slug, params) => http.get(`/tags/${slug}/articles`, { params })
export const archives = () => http.get('/archives')
export const articlesByMonth = (ym, params) => http.get(`/archives/${ym}`, { params })
export const search = (params) => http.get('/articles/search', { params: { q: params.kw, limit: params.limit || 20 } })

export const categoriesAll = () => http.get('/categories')
export const tagsAll = () => http.get('/tags')
export const pagesAll = () => http.get('/pages')
export const pageBySlug = (slug) => http.get(`/pages/${slug}`)
export const siteConfig = () => http.get('/site')

export const projects = () => http.get('/projects')
export const friendLinks = () => http.get('/friend-links')
export const applyFriendLink = (data) => http.post('/friend-links/apply', data)

export const submitComment = (data) => http.post('/comments', data)
export const commentsByArticle = (id) => http.get(`/comments/article/${id}`)
export const guestbookComments = () => http.get('/comments/guestbook')

export const trackView = (id) => http.post(`/articles/${id}/view`)

export const login = (data) => http.post('/auth/login', data)