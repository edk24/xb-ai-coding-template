import client from './client'

/** 上传接口单独放宽超时，大文件上传不受默认 15s 限制 */
const UPLOAD_TIMEOUT = 5 * 60 * 1000

export interface Attachment {
  id: number
  admin_user_id: number
  name: string
  size: number
  mime_type: string
  storage_type: string
  /** 浏览器可直接访问的地址：本地附件是 /admin-api/attachments/{id}/file，COS 是 https 绝对地址 */
  file_url: string
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
  /** 对象存储中的对象 key，COS/OSS 直传仍需提交 */
  path: string
  /** 访问地址，后端要求必须是 https 绝对地址或 /admin-api/ 同源路径 */
  url?: string
  cdn_url?: string
}

/** 附件访问地址白名单：https 绝对地址或同源 /admin-api 路径 */
export function isValidFileUrl(url: string): boolean {
  return url.startsWith('https://') || url.startsWith('/admin-api/')
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
    timeout: UPLOAD_TIMEOUT,
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
