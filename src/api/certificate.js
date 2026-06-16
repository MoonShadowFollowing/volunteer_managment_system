import http from './index'

// 我的证书；params: page,pageSize,activityName
export const myCertificates = (params) => http.get('/certificates/mine', { params })

// 下载证书 PDF：拿 blob（拦截器对非 {code} 响应直接放行返回 Blob），触发浏览器下载
export const downloadCertPdf = async (id, fileName) => {
  const blob = await http.get(`/certificates/${id}/pdf`, { responseType: 'blob' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName || `VMS-Certificate-${id}.pdf`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
