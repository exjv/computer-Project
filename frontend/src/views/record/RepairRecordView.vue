<template>
  <div>
    <h2>维修记录管理</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单ID"><el-input v-model="query.repairOrderId"/></el-form-item>
      <el-form-item label="设备ID"><el-input v-model="query.deviceId"/></el-form-item>
      <el-form-item label="维修人ID"><el-input v-model="query.maintainerId"/></el-form-item>
      <el-form-item label="是否解决"><el-select v-model="query.isResolved" clearable><el-option label="是" :value="1"/><el-option label="否" :value="0"/></el-select></el-form-item>
      <el-form-item label="维修时间区间"><el-date-picker v-model="query.timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
      <el-button v-if="isAdmin" type="success" @click="exportRecords">导出设备维修记录Excel</el-button>
    </el-form>

    <el-card style="margin-bottom:12px">
      <div><b>设备频繁报修Top</b>：{{ (deviceStats.frequentRepairDevices||[]).slice(0,3).map(v=>`${v.deviceCode||('设备'+v.deviceId)}:${v.repairCount||0}次`).join('；') || '-' }}</div>
      <div style="margin-top:6px"><b>平均维修时长最长Top</b>：{{ (deviceStats.longestAvgRepairDevices||[]).slice(0,3).map(v=>`${v.deviceCode||('设备'+v.deviceId)}:${Number(v.avgRepairHours||0).toFixed(1)}h`).join('；') || '-' }}</div>
      <div style="margin-top:6px"><b>接近淘汰/更新周期</b>：{{ (deviceStats.nearRetireDevices||[]).slice(0,5).map(v=>v.deviceCode||('设备'+v.deviceId)).join('、') || '-' }}</div>
    </el-card>

    <el-button type="primary" @click="openAdd">新增维修记录</el-button>
    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="repairOrderNo" label="工单编号" width="180"/>
      <el-table-column prop="deviceCode" label="设备编号" width="140"/>
      <el-table-column prop="repairSequence" label="第几次报修" width="110"/>
      <el-table-column prop="maintenanceSequence" label="第几次维修" width="110"/>
      <el-table-column prop="maintainerName" label="维修人员" width="100"/>
      <el-table-column prop="faultReason" label="故障原因" min-width="140"/>
      <el-table-column prop="laborHours" label="实际工期(h)" width="100"/>
      <el-table-column prop="userSatisfaction" label="满意度" width="90"/>
      <el-table-column prop="repairTime" label="维修时间" width="180"/>
      <el-table-column label="操作" width="280">
        <template #default="s"><el-button link @click="detail(s.row)">查看</el-button><el-button link @click="edit(s.row)">编辑</el-button><el-button link type="danger" v-if="isAdmin" @click="remove(s.row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="dialog" :title="form.id?'编辑维修记录':'新增维修记录'" width="920px">
      <el-form :model="form" label-width="120px">
        <el-row :gutter="12"><el-col :span="8"><el-form-item label="工单ID"><el-input v-model="form.repairOrderId"/></el-form-item></el-col><el-col :span="8"><el-form-item label="设备ID"><el-input v-model="form.deviceId"/></el-form-item></el-col><el-col :span="8"><el-form-item label="维修人ID"><el-input v-model="form.maintainerId"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="报修时间"><el-date-picker v-model="form.reportTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item></el-col><el-col :span="12"><el-form-item label="接单时间"><el-date-picker v-model="form.acceptTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="开始维修时间"><el-date-picker v-model="form.startRepairTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item></el-col><el-col :span="12"><el-form-item label="完成时间"><el-date-picker v-model="form.finishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item></el-col></el-row>
        <el-form-item label="故障原因"><el-input v-model="form.faultReason"/></el-form-item>
        <el-form-item label="维修处理措施"><el-input type="textarea" v-model="form.fixMeasure"/></el-form-item>
        <el-form-item label="处理过程"><el-input type="textarea" v-model="form.processDetail"/></el-form-item>
        <el-form-item label="处理结果"><el-input type="textarea" v-model="form.resultDetail"/></el-form-item>
        <el-row :gutter="12"><el-col :span="8"><el-form-item label="是否更换配件"><el-select v-model="form.usedParts"><el-option label="是" :value="1"/><el-option label="否" :value="0"/></el-select></el-form-item></el-col><el-col :span="16"><el-form-item label="配件信息"><el-input v-model="form.usedPartsDesc"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="8"><el-form-item label="是否延期"><el-select v-model="form.delayApplied"><el-option label="是" :value="1"/><el-option label="否" :value="0"/></el-select></el-form-item></el-col><el-col :span="16"><el-form-item label="延期原因"><el-input v-model="form.delayReason"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="8"><el-form-item label="实际工期"><el-input-number v-model="form.laborHours" :min="0"/></el-form-item></el-col><el-col :span="8"><el-form-item label="用户确认结果"><el-input v-model="form.userConfirmResult"/></el-form-item></el-col><el-col :span="8"><el-form-item label="用户满意度"><el-input-number v-model="form.userSatisfaction" :min="1" :max="5"/></el-form-item></el-col></el-row>
        <el-form-item label="维修照片URL集合"><el-input type="textarea" v-model="form.photoUrls"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
        <el-form-item label="是否解决"><el-select v-model="form.isResolved"><el-option label="是" :value="1"/><el-option label="否" :value="0"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="维修记录详情" width="760px"><pre>{{ JSON.stringify(current, null, 2) }}</pre></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { exportRepairRecordReportApi, getPage, postApi, putApi, delApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const isAdmin=computed(()=>useUserStore().userInfo.role==='admin')
const query=reactive({repairOrderId:'',deviceId:'',maintainerId:'',isResolved:'',timeRange:[]}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0),dialog=ref(false),detailDialog=ref(false),current=ref({}),form=reactive({})
const deviceStats = ref({})

const load=async()=>{const r=await getPage('/repair-records/page',{...query,...page});list.value=r.records;total.value=r.total;deviceStats.value=await getPage('/repair-records/device-statistics')}
const reset=()=>{Object.assign(query,{repairOrderId:'',deviceId:'',maintainerId:'',isResolved:'',timeRange:[]});load()}
const openAdd=()=>{Object.assign(form,{id:null,repairOrderId:'',deviceId:'',maintainerId:'',faultReason:'',processDetail:'',resultDetail:'',isResolved:1,usedParts:0,usedPartsDesc:'',delayApplied:0,delayReason:'',laborHours:0,fixMeasure:'',userConfirmResult:'',userSatisfaction:5,photoUrls:'',remark:'',reportTime:'',acceptTime:'',startRepairTime:'',finishTime:''});dialog.value=true}
const edit=(row)=>{Object.assign(form,row);dialog.value=true}
const detail=(row)=>{current.value=row;detailDialog.value=true}
const save=async()=>{if(form.id) await putApi(`/repair-records/${form.id}`,form); else await postApi('/repair-records',form);ElMessage.success('保存成功');dialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该维修记录吗？','删除确认');await delApi(`/repair-records/${row.id}`);ElMessage.success('删除成功');load()}
const exportRecords = async () => {
  const [startTime, endTime] = query.timeRange || []
  await exportRepairRecordReportApi({
    deviceId: query.deviceId || undefined,
    startTime,
    endTime
  })
  ElMessage.success('设备维修记录报表导出成功')
}

onMounted(load)
</script>
