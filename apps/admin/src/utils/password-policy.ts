/** 新密码策略：8-64 位且同时包含字母和数字（与后端校验保持一致） */
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[\s\S]{8,64}$/

export function isValidPassword(value: string): boolean {
  return PASSWORD_PATTERN.test(value)
}
