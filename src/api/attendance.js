import http from './index'

// 组织者看签到记录；params: activityId,page,pageSize
export const listAttendance = (params) => http.get('/attendance', { params })

// 修改志愿时：{hours,minutes}；=0 联动证书失效，>0 自动发/复活证书
export const updateHours = (recordId, { hours, minutes }) =>
  http.put(`/attendance/${recordId}/hours`, { hours, minutes })

// 手动补签：{checkInTime,checkOutTime,hours,minutes}
export const manualSign = (recordId, body) =>
  http.put(`/attendance/${recordId}/manual`, body)

// 志愿者自助签到；params: {activityId}
export const checkIn = (activityId) =>
  http.post(`/attendance/check-in`, null, { params: { activityId } })

// 志愿者自助签退；params: {activityId}
export const checkOut = (activityId) =>
  http.post(`/attendance/check-out`, null, { params: { activityId } })
