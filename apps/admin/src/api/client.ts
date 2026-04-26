import axios from 'axios'
import { useAuthStore } from '../store/auth'

const client = axios.create({
  baseURL: '/admin-api',
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
  window.location.href = '/login'
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
