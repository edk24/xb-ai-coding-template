import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Typography } from 'antd'
import {
  TeamOutlined, SafetyCertificateOutlined, ApartmentOutlined,
  KeyOutlined,
} from '@ant-design/icons'
import { getDashboardApi } from '../../api/meta'
import type { DashboardData } from '../../api/meta'

const { Title } = Typography

export default function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    getDashboardApi()
      .then((res) => setData(res.data.data))
      .finally(() => setLoading(false))
  }, [])

  const cards = [
    { title: '管理员', value: data?.stats.admin_users, icon: <TeamOutlined />, color: '#1677ff' },
    { title: '角色', value: data?.stats.roles, icon: <SafetyCertificateOutlined />, color: '#52c41a' },
    { title: '部门', value: data?.stats.departments, icon: <ApartmentOutlined />, color: '#faad14' },
    { title: '权限', value: data?.stats.permissions, icon: <KeyOutlined />, color: '#ff4d4f' },
  ]

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        {data?.welcome || '加载中...'}
      </Title>
      <Row gutter={[16, 16]}>
        {cards.map((card) => (
          <Col xs={12} sm={12} md={6} key={card.title}>
            <Card loading={loading}>
              <Statistic
                title={card.title}
                value={card.value}
                prefix={<span style={{ color: card.color }}>{card.icon}</span>}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}
