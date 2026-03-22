<template>
  <div>
    <el-page-header @back="router.back()" content="工单进度跟踪"/>
    <el-card style="margin-top:12px">
      <div style="margin-bottom:12px">工单编号：{{ detail.orderNo }} ｜ 当前状态：<el-tag type="primary">{{ detail.status }}</el-tag></div>
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="s in steps" :key="s" :title="s"/>
      </el-steps>
      <el-divider>流程进度</el-divider>
      <el-timeline>
        <el-timeline-item v-for="f in flows" :key="f.id" :timestamp="f.createTime">
          {{ f.fromStatus || '开始' }} → {{ f.toStatus }} ｜ {{ f.operatorName || '-' }}（{{ f.operatorRole || '-' }}）｜ {{ f.remark || '无备注' }}
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage } from '../../api'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const flows = ref([])
const baseSteps = ['已提交/待审核', '审核通过', '待分配', '待接单', '维修人员已接单', '维修中', '待验收/待确认', '已完成']
const steps = computed(() => baseSteps.includes(detail.value.status) ? baseSteps : [...baseSteps, detail.value.status].filter(Boolean))
const activeStep = computed(() => {
  const idx = steps.value.indexOf(detail.value.status)
  return idx < 0 ? 0 : idx
})

onMounted(async () => {
  const id = route.params.id
  detail.value = await getPage(`/repair-orders/${id}`)
  flows.value = await getPage(`/repair-orders/${id}/flows`)
})
</script>
