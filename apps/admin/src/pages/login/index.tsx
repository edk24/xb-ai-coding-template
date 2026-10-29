import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, App } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import type { AxiosError } from 'axios'
import { loginApi } from '../../api/auth'
import { useAuthStore } from '../../store/auth'
import { useThemeStore } from '../../store/theme'
import { messageError } from '../../utils/global-message'

const { Title } = Typography

interface LoginForm {
  username: string
  password: string
}

export default function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const setUser = useAuthStore((s) => s.setUser)
  const mode = useThemeStore((s) => s.mode)
  const { message } = App.useApp()

  // 展示因 401 会话失效被重定向到登录页的原因
  useEffect(() => {
    const redirect = sessionStorage.getItem('auth_redirect')
    if (redirect === '401') {
      const authMsg = sessionStorage.getItem('auth_message')
      message.warning(authMsg || '登录状态已过期，请重新登录')
    }
    sessionStorage.removeItem('auth_redirect')
    sessionStorage.removeItem('auth_message')
  }, [message])

  const handleError = (msg: string) => {
    messageError(msg)
  }

  const onFinish = async (values: LoginForm) => {
    setLoading(true)
    try {
      const res = await loginApi(values)
      if (res.data.code !== 0) {
        handleError(res.data.message || '登录失败，请检查账号密码')
        return
      }
      const { user } = res.data.data
      setUser(user)
      if (user.must_change_password) {
        message.warning('请先修改初始密码')
        navigate('/profile/password', { replace: true })
        return
      }
      message.success('登录成功')
      navigate('/dashboard', { replace: true })
    } catch (err) {
      // 401 账号密码错误、429 登录过于频繁都直接展示后端提示
      const axiosErr = err as AxiosError<{ message: string }>
      const msg = axiosErr.response?.data?.message || '登录失败，请检查账号密码'
      handleError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: mode === 'dark' ? '#141414' : '#f5f5f5',
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 32 }}>
          后台管理系统
        </Title>
        <Form<LoginForm>
          name="login"
          onFinish={onFinish}
          size="large"
          autoComplete="off"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入登录账号' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="登录账号" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入登录密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="登录密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登 录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
