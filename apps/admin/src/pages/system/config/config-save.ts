import type { BatchSaveItem, ConfigItem } from '../../../api/config'

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
