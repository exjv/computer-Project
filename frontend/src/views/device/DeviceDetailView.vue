<template>
  <div>
    <el-page-header @back="router.back()" content="设备详情档案"/>
    <el-card style="margin-top:12px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ device.deviceName }}（{{ device.deviceCode }}）</span>
          <el-tag :type="statusTag(device.status)">{{ device.status }}</el-tag>
        </div>
      </template>

      <el-row :gutter="12" style="margin-bottom:12px">
        <el-col :span="6"><el-card shadow="never"><div>历史维修次数</div><h3>{{ profile.totalRepairs || 0 }}</h3></el-card></el-col>
        <el-col :span="6"><el-card shadow="never"><div>最近维修记录</div><div>{{ (profile.recentRecords||[])[0]?.startTime || '-' }}</div></el-card></el-col>
        <el-col :span="6"><el-card shadow="never"><div>保修状态</div><el-tag :type="profile.inWarranty?'success':'danger'">{{ profile.inWarranty?'保修期内':'已过保' }}</el-tag></el-card></el-col>
        <el-col :span="6"><el-card shadow="never"><div>高故障预警</div><el-tag :type="profile.isHighFault?'danger':'success'">{{ profile.isHighFault?'已触发':'未触发' }}</el-tag></el-card></el-col>
      </el-row>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="设备类型">{{ device.deviceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品牌型号">{{ (device.brand||'-') + ' / ' + (device.model||'-') }}</el-descriptions-item>
        <el-descriptions-item label="序列号">{{ device.serialNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ device.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="管理部门">{{ device.managementDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="责任人">{{ device.ownerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建议更换">{{ profile.needReplace ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="建议重点巡检">{{ profile.suggestPatrol ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="最近故障时间">{{ profile.recentFaultTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>维修时间线</el-divider>
      <el-timeline>
        <el-timeline-item v-for="t in timeline" :key="t.key" :timestamp="t.time">{{ t.text }}</el-timeline-item>
      </el-timeline>

      <el-divider>故障原因统计</el-divider>
      <el-empty v-if="!Object.keys(profile.faultReasonStats||{}).length" description="暂无统计"/>
      <el-tag v-for="(count, reason) in profile.faultReasonStats || {}" :key="reason" style="margin-right:8px">{{ reason }}: {{ count }}</el-tag>

      <el-divider>维修照片</el-divider>
      <el-row :gutter="12">
        <el-col :span="6" v-for="p in photos" :key="p.id" style="margin-bottom:10px">
          <el-image :src="p.fileUrl" style="width:100%;height:120px" fit="cover" :preview-src-list="photos.map(v=>v.fileUrl)"/>
          <div style="font-size:12px">{{ p.fileName }}</div>
        </el-col>
      </el-row>
      <el-empty v-if="!photos.length" description="暂无照片"/>

      <el-form :model="uploadForm" label-width="110px" style="margin-top:10px">
        <el-form-item label="照片分类"><el-select v-model="uploadForm.category"><el-option label="设备照片" value="DEVICE_PHOTO"/><el-option label="故障现场" value="FAULT_SCENE"/><el-option label="维修完成" value="REPAIR_RESULT"/></el-select></el-form-item>
        <el-form-item label="图片名称"><el-input v-model="uploadForm.fileName"/></el-form-item>
        <el-form-item label="图片URL"><el-input v-model="uploadForm.fileUrl"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="uploadForm.remark"/></el-form-item>
        <el-button type="primary" @click="upload">上传图片</el-button>
      </el-form>

      <el-divider>关联工单列表</el-divider>
      <el-table :data="profile.recentOrders || []" size="small">
        <el-table-column prop="orderNo" label="工单编号" width="170"/>
        <el-table-column prop="title" label="标题"/>
        <el-table-column prop="status" label="状态" width="150"/>
        <el-table-column prop="reportTime" label="报修时间" width="180"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage, postApi } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const device = reactive({})
const profile = ref({})
const photos = ref([])
const uploadForm = reactive({ category: 'DEVICE_PHOTO', fileName: '', fileUrl: '', remark: '' })

const timeline = computed(() => {
  const orderItems = (profile.value.recentOrders || []).map(v => ({ key: `o-${v.id}`, time: v.reportTime, text: `工单 ${v.orderNo}：${v.status}` }))
  const recordItems = (profile.value.recentRecords || []).map(v => ({ key: `r-${v.id}`, time: v.startTime, text: `维修记录：${v.faultReason || '-'} -> ${v.repairResult || '-'}` }))
  return [...orderItems, ...recordItems].sort((a, b) => (a.time > b.time ? -1 : 1))
})

const statusTag = status => status === '正常' ? 'success' : (status === '维修中' ? 'warning' : (status === '停用' ? 'info' : 'danger'))

const load = async () => {
  const id = route.params.id
  profile.value = await getPage(`/devices/${id}/profile`)
  Object.assign(device, profile.value.device || {})
  photos.value = await getPage(`/devices/${id}/attachments`)
}

const upload = async () => {
  await postApi(`/devices/${route.params.id}/attachments`, uploadForm)
  ElMessage.success('上传成功')
  Object.assign(uploadForm, { category: 'DEVICE_PHOTO', fileName: '', fileUrl: '', remark: '' })
  await load()
}

onMounted(load)
</script>
