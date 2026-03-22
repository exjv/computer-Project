<template>
  <div>
    <h2>设备档案管理</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="设备编号"><el-input v-model="query.deviceCode"/></el-form-item>
      <el-form-item label="设备名称"><el-input v-model="query.deviceName"/></el-form-item>
      <el-form-item label="设备类型"><el-input v-model="query.deviceType"/></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable><el-option v-for="s in statusOptions" :key="s" :label="s" :value="s"/></el-select></el-form-item>
      <el-form-item label="位置"><el-input v-model="query.location"/></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>

    <div style="margin-bottom:10px">
      <el-button type="primary" v-if="isAdmin" @click="openAdd">新增设备档案</el-button>
    </div>

    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="deviceCode" label="设备编号" width="140"/>
      <el-table-column prop="deviceName" label="设备名称" width="150"/>
      <el-table-column prop="deviceType" label="设备类型" width="110"/>
      <el-table-column prop="brand" label="品牌" width="100"/>
      <el-table-column prop="model" label="型号" width="120"/>
      <el-table-column prop="campus" label="校区" width="100"/>
      <el-table-column prop="location" label="楼宇/机房/办公室" min-width="180"/>
      <el-table-column prop="ownerName" label="责任人" width="100"/>
      <el-table-column prop="managementDept" label="管理部门" width="120"/>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="s"><el-tag :type="statusTag(s.row.status)">{{ s.row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="s">
          <el-button link @click="detail(s.row)">档案详情</el-button>
          <el-button link v-if="isAdmin" @click="edit(s.row)">编辑</el-button>
          <el-button link v-if="isAdmin" @click="openStatus(s.row)">状态维护</el-button>
          <el-button link v-if="isAdmin" type="danger" @click="remove(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="dialog" :title="mode==='view'?'设备完整档案':(form.id?'编辑设备档案':'新增设备档案')" width="980px">
      <el-row v-if="mode==='view'" :gutter="12" style="margin-bottom: 12px">
        <el-col :span="6"><el-card shadow="never"><div>累计报修次数</div><h3>{{ profile.totalOrders || 0 }}</h3></el-card></el-col>
        <el-col :span="6"><el-card shadow="never"><div>累计维修次数</div><h3>{{ profile.totalRepairs || 0 }}</h3></el-card></el-col>
        <el-col :span="6"><el-card shadow="never"><div>最近故障时间</div><div>{{ profile.recentFaultTime || '-' }}</div></el-card></el-col>
        <el-col :span="6"><el-card shadow="never"><div>平均处理时长(h)</div><h3>{{ profile.avgRepairHours || 0 }}</h3></el-card></el-col>
      </el-row>
      <el-form :model="form" :disabled="mode==='view'" label-width="130px">
        <el-divider>基础信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="设备编号"><el-input v-model="form.deviceCode"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备名称"><el-input v-model="form.deviceName"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="设备类型"><el-input v-model="form.deviceType"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备类型名称"><el-input v-model="form.deviceTypeName"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="品牌"><el-input v-model="form.brand"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="型号"><el-input v-model="form.model"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="序列号"><el-input v-model="form.serialNo"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="IP地址"><el-input v-model="form.ipAddress"/></el-form-item></el-col>
        </el-row>

        <el-divider>位置与归属</el-divider>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="所属校区"><el-input v-model="form.campus"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="所属楼宇"><el-input v-model="form.building"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="机房/办公室"><el-input v-model="form.machineRoom"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="完整位置"><el-input v-model="form.location"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="管理部门"><el-input v-model="form.managementDept"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="责任人"><el-input v-model="form.ownerName"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="责任人工号"><el-input v-model="form.ownerEmployeeNo"/></el-form-item></el-col>
        </el-row>

        <el-divider>生命周期与状态</el-divider>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="购买时间"><el-date-picker v-model="form.purchaseDate" value-format="YYYY-MM-DD"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="启用时间"><el-date-picker v-model="form.enableDate" value-format="YYYY-MM-DD"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="保修截止"><el-date-picker v-model="form.warrantyExpireDate" value-format="YYYY-MM-DD"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="当前状态"><el-select v-model="form.status"><el-option v-for="s in statusOptions" :key="s" :label="s" :value="s"/></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最近故障时间"><el-date-picker v-model="form.lastFaultTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="累计报修次数"><el-input-number v-model="form.totalRepairOrderCount" :min="0"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="累计维修次数"><el-input-number v-model="form.totalRepairCount" :min="0"/></el-form-item></el-col>
          <el-col :span="16"><el-form-item label="故障原因统计JSON"><el-input type="textarea" v-model="form.faultReasonStats"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
      </el-form>

      <template v-if="mode==='view'">
        <el-divider>关联工单（最近10条）</el-divider>
        <el-timeline>
          <el-timeline-item v-for="o in profile.recentOrders || []" :key="o.id" :timestamp="o.reportTime">
            {{ o.orderNo }} | {{ o.status }} | {{ o.title }}
          </el-timeline-item>
        </el-timeline>
        <el-divider>关联维修记录（最近10条）</el-divider>
        <el-timeline>
          <el-timeline-item v-for="r in profile.recentRecords || []" :key="r.id" :timestamp="r.startTime">
            {{ r.faultReason || '-' }} | {{ r.repairResult || '-' }}
          </el-timeline-item>
        </el-timeline>
      </template>

      <template #footer><el-button @click="dialog=false">关闭</el-button><el-button v-if="mode!=='view'" type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="statusDialog" title="设备状态维护" width="420px">
      <el-form :model="statusForm" label-width="90px">
        <el-form-item label="设备状态"><el-select v-model="statusForm.status"><el-option v-for="s in statusOptions" :key="s" :label="s" :value="s"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="statusDialog=false">取消</el-button><el-button type="primary" @click="saveStatus">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { getPage, postApi, putApi, delApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'

const statusOptions = ['正常', '维修中', '停用', '报废']
const router = useRouter()
const isAdmin = computed(()=>useUserStore().userInfo.role==='admin')
const query=reactive({deviceCode:'',deviceName:'',deviceType:'',status:'',location:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const dialog=ref(false),mode=ref('edit'),form=reactive({})
const profile = ref({})
const statusDialog=ref(false),statusForm=reactive({id:null,status:'正常'})

const load=async()=>{const r=await getPage('/devices/page',{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{Object.assign(query,{deviceCode:'',deviceName:'',deviceType:'',status:'',location:''});load()}
const openAdd=()=>{mode.value='edit';Object.assign(form,{id:null,deviceCode:'',deviceName:'',deviceType:'交换机',deviceTypeName:'',brand:'',model:'',serialNo:'',campus:'',building:'',machineRoom:'',office:'',location:'',purchaseDate:'',enableDate:'',warrantyExpireDate:'',ownerName:'',ownerEmployeeNo:'',managementDept:'',status:'正常',lastFaultTime:'',totalRepairOrderCount:0,totalRepairCount:0,faultReasonStats:'',remark:''});dialog.value=true}
const edit=(row)=>{mode.value='edit';Object.assign(form,row);dialog.value=true}
const detail=(row)=>router.push(`/devices/${row.id}/profile`)

const save=async()=>{
  const ok = await getPage('/devices/check-code', { deviceCode: form.deviceCode, excludeId: form.id || undefined })
  if (!ok) { ElMessage.error('设备编号已存在'); return }
  if(form.id) await putApi(`/devices/${form.id}`,form); else await postApi('/devices',form)
  ElMessage.success('保存成功');dialog.value=false;load()
}
const openStatus=(row)=>{statusForm.id=row.id;statusForm.status=row.status;statusDialog.value=true}
const saveStatus=async()=>{await putApi(`/devices/${statusForm.id}/status`,{status:statusForm.status});ElMessage.success('状态更新成功');statusDialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该设备吗？','删除确认');await delApi(`/devices/${row.id}`);ElMessage.success('删除成功');load()}
const statusTag=(status)=>status==='正常'?'success':(status==='维修中'?'warning':(status==='停用'?'info':'danger'))

onMounted(load)
</script>
