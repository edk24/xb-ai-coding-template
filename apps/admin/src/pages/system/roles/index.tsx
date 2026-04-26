import { useEffect, useState } from 'react'
import {
  Table, Button, Drawer, Form, Input, Tree, Select,
  Popconfirm, Tag, Space, Card, App,
} from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { TreeProps } from 'antd'
import { getRolesApi, createRoleApi, updateRoleApi, deleteRoleApi } from '../../../api/roles'
import type { Role, CreateRoleParams } from '../../../api/roles'
import { getPermissionsTreeApi } from '../../../api/permissions'
import type { Permission } from '../../../api/permissions'

export default function Roles() {
  const [list, setList] = useState<Role[]>([])
  const [permTree, setPermTree] = useState<Permission[]>([])
  const [loading, setLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [checkedKeys, setCheckedKeys] = useState<number[]>([])
  const [form] = Form.useForm()
  const { message } = App.useApp()

  const loadList = () => {
    setLoading(true)
    getRolesApi().then((res) => setList(res.data.data)).finally(() => setLoading(false))
  }

  const loadPermTree = () => {
    getPermissionsTreeApi().then((res) => setPermTree(res.data.data))
  }

  useEffect(() => { loadList(); loadPermTree() }, [])

  const openCreate = () => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({ status: 1 })
    setCheckedKeys([])
    setDrawerOpen(true)
  }

  const openEdit = (record: Role) => {
    setEditingId(record.id)
    form.setFieldsValue({
      name: record.name,
      code: record.code,
      status: record.status,
      remark: record.remark,
    })
    setCheckedKeys(record.permission_ids)
    setDrawerOpen(true)
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const params: CreateRoleParams = {
        name: values.name,
        code: values.code,
        status: values.status,
        remark: values.remark || '',
        permission_ids: checkedKeys,
      }
      if (editingId) {
        await updateRoleApi(editingId, params)
        message.success('角色已更新')
      } else {
        await createRoleApi(params)
        message.success('角色已创建')
      }
      setDrawerOpen(false)
      loadList()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteRoleApi(id)
    message.success('角色已删除')
    loadList()
  }

  const flattenKeys = (items: Permission[]): number[] =>
    items.flatMap((item) => [item.id, ...flattenKeys(item.children || [])])

  // Build Ant Design TreeData from permission tree
  const buildTreeData = (items: Permission[]): TreeProps['treeData'] =>
    items.map((item) => ({
      key: item.id,
      title: `${item.name} (${item.permission_key})`,
      children: item.children ? buildTreeData(item.children) : undefined,
    }))

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    { title: '角色名称', dataIndex: 'name', width: 140 },
    { title: '角色编码', dataIndex: 'code', width: 140 },
    {
      title: '状态', dataIndex: 'status', width: 72,
      render: (s: number) => s === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
    },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作', width: 160,
      render: (_: unknown, record: Role) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
          {record.id !== 1 && (
            <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
              <Button type="link" size="small" danger>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card title="角色管理" extra={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增</Button>}>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
      />
      <Drawer
        title={editingId ? '编辑角色' : '新增角色'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={520}
        extra={<Space><Button onClick={() => setDrawerOpen(false)}>取消</Button><Button type="primary" loading={saving} onClick={handleSave}>保存</Button></Space>}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="status" label="状态">
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="菜单权限">
            <Tree
              checkable
              defaultExpandAll
              checkedKeys={checkedKeys}
              onCheck={(keys) => setCheckedKeys(keys as number[])}
              treeData={buildTreeData(permTree)}
            />
          </Form.Item>
        </Form>
      </Drawer>
    </Card>
  )
}
