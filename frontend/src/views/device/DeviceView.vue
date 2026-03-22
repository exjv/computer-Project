<template>
  <div>
    <h2>设备管理档案</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="设备编号"><el-input v-model="query.deviceCode"/></el-form-item>
      <el-form-item label="设备名称"><el-input v-model="query.deviceName"/></el-form-item>
      <el-form-item label="设备类型"><el-input v-model="query.deviceType"/></el-form-item>
      <el-form-item label="所属校区"><el-input v-model="query.campus"/></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable>
          <el-option label="正常" value="正常"/><el-option label="维修中" value="维修中"/>
          <el-option label="停用" value="停用"/><el-option label="报废" value="报废"/>
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>

    <el-button type="primary" v-if="isAdmin" @click="openAdd">新增设备档案</el-button>
    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="deviceCode" label="设备编号" width="140"/>
      <el-table-column prop="deviceName" label="设备名称" width="150"/>
      <el-table-column prop="deviceType" label="设备类型" width="120"/>
      <el-table-column prop="campus" label="校区" width="120"/>
      <el-table-column prop="buildingLocation" label="楼宇/机房/办公室" min-width="180"/>
      <el-table-column prop="ownerName" label="责任人" width="120"/>
      <el-table-column prop="status" label="状态" width="90"/>
      <el-table-column label="报修/维修" width="120">
        <template #default="s">{{ s.row.totalRepairRequests || 0 }}/{{ s.row.totalRepairCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="审批" width="100"><template #default="s">{{ s.row.repairApprovalRequired===1?'需审批':'直达分配' }}</template></el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="s">
          <el-button link @click="detail(s.row)">详情页</el-button>
          <template v-if="isAdmin"><el-button link @click="edit(s.row)">编辑</el-button><el-button link type="danger" @click="remove(s.row)">删除</el-button></template>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="dialog" :title="mode==='view'?'设备完整档案':(form.id?'编辑设备档案':'新增设备档案')" width="980px">
      <template v-if="mode==='view'">
        <el-row :gutter="12" style="margin-bottom:12px">
          <el-col :span="6"><el-card shadow="never"><div>累计报修</div><h3>{{ profile.totalOrders || form.totalRepairRequests || 0 }}</h3></el-card></el-col>
          <el-col :span="6"><el-card shadow="never"><div>累计维修</div><h3>{{ profile.totalRepairs || form.totalRepairCount || 0 }}</h3></el-card></el-col>
          <el-col :span="6"><el-card shadow="never"><div>最近故障时间</div><h4>{{ profile.recentFaultTime || form.lastFaultTime || '-' }}</h4></el-card></el-col>
          <el-col :span="6"><el-card shadow="never"><div>审核策略</div><el-tag :type="form.repairApprovalRequired===1?'warning':'success'">{{ form.repairApprovalRequired===1?'需管理员审核':'无需审核' }}</el-tag></el-card></el-col>
        </el-row>
      </template>

      <el-form :model="form" label-width="130px" :disabled="mode==='view'">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="设备编号"><el-input v-model="form.deviceCode"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备名称"><el-input v-model="form.deviceName"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="设备类型"><el-input v-model="form.deviceType"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="品牌"><el-input v-model="form.brand"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="型号"><el-input v-model="form.model"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="序列号"><el-input v-model="form.serialNumber"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="所属校区"><el-input v-model="form.campus"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="楼宇/机房/办公室"><el-input v-model="form.buildingLocation"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="购买时间"><el-date-picker v-model="form.purchaseDate" value-format="YYYY-MM-DD"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="启用时间"><el-date-picker v-model="form.enableDate" value-format="YYYY-MM-DD"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="保修截止时间"><el-date-picker v-model="form.warrantyExpiryDate" value-format="YYYY-MM-DD"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="责任人"><el-input v-model="form.ownerName"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="管理部门"><el-input v-model="form.manageDepartment"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="当前状态"><el-select v-model="form.status"><el-option label="正常" value="正常"/><el-option label="维修中" value="维修中"/><el-option label="停用" value="停用"/><el-option label="报废" value="报废"/></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="审批策略"><el-select v-model="form.repairApprovalRequired"><el-option label="无需审核" :value="0"/><el-option label="需管理员审核" :value="1"/></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最近故障时间"><el-input :model-value="form.lastFaultTime || '-'" disabled/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="累计报修次数"><el-input :model-value="form.totalRepairRequests || 0" disabled/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="累计维修次数"><el-input :model-value="form.totalRepairCount || 0" disabled/></el-form-item></el-col>
        </el-row>
        <el-form-item label="故障原因统计"><el-input type="textarea" :rows="2" :model-value="form.faultReasonStats || '-'" disabled/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
      </el-form>

      <template v-if="mode==='view'">
        <el-divider>最近工单</el-divider>
        <el-timeline>
          <el-timeline-item v-for="o in profile.recentOrders || []" :key="o.id" :timestamp="o.reportTime">
            {{ o.orderNo }} | {{ o.status }} | {{ o.title }}
          </el-timeline-item>
        </el-timeline>
      </template>
      <template #footer><el-button @click="dialog=false">关闭</el-button><el-button v-if="mode!=='view'" type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getPage, postApi, putApi, delApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const isAdmin = computed(()=>useUserStore().userInfo.role==='admin')
const router = useRouter()
const query=reactive({deviceCode:'',deviceName:'',deviceType:'',campus:'',status:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const dialog=ref(false),mode=ref('edit'),form=reactive({})
const profile = ref({})

const blankForm = () => ({ id:null, deviceCode:'', deviceName:'', deviceType:'', brand:'', model:'', serialNumber:'', campus:'', buildingLocation:'', purchaseDate:'', enableDate:'', warrantyExpiryDate:'', ownerName:'', manageDepartment:'', status:'正常', totalRepairRequests:0, totalRepairCount:0, lastFaultTime:'', faultReasonStats:'', repairApprovalRequired:0, remark:'' })

const load=async()=>{const r=await getPage('/devices/page',{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{Object.assign(query,{deviceCode:'',deviceName:'',deviceType:'',campus:'',status:''});load()}
const openAdd=()=>{mode.value='edit';Object.assign(form,blankForm());dialog.value=true}
const edit=(row)=>{mode.value='edit';Object.assign(form, blankForm(), row);dialog.value=true}
const detail=(row)=>{router.push(`/devices/${row.id}`)}
const save=async()=>{if(form.id) await putApi(`/devices/${form.id}`,form); else await postApi('/devices',form);ElMessage.success('保存成功');dialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该设备吗？','删除确认');await delApi(`/devices/${row.id}`);ElMessage.success('删除成功');load()}

onMounted(load)
</script>
