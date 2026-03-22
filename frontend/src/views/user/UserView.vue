<template>
  <div>
    <h2>用户管理</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工号"><el-input v-model="query.employeeNo" placeholder="请输入工号"/></el-form-item>
      <el-form-item label="用户名"><el-input v-model="query.username" placeholder="请输入用户名"/></el-form-item>
      <el-form-item label="角色"><el-select v-model="query.role" clearable><el-option label="管理员" value="admin"/><el-option label="普通用户" value="user"/><el-option label="维修人员" value="maintainer"/></el-select></el-form-item>
      <el-form-item label="手机号"><el-input v-model="query.phone"/></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>

    <el-button type="primary" @click="openAdd">新增用户</el-button>
    <el-button type="success" style="margin-left:8px" @click="batchDialog=true">批量新增</el-button>
    <el-upload :auto-upload="false" :show-file-list="false" :on-change="importExcel" style="display:inline-block;margin-left:8px">
      <el-button type="warning">Excel导入</el-button>
    </el-upload>

    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="id" label="用户ID" width="80"/>
      <el-table-column prop="employeeNo" label="工号" width="120"/>
      <el-table-column prop="realName" label="姓名" width="100"/>
      <el-table-column prop="role" label="角色" width="110"/>
      <el-table-column prop="phone" label="手机号" width="130"/>
      <el-table-column prop="email" label="邮箱" min-width="160"/>
      <el-table-column prop="department" label="部门" width="120"/>
      <el-table-column prop="status" label="状态" width="80"><template #default="s">{{s.row.status===1?'启用':'禁用'}}</template></el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170"/>
      <el-table-column prop="lastLoginTime" label="最后登录" width="170"/>
      <el-table-column label="第三方绑定" min-width="180"><template #default="s">{{ (s.row.thirdPartyBinds||[]).map(v=>`${v.provider}:${v.bindStatus}`).join('；') || '-' }}</template></el-table-column>
      <el-table-column label="操作" width="380">
        <template #default="s">
          <el-button link @click="edit(s.row)">编辑</el-button>
          <el-button link @click="changeStatus(s.row)">{{s.row.status===1?'禁用':'启用'}}</el-button>
          <el-button link @click="resetPwd(s.row)">重置密码</el-button>
          <el-button link @click="openAssignRole(s.row)">分配角色</el-button>
          <el-button link type="danger" @click="remove(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="dialog" :title="form.id?'编辑用户':'新增用户'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="工号"><el-input v-model="form.employeeNo"/></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id"/></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName"/></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role"><el-option label="管理员" value="admin"/><el-option label="普通用户" value="user"/><el-option label="维修人员" value="maintainer"/></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone"/></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email"/></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department"/></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0"/></el-form-item>
        <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignRoleDialog" title="分配角色">
      <el-form :model="assignRoleForm"><el-form-item label="角色"><el-select v-model="assignRoleForm.role"><el-option label="管理员" value="admin"/><el-option label="普通用户" value="user"/><el-option label="维修人员" value="maintainer"/></el-select></el-form-item></el-form>
      <template #footer><el-button @click="assignRoleDialog=false">取消</el-button><el-button type="primary" @click="saveAssignRole">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="batchDialog" title="批量新增用户(JSON数组)">
      <el-input type="textarea" v-model="batchText" :rows="10" placeholder='[{"employeeNo":"U001","username":"u001","realName":"张三","role":"user","phone":"13800138000","department":"运维"}]'/>
      <template #footer><el-button @click="batchDialog=false">取消</el-button><el-button type="primary" @click="batchAdd">提交</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'
import { getPage, postApi, putApi, delApi } from '../../api'
import { ElMessageBox, ElMessage } from 'element-plus'

const query=reactive({employeeNo:'',username:'',role:'',phone:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const dialog=ref(false),form=reactive({})
const assignRoleDialog=ref(false),assignRoleForm=reactive({id:null,role:'user'})
const batchDialog=ref(false),batchText=ref('')

const load=async()=>{const r=await getPage('/users/page',{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{query.employeeNo='';query.username='';query.role='';query.phone='';load()}
const openAdd=()=>{Object.assign(form,{id:null,employeeNo:'',username:'',realName:'',role:'user',phone:'',email:'',department:'',status:1,password:'123456'});dialog.value=true}
const edit=(row)=>{Object.assign(form,{...row,password:''});dialog.value=true}
const save=async()=>{if(form.id) await putApi(`/users/${form.id}`,form); else await postApi('/users',form);ElMessage.success('保存成功');dialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该用户吗？','删除确认');await delApi(`/users/${row.id}`);ElMessage.success('删除成功');load()}
const resetPwd=async(row)=>{await putApi(`/users/${row.id}/reset-password`,{});ElMessage.success('密码已重置为123456')}
const changeStatus=async(row)=>{await putApi(`/users/${row.id}/status`,{status:row.status===1?0:1});ElMessage.success('状态更新成功');load()}
const openAssignRole=(row)=>{assignRoleForm.id=row.id;assignRoleForm.role=row.role||'user';assignRoleDialog.value=true}
const saveAssignRole=async()=>{await putApi(`/users/${assignRoleForm.id}/assign-role?role=${assignRoleForm.role}`,{});ElMessage.success('角色分配成功');assignRoleDialog.value=false;load()}
const batchAdd=async()=>{const data=JSON.parse(batchText.value||'[]');const r=await postApi('/users/batch',data);ElMessage.success(`成功${r.successCount}条，失败${r.failCount}条`);if((r.errors||[]).length) ElMessageBox.alert((r.errors||[]).join('<br/>'),'失败明细',{dangerouslyUseHTMLString:true});batchDialog.value=false;load()}
const importExcel=async(file)=>{const fd=new FormData();fd.append('file',file.raw);const token=localStorage.getItem('token');const res=await fetch('/api/users/import-excel',{method:'POST',headers:token?{Authorization:`Bearer ${token}`}:{},body:fd});const json=await res.json();if(json.code!==200) throw new Error(json.message||'导入失败');ElMessage.success(`导入成功${json.data.successCount}条，失败${json.data.failCount}条`);if((json.data.errors||[]).length) ElMessageBox.alert((json.data.errors||[]).join('<br/>'),'导入失败明细',{dangerouslyUseHTMLString:true});load()}

onMounted(load)
</script>
