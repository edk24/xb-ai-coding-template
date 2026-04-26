import { useEffect, useState, useCallback } from 'react'
import {
  Card, Table, Button, Tag, Space, Popconfirm, App, Select, Modal, Upload,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import { UploadOutlined, DeleteOutlined, PaperClipOutlined, LinkOutlined } from '@ant-design/icons'
import {
  getAttachmentsApi,
  getAttachmentsConfigApi,
  getCredentialsApi,
  recordAttachmentApi,
  uploadAttachmentApi,
  deleteAttachmentApi,
} from '../../api/attachments'
import type { Attachment, AttachmentConfig } from '../../api/attachments'

const STORAGE_TYPE_MAP: Record<string, { color: string; label: string }> = {
  local: { color: 'blue', label: '本地' },
  cos: { color: 'green', label: '腾讯云 COS' },
  oss: { color: 'orange', label: '阿里云 OSS' },
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i]
}

function getFileExt(name: string): string {
  return name.includes('.') ? name.split('.').pop()!.toLowerCase() : ''
}

const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']

export default function Attachments() {
  const [list, setList] = useState<Attachment[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [storageType, setStorageType] = useState('local')
  const [typeFilter, setTypeFilter] = useState<string>('')
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([])
  const { message } = App.useApp()

  const loadList = useCallback(() => {
    setLoading(true)
    getAttachmentsApi({ page, page_size: pageSize, storage_type: typeFilter || undefined })
      .then((res) => {
        setList(res.data.data.items)
        setTotal(res.data.data.total)
      })
      .finally(() => setLoading(false))
  }, [page, pageSize, typeFilter])

  useEffect(() => { loadList() }, [loadList])

  useEffect(() => {
    getAttachmentsConfigApi().then((res) => {
      setStorageType((res.data.data as AttachmentConfig).storage_type)
    }).catch(() => { /* 默认 local */ })
  }, [])

  const handleDelete = async (id: number) => {
    await deleteAttachmentApi(id)
    message.success('附件已删除')
    loadList()
  }

  /** 本地上传 */
  const handleLocalUpload = async (file: File): Promise<boolean> => {
    setUploading(true)
    try {
      await uploadAttachmentApi(file)
      return true
    } catch (e: any) {
      message.error(e?.response?.data?.message || '上传失败')
      return false
    } finally {
      setUploading(false)
    }
  }

  /** COS/OSS 前端直传 */
  const handleCloudUpload = async (file: File, cloudType: string): Promise<boolean> => {
    setUploading(true)
    try {
      const credRes = await getCredentialsApi(cloudType)
      const { credentials, region, bucket, path_prefix, cdn_url } = credRes.data.data

      const ext = getFileExt(file.name)
      const filename = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}.${ext}`
      const objectKey = `${path_prefix}/${filename}`

      if (cloudType === 'cos') {
        let COS: any
        try {
          COS = (await import('cos-js-sdk-v5')).default
        } catch {
          message.error('请先安装依赖: npm install cos-js-sdk-v5')
          return false
        }
        const cos = new COS({
          SecretId: credentials.tmpSecretId,
          SecretKey: credentials.tmpSecretKey,
          XCosSecurityToken: credentials.sessionToken,
        })
        await cos.putObject({ Bucket: bucket, Region: region, Key: objectKey, Body: file })
      } else if (cloudType === 'oss') {
        let OSS: any
        try {
          OSS = (await import('ali-oss')).default
        } catch {
          message.error('请先安装依赖: npm install ali-oss')
          return false
        }
        const client = new OSS({
          region, bucket,
          accessKeyId: credentials.tmpSecretId,
          accessKeySecret: credentials.tmpSecretKey,
          stsToken: credentials.sessionToken,
        })
        await client.put(objectKey, file)
      }

      const fileUrl = cdn_url
        ? `${cdn_url.replace(/\/+$/, '')}/${objectKey}`
        : `${path_prefix}/${filename}`

      await recordAttachmentApi({
        name: file.name,
        size: file.size,
        mime_type: file.type || '',
        storage_type: cloudType,
        path: objectKey,
        url: fileUrl,
        cdn_url,
      })
      return true
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '上传失败')
      return false
    } finally {
      setUploading(false)
    }
  }

  const handleUpload = async (file: File): Promise<boolean> => {
    if (storageType === 'local') return handleLocalUpload(file)
    return handleCloudUpload(file, storageType)
  }

  const uploadProps: UploadProps = {
    multiple: true,
    fileList: uploadFileList,
    beforeUpload: (file) => {
      handleUpload(file as File).then((success) => {
        const newFile: UploadFile = {
          uid: file.uid,
          name: file.name,
          size: file.size,
          status: success ? 'done' : 'error',
        }
        setUploadFileList((prev) => [...prev, newFile])
        if (success) loadList()
      })
      return false
    },
    onRemove: (file) => {
      setUploadFileList((prev) => prev.filter((f) => f.uid !== file.uid))
    },
  }

  const columns: ColumnsType<Attachment> = [
    {
      title: '文件名',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
      render: (text: string, record: Attachment) => {
        const ext = getFileExt(text)
        const isImage = IMAGE_EXTS.includes(ext)
        return (
          <Space>
            {isImage && record.url ? (
              <img src={record.url} alt={text} style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 4 }} />
            ) : (
              <PaperClipOutlined style={{ fontSize: 20 }} />
            )}
            <a href={record.url} target="_blank" rel="noopener noreferrer" style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }}>
              {text}
            </a>
          </Space>
        )
      },
    },
    { title: '大小', dataIndex: 'size', key: 'size', width: 100, render: (size: number) => formatSize(size) },
    { title: 'MIME 类型', dataIndex: 'mime_type', key: 'mime_type', width: 130, ellipsis: true, render: (t: string) => t || '-' },
    {
      title: '存储方式', dataIndex: 'storage_type', key: 'storage_type', width: 130,
      render: (type: string) => <Tag color={STORAGE_TYPE_MAP[type]?.color}>{STORAGE_TYPE_MAP[type]?.label || type}</Tag>,
    },
    { title: '上传时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
    {
      title: '操作', key: 'action', width: 150,
      render: (_: unknown, record: Attachment) => (
        <Space size={4}>
          <Button type="link" size="small" icon={<LinkOutlined />} onClick={() => {
            navigator.clipboard.writeText(record.url).then(() => message.success('链接已复制'))
          }}>复制链接</Button>
          <Popconfirm title="确定删除该附件？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card title="附件中心">
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <span>存储方式：</span>
          <Select value={typeFilter} onChange={(v) => { setTypeFilter(v); setPage(1) }} style={{ width: 160 }} allowClear>
            <Select.Option value="">全部</Select.Option>
            <Select.Option value="local">本地</Select.Option>
            <Select.Option value="cos">腾讯云 COS</Select.Option>
            <Select.Option value="oss">阿里云 OSS</Select.Option>
          </Select>
          <Button type="primary" icon={<UploadOutlined />} onClick={() => {
            setUploadFileList([])
            setUploadModalOpen(true)
          }}>上传附件</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={list}
          rowKey="id"
          loading={loading}
          size="middle"
          pagination={{
            current: page,
            total,
            pageSize,
            onChange: (p) => setPage(p),
            showSizeChanger: false,
            showTotal: (t) => `共 ${t} 条`,
          }}
        />
      </Card>

      <Modal
        title="上传附件"
        open={uploadModalOpen}
        onCancel={() => setUploadModalOpen(false)}
        footer={null}
        width={520}
        destroyOnClose
      >
        <div style={{ padding: '24px 0' }}>
          <div style={{ marginBottom: 12, color: '#888' }}>
            当前存储方式：{STORAGE_TYPE_MAP[storageType]?.label || storageType}
            {storageType !== 'local' && '（前端直传）'}
          </div>
          <Upload.Dragger {...uploadProps}>
            <p className="ant-upload-drag-icon"><UploadOutlined /></p>
            <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
            <p className="ant-upload-hint">支持多文件上传</p>
          </Upload.Dragger>
        </div>
      </Modal>
    </Card>
  )
}
