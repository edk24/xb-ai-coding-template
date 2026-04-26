import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Drawer, Form, Input, InputNumber, TreeSelect, Select,
  Popconfirm, Space, Tag, App,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { getPermissionsTreeApi, createPermissionApi, updatePermissionApi, deletePermissionApi } from '../../../api/permissions'
import type { Permission, CreatePermissionParams } from '../../../api/permissions'

const TYPE_MAP: Record<string, { color: string; label: string }> = {
  catalog: { color: 'blue', label: '目录' },
  menu: { color: 'green', label: '菜单' },
  button: { color: 'orange', label: '按钮' },
}

export default function Permissions() {
  const [treeData, setTreeData] = useState<Permission[]>([])
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [typeValue, setTypeValue] = useState<string>('menu')
  const [form] = Form.useForm()
  const { message } = App.useApp()

  const loadTree = () => {
    getPermissionsTreeApi().then((res) => setTreeData(res.data.data))
  }

  useEffect(() => { loadTree() }, [])

  const openCreate = (parentId = 0) => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({ parent_id: parentId, type: 'menu', sort: 0, hidden: 0, status: 1 })
    setTypeValue('menu')
    setDrawerOpen(true)
  }

  const openEdit = (record: Permission) => {
    setEditingId(record.id)
    form.setFieldsValue({
      parent_id: record.parent_id,
      name: record.name,
      type: record.type,
      route_path: record.route_path,
      component_path: record.component_path,
      permission_key: record.permission_key,
      sort: record.sort,
      hidden: record.hidden,
      status: record.status,
      remark: record.remark,
    })
    setTypeValue(record.type)
    setDrawerOpen(true)
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const params: CreatePermissionParams = {
        parent_id: values.parent_id || 0,
        name: values.name,
        type: values.type,
        route_path: values.type === 'menu' ? (values.route_path || '') : '',
        component_path: values.type === 'menu' ? (values.component_path || '') : '',
        permission_key: values.permission_key,
        sort: values.sort ?? 0,
        hidden: values.hidden,
        status: values.status,
        remark: values.remark || '',
      }
      if (editingId) {
        await updatePermissionApi(editingId, params)
        message.success('权限已更新')
      } else {
        await createPermissionApi(params)
        message.success('权限已创建')
      }
      setDrawerOpen(false)
      loadTree()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    await deletePermissionApi(id)
    message.success('权限已删除')
    loadTree()
  }

  const flattenTree = (items: Permission[], excludeId?: number): { value: number; title: string }[] =>
    items.flatMap((item) => {
      if (item.id === excludeId) return []
      return [
        { value: item.id, title: `${item.name} (${item.type})` },
        ...flattenTree(item.children || [], excludeId),
      ]
    })

  const columns: ColumnsType<Permission> = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: Permission) => (
        <Space>
          <span>{text}</span>
          {record.hidden === 1 && <Tag>隐藏</Tag>}
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => (
        <Tag color={TYPE_MAP[type]?.color}>{TYPE_MAP[type]?.label || type}</Tag>
      ),
    },
    {
      title: '权限标识',
      dataIndex: 'permission_key',
      key: 'permission_key',
      width: 200,
    },
    {
      title: '路由路径',
      dataIndex: 'route_path',
      key: 'route_path',
      render: (text: string) => (text ? <code>{text}</code> : '-'),
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
      render: (_: unknown, record: Permission) => (
        <Space size={4}>
          <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => openCreate(record.id)} />
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          {record.id > 16 && (
            <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="菜单权限"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate(0)}>新增</Button>}
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
        title={editingId ? '编辑权限' : '新增权限'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={480}
        extra={<Space><Button onClick={() => setDrawerOpen(false)}>取消</Button><Button type="primary" loading={saving} onClick={handleSave}>保存</Button></Space>}
      >
        <Form form={form} layout="vertical" onValuesChange={(changed) => { if (changed.type) setTypeValue(changed.type) }}>
          <Form.Item name="parent_id" label="父级节点">
            <TreeSelect
              treeData={flattenTree(treeData, editingId ?? undefined)}
              fieldNames={{ label: 'title', value: 'value', children: 'children' }}
              placeholder="顶级节点"
              allowClear
            />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
            <Select>
              <Select.Option value="catalog">目录</Select.Option>
              <Select.Option value="menu">菜单</Select.Option>
              <Select.Option value="button">按钮</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="permission_key" label="权限标识" rules={[{ required: true, message: '请输入权限标识' }]}>
            <Input placeholder="例：admin-users:create" />
          </Form.Item>
          {typeValue === 'menu' && (
            <>
              <Form.Item name="route_path" label="路由路径"><Input placeholder="/system/xxx" /></Form.Item>
              <Form.Item name="component_path" label="组件路径"><Input placeholder="system/xxx" /></Form.Item>
            </>
          )}
          <Form.Item name="sort" label="排序值"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="hidden" label="是否隐藏">
            <Select>
              <Select.Option value={0}>显示</Select.Option>
              <Select.Option value={1}>隐藏</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Drawer>
    </Card>
  )
}
