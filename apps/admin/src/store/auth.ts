import { create } from 'zustand'
import { profileApi } from '../api/auth'
import type { UserProfile } from '../api/auth'

/** idle：尚未拉取会话信息；loading：正在拉取；anonymous：未登录或会话已失效 */
export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  user: UserProfile | null
  status: AuthStatus
  mustChangePassword: boolean
  fetchProfile: () => Promise<void>
  setUser: (user: UserProfile | null) => void
  clear: () => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  status: 'idle',
  mustChangePassword: false,

  /** 会话由后端 HttpOnly Cookie 维护，刷新页面后凭 Cookie 重新拉取用户信息 */
  fetchProfile: async () => {
    // 已经拉取或正在拉取时直接复用，避免 StrictMode 重复请求
    if (get().status !== 'idle') return

    set({ status: 'loading' })
    try {
      const res = await profileApi()
      if (res.data.code === 0 && res.data.data) {
        set({
          user: res.data.data,
          status: 'authenticated',
          mustChangePassword: !!res.data.data.must_change_password,
        })
        return
      }
      set({ user: null, status: 'anonymous', mustChangePassword: false })
    } catch {
      // profile 401 表示会话失效，进入匿名态交给路由守卫跳转登录
      set({ user: null, status: 'anonymous', mustChangePassword: false })
    }
  },

  setUser: (user) =>
    set({
      user,
      status: user ? 'authenticated' : 'anonymous',
      mustChangePassword: !!user?.must_change_password,
    }),

  clear: () => set({ user: null, status: 'anonymous', mustChangePassword: false }),
}))
