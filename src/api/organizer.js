import http from './index'

// 志愿者提交组织者申请：{reason,materialUrl}
export const submitApplication = (body) => http.post('/organizer-applications', body)

// 我的申请历史；params: page,pageSize
export const myApplications = (params) => http.get('/organizer-applications/mine', { params })

// 管理员看申请列表；params: status,page,pageSize
export const listApplications = (params) => http.get('/organizer-applications', { params })

// 管理员审核申请：approve=true 通过 / false 拒绝
export const auditApplication = (appId, approve) =>
  http.put(`/organizer-applications/${appId}/audit`, { approve })

// 已通过组织者列表（含 actCount）；params: name,userNo,page,pageSize
export const listOrganizers = (params) => http.get('/organizers', { params })

// 撤销组织者资质
export const revokeOrganizer = (userId) => http.delete(`/organizers/${userId}`)
