import { createRouter, createWebHistory } from 'vue-router'

const SITE_NAME = '个人博客'

// 各路由对应的页标题（无对应则用 SITE_NAME）
const TITLE_MAP = {
  'home': '首页',
  'articles': '文章',
  'article-detail': '阅读',
  'category': '分类',
  'tag': '标签',
  'projects': '项目',
  'friends': '友链',
  'archives': '归档',
  'search': '搜索',
  'page': '页面',
  'guestbook': '留言板',
  'about': '关于我',
  'admin-login': '登录',
  'admin-dashboard': '仪表盘',
  'admin-articles': '文章管理',
  'admin-archives': '归档管理',
  'admin-article-new': '写文章',
  'admin-article-edit': '编辑文章',
  'admin-categories': '分类管理',
  'admin-tags': '标签管理',
  'admin-comments': '评论管理',
  'admin-guestbook': '留言管理',
  'admin-pages': '页面管理',
  'admin-projects': '项目管理',
  'admin-friend-links': '友链管理',
  'admin-profile': '个人资料'
}

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/FrontLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/front/Home.vue') },
      { path: 'articles', name: 'articles', component: () => import('@/views/front/Articles.vue') },
      { path: 'articles/:slug', name: 'article-detail', component: () => import('@/views/front/ArticleDetail.vue') },
      { path: 'categories/:slug', name: 'category', component: () => import('@/views/front/Category.vue') },
      { path: 'tags/:slug', name: 'tag', component: () => import('@/views/front/Tag.vue') },
      { path: 'projects', name: 'projects', component: () => import('@/views/front/Projects.vue') },
      { path: 'friends', name: 'friends', component: () => import('@/views/front/Friends.vue') },
      { path: 'archives', name: 'archives', component: () => import('@/views/front/Archives.vue') },
      { path: 'search', name: 'search', component: () => import('@/views/front/Search.vue') },
      { path: 'page/:slug', name: 'page', component: () => import('@/views/front/Page.vue') },
      { path: 'guestbook', name: 'guestbook', component: () => import('@/views/front/Guestbook.vue') },
      { path: 'about', name: 'about', component: () => import('@/views/front/About.vue') }
    ]
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { auth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'articles', name: 'admin-articles', component: () => import('@/views/admin/ArticleList.vue') },
      { path: 'archives', name: 'admin-archives', component: () => import('@/views/admin/Archives.vue') },
      { path: 'articles/new', name: 'admin-article-new', component: () => import('@/views/admin/ArticleEdit.vue') },
      { path: 'articles/:id/edit', name: 'admin-article-edit', component: () => import('@/views/admin/ArticleEdit.vue') },
      { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoryList.vue') },
      { path: 'tags', name: 'admin-tags', component: () => import('@/views/admin/TagList.vue') },
      { path: 'comments', name: 'admin-comments', component: () => import('@/views/admin/CommentList.vue') },
      { path: 'guestbook', name: 'admin-guestbook', component: () => import('@/views/admin/GuestbookList.vue') },
      { path: 'pages', name: 'admin-pages', component: () => import('@/views/admin/PageList.vue') },
      { path: 'projects', name: 'admin-projects', component: () => import('@/views/admin/ProjectList.vue') },
      { path: 'friend-links', name: 'admin-friend-links', component: () => import('@/views/admin/FriendLinkList.vue') },
      { path: 'profile', name: 'admin-profile', component: () => import('@/views/admin/Profile.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/front/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    if (saved) return saved
    return { top: 0, behavior: 'smooth' }
  }
})

router.beforeEach((to, from, next) => {
  if (to.meta.auth) {
    // 兼容两种存储：直接的 localStorage.token，和 pinia 持久化键 localStorage.user
    let token = localStorage.getItem('token')
    if (!token) {
      try {
        const raw = localStorage.getItem('user')
        if (raw) token = (JSON.parse(raw) || {}).token || ''
      } catch (_) {}
    }
    if (!token) return next({ path: '/admin/login', query: { redirect: to.fullPath } })
  }
  next()
})

// 路由切换后设置浏览器 tab title。优先级：自定义标题 > 路由名映射 > 默认
router.afterEach((to) => {
  const map = to.matched[to.matched.length - 1]
  const pageName = TITLE_MAP[to.name] || (map && map.name) || ''
  // 尝试读取 article/page 等动态标题（在 meta.title 里设置）
  const custom = to.meta && to.meta.title
  if (typeof custom === 'string' && custom) {
    document.title = custom
  } else if (pageName) {
    document.title = `${pageName} · ${SITE_NAME}`
  } else {
    document.title = SITE_NAME
  }
})

// 防止 Vue Router 4 默认抛 NavigationDuplicated 阻断流程
const originalPush = router.push
router.push = function (target) {
  return originalPush.call(this, target).catch((err) => {
    if (err && err.name !== 'NavigationDuplicated') throw err
    return err
  })
}

export default router