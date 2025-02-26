import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
instance.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  response => {
    return response
  },
  error => {
    console.error('Response error:', error)
    
    if (error.response) {
      const { status } = error.response
      
      if (status === 401) {
        // 未授权，清除token并跳转到登录页
        localStorage.removeItem('token')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else if (status === 500) {
        ElMessage.error('服务器错误，请稍后再试')
      } else {
        ElMessage.error(error.response.data.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查您的连接')
    }
    
    return Promise.reject(error)
  }
)

export default instance 