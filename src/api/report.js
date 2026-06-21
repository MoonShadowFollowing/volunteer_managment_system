import http from './index'

const downloadBlob = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

// 导出本人或指定志愿者的工时 xlsx
export const downloadPersonalHoursXlsx = async (volunteerId) => {
  const params = volunteerId ? { volunteerId } : {}
  const blob = await http.get('/reports/personal-hours.xlsx', { params, responseType: 'blob' })
  const suffix = volunteerId ? `-${volunteerId}` : ''
  downloadBlob(blob, `VMS-PersonalHours${suffix}.xlsx`)
}

// 导出活动签到汇总 xlsx
export const downloadActivitySummaryXlsx = async (activityId) => {
  const blob = await http.get('/reports/activity-summary.xlsx', { params: { activityId }, responseType: 'blob' })
  downloadBlob(blob, `VMS-Activity-${activityId}.xlsx`)
}

// 导出月度全院汇总 xlsx（admin）
export const downloadMonthlyXlsx = async (year, month) => {
  const blob = await http.get('/reports/monthly.xlsx', { params: { year, month }, responseType: 'blob' })
  const mm = String(month).padStart(2, '0')
  downloadBlob(blob, `VMS-Monthly-${year}-${mm}.xlsx`)
}
