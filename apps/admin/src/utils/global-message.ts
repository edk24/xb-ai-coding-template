import { useEffect } from 'react'
import { App } from 'antd'
import type { MessageInstance } from 'antd/es/message/interface'

let instance: MessageInstance | null = null

export function useInitMessage() {
  const { message } = App.useApp()
  useEffect(() => {
    instance = message
  }, [message])
}

export function messageWarning(content: string) {
  instance?.warning(content)
}

export function messageError(content: string) {
  instance?.error(content)
}

export function messageSuccess(content: string) {
  instance?.success(content)
}
