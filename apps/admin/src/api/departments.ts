import client from './client'

export interface Department {
  id: number
  parent_id: number
  name: string
  leader: string
  phone: string
  sort: number
  status: number
  remark: string
  children: Department[]
}

export interface CreateDepartmentParams {
  parent_id: number
  name: string
  leader?: string
  phone?: string
  sort?: number
  status: number
  remark?: string
}

export function getDepartmentsTreeApi() {
  return client.get<{ code: number; message: string; data: Department[] }>(
    '/departments/tree',
  )
}

export function createDepartmentApi(params: CreateDepartmentParams) {
  return client.post<{ code: number; message: string; data: Department }>(
    '/departments',
    params,
  )
}

export function updateDepartmentApi(id: number, params: CreateDepartmentParams) {
  return client.put<{ code: number; message: string; data: Department }>(
    `/departments/${id}`,
    params,
  )
}

export function deleteDepartmentApi(id: number) {
  return client.delete<{ code: number; message: string }>(
    `/departments/${id}`,
  )
}
