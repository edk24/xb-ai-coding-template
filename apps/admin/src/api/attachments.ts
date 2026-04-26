import client from './client'

export interface Attachment {
  id: number
  admin_user_id: number
  name: string
  size: number
  mime_type: string
  storage_type: string
  path: string
  url: string
  created_at: string
}

export interface AttachmentListResult {
  items: Attachment[]
  total: number
  page: number
  page_size: number
}

export interface AttachmentConfig {
  storage_type: string
  max_file_size: number
  allowed_extensions: string
}

export interface CredentialsResult {
  storage_type: string
  credentials: {
    tmpSecretId: string
    tmpSecretKey: string
    sessionToken: string
  }
  region: string
  bucket: string
  path_prefix: string
  cdn_url: string
  expired_time: number
}

export interface RecordAttachmentParams {
  name: string
  size: number
  mime_type: string
  storage_type: string
  path: string
  url?: string
  cdn_url?: string
}

export function getAttachmentsConfigApi() {
  return client.get<{ code: number; message: string; data: AttachmentConfig }>('/attachments/config')
}

export function getAttachmentsApi(params: { page?: number; page_size?: number; storage_type?: string }) {
  return client.get<{ code: number; message: string; data: AttachmentListResult }>('/attachments', { params })
}

export function uploadAttachmentApi(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return client.post<{ code: number; message: string; data: Attachment }>('/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getCredentialsApi(storageType: string) {
  return client.post<{ code: number; message: string; data: CredentialsResult }>('/attachments/credentials', {
    storage_type: storageType,
  })
}

export function recordAttachmentApi(params: RecordAttachmentParams) {
  return client.post<{ code: number; message: string; data: Attachment }>('/attachments/record', params)
}

export function deleteAttachmentApi(id: number) {
  return client.delete<{ code: number; message: string }>(`/attachments/${id}`)
}
