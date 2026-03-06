<template><CrudPage title="维修记录" url="/repair-records/page" :columns="cols" create-url="/repair-records" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
const cols=[{prop:'repairOrderId',label:'工单ID'},{prop:'faultReason',label:'故障原因'},{prop:'resultDetail',label:'处理结果'},{prop:'isResolved',label:'是否解决'},{prop:'repairTime',label:'处理时间'}]
</script>
