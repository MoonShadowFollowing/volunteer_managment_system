import http from './index'

// 分页查活动；params 可含 page,pageSize,name,startDate,endDate,auditStatus,publishStatus,organizerId,volunteerView
export const listActivities = (params) => http.get('/activities', { params })

export const getActivity = (id) => http.get(`/activities/${id}`)

// body: { name, desc, location, startTime, endTime, limitNum }
export const createActivity = (body) => http.post('/activities', body)

export const updateActivity = (id, body) => http.put(`/activities/${id}`, body)

export const deleteActivity = (id) => http.delete(`/activities/${id}`)

// 管理员审核：approve=true 通过 / false 驳回
export const auditActivity = (id, approve) =>
  http.put(`/activities/${id}/audit`, { approve })

// 组织者切前台发布开关：publish=true 发布中 / false 已停止
export const publishActivity = (id, publish) =>
  http.put(`/activities/${id}/publish`, { publish })
