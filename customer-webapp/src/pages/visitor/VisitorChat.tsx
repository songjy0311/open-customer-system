import { useState, useEffect, useRef } from 'react'
import { message, Input, Button, Badge, Space } from 'antd'
import { CustomerServiceOutlined, SendOutlined, CloseOutlined, MinusOutlined } from '@ant-design/icons'
import WebSocketClient from '../../utils/websocket'

interface ChatMessage {
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

interface SystemNotice {
  id: string
  type: 'system'
  content: string
}

type ChatItem = ChatMessage | SystemNotice

function isSystemNotice(item: ChatItem): item is SystemNotice {
  return (item as SystemNotice).type === 'system'
}

const VisitorChat: React.FC = () => {
  const [open, setOpen] = useState(false)
  const [minimized, setMinimized] = useState(false)
  const [chatItems, setChatItems] = useState<ChatItem[]>([])
  const [inputValue, setInputValue] = useState('')
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [status, setStatus] = useState<number>(0)
  const [queuePosition, setQueuePosition] = useState(0)
  const [agentNickname, setAgentNickname] = useState('')
  const wsRef = useRef<WebSocketClient | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

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
  }, [chatItems])

  const initWebSocket = () => {
    const visitorId = getVisitorId()
    const wsUrl = import.meta.env.PROD
      ? `wss://${location.host}/ws/visitor?visitorId=${visitorId}`
      : `ws://localhost:8080/ws/visitor?visitorId=${visitorId}`
    const ws = new WebSocketClient(wsUrl)
    ws.connect()
      .then(() => {
        ws.send({
          type: 'VISITOR_JOIN',
          visitorId,
          nickname: '访客'
        })
      })
      .catch(console.error)

    ws.on('CONVERSATION_STARTED', (data: {
      data: {
        conversationId: number
        status: number
        agentNickname?: string
        queuePosition?: number
        historyMessages?: ChatMessage[]
      }
    }) => {
      const { conversationId: convId, status: convStatus, agentNickname: agent, queuePosition: pos, historyMessages } = data.data

      // 加载历史消息（客服接手时会推送）
      if (historyMessages && historyMessages.length > 0) {
        const items: ChatItem[] = historyMessages.map((msg) => ({ ...msg }))
        setChatItems(items)
      } else {
        setChatItems([])
      }

      setConversationId(convId)
      setStatus(convStatus)
      setQueuePosition(pos || 0)

      if (agent) {
        setAgentNickname(agent)
        const notice: SystemNotice = {
          id: `notice_${Date.now()}`,
          type: 'system',
          content: `客服 ${agent} 已接入，开始为您服务`
        }
        setChatItems((prev) => [...prev, notice])
      }
    })

    ws.on('NEW_MESSAGE', (data: { data: ChatMessage }) => {
      setChatItems((prev) => [...prev, data.data])
    })

    ws.on('MESSAGES_READ', (data: { data: { conversationId: number; readMessageIds: number[] } }) => {
      const readSet = new Set(data.data.readMessageIds)
      setChatItems((prev) =>
        prev.map((item) => {
          if (!isSystemNotice(item) && readSet.has(item.id)) {
            return { ...item, isRead: 1 }
          }
          return item
        })
      )
    })

    ws.on('CONVERSATION_ENDED', () => {
      setStatus(3)
      message.info('会话已结束，请对本次服务进行评价')
    })

    wsRef.current = ws
  }

  const handleSendMessage = () => {
    if (!inputValue.trim()) return

    if (!conversationId) {
      initWebSocket()
      setOpen(true)
      setMinimized(false)
      message.warning('正在建立连接，请稍后再试')
      return
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

  const renderReadStatus = (item: ChatMessage) => {
    if (item.senderType !== 1) return null
    return (
      <div style={{ fontSize: 11, marginTop: 2, textAlign: 'right', color: item.isRead === 1 ? '#1890ff' : '#999' }}>
        {item.isRead === 1 ? '已读' : '未读'}
      </div>
    )
  }

  return (
    <>
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
                icon={<MinusOutlined />}
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

          <div style={{ height: 340, overflow: 'auto', padding: 16 }}>
            {chatItems.map((item) => {
              if (isSystemNotice(item)) {
                return (
                  <div
                    key={item.id}
                    style={{
                      textAlign: 'center',
                      margin: '8px 0',
                      fontSize: 12,
                      color: '#999'
                    }}
                  >
                    {item.content}
                  </div>
                )
              }
              return (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: item.senderType === 1 ? 'flex-end' : 'flex-start',
                    marginBottom: 12
                  }}
                >
                  <div style={{ maxWidth: '70%' }}>
                    <div
                      style={{
                        padding: '8px 12px',
                        borderRadius: 8,
                        background: item.senderType === 1 ? '#1890ff' : '#f0f0f0',
                        color: item.senderType === 1 ? '#fff' : '#333'
                      }}
                    >
                      <div style={{ fontSize: 12, marginBottom: 4, opacity: 0.7 }}>
                        {item.senderName}
                      </div>
                      {item.content}
                    </div>
                    {renderReadStatus(item)}
                  </div>
                </div>
              )
            })}
            <div ref={messagesEndRef} />
          </div>

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