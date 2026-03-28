import axios, { AxiosResponse } from 'axios'

const baseURL = '/api'

const instance = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 — 直接返回 response.data，类型为业务响应体
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 用类型断言导出，使调用处类型推断为 Promise<any>
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const api = instance as unknown as {
  get(url: string, config?: object): Promise<any>
  post(url: string, data?: unknown, config?: object): Promise<any>
  put(url: string, data?: unknown, config?: object): Promise<any>
  delete(url: string, config?: object): Promise<any>
}

export default api
