import { useEffect, useState } from 'react'
import { Table, Tag, Card } from 'antd'
import { getLoginLogsApi } from '../../../api/logs'
import type { LoginLog } from '../../../api/logs'

export default function LoginLogs() {
  const [list, setList] = useState<LoginLog[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    getLoginLogsApi()
      .then((res) => setList(res.data.data))
      .finally(() => setLoading(false))
  }, [])

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    { title: '登录账号', dataIndex: 'username_snapshot', width: 120 },
    { title: '登录 IP', dataIndex: 'ip', width: 140 },
    { title: '设备信息', dataIndex: 'user_agent', ellipsis: true },
    {
      title: '状态', dataIndex: 'status', width: 72,
      render: (s: number) =>
        s === 1 ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag>,
    },
    { title: '失败原因', dataIndex: 'fail_reason', width: 140 },
    { title: '登录时间', dataIndex: 'login_at', width: 180 },
  ]

  return (
    <Card title="登录日志">
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        scroll={{ x: 800 }}
      />
    </Card>
  )
}
