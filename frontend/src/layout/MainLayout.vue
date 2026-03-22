<template>
  <el-container class="layout-root">
    <el-aside width="220px" class="layout-aside">
      <h3 class="logo-title">校园网络报修系统</h3>
      <el-menu router background-color="#001529" text-color="#fff" active-text-color="#ffd04b">
        <el-menu-item v-for="m in visibleMenus" :key="m.path" :index="m.path">{{ m.label }}</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="welcome-text">欢迎使用校园网络设备管理与故障报修系统</div>
        <div class="user-bar">
          <span>{{ user.userInfo.realName || user.userInfo.username }}</span>
          <el-button link @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>
<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { MENU_CONFIG } from '../constants/rbac'
const user = useUserStore(); const router = useRouter()
const visibleMenus = computed(() => MENU_CONFIG.filter(m => {
  if (m.perm) return user.hasPerm(m.perm)
  if (m.anyPerm) return user.hasAnyPerm(m.anyPerm)
  return true
}))
const logout = () => { user.logout(); router.push('/login') }
</script>
<style scoped>
.layout-root{height:100vh;background:#f5f7fa}
.layout-aside{background:#001529;color:#fff}
.logo-title{padding:16px 20px;color:#fff;font-size:18px;font-weight:700}
.layout-header{background:#fff;display:flex;justify-content:space-between;align-items:center;padding:0 24px;box-shadow:0 1px 8px rgba(0,0,0,.06)}
.welcome-text{font-size:16px;font-weight:600;color:#303133}
.user-bar{display:flex;align-items:center;gap:8px}
.layout-main{padding:20px}
</style>
