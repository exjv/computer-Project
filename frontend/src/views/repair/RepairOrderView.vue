<template>
  <div>
    <h2>{{ pageTitle }}</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单编号"><el-input v-model="query.orderNo" /></el-form-item>
      <el-form-item label="故障标题"><el-input v-model="query.title" /></el-form-item>
      <el-form-item label="优先级"><el-select v-model="query.priority" clearable><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item>
      <el-form-item label="工单状态"><el-select v-model="query.status" clearable><el-option v-for="s in allStatus" :key="s" :label="s" :value="s"/></el-select></el-form-item>
      <el-form-item label="排序字段">
        <el-select v-model="query.sortField" style="width:120px"><el-option label="创建时间" value="id"/><el-option label="报修时间" value="reportTime"/><el-option label="优先级" value="priority"/><el-option label="状态" value="status"/></el-select>
      </el-form-item>
      <el-form-item label="排序方式">
        <el-select v-model="query.sortOrder" style="width:100px"><el-option label="降序" value="desc"/><el-option label="升序" value="asc"/></el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </el-form>

    <div style="margin-bottom:10px">
      <el-button type="primary" v-if="isUser || isAdmin" @click="openAdd">提交报修</el-button>
      <el-button type="success" v-if="isAdmin" @click="autoDispatch">自动分配工单</el-button>
    </div>
    <el-card v-if="stats.predictionComparableCount!==undefined" style="margin-bottom:10px">
      预测样本：{{ stats.predictionComparableCount }}，
      平均绝对误差：{{ Number(stats.predictionAvgAbsErrorHours || 0).toFixed(2) }}小时，
      4小时内命中：{{ stats.predictionWithin4hCount || 0 }}，
      24小时内命中：{{ stats.predictionWithin24hCount || 0 }}
    </el-card>
    <el-card v-if="isAdmin" style="margin-bottom:10px">
      反馈总数：{{ stats.feedbackTotalCount || 0 }}，
      平均满意度：{{ Number(stats.satisfactionAvgScore || 0).toFixed(2) }}分，
      差评工单（<=2分）：{{ stats.lowSatisfactionCount || 0 }}，
      未解决返修：{{ stats.unresolvedFeedbackCount || 0 }}
      <div style="margin-top:8px">
        <el-button size="small" type="warning" @click="openLowDialog">查看差评工单</el-button>
        <el-button size="small" type="danger" @click="openUnresolvedDialog">查看未解决返修</el-button>
      </div>
    </el-card>
    <el-card v-if="isAdmin" style="margin-bottom:10px">
      <el-form :inline="true" :model="analyticsQuery">
        <el-form-item label="统计维度">
          <el-select v-model="analyticsQuery.rangeType" style="width:160px">
            <el-option label="日" value="day"/>
            <el-option label="月" value="month"/>
            <el-option label="半年" value="halfyear"/>
            <el-option label="年" value="year"/>
            <el-option label="自定义" value="custom"/>
          </el-select>
        </el-form-item>
        <el-form-item label="自定义区间" v-if="analyticsQuery.rangeType==='custom'">
          <el-date-picker v-model="analyticsQuery.customRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" range-separator="~" start-placeholder="开始" end-placeholder="结束"/>
        </el-form-item>
        <el-button type="primary" @click="loadAnalytics">刷新分析</el-button>
      </el-form>
      <div style="display:flex;gap:12px;flex-wrap:wrap">
        <div ref="trendChartRef" style="width:48%;height:300px;min-width:420px"></div>
        <div ref="faultChartRef" style="width:48%;height:300px;min-width:420px"></div>
        <div ref="maintainerChartRef" style="width:48%;height:300px;min-width:420px"></div>
        <div ref="satisfactionChartRef" style="width:48%;height:300px;min-width:420px"></div>
      </div>
      <div style="margin-top:8px">
        <el-tag type="info" style="margin-right:8px">延期占比 {{ Number(analyticsData.delayOrderRatio || 0).toFixed(2) }}%</el-tag>
        <el-tag type="info" style="margin-right:8px">配件采购占比 {{ Number(analyticsData.partsPurchaseRatio || 0).toFixed(2) }}%</el-tag>
        <el-tag type="success" style="margin-right:8px;cursor:pointer" @click="applyDrilldown({applyDelay:1,status:'维修中'})">延期工单数联动</el-tag>
        <el-tag type="warning" style="cursor:pointer" @click="applyDrilldown({needPurchaseParts:1})">配件采购工单联动</el-tag>
      </div>
    </el-card>

    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="orderNo" label="工单编号" width="180" />
      <el-table-column prop="title" label="故障标题" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="status" label="工单状态" width="160" />
      <el-table-column prop="reportTime" label="报修时间" width="180" />
      <el-table-column label="操作" width="320">
        <template #default="s">
          <el-button link @click="goDetail(s.row)">详情</el-button>
          <el-button v-if="isAdmin && s.row.status==='待分配'" link @click="assign(s.row)">分配</el-button>
          <el-button v-if="isAdmin && canReassign(s.row)" link @click="reassign(s.row)">改派</el-button>
          <el-button v-if="isAdmin && s.row.status==='申请延期中'" link @click="openDelayApprove(s.row)">审批延期</el-button>
          <el-button v-if="isAdmin && (s.row.status==='已完成' || s.row.status!=='已关闭')" link type="danger" @click="openClose(s.row)">关闭/强制关闭</el-button>
          <el-button v-if="canAction(s.row,'ADMIN_APPROVE')" link @click="quickAction(s.row,'ADMIN_APPROVE')">审核通过</el-button>
          <el-button v-if="canAction(s.row,'ADMIN_REJECT')" link @click="quickAction(s.row,'ADMIN_REJECT')">审核驳回</el-button>
          <el-button v-if="canAction(s.row,'MAINTAINER_ACCEPT')" link @click="quickAction(s.row,'MAINTAINER_ACCEPT')">接单</el-button>
          <el-button v-if="canAction(s.row,'MAINTAINER_START')" link @click="quickAction(s.row,'MAINTAINER_START')">开始维修</el-button>
          <el-button v-if="canAction(s.row,'MAINTAINER_FINISH')" link @click="quickAction(s.row,'MAINTAINER_FINISH')">提交完工</el-button>
          <el-button v-if="isAdmin || isMaintainer" link @click="changeStatus(s.row)">手工改状态</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="addDialog" :title="editMode ? '编辑工单':'提交报修'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备"><el-select v-model="form.deviceId"><el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id"/></el-select></el-form-item>
        <el-form-item label="故障标题"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="故障类型"><el-input v-model="form.faultType"/></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contactPhone"/></el-form-item>
        <el-form-item label="报修地点"><el-input v-model="form.reportLocation"/></el-form-item>
        <el-form-item label="故障描述"><el-input type="textarea" v-model="form.description"/></el-form-item>
        <el-form-item label="优先级"><el-select v-model="form.priority"><el-option label="低" value="低"/><el-option label="中" value="中"/><el-option label="高" value="高"/></el-select></el-form-item>
        <el-form-item label="影响范围"><el-switch v-model="form.affectWideAreaNetwork" :active-value="1" :inactive-value="0" active-text="影响大范围网络"/></el-form-item>
        <el-form-item label="原预计完成"><el-date-picker v-model="form.originalExpectedFinishTime" value-format="YYYY-MM-DD HH:mm:ss" type="datetime"/></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="addDialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="分配维修人员" width="760px">
      <el-form :model="assignForm">
        <el-form-item label="维修人员"><el-select v-model="assignForm.assignMaintainerId"><el-option v-for="m in maintainers" :key="m.id" :label="m.realName+'('+m.username+')'" :value="m.id"/></el-select></el-form-item>
      </el-form>
      <el-divider>推荐分配（可手动调整）</el-divider>
      <el-table :data="recommendations" size="small" max-height="280">
        <el-table-column prop="maintainerName" label="维修人员" width="120"/>
        <el-table-column prop="recommendationScore" label="推荐分" width="90"/>
        <el-table-column prop="loadScore" label="负载分" width="90"/>
        <el-table-column label="负载情况" width="180">
          <template #default="s">未完成{{ s.row.unfinishedCount }} / 处理中{{ s.row.processingCount }}</template>
        </el-table-column>
        <el-table-column prop="recommendReason" label="推荐理由"/>
        <el-table-column label="选择" width="80"><template #default="s"><el-button link type="primary" @click="assignForm.assignMaintainerId=s.row.maintainerId">选中</el-button></template></el-table-column>
      </el-table>
      <template #footer><el-button @click="assignDialog=false">取消</el-button><el-button type="primary" @click="saveAssign">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="reassignDialog" title="改派维修人员"><el-form :model="reassignForm"><el-form-item label="维修人员"><el-select v-model="reassignForm.assignMaintainerId"><el-option v-for="m in maintainers" :key="m.id" :label="m.realName+'('+m.username+')'" :value="m.id"/></el-select></el-form-item><el-form-item label="备注"><el-input type="textarea" v-model="reassignForm.remark"/></el-form-item></el-form><template #footer><el-button @click="reassignDialog=false">取消</el-button><el-button type="primary" @click="saveReassign">保存</el-button></template></el-dialog>
    <el-dialog v-model="delayDialog" title="延期审批"><el-form :model="delayForm"><el-form-item label="审批结果"><el-radio-group v-model="delayForm.approved"><el-radio :label="true">通过</el-radio><el-radio :label="false">驳回</el-radio></el-radio-group></el-form-item><el-form-item label="延期完成时间"><el-date-picker v-model="delayForm.delayedExpectedFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item><el-form-item label="备注"><el-input type="textarea" v-model="delayForm.remark"/></el-form-item></el-form><template #footer><el-button @click="delayDialog=false">取消</el-button><el-button type="primary" @click="saveDelayApprove">提交</el-button></template></el-dialog>
    <el-dialog v-model="closeDialog" title="关闭工单"><el-form :model="closeForm"><el-form-item label="关闭类型"><el-radio-group v-model="closeForm.forceClose"><el-radio :label="false">正常关闭</el-radio><el-radio :label="true">强制关闭</el-radio></el-radio-group></el-form-item><el-form-item label="关闭原因"><el-input type="textarea" v-model="closeForm.closeReason"/></el-form-item></el-form><template #footer><el-button @click="closeDialog=false">取消</el-button><el-button type="primary" @click="saveClose">提交</el-button></template></el-dialog>
    <el-dialog v-model="statusDialog" title="修改工单状态"><el-form :model="statusForm"><el-form-item label="状态"><el-select v-model="statusForm.status"><el-option v-for="s in allStatus" :key="s" :label="s" :value="s"/></el-select></el-form-item></el-form><template #footer><el-button @click="statusDialog=false">取消</el-button><el-button type="primary" @click="saveStatus">保存</el-button></template></el-dialog>
    <el-dialog v-model="lowDialog" title="差评工单列表" width="900px">
      <el-table :data="lowList">
        <el-table-column prop="orderNo" label="工单编号" width="180"/>
        <el-table-column prop="title" label="标题" min-width="140"/>
        <el-table-column prop="satisfactionScore" label="满意度" width="90"/>
        <el-table-column prop="feedbackContent" label="反馈意见" min-width="220"/>
        <el-table-column prop="confirmTime" label="确认时间" width="180"/>
      </el-table>
    </el-dialog>
    <el-dialog v-model="unresolvedDialog" title="未解决返修工单列表" width="900px">
      <el-table :data="unresolvedList">
        <el-table-column prop="orderNo" label="工单编号" width="180"/>
        <el-table-column prop="title" label="标题" min-width="140"/>
        <el-table-column prop="orderStatus" label="当前状态" width="130"/>
        <el-table-column prop="assignMaintainerName" label="维修人员" width="110"/>
        <el-table-column prop="feedbackContent" label="反馈意见" min-width="220"/>
        <el-table-column prop="confirmTime" label="确认时间" width="180"/>
      </el-table>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getPage, postApi, putApi, autoDispatchApi } from '../../api'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'

const router = useRouter()
const role = computed(()=>useUserStore().userInfo.role)
const isAdmin = computed(()=>role.value==='admin')
const isUser = computed(()=>role.value==='user')
const isMaintainer = computed(()=>role.value==='maintainer')
const pageTitle = computed(()=>isAdmin.value?'工单管理':(isUser.value?'我的报修':'我的工单'))
const allStatus = ['待提交','已提交/待审核','审核通过','审核驳回','待分配','已分配','待接单','维修人员已接单','维修中','待采购/待配件','申请延期中','延期已批准','待验收/待确认','已完成','已关闭','已取消']
const query=reactive({orderNo:'',title:'',priority:'',status:'',deviceType:'',faultType:'',assignMaintainerId:'',applyDelay:'',needPurchaseParts:'',reportTimeStart:'',reportTimeEnd:'',sortField:'id',sortOrder:'desc'}),page=reactive({current:1,size:10}),list=ref([]),total=ref(0)
const devices=ref([]),maintainers=ref([])
const stats=reactive({})
const analyticsQuery=reactive({ rangeType:'month', customRange:[] })
const analyticsData=reactive({})
const recommendations=ref([])
const addDialog=ref(false),assignDialog=ref(false),statusDialog=ref(false),editMode=ref(false)
const reassignDialog=ref(false),delayDialog=ref(false),closeDialog=ref(false)
const lowDialog=ref(false),unresolvedDialog=ref(false)
const lowList=ref([]),unresolvedList=ref([])
const trendChartRef=ref(null),faultChartRef=ref(null),maintainerChartRef=ref(null),satisfactionChartRef=ref(null)
let trendChart, faultChart, maintainerChart, satisfactionChart
const form=reactive({id:null,deviceId:'',title:'',description:'',priority:'中'})
const assignForm=reactive({id:null,assignMaintainerId:null}),statusForm=reactive({id:null,status:'维修中'})
const reassignForm=reactive({id:null,assignMaintainerId:null,remark:''})
const delayForm=reactive({id:null,approved:true,delayedExpectedFinishTime:'',remark:''})
const closeForm=reactive({id:null,forceClose:false,closeReason:''})
const apiPath = computed(()=>isAdmin.value?'/repair-orders/page':'/repair-orders/my')
const load = async()=>{const r=await getPage(apiPath.value,{...query,...page});list.value=r.records;total.value=r.total;Object.assign(stats, await getPage('/repair-orders/statistics'))}
const reset=()=>{Object.assign(query,{orderNo:'',title:'',priority:'',status:'',deviceType:'',faultType:'',assignMaintainerId:'',applyDelay:'',needPurchaseParts:'',reportTimeStart:'',reportTimeEnd:'',sortField:'id',sortOrder:'desc'});load()}
const openAdd=()=>{editMode.value=false;Object.assign(form,{id:null,deviceId:'',title:'',description:'',priority:'中',faultType:'',contactPhone:'',reportLocation:'',affectWideAreaNetwork:0,remark:'',originalExpectedFinishTime:''});addDialog.value=true}
const save=async()=>{if(editMode.value){await putApi(`/repair-orders/${form.id}`,form);ElMessage.success('修改成功')}else{await postApi('/repair-orders',form);ElMessage.success('提交成功')}addDialog.value=false;load()}
const assign=async(row)=>{assignForm.id=row.id;assignForm.assignMaintainerId=row.assignMaintainerId;recommendations.value=await getPage(`/repair-orders/${row.id}/assign-recommendations`);assignDialog.value=true}
const saveAssign=async()=>{await putApi(`/repair-orders/${assignForm.id}/assign`,assignForm);ElMessage.success('分配成功');assignDialog.value=false;load()}
const canReassign=(row)=>['待接单','维修人员已接单','维修中'].includes(row.status)
const reassign=(row)=>{reassignForm.id=row.id;reassignForm.assignMaintainerId=row.assignMaintainerId;reassignForm.remark='';reassignDialog.value=true}
const saveReassign=async()=>{await putApi(`/repair-orders/${reassignForm.id}/reassign`,reassignForm);ElMessage.success('改派成功');reassignDialog.value=false;load()}
const openDelayApprove=(row)=>{delayForm.id=row.id;delayForm.approved=true;delayForm.delayedExpectedFinishTime='';delayForm.remark='';delayDialog.value=true}
const saveDelayApprove=async()=>{await putApi(`/repair-orders/${delayForm.id}/delay-approve`,delayForm);ElMessage.success('延期审批完成');delayDialog.value=false;load()}
const openClose=(row)=>{closeForm.id=row.id;closeForm.forceClose=false;closeForm.closeReason='';closeDialog.value=true}
const saveClose=async()=>{await putApi(`/repair-orders/${closeForm.id}/close`,closeForm);ElMessage.success('关闭处理成功');closeDialog.value=false;load()}
const changeStatus=(row)=>{statusForm.id=row.id;statusForm.status=row.status;statusDialog.value=true}
const saveStatus=async()=>{await putApi(`/repair-orders/${statusForm.id}/status`,statusForm);ElMessage.success('状态更新成功');statusDialog.value=false;load()}
const autoDispatch=async()=>{const r=await autoDispatchApi();ElMessage.success(`自动分配完成，共分配${r.count}条工单`);load()}
const goDetail = (row)=> router.push(`/repair-orders/${row.id}`)
const quickAction = async (row, action) => { await putApi(`/repair-orders/${row.id}/action`, { action }); ElMessage.success('操作成功'); await load() }
const openLowDialog = async () => {
  const data = await getPage('/repair-orders/feedback/low-satisfaction', { current: 1, size: 50, threshold: 2 })
  lowList.value = data.records || []
  lowDialog.value = true
}
const openUnresolvedDialog = async () => {
  const data = await getPage('/repair-orders/feedback/unresolved', { current: 1, size: 50 })
  unresolvedList.value = data.records || []
  unresolvedDialog.value = true
}
const applyDrilldown = async (filters={}) => {
  Object.assign(query, { deviceType:'', faultType:'', assignMaintainerId:'', applyDelay:'', needPurchaseParts:'' }, filters)
  if (analyticsData.rangeStart && analyticsData.rangeEnd) {
    query.reportTimeStart = String(analyticsData.rangeStart).replace('T', ' ').slice(0,19)
    query.reportTimeEnd = String(analyticsData.rangeEnd).replace('T', ' ').slice(0,19)
  }
  page.current = 1
  await load()
}
const loadAnalytics = async () => {
  const params = { rangeType: analyticsQuery.rangeType }
  if (analyticsQuery.rangeType === 'custom' && analyticsQuery.customRange?.length === 2) {
    params.start = analyticsQuery.customRange[0]
    params.end = analyticsQuery.customRange[1]
  }
  const data = await getPage('/repair-orders/analytics', params)
  Object.assign(analyticsData, data || {})
  await nextTick()
  renderCharts()
}
const initChart = (inst, el) => (inst || echarts.init(el))
const renderCharts = () => {
  if (!isAdmin.value || !trendChartRef.value) return
  trendChart = initChart(trendChart, trendChartRef.value)
  faultChart = initChart(faultChart, faultChartRef.value)
  maintainerChart = initChart(maintainerChart, maintainerChartRef.value)
  satisfactionChart = initChart(satisfactionChart, satisfactionChartRef.value)
  const trend = analyticsData.timeTrend || []
  trendChart.setOption({ title:{text:'报修/完工趋势'}, tooltip:{trigger:'axis'}, xAxis:{type:'category',data:trend.map(v=>v.bucket)}, yAxis:{type:'value'}, series:[{name:'报修数量',type:'bar',data:trend.map(v=>v.reportCount||0)},{name:'已完成数量',type:'line',data:trend.map(v=>v.finishedCount||0)}] })
  const faultRank = analyticsData.faultReasonDistribution || []
  faultChart.setOption({ title:{text:'故障原因分布（点击联动）'}, tooltip:{trigger:'item'}, xAxis:{type:'category',data:faultRank.map(v=>v.name)}, yAxis:{type:'value'}, series:[{type:'bar',data:faultRank.map(v=>v.value||0)}] })
  faultChart.off('click'); faultChart.on('click', p => applyDrilldown({ faultType: faultRank[p.dataIndex]?.name || '' }))
  const maintainer = analyticsData.maintainerOrderCount || []
  maintainerChart.setOption({ title:{text:'维修人员工单数（点击联动）'}, tooltip:{trigger:'item'}, xAxis:{type:'category',data:maintainer.map(v=>v.name)}, yAxis:{type:'value'}, series:[{type:'bar',data:maintainer.map(v=>v.value||0)}] })
  maintainerChart.off('click'); maintainerChart.on('click', p => {
    const name = maintainer[p.dataIndex]?.name
    const m = (maintainers.value || []).find(v => v.realName === name)
    applyDrilldown({ assignMaintainerId: m ? m.id : '' })
  })
  const satisfaction = analyticsData.satisfactionStats?.distribution || []
  satisfactionChart.setOption({ title:{text:'用户满意度统计'}, tooltip:{trigger:'item'}, series:[{type:'pie',radius:'60%',data:satisfaction.map(v=>({name:v.name,value:v.value}))}] })
}
const canAction = (row, action) => {
  if (action === 'ADMIN_APPROVE' || action === 'ADMIN_REJECT') return isAdmin.value && row.status === '已提交/待审核'
  if (action === 'MAINTAINER_ACCEPT') return isMaintainer.value && row.status === '待接单'
  if (action === 'MAINTAINER_START') return isMaintainer.value && row.status === '维修人员已接单'
  if (action === 'MAINTAINER_FINISH') return isMaintainer.value && row.status === '维修中'
  return false
}
onMounted(async()=>{
  await load()
  const d=await getPage('/devices/page',{current:1,size:100})
  devices.value=d.records||[]
  if(isAdmin.value){
    maintainers.value=await getPage('/users/list-by-role',{role:'maintainer'})
    await loadAnalytics()
  }
})
onBeforeUnmount(()=>{ trendChart?.dispose(); faultChart?.dispose(); maintainerChart?.dispose(); satisfactionChart?.dispose() })
</script>
