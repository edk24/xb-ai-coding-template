import { useEffect, useState, useCallback } from 'react'
import {
  Card, Table, Button, Drawer, Form, Input, InputNumber, TreeSelect,
  Popconfirm, Space, Tag, App,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { getDepartmentsTreeApi, createDepartmentApi, updateDepartmentApi, deleteDepartmentApi } from '../../../api/departments'
import type { Department, CreateDepartmentParams } from '../../../api/departments'

export default function Departments() {
  const [treeData, setTreeData] = useState<Department[]>([])
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()
  const { message } = App.useApp()

  const loadTree = useCallback(() => {
    getDepartmentsTreeApi().then((res) => setTreeData(res.data.data))
  }, [])

  useEffect(() => { loadTree() }, [loadTree])

  const openCreate = (parentId = 0) => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({ parent_id: parentId, sort: 0, status: 1 })
    setDrawerOpen(true)
  }

  const openEdit = (record: Department) => {
    setEditingId(record.id)
    form.setFieldsValue({
      parent_id: record.parent_id,
      name: record.name,
      leader: record.leader,
      phone: record.phone,
      sort: record.sort,
      status: record.status,
      remark: record.remark,
    })
    setDrawerOpen(true)
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const params: CreateDepartmentParams = {
        parent_id: values.parent_id || 0,
        name: values.name,
        leader: values.leader || '',
        phone: values.phone || '',
        sort: values.sort ?? 0,
        status: values.status,
        remark: values.remark || '',
      }
      if (editingId) {
        await updateDepartmentApi(editingId, params)
        message.success('部门已更新')
      } else {
        await createDepartmentApi(params)
        message.success('部门已创建')
      }
      setDrawerOpen(false)
      loadTree()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteDepartmentApi(id)
    message.success('部门已删除')
    loadTree()
  }

  // Flatten tree for TreeSelect (excluding the current node if editing)
  const flattenTree = (items: Department[], excludeId?: number): { value: number; title: string }[] =>
    items.flatMap((item) => {
      if (item.id === excludeId) return []
      return [
        { value: item.id, title: item.name },
        ...flattenTree(item.children || [], excludeId),
      ]
    })

  const columns: ColumnsType<Department> = [
    {
      title: '部门名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <span style={{ fontWeight: 500 }}>{text}</span>,
    },
    {
      title: '负责人',
      dataIndex: 'leader',
      key: 'leader',
    },
    {
      title: '联系电话',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>{status === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_: unknown, record: Department) => (
        <Space size={4}>
          <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => openCreate(record.id)} />
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          {record.id !== 1 && (
            <Popconfirm title="确定删除？子部门和管理员将被校验" onConfirm={() => handleDelete(record.id)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="部门管理"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate(0)}>新增根部门</Button>}
    >
      <Table
        columns={columns}
        dataSource={treeData}
        rowKey="id"
        defaultExpandAllRows
        pagination={false}
        size="middle"
      />
      <Drawer
        title={editingId ? '编辑部门' : '新增部门'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={480}
        extra={<Space><Button onClick={() => setDrawerOpen(false)}>取消</Button><Button type="primary" loading={saving} onClick={handleSave}>保存</Button></Space>}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="parent_id" label="上级部门">
            <TreeSelect
              treeData={flattenTree(treeData, editingId ?? undefined)}
              fieldNames={{ label: 'title', value: 'value', children: 'children' }}
              placeholder="顶级部门"
              allowClear
            />
          </Form.Item>
          <Form.Item name="name" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="leader" label="负责人"><Input /></Form.Item>
          <Form.Item name="phone" label="联系电话"><Input /></Form.Item>
          <Form.Item name="sort" label="排序值"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="status" label="状态">
            <TreeSelect
              treeData={[
                { value: 1, title: '启用' },
                { value: 0, title: '禁用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Drawer>
    </Card>
  )
}
