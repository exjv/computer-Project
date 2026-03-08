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
    </el-tabs>
  </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import { getPage } from '../../api'
const active=ref('login')
const loginList=ref([]),loginTotal=ref(0),loginPage=reactive({current:1,size:10})
const opList=ref([]),opTotal=ref(0),opPage=reactive({current:1,size:10})
const loadLogin=async()=>{const r=await getPage('/logs/login/page',loginPage);loginList.value=r.records;loginTotal.value=r.total}
const loadOp=async()=>{const r=await getPage('/logs/operation/page',opPage);opList.value=r.records;opTotal.value=r.total}
onMounted(async()=>{await loadLogin();await loadOp()})
</script>
