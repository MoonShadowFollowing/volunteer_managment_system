import http from './index'

// 提交评价（志愿者评组织者 / 组织者评志愿者）
export const submitReview = (payload) => http.post('/reviews', payload)

// 我提交过的评价
export const myReviews = (params) => http.get('/reviews/mine', { params })

// 我收到的评价
export const myReceivedReviews = (params) => http.get('/reviews/received', { params })

// 某活动下的评价列表
export const reviewsByActivity = (params) => http.get('/reviews', { params })

// 聚合：某用户在某活动 / 全部活动的平均分与条数
export const reviewSummary = (params) => http.get('/reviews/summary', { params })
