<template>
  <div>
    <h2>个人中心</h2>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="修改个人信息">
          <el-form :model="profile" label-width="100px">
            <el-form-item label="用户名"><el-input v-model="profile.username" disabled/></el-form-item>
            <el-form-item label="姓名"><el-input v-model="profile.realName"/></el-form-item>
            <el-form-item label="手机号"><el-input v-model="profile.phone"/></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email"/></el-form-item>
            <el-button type="primary" @click="saveProfile">保存信息</el-button>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="修改密码">
          <el-form :model="pwd" label-width="100px">
            <el-form-item label="旧密码"><el-input v-model="pwd.oldPassword" type="password"/></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password"/></el-form-item>
            <el-button type="primary" @click="savePwd">修改密码</el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { updatePasswordApi, updateProfileApi } from '../../api'
import { ElMessage } from 'element-plus'
const store=useUserStore()
const profile=reactive({...store.userInfo})
const pwd=reactive({oldPassword:'',newPassword:''})
const saveProfile=async()=>{await updateProfileApi(profile);ElMessage.success('个人信息更新成功');await store.fetchUserInfo();Object.assign(profile,store.userInfo)}
const savePwd=async()=>{await updatePasswordApi(pwd);ElMessage.success('密码修改成功');pwd.oldPassword='';pwd.newPassword=''}
</script>
