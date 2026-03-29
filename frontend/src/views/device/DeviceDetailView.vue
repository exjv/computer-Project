<template>
  <div>
    <el-page-header @back="router.push('/devices')" content="设备详情" />

    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="8"><el-card><template #header>基本信息卡片</template>
        <div>设备编号：{{ d.deviceCode || '-' }}</div>
        <div>设备名称：{{ d.deviceName || '-' }}</div>
        <div>设备类型：{{ d.deviceType || '-' }}</div>
        <div>品牌/型号：{{ d.brand || '-' }}/{{ d.model || '-' }}</div>
        <div>序列号：{{ d.serialNumber || '-' }}</div>
        <div>责任人：{{ d.ownerName || '-' }}</div>
        <div>管理部门：{{ d.manageDepartment || d.managementDept || '-' }}</div>
        <div>位置：{{ d.campus || '-' }} {{ d.buildingLocation || '-' }}</div>
      </el-card></el-col>

      <el-col :span="8"><el-card><template #header>状态与保修</template>
        <div>当前状态：<el-tag :type="statusType(detail.currentStatus)">{{ detail.currentStatus || '-' }}</el-tag></div>
        <div style="margin-top:8px">保修截止：{{ d.warrantyExpiryDate || d.warrantyExpireDate || '-' }}</div>
        <div>是否在保修期：<el-tag :type="detail.inWarranty?'success':'danger'">{{ detail.inWarranty ? '是' : '否' }}</el-tag></div>
        <div>高故障预警：<el-tag :type="detail.highFaultWarning?'danger':'success'">{{ detail.highFaultWarning?'触发':'未触发' }}</el-tag></div>
        <div style="color:#999">阈值：{{ detail.highFaultThreshold || '-' }}</div>
      </el-card></el-col>

      <el-col :span="8"><el-card><template #header>处置建议</template>
        <div>建议：<el-tag :type="detail.suggestReplace?'danger':(detail.suggestInspect?'warning':'success')">{{ detail.recommendation || '-' }}</el-tag></div>
        <div style="margin-top:8px">历史维修次数：{{ detail.historyRepairCount || 0 }}</div>
        <div>最近维修记录：{{ detail.recentRepairRecord?.faultReason || '-' }}</div>
        <div>最近维修时间：{{ detail.recentRepairRecord?.repairTime || '-' }}</div>
      </el-card></el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="6"><el-card><div>累计工单</div><h2>{{ detail.stats?.totalOrders || 0 }}</h2></el-card></el-col>
      <el-col :span="6"><el-card><div>活跃工单</div><h2>{{ detail.stats?.activeOrders || 0 }}</h2></el-card></el-col>
      <el-col :span="6"><el-card><div>近90天故障</div><h2>{{ detail.stats?.recent90dFaults || 0 }}</h2></el-card></el-col>
      <el-col :span="6"><el-card><div>累计维修</div><h2>{{ detail.stats?.totalRepairs || 0 }}</h2></el-card></el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="12"><el-card><template #header>故障原因统计</template>
        <el-empty v-if="!Object.keys(detail.faultReasonStats||{}).length" description="暂无故障原因"/>
        <el-tag v-for="(v,k) in detail.faultReasonStats || {}" :key="k" style="margin-right:8px;margin-bottom:8px">{{ k }}: {{ v }}</el-tag>
      </el-card></el-col>

      <el-col :span="12"><el-card>
        <template #header><div class="header-action"><span>维修留痕图片</span><el-button type="primary" size="small" @click="openUpload('DEVICE_PHOTO')">上传设备照片</el-button></div></template>
        <h4>设备照片</h4>
        <div class="photo-grid" v-if="(detail.devicePhotos||[]).length">
          <div v-for="it in detail.devicePhotos" :key="it.id" class="photo-item"><el-image :src="it.fileUrl" :preview-src-list="fileUrls(detail.devicePhotos)" fit="cover" style="width:100%;height:100px"/><div class="photo-title">{{ it.fileName || '-' }}</div></div>
        </div>
        <el-empty v-else description="暂无设备照片"/>

        <h4>故障现场照片 <el-button type="warning" link @click="openUpload('FAULT_SCENE')">上传</el-button></h4>
        <div class="photo-grid" v-if="(detail.faultScenePhotos||[]).length">
          <div v-for="it in detail.faultScenePhotos" :key="it.id" class="photo-item"><el-image :src="it.fileUrl" :preview-src-list="fileUrls(detail.faultScenePhotos)" fit="cover" style="width:100%;height:100px"/><div class="photo-title">{{ it.fileName || '-' }}</div></div>
        </div>
        <el-empty v-else description="暂无故障现场照片"/>

        <h4>维修完成照片 <el-button type="success" link @click="openUpload('REPAIR_RESULT')">上传</el-button></h4>
        <div class="photo-grid" v-if="(detail.repairResultPhotos||[]).length">
          <div v-for="it in detail.repairResultPhotos" :key="it.id" class="photo-item"><el-image :src="it.fileUrl" :preview-src-list="fileUrls(detail.repairResultPhotos)" fit="cover" style="width:100%;height:100px"/><div class="photo-title">{{ it.fileName || '-' }}</div></div>
        </div>
        <el-empty v-else description="暂无维修完成照片"/>
      </el-card></el-col>
    </el-row>


    <el-card style="margin-top:12px"><template #header>最近维修记录</template>
      <el-table :data="detail.recentRepairRecords || []">
        <el-table-column prop="repairOrderNo" label="工单号" width="180"/>
        <el-table-column prop="repairSequence" label="报修序号" width="100"/>
        <el-table-column prop="maintenanceSequence" label="维修序号" width="100"/>
        <el-table-column prop="faultReason" label="故障原因" min-width="160"/>
        <el-table-column prop="fixMeasure" label="维修措施" min-width="180"/>
        <el-table-column prop="laborHours" label="工期(h)" width="90"/>
        <el-table-column prop="maintainerName" label="维修人员" width="110"/>
        <el-table-column prop="finishTime" label="完成时间" width="180"/>
      </el-table>
    </el-card>

    <el-card style="margin-top:12px"><template #header>维修时间线</template>
      <el-empty v-if="!(detail.repairTimeline || []).length" description="暂无时间线记录"/>
      <el-timeline v-else>
        <el-timeline-item v-for="(t,i) in detail.repairTimeline || []" :key="i" :timestamp="t.time">
          工单#{{ t.orderId }}：{{ t.fromStatus || '开始' }} → {{ t.toStatus || '-' }}（{{ t.action || '-' }}）{{ t.remark ? ' | '+t.remark : '' }}
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card style="margin-top:12px"><template #header>关联工单列表</template>
      <el-table :data="detail.relatedOrders || []">
        <el-table-column prop="orderNo" label="工单号" width="180"/>
        <el-table-column prop="title" label="标题" min-width="180"/>
        <el-table-column prop="priority" label="优先级" width="90"/>
        <el-table-column prop="status" label="状态" width="140"/>
        <el-table-column prop="reportTime" label="报修时间" width="180"/>
        <el-table-column prop="finishTime" label="完成时间" width="180"/>
      </el-table>
    </el-card>

    <el-dialog v-model="uploadDialog" title="上传维修留痕图片" width="520px">
      <el-form :model="uploadForm" label-width="90px">
        <el-form-item label="图片分类"><el-tag>{{ categoryText(uploadForm.category) }}</el-tag></el-form-item>
        <el-form-item label="文件名"><el-input v-model="uploadForm.fileName"/></el-form-item>
        <el-form-item label="图片URL"><el-input v-model="uploadForm.fileUrl"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" :rows="2" v-model="uploadForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="uploadDialog=false">取消</el-button><el-button type="primary" @click="submitUpload">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage, postApi } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const d = computed(() => detail.value.device || {})
const uploadDialog = ref(false)
const uploadForm = reactive({ category: 'DEVICE_PHOTO', fileName: '', fileUrl: '', remark: '' })

const statusType = (s) => {
  if (s === '正常') return 'success'
  if (s === '维修中') return 'warning'
  if (s === '报废') return 'danger'
  return 'info'
}

const categoryText = (c) => ({ DEVICE_PHOTO: '设备照片', FAULT_SCENE: '故障现场照片', REPAIR_RESULT: '维修完成照片' }[c] || c)
const fileUrls = (arr = []) => arr.map(i => i.fileUrl).filter(Boolean)

const loadDetail = async () => { detail.value = await getPage(`/devices/${route.params.id}/detail`) }

const openUpload = (category) => {
  uploadForm.category = category
  uploadForm.fileName = ''
  uploadForm.fileUrl = ''
  uploadForm.remark = ''
  uploadDialog.value = true
}

const submitUpload = async () => {
  if (!uploadForm.fileName) return ElMessage.warning('请填写文件名')
  if (!uploadForm.fileUrl) return ElMessage.warning('请填写图片URL')
  await postApi(`/devices/${route.params.id}/attachments`, {
    fileName: uploadForm.fileName,
    fileUrl: uploadForm.fileUrl,
    fileType: 'image',
    category: uploadForm.category,
    remark: uploadForm.remark
  })
  ElMessage.success('上传记录成功')
  uploadDialog.value = false
  await loadDetail()
}

onMounted(loadDetail)
</script>

<style scoped>
.header-action { display:flex; justify-content:space-between; align-items:center; gap:8px; }
.photo-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; margin-bottom:8px; }
.photo-item { border:1px solid #ebeef5; border-radius:6px; padding:6px; }
.photo-title { margin-top:6px; font-size:12px; color:#303133; word-break:break-all; }
</style>
