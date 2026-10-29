import assert from 'node:assert/strict'
import test from 'node:test'
import { isSensitiveConfigKey } from '../src/pages/system/config/config-save.ts'

test('敏感配置项：含 secret/access_key 或以 _password/_token 结尾', () => {
  assert.equal(isSensitiveConfigKey('cos_secret_id'), true)
  assert.equal(isSensitiveConfigKey('cos_secret_key'), true)
  assert.equal(isSensitiveConfigKey('sms_access_key'), true)
  assert.equal(isSensitiveConfigKey('mail_password'), true)
  assert.equal(isSensitiveConfigKey('api_token'), true)
})

test('非敏感配置项不能误判，避免把普通配置当成密钥隐藏', () => {
  assert.equal(isSensitiveConfigKey('site_name'), false)
  assert.equal(isSensitiveConfigKey('storage_driver'), false)
  assert.equal(isSensitiveConfigKey('cos_bucket'), false)
  assert.equal(isSensitiveConfigKey('site_version'), false)
})
