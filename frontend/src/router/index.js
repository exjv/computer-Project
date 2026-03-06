import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', component: () => import('../views/auth/LoginView.vue') },
  { path: '/', component: () => import('../layout/MainLayout.vue'), children: [
    { path: '', component: () => import('../views/dashboard/HomeView.vue') },
    { path: 'users', component: () => import('../views/user/UserView.vue') },
    { path: 'devices', component: () => import('../views/device/DeviceView.vue') },
    { path: 'repair-orders', component: () => import('../views/repair/RepairOrderView.vue') },
    { path: 'repair-records', component: () => import('../views/record/RepairRecordView.vue') },
    { path: 'notices', component: () => import('../views/notice/NoticeView.vue') },
    { path: 'logs', component: () => import('../views/log/LogView.vue') },
    { path: 'profile', component: () => import('../views/profile/ProfileView.vue') }
  ] }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.path === '/login') return true
  if (!store.token) return '/login'
  if (!store.userInfo.role) await store.fetchUserInfo()
  return true
})
export default router
