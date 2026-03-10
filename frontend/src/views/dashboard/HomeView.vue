<template>
  <div>
    <h2>首页概览</h2>
    <el-row :gutter="16">
      <el-col :span="6"><el-card>工单总数<div class="num">{{ orderStats.total || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card>待处理工单<div class="num">{{ orderStats.pending || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card>处理中<div class="num">{{ orderStats.processing || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card>已完成<div class="num">{{ orderStats.finished || 0 }}</div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px" v-if="user.userInfo.role==='admin'">
      <el-col :span="8"><el-card>设备总数<div class="num">{{ deviceStats.total || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card>正常设备数<div class="num">{{ deviceStats.normal || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card>故障设备数<div class="num">{{ deviceStats.fault || 0 }}</div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px" v-if="user.userInfo.role==='admin'">
      <el-col :span="8"><el-card><div class="chart-title">设备状态分布</div><div ref="deviceChartRef" class="chart"/></el-card></el-col>
      <el-col :span="8"><el-card><div class="chart-title">工单趋势</div><div ref="trendChartRef" class="chart"/></el-card></el-col>
      <el-col :span="8"><el-card><div class="chart-title">完成率统计</div><div ref="rateChartRef" class="chart"/></el-card></el-col>
    </el-row>
  </div>
</template>
<script setup>
import { reactive, onMounted, ref, nextTick } from 'vue'
import { getPage } from '../../api'
import { useUserStore } from '../../stores/user'
import * as echarts from 'echarts'
const user = useUserStore()
const orderStats = reactive({})
const deviceStats = reactive({})
const deviceChartRef=ref(), trendChartRef=ref(), rateChartRef=ref()

const renderCharts=()=>{
  const d=echarts.init(deviceChartRef.value)
  d.setOption({tooltip:{},series:[{type:'pie',data:[{name:'正常',value:deviceStats.normal||0},{name:'故障',value:deviceStats.fault||0}],radius:'65%'}]})
  const t=echarts.init(trendChartRef.value)
  t.setOption({xAxis:{type:'category',data:['待处理','处理中','已完成']},yAxis:{type:'value'},series:[{type:'bar',data:[orderStats.pending||0,orderStats.processing||0,orderStats.finished||0]}]})
  const r=echarts.init(rateChartRef.value)
  const finishRate=(orderStats.total?((orderStats.finished||0)/orderStats.total*100):0).toFixed(1)
  r.setOption({series:[{type:'gauge',progress:{show:true},detail:{formatter:`${finishRate}%`},data:[{value:finishRate,name:'完成率'}]}]})
}

onMounted(async () => {
  Object.assign(orderStats, await getPage('/repair-orders/statistics', {}))
  if (user.userInfo.role === 'admin') {
    Object.assign(deviceStats, await getPage('/devices/statistics', {}))
    await nextTick(); renderCharts()
  }
})
</script>
<style scoped>
.num{font-size:28px;font-weight:700;color:#409eff;margin-top:8px}
.chart{height:220px}
.chart-title{font-weight:600;margin-bottom:8px}
</style>
