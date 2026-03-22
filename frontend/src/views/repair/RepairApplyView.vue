<template>
  <div>
    <h2>报修申请</h2>
    <el-card>
      <el-form :model="form" label-width="120px">
        <el-form-item label="设备">
          <el-select v-model="form.deviceId" style="width:100%">
            <el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="故障标题"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="故障描述"><el-input type="textarea" v-model="form.description"/></el-form-item>
        <el-form-item label="故障类型"><el-input v-model="form.faultType"/></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contactPhone"/></el-form-item>
        <el-form-item label="报修地点"><el-input v-model="form.reportLocation"/></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority">
            <el-option label="低" value="低"/>
            <el-option label="中" value="中"/>
            <el-option label="高" value="高"/>
          </el-select>
        </el-form-item>
        <el-form-item label="影响范围">
          <el-switch v-model="form.affectWideAreaNetwork" :active-value="1" :inactive-value="0" active-text="影响大范围网络"/>
        </el-form-item>
        <el-form-item label="原预计完成">
          <el-date-picker v-model="form.originalExpectedFinishTime" value-format="YYYY-MM-DD HH:mm:ss" type="datetime"/>
        </el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
      </el-form>
      <el-button type="primary" @click="submit">提交报修</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPage, postApi } from '../../api'
import { ElMessage } from 'element-plus'

const devices = ref([])
const form = reactive({
  deviceId: '',
  title: '',
  description: '',
  faultType: '',
  contactPhone: '',
  reportLocation: '',
  priority: '中',
  affectWideAreaNetwork: 0,
  originalExpectedFinishTime: '',
  remark: ''
})

const submit = async () => {
  await postApi('/repair-orders', form)
  ElMessage.success('提交成功')
  Object.assign(form, {
    deviceId: '', title: '', description: '', faultType: '', contactPhone: '', reportLocation: '',
    priority: '中', affectWideAreaNetwork: 0, originalExpectedFinishTime: '', remark: ''
  })
}

onMounted(async () => {
  const d = await getPage('/devices/page', { current: 1, size: 200 })
  devices.value = d.records || []
})
</script>
