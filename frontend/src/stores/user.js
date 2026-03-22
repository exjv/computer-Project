import { defineStore } from 'pinia'
import { loginApi, userInfoApi } from '../api'
import { PERMISSIONS } from '../constants/rbac'

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
      const role = data.role || ''
      if (!data.roles || !data.roles.length) data.roles = role ? [role] : []
      if (!data.permissions || !data.permissions.length) {
        data.permissions = PERMISSIONS[(role || '').toUpperCase()] || []
      }
      this.userInfo = data
      this.permissions = data.permissions || []
      this.roles = data.roles || []
      localStorage.setItem('userInfo', JSON.stringify(data))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      localStorage.setItem('roles', JSON.stringify(this.roles))
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
