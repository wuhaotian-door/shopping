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
        bypass(req) {
          const accept = req.headers?.accept || ''
          if (accept.includes('text/html')) {
            return '/index.html'
          }
        },
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/models': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
