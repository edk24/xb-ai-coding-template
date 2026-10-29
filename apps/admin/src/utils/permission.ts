import { useAuthStore } from '../store/auth'

/**
 * 前端权限判断：超管放行，其他用户按 permission_keys 判断。
 * 仅用于隐藏/展示入口，真正的鉴权由后端接口负责。
 */
export function usePermission() {
  const user = useAuthStore((s) => s.user)
  const has = (key: string) => !!user && (user.is_super || (user.permission_keys || []).includes(key))
  return { has, isSuper: !!user?.is_super }
}
