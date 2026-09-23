import { useEffect, useState, useMemo } from 'react'
import { useNavigate, Outlet, useLocation } from 'react-router-dom'
import { Layout, Menu, Dropdown, Avatar, Typography, Modal, Breadcrumb, Button, ConfigProvider } from 'antd'
import type { MenuProps } from 'antd'
import {
  UserOutlined, LogoutOutlined, TeamOutlined, SafetyCertificateOutlined,
  ApartmentOutlined, KeyOutlined, DashboardOutlined, FileTextOutlined,
  SunOutlined, MoonOutlined, PaperClipOutlined, ClockCircleOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../store/auth'
import { useThemeStore } from '../store/theme'
import { logoutApi } from '../api/auth'
import { getPermissionsTreeApi } from '../api/permissions'
import type { Permission } from '../api/permissions'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const sidebarTheme = {
  background: '#0f172a',
  hoverBackground: '#1e293b',
  selectedBackground: '#2563eb',
  subMenuBackground: '#111827',
  textColor: '#cbd5e1',
  selectedTextColor: '#fff',
}

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
  PaperClipOutlined: <PaperClipOutlined />,
  ClockCircleOutlined: <ClockCircleOutlined />,
  LogoutOutlined: <LogoutOutlined />,
}

// Path → breadcrumb title mapping
const breadcrumbMap: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/system/admin-users': '管理员管理',
  '/system/roles': '角色管理',
  '/system/departments': '部门管理',
  '/system/permissions': '菜单权限',
  '/system/config': '系统配置',
  '/system/scheduled-jobs': '定时任务',
  '/logs/login': '登录日志',
  '/logs/operation': '操作日志',
  '/attachments': '附件中心',
  '/profile/basic': '个人资料',
  '/profile/password': '修改密码',
}

function buildMenuItems(items: Permission[] | null): MenuProps['items'] {
  if (!items) return []
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
  const { mode, toggle: toggleTheme } = useThemeStore()
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
    } else if (path.startsWith('/attachments')) {
      items.push({ title: '附件中心' })
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
      <ConfigProvider
        theme={{
          components: {
            Layout: {
              siderBg: sidebarTheme.background,
              triggerBg: sidebarTheme.subMenuBackground,
              triggerColor: sidebarTheme.selectedTextColor,
            },
            Menu: {
              darkItemBg: sidebarTheme.background,
              darkSubMenuItemBg: sidebarTheme.subMenuBackground,
              darkItemHoverBg: sidebarTheme.hoverBackground,
              darkItemSelectedBg: sidebarTheme.selectedBackground,
              darkItemColor: sidebarTheme.textColor,
              darkItemSelectedColor: sidebarTheme.selectedTextColor,
            },
          },
        }}
      >
        <Sider
          width={240}
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          style={{ background: sidebarTheme.background }}
        >
          <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Text strong style={{ color: sidebarTheme.selectedTextColor, fontSize: 18 }}>
              {collapsed ? '后台' : '后台管理系统'}
            </Text>
          </div>
          <Menu
            mode="inline"
            theme="dark"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={handleMenuClick}
            style={{ background: sidebarTheme.background, borderInlineEnd: 0 }}
          />
        </Sider>
      </ConfigProvider>
      <Layout>
        <Header
          style={{
            background: mode === 'dark' ? '#1f1f1f' : '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 12,
            borderBottom: mode === 'dark' ? 'none' : '1px solid #f0f0f0',
          }}
        >
          <Button
            type="text"
            icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
            onClick={toggleTheme}
            style={{ color: mode === 'dark' ? '#fff' : undefined }}
          />
          <Dropdown menu={{ items: userMenuItems, onClick: onUserMenuClick }} placement="bottomRight">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <Avatar size={32} icon={<UserOutlined />} />
              <Text style={{ color: mode === 'dark' ? '#fff' : undefined }}>{user?.nickname || user?.username}</Text>
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
