import { defineStore } from 'pinia'
import { loginApi, userInfoApi } from '../api'
import { PERMISSIONS } from '../constants/rbac'

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
      const role = data.role || ''
      if (!data.roles || !data.roles.length) data.roles = role ? [role] : []
      if (!data.permissions || !data.permissions.length) {
        data.permissions = PERMISSIONS[(role || '').toUpperCase()] || []
      }
      this.userInfo = data
      localStorage.setItem('userInfo', JSON.stringify(data))
    },
    hasRole(role) {
      const roles = this.userInfo.roles || []
      return roles.includes(role)
    },
    hasAnyRole(roles = []) {
      const mine = this.userInfo.roles || []
      return roles.some(r => mine.includes(r))
    },
    hasPerm(perm) {
      const perms = this.userInfo.permissions || []
      return perms.includes(perm)
    },
    hasAnyPerm(perms = []) {
      const mine = this.userInfo.permissions || []
      return perms.some(p => mine.includes(p))
    },
    logout() { this.token=''; this.userInfo={}; localStorage.clear() }
  }
})
