<template>
  <div class="detail-page">
    <div class="top-bar">
      <el-button @click="router.back()">返回</el-button>
      <div style="display:flex;gap:8px">
        <el-button @click="router.push(`/repair-orders/${id}/progress`)">进度跟踪页</el-button>
        <el-button type="primary" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="header-row">
              <span>{{ order.title || '工单详情' }}</span>
              <el-tag :type="statusTag(order.status)">{{ order.status }}</el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单编号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="优先级">{{ order.priority }}</el-descriptions-item>
            <el-descriptions-item label="报修人ID">{{ order.reporterId }}</el-descriptions-item>
            <el-descriptions-item label="维修人员ID">{{ order.assignMaintainerId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报修时间">{{ order.reportTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="进度">{{ order.progress || 0 }}%</el-descriptions-item>
            <el-descriptions-item label="处理说明" :span="2">{{ order.handleDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="故障描述" :span="2">{{ order.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="现场图片" :span="2">{{ order.scenePhotoUrls || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关闭原因" :span="2">{{ order.closeReason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card style="margin-top:12px">
          <template #header>流程节点</template>
          <el-steps :active="activeStep" align-center finish-status="success">
            <el-step v-for="s in stepLabels" :key="s" :title="s" />
          </el-steps>
          <div class="action-row">
            <el-button v-for="a in actionButtons" :key="a.action" :type="a.type || 'primary'" plain @click="doAction(a.action)">{{ a.label }}</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>业务流程时间轴</template>
          <el-timeline>
            <el-timeline-item v-for="f in flows" :key="f.id" :timestamp="f.createTime" :type="f.toStatus === order.status ? 'primary' : ''">
              <b>{{ f.fromStatus || '开始' }} → {{ f.toStatus }}</b>
              <div>{{ f.action }} ｜ {{ f.remark || '无备注' }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card style="margin-top:12px">
          <template #header>消息/操作日志</template>
          <el-empty v-if="!logs.length" description="暂无日志" />
          <div v-for="l in logs" :key="l.id" class="log-item">
            <div><b>{{ l.operationType }}</b> - {{ l.module }}</div>
            <div>{{ l.operationDesc }}</div>
            <div class="time">{{ l.operationTime }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage, putApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const id = route.params.id

const order = ref({})
const flows = ref([])
const logs = ref([])

const stepLabels = ['已提交/待审核', '待分配', '待接单', '维修人员已接单', '维修中', '待验收/待确认', '已完成/已关闭']
const stepMap = {
  '已提交/待审核': 0, '审核通过': 1, '待分配': 1, '已分配': 2, '待接单': 2,
  '维修人员已接单': 3, '维修中': 4, '待采购/待配件': 4, '申请延期中': 4, '延期已批准': 4,
  '待验收/待确认': 5, '已完成': 6, '已关闭': 6, '已取消': 6, '审核驳回': 0
}
const activeStep = computed(() => stepMap[order.value.status] ?? 0)

const statusTag = (s) => {
  const map = { '已提交/待审核':'warning','审核通过':'success','审核驳回':'danger','待分配':'warning','已分配':'warning','待接单':'warning',
    '维修人员已接单':'primary','维修中':'primary','待采购/待配件':'warning','申请延期中':'warning','延期已批准':'success','待验收/待确认':'info',
    '已完成':'success','已关闭':'info','已取消':'info' }
  return map[s] || 'info'
}

const role = computed(() => store.userInfo.role)
const actionButtons = computed(() => {
  const s = order.value.status
  const buttons = []
  if (store.hasPerm('repair:audit') && s === '已提交/待审核') buttons.push({ label:'审核通过', action:'ADMIN_APPROVE' })
  if (store.hasPerm('repair:reject') && s === '已提交/待审核') buttons.push({ label:'审核驳回', action:'ADMIN_REJECT', type:'warning' })
  if (store.hasPerm('repair:accept') && s === '待接单') buttons.push({ label:'接单', action:'MAINTAINER_ACCEPT' })
  if (store.hasPerm('repair:start') && s === '维修人员已接单') buttons.push({ label:'开始维修', action:'MAINTAINER_START' })
  if (store.hasPerm('repair:finish') && ['维修中','延期已批准','待采购/待配件'].includes(s)) buttons.push({ label:'提交完工', action:'MAINTAINER_FINISH' })
  if (store.hasPerm('repair:confirm') && s === '待验收/待确认') buttons.push({ label:'确认修复', action:'USER_CONFIRM_RESOLVED', type:'success' })
  if (store.hasPerm('repair:cancel') && ['已提交/待审核','审核驳回'].includes(s)) buttons.push({ label:'撤销报修', action:'USER_CANCEL', type:'danger' })
  if (store.hasPerm('repair:close') && !['已完成','已关闭','已取消'].includes(s)) buttons.push({ label:'关闭工单', action:'ADMIN_CLOSE', type:'danger' })
  return buttons
})

const loadAll = async () => {
  order.value = await getPage(`/repair-orders/${id}`)
  const records = await getPage(`/repair-orders/${id}/records`)
  flows.value = records.flows || []
  logs.value = records.logs || []
}

const doAction = async (action) => {
  const payload = { action }
  if (action === 'ADMIN_CLOSE' || action === 'USER_CANCEL') {
    const { value } = await ElMessageBox.prompt('请输入原因', '原因说明')
    payload.remark = value
  }
  if (action === 'USER_CONFIRM_RESOLVED') {
    const { value } = await ElMessageBox.prompt('请输入评价（可选）', '维修评价', { inputPlaceholder: '处理及时，体验良好' })
    payload.feedback = value
    payload.satisfactionScore = 5
  }
  await putApi(`/repair-orders/${id}/action`, payload)
  ElMessage.success('操作成功')
  await loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.detail-page { display:flex; flex-direction:column; gap:12px; }
.top-bar { display:flex; justify-content:space-between; align-items:center; }
.header-row { display:flex; justify-content:space-between; align-items:center; }
.action-row { margin-top: 14px; display:flex; flex-wrap:wrap; gap:8px; }
.log-item { padding:8px 0; border-bottom:1px solid #f0f0f0; }
.time { color:#909399; font-size:12px; }
</style>
