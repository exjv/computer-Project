<template>
  <div>
    <h2>我的待处理工单页</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单编号"><el-input v-model="query.orderNo"/></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable><el-option v-for="s in statuses" :key="s" :label="s" :value="s"/></el-select></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>

    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="orderNo" label="工单编号" width="180"/>
      <el-table-column prop="title" label="故障标题"/>
      <el-table-column prop="status" label="状态" width="150"/>
      <el-table-column prop="progress" label="进度" width="100"><template #default="s">{{ s.row.progress || 0 }}%</template></el-table-column>
      <el-table-column label="操作" width="520">
        <template #default="s">
          <el-button v-if="s.row.status==='待接单'" link type="primary" @click="accept(s.row)">接单</el-button>
          <el-button v-if="s.row.status==='待接单'" link type="danger" @click="openReject(s.row)">拒单</el-button>
          <el-button v-if="s.row.status==='维修人员已接单'" link type="warning" @click="start(s.row)">开始维修</el-button>
          <el-button v-if="canMaintain(s.row.status)" link @click="openProgress(s.row)">更新进度</el-button>
          <el-button v-if="canMaintain(s.row.status)" link @click="openDelay(s.row)">申请延期</el-button>
          <el-button v-if="canMaintain(s.row.status)" link @click="openParts(s.row)">申请配件</el-button>
          <el-button v-if="canMaintain(s.row.status)" link @click="openPhoto(s.row)">上传现场照片</el-button>
          <el-button v-if="canFinish(s.row.status)" link type="success" @click="openFinish(s.row)">提交完工</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="rejectDialog" title="拒单（需填写原因）"><el-input type="textarea" v-model="rejectForm.reason"/><template #footer><el-button @click="rejectDialog=false">取消</el-button><el-button type="primary" @click="submitReject">提交</el-button></template></el-dialog>

    <el-dialog v-model="progressDialog" title="维修中操作面板 / 进度更新">
      <el-form :model="progressForm" label-width="120px">
        <el-form-item label="进度百分比"><el-input-number v-model="progressForm.progress" :min="0" :max="100"/></el-form-item>
        <el-form-item label="预计完成时间"><el-date-picker v-model="progressForm.expectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        <el-form-item label="处理说明"><el-input type="textarea" v-model="progressForm.handleDescription"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="progressForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="progressDialog=false">取消</el-button><el-button type="primary" @click="submitProgress">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="delayDialog" title="延期申请表单">
      <el-form :model="delayForm" label-width="120px">
        <el-form-item label="延期至"><el-date-picker v-model="delayForm.delayedExpectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        <el-form-item label="申请原因"><el-input type="textarea" v-model="delayForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="delayDialog=false">取消</el-button><el-button type="primary" @click="submitDelay">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="partsDialog" title="配件采购申请表单">
      <el-form :model="partsForm" label-width="120px">
        <el-form-item label="配件说明"><el-input type="textarea" v-model="partsForm.partsDescription"/></el-form-item>
        <el-form-item label="申请原因"><el-input type="textarea" v-model="partsForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="partsDialog=false">取消</el-button><el-button type="primary" @click="submitParts">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="photoDialog" title="图片上传与预览" width="680px">
      <el-form :model="photoForm" label-width="100px">
        <el-form-item label="图片URL"><el-input v-model="photoForm.fileUrl"/></el-form-item>
        <el-form-item label="处理说明"><el-input type="textarea" v-model="photoForm.handleDescription"/></el-form-item>
      </el-form>
      <div style="display:flex;gap:10px;align-items:center">
        <el-image v-if="photoForm.fileUrl" :src="photoForm.fileUrl" style="width:120px;height:120px" fit="cover"/>
      </div>
      <template #footer><el-button @click="photoDialog=false">取消</el-button><el-button type="primary" @click="submitPhoto">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="finishDialog" title="完工提交表单">
      <el-form :model="finishForm" label-width="120px">
        <el-form-item label="处理说明"><el-input type="textarea" v-model="finishForm.handleDescription"/></el-form-item>
        <el-form-item label="完工备注"><el-input type="textarea" v-model="finishForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="finishDialog=false">取消</el-button><el-button type="primary" @click="submitFinish">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPage, putApi } from '../../api'
import { ElMessage } from 'element-plus'

const statuses = ['待接单', '维修人员已接单', '维修中', '申请延期中', '延期已批准', '待采购/待配件']
const query = reactive({ orderNo: '', status: '' })
const list = ref([])
const currentId = ref(null)

const rejectDialog = ref(false)
const progressDialog = ref(false)
const delayDialog = ref(false)
const partsDialog = ref(false)
const photoDialog = ref(false)
const finishDialog = ref(false)

const rejectForm = reactive({ reason: '' })
const progressForm = reactive({ progress: 60, expectedFinishTime: '', handleDescription: '', remark: '', scenePhotoUrls: [] })
const delayForm = reactive({ delayedExpectedFinishTime: '', remark: '' })
const partsForm = reactive({ partsDescription: '', remark: '' })
const photoForm = reactive({ fileUrl: '', handleDescription: '' })
const finishForm = reactive({ handleDescription: '', remark: '' })

const canMaintain = s => ['维修中', '延期已批准', '待采购/待配件'].includes(s)
const canFinish = s => ['维修中', '延期已批准', '待采购/待配件'].includes(s)

const load = async () => {
  const r = await getPage('/repair-orders/my', { ...query, current: 1, size: 100 })
  list.value = (r.records || []).filter(v => statuses.includes(v.status))
}

const accept = async row => { await putApi(`/repair-orders/${row.id}/maintainer/accept`, { remark: '维修人员接单' }); ElMessage.success('接单成功'); await load() }
const start = async row => { await putApi(`/repair-orders/${row.id}/maintainer/start`, { remark: '开始维修' }); ElMessage.success('已开始维修'); await load() }

const openReject = row => { currentId.value = row.id; rejectForm.reason = ''; rejectDialog.value = true }
const submitReject = async () => { await putApi(`/repair-orders/${currentId.value}/maintainer/reject`, { reason: rejectForm.reason }); ElMessage.success('拒单成功'); rejectDialog.value = false; await load() }

const openProgress = row => { currentId.value = row.id; progressForm.progress = row.progress || 60; progressForm.expectedFinishTime = row.expectedFinishTime || ''; progressForm.handleDescription = row.handleDescription || ''; progressForm.remark = ''; progressDialog.value = true }
const submitProgress = async () => { await putApi(`/repair-orders/${currentId.value}/maintainer/progress`, progressForm); ElMessage.success('进度更新成功'); progressDialog.value = false; await load() }

const openDelay = row => { currentId.value = row.id; delayForm.delayedExpectedFinishTime = ''; delayForm.remark = ''; delayDialog.value = true }
const submitDelay = async () => { await putApi(`/repair-orders/${currentId.value}/maintainer/delay-apply`, delayForm); ElMessage.success('延期申请已提交'); delayDialog.value = false; await load() }

const openParts = row => { currentId.value = row.id; partsForm.partsDescription = ''; partsForm.remark = ''; partsDialog.value = true }
const submitParts = async () => { await putApi(`/repair-orders/${currentId.value}/maintainer/parts-apply`, partsForm); ElMessage.success('配件采购申请已提交'); partsDialog.value = false; await load() }

const openPhoto = row => { currentId.value = row.id; photoForm.fileUrl = ''; photoForm.handleDescription = ''; photoDialog.value = true }
const submitPhoto = async () => {
  const payload = {
    progress: progressForm.progress || 60,
    handleDescription: photoForm.handleDescription,
    scenePhotoUrls: [photoForm.fileUrl],
    remark: '上传现场照片'
  }
  await putApi(`/repair-orders/${currentId.value}/maintainer/progress`, payload)
  ElMessage.success('图片上传并记录成功')
  photoDialog.value = false
  await load()
}

const openFinish = row => { currentId.value = row.id; finishForm.handleDescription = ''; finishForm.remark = ''; finishDialog.value = true }
const submitFinish = async () => { await putApi(`/repair-orders/${currentId.value}/maintainer/finish`, finishForm); ElMessage.success('完工已提交，进入待验收'); finishDialog.value = false; await load() }

onMounted(load)
</script>
