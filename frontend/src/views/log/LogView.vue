<template>
  <div>
    <h2>日志管理</h2>
    <el-tabs v-model="active">
      <el-tab-pane label="登录日志" name="login">
        <el-table :data="loginList"><el-table-column prop="username" label="用户名"/><el-table-column prop="ip" label="IP地址"/><el-table-column prop="loginStatus" label="登录状态"/><el-table-column prop="loginTime" label="登录时间"/></el-table>
        <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="loginTotal" @current-change="p=>{loginPage.current=p;loadLogin()}"/>
      </el-tab-pane>
      <el-tab-pane label="操作日志" name="op">
        <el-table :data="opList"><el-table-column prop="username" label="操作人"/><el-table-column prop="module" label="模块"/><el-table-column prop="operationType" label="类型"/><el-table-column prop="operationDesc" label="描述"/><el-table-column prop="operationTime" label="时间"/></el-table>
        <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="opTotal" @current-change="p=>{opPage.current=p;loadOp()}"/>
      </el-tab-pane>
      <el-tab-pane label="业务日志" name="business">
        <el-form :inline="true" :model="bizQuery" style="margin-bottom:8px">
          <el-form-item label="工号"><el-input v-model="bizQuery.employeeNo" /></el-form-item>
          <el-form-item label="用户"><el-input v-model="bizQuery.username" /></el-form-item>
          <el-form-item label="工单号"><el-input v-model="bizQuery.orderNo" /></el-form-item>
          <el-form-item label="动作"><el-input v-model="bizQuery.actionType" /></el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="bizDateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-button type="primary" @click="applyBizFilters">筛选</el-button>
          <el-button @click="resetBiz">重置</el-button>
        </el-form>
        <el-table :data="bizList">
          <el-table-column prop="employeeNo" label="工号"/>
          <el-table-column prop="username" label="用户"/>
          <el-table-column prop="role" label="角色"/>
          <el-table-column prop="orderNo" label="工单号"/>
          <el-table-column prop="actionType" label="动作"/>
          <el-table-column prop="content" label="内容"/>
          <el-table-column prop="createTime" label="时间"/>
        </el-table>
        <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="bizTotal" @current-change="p=>{bizPage.current=p;loadBiz()}"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import { getPage } from '../../api'
const active=ref('login')
const loginList=ref([]),loginTotal=ref(0),loginPage=reactive({current:1,size:10})
const opList=ref([]),opTotal=ref(0),opPage=reactive({current:1,size:10})
const bizList=ref([]),bizTotal=ref(0),bizPage=reactive({current:1,size:10})
const bizQuery=reactive({employeeNo:'',username:'',orderNo:'',actionType:'',dateFrom:'',dateTo:''})
const bizDateRange=ref([])
const loadLogin=async()=>{const r=await getPage('/logs/login/page',loginPage);loginList.value=r.records;loginTotal.value=r.total}
const loadOp=async()=>{const r=await getPage('/logs/operation/page',opPage);opList.value=r.records;opTotal.value=r.total}
const loadBiz=async()=>{
  bizQuery.dateFrom=bizDateRange.value?.[0] || ''
  bizQuery.dateTo=bizDateRange.value?.[1] || ''
  const r=await getPage('/logs/business/page',{...bizPage,...bizQuery})
  bizList.value=r.records
  bizTotal.value=r.total
}
const applyBizFilters=()=>{
  bizPage.current=1
  loadBiz()
}
const resetBiz=()=>{
  bizPage.current=1
  bizQuery.employeeNo=''
  bizQuery.username=''
  bizQuery.orderNo=''
  bizQuery.actionType=''
  bizQuery.dateFrom=''
  bizQuery.dateTo=''
  bizDateRange.value=[]
  loadBiz()
}
onMounted(async()=>{await loadLogin();await loadOp();await loadBiz()})
</script>
