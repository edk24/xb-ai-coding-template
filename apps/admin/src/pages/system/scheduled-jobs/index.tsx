import { useEffect, useMemo, useState } from 'react'
import {
  App, Button, Card, Drawer, Form, Input, InputNumber, Popconfirm,
  Select, Space, Switch, Table, Tag, Typography,
} from 'antd'
import { ClockCircleOutlined, FileTextOutlined, PlusOutlined, PlayCircleOutlined } from '@ant-design/icons'
import {
  createScheduledJobApi,
  deleteScheduledJobApi,
  getScheduledJobHandlersApi,
  getScheduledJobLogsApi,
  getScheduledJobsApi,
  runScheduledJobApi,
  updateScheduledJobApi,
  updateScheduledJobStatusApi,
} from '../../../api/scheduled-jobs'
import type {
  ScheduledJob, ScheduledJobHandlerOption, ScheduledJobLog, ScheduledJobParams,
} from '../../../api/scheduled-jobs'

const statusMap: Record<string, { text: string; color: string }> = {
  running: { text: '执行中', color: 'processing' },
  success: { text: '成功', color: 'green' },
  failed: { text: '失败', color: 'red' },
}

export default function ScheduledJobs() {
  const { message } = App.useApp()
  const [jobs, setJobs] = useState<ScheduledJob[]>([])
  const [handlers, setHandlers] = useState<ScheduledJobHandlerOption[]>([])
  const [logs, setLogs] = useState<ScheduledJobLog[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [runningId, setRunningId] = useState<number | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [logDrawerOpen, setLogDrawerOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [logJob, setLogJob] = useState<ScheduledJob | null>(null)
  const [form] = Form.useForm()

  const handlerOptions = useMemo(() =>
    handlers.map((item) => ({ label: `${item.label} (${item.value})`, value: item.value })),
    [handlers],
  )

  const loadJobs = () => {
    setLoading(true)
    getScheduledJobsApi()
      .then((res) => setJobs(res.data.data))
      .finally(() => setLoading(false))
  }

  const loadHandlers = () => {
    getScheduledJobHandlersApi().then((res) => setHandlers(res.data.data))
  }

  useEffect(() => {
    loadJobs()
    loadHandlers()
  }, [])

  const openCreate = () => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({
      status: true,
      allow_manual: true,
      lock_at_most_seconds: 600,
      cron_expression: '0 0 2 * * *',
      params: '{}',
    })
    setDrawerOpen(true)
  }

  const openEdit = (record: ScheduledJob) => {
    setEditingId(record.id)
    form.setFieldsValue({
      name: record.name,
      code: record.code,
      job_key: record.job_key,
      cron_expression: record.cron_expression,
      params: record.params || '{}',
      status: record.status === 1,
      allow_manual: record.allow_manual === 1,
      lock_at_most_seconds: record.lock_at_most_seconds,
      remark: record.remark,
    })
    setDrawerOpen(true)
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const params: ScheduledJobParams = {
        name: values.name,
        code: values.code,
        job_key: values.job_key,
        cron_expression: values.cron_expression,
        params: values.params || '{}',
        status: values.status ? 1 : 0,
        allow_manual: values.allow_manual ? 1 : 0,
        lock_at_most_seconds: values.lock_at_most_seconds,
        remark: values.remark || '',
      }
      if (editingId) {
        await updateScheduledJobApi(editingId, params)
        message.success('任务已更新')
      } else {
        await createScheduledJobApi(params)
        message.success('任务已创建')
      }
      setDrawerOpen(false)
      loadJobs()
    } finally {
      setSaving(false)
    }
  }

  const handleStatusChange = async (record: ScheduledJob, checked: boolean) => {
    await updateScheduledJobStatusApi(record.id, checked ? 1 : 0)
    message.success(checked ? '任务已启用' : '任务已停用')
    loadJobs()
  }

  const handleRun = async (record: ScheduledJob) => {
    setRunningId(record.id)
    try {
      const res = await runScheduledJobApi(record.id)
      if (res.data.data.status === 'success') {
        message.success('任务执行成功')
      } else {
        message.error(res.data.data.error || '任务执行失败')
      }
      loadJobs()
    } finally {
      setRunningId(null)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteScheduledJobApi(id)
    message.success('任务已删除')
    loadJobs()
  }

  const openLogs = async (record: ScheduledJob) => {
    setLogJob(record)
    setLogDrawerOpen(true)
    const res = await getScheduledJobLogsApi(record.id)
    setLogs(res.data.data)
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    { title: '任务名称', dataIndex: 'name', width: 140 },
    { title: '任务编码', dataIndex: 'code', width: 140, render: (v: string) => <Tag>{v}</Tag> },
    { title: '处理器', dataIndex: 'job_key', width: 150 },
    { title: 'Cron', dataIndex: 'cron_expression', width: 140 },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (_: number, record: ScheduledJob) => (
        <Switch
          checked={record.status === 1}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={(checked) => handleStatusChange(record, checked)}
        />
      ),
    },
    { title: '最近执行', dataIndex: 'last_run_at', width: 160, render: (v: string) => v || '-' },
    { title: '下次执行', dataIndex: 'next_run_at', width: 160, render: (v: string) => v || '-' },
    {
      title: '操作', width: 230, fixed: 'right' as const,
      render: (_: unknown, record: ScheduledJob) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button type="link" size="small" icon={<FileTextOutlined />} onClick={() => openLogs(record)}>日志</Button>
          <Button
            type="link"
            size="small"
            icon={<PlayCircleOutlined />}
            disabled={record.allow_manual !== 1}
            loading={runningId === record.id}
            onClick={() => handleRun(record)}
          >
            执行
          </Button>
          <Popconfirm title="确定删除该任务？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const logColumns = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    {
      title: '触发方式', dataIndex: 'trigger_type', width: 90,
      render: (v: string) => v === 'manual' ? '手动' : '定时',
    },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (v: string) => <Tag color={statusMap[v]?.color || 'default'}>{statusMap[v]?.text || v}</Tag>,
    },
    { title: '实例', dataIndex: 'instance_id', width: 140, ellipsis: true },
    { title: '开始时间', dataIndex: 'started_at', width: 160 },
    { title: '耗时', dataIndex: 'duration_ms', width: 90, render: (v: number) => `${v || 0} ms` },
    {
      title: '输出',
      dataIndex: 'output_summary',
      ellipsis: true,
      render: (v: string) => (
        <Typography.Text copyable={{ text: v || '' }} ellipsis style={{ maxWidth: 220 }}>
          {v || '-'}
        </Typography.Text>
      ),
    },
    {
      title: '错误',
      dataIndex: 'error_message',
      ellipsis: true,
      render: (v: string) => (
        <Typography.Text type={v ? 'danger' : undefined} copyable={v ? { text: v } : false} ellipsis style={{ maxWidth: 220 }}>
          {v || '-'}
        </Typography.Text>
      ),
    },
  ]

  return (
    <Card
      title="定时任务"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增任务</Button>}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={jobs}
        loading={loading}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        scroll={{ x: 1300 }}
      />

      <Drawer
        title={editingId ? '编辑任务' : '新增任务'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={560}
        extra={
          <Space>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={saving} onClick={handleSave}>保存</Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="任务名称" rules={[{ required: true, message: '请输入任务名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="任务编码"
            rules={[
              { required: true, message: '请输入任务编码' },
              { pattern: /^[a-z][a-z0-9_]+$/, message: '以小写字母开头，仅支持小写字母、数字和下划线' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="job_key" label="任务处理器" rules={[{ required: true, message: '请选择任务处理器' }]}>
            <Select options={handlerOptions} placeholder="请选择" />
          </Form.Item>
          <Form.Item name="cron_expression" label="Cron 表达式" rules={[{ required: true, message: '请输入 cron 表达式' }]}>
            <Input prefix={<ClockCircleOutlined />} />
          </Form.Item>
          <Form.Item name="lock_at_most_seconds" label="锁最长持有秒数" rules={[{ required: true, message: '请输入锁最长持有秒数' }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="params" label="任务参数">
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item name="allow_manual" label="允许手动执行" valuePropName="checked">
            <Switch checkedChildren="允许" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={logJob ? `${logJob.name} - 执行日志` : '执行日志'}
        open={logDrawerOpen}
        onClose={() => setLogDrawerOpen(false)}
        width={960}
      >
        <Table
          rowKey="id"
          columns={logColumns}
          dataSource={logs}
          pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
          scroll={{ x: 1100 }}
          size="small"
        />
      </Drawer>
    </Card>
  )
}
