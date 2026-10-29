import axios from 'axios'
import { useAuthStore } from '../store/auth'

/** 后台 API 地址由环境变量控制，未配置时保持同域 /admin-api 的部署方式 */
const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/admin-api'

const client = axios.create({
  baseURL: apiBaseURL,
  timeout: 15000,
  // 会话由后端 HttpOnly Cookie 维护，跨域部署时也必须带上 Cookie
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

/** 登录接口的 401 表示账号密码错误，交给登录页展示，不能当作会话失效跳转 */
function isLoginRequest(url?: string): boolean {
  return !!url && url.includes('/auth/login')
}

/**
 * 会话失效：清空 store 切到 anonymous，由 AuthGuard 跳转登录页。
 * 不能用 window.location 跳转：HashRouter 下第二次失效时地址已相同，跳转会失效。
 */
function onUnauthorized(message?: string) {
  const { status, clear } = useAuthStore.getState()
  // 只有已登录会话失效才提示"登录状态已过期"；首次访问没有会话时不打扰用户。
  // 先写提示信息再清空 store，保证登录页挂载时能读到。
  if (status === 'authenticated') {
    sessionStorage.setItem('auth_redirect', '401')
    if (message) {
      sessionStorage.setItem('auth_message', message)
    }
  }
  clear()
}

client.interceptors.response.use(
  (res) => {
    if (res.data?.code === 401) {
      if (!isLoginRequest(res.config.url)) onUnauthorized(res.data.message)
      return Promise.reject(new Error(res.data.message || '登录已过期'))
    }
    return res
  },
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      if (!isLoginRequest(error.config?.url)) {
        onUnauthorized((error.response.data as { message?: string })?.message)
      }
    }
    return Promise.reject(error)
  },
)

export default client
