<template><div style="width:360px;margin:120px auto"><el-card><h2>系统登录</h2><el-form :model="form"><el-form-item label="用户名"><el-input v-model="form.username"/></el-form-item><el-form-item label="密码"><el-input v-model="form.password" type="password"/></el-form-item><el-button type="primary" style="width:100%" @click="onLogin">登录</el-button></el-form></el-card></div></template>
<script setup>
import { reactive } from 'vue';import { useUserStore } from '../../stores/user';import { useRouter } from 'vue-router'
const form=reactive({username:'admin',password:'123456'});const store=useUserStore();const router=useRouter();
const onLogin=async()=>{await store.login(form);router.push('/')}
</script>
