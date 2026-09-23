import { ConfigProvider, theme, App as AntApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { HashRouter } from 'react-router-dom'
import { useThemeStore } from './store/theme'
import { useInitMessage } from './utils/global-message'
import AppRouter from './router'

function MessageInitializer() {
  useInitMessage()
  return null
}

export default function App() {
  const mode = useThemeStore((s) => s.mode)

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: mode === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
      }}
    >
      <AntApp>
        <HashRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <MessageInitializer />
          <AppRouter />
        </HashRouter>
      </AntApp>
    </ConfigProvider>
  )
}
