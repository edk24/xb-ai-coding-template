import client from './client'

export interface AdminUser {
  id: number
  department_id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  status: number
  is_super: number
  remark: string
  created_at: string
  department_name: string
  roles: string[]
  role_ids: number[]
}

export interface CreateAdminUserParams {
  department_id: number
  username: string
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  password: string
  status: number
  remark?: string
  role_ids: number[]
}

export interface UpdateAdminUserParams {
  department_id: number
  username: string
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  status: number
  remark?: string
  role_ids: number[]
}

export function getAdminUsersApi() {
  return client.get<{ code: number; message: string; data: AdminUser[] }>(
    '/admin-users',
  )
}

export function createAdminUserApi(params: CreateAdminUserParams) {
  return client.post<{ code: number; message: string; data: AdminUser }>(
    '/admin-users',
    params,
  )
}

export function updateAdminUserApi(id: number, params: UpdateAdminUserParams) {
  return client.put<{ code: number; message: string; data: AdminUser }>(
    `/admin-users/${id}`,
    params,
  )
}

export function deleteAdminUserApi(id: number) {
  return client.delete<{ code: number; message: string }>(
    `/admin-users/${id}`,
  )
}

export interface ResetPasswordParams {
  new_password?: string
}

export function resetPasswordApi(id: number, params?: ResetPasswordParams) {
  return client.put<{ code: number; message: string; data: { new_password: string } }>(
    `/admin-users/${id}/reset-password`,
    params || {},
  )
}
