import { defineStore } from 'pinia'
import { loginApi, userInfoApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({ token: localStorage.getItem('token') || '', userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}') }),
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.token
      localStorage.setItem('token', res.token)
      await this.fetchUserInfo()
    },
    async fetchUserInfo() {
      const data = await userInfoApi()
      this.userInfo = data
      localStorage.setItem('userInfo', JSON.stringify(data))
    },
    logout() { this.token=''; this.userInfo={}; localStorage.clear() }
  }
})
