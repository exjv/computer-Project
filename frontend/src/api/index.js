import request from '../utils/request'
import axios from 'axios'

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

const downloadByGet = async (url, params = {}, defaultFileName = 'export.xlsx') => {
  const token = localStorage.getItem('token')
  const response = await axios.get(`/api${url}`, {
    params,
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const disposition = response.headers['content-disposition'] || ''
  const match = disposition.match(/filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?/)
  const fileName = decodeURIComponent((match && (match[1] || match[2])) || defaultFileName)
  const blob = new Blob([response.data])
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}

export const exportRepairOrderReportApi = params => downloadByGet('/reports/repair-orders/export/excel', params, '工单统计报表.xlsx')
export const exportRepairRecordReportApi = params => downloadByGet('/reports/repair-records/export/excel', params, '设备维修记录报表.xlsx')
