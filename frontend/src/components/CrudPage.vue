<template>
  <el-card>
    <template #header>{{ title }}</template>
    <el-button v-if="!disableCreate" type="primary" @click="dialog=true">新增</el-button>
    <el-table :data="list" style="margin-top:12px"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" /></el-table>
    <el-pagination background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}" />
  </el-card>
  <el-dialog v-model="dialog" title="新增">
    <el-form :model="form"><el-form-item v-for="c in columns.slice(0,3)" :key="c.prop" :label="c.label"><el-input v-model="form[c.prop]"/></el-form-item></el-form>
    <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue';import { pageApi, postApi } from '../api'
const props=defineProps({title:String,url:String,columns:Array,createUrl:String,disableCreate:Boolean})
const list=ref([]),total=ref(0),dialog=ref(false),form=reactive({}),page=reactive({current:1,size:10})
const load=async()=>{const res=await pageApi(props.url,page);list.value=res.records||[];total.value=res.total||0}
const save=async()=>{await postApi(props.createUrl,form);dialog.value=false;Object.keys(form).forEach(k=>form[k]='');load()}
onMounted(load)
</script>
