/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后台管理端 API 基础地址，例如 /admin-api 或 https://api.example.com/admin-api */
  readonly VITE_API_BASE_URL?: string
  /** 后台管理端页面和静态资源部署前缀，例如 / 或 /backend/ */
  readonly VITE_ADMIN_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'cos-js-sdk-v5' {
  const COS: any
  export default COS
}

declare module 'ali-oss' {
  const OSS: any
  export default OSS
}

declare module 'md5' {
  export default function md5(value: string): string
}
