import assert from 'node:assert/strict'
import test from 'node:test'
import { buildConfigSaveBatch } from '../src/pages/system/config/config-save.ts'

test('只提交当前分组的配置项，未打开的分组不会被空值覆盖', () => {
  const mailSettings = [
    { id: 3, key: 'mail_host', type: 'input' },
    { id: 4, key: 'mail_password', type: 'input' },
  ]

  const batch = buildConfigSaveBatch(mailSettings, {
    mail_host: 'smtp.163.com',
    mail_password: 'SECRET_AUTH_CODE',
  })

  assert.deepEqual(batch, [
    { id: 3, value: 'smtp.163.com' },
    { id: 4, value: 'SECRET_AUTH_CODE' },
  ])
})

test('开关存 1/0，多选存 JSON 字符串，缺失值存空串', () => {
  const items = [
    { id: 5, key: 'pay_enabled', type: 'switch' },
    { id: 6, key: 'pay_channels', type: 'checkbox' },
    { id: 7, key: 'pay_remark', type: 'input' },
  ]

  const batch = buildConfigSaveBatch(items, {
    pay_enabled: true,
    pay_channels: ['wechat', 'alipay'],
  })

  assert.deepEqual(batch, [
    { id: 5, value: '1' },
    { id: 6, value: '["wechat","alipay"]' },
    { id: 7, value: '' },
  ])
})
