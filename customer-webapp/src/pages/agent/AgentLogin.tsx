import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import api from '../../utils/api'

interface LoginForm {
  username: string
  password: string
}

const AgentLogin: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (values: LoginForm) => {
    setLoading(true)
    try {
      const res = await api.post('/auth/login', values)
      if (res.code === 200) {
        localStorage.setItem('agentId', res.data.id)
        localStorage.setItem('agentNickname', res.data.nickname)
        localStorage.setItem('agentUsername', res.data.username)
        message.success('登录成功')
        navigate('/agent/dashboard')
      } else {
        message.error(res.message || '登录失败')
      }
    } catch (error) {
      message.error('登录失败，请检查网络')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <Card
        style={{ width: 400, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}
        hoverable
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <h1 style={{ fontSize: 24, fontWeight: 'bold', color: '#333' }}>客服工作台</h1>
          <p style={{ color: '#888', marginTop: 8 }}>请登录您的账号</p>
        </div>
        <Form
          name="login"
          onFinish={handleSubmit}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={{ height: 44 }}
            >
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default AgentLogin
