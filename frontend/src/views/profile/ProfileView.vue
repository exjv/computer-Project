<template><el-card><el-descriptions title="个人信息" :column="1"><el-descriptions-item label="用户名">{{u.username}}</el-descriptions-item><el-descriptions-item label="姓名">{{u.realName}}</el-descriptions-item><el-descriptions-item label="角色">{{u.role}}</el-descriptions-item></el-descriptions></el-card></template>
<script setup>
import { useUserStore } from '../../stores/user';const u=useUserStore().userInfo
</script>
