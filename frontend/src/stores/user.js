import { defineStore } from 'pinia'
import { loginApi, userInfoApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]'),
    roles: JSON.parse(localStorage.getItem('roles') || '[]')
  }),
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.token
      localStorage.setItem('token', res.token)
      this.permissions = res.permissions || []
      this.roles = res.roles || []
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      localStorage.setItem('roles', JSON.stringify(this.roles))
      await this.fetchUserInfo()
    },
    async fetchUserInfo() {
      const data = await userInfoApi()
      this.userInfo = data
      this.permissions = data.permissions || []
      this.roles = data.roles || []
      localStorage.setItem('userInfo', JSON.stringify(data))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      localStorage.setItem('roles', JSON.stringify(this.roles))
    },
    hasPermission(code) { return this.permissions.includes(code) },
    logout() { this.token=''; this.userInfo={}; this.permissions=[]; this.roles=[]; localStorage.clear() }
  }
})
