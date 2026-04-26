import { create } from 'zustand'

interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  remark: string
  department_name: string
  roles: string[]
  permission_keys: string[]
  is_super: boolean
}

interface AuthState {
  token: string | null
  user: User | null
  setToken: (token: string | null) => void
  setUser: (user: User | null) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('admin_token'),
  user: null,

  setToken: (token) => {
    if (token) {
      localStorage.setItem('admin_token', token)
    } else {
      localStorage.removeItem('admin_token')
    }
    set({ token })
  },

  setUser: (user) => set({ user }),

  logout: () => {
    localStorage.removeItem('admin_token')
    set({ token: null, user: null })
  },
}))
