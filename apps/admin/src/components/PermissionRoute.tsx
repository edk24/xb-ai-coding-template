import { useNavigate, useLocation } from 'react-router-dom'
import { Button, Result } from 'antd'
import { usePermission } from '../utils/permission'

/** 静态路由 → 页面查看权限映射，/profile/* 无需页面权限 */
const ROUTE_PERMISSIONS: Record<string, string> = {
  '/dashboard': 'dashboard:view',
  '/system/admin-users': 'admin-users:view',
  '/system/roles': 'roles:view',
  '/system/departments': 'departments:view',
  '/system/permissions': 'permissions:view',
  '/system/config': 'config:view',
  '/system/scheduled-jobs': 'scheduled-jobs:view',
  '/logs/login': 'login-logs:view',
  '/logs/operation': 'operation-logs:view',
  '/attachments': 'attachments:view',
}

export default function PermissionRoute({ children }: { children: React.ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { has } = usePermission()

  const required = ROUTE_PERMISSIONS[location.pathname]
  if (!required || has(required)) {
    return <>{children}</>
  }

  return (
    <Result
      status="403"
      title="403"
      subTitle="抱歉，你没有访问该页面的权限"
      extra={<Button type="primary" onClick={() => navigate('/dashboard')}>返回仪表盘</Button>}
    />
  )
}
