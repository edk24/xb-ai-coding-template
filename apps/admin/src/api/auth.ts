import client from './client'

export interface LoginParams {
  username: string
  password: string
}

export interface UserProfile {
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

export interface LoginResult {
  token: string
  expire_in: number
  user: UserProfile
}

export function loginApi(params: LoginParams) {
  return client.post<{ code: number; message: string; data: LoginResult }>(
    '/auth/login',
    params,
  )
}

export function profileApi() {
  return client.get<{ code: number; message: string; data: UserProfile }>(
    '/auth/profile',
  )
}

export interface UpdateProfileParams {
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  remark?: string
}

export interface UpdatePasswordParams {
  old_password: string
  new_password: string
  confirm_password: string
}

export function updateProfileApi(params: UpdateProfileParams) {
  return client.put<{ code: number; message: string; data: UserProfile }>(
    '/auth/profile',
    params,
  )
}

export function updatePasswordApi(params: UpdatePasswordParams) {
  return client.put<{ code: number; message: string }>(
    '/auth/password',
    params,
  )
}

export function logoutApi() {
  return client.post<{ code: number; message: string }>('/auth/logout')
}
