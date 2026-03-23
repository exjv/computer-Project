<template>
  <div class="detail-page">
    <div class="toolbar">
      <el-button @click="router.push('/repair-orders')">返回列表</el-button>
      <el-button type="primary" @click="loadAll">刷新</el-button>
    </div>

    <el-row :gutter="12">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="header-row">
              <span>工单详情 - {{ order.orderNo || '-' }}</span>
              <el-tag :type="statusType(order.status)">{{ order.status || '-' }}</el-tag>
            </div>
          </template>

          <h4>基本信息区</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单编号">{{ order.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报修用户">{{ order.reporterName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报修人工号">{{ order.reporterEmployeeNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ order.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ order.reporterDepartment || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地点">{{ order.reportLocation || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备编号">{{ order.deviceCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ order.deviceName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ order.deviceType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="故障类型">{{ order.faultType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="紧急程度">{{ order.priority || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否影响大范围网络">{{ yesNo(order.affectWideAreaNetwork) }}</el-descriptions-item>
            <el-descriptions-item label="故障描述" :span="2">{{ order.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否需要采购配件">{{ yesNo(order.needPurchaseParts) }}</el-descriptions-item>
            <el-descriptions-item label="配件说明">{{ order.partsDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否申请延期">{{ yesNo(order.applyDelay) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户确认结果">{{ order.userConfirmResult || '-' }}</el-descriptions-item>
            <el-descriptions-item label="满意度评分">{{ order.satisfactionScore ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户反馈内容" :span="2">{{ order.feedback || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关闭原因" :span="2">{{ order.closeReason || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 style="margin-top:14px">进度展示</h4>
          <el-progress :percentage="order.progress || 0" :status="(order.progress||0) >= 100 ? 'success' : ''"/>

          <h4 style="margin-top:14px">工单状态时间轴</h4>
          <el-steps :active="activeStep" finish-status="success" process-status="process" align-center>
            <el-step v-for="s in stepNodes" :key="s" :title="s"/>
          </el-steps>

          <h4 style="margin-top:14px">时间信息区</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="报修时间">{{ order.reportTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ order.auditTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ order.auditByName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分配时间">{{ order.assignTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分配人">{{ order.assignByName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="指派维修人员">{{ order.assignMaintainerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接单时间">{{ order.acceptTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="开始维修时间">{{ order.startRepairTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="原预计完成时间">{{ order.originalExpectedFinishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="延期后预计完成时间">{{ order.delayedExpectedFinishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际完成时间">{{ order.finishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验收确认时间">{{ order.confirmTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card style="margin-top:12px">
          <template #header>可操作按钮区</template>
          <div class="action-wrap">
            <el-button v-for="btn in actionButtons" :key="btn.status" :type="btn.type || 'primary'" @click="quickUpdate(btn)">{{ btn.label }}</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>附件图片区</template>
          <div v-if="attachments.length" class="img-grid">
            <a v-for="a in attachments" :key="a.id" :href="a.fileUrl" target="_blank">
              <img :src="a.fileUrl" :alt="a.fileName || '附件'" />
            </a>
          </div>
          <el-empty v-else description="暂无附件"/>
        </el-card>

        <el-card style="margin-top:12px">
          <template #header>流程记录区</template>
          <el-timeline>
            <el-timeline-item v-for="f in flows" :key="f.id" :timestamp="f.operationTime || f.createTime" :type="f.toStatus===order.status ? 'primary' : 'info'">
              <div><b>{{ f.fromStatus || '开始' }} → {{ f.toStatus }}</b></div>
              <div>操作人：{{ f.operatorName || '系统' }}（{{ f.operatorRole || '-' }}）</div>
              <div>操作类型：{{ f.operationType || f.action || '-' }}</div>
              <div>处理说明：{{ f.remark || '-' }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card style="margin-top:12px">
          <template #header>消息/操作日志展示</template>
          <el-tabs>
            <el-tab-pane label="业务日志">
              <el-timeline>
                <el-timeline-item v-for="b in businessLogs" :key="`b-${b.id}`" :timestamp="b.operationTime || b.createTime">
                  <div><b>{{ b.action || '-' }}</b></div>
                  <div>操作人：{{ b.operatorName || '-' }}（{{ b.operatorRole || '-' }}）</div>
                  <div>{{ b.content || '-' }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="!businessLogs.length" description="暂无业务日志"/>
            </el-tab-pane>
            <el-tab-pane label="操作日志">
              <el-timeline>
                <el-timeline-item v-for="l in operationLogs" :key="`o-${l.id}`" :timestamp="l.operationTime || l.createTime">
                  <div><b>{{ l.module || '-' }} / {{ l.operationType || '-' }}</b></div>
                  <div>{{ l.operationDesc || '-' }}</div>
                  <div>URL: {{ l.requestUrl || '-' }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="!operationLogs.length" description="暂无操作日志"/>
            </el-tab-pane>
          </el-tabs>
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
const userStore = useUserStore()
const id = route.params.id

const order = ref({})
const flows = ref([])
const attachments = ref([])
const businessLogs = ref([])
const operationLogs = ref([])

const role = computed(() => userStore.userInfo?.role || '')
const isAdmin = computed(() => role.value === 'admin')
const isMaintainer = computed(() => role.value === 'maintainer')
const isUser = computed(() => role.value === 'user')

const actionButtons = computed(() => {
  const status = order.value.status
  const buttons = []
  if (isAdmin.value && status === '已提交/待审核') buttons.push({ label: '审核通过', status: '审核通过', type: 'success' })
  if (isAdmin.value && status === '已提交/待审核') buttons.push({ label: '审核驳回', status: '审核驳回', type: 'danger' })
  if (isMaintainer.value && status === '待接单') buttons.push({ label: '接单', status: '维修人员已接单' })
  if (isMaintainer.value && status === '维修人员已接单') buttons.push({ label: '开始维修', status: '维修中' })
  if (isMaintainer.value && ['维修中', '延期已批准'].includes(status)) buttons.push({ label: '提交完工', status: '已完成', type: 'success' })
  if (isUser.value && status === '待验收/待确认') buttons.push({ label: '确认完成', status: '已完成', type: 'success' })
  if (isAdmin.value && !['已关闭', '已取消'].includes(status)) buttons.push({ label: '关闭工单', status: '已关闭', type: 'warning' })
  return buttons
})

const stepNodes = ['已提交/待审核', '审核通过', '待接单', '维修中', '待验收/待确认', '已完成/已关闭']
const stepIndexMap = {
  '已提交/待审核': 0,
  '审核通过': 1,
  '待分配': 1,
  '已分配': 2,
  '待接单': 2,
  '维修人员已接单': 2,
  '维修中': 3,
  '待采购/待配件': 3,
  '申请延期中': 3,
  '延期已批准': 3,
  '待验收/待确认': 4,
  '已完成': 5,
  '已关闭': 5,
  '已取消': 5,
  '审核驳回': 1
}
const activeStep = computed(() => stepIndexMap[order.value.status] ?? 0)

const loadAll = async () => {
  order.value = await getPage(`/repair-orders/${id}`)
  const records = await getPage(`/repair-orders/${id}/records`)
  flows.value = records.flows || []
  businessLogs.value = records.businessLogs || []
  operationLogs.value = records.operationLogs || []
  attachments.value = await getPage(`/repair-orders/${id}/attachments`)
}

const quickUpdate = async (btn) => {
  const payload = { status: btn.status }
  if (btn.status === '已关闭') {
    const { value } = await ElMessageBox.prompt('请输入关闭原因', '关闭工单')
    payload.closeReason = value
  }
  if (btn.status === '已完成' && isUser.value) {
    const { value } = await ElMessageBox.prompt('请输入用户反馈内容（可选）', '反馈')
    payload.userConfirmResult = '已解决'
    payload.feedback = value
    payload.satisfactionScore = 5
  }
  await putApi(`/repair-orders/${id}/status`, payload)
  ElMessage.success('操作成功')
  await loadAll()
}

const statusType = (status) => ({
  '已提交/待审核': 'warning',
  '审核通过': 'success',
  '审核驳回': 'danger',
  '待分配': 'info',
  '待接单': 'warning',
  '维修人员已接单': 'info',
  '维修中': 'primary',
  '待验收/待确认': 'warning',
  '已完成': 'success',
  '已关闭': 'info',
  '已取消': 'danger'
}[status] || 'info')

const yesNo = (v) => (v === 1 ? '是' : '否')

onMounted(loadAll)
</script>

<style scoped>
.detail-page { display:flex; flex-direction:column; gap:12px; }
.toolbar { display:flex; justify-content:space-between; align-items:center; }
.header-row { display:flex; justify-content:space-between; align-items:center; }
.action-wrap { display:flex; gap:10px; flex-wrap:wrap; }
.img-grid { display:grid; grid-template-columns: repeat(2, 1fr); gap:8px; }
.img-grid img { width:100%; height:96px; object-fit:cover; border-radius:4px; border:1px solid #ebeef5; }
</style>
