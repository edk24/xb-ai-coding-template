import type { BatchSaveItem, ConfigItem } from '../../../api/config'

/**
 * 敏感配置项判定：key 含 secret/access_key，或以 _password/_token 结尾。
 * 后端对敏感项只返回空值 + has_value，保存时留空表示保持原值不变。
 */
export function isSensitiveConfigKey(key: string): boolean {
  // 与后端 QueryService.isSensitiveConfigKey 保持一致：统一小写后再判断
  const lower = (key || '').toLowerCase()
  return lower.includes('secret') || lower.includes('access_key')
    || lower.endsWith('_password') || lower.endsWith('_token')
}

/**
 * 将某个分组（当前选项卡）的表单值转换为批量保存接口的请求体。
 * 只包含传入分组的配置项，未打开过的选项卡字段不会以空值覆盖其他分组。
 */
export function buildConfigSaveBatch(groupItems: ConfigItem[], values: Record<string, unknown>): BatchSaveItem[] {
  return groupItems.map((item) => ({
    id: item.id,
    value: serializeConfigValue(item, values[item.key]),
  }))
}

/** 后端开关配置固定保存为 '1'/'0'，避免直接写入布尔值。 */
function serializeConfigValue(item: ConfigItem, value: unknown): unknown {
  if (item.type === 'switch') {
    return value ? '1' : '0'
  }
  if (value === undefined || value === null) {
    return ''
  }
  if (Array.isArray(value)) {
    return JSON.stringify(value)
  }
  // ColorPicker 的取值是颜色对象，取其十六进制字符串
  if (typeof value === 'object' && typeof (value as { toHexString?: () => string }).toHexString === 'function') {
    return (value as { toHexString: () => string }).toHexString()
  }
  return value
}
