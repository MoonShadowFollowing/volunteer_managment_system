import http from './index'

export const login = (account, password) =>
  http.post('/auth/login', { account, password })

export const fetchMe = () => http.get('/auth/me')

export const logout = () => http.post('/auth/logout')
