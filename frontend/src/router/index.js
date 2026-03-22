import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/portal', component: () => import('../views/auth/PortalView.vue') },
  { path: '/login', component: () => import('../views/auth/LoginView.vue') },
  { path: '/', component: () => import('../layout/MainLayout.vue'), children: [
    { path: '', component: () => import('../views/dashboard/HomeView.vue') },
    { path: 'users', component: () => import('../views/user/UserView.vue'), meta: { permission: 'user:manage' } },
    { path: 'devices', component: () => import('../views/device/DeviceView.vue'), meta: { permission: 'device:manage' } },
    { path: 'repair-orders', component: () => import('../views/repair/RepairOrderView.vue') },
    { path: 'repair-orders/:id', component: () => import('../views/repair/RepairOrderDetailView.vue') },
    { path: 'repair-records', component: () => import('../views/record/RepairRecordView.vue'), meta: { permission: 'repair:record:write' } },
    { path: 'logs', component: () => import('../views/log/LogView.vue'), meta: { permission: 'log:operation:view' } },
    { path: 'profile', component: () => import('../views/profile/ProfileView.vue') }
  ] }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.path === '/login' || to.path === '/portal') return true
  if (!store.token) return '/portal'
  if (!store.userInfo.role) await store.fetchUserInfo()
  const permission = to.meta?.permission
  if (permission && !store.hasPermission(permission)) return '/'
  return true
})
export default router
