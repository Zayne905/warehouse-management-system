import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use(
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

request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 检查业务错误码
    if (data && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      ElMessage.error(error.response?.data?.message || '登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(error)
    }
    if (status === 403) {
      ElMessage.error(error.response?.data?.message || '权限不足，请联系管理员')
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
