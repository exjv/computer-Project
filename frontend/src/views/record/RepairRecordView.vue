<template>
  <div>
    <h2>维修记录管理</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单ID"><el-input v-model="query.repairOrderId"/></el-form-item>
      <el-form-item label="设备ID"><el-input v-model="query.deviceId"/></el-form-item>
      <el-form-item label="维修人ID"><el-input v-model="query.maintainerId"/></el-form-item>
      <el-form-item label="是否解决"><el-select v-model="query.isResolved" clearable><el-option label="是" :value="1"/><el-option label="否" :value="0"/></el-select></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>
    <el-button type="primary" @click="openAdd">新增维修记录</el-button>
    <el-table :data="list" style="margin-top:12px"><el-table-column prop="repairOrderId" label="工单ID"/><el-table-column prop="faultReason" label="故障原因"/><el-table-column prop="processDetail" label="处理过程"/><el-table-column prop="resultDetail" label="处理结果"/><el-table-column prop="isResolved" label="是否解决"><template #default="s">{{s.row.isResolved===1?'是':'否'}}</template></el-table-column><el-table-column prop="repairTime" label="维修时间"/><el-table-column label="操作" width="280"><template #default="s"><el-button link @click="detail(s.row)">查看</el-button><el-button link @click="edit(s.row)">编辑</el-button><el-button link type="danger" v-if="isAdmin" @click="remove(s.row)">删除</el-button></template></el-table-column></el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>
    <el-dialog v-model="dialog" :title="form.id?'编辑维修记录':'新增维修记录'">
      <el-form :model="form" label-width="100px"><el-form-item label="工单ID"><el-input v-model="form.repairOrderId"/></el-form-item><el-form-item label="设备ID"><el-input v-model="form.deviceId"/></el-form-item><el-form-item label="维修人ID"><el-input v-model="form.maintainerId"/></el-form-item><el-form-item label="故障原因"><el-input v-model="form.faultReason"/></el-form-item><el-form-item label="处理过程"><el-input type="textarea" v-model="form.processDetail"/></el-form-item><el-form-item label="处理结果"><el-input type="textarea" v-model="form.resultDetail"/></el-form-item><el-form-item label="是否解决"><el-select v-model="form.isResolved"><el-option label="是" :value="1"/><el-option label="否" :value="0"/></el-select></el-form-item></el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="detailDialog" title="维修记录详情"><pre>{{current}}</pre></el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getPage, postApi, putApi, delApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
const isAdmin=computed(()=>useUserStore().userInfo.role==='admin')
const query=reactive({repairOrderId:'',deviceId:'',maintainerId:'',isResolved:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0),dialog=ref(false),detailDialog=ref(false),current=ref({}),form=reactive({})
const load=async()=>{const r=await getPage('/repair-records/page',{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{Object.assign(query,{repairOrderId:'',deviceId:'',maintainerId:'',isResolved:''});load()}
const openAdd=()=>{Object.assign(form,{id:null,repairOrderId:'',deviceId:'',maintainerId:'',faultReason:'',processDetail:'',resultDetail:'',isResolved:1});dialog.value=true}
const edit=(row)=>{Object.assign(form,row);dialog.value=true}
const detail=(row)=>{current.value=row;detailDialog.value=true}
const save=async()=>{if(form.id) await putApi(`/repair-records/${form.id}`,form); else await postApi('/repair-records',form);ElMessage.success('保存成功');dialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该维修记录吗？','删除确认');await delApi(`/repair-records/${row.id}`);ElMessage.success('删除成功');load()}
onMounted(load)
</script>
