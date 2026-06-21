// 登录 + 拉用户 + 登出。refresh 没在前端用，401 拦截直接踢登录页就好
import http from './index'

export const login = (account, password) =>
  http.post('/auth/login', { account, password })

export const fetchMe = () => http.get('/auth/me')

export const logout = () => http.post('/auth/logout')
