import client from './client'

export interface Role {
  id: number
  name: string
  code: string
  status: number
  data_scope: number
  remark: string
  created_at: string
  updated_at: string
  permission_ids: number[]
}

export interface CreateRoleParams {
  name: string
  code: string
  status: number
  data_scope: number
  remark?: string
  permission_ids: number[]
}

export function getRolesApi() {
  return client.get<{ code: number; message: string; data: Role[] }>(
    '/roles',
  )
}

export function createRoleApi(params: CreateRoleParams) {
  return client.post<{ code: number; message: string; data: Role }>(
    '/roles',
    params,
  )
}

export function updateRoleApi(id: number, params: CreateRoleParams) {
  return client.put<{ code: number; message: string; data: Role }>(
    `/roles/${id}`,
    params,
  )
}

export function deleteRoleApi(id: number) {
  return client.delete<{ code: number; message: string }>(`/roles/${id}`)
}
