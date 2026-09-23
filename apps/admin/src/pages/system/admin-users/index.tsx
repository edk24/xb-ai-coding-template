import { useEffect, useState } from 'react'
import {
  Table, Button, Drawer, Form, Input, Select, TreeSelect,
  Switch, Modal, Popconfirm, Tag, Avatar, Space, Card, App,
} from 'antd'
import { PlusOutlined, UserOutlined } from '@ant-design/icons'
import {
  getAdminUsersApi, createAdminUserApi, updateAdminUserApi,
  deleteAdminUserApi, resetPasswordApi,
} from '../../../api/admin-users'
import type { AdminUser, CreateAdminUserParams, UpdateAdminUserParams } from '../../../api/admin-users'
import { getRolesApi } from '../../../api/roles'
import type { Role } from '../../../api/roles'
import { getDepartmentsTreeApi } from '../../../api/departments'
import type { Department } from '../../../api/departments'
import AvatarUpload from '../../../components/AvatarUpload'

export default function AdminUsers() {
  const [list, setList] = useState<AdminUser[]>([])
  const [roles, setRoles] = useState<Role[]>([])
  const [deptTree, setDeptTree] = useState<Department[]>([])
  const [loading, setLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()
  const { message } = App.useApp()

  const loadList = () => {
    setLoading(true)
    getAdminUsersApi().then((res) => setList(res.data.data)).finally(() => setLoading(false))
  }

  const loadMeta = () => {
    getRolesApi().then((res) => setRoles(res.data.data))
    getDepartmentsTreeApi().then((res) => {
      setDeptTree(res.data.data)
      form.setFieldsValue({ department_id: undefined })
    })
  }

  useEffect(() => { loadList(); loadMeta() }, [])

  const openCreate = () => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({ status: 1 })
    setDrawerOpen(true)
  }

  const openEdit = (record: AdminUser) => {
    setEditingId(record.id)
    form.setFieldsValue({
      department_id: record.department_id,
      username: record.username,
      nickname: record.nickname,
      avatar: record.avatar,
      phone: record.phone,
      email: record.email,
      status: record.status,
      remark: record.remark,
      role_ids: record.role_ids,
    })
    setDrawerOpen(true)
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      if (editingId) {
        const params: UpdateAdminUserParams = {
          department_id: values.department_id,
          username: values.username,
          nickname: values.nickname,
          avatar: values.avatar || '',
          phone: values.phone || '',
          email: values.email || '',
          status: values.status,
          remark: values.remark || '',
          role_ids: values.role_ids || [],
        }
        await updateAdminUserApi(editingId, params)
        message.success('管理员已更新')
      } else {
        const params: CreateAdminUserParams = {
          department_id: values.department_id,
          username: values.username,
          nickname: values.nickname,
          avatar: values.avatar || '',
          phone: values.phone || '',
          email: values.email || '',
          password: values.password,
          status: values.status,
          remark: values.remark || '',
          role_ids: values.role_ids || [],
        }
        await createAdminUserApi(params)
        message.success('管理员已创建')
      }
      setDrawerOpen(false)
      loadList()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteAdminUserApi(id)
    message.success('管理员已删除')
    loadList()
  }

  const handleResetPassword = (id: number) => {
    let newPassword = ''
    Modal.confirm({
      title: '重置密码',
      content: (
        <Input.Password
          placeholder="留空则使用默认密码 123456"
          onChange={(e) => { newPassword = e.target.value }}
          style={{ marginTop: 8 }}
        />
      ),
      onOk: async () => {
        await resetPasswordApi(id, newPassword ? { new_password: newPassword } : {})
        message.success('密码已重置')
      },
    })
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    {
      title: '头像', dataIndex: 'avatar', width: 56,
      render: (avatar: string) => <Avatar size={32} src={avatar || undefined} icon={<UserOutlined />} />,
    },
    { title: '登录账号', dataIndex: 'username', width: 120 },
    { title: '昵称', dataIndex: 'nickname', width: 120 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '邮箱', dataIndex: 'email', width: 160, ellipsis: true },
    { title: '部门', dataIndex: 'department_name', width: 100 },
    {
      title: '角色', dataIndex: 'roles', width: 160,
      render: (roles: string[]) => (
        <Space size={4} wrap>{roles.map((r) => <Tag key={r}>{r}</Tag>)}</Space>
      ),
    },
    {
      title: '状态', dataIndex: 'status', width: 72,
      render: (s: number) => s === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
    },
    {
      title: '操作', width: 200,
      render: (_: unknown, record: AdminUser) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
          {record.is_super !== 1 && (
            <>
              <Button type="link" size="small" onClick={() => handleResetPassword(record.id)}>重置密码</Button>
              <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
                <Button type="link" size="small" danger>删除</Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  const flattenTree = (items: Department[]): { value: number; title: string }[] =>
    items.flatMap((item) => [
      { value: item.id, title: item.name },
      ...flattenTree(item.children || []),
    ])

  return (
    <Card title="管理员管理" extra={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增</Button>}>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        scroll={{ x: 1000 }}
      />
      <Drawer
        title={editingId ? '编辑管理员' : '新增管理员'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={480}
        extra={<Space><Button onClick={() => setDrawerOpen(false)}>取消</Button><Button type="primary" loading={saving} onClick={handleSave}>保存</Button></Space>}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="department_id" label="所属部门" rules={[{ required: true, message: '请选择部门' }]}>
            <TreeSelect treeData={deptTree} fieldNames={{ label: 'name', value: 'id', children: 'children' }} placeholder="选择部门" />
          </Form.Item>
          <Form.Item name="username" label="登录账号" rules={[{ required: true, message: '请输入登录账号' }]}>
            <Input disabled={!!editingId} />
          </Form.Item>
          <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
            <Input />
          </Form.Item>
          {!editingId && (
            <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少 6 位' }]}>
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item name="role_ids" label="绑定角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select mode="multiple" placeholder="选择角色" options={roles.map((r) => ({ value: r.id, label: r.name }))} />
          </Form.Item>
          <Form.Item name="phone" label="手机号"><Input /></Form.Item>
          <Form.Item name="email" label="邮箱"><Input /></Form.Item>
          <Form.Item name="avatar" label="头像"><AvatarUpload /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked" getValueFromEvent={(e) => e ? 1 : 0}>
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Drawer>
    </Card>
  )
}
