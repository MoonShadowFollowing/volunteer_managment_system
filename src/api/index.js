// 全项目共用的 axios 实例
// 拦截器把后端的 Result 壳子拆开；401 跳登录；403 静默刷一下 me() 看权限是不是被改了
// 业务错误有 fix 字段时用 ElNotification 同时展示错误信息和修正指引
import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import router from '../router'

const http = axios.create({
  // 默认后端在本机 8081；CI 或别的环境可以走 VUE_APP_API_BASE 覆盖
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

// 根据后端返回决定用 ElMessage 还是 ElNotification
// fix 字段非空 → 用 Notification 同时展示问题和修正建议；否则用 Message
const showError = (msg, fix) => {
  if (fix) {
    ElNotification({
      title: '操作失败',
      message: msg + '\n\n💡 您可以：' + fix,
      type: 'error',
      duration: 6000
    })
  } else {
    ElMessage.error(msg)
  }
}

// 响应拦截：拆 Result、统一错误提示
http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      showError(body.msg || `业务错误 (code=${body.code})`, body.fix)
      return Promise.reject(body)
    }
    return body
  },
  async err => {
    const status = err?.response?.status
    const body = err?.response?.data
    if (status === 401) {
      clearAuthAndRedirect(body?.msg || '登录已过期，请重新登录')
    } else if (status === 403) {
      showError(body?.msg || '权限不足', body?.fix)
      // 可能权限刚被撤销了，悄悄重拉 /auth/me 看下，把本地 localStorage 同步过来
      // 然后看当前页是组织者/管理员页就跳走，免得用户一脸懵
      try {
        const token = localStorage.getItem('token')
        if (token) {
          const me = await http.get('/auth/me')
          if (me) {
            localStorage.setItem('isOrganizerQualified', String(me.isOrganizerQualified))
            localStorage.setItem('isAdmin', String(me.isAdmin))
            localStorage.setItem('userRole', me.role)
            const isOrgRoute = router.currentRoute.value.path.startsWith('/sys/org-')
            if (!me.isOrganizerQualified && isOrgRoute) {
              ElMessage.warning('您的组织者权限已被撤销，已切换为志愿者身份')
              router.push('/sys/dashboard-volun')
            }
            if (!me.isAdmin && router.currentRoute.value.path.startsWith('/sys/admin-')) {
              ElMessage.warning('您的管理员权限已被撤销')
              router.push('/sys/dashboard-volun')
            }
          }
        }
      } catch (_) { /* 静默 */ }
    } else if (status === 500) {
      showError(body?.msg || '服务器内部错误', body?.fix)
    } else if (err?.code === 'ECONNABORTED') {
      showError('请求超时，请重试', '请检查网络连接是否正常，或稍后再试。')
    } else {
      showError(body?.msg || err?.message || '网络异常', body?.fix)
    }
    return Promise.reject(err)
  }
)

export default http
