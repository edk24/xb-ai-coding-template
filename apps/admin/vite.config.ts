import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // Vite base 控制生产包中 JS/CSS 等静态资源的访问前缀，部署到子路径时必须配置。
  const adminBase = env.VITE_ADMIN_BASE || '/'

  return {
    base: adminBase,
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/admin-api': {
          target: 'http://localhost:8000',
          changeOrigin: true,
        },
        '/storage': {
          target: 'http://localhost:8000',
          changeOrigin: true,
        },
      },
    },
  }
})
