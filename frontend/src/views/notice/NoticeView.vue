<template><CrudPage title="公告管理" url="/notices/page" :columns="cols" create-url="/notices" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
const cols=[{prop:'title',label:'标题'},{prop:'content',label:'内容'},{prop:'createTime',label:'发布时间'}]
</script>
