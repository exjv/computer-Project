<template>
  <el-card>
    <template #header>报修申请页</template>
    <el-form :model="form" label-width="120px">
      <el-form-item label="设备"><el-select v-model="form.deviceId" style="width:100%"><el-option v-for="d in devices" :key="d.id" :label="`${d.deviceCode} - ${d.deviceName}`" :value="d.id"/></el-select></el-form-item>
      <el-form-item label="审批策略">
        <el-alert v-if="selectedDevice" :type="selectedDevice.repairApprovalRequired===1 ? 'warning' : 'success'" :closable="false"
          :title="selectedDevice.repairApprovalRequired===1 ? '该设备报修需管理员审核，审核通过后进入分配流程' : '该设备报修后将直接进入待分配流程'" />
      </el-form-item>
      <el-form-item label="联系方式"><el-input v-model="form.contactPhone"/></el-form-item>
      <el-form-item label="所属部门"><el-input v-model="form.reporterDepartment"/></el-form-item>
      <el-form-item label="地点"><el-input v-model="form.reportLocation"/></el-form-item>
      <el-form-item label="故障类型"><el-input v-model="form.faultType"/></el-form-item>
      <el-form-item label="故障描述"><el-input v-model="form.description" type="textarea"/></el-form-item>
      <el-form-item label="紧急程度"><el-select v-model="form.priority" style="width:100%"><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item>
      <el-form-item label="影响大范围网络"><el-switch v-model="form.affectWideAreaNetwork" :active-value="1" :inactive-value="0"/></el-form-item>
      <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
    </el-form>
    <div style="display:flex;justify-content:flex-end;gap:8px">
      <el-button @click="router.push('/my-repairs')">查看本人报修记录</el-button>
      <el-button type="primary" @click="submitApply">提交报修</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getPage, postApi } from '../../api'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const devices = ref([])

const form = reactive({
  deviceId: null,
  contactPhone: '',
  reporterDepartment: '',
  reportLocation: '',
  faultType: '',
  description: '',
  priority: '中',
  affectWideAreaNetwork: 0,
  remark: ''
})

const selectedDevice = computed(() => devices.value.find(d => d.id === form.deviceId))

const submitApply = async () => {
  if (!form.deviceId) return ElMessage.warning('请选择设备')
  if (!form.description) return ElMessage.warning('请填写故障描述')
  await postApi('/repair-orders/apply', form)
  ElMessage.success('报修提交成功')
  router.push('/my-repairs')
}

onMounted(async () => {
  const r = await getPage('/devices/page', { current: 1, size: 200 })
  devices.value = r.records || []
})
</script>
