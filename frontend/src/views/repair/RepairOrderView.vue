<template>
  <div>
    <h2>{{ pageTitle }}</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单编号"><el-input v-model="query.orderNo" /></el-form-item>
      <el-form-item label="故障标题"><el-input v-model="query.title" /></el-form-item>
      <el-form-item label="优先级"><el-select v-model="query.priority" clearable><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item>
      <el-form-item label="工单状态"><el-select v-model="query.status" clearable><el-option v-for="s in allStatus" :key="s" :label="s" :value="s"/></el-select></el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>

    <div style="margin-bottom:10px">
      <el-button type="primary" v-if="can('repair:create')" @click="openAdd">提交报修</el-button>
      <el-button type="success" v-if="can('repair:assign')" @click="autoDispatch">自动分配工单</el-button>
    </div>

    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="orderNo" label="工单编号" />
      <el-table-column prop="title" label="故障标题" />
      <el-table-column prop="priority" label="优先级" />
      <el-table-column prop="status" label="工单状态" />
      <el-table-column prop="assignMaintainerId" label="维修人员ID" />
      <el-table-column prop="reportTime" label="报修时间" />
      <el-table-column label="操作" width="360">
        <template #default="s">
          <el-button link @click="detail(s.row)">详情</el-button>
              <el-button link @click="track(s.row)">跟踪</el-button>
              <el-button v-if="can('repair:assign') && s.row.status==='待分配'" link @click="assign(s.row)">分配</el-button>
              <el-button v-if="canAction(s.row,'ADMIN_APPROVE')" link @click="quickAction(s.row,'ADMIN_APPROVE')">审核通过</el-button>
              <el-button v-if="canAction(s.row,'ADMIN_REJECT')" link @click="quickAction(s.row,'ADMIN_REJECT')">审核驳回</el-button>
              <el-button v-if="canAction(s.row,'MAINTAINER_ACCEPT')" link @click="quickAction(s.row,'MAINTAINER_ACCEPT')">接单</el-button>
              <el-button v-if="canAction(s.row,'MAINTAINER_REJECT')" link type="warning" @click="quickAction(s.row,'MAINTAINER_REJECT')">拒单</el-button>
              <el-button v-if="canAction(s.row,'MAINTAINER_START')" link @click="quickAction(s.row,'MAINTAINER_START')">开始维修</el-button>
              <el-button v-if="canAction(s.row,'MAINTAINER_DELAY_APPLY')" link @click="quickAction(s.row,'MAINTAINER_DELAY_APPLY')">申请延期</el-button>
              <el-button v-if="canAction(s.row,'MAINTAINER_PARTS_APPLY')" link @click="quickAction(s.row,'MAINTAINER_PARTS_APPLY')">申请配件</el-button>
              <el-button v-if="canAction(s.row,'MAINTAINER_FINISH')" link @click="quickAction(s.row,'MAINTAINER_FINISH')">提交完工</el-button>
              <el-button v-if="canAction(s.row,'USER_CANCEL')" link type="warning" @click="quickAction(s.row,'USER_CANCEL')">撤销报修</el-button>
              <el-button v-if="canAction(s.row,'USER_CONFIRM_RESOLVED')" link @click="quickAction(s.row,'USER_CONFIRM_RESOLVED')">确认修复</el-button>
              <el-button v-if="canAction(s.row,'USER_CONFIRM_UNRESOLVED')" link type="warning" @click="quickAction(s.row,'USER_CONFIRM_UNRESOLVED')">未解决退回</el-button>
              <el-button v-if="can('repair:delay:approve') && s.row.status==='维修中'" link @click="quickAction(s.row,'ADMIN_DELAY_APPROVE')">审批延期</el-button>
              <el-button v-if="can('repair:reassign') && ['待接单','维修人员已接单','维修中'].includes(s.row.status)" link @click="reassign(s.row)">改派</el-button>
              <el-button v-if="can('repair:close') && !['已完成','已关闭','已取消'].includes(s.row.status)" link type="danger" @click="quickAction(s.row,'ADMIN_CLOSE')">关闭工单</el-button>
              <el-button v-if="can('repair:all:view') || can('repair:assigned:view')" link @click="changeStatus(s.row)">手工改状态</el-button>
              <el-button v-if="can('repair:all:view')" link @click="edit(s.row)">编辑</el-button>
              <el-button v-if="can('repair:all:view')" link type="danger" @click="remove(s.row)">删除</el-button>
            </template>
          </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="addDialog" :title="editMode ? '编辑工单':'提交报修'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备"><el-select v-model="form.deviceId"><el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id"/></el-select></el-form-item>
        <el-form-item label="故障标题"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="故障描述"><el-input type="textarea" v-model="form.description"/></el-form-item>
        <el-form-item label="优先级"><el-select v-model="form.priority"><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="addDialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignDialog" :title="assignMode==='assign' ? '分配维修人员' : '改派维修人员'"><el-form :model="assignForm"><el-form-item label="维修人员"><el-select v-model="assignForm.assignMaintainerId"><el-option v-for="m in maintainers" :key="m.id" :label="m.realName+'('+m.username+')'" :value="m.id"/></el-select></el-form-item></el-form><template #footer><el-button @click="assignDialog=false">取消</el-button><el-button type="primary" @click="saveAssign">保存</el-button></template></el-dialog>
    <el-dialog v-model="statusDialog" title="修改工单状态"><el-form :model="statusForm"><el-form-item label="状态"><el-select v-model="statusForm.status"><el-option v-for="s in allStatus" :key="s" :label="s" :value="s"/></el-select></el-form-item></el-form><template #footer><el-button @click="statusDialog=false">取消</el-button><el-button type="primary" @click="saveStatus">保存</el-button></template></el-dialog>
    <el-dialog v-model="detailDialog" title="工单详情" width="860px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ current.status }}</el-descriptions-item>
        <el-descriptions-item label="故障标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ current.priority }}</el-descriptions-item>
        <el-descriptions-item label="维修进度">{{ current.progress || 0 }}%</el-descriptions-item>
        <el-descriptions-item label="维修人员">{{ current.assignMaintainerId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>流程时间轴</el-divider>
      <el-timeline>
        <el-timeline-item v-for="f in flowList" :key="f.id" :timestamp="f.createTime">
          {{ f.fromStatus || '开始' }} → {{ f.toStatus }} | {{ f.action }} | {{ f.remark || '无备注' }}
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPage, postApi, putApi, delApi, autoDispatchApi } from '../../api'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
const store = useUserStore()
const router = useRouter()
const role = computed(()=>store.userInfo.role)
const isAdmin = computed(()=>role.value==='admin')
const isUser = computed(()=>role.value==='user')
const isMaintainer = computed(()=>role.value==='maintainer')
const pageTitle = computed(()=>isAdmin.value?'工单管理':(isUser.value?'我的报修':'我的工单'))
const allStatus = ['待提交','已提交/待审核','审核通过','审核驳回','待分配','已分配','待接单','维修人员已接单','维修中','待采购/待配件','申请延期中','延期已批准','待验收/待确认','已完成','已关闭','已取消']
const query=reactive({orderNo:'',title:'',priority:'',status:''}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const devices=ref([]),maintainers=ref([])
const addDialog=ref(false),assignDialog=ref(false),statusDialog=ref(false),detailDialog=ref(false),editMode=ref(false)
const assignMode=ref('assign')
const form=reactive({id:null,deviceId:'',title:'',description:'',priority:'中'})
const current=ref({}),assignForm=reactive({id:null,assignMaintainerId:null}),statusForm=reactive({id:null,status:'维修中'})
const flowList=ref([])
const apiPath = computed(()=>isAdmin.value?'/repair-orders/page':'/repair-orders/my')
const can = (perm) => store.hasPerm(perm)
const load = async()=>{const r=await getPage(apiPath.value,{...query,...page});list.value=r.records;total.value=r.total}
const reset=()=>{Object.assign(query,{orderNo:'',title:'',priority:'',status:''});load()}
const openAdd=()=>{editMode.value=false;Object.assign(form,{id:null,deviceId:'',title:'',description:'',priority:'中'});addDialog.value=true}
const edit=(row)=>{editMode.value=true;Object.assign(form,row);addDialog.value=true}
const save=async()=>{if(editMode.value){await putApi(`/repair-orders/${form.id}`,form);ElMessage.success('修改成功')}else{await postApi('/repair-orders',form);ElMessage.success('提交成功')}addDialog.value=false;load()}
const assign=(row)=>{assignMode.value='assign';assignForm.id=row.id;assignForm.assignMaintainerId=row.assignMaintainerId;assignDialog.value=true}
const saveAssign=async()=>{
  if (!assignForm.assignMaintainerId) return ElMessage.warning('请选择维修人员')
  if (assignMode.value === 'assign') {
    await putApi(`/repair-orders/${assignForm.id}/assign`,assignForm)
    ElMessage.success('分配成功')
  } else {
    await putApi(`/repair-orders/${assignForm.id}/action`,{ action:'ADMIN_REASSIGN', assignMaintainerId: assignForm.assignMaintainerId })
    ElMessage.success('改派成功')
  }
  assignDialog.value=false
  load()
}
const changeStatus=(row)=>{statusForm.id=row.id;statusForm.status=row.status;statusDialog.value=true}
const saveStatus=async()=>{await putApi(`/repair-orders/${statusForm.id}/status`,statusForm);ElMessage.success('状态更新成功');statusDialog.value=false;load()}
const detail=async(row)=>{router.push(`/repair-orders/${row.id}`)}
const track=async(row)=>{router.push(`/repair-orders/${row.id}/progress`)}
const remove=async(row)=>{await ElMessageBox.confirm('确认删除该工单吗？','删除确认');await delApi(`/repair-orders/${row.id}`);ElMessage.success('删除成功');load()}
const autoDispatch=async()=>{const r=await autoDispatchApi();ElMessage.success(`自动分配完成，共分配${r.count}条工单`);load()}
const quickAction = async (row, action) => {
  let payload = { action }
  if (action === 'ADMIN_CLOSE' || action === 'USER_CANCEL') {
    const { value } = await ElMessageBox.prompt('请输入原因说明', action === 'ADMIN_CLOSE' ? '关闭工单' : '撤销报修', { inputPlaceholder: '请填写原因' })
    payload.remark = value
  }
  if (action === 'USER_CONFIRM_RESOLVED') {
    const { value } = await ElMessageBox.prompt('请输入评价（可选）', '维修评价', { inputPlaceholder: '如：处理及时，问题解决' })
    payload.feedback = value
    payload.satisfactionScore = 5
  }
  await putApi(`/repair-orders/${row.id}/action`, payload)
  ElMessage.success('操作成功')
  await load()
}
const canAction = (row, action) => {
  if (action === 'ADMIN_APPROVE') return can('repair:audit') && row.status === '已提交/待审核'
  if (action === 'ADMIN_REJECT') return can('repair:reject') && row.status === '已提交/待审核'
  if (action === 'MAINTAINER_ACCEPT') return can('repair:accept') && row.status === '待接单'
  if (action === 'MAINTAINER_REJECT') return can('repair:reject:receive') && row.status === '待接单'
  if (action === 'MAINTAINER_START') return can('repair:start') && row.status === '维修人员已接单'
  if (action === 'MAINTAINER_DELAY_APPLY') return can('repair:delay:apply') && ['维修中','延期已批准'].includes(row.status)
  if (action === 'MAINTAINER_PARTS_APPLY') return can('repair:parts:apply') && row.status === '维修中'
  if (action === 'MAINTAINER_FINISH') return can('repair:finish') && ['维修中','延期已批准','待采购/待配件'].includes(row.status)
  if (action === 'USER_CANCEL') return can('repair:cancel') && ['已提交/待审核','审核驳回'].includes(row.status)
  if (action === 'USER_CONFIRM_RESOLVED' || action === 'USER_CONFIRM_UNRESOLVED') return can('repair:confirm') && row.status === '待验收/待确认'
  return false
}
const reassign = async (row) => { assignMode.value='reassign'; assignForm.id=row.id; assignForm.assignMaintainerId=row.assignMaintainerId; assignDialog.value=true }
onMounted(async()=>{await load();const d=await getPage('/devices/page',{current:1,size:100});devices.value=d.records||[];if(isAdmin.value){maintainers.value=await getPage('/users/list-by-role',{role:'maintainer'})}})
</script>
