<template>
  <div>
    <h2>首页概览</h2>
    <el-row :gutter="16">
      <el-col :span="6"><el-card>工单总数<div class="num">{{ orderStats.total || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card>待处理工单数<div class="num">{{ orderStats.pending || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card>处理中工单数<div class="num">{{ orderStats.processing || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card>已完成工单数<div class="num">{{ orderStats.finished || 0 }}</div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px" v-if="user.userInfo.role==='admin'">
      <el-col :span="8"><el-card>设备总数<div class="num">{{ deviceStats.total || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card>正常设备数<div class="num">{{ deviceStats.normal || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card>故障设备数<div class="num">{{ deviceStats.fault || 0 }}</div></el-card></el-col>
    </el-row>
  </div>
</template>
<script setup>
import { reactive, onMounted } from 'vue'
import { getPage } from '../../api'
import { useUserStore } from '../../stores/user'
const user = useUserStore()
const orderStats = reactive({})
const deviceStats = reactive({})
onMounted(async () => {
  Object.assign(orderStats, await getPage('/repair-orders/statistics', {}))
  if (user.userInfo.role === 'admin') Object.assign(deviceStats, await getPage('/devices/statistics', {}))
})
</script>
<style scoped>.num{font-size:28px;font-weight:700;color:#409eff;margin-top:8px}</style>
