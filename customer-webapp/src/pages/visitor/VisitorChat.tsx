import { useState, useEffect, useRef } from 'react'
import { Message, Input, Button, Badge, Space } from 'antd'
import { CustomerServiceOutlined, SendOutlined, CloseOutlined, MinOutlined } from '@ant-design/icons'
import WebSocketClient from '../utils/websocket'

interface Message {
  id: number
  conversationId: number
  senderType: number
  senderId: string
  senderName: string
  content: string
  messageType: number
  createdAt: string
}

const VisitorChat: React.FC = () => {
  const [open, setOpen] = useState(false)
  const [minimized, setMinimized] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [inputValue, setInputValue] = useState('')
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [status, setStatus] = useState<number>(0) // 0: 未连接, 1: 等待中, 2: 进行中, 3: 已结束
  const [queuePosition, setQueuePosition] = useState(0)
  const [agentNickname, setAgentNickname] = useState('')
  const wsRef = useRef<WebSocketClient | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // 生成访客ID
  const getVisitorId = () => {
    let visitorId = localStorage.getItem('visitorId')
    if (!visitorId) {
      visitorId = 'visitor_' + Date.now()
      localStorage.setItem('visitorId', visitorId)
    }
    return visitorId
  }

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const initWebSocket = () => {
    const wsUrl = import.meta.env.PROD
      ? `wss://${location.host}/ws/visitor`
      : `ws://localhost:8080/ws/visitor`
    const ws = new WebSocketClient(wsUrl)
    ws.connect()
      .then(() => {
        ws.send({
          type: 'VISITOR_JOIN',
          visitorId: getVisitorId(),
          nickname: '访客'
        })
      })
      .catch(console.error)

    ws.on('CONVERSATION_STARTED', (data) => {
      setConversationId(data.data.conversationId)
      setStatus(data.data.status)
      if (data.data.agentNickname) {
        setAgentNickname(data.data.agentNickname)
      }
      if (data.data.queuePosition) {
        setQueuePosition(data.data.queuePosition)
      }
    })

    ws.on('NEW_MESSAGE', (data) => {
      setMessages((prev) => [...prev, data.data])
    })

    ws.on('CONVERSATION_ENDED', (data) => {
      setStatus(3)
      Message.info('会话已结束，请对本次服务进行评价')
    })

    wsRef.current = ws
  }

  const handleSendMessage = () => {
    if (!inputValue.trim()) return

    if (!conversationId) {
      // 首次发送消息，创建会话
      initWebSocket()
      setOpen(true)
      setMinimized(false)
    }

    wsRef.current?.send({
      type: 'VISITOR_MESSAGE',
      conversationId,
      content: inputValue,
      messageType: 1
    })

    setInputValue('')
  }

  const getStatusText = () => {
    switch (status) {
      case 1:
        return `等待中，当前排队位置: ${queuePosition}`
      case 2:
        return `已接入，与 ${agentNickname} 对话中`
      case 3:
        return '会话已结束'
      default:
        return '点击发起咨询'
    }
  }

  return (
    <>
      {/* 咨询浮窗 */}
      {!open && (
        <div
          onClick={() => {
            setOpen(true)
            if (!wsRef.current) {
              initWebSocket()
            }
          }}
          style={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            width: 60,
            height: 60,
            borderRadius: '50%',
            background: '#1890ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(24, 144, 255, 0.4)',
            zIndex: 1000
          }}
        >
          <CustomerServiceOutlined style={{ fontSize: 28, color: '#fff' }} />
        </div>
      )}

      {/* 聊天窗口 */}
      {open && (
        <div
          style={{
            position: 'fixed',
            bottom: minimized ? -400 : 24,
            right: 24,
            width: 380,
            height: minimized ? 0 : 500,
            background: '#fff',
            borderRadius: 12,
            boxShadow: '0 4px 24px rgba(0,0,0,0.15)',
            zIndex: 1000,
            transition: 'all 0.3s ease',
            overflow: 'hidden'
          }}
        >
          {/* 头部 */}
          <div
            style={{
              height: 56,
              padding: '0 16px',
              background: '#1890ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <Space>
              <CustomerServiceOutlined style={{ fontSize: 20, color: '#fff' }} />
              <span style={{ color: '#fff', fontSize: 16 }}>在线客服</span>
            </Space>
            <Space>
              <Badge status={status === 2 ? 'success' : 'processing'} />
              <span style={{ color: '#fff', fontSize: 12 }}>{getStatusText()}</span>
              <Button
                type="text"
                icon={<MinOutlined />}
                onClick={() => setMinimized(true)}
                style={{ color: '#fff' }}
              />
              <Button
                type="text"
                icon={<CloseOutlined />}
                onClick={() => setOpen(false)}
                style={{ color: '#fff' }}
              />
            </Space>
          </div>

          {/* 消息列表 */}
          <div style={{ height: 340, overflow: 'auto', padding: 16 }}>
            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  display: 'flex',
                  justifyContent: msg.senderType === 1 ? 'flex-start' : 'flex-end',
                  marginBottom: 12
                }}
              >
                <div
                  style={{
                    maxWidth: '70%',
                    padding: '8px 12px',
                    borderRadius: 8,
                    background: msg.senderType === 1 ? '#f0f0f0' : '#1890ff',
                    color: msg.senderType === 1 ? '#333' : '#fff'
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

          {/* 输入框 */}
          <div style={{ padding: 16, borderTop: '1px solid #f0f0f0' }}>
            <Space.Compact style={{ width: '100%' }}>
              <Input
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onPressEnter={(e) => {
                  if (!e.shiftKey) {
                    e.preventDefault()
                    handleSendMessage()
                  }
                }}
                placeholder="请输入消息..."
                disabled={status === 3}
              />
              <Button type="primary" icon={<SendOutlined />} onClick={handleSendMessage} disabled={status === 3}>
                发送
              </Button>
            </Space.Compact>
          </div>
        </div>
      )}
    </>
  )
}

export default VisitorChat
