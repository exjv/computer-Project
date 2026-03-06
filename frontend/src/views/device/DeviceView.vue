<template><CrudPage title="设备管理" url="/devices/page" :columns="cols" create-url="/devices" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
const cols=[{prop:'deviceCode',label:'设备编号'},{prop:'deviceName',label:'设备名称'},{prop:'deviceType',label:'类型'},{prop:'ipAddress',label:'IP'},{prop:'status',label:'状态'}]
</script>
