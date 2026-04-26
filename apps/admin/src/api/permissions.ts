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
  icon?: string
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
