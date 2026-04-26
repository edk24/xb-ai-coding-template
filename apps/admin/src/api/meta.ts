import client from './client'

export interface DashboardData {
  welcome: string
  stats: {
    admin_users: number
    roles: number
    departments: number
    permissions: number
  }
}

export function getDashboardApi() {
  return client.get<{ code: number; message: string; data: DashboardData }>(
    '/meta/dashboard',
  )
}
