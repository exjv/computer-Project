<template>
  <el-container style="height:100vh">
    <el-aside width="220px">
      <h3 style="padding:16px">校园网络报修系统</h3>
      <el-menu router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">用户管理</el-menu-item>
        <el-menu-item index="/devices">设备管理</el-menu-item>
        <el-menu-item index="/repair-orders">报修工单</el-menu-item>
        <el-menu-item v-if="isAdmin||isMaintainer" index="/repair-records">维修记录</el-menu-item>
        <el-menu-item index="/notices">公告</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs">日志</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="text-align:right">{{ user.userInfo.realName || user.userInfo.username }}
        <el-button link @click="logout">退出</el-button>
      </el-header>
      <el-main><router-view/></el-main>
    </el-container>
  </el-container>
</template>
<script setup>
import { computed } from 'vue';import { useUserStore } from '../stores/user';import { useRouter } from 'vue-router'
const user = useUserStore();const router=useRouter();
const isAdmin = computed(()=>user.userInfo.role==='admin')
const isMaintainer = computed(()=>user.userInfo.role==='maintainer')
const logout=()=>{user.logout();router.push('/login')}
</script>
