import request from '../utils/request'
export const loginApi = data => request.post('/auth/login', data)
export const userInfoApi = () => request.get('/auth/userInfo')
export const pageApi = (url, params) => request.get(url, { params })
export const postApi = (url, data) => request.post(url, data)
export const putApi = (url, data) => request.put(url, data)
export const delApi = url => request.delete(url)
