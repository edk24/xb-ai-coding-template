import { useEffect } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuthStore } from '../store/auth'

/** 强制修改密码时只允许停留在该页面 */
const PASSWORD_PATH = '/profile/password'

export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const status = useAuthStore((s) => s.status)
  const mustChangePassword = useAuthStore((s) => s.mustChangePassword)
  const fetchProfile = useAuthStore((s) => s.fetchProfile)
  const location = useLocation()

  useEffect(() => {
    if (status === 'idle') fetchProfile()
  }, [status, fetchProfile])

  // 首次进入时先用 Cookie 恢复会话，避免刷新后误判为未登录
  if (status === 'idle' || status === 'loading') {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (status === 'anonymous') {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (mustChangePassword && location.pathname !== PASSWORD_PATH) {
    return <Navigate to={PASSWORD_PATH} replace />
  }

  return <>{children}</>
}
