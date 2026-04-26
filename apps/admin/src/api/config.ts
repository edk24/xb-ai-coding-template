import client from './client'

export type ConfigItemType =
  | 'input' | 'textarea' | 'number'
  | 'select' | 'radio' | 'checkbox'
  | 'image' | 'multi_image' | 'rich_text'
  | 'switch' | 'color'

export interface ConfigItem {
  id: number
  group_name: string
  name: string
  key: string
  value: string
  type: ConfigItemType
  options: string // JSON string
  sort: number
  status: number
  remark: string
  created_at: string
}

export interface CreateConfigItemParams {
  name: string
  group_name?: string
  key: string
  value?: string
  type: ConfigItemType
  options?: string
  sort?: number
  status?: number
  remark?: string
}

export interface BatchSaveItem {
  id: number
  value: string | string[] | number | boolean
}

// -- Items --
export function getConfigItemsApi() {
  return client.get<{ code: number; message: string; data: ConfigItem[] }>(
    '/config/items',
  )
}

export function createConfigItemApi(params: CreateConfigItemParams) {
  return client.post<{ code: number; message: string; data: ConfigItem }>(
    '/config/items',
    params,
  )
}

export function updateConfigItemApi(id: number, params: CreateConfigItemParams) {
  return client.put<{ code: number; message: string; data: ConfigItem }>(
    `/config/items/${id}`,
    params,
  )
}

export function deleteConfigItemApi(id: number) {
  return client.delete<{ code: number; message: string }>(`/config/items/${id}`)
}

export function batchSaveConfigItemsApi(items: BatchSaveItem[]) {
  return client.put<{ code: number; message: string }>('/config/items/batch-save', { items })
}
