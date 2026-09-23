import axios from 'axios'
import { useAuthStore } from '../store/auth'

/** 后台 API 地址由环境变量控制，未配置时保持同域 /admin-api 的部署方式 */
const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/admin-api'

const client = axios.create({
  baseURL: apiBaseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

/** 防止重复执行 401 跳转 */
let isRedirecting = false

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function onUnauthorized(message?: string) {
  if (isRedirecting) return
  isRedirecting = true

  useAuthStore.getState().logout()
  sessionStorage.setItem('auth_redirect', '401')
  if (message) {
    sessionStorage.setItem('auth_message', message)
  }
  // 使用 HashRouter，跳转必须带上部署前缀，例如 /backend/#/login
  window.location.href = `${import.meta.env.BASE_URL}#/login`
}

client.interceptors.response.use(
  (res) => {
    if (res.data?.code === 401) {
      onUnauthorized(res.data.message)
      return Promise.reject(new Error(res.data.message || '登录已过期'))
    }
    return res
  },
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const msg = (error.response.data as any)?.message
      onUnauthorized(msg)
    }
    return Promise.reject(error)
  },
)

export default client
