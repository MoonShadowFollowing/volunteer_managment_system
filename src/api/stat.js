import http from './index'

// 工作台统计：返回 { role, metrics }
export const dashboard = () => http.get('/stat/dashboard')
