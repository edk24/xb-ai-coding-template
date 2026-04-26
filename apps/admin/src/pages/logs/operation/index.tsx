import { useEffect, useState } from 'react'
import { Table, Card, Typography } from 'antd'
import { getOperationLogsApi } from '../../../api/logs'
import type { OperationLog } from '../../../api/logs'

export default function OperationLogs() {
  const [list, setList] = useState<OperationLog[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    getOperationLogsApi()
      .then((res) => setList(res.data.data))
      .finally(() => setLoading(false))
  }, [])

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    { title: '操作人', dataIndex: 'admin_user_id', width: 80 },
    { title: '模块', dataIndex: 'module', width: 100 },
    { title: '操作', dataIndex: 'action', width: 100 },
    { title: '方法', dataIndex: 'method', width: 72 },
    { title: '路径', dataIndex: 'path', width: 160 },
    {
      title: '请求参数',
      dataIndex: 'request_summary',
      ellipsis: true,
      render: (v: string) => (
        <Typography.Text copyable={{ text: v }} ellipsis style={{ maxWidth: 200 }}>
          {v}
        </Typography.Text>
      ),
    },
    { title: '操作 IP', dataIndex: 'ip', width: 140 },
    { title: '操作时间', dataIndex: 'operated_at', width: 180 },
  ]

  return (
    <Card title="操作日志">
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        scroll={{ x: 1000 }}
      />
    </Card>
  )
}
