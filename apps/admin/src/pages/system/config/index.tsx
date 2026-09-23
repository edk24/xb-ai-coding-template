import { useEffect, useMemo, useState } from 'react'
import {
  Card, Tabs, Button, Drawer, Form, Input, InputNumber, Select, Radio, Checkbox,
  Switch, Tag, Table, Space, Popconfirm, App, ColorPicker, Empty,
} from 'antd'
import { PlusOutlined, SaveOutlined, SettingOutlined } from '@ant-design/icons'
import {
  getConfigItemsApi, createConfigItemApi, updateConfigItemApi, deleteConfigItemApi, batchSaveConfigItemsApi,
} from '../../../api/config'
import type {
  ConfigItem, ConfigItemType, CreateConfigItemParams,
} from '../../../api/config'
import { buildConfigSaveBatch } from './config-save'

const typeLabels: Record<ConfigItemType, string> = {
  input: '输入框', textarea: '文本域', number: '数字',
  select: '下拉选项', radio: '单选', checkbox: '多选',
  image: '单图', multi_image: '多图', rich_text: '富文本',
  switch: '开关', color: '颜色',
}

const typeColors: Record<ConfigItemType, string> = {
  input: 'blue', textarea: 'cyan', number: 'geekblue',
  select: 'purple', radio: 'magenta', checkbox: 'volcano',
  image: 'green', multi_image: 'lime', rich_text: 'orange',
  switch: 'gold', color: 'red',
}

/** 解析 options JSON → { label, value }[] */
function parseOptions(raw: string): { label: string; value: string }[] {
  try {
    const parsed = JSON.parse(raw || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export default function Config() {
  const { message } = App.useApp()

  const [items, setItems] = useState<ConfigItem[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState<string>('')
  const [form] = Form.useForm()

  // 监听存储驱动，动态显示/隐藏对应的云存储配置项
  const storageDriver = Form.useWatch('storage_driver', form)

  // --- Item management drawer ---
  const [itemMgrOpen, setItemMgrOpen] = useState(false)

  // --- Item edit drawer ---
  const [itemEditOpen, setItemEditOpen] = useState(false)
  const [editingItemId, setEditingItemId] = useState<number | null>(null)
  const [itemSaving, setItemSaving] = useState(false)
  const [itemForm] = Form.useForm()
  const [selectedType, setSelectedType] = useState<ConfigItemType>('input')

  // --- Group items by group_name for tabs ---
  const groups = useMemo(() => {
    const map = new Map<string, ConfigItem[]>()
    items.forEach((item) => {
      const g = item.group_name || '默认'
      if (!map.has(g)) map.set(g, [])
      map.get(g)!.push(item)
    })
    return Array.from(map.entries()).map(([name, list]) => ({
      name,
      items: list.sort((a, b) => a.sort - b.sort),
    }))
  }, [items])

  // --- Load all items ---
  const loadItems = () => {
    setLoading(true)
    getConfigItemsApi()
      .then((res) => {
        const data = res.data.data
        setItems(data)
        // Set form values
        const values: Record<string, any> = {}
        data.forEach((item) => {
          if (item.type === 'checkbox' || item.type === 'multi_image') {
            try { values[item.key] = JSON.parse(item.value || '[]') } catch { values[item.key] = [] }
          } else if (item.type === 'number') {
            values[item.key] = item.value !== '' ? Number(item.value) : undefined
          } else if (item.type === 'switch') {
            // 兼容早期批量保存写下的 true/false，统一按 1/0 回写
            values[item.key] = item.value === '1' || item.value === 'true'
          } else if (item.type === 'color' && item.value) {
            values[item.key] = item.value
          } else {
            values[item.key] = item.value ?? ''
          }
        })
        form.setFieldsValue(values)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadItems() }, [])

  // Set default active tab
  useEffect(() => {
    if (groups.length > 0 && !activeTab) {
      setActiveTab(groups[0].name)
    }
  }, [groups])

  // --- Batch save ---
  /**
   * 保存当前选项卡的配置。
   * 未打开过的选项卡不会渲染表单字段，全量提交会把它们的值写成空串、清空其他分组配置，
   * 因此这里只校验并提交当前分组的配置项。
   */
  const handleSave = async (groupItems: ConfigItem[]) => {
    const fieldKeys = groupItems.map((item) => item.key)
    await form.validateFields(fieldKeys)
    const values = form.getFieldsValue(fieldKeys) as Record<string, unknown>
    setSaving(true)
    try {
      await batchSaveConfigItemsApi(buildConfigSaveBatch(groupItems, values))
      message.success('配置已保存')
    } finally {
      setSaving(false)
    }
  }

  // --- Item CRUD ---
  const openCreateItem = () => {
    setEditingItemId(null)
    itemForm.resetFields()
    itemForm.setFieldsValue({ status: 1, sort: 0, type: 'input', group_name: activeTab || '' })
    setSelectedType('input')
    setItemEditOpen(true)
  }

  const openEditItem = (record: ConfigItem) => {
    setEditingItemId(record.id)
    itemForm.setFieldsValue({
      group_name: record.group_name, name: record.name, key: record.key,
      value: record.value, type: record.type, options: record.options,
      sort: record.sort, status: record.status, remark: record.remark,
    })
    setSelectedType(record.type as ConfigItemType)
    setItemEditOpen(true)
  }

  const handleItemSave = async () => {
    const values = await itemForm.validateFields()
    setItemSaving(true)
    try {
      const params: CreateConfigItemParams = {
        name: values.name, group_name: values.group_name ?? '',
        key: values.key, value: values.value ?? '', type: values.type,
        options: values.options ?? '', sort: values.sort ?? 0,
        status: values.status ?? 1, remark: values.remark ?? '',
      }
      if (editingItemId) {
        await updateConfigItemApi(editingItemId, params)
        message.success('配置项已更新')
      } else {
        await createConfigItemApi(params)
        message.success('配置项已创建')
      }
      setItemEditOpen(false)
      loadItems()
    } finally { setItemSaving(false) }
  }

  const handleDeleteItem = async (id: number) => {
    await deleteConfigItemApi(id)
    message.success('配置项已删除')
    loadItems()
  }

  // --- Dynamic validation rules ---
  const getFieldRules = (item: ConfigItem) => {
    const rules: any[] = []
    if (item.key === 'site_name') {
      rules.push({ required: true, message: `请输入${item.name}` })
      rules.push({ max: 32, message: `${item.name}最多32个字符` })
    } else if (item.key === 'site_version') {
      rules.push({ required: true, message: `请输入${item.name}` })
    }
    return rules
  }

  // --- Render form control by type ---
  const renderFormControl = (item: ConfigItem) => {
    const opts = parseOptions(item.options)
    switch (item.type) {
      case 'textarea':
        return <Input.TextArea rows={4} />
      case 'number':
        return <InputNumber style={{ width: '100%' }} />
      case 'select':
        return (
          <Select placeholder="请选择" allowClear>
            {opts.map((o) => <Select.Option key={o.value} value={o.value}>{o.label}</Select.Option>)}
          </Select>
        )
      case 'radio':
        return <Radio.Group options={opts.map((o) => ({ label: o.label, value: o.value }))} />
      case 'checkbox':
        return <Checkbox.Group options={opts.map((o) => ({ label: o.label, value: o.value }))} />
      case 'switch':
        return <Switch />
      case 'color':
        return <ColorPicker />
      case 'image':
        return <Input placeholder="请输入图片URL" />
      case 'multi_image':
        return <Input.TextArea rows={3} placeholder="请输入图片URL，每行一个" />
      case 'rich_text':
        return <Input.TextArea rows={6} />
      default:
        // 密码类配置不回显明文
        if (item.key.endsWith('_password')) {
          return <Input.Password autoComplete="new-password" />
        }
        return <Input />
    }
  }

  // --- Item mgr drawer columns ---
  const itemColumns = [
    { title: '名称', dataIndex: 'name', width: 120 },
    { title: '键名', dataIndex: 'key', width: 140, render: (v: string) => <Tag>{v}</Tag> },
    { title: '分组', dataIndex: 'group_name', width: 100, render: (v: string) => v || <Tag>默认</Tag> },
    { title: '类型', dataIndex: 'type', width: 100, render: (v: ConfigItemType) => <Tag color={typeColors[v]}>{typeLabels[v]}</Tag> },
    { title: '排序', dataIndex: 'sort', width: 60 },
    { title: '说明', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作', width: 120,
      render: (_: unknown, record: ConfigItem) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEditItem(record)}>编辑</Button>
          <Popconfirm title="确定删除该配置项？" onConfirm={() => handleDeleteItem(record.id)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // --- Tab items ---
  const tabItems = useMemo(() =>
    groups.map((g) => ({
      key: g.name,
      label: g.name,
      children: (
        <div style={{ marginTop: 16 }}>
          <Form form={form} layout="vertical">
            {g.items.map((item) => {
              // 存储配置项：根据 storage_driver 动态显示对应的云存储参数
              const isStorageGroup = g.name === '存储'
              const isStorageDriverField = item.key === 'storage_driver'
              const hideStorageField = isStorageGroup && !isStorageDriverField
                && storageDriver !== undefined
                && (storageDriver === 'local' || !item.key.startsWith(storageDriver + '_'))
              return (
                <div key={item.id} style={hideStorageField ? { display: 'none' } : undefined}>
                  <Form.Item
                    name={item.key}
                    label={<span>{item.name} <Tag style={{ fontSize: 11 }}>{item.key}</Tag></span>}
                    tooltip={item.remark || undefined}
                    valuePropName={item.type === 'switch' ? 'checked' : undefined}
                    rules={getFieldRules(item)}
                  >
                    {renderFormControl(item)}
                  </Form.Item>
                </div>
              )
            })}
          </Form>
          {g.items.length > 0 && (
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={() => handleSave(g.items)} size="large">
              保存修改
            </Button>
          )}
        </div>
      ),
    })),
    [groups, saving, storageDriver],
  )

  return (
    <Card
      title="系统配置"
      extra={
        <Button onClick={() => { setItemMgrOpen(true); loadItems() }} icon={<SettingOutlined />}>
          管理配置项
        </Button>
      }
    >
      {groups.length === 0 ? (
        <Empty description="暂无配置项，请先添加配置项" />
      ) : (
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
      )}

      {/* Item Management Drawer */}
      <Drawer
        title="配置项管理"
        open={itemMgrOpen}
        onClose={() => setItemMgrOpen(false)}
        width={640}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateItem}>新增配置项</Button>
        }
      >
        <Table rowKey="id" columns={itemColumns} dataSource={items} loading={loading} size="small" pagination={false} />
      </Drawer>

      {/* Item Edit Drawer */}
      <Drawer
        title={editingItemId ? '编辑配置项' : '新增配置项'}
        open={itemEditOpen}
        onClose={() => setItemEditOpen(false)}
        width={520}
        extra={
          <Space>
            <Button onClick={() => setItemEditOpen(false)}>取消</Button>
            <Button type="primary" loading={itemSaving} onClick={handleItemSave}>保存</Button>
          </Space>
        }
      >
        <Form form={itemForm} layout="vertical" onValuesChange={(changed) => { if (changed.type) setSelectedType(changed.type) }}>
          <Form.Item name="group_name" label="分组" rules={[{ required: true, message: '请输入分组名称' }]}>
            <Input placeholder="例如：基本设置" />
          </Form.Item>
          <Form.Item name="name" label="配置项名称" rules={[{ required: true, message: '请输入配置项名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="key"
            label="键名"
            rules={[
              { required: true, message: '请输入键名' },
              { pattern: /^[a-z][a-zA-Z0-9_]+$/, message: '以小写字母开头，仅支持字母、数字和下划线' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="type" label="字段类型" rules={[{ required: true }]}>
            <Select>
              {(Object.entries(typeLabels) as [ConfigItemType, string][]).map(([value, label]) => (
                <Select.Option key={value} value={value}>{label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          {(selectedType === 'select' || selectedType === 'radio' || selectedType === 'checkbox') && (
            <Form.Item
              name="options"
              label="选项配置"
              tooltip={'JSON 数组格式，如 [{"label":"选项A","value":"a"},{"label":"选项B","value":"b"}]'}
              rules={[{ validator: (_, value) => { if (!value) return Promise.resolve(); try { JSON.parse(value); return Promise.resolve() } catch { return Promise.reject('JSON 格式不正确') } } }]}
            >
              <Input.TextArea rows={4} placeholder='[{"label":"选项A","value":"a"},{"label":"选项B","value":"b"}]' />
            </Form.Item>
          )}
          <Form.Item name="value" label="默认值"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="sort" label="排序"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="remark" label="说明"><Input.TextArea rows={2} /></Form.Item>
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
