<template><CrudPage title="用户管理" url="/users/page" :columns="cols" create-url="/users" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
const cols=[{prop:'username',label:'用户名'},{prop:'realName',label:'姓名'},{prop:'role',label:'角色'},{prop:'phone',label:'手机号'},{prop:'status',label:'状态'}]
</script>
