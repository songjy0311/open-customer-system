import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Layout, Menu, List, Card, Button, Input, Badge, Tag, Space, message, Modal } from 'antd'
import {
  MessageOutlined,
  TeamOutlined,
  LogoutOutlined,
  SendOutlined,
  CustomerServiceOutlined
} from '@ant-design/icons'
import api from '../utils/api'
import WebSocketClient from '../utils/websocket'

const { Header, Sider, Content } = Layout
const { TextArea } = Input

interface Conversation {
  id: number
  visitorId: string
  visitorNickname: string
  status: number
  queuePosition: number
  createdAt: string
}

interface Message {
  id: number
  conversationId: number
  senderType: number
  senderId: string
  senderName: string
  content: string
  messageType: number
  isRead: number
  createdAt: string
}

const AgentDashboard: React.FC = () => {
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const [queueList, setQueueList] = useState<Conversation[]>([])
  const [activeConversations, setActiveConversations] = useState<Conversation[]>([])
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [inputValue, setInputValue] = useState('')
  const [agentStatus, setAgentStatus] = useState(1)
  const wsRef = useRef<WebSocketClient | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const agentId = localStorage.getItem('agentId')
  const agentNickname = localStorage.getItem('agentNickname')

  useEffect(() => {
    if (!agentId) {
      navigate('/agent/login')
      return
    }
    initWebSocket()
    loadData()
    return () => {
      wsRef.current?.close()
    }
  }, [])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const initWebSocket = () => {
    const wsUrl = import.meta.env.PROD
      ? `wss://${location.host}/ws/agent`
      : `ws://localhost:8080/ws/agent`
    const ws = new WebSocketClient(wsUrl)
    ws.connect()
      .then(() => {
        ws.send({
          type: 'AGENT_LOGIN',
          agentId: Number(agentId),
          nickname: agentNickname
        })
      })
      .catch(console.error)

    ws.on('QUEUE_UPDATE', (data) => {
      setQueueList(data.data || [])
    })

    ws.on('NEW_MESSAGE', (data) => {
      if (selectedConversation?.id === data.data.conversationId) {
        setMessages((prev) => [...prev, data.data])
      }
    })

    wsRef.current = ws
  }

  const loadData = async () => {
    try {
      const queueRes = await api.get('/conversation/queue')
      if (queueRes.code === 200) {
        setQueueList(queueRes.data || [])
      }
      const activeRes = await api.get('/conversation/active', { params: { agentId } })
      if (activeRes.code === 200) {
        setActiveConversations(activeRes.data || [])
      }
    } catch (error) {
      console.error('Load data error:', error)
    }
  }

  const handleTakeConversation = async (conversation: Conversation) => {
    try {
      const res = await api.post(`/conversation/${conversation.id}/take`, null, {
        params: { agentId, agentNickname }
      })
      if (res.code === 200) {
        message.success('接手成功')
        loadData()
        setSelectedConversation(conversation)
        loadMessages(conversation.id)
      }
    } catch (error) {
      message.error('接手失败')
    }
  }

  const loadMessages = async (conversationId: number) => {
    try {
      const res = await api.get(`/message/history/${conversationId}`)
      if (res.code === 200) {
        setMessages(res.data || [])
      }
    } catch (error) {
      console.error('Load messages error:', error)
    }
  }

  const handleSendMessage = () => {
    if (!inputValue.trim() || !selectedConversation) return

    wsRef.current?.send({
      type: 'AGENT_MESSAGE',
      conversationId: selectedConversation.id,
      content: inputValue,
      agentId: Number(agentId),
      agentNickname
    })

    setInputValue('')
  }

  const handleEndConversation = async () => {
    if (!selectedConversation) return

    Modal.confirm({
      title: '确认结束会话',
      content: '确定要结束当前会话吗？',
      onOk: async () => {
        try {
          const res = await api.post(`/conversation/${selectedConversation.id}/end`)
          if (res.code === 200) {
            message.success('会话已结束')
            setSelectedConversation(null)
            setMessages([])
            loadData()
          }
        } catch (error) {
          message.error('结束会话失败')
        }
      }
    })
  }

  const handleLogout = () => {
    localStorage.clear()
    navigate('/agent/login')
  }

  const getStatusTag = (status: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      1: { color: 'blue', text: '等待中' },
      2: { color: 'green', text: '进行中' },
      3: { color: 'gray', text: '已结束' }
    }
    return <Tag color={statusMap[status]?.color}>{statusMap[status]?.text}</Tag>
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <CustomerServiceOutlined style={{ fontSize: 28, color: '#fff' }} />
        </div>
        <Menu
          theme="dark"
          defaultSelectedKeys={['queue']}
          mode="inline"
          items={[
            {
              key: 'queue',
              icon: <TeamOutlined />,
              label: '排队列表'
            },
            {
              key: 'active',
              icon: <MessageOutlined />,
              label: '进行会话'
            }
          ]}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: '0 24px', background: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: 18, fontWeight: 'bold' }}>客服工作台</span>
          <Space>
            <Badge status={agentStatus === 1 ? 'success' : agentStatus === 2 ? 'warning' : 'default'} />
            <span>{agentNickname}</span>
            <Button icon={<LogoutOutlined />} onClick={handleLogout}>
              退出
            </Button>
          </Space>
        </Header>
        <Content style={{ padding: 24 }}>
          <div style={{ display: 'flex', gap: 24, height: 'calc(100vh - 180px)' }}>
            {/* 左侧列表 */}
            <Card style={{ width: 320, overflow: 'auto' }} title="等待中的访客">
              <List
                dataSource={queueList}
                renderItem={(item) => (
                  <List.Item
                    actions={[
                      <Button type="primary" size="small" onClick={() => handleTakeConversation(item)}>
                        接手
                      </Button>
                    ]}
                  >
                    <List.Item.Meta
                      title={item.visitorNickname || '访客'}
                      description={`排队位置: ${item.queuePosition}`}
                    />
                  </List.Item>
                )}
              />
            </Card>

            {/* 右侧聊天区域 */}
            <Card style={{ flex: 1, display: 'flex', flexDirection: 'column' }} title={selectedConversation ? `正在对话: ${selectedConversation.visitorNickname}` : '请选择会话'}>
              {selectedConversation ? (
                <>
                  <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
                    {messages.map((msg) => (
                      <div
                        key={msg.id}
                        style={{
                          display: 'flex',
                          justifyContent: msg.senderType === 2 ? 'flex-end' : 'flex-start',
                          marginBottom: 12
                        }}
                      >
                        <div
                          style={{
                            maxWidth: '70%',
                            padding: '8px 12px',
                            borderRadius: 8,
                            background: msg.senderType === 2 ? '#1890ff' : '#f0f0f0',
                            color: msg.senderType === 2 ? '#fff' : '#333'
                          }}
                        >
                          <div style={{ fontSize: 12, marginBottom: 4, opacity: 0.7 }}>
                            {msg.senderName}
                          </div>
                          {msg.content}
                        </div>
                      </div>
                    ))}
                    <div ref={messagesEndRef} />
                  </div>
                  <div style={{ padding: 16, borderTop: '1px solid #f0f0f0' }}>
                    <Space.Compact style={{ width: '100%' }}>
                      <TextArea
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onPressEnter={(e) => {
                          if (!e.shiftKey) {
                            e.preventDefault()
                            handleSendMessage()
                          }
                        }}
                        placeholder="输入消息..."
                        autoSize={{ minRows: 1, maxRows: 4 }}
                        style={{ flex: 1 }}
                      />
                      <Button type="primary" icon={<SendOutlined />} onClick={handleSendMessage}>
                        发送
                      </Button>
                    </Space.Compact>
                  </div>
                  <div style={{ padding: '8px 16px', textAlign: 'right' }}>
                    <Button danger onClick={handleEndConversation}>
                      结束会话
                    </Button>
                  </div>
                </>
              ) : (
                <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
                  请从左侧选择一个会话开始对话
                </div>
              )}
            </Card>
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}

export default AgentDashboard