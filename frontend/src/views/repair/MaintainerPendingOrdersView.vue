<template>
  <div>
    <h2>我的待处理工单</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单编号"><el-input v-model="query.orderNo"/></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable>
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s"/>
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>

    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="orderNo" label="工单编号" width="180"/>
      <el-table-column prop="title" label="故障标题"/>
      <el-table-column prop="status" label="状态" width="150"/>
      <el-table-column prop="progress" label="进度" width="100">
        <template #default="s">{{ s.row.progress || 0 }}%</template>
      </el-table-column>
      <el-table-column label="操作" width="420">
        <template #default="s">
          <el-button v-if="s.row.status==='待接单'" link type="primary" @click="action(s.row,'MAINTAINER_ACCEPT')">接单</el-button>
          <el-button v-if="s.row.status==='待接单'" link type="danger" @click="openReject(s.row)">拒单</el-button>
          <el-button v-if="s.row.status==='维修人员已接单'" link type="warning" @click="openStart(s.row)">开始维修</el-button>
          <el-button v-if="s.row.status==='维修中'" link @click="openProgress(s.row)">更新进度</el-button>
          <el-button v-if="s.row.status==='维修中'" link @click="openDelay(s.row)">申请延期</el-button>
          <el-button v-if="s.row.status==='维修中'" link @click="openParts(s.row)">申请配件</el-button>
          <el-button v-if="s.row.status==='维修中' || s.row.status==='待采购/待配件'" link @click="openPhoto(s.row)">上传现场照片</el-button>
          <el-button v-if="s.row.status==='维修中'" link type="success" @click="openFinish(s.row)">提交完工</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="rejectDialog" title="拒单原因">
      <el-input type="textarea" v-model="rejectForm.remark"/>
      <template #footer><el-button @click="rejectDialog=false">取消</el-button><el-button type="primary" @click="submitReject">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="startDialog" title="开始维修">
      <el-form :model="startForm" label-width="120px">
        <el-form-item label="预计完成时间"><el-date-picker v-model="startForm.expectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        <el-form-item label="处理说明"><el-input type="textarea" v-model="startForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="startDialog=false">取消</el-button><el-button type="primary" @click="submitStart">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="progressDialog" title="更新维修进度">
      <el-form :model="progressForm" label-width="120px">
        <el-form-item label="进度百分比"><el-input-number v-model="progressForm.progress" :min="0" :max="100"/></el-form-item>
        <el-form-item label="预计完成时间"><el-date-picker v-model="progressForm.expectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        <el-form-item label="处理说明"><el-input type="textarea" v-model="progressForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="progressDialog=false">取消</el-button><el-button type="primary" @click="submitProgress">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="delayDialog" title="延期申请">
      <el-form :model="delayForm" label-width="120px">
        <el-form-item label="延期至"><el-date-picker v-model="delayForm.delayedExpectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
        <el-form-item label="申请原因"><el-input type="textarea" v-model="delayForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="delayDialog=false">取消</el-button><el-button type="primary" @click="submitDelay">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="partsDialog" title="配件采购申请">
      <el-form :model="partsForm" label-width="120px">
        <el-form-item label="配件说明"><el-input type="textarea" v-model="partsForm.partsDescription"/></el-form-item>
        <el-form-item label="申请原因"><el-input type="textarea" v-model="partsForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="partsDialog=false">取消</el-button><el-button type="primary" @click="submitParts">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="photoDialog" title="上传现场照片（URL占位）" width="620px">
      <el-form :model="photoForm" label-width="100px">
        <el-form-item label="图片名称"><el-input v-model="photoForm.fileName"/></el-form-item>
        <el-form-item label="图片URL"><el-input v-model="photoForm.fileUrl"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="photoForm.remark"/></el-form-item>
      </el-form>
      <el-image v-if="photoForm.fileUrl" :src="photoForm.fileUrl" style="width:120px;height:120px" fit="cover"/>
      <template #footer><el-button @click="photoDialog=false">取消</el-button><el-button type="primary" @click="submitPhoto">上传</el-button></template>
    </el-dialog>

    <el-dialog v-model="finishDialog" title="提交完工结果">
      <el-form :model="finishForm" label-width="120px">
        <el-form-item label="处理说明"><el-input type="textarea" v-model="finishForm.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="finishDialog=false">取消</el-button><el-button type="primary" @click="submitFinish">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPage, postApi, putApi } from '../../api'
import { ElMessage } from 'element-plus'

const statuses = ['待接单', '维修人员已接单', '维修中', '申请延期中', '待采购/待配件']
const query = reactive({ orderNo: '', status: '' })
const list = ref([])
const currentId = ref(null)

const rejectDialog = ref(false)
const startDialog = ref(false)
const progressDialog = ref(false)
const delayDialog = ref(false)
const partsDialog = ref(false)
const photoDialog = ref(false)
const finishDialog = ref(false)

const rejectForm = reactive({ remark: '' })
const startForm = reactive({ expectedFinishTime: '', remark: '' })
const progressForm = reactive({ progress: 60, expectedFinishTime: '', remark: '' })
const delayForm = reactive({ delayedExpectedFinishTime: '', remark: '' })
const partsForm = reactive({ partsDescription: '', remark: '' })
const photoForm = reactive({ fileName: '', fileUrl: '', remark: '' })
const finishForm = reactive({ remark: '' })

const load = async () => {
  const r = await getPage('/repair-orders/my', { ...query, current: 1, size: 100 })
  list.value = (r.records || []).filter(v => statuses.includes(v.status))
}

const action = async (row, actionType, payload = {}) => {
  await putApi(`/repair-orders/${row.id}/action`, { action: actionType, ...payload })
  ElMessage.success('操作成功')
  await load()
}

const openReject = row => { currentId.value = row.id; rejectForm.remark = ''; rejectDialog.value = true }
const submitReject = async () => { await putApi(`/repair-orders/${currentId.value}/action`, { action: 'MAINTAINER_REJECT', remark: rejectForm.remark }); ElMessage.success('拒单成功'); rejectDialog.value = false; await load() }

const openStart = row => { currentId.value = row.id; startForm.expectedFinishTime = ''; startForm.remark = ''; startDialog.value = true }
const submitStart = async () => { await putApi(`/repair-orders/${currentId.value}/action`, { action: 'MAINTAINER_START', expectedFinishTime: startForm.expectedFinishTime, remark: startForm.remark }); ElMessage.success('已开始维修'); startDialog.value = false; await load() }

const openProgress = row => { currentId.value = row.id; progressForm.progress = row.progress || 60; progressForm.expectedFinishTime = row.expectedFinishTime || ''; progressForm.remark = ''; progressDialog.value = true }
const submitProgress = async () => { await putApi(`/repair-orders/${currentId.value}/action`, { action: 'MAINTAINER_PROGRESS', progress: progressForm.progress, expectedFinishTime: progressForm.expectedFinishTime, remark: progressForm.remark }); ElMessage.success('进度更新成功'); progressDialog.value = false; await load() }

const openDelay = row => { currentId.value = row.id; delayForm.delayedExpectedFinishTime = ''; delayForm.remark = ''; delayDialog.value = true }
const submitDelay = async () => { await putApi(`/repair-orders/${currentId.value}/action`, { action: 'MAINTAINER_DELAY_APPLY', delayedExpectedFinishTime: delayForm.delayedExpectedFinishTime, remark: delayForm.remark }); ElMessage.success('延期申请已提交'); delayDialog.value = false; await load() }

const openParts = row => { currentId.value = row.id; partsForm.partsDescription = ''; partsForm.remark = ''; partsDialog.value = true }
const submitParts = async () => { await putApi(`/repair-orders/${currentId.value}/action`, { action: 'MAINTAINER_PARTS_APPLY', partsDescription: partsForm.partsDescription, remark: partsForm.remark }); ElMessage.success('配件采购申请已提交'); partsDialog.value = false; await load() }

const openPhoto = row => { currentId.value = row.id; photoForm.fileName = ''; photoForm.fileUrl = ''; photoForm.remark = ''; photoDialog.value = true }
const submitPhoto = async () => {
  await postApi(`/repair-orders/${currentId.value}/attachments`, { fileName: photoForm.fileName, fileUrl: photoForm.fileUrl, fileType: 'image', remark: photoForm.remark })
  ElMessage.success('图片上传记录成功')
  photoDialog.value = false
}

const openFinish = row => { currentId.value = row.id; finishForm.remark = ''; finishDialog.value = true }
const submitFinish = async () => { await putApi(`/repair-orders/${currentId.value}/action`, { action: 'MAINTAINER_FINISH', remark: finishForm.remark }); ElMessage.success('完工已提交，待验收'); finishDialog.value = false; await load() }

onMounted(load)
</script>
