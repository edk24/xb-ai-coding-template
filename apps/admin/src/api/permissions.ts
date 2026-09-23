import client from './client'

export interface Permission {
  id: number
  parent_id: number
  name: string
  type: 'catalog' | 'menu' | 'button'
  route_path: string
  component_path: string
  permission_key: string
  icon: string
  sort: number
  hidden: number
  status: number
  remark: string
  children: Permission[]
}

export interface CreatePermissionParams {
  parent_id: number
  name: string
  type: 'catalog' | 'menu' | 'button'
  route_path?: string
  component_path?: string
  permission_key: string
  sort?: number
  hidden?: number
  status: number
  remark?: string
}

export function getPermissionsTreeApi() {
  return client.get<{ code: number; message: string; data: Permission[] }>(
    '/permissions/tree',
  )
}

/**
 * 侧边栏菜单树：后端按当前登录用户的角色过滤，只返回该用户可用的菜单。
 * 与 getPermissionsTreeApi（权限管理页使用的全量权限树）区分开。
 */
export function getMenuTreeApi() {
  return client.get<{ code: number; message: string; data: Permission[] }>(
    '/meta/menu-tree',
  )
}

export function createPermissionApi(params: CreatePermissionParams) {
  return client.post<{ code: number; message: string; data: Permission }>(
    '/permissions',
    params,
  )
}

export function updatePermissionApi(id: number, params: CreatePermissionParams) {
  return client.put<{ code: number; message: string; data: Permission }>(
    `/permissions/${id}`,
    params,
  )
}

export function deletePermissionApi(id: number) {
  return client.delete<{ code: number; message: string }>(
    `/permissions/${id}`,
  )
}
