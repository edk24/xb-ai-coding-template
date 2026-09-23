import client from './client'

export interface ScheduledJob {
  id: number
  name: string
  code: string
  job_key: string
  cron_expression: string
  params: string
  status: number
  allow_manual: number
  lock_at_most_seconds: number
  remark: string
  last_run_at: string
  next_run_at: string
  created_at: string
  updated_at: string
}

export interface ScheduledJobLog {
  id: number
  scheduled_job_id: number
  job_code: string
  job_key: string
  trigger_type: string
  instance_id: string
  status: string
  started_at: string
  ended_at: string
  duration_ms: number
  output_summary: string
  error_message: string
}

export interface ScheduledJobHandlerOption {
  label: string
  value: string
}

export interface ScheduledJobParams {
  name: string
  code: string
  job_key: string
  cron_expression: string
  params?: string
  status: number
  allow_manual: number
  lock_at_most_seconds: number
  remark?: string
}

export function getScheduledJobsApi() {
  return client.get<{ code: number; message: string; data: ScheduledJob[] }>(
    '/scheduled-jobs',
  )
}

export function getScheduledJobHandlersApi() {
  return client.get<{ code: number; message: string; data: ScheduledJobHandlerOption[] }>(
    '/scheduled-jobs/handlers',
  )
}

export function getScheduledJobLogsApi(jobId?: number) {
  const query = jobId ? `?job_id=${jobId}` : ''
  return client.get<{ code: number; message: string; data: ScheduledJobLog[] }>(
    `/scheduled-jobs/logs${query}`,
  )
}

export function createScheduledJobApi(params: ScheduledJobParams) {
  return client.post<{ code: number; message: string; data: ScheduledJob }>(
    '/scheduled-jobs',
    params,
  )
}

export function updateScheduledJobApi(id: number, params: ScheduledJobParams) {
  return client.put<{ code: number; message: string; data: ScheduledJob }>(
    `/scheduled-jobs/${id}`,
    params,
  )
}

export function updateScheduledJobStatusApi(id: number, status: number) {
  return client.put<{ code: number; message: string; data: ScheduledJob }>(
    `/scheduled-jobs/${id}/status`,
    { status },
  )
}

export function runScheduledJobApi(id: number) {
  return client.post<{ code: number; message: string; data: { status: string; log_id?: number; error?: string } }>(
    `/scheduled-jobs/${id}/run`,
  )
}

export function deleteScheduledJobApi(id: number) {
  return client.delete<{ code: number; message: string }>(`/scheduled-jobs/${id}`)
}
