type WebSocketCallback = (data: any) => void

class WebSocketClient {
  private ws: WebSocket | null = null
  private url: string = ''
  private callbacks: Map<string, WebSocketCallback[]> = new Map()
  private reconnectInterval: number = 3000
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private isManualClose: boolean = false

  constructor(url: string) {
    this.url = url
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('WebSocket connected')
          this.isManualClose = false
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            console.log('[WS] received ALL:', data.type, JSON.stringify(data))
            const type = data.type
            const callbacks = this.callbacks.get(type)
            if (callbacks) {
              callbacks.forEach((callback) => callback(data))
            }
            // 也触发 ALL 类型的回调
            const allCallbacks = this.callbacks.get('ALL')
            if (allCallbacks) {
              allCallbacks.forEach((callback) => callback(data))
            }
          } catch (e) {
            console.error('Parse message error:', e)
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error)
          reject(error)
        }

        this.ws.onclose = () => {
          console.log('WebSocket closed')
          if (!this.isManualClose) {
            this.reconnect()
          }
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  on(type: string, callback: WebSocketCallback) {
    const callbacks = this.callbacks.get(type) || []
    callbacks.push(callback)
    this.callbacks.set(type, callbacks)
  }

  off(type: string, callback: WebSocketCallback) {
    const callbacks = this.callbacks.get(type)
    if (callbacks) {
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  send(data: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  close() {
    this.isManualClose = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  private reconnect() {
    if (this.reconnectTimer) {
      return
    }
    this.reconnectTimer = setTimeout(() => {
      console.log('Reconnecting...')
      this.connect().catch(console.error)
      this.reconnectTimer = null
    }, this.reconnectInterval)
  }

  getReadyState(): number {
    return this.ws ? this.ws.readyState : WebSocket.CLOSED
  }
}

export default WebSocketClient
