<template>
  <div>
    <el-page-header @back="router.back()" content="工单详情"/>
    <el-card style="margin-top:12px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>工单编号：{{ detail.orderNo }}</span>
          <el-tag type="primary">{{ detail.status }}</el-tag>
        </div>
      </template>

      <el-divider>工单状态时间轴</el-divider>
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="(s, idx) in statusSteps" :key="s" :title="s">
          <template #description>
            <span v-if="idx === activeStep" style="color:#409EFF;font-weight:600">当前节点</span>
          </template>
        </el-step>
      </el-steps>

      <el-divider>基本信息区</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报修用户">{{ detail.reporterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修人工号">{{ detail.reporterEmployeeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属部门/地点">{{ (detail.reporterDepartment||'-') + ' / ' + (detail.reportLocation||'-') }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detail.deviceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ detail.deviceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障类型">{{ detail.faultType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障描述">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ detail.priority || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否影响大范围网络">{{ detail.affectWideAreaNetwork===1?'是':'否' }}</el-descriptions-item>
        <el-descriptions-item label="当前进度百分比">{{ detail.progress || 0 }}%</el-descriptions-item>
        <el-descriptions-item label="是否需要采购配件">{{ detail.needPurchaseParts===1?'是':'否' }}</el-descriptions-item>
        <el-descriptions-item label="配件说明">{{ detail.partsDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否申请延期">{{ detail.applyDelay===1?'是':'否' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>时间信息区</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报修时间">{{ detail.reportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detail.auditTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.auditByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分配时间">{{ detail.assignTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分配人">{{ detail.assignByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="指派维修人员">{{ detail.assignMaintainerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始维修时间">{{ detail.startRepairTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="原预计完成时间">{{ detail.originalExpectedFinishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="延期后预计完成时间">{{ detail.delayedExpectedFinishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际完成时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="验收确认时间">{{ detail.confirmTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户确认结果">{{ detail.userConfirmResult || '-' }}</el-descriptions-item>
        <el-descriptions-item label="满意度评分">{{ detail.satisfactionScore || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户反馈内容">{{ detail.feedback || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关闭原因">{{ detail.closeReason || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>预计修复时间预测</el-divider>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="预计完成时间">{{ estimate.estimatedFinishTime || detail.expectedFinishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预测工时（小时）">{{ estimate.estimatedHours || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预测依据说明">{{ estimate.basis || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>附件图片区</el-divider>
      <el-empty v-if="!attachments.length" description="暂无附件"/>
      <el-row v-else :gutter="12">
        <el-col :span="6" v-for="a in attachments" :key="a.id" style="margin-bottom:12px">
          <el-image :src="a.fileUrl" fit="cover" style="width:100%;height:120px" :preview-src-list="attachments.map(v=>v.fileUrl)"/>
          <div style="font-size:12px;margin-top:4px">{{ a.fileName }}</div>
        </el-col>
      </el-row>

      <el-divider>每一步处理记录</el-divider>
      <el-timeline>
        <el-timeline-item v-for="f in flows" :key="f.id" :timestamp="f.createTime">
          <el-card shadow="never">
            <div style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap">
              <span><b>状态变化：</b>{{ f.fromStatus || '开始' }} → {{ f.toStatus || '-' }}</span>
              <span><b>操作类型：</b>{{ f.operationType || f.action || '-' }}</span>
            </div>
            <div style="margin-top:6px"><b>操作人：</b>{{ f.operatorName || '-' }}（{{ f.operatorRole || '-' }}）</div>
            <div style="margin-top:6px"><b>处理意见：</b>{{ f.remark || '无备注' }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-divider>消息/操作日志展示</el-divider>
      <el-empty v-if="!businessLogs.length" description="暂无操作日志"/>
      <el-timeline v-else>
        <el-timeline-item v-for="l in businessLogs" :key="l.id" :timestamp="l.createTime" type="primary">
          <el-card shadow="never">
            <div><b>{{ l.action }}</b> ｜ {{ l.status || '-' }}</div>
            <div style="margin-top:6px"><b>操作人：</b>{{ l.operatorName || '-' }}</div>
            <div style="margin-top:6px"><b>说明：</b>{{ l.content || '-' }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-divider>可操作按钮区</el-divider>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <el-button v-if="isAdmin && detail.status==='已提交/待审核'" type="primary" @click="doAction('ADMIN_APPROVE')">审核通过</el-button>
        <el-button v-if="isAdmin && detail.status==='已提交/待审核'" type="danger" @click="doAction('ADMIN_REJECT')">审核驳回</el-button>
        <el-button v-if="isMaintainer && detail.status==='待接单'" type="primary" @click="doAction('MAINTAINER_ACCEPT')">接单</el-button>
        <el-button v-if="isMaintainer && detail.status==='维修人员已接单'" type="warning" @click="doAction('MAINTAINER_START')">开始维修</el-button>
        <el-button v-if="isMaintainer && detail.status==='维修中'" type="success" @click="doAction('MAINTAINER_FINISH')">提交完工</el-button>
        <el-button v-if="isUser && detail.status==='待验收/待确认'" type="success" @click="doAction('USER_CONFIRM_RESOLVED')">确认已解决</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage, putApi } from '../../api'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const flows = ref([])
const attachments = ref([])
const businessLogs = ref([])
const estimate = ref({})
const role = computed(() => useUserStore().userInfo.role)
const isAdmin = computed(() => role.value === 'admin')
const isMaintainer = computed(() => role.value === 'maintainer')
const isUser = computed(() => role.value === 'user')
const baseStatusSteps = [
  '已提交/待审核',
  '审核通过',
  '待分配',
  '待接单',
  '维修人员已接单',
  '维修中',
  '待验收/待确认',
  '已完成'
]
const statusSteps = computed(() => {
  const current = detail.value.status
  if (!current) return baseStatusSteps
  return baseStatusSteps.includes(current) ? baseStatusSteps : [...baseStatusSteps, current]
})
const activeStep = computed(() => {
  const idx = statusSteps.value.indexOf(detail.value.status)
  return idx < 0 ? 0 : idx
})

const load = async () => {
  const id = route.params.id
  detail.value = await getPage(`/repair-orders/${id}`)
  flows.value = await getPage(`/repair-orders/${id}/flows`)
  attachments.value = await getPage(`/repair-orders/${id}/attachments`)
  businessLogs.value = await getPage(`/repair-orders/${id}/business-logs`)
  estimate.value = await getPage(`/repair-orders/${id}/estimate-finish-time`)
}

const doAction = async (action) => {
  await putApi(`/repair-orders/${route.params.id}/action`, { action })
  ElMessage.success('操作成功')
  await load()
}

onMounted(load)
</script>
