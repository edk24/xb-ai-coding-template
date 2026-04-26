import client from './client'

export interface LoginLog {
  id: number
  admin_user_id: number | null
  username_snapshot: string
  ip: string
  user_agent: string
  status: number
  fail_reason: string
  login_at: string
}

export interface OperationLog {
  id: number
  admin_user_id: number | null
  module: string
  action: string
  method: string
  path: string
  request_summary: string
  response_summary: string
  ip: string
  operated_at: string
}

export function getLoginLogsApi() {
  return client.get<{ code: number; message: string; data: LoginLog[] }>(
    '/login-logs',
  )
}

export function getOperationLogsApi() {
  return client.get<{ code: number; message: string; data: OperationLog[] }>(
    '/operation-logs',
  )
}
