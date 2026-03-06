<template><el-row :gutter="16"><el-col :span="8" v-for="(v,k) in stats" :key="k"><el-card>{{k}}: {{v}}</el-card></el-col></el-row></template>
<script setup>
import { onMounted, reactive } from 'vue';import { pageApi } from '../../api'
const stats=reactive({})
onMounted(async()=>{Object.assign(stats, await pageApi('/repair-orders/statistics', {}))})
</script>
