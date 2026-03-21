<template>
  <div>
    <h2>公告管理</h2>
    <el-form :inline="true" :model="query"><el-form-item label="标题"><el-input v-model="query.title"/></el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button></el-form>
    <el-button type="primary" v-if="isAdmin" @click="openAdd">新增公告</el-button>
    <el-table :data="list" style="margin-top:12px"><el-table-column prop="title" label="标题"/><el-table-column prop="content" label="内容" show-overflow-tooltip/><el-table-column prop="createTime" label="发布时间"/><el-table-column label="操作" width="220"><template #default="s"><el-button link @click="view(s.row)">查看</el-button><template v-if="isAdmin"><el-button link @click="edit(s.row)">编辑</el-button><el-button link type="danger" @click="remove(s.row)">删除</el-button></template></template></el-table-column></el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>
    <el-dialog v-model="dialog" :title="mode==='view'?'公告详情':(form.id?'编辑公告':'新增公告')"><el-form :model="form" :disabled="mode==='view'" label-width="70px"><el-form-item label="标题"><el-input v-model="form.title"/></el-form-item><el-form-item label="内容"><el-input type="textarea" rows="6" v-model="form.content"/></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button v-if="mode!=='view'" type="primary" @click="save">保存</el-button></template></el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getPage, postApi, putApi, delApi } from '../../api'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
const isAdmin = computed(()=>useUserStore().userInfo.role==='admin')
const query=reactive({title:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0),dialog=ref(false),mode=ref('edit'),form=reactive({})
const load=async()=>{const r=await getPage('/notices/page',{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{query.title='';load()}
const openAdd=()=>{mode.value='edit';Object.assign(form,{id:null,title:'',content:''});dialog.value=true}
const edit=(row)=>{mode.value='edit';Object.assign(form,row);dialog.value=true}
const view=(row)=>{mode.value='view';Object.assign(form,row);dialog.value=true}
const save=async()=>{if(form.id) await putApi(`/notices/${form.id}`,form); else await postApi('/notices',form);ElMessage.success('保存成功');dialog.value=false;load()}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该公告吗？','删除确认');await delApi(`/notices/${row.id}`);ElMessage.success('删除成功');load()}
onMounted(load)
</script>
