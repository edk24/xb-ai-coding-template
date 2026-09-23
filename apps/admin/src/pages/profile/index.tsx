import { useEffect, useState } from 'react'
import { Form, Input, Button, Card, Typography, App } from 'antd'
import { useAuthStore } from '../../store/auth'
import { updateProfileApi, profileApi } from '../../api/auth'
import AvatarUpload from '../../components/AvatarUpload'

interface ProfileForm {
  nickname: string
  avatar: string
  phone: string
  email: string
  remark: string
}

export default function Profile() {
  const [form] = Form.useForm<ProfileForm>()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const { message } = App.useApp()

  useEffect(() => {
    setLoading(true)
    profileApi()
      .then((res) => {
        const u = res.data.data
        setUser(u)
        form.setFieldsValue({
          nickname: u.nickname,
          avatar: u.avatar,
          phone: u.phone,
          email: u.email,
          remark: u.remark,
        })
      })
      .catch(() => message.error('获取资料失败'))
      .finally(() => setLoading(false))
  }, [])

  const onFinish = async (values: ProfileForm) => {
    setSaving(true)
    try {
      const res = await updateProfileApi(values)
      setUser(res.data.data)
      message.success('资料已更新')
    } catch {
      message.error('更新失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card title="个人资料" loading={loading}>
      <div style={{ marginBottom: 16 }}>
        <Typography.Text type="secondary">登录账号：</Typography.Text>
        <Typography.Text strong>{user?.username}</Typography.Text>
      </div>
      <Form<ProfileForm>
        form={form}
        layout="vertical"
        onFinish={onFinish}
        style={{ maxWidth: 480 }}
      >
        <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="avatar" label="头像">
          <AvatarUpload />
        </Form.Item>
        <Form.Item name="phone" label="手机号">
          <Input />
        </Form.Item>
        <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="remark" label="备注">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={saving}>
            保存
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
