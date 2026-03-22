<template>
  <div>
    <el-page-header @back="router.back()" content="工单详情"/>
    <el-card style="margin-top:12px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="故障标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ detail.priority }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ detail.reporterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修人工号">{{ detail.reporterEmployeeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detail.deviceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ detail.reportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ detail.progress || 0 }}%</el-descriptions-item>
      </el-descriptions>

      <el-divider>流程记录</el-divider>
      <el-timeline>
        <el-timeline-item v-for="f in flows" :key="f.id" :timestamp="f.createTime">
          {{ f.fromStatus || '开始' }} → {{ f.toStatus }} ｜ {{ f.action }} ｜ {{ f.remark || '无备注' }}
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage } from '../../api'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const flows = ref([])

onMounted(async () => {
  const id = route.params.id
  detail.value = await getPage(`/repair-orders/${id}`)
  flows.value = await getPage(`/repair-orders/${id}/flows`)
})
</script>
