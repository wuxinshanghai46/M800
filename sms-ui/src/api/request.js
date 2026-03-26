import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/v1', timeout: 15000 })

// request interceptor: attach token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// response interceptor
http.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== undefined && data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(err)
    }
    ElMessage.error(err.response?.data?.message || err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default http
