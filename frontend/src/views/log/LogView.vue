<template><CrudPage title="登录日志" url="/logs/login/page" :columns="cols" :disable-create="true" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
const cols=[{prop:'username',label:'用户名'},{prop:'ip',label:'IP'},{prop:'loginStatus',label:'状态'},{prop:'loginTime',label:'时间'}]
</script>
