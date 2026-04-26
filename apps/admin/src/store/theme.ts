import { create } from 'zustand'

export type ThemeMode = 'dark' | 'light'

interface ThemeState {
  mode: ThemeMode
  toggle: () => void
  setMode: (mode: ThemeMode) => void
}

export const useThemeStore = create<ThemeState>((set) => ({
  mode: (localStorage.getItem('admin_theme') as ThemeMode) || 'dark',
  toggle: () =>
    set((state) => {
      const next = state.mode === 'dark' ? 'light' : 'dark'
      localStorage.setItem('admin_theme', next)
      return { mode: next }
    }),
  setMode: (mode) => {
    localStorage.setItem('admin_theme', mode)
    set({ mode })
  },
}))
