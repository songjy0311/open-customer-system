import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import AgentLogin from './pages/agent/AgentLogin'
import AgentDashboard from './pages/agent/AgentDashboard'
import VisitorChat from './pages/visitor/VisitorChat'
import './index.css'

const App = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/visitor" replace />} />
          <Route path="/agent/login" element={<AgentLogin />} />
          <Route path="/agent/dashboard" element={<AgentDashboard />} />
          <Route path="/visitor" element={<VisitorChat />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
