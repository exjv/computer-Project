import request from '../utils/request'

export const loginApi = data => request.post('/auth/login', data)
export const captchaApi = () => request.get('/auth/captcha')
export const oauthUrlApi = provider => request.get(`/auth/oauth/${provider}/url`)
export const oauthCallbackApi = (provider, code) => request.get(`/auth/oauth/${provider}/callback`, { params: { code } })
export const bindThirdPartyApi = (provider, data) => request.post(`/auth/oauth/${provider}/bind`, data)
export const unbindThirdPartyApi = provider => request.delete(`/auth/oauth/${provider}/unbind`)
export const userInfoApi = () => request.get('/auth/userInfo')
export const updateProfileApi = data => request.put('/auth/updateProfile', data)
export const updatePasswordApi = data => request.put('/auth/updatePassword', data)
export const portalHomeApi = () => request.get('/portal/home')

export const getPage = (url, params) => request.get(url, { params })
export const postApi = (url, data) => request.post(url, data)
export const putApi = (url, data) => request.put(url, data)
export const delApi = url => request.delete(url)

export const autoDispatchApi = () => request.post('/repair-orders/auto-dispatch')
