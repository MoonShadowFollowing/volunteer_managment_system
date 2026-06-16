import http from './index'

// 我的消息（含公告实化条目）；params: page,pageSize,type
export const myMessages = (params) => http.get('/messages/mine', { params })

// 历史公告列表（原始条目）；params: page,pageSize
export const listNotices = (params) => http.get('/messages/notices', { params })

// 管理员发公告：{title,content,targets:["全体志愿者","全体组织者"]}
export const sendNotice = (body) => http.post('/messages/notice', body)
