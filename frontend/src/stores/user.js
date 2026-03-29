import { defineStore } from 'pinia'
import { loginApi, userInfoApi } from '../api'
import { PERMISSIONS, ROUTE_ROLE_MAP } from '../constants/rbac'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]'),
    roles: JSON.parse(localStorage.getItem('roles') || '[]'),
    routeRoleMap: JSON.parse(localStorage.getItem('routeRoleMap') || JSON.stringify(ROUTE_ROLE_MAP))
  }),
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.token
      localStorage.setItem('token', res.token)

      this.permissions = Array.from(res.permissions || [])
      this.roles = Array.from(res.roles || [])
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      localStorage.setItem('roles', JSON.stringify(this.roles))

      this.userInfo = {
        role: res.role,
        username: res.username,
        employeeNo: res.employeeNo,
        roles: this.roles,
        permissions: this.permissions
      }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
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
      this.routeRoleMap = data.routeRoleMap || ROUTE_ROLE_MAP
      localStorage.setItem('userInfo', JSON.stringify(data))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      localStorage.setItem('roles', JSON.stringify(this.roles))
      localStorage.setItem('routeRoleMap', JSON.stringify(this.routeRoleMap))
    },
    hasRole(role) {
      return (this.roles || []).includes(role)
    },
    hasAnyRole(roles = []) {
      const mine = this.roles || []
      return roles.some(r => mine.includes(r))
    },
    hasPerm(perm) {
      return (this.permissions || []).includes(perm)
    },
    hasAnyPerm(perms = []) {
      const mine = this.permissions || []
      return perms.some(p => mine.includes(p))
    },
    logout() {
      this.token = ''
      this.userInfo = {}
      this.permissions = []
      this.roles = []
      this.routeRoleMap = ROUTE_ROLE_MAP
      localStorage.clear()
    }
  }
})
