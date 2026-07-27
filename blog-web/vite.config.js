import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  server: {
    port: 5173,
    host: true,
    open: false,
    proxy: {
      '/api': {
        target: 'https://203.0.113.10',
        changeOrigin: true,
        secure: false
      },
      '/uploads': {
        target: 'https://203.0.113.10',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
}))