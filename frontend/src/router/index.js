import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ROUTE_ROLE_MAP } from '../constants/rbac'

const routes = [
  { path: '/portal', component: () => import('../views/auth/PortalView.vue') },
  { path: '/login', component: () => import('../views/auth/LoginView.vue') },
  { path: '/', component: () => import('../layout/MainLayout.vue'), children: [
    { path: '', component: () => import('../views/dashboard/HomeView.vue'), meta: { roles: ROUTE_ROLE_MAP['/'] } },
    { path: 'users', component: () => import('../views/user/UserView.vue'), meta: { roles: ROUTE_ROLE_MAP['/users'] } },
    { path: 'devices', component: () => import('../views/device/DeviceView.vue'), meta: { roles: ROUTE_ROLE_MAP['/devices'] } },
    { path: 'devices/:id', component: () => import('../views/device/DeviceDetailView.vue'), meta: { roles: ROUTE_ROLE_MAP['/devices'] } },
    { path: 'repair-orders', component: () => import('../views/repair/RepairOrderView.vue'), meta: { roles: ROUTE_ROLE_MAP['/repair-orders'] } },
    { path: 'repair-orders/:id', component: () => import('../views/repair/RepairOrderDetailView.vue'), meta: { roles: ROUTE_ROLE_MAP['/repair-orders'] } },
    { path: 'repair-orders/:id/progress', component: () => import('../views/repair/RepairOrderProgressView.vue'), meta: { roles: ROUTE_ROLE_MAP['/repair-orders'] } },
    { path: 'repair-apply', component: () => import('../views/repair/RepairApplyView.vue'), meta: { roles: ROUTE_ROLE_MAP['/repair-apply'] } },
    { path: 'my-repairs', component: () => import('../views/repair/MyRepairRecordsView.vue'), meta: { roles: ROUTE_ROLE_MAP['/my-repairs'] } },
    { path: 'my-repairs/:id/progress', component: () => import('../views/repair/RepairOrderProgressView.vue'), meta: { roles: ROUTE_ROLE_MAP['/my-repairs'] } },
    { path: 'repair-records', component: () => import('../views/record/RepairRecordView.vue'), meta: { roles: ROUTE_ROLE_MAP['/repair-records'] } },
    { path: 'logs', component: () => import('../views/log/LogView.vue'), meta: { roles: ROUTE_ROLE_MAP['/logs'] } },
    { path: 'profile', component: () => import('../views/profile/ProfileView.vue'), meta: { roles: ROUTE_ROLE_MAP['/profile'] } }
  ] }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.path === '/login' || to.path === '/portal') return true
  if (!store.token) return '/portal'
  if (!store.userInfo.role) await store.fetchUserInfo()
  const dynamicMap = store.routeRoleMap || ROUTE_ROLE_MAP
  const targetRoles = to.meta.roles || dynamicMap[to.path] || []
  if (targetRoles.length && !store.hasAnyRole(targetRoles)) return '/'
  return true
})
export default router
