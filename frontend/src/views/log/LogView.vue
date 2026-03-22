<template>
  <div>
    <h2>系统日志中心</h2>
    <el-tabs v-model="active">
      <el-tab-pane label="系统审计日志" name="audit">
        <el-form :inline="true" :model="auditQuery">
          <el-form-item label="用户"><el-input v-model="auditQuery.username"/></el-form-item>
          <el-form-item label="操作类型"><el-input v-model="auditQuery.operationType"/></el-form-item>
          <el-form-item label="登录状态"><el-select v-model="auditQuery.loginStatus" clearable><el-option label="成功" value="SUCCESS"/><el-option label="失败" value="FAIL"/></el-select></el-form-item>
          <el-form-item label="时间区间"><el-date-picker v-model="auditQuery.range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
          <el-button type="primary" @click="loadAudit">查询</el-button>
        </el-form>
        <el-table :data="auditList">
          <el-table-column prop="category" label="日志类型" width="100"/>
          <el-table-column prop="username" label="用户" width="120"/>
          <el-table-column prop="operationType" label="操作" width="140"/>
          <el-table-column prop="time" label="时间" width="180"/>
          <el-table-column prop="friendlyText" label="友好文案" min-width="420"/>
          <el-table-column label="操作" width="80"><template #default="s"><el-button link @click="detailAudit(s.row)">详情</el-button></template></el-table-column>
        </el-table>
        <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="auditTotal" @current-change="p=>{auditPage.current=p;loadAudit()}"/>
      </el-tab-pane>

      <el-tab-pane label="业务处理日志" name="biz">
        <el-form :inline="true" :model="bizQuery">
          <el-form-item label="处理人"><el-input v-model="bizQuery.operatorName"/></el-form-item>
          <el-form-item label="工号"><el-input v-model="bizQuery.operatorEmployeeNo"/></el-form-item>
          <el-form-item label="工单号"><el-input v-model="bizQuery.orderNo"/></el-form-item>
          <el-form-item label="设备编号"><el-input v-model="bizQuery.deviceCode"/></el-form-item>
          <el-form-item label="操作类型"><el-input v-model="bizQuery.action"/></el-form-item>
          <el-form-item label="时间区间"><el-date-picker v-model="bizQuery.range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"/></el-form-item>
          <el-button type="primary" @click="loadBiz">查询</el-button>
        </el-form>
        <el-table :data="bizList">
          <el-table-column prop="orderNo" label="工单号" width="170"/>
          <el-table-column prop="deviceCode" label="设备编号" width="140"/>
          <el-table-column prop="operatorName" label="处理人" width="110"/>
          <el-table-column prop="operatorEmployeeNo" label="工号" width="120"/>
          <el-table-column prop="action" label="动作" width="130"/>
          <el-table-column prop="time" label="时间" width="180"/>
          <el-table-column prop="friendlyText" label="友好文案" min-width="420"/>
          <el-table-column label="操作" width="80"><template #default="s"><el-button link @click="detailBiz(s.row)">详情</el-button></template></el-table-column>
        </el-table>
        <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="bizTotal" @current-change="p=>{bizPage.current=p;loadBiz()}"/>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailDialog" title="日志详情" width="760px">
      <div style="margin-bottom:8px"><b>友好文案：</b>{{ detailData.friendlyText || '-' }}</div>
      <pre>{{ JSON.stringify(detailData, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import { getPage } from '../../api'

const active=ref('audit')
const detailDialog=ref(false),detailData=ref({})

const auditList=ref([]),auditTotal=ref(0),auditPage=reactive({current:1,size:10})
const auditQuery=reactive({username:'',operationType:'',loginStatus:'',range:[]})

const bizList=ref([]),bizTotal=ref(0),bizPage=reactive({current:1,size:10})
const bizQuery=reactive({operatorName:'',operatorEmployeeNo:'',orderNo:'',deviceCode:'',action:'',range:[]})

const loadAudit=async()=>{
  const params={...auditPage,username:auditQuery.username,operationType:auditQuery.operationType,loginStatus:auditQuery.loginStatus}
  if((auditQuery.range||[]).length===2){params.startTime=auditQuery.range[0];params.endTime=auditQuery.range[1]}
  const r=await getPage('/logs/audit/page',params);auditList.value=r.records||[];auditTotal.value=r.total||0
}
const loadBiz=async()=>{
  const params={...bizPage,operatorName:bizQuery.operatorName,operatorEmployeeNo:bizQuery.operatorEmployeeNo,orderNo:bizQuery.orderNo,deviceCode:bizQuery.deviceCode,action:bizQuery.action}
  if((bizQuery.range||[]).length===2){params.startTime=bizQuery.range[0];params.endTime=bizQuery.range[1]}
  const r=await getPage('/logs/business/page',params);bizList.value=r.records||[];bizTotal.value=r.total||0
}
const detailAudit=async(row)=>{detailData.value=await getPage(`/logs/audit/${row.category}/${row.id}`);detailDialog.value=true}
const detailBiz=async(row)=>{detailData.value=await getPage(`/logs/business/${row.id}`);detailDialog.value=true}

onMounted(async()=>{await loadAudit();await loadBiz()})
</script>
