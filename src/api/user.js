import http from './index'

// 可提升用户列表（非超管+非管理员）；params: name,userNo,page,pageSize
export const promotable = (params) => http.get('/users/promotable', { params })

// 当前管理员列表（不含超管）；params: name,userNo,page,pageSize
export const admins = (params) => http.get('/users/admins', { params })

// 提升为管理员
export const promoteAdmin = (userId) => http.put(`/users/${userId}/promote-admin`)

// 撤销管理员
export const revokeAdmin = (userId) => http.put(`/users/${userId}/revoke-admin`)
