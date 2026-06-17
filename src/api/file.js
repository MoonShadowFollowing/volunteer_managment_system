import http from './index'

const BASE = process.env.VUE_APP_API_BASE || 'http://localhost:8081/api'

// ElUpload 直传用：action 地址 + 带 JWT 的请求头
export const uploadAction = `${BASE}/files/upload`
export const uploadHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

// 把后端返回的相对 url（/api/files/static/...）拼成可访问的完整地址
export const resolveFileUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  // 后端返回形如 /api/files/static/...，BASE 已含 /api，去掉重复前缀
  const origin = BASE.replace(/\/api$/, '')
  return origin + url
}

// 也提供函数式上传（FormData）
export const uploadFile = (formData) =>
  http.post('/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
