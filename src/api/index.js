import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: process.env.VUE_APP_API_BASE || 'http://localhost:8081/api',
  timeout: 10000
})

// 请求拦截：自动带上 Authorization
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const clearAuthAndRedirect = (msg) => {
  localStorage.removeItem('token')
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('userRole')
  localStorage.removeItem('isOrganizerQualified')
  localStorage.removeItem('isAdmin')
  ElMessage.warning(msg || '请重新登录')
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 响应拦截：拆 Result、统一错误提示
http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      ElMessage.error(body.msg || `业务错误 (code=${body.code})`)
      return Promise.reject(body)
    }
    return body
  },
  err => {
    const status = err?.response?.status
    const body = err?.response?.data
    if (status === 401) {
      clearAuthAndRedirect(body?.msg || '登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error(body?.msg || '权限不足')
    } else if (status === 500) {
      ElMessage.error(body?.msg || '服务器内部错误')
    } else if (err?.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请重试')
    } else {
      ElMessage.error(body?.msg || err?.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default http
