<template>
  <div>
    <h2>{{ pageTitle }}</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单状态"><el-select v-model="query.status" clearable><el-option label="待处理" value="待处理"/><el-option label="已分配" value="已分配"/><el-option label="处理中" value="处理中"/><el-option label="已完成" value="已完成"/><el-option label="已关闭" value="已关闭"/></el-select></el-form-item>
      <el-form-item v-if="isAdmin" label="标题"><el-input v-model="query.title"/></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>
    <el-button type="primary" v-if="isUser || isAdmin" @click="openAdd">提交报修</el-button>
    <el-table :data="list" style="margin-top:12px"><el-table-column prop="orderNo" label="工单编号"/><el-table-column prop="title" label="故障标题"/><el-table-column prop="priority" label="优先级"/><el-table-column prop="status" label="工单状态"/><el-table-column prop="assignMaintainerId" label="维修人员ID"/><el-table-column prop="reportTime" label="报修时间"/><el-table-column label="操作" width="320"><template #default="s"><el-button link @click="detail(s.row)">查看</el-button><el-button v-if="isAdmin && s.row.status==='待处理'" link @click="assign(s.row)">分配</el-button><el-button v-if="isAdmin || isMaintainer" link @click="changeStatus(s.row)">修改状态</el-button></template></el-table-column></el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="addDialog" title="提交报修">
      <el-form :model="form" label-width="100px"><el-form-item label="设备"><el-select v-model="form.deviceId"><el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id"/></el-select></el-form-item><el-form-item label="故障标题"><el-input v-model="form.title"/></el-form-item><el-form-item label="故障描述"><el-input type="textarea" v-model="form.description"/></el-form-item><el-form-item label="优先级"><el-select v-model="form.priority"><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item></el-form>
      <template #footer><el-button @click="addDialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="分配维修人员"><el-form :model="assignForm"><el-form-item label="维修人员"><el-select v-model="assignForm.assignMaintainerId"><el-option v-for="m in maintainers" :key="m.id" :label="m.realName+'('+m.username+')'" :value="m.id"/></el-select></el-form-item></el-form><template #footer><el-button @click="assignDialog=false">取消</el-button><el-button type="primary" @click="saveAssign">保存</el-button></template></el-dialog>
    <el-dialog v-model="statusDialog" title="修改工单状态"><el-form :model="statusForm"><el-form-item label="状态"><el-select v-model="statusForm.status"><el-option label="已分配" value="已分配"/><el-option label="处理中" value="处理中"/><el-option label="已完成" value="已完成"/><el-option label="已关闭" value="已关闭"/></el-select></el-form-item></el-form><template #footer><el-button @click="statusDialog=false">取消</el-button><el-button type="primary" @click="saveStatus">保存</el-button></template></el-dialog>
    <el-dialog v-model="detailDialog" title="工单详情"><pre>{{ current }}</pre></el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getPage, postApi, putApi } from '../../api'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
const role = computed(()=>useUserStore().userInfo.role)
const isAdmin = computed(()=>role.value==='admin')
const isUser = computed(()=>role.value==='user')
const isMaintainer = computed(()=>role.value==='maintainer')
const pageTitle = computed(()=>isAdmin.value?'工单管理':(isUser.value?'我的报修':'我的工单'))
const query=reactive({status:'',title:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const devices=ref([]),maintainers=ref([])
const addDialog=ref(false),assignDialog=ref(false),statusDialog=ref(false),detailDialog=ref(false)
const form=reactive({deviceId:'',title:'',description:'',priority:'中'})
const current=ref({}),assignForm=reactive({id:null,assignMaintainerId:null}),statusForm=reactive({id:null,status:'处理中'})
const apiPath = computed(()=>isAdmin.value?'/repair-orders/page':'/repair-orders/my')
const load = async()=>{const r=await getPage(apiPath.value,{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{query.status='';query.title='';load()}
const openAdd=()=>{addDialog.value=true}
const save=async()=>{await postApi('/repair-orders',form);ElMessage.success('提交成功');addDialog.value=false;load()}
const assign=(row)=>{assignForm.id=row.id;assignForm.assignMaintainerId=row.assignMaintainerId;assignDialog.value=true}
const saveAssign=async()=>{await putApi(`/repair-orders/${assignForm.id}/assign`,assignForm);ElMessage.success('分配成功');assignDialog.value=false;load()}
const changeStatus=(row)=>{statusForm.id=row.id;statusForm.status=row.status;statusDialog.value=true}
const saveStatus=async()=>{await putApi(`/repair-orders/${statusForm.id}/status`,statusForm);ElMessage.success('状态更新成功');statusDialog.value=false;load()}
const detail=(row)=>{current.value=row;detailDialog.value=true}
onMounted(async()=>{await load();const d=await getPage('/devices/page',{current:1,size:100});devices.value=d.records||[];if(isAdmin.value){maintainers.value=await getPage('/users/list-by-role',{role:'maintainer'})}})
</script>
