import http from './index'

// 工作台统计：返回 { role, metrics }，view 指定前台角色用来让后端区分双身份用户
export const dashboard = (view) => http.get('/stat/dashboard', { params: { view } })
