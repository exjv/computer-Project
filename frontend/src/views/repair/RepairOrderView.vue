<template>
  <div>
    <el-card>
      <template #header>
        <div class="header-row">
          <span>{{ isUser ? '我的报修工单' : (isMaintainer ? '我的维修工单' : '工单管理') }}</span>
          <el-button v-if="canCreate" type="primary" @click="openCreate">新建工单</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query">
        <el-form-item label="工单编号"><el-input v-model="query.orderNo" clearable/></el-form-item>
        <el-form-item label="设备名称"><el-input v-model="query.title" clearable/></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width:160px">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s"/>
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="query.priority" clearable style="width:100px">
            <el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/>
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>

      <el-card shadow="never" style="margin-top:10px">
        <div>预测样本：{{ stats.predictionComparableCount || 0 }}，平均绝对误差：{{ stats.predictionAvgAbsErrorHours || 0 }}h，4h内命中：{{ stats.predictionWithin4hCount || 0 }}，24h内命中：{{ stats.predictionWithin24hCount || 0 }}</div>
      </el-card>

      <el-table :data="list" style="margin-top: 12px">
        <el-table-column prop="orderNo" label="工单编号" width="170"/>
        <el-table-column prop="reporterName" label="报修用户" width="110"/>
        <el-table-column prop="deviceName" label="设备名称" min-width="160"/>
        <el-table-column prop="faultType" label="故障类型" width="120"/>
        <el-table-column prop="priority" label="紧急程度" width="90"/>
        <el-table-column prop="status" label="当前状态" width="130"/>
        <el-table-column prop="progress" label="进度" width="80">
          <template #default="s">{{ s.row.progress || 0 }}%</template>
        </el-table-column>
        <el-table-column prop="reportTime" label="报修时间" width="170"/>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="s">
            <el-button link @click="goDetail(s.row)">详情</el-button>
            <el-button v-if="canEdit(s.row)" link @click="openEdit(s.row)">编辑</el-button>
            <el-button v-if="isAdmin" link @click="openAssign(s.row)">分配</el-button>
            <el-button v-if="canDelete(s.row)" link type="danger" @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top:12px"
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="page.size"
        :current-page="page.current"
        @current-change="onPageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editMode ? '编辑工单' : '新建工单'" width="720px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="设备"><el-select v-model="form.deviceId" style="width:100%"><el-option v-for="d in devices" :key="d.id" :label="`${d.deviceCode} - ${d.deviceName}`" :value="d.id"/></el-select></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contactPhone"/></el-form-item>
        <el-form-item label="所属部门"><el-input v-model="form.reporterDepartment"/></el-form-item>
        <el-form-item label="地点"><el-input v-model="form.reportLocation"/></el-form-item>
        <el-form-item label="故障类型"><el-input v-model="form.faultType"/></el-form-item>
        <el-form-item label="故障描述"><el-input v-model="form.description" type="textarea"/></el-form-item>
        <el-form-item label="紧急程度"><el-select v-model="form.priority" style="width:100%"><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item>
        <el-form-item label="影响大范围网络"><el-switch v-model="form.affectWideAreaNetwork" :active-value="1" :inactive-value="0"/></el-form-item>
        <el-form-item label="需采购配件"><el-switch v-model="form.needPurchaseParts" :active-value="1" :inactive-value="0"/></el-form-item>
        <el-form-item label="配件说明"><el-input v-model="form.partsDescription"/></el-form-item>
        <el-form-item label="原预计完成"><el-date-picker v-model="form.originalExpectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="分配维修人员" width="980px">
      <el-alert title="系统根据工单优先级、人员负载、历史能力进行推荐，可手动调整" type="info" :closable="false" style="margin-bottom:10px"/>
      <el-table :data="recommendations" size="small" max-height="220">
        <el-table-column prop="maintainerName" label="维修人员" width="100"/>
        <el-table-column prop="recommendationScore" label="推荐分" width="80"/>
        <el-table-column prop="unfinishedCount" label="未完成" width="70"/>
        <el-table-column prop="processingCount" label="处理中" width="70"/>
        <el-table-column prop="avgHandleHours" label="均时(h)" width="75"/>
        <el-table-column prop="recommendReason" label="推荐说明" min-width="220" show-overflow-tooltip/>
        <el-table-column label="选择" width="70"><template #default="s"><el-button link @click="assignForm.assignMaintainerId=s.row.maintainerId">选中</el-button></template></el-table-column>
      </el-table>
      <el-form :model="assignForm" label-width="120px">
        <el-form-item label="维修人员">
          <el-select v-model="assignForm.assignMaintainerId" style="width:100%">
            <el-option v-for="m in maintainers" :key="m.id" :label="`${m.realName}(${m.employeeNo || m.username})`" :value="m.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog=false">取消</el-button>
        <el-button type="primary" @click="saveAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { delApi, getPage, postApi, putApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isAdmin = computed(() => role.value === 'admin')
const isUser = computed(() => role.value === 'user')
const isMaintainer = computed(() => role.value === 'maintainer')
const canCreate = computed(() => isUser.value || isAdmin.value)

const query = reactive({ orderNo: '', title: '', status: '', priority: '' })
const page = reactive({ current: 1, size: 10 })
const list = ref([])
const total = ref(0)
const statusOptions = ref([])
const stats = ref({})

const devices = ref([])
const maintainers = ref([])
const recommendations = ref([])
const dialogVisible = ref(false)
const editMode = ref(false)
const assignDialog = ref(false)
const assignId = ref(null)
const form = reactive({ deviceId: null, contactPhone: '', reporterDepartment: '', reportLocation: '', faultType: '', description: '', priority: '中', affectWideAreaNetwork: 0, needPurchaseParts: 0, partsDescription: '', originalExpectedFinishTime: '', remark: '' })
const assignForm = reactive({ assignMaintainerId: null })

const load = async () => {
  const path = isAdmin.value ? '/repair-orders/page' : '/repair-orders/my'
  const data = await getPage(path, { ...query, ...page })
  list.value = data.records || []
  total.value = data.total || 0
  stats.value = await getPage('/repair-orders/statistics')
}

const reset = async () => {
  Object.assign(query, { orderNo: '', title: '', status: '', priority: '' })
  page.current = 1
  await load()
}

const onPageChange = async (p) => {
  page.current = p
  await load()
}

const goDetail = (row) => router.push(`/repair-orders/${row.id}`)
const canEdit = (row) => isAdmin.value || (isUser.value && row.reporterId === userStore.userInfo.id)
const canDelete = (row) => canEdit(row)

const openCreate = () => {
  editMode.value = false
  Object.assign(form, { id: null, deviceId: null, contactPhone: '', reporterDepartment: '', reportLocation: '', faultType: '', description: '', priority: '中', affectWideAreaNetwork: 0, needPurchaseParts: 0, partsDescription: '', originalExpectedFinishTime: '', remark: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  editMode.value = true
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

const save = async () => {
  if (!form.deviceId) return ElMessage.warning('请选择设备')
  if (!form.description) return ElMessage.warning('请输入故障描述')
  if (editMode.value) {
    await putApi(`/repair-orders/${form.id}`, form)
    ElMessage.success('修改成功')
  } else {
    await postApi('/repair-orders', form)
    ElMessage.success('提交成功')
  }
  dialogVisible.value = false
  await load()
}

const remove = async (row) => {
  await ElMessageBox.confirm('确认删除该工单？', '提示', { type: 'warning' })
  await delApi(`/repair-orders/${row.id}`)
  ElMessage.success('删除成功')
  await load()
}

const openAssign = async (row) => {
  assignId.value = row.id
  assignForm.assignMaintainerId = row.assignMaintainerId || null
  recommendations.value = await getPage(`/repair-orders/${row.id}/assign-recommendations`)
  assignDialog.value = true
}

const saveAssign = async () => {
  if (!assignForm.assignMaintainerId) return ElMessage.warning('请选择维修人员')
  await putApi(`/repair-orders/${assignId.value}/assign`, { assignMaintainerId: assignForm.assignMaintainerId })
  ElMessage.success('分配成功')
  assignDialog.value = false
  await load()
}

onMounted(async () => {
  statusOptions.value = await getPage('/repair-orders/status-options')
  const devicePage = await getPage('/devices/page', { current: 1, size: 200 })
  devices.value = devicePage.records || []
  const userPage = await getPage('/users/page', { current: 1, size: 200 })
  maintainers.value = (userPage.records || []).filter(u => u.role === 'maintainer')
  await load()
})
</script>

<style scoped>
.header-row { display:flex; justify-content:space-between; align-items:center; }
</style>
