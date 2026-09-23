import { useState } from 'react'
import { App, Upload } from 'antd'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import { PlusOutlined } from '@ant-design/icons'
import { uploadAttachmentApi } from '../api/attachments'

interface AvatarUploadProps {
  value?: string
  onChange?: (url: string) => void
}

function buildAvatarFile(url?: string): UploadFile[] {
  if (!url) return []
  // AntD Upload 展示需要 fileList，表单实际保存的仍然只是头像 URL 字符串。
  return [{
    uid: 'avatar',
    name: '头像',
    status: 'done',
    url,
    thumbUrl: url,
  }]
}

function isImageFile(file: File): boolean {
  return file.type.startsWith('image/')
}

export default function AvatarUpload({ value, onChange }: AvatarUploadProps) {
  const [uploading, setUploading] = useState(false)
  const { message } = App.useApp()

  // 上传成功后只把后端返回的访问 URL 写回 Form 字段，保存接口仍沿用 avatar 字符串。
  const uploadAvatar = async (file: File) => {
    if (!isImageFile(file)) {
      message.error('请选择图片文件')
      return
    }

    setUploading(true)
    try {
      const res = await uploadAttachmentApi(file)
      onChange?.(res.data.data.url)
      message.success('头像已上传')
    } catch (e: any) {
      message.error(e?.response?.data?.message || '头像上传失败')
    } finally {
      setUploading(false)
    }
  }

  const uploadProps: UploadProps = {
    accept: 'image/*',
    fileList: buildAvatarFile(value),
    listType: 'picture-circle',
    maxCount: 1,
    showUploadList: { showPreviewIcon: true, showRemoveIcon: true },
    beforeUpload: (file) => {
      uploadAvatar(file as File)
      return Upload.LIST_IGNORE
    },
    onRemove: () => {
      onChange?.('')
      return true
    },
  }

  return (
    <Upload {...uploadProps}>
      {!value && (
        <button type="button" disabled={uploading} style={{ border: 0, background: 'none' }}>
          <PlusOutlined />
          <div style={{ marginTop: 8 }}>{uploading ? '上传中' : '上传头像'}</div>
        </button>
      )}
    </Upload>
  )
}
