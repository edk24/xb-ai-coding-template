import { usePermission } from '../utils/permission'

interface CanProps {
  permission: string
  children: React.ReactNode
}

/** 无权限时直接不渲染，用于按钮等操作入口的权限控制 */
export default function Can({ permission, children }: CanProps) {
  const { has } = usePermission()
  return has(permission) ? <>{children}</> : null
}
