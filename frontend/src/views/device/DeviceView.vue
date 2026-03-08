<template>
  <div>
    <h2>设备管理</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="设备名称"><el-input v-model="query.deviceName"/></el-form-item>
      <el-form-item label="设备类型"><el-select v-model="query.deviceType" clearable><el-option label="交换机" value="交换机"/><el-option label="路由器" value="路由器"/><el-option label="防火墙" value="防火墙"/><el-option label="无线AP" value="无线AP"/><el-option label="服务器" value="服务器"/></el-select></el-form-item>
      <el-form-item label="运行状态"><el-select v-model="query.status" clearable><el-option label="正常" value="正常"/><el-option label="故障" value="故障"/><el-option label="维修中" value="维修中"/><el-option label="停用" value="停用"/></el-select></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>
    <el-button type="primary" v-if="isAdmin" @click="openAdd">新增设备</el-button>
    <el-table :data="list" style="margin-top:12px"><el-table-column prop="deviceCode" label="设备编号"/><el-table-column prop="deviceName" label="设备名称"/><el-table-column prop="deviceType" label="设备类型"/><el-table-column prop="brandModel" label="品牌型号"/><el-table-column prop="ipAddress" label="IP地址"/><el-table-column prop="location" label="安装位置"/><el-table-column prop="status" label="运行状态"/><el-table-column label="操作" width="260"><template #default="s"><el-button link @click="detail(s.row)">查看</el-button><template v-if="isAdmin"><el-button link @click="edit(s.row)">编辑</el-button><el-button link type="danger" @click="remove(s.row)">删除</el-button></template></template></el-table-column></el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="dialog" :title="mode==='view'?'设备详情':(form.id?'编辑设备':'新增设备')" width="700px">
      <el-form :model="form" label-width="100px" :disabled="mode==='view'">
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="设备编号"><el-input v-model="form.deviceCode"/></el-form-item></el-col><el-col :span="12"><el-form-item label="设备名称"><el-input v-model="form.deviceName"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="设备类型"><el-input v-model="form.deviceType"/></el-form-item></el-col><el-col :span="12"><el-form-item label="品牌型号"><el-input v-model="form.brandModel"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="IP地址"><el-input v-model="form.ipAddress"/></el-form-item></el-col><el-col :span="12"><el-form-item label="MAC地址"><el-input v-model="form.macAddress"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="安装位置"><el-input v-model="form.location"/></el-form-item></el-col><el-col :span="12"><el-form-item label="运行状态"><el-input v-model="form.status"/></el-form-item></el-col></el-row>
        <el-form-item label="采购日期"><el-date-picker v-model="form.purchaseDate" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">关闭</el-button><el-button v-if="mode!=='view'" type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { getPage, postApi, putApi, delApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
const isAdmin = computed(()=>useUserStore().userInfo.role==='admin')
const query=reactive({deviceName:'',deviceType:'',status:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const dialog=ref(false),mode=ref('edit'),form=reactive({})
const load=async()=>{const r=await getPage('/devices/page',{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{query.deviceName='';query.deviceType='';query.status='';load()}
const openAdd=()=>{mode.value='edit';Object.assign(form,{id:null,deviceCode:'',deviceName:'',deviceType:'交换机',brandModel:'',ipAddress:'',macAddress:'',location:'',purchaseDate:'',status:'正常',remark:''});dialog.value=true}
const edit=(row)=>{mode.value='edit';Object.assign(form,row);dialog.value=true}
const detail=(row)=>{mode.value='view';Object.assign(form,row);dialog.value=true}
const save=async()=>{if(form.id) await putApi(`/devices/${form.id}`,form); else await postApi('/devices',form);ElMessage.success('保存成功');dialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该设备吗？','删除确认');await delApi(`/devices/${row.id}`);ElMessage.success('删除成功');load()}
onMounted(load)
</script>
