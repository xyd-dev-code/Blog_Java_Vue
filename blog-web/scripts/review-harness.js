import { createApp, h } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory, RouterView } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { MdPreview } from 'md-editor-v3'
import { sanitizeMarkdown, sanitizeDiagram } from '../src/utils/markdownSecurity'
import { parseMarkdownInWorker } from '../src/utils/markdownImportWorker'
import ArticleEdit from '../src/views/admin/ArticleEdit.vue'
import CommentSection from '../src/components/CommentSection.vue'
import CommentItem from '../src/components/CommentItem.vue'
import { useUserStore } from '../src/stores/user'
let app
const mount = (component, router) => {
  app?.unmount()
  app = createApp(component).use(createPinia()).use(ElementPlus)
  app.use(router || createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { render: () => null } }] }))
  app.mount('#app')
}
window.mountMarkdown = (markdown) => mount({
  render: () => h(MdPreview, { modelValue: markdown, noHighlight: true, sanitize: sanitizeMarkdown, sanitizeMermaid: sanitizeDiagram })
})
window.mountEditor = async () => {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/admin/articles/:id/edit', component: ArticleEdit },
    { path: '/admin/articles', component: { render: () => h('div', { id: 'left-editor' }, 'Article list') } }
  ] })
  await router.push('/admin/articles/1/edit')
  mount({ render: () => h(RouterView) }, router)
  await router.isReady()
  window.navigateToArticle = (id) => router.push(`/admin/articles/${id}/edit`)
  window.navigateAway = () => router.push('/admin/articles')
}
window.mountClosedComments = () => mount({ render: () => h(CommentSection, { articleId: 1, allowComment: false }) })
window.mountReplyTree = () => mount({
  data: () => ({ comment: { id: 100, nickname: 'Parent', content: 'Root', replies: [], hasMoreReplies: true } }),
  render() { return h(CommentItem, { comment: this.comment }) }
})
window.parseImport = parseMarkdownInWorker
window.testLogout = async () => {
  const store = useUserStore()
  store.token = 'synthetic-review-token'
  await store.logout()
  return store.token
}
window.reviewReady = true
