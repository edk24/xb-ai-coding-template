import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, message, Modal } from 'antd'
import { updatePasswordApi } from '../../api/auth'
import { useAuthStore } from '../../store/auth'

interface PasswordForm {
  old_password: string
  new_password: string
  confirm_password: string
}

export default function Password() {
  const [form] = Form.useForm<PasswordForm>()
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)

  const onFinish = async (values: PasswordForm) => {
    setLoading(true)
    try {
      await updatePasswordApi({
        old_password: values.old_password,
        new_password: values.new_password,
        confirm_password: values.confirm_password,
      })
      Modal.confirm({
        title: '密码修改成功',
        content: '请重新登录',
        okText: '去登录',
        cancelButtonProps: { style: { display: 'none' } },
        onOk: () => {
          logout()
          navigate('/login', { replace: true })
        },
      })
    } catch {
      message.error('密码修改失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title="修改密码" style={{ maxWidth: 480 }}>
      <Form<PasswordForm>
        form={form}
        layout="vertical"
        onFinish={onFinish}
      >
        <Form.Item
          name="old_password"
          label="原密码"
          rules={[{ required: true, message: '请输入原密码' }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="new_password"
          label="新密码"
          rules={[
            { required: true, message: '请输入新密码' },
            { min: 6, message: '密码至少 6 位' },
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="confirm_password"
          label="确认新密码"
          dependencies={['new_password']}
          rules={[
            { required: true, message: '请再次输入新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('new_password') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('两次输入的密码不一致'))
              },
            }),
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            保存
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
