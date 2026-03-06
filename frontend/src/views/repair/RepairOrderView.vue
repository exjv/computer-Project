<template><CrudPage title="报修工单" url="/repair-orders/my" :columns="cols" create-url="/repair-orders" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
const cols=[{prop:'orderNo',label:'工单编号'},{prop:'title',label:'故障标题'},{prop:'priority',label:'优先级'},{prop:'status',label:'状态'},{prop:'reportTime',label:'报修时间'}]
</script>
