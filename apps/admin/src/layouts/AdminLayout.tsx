import { useEffect, useState, useMemo } from 'react'
import { useNavigate, Outlet, useLocation } from 'react-router-dom'
import { Layout, Menu, Dropdown, Avatar, Typography, Modal, Breadcrumb } from 'antd'
import type { MenuProps } from 'antd'
import {
  UserOutlined, LogoutOutlined, TeamOutlined, SafetyCertificateOutlined,
  ApartmentOutlined, KeyOutlined, DashboardOutlined, FileTextOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../store/auth'
import { logoutApi } from '../api/auth'
import { getPermissionsTreeApi } from '../api/permissions'
import type { Permission } from '../api/permissions'

const { Header, Sider, Content } = Layout
const { Text } = Typography

// Icon mapping: permission icon name → Ant Design icon component
const iconMap: Record<string, React.ReactNode> = {
  DashboardOutlined: <DashboardOutlined />,
  SettingOutlined: <SafetyCertificateOutlined />,
  UserOutlined: <UserOutlined />,
  TeamOutlined: <TeamOutlined />,
  SafetyCertificateOutlined: <SafetyCertificateOutlined />,
  ApartmentOutlined: <ApartmentOutlined />,
  KeyOutlined: <KeyOutlined />,
  FileTextOutlined: <FileTextOutlined />,
  LogoutOutlined: <LogoutOutlined />,
}

// Path → breadcrumb title mapping
const breadcrumbMap: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/system/admin-users': '管理员管理',
  '/system/roles': '角色管理',
  '/system/departments': '部门管理',
  '/system/permissions': '菜单权限',
  '/logs/login': '登录日志',
  '/logs/operation': '操作日志',
  '/profile/basic': '个人资料',
  '/profile/password': '修改密码',
}

function buildMenuItems(items: Permission[]): MenuProps['items'] {
  return items
    .filter((item) => item.type !== 'button' && item.status === 1 && item.permission_key !== 'profile')
    .map((item) => {
      const routePath = item.route_path || '/'
      const children = item.children ? buildMenuItems(item.children) : undefined
      return {
        key: routePath,
        icon: item.icon ? iconMap[item.icon] : undefined,
        label: item.name,
        children: children?.length ? children : undefined,
      }
    })
}

export default function AdminLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const [menuItems, setMenuItems] = useState<MenuProps['items']>([])
  const [collapsed, setCollapsed] = useState(false)

  useEffect(() => {
    getPermissionsTreeApi().then((res) => {
      setMenuItems(buildMenuItems(res.data.data))
    })
  }, [])

  const breadcrumbItems = useMemo(() => {
    const path = location.pathname
    const items = [{ title: '首页' }]
    if (path.startsWith('/system')) {
      items.push({ title: '系统管理' })
    } else if (path.startsWith('/logs')) {
      items.push({ title: '日志审计' })
    } else if (path.startsWith('/profile')) {
      items.push({ title: '个人中心' })
    }
    const currentTitle = breadcrumbMap[path]
    if (currentTitle) {
      items.push({ title: currentTitle })
    }
    return items
  }, [location.pathname])

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key)
  }

  const handleLogout = () => {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '退出',
      cancelText: '取消',
      onOk: async () => {
        try { await logoutApi() } catch { /* ignore */ }
        logout()
        navigate('/login', { replace: true })
      },
    })
  }

  const userMenuItems: MenuProps['items'] = [
    { key: 'profile', icon: <UserOutlined />, label: '个人资料' },
    { key: 'password', icon: <LogoutOutlined />, label: '修改密码' },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ]

  const onUserMenuClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'profile') navigate('/profile/basic')
    else if (key === 'password') navigate('/profile/password')
    else if (key === 'logout') handleLogout()
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={240}
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        style={{ background: '#141414' }}
      >
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Text strong style={{ color: '#fff', fontSize: 18 }}>
            {collapsed ? '后台' : '后台管理系统'}
          </Text>
        </div>
        <Menu
          mode="inline"
          theme="dark"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#1f1f1f',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
          }}
        >
          <Dropdown menu={{ items: userMenuItems, onClick: onUserMenuClick }} placement="bottomRight">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <Avatar size={32} icon={<UserOutlined />} />
              <Text style={{ color: '#fff' }}>{user?.nickname || user?.username}</Text>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ margin: 24 }}>
          <Breadcrumb items={breadcrumbItems} style={{ marginBottom: 16 }} />
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
