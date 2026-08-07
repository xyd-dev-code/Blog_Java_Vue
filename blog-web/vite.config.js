import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 本版本 unplugin-vue-components 不再导出 ElementPlusIconsResolver，
// 这里用 icons-vue 全量名自建解析器：只接管位于该集合内的 PascalCase 组件名（图标），
// 不碰 ElXxx 业务组件，避免命名冲突。
const iconNames = new Set(Object.keys(ElementPlusIconsVue))
const ElementPlusIconsResolver = (name, type) => {
  if (type !== 'component') return
  if (iconNames.has(name)) return { name, from: '@element-plus/icons-vue' }
  return undefined
}

// 阻止 ElementPlusResolver 自动 inject ElMessage / ElMessageBox / ElNotification / ElLoading 等
// 命令式 API —— 它们已经在 main.js 顶端从 element-plus/es/components/<name> 深路径
// 显式 import 了；这里若不阻止，unplugin-auto-import 会按默认解析走
// `from 'element-plus'` 全量 ESM 入口，瞬间把整包 EP（~336KB gzip）拉回首屏。
const elementPlusApiBlocker = (name) => {
  if (
    name === 'ElMessage' ||
    name === 'ElMessageBox' ||
    name === 'ElNotification' ||
    name === 'ElLoading' ||
    name === 'ElMessageServiceImpl' ||
    name === 'ElNotificationServiceImpl'
  ) {
    return false
  }
  return undefined
}

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    AutoImport({
      // 1. 先用 blocker 阻止 unplugin-auto-import 自动 inject ElMessage / ElMessageBox / ElNotification 命令式 API，
      //    避免它走 `from 'element-plus'` 全量 ESM 入口偷偷把整包拉回首屏。
      // 2. 再让 ElementPlusResolver 接管其余场景（如 <el-button> 组件的副作用 API 等）。
      resolvers: [elementPlusApiBlocker, ElementPlusResolver({ importStyle: 'css' })],
      dts: false
    }),
    Components({
      // 接管模板里的 <el-xxx> 组件 + 图标，样式按组件按需注入（不再引全量 CSS）
      resolvers: [
        ElementPlusResolver({ importStyle: 'css' }),
        ElementPlusIconsResolver
      ],
      dts: false
    })
  ],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  // 顶层 esbuild.drop 给所有 transform 生效;生产剥离 console/debugger
  esbuild: mode === 'production' ? { drop: ['console', 'debugger'] } : {},
  server: {
    port: 5173,
    host: true,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500,
    // esbuild minifier:默认已启用,显式声明以便 drop 生效
    minify: 'esbuild',
    rollupOptions: {
      output: {
        // 只把首屏关键路径上的重型依赖拆成独立 chunk（并行下载 + 内容哈希可跨部署缓存）。
        // ⚠️ 其余一律交还 Rollup 自然分包：绝不能把仅懒加载用到的重型依赖
        // （如 md-editor / tiptap / prosemirror / highlight.js，只在后台 ArticleEdit 用到）
        // 强拉进首屏 vendor，否则首页反而会多下载数百 KB。
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          // 不再把 element-plus 强合到一个具名 chunk —— unplugin-vue-components + ElementPlusResolver
          // 已经把 EP 拆成各路由/页面用到的子模块各自成块，再合一处反而把整包拉回首屏
          // （manualChunks 一旦给具名块，Rollup 会无脑把所有匹配模块塞进去，绕过按需！）
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('@vue') || id.includes('/vue/') || id.includes('vue-router') || id.includes('pinia')) return 'vue-vendor'
          // 返回 undefined → 交给 Rollup 默认分包（懒加载依赖留在各自的懒 chunk 里）
          return undefined
        }
      }
    }
  }
}))