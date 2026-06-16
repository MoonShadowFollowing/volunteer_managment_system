import http from './index'

// 志愿者报名
export const register = (activityId) => http.post('/registrations', { activityId })

// 我的报名记录；params: page,pageSize,auditStatus
export const myRegistrations = (params) => http.get('/registrations/mine', { params })

// 取消报名（仅待审核）
export const cancelRegistration = (regId) => http.delete(`/registrations/${regId}`)

// 组织者看某活动的报名列表；params: activityId,page,pageSize
export const listRegistrations = (params) => http.get('/registrations', { params })

// 组织者审核报名：approve=true 通过 / false 拒绝
export const auditRegistration = (regId, approve) =>
  http.put(`/registrations/${regId}/audit`, { approve })
