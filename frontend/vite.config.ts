import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    proxy: {
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 页面刷新请求不走代理，由 Vite 返回 index.html（SPA 模式）
        // 只有 API / JSON 请求才转发到后端
        bypass(req) {
          const accept = req.headers?.accept || ''
          if (accept.includes('text/html')) {
            return '/index.html'
          }
        },
      },
    },
  },
})
