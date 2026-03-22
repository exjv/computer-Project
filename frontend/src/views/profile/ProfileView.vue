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
          <el-form :model="pwd" label-width="120px">
            <el-form-item label="旧密码"><el-input v-model="pwd.oldPassword" type="password"/></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password"/></el-form-item>
            <el-form-item label="确认新密码"><el-input v-model="pwd.confirmNewPassword" type="password"/></el-form-item>
            <el-form-item label="验证码">
              <div style="display:flex;gap:8px;width:100%">
                <el-input v-model="pwd.captchaCode" placeholder="请输入验证码"/>
                <el-button @click="refreshCaptcha">{{ pwd.captchaView || '加载中' }}</el-button>
              </div>
            </el-form-item>
            <el-alert :closable="false" type="warning" show-icon style="margin-bottom: 10px">
              密码需至少 8 位，且必须包含字母和数字。
            </el-alert>
            <el-button type="primary" @click="savePwd">修改密码</el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { onMounted, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { captchaApi, updatePasswordApi, updateProfileApi } from '../../api'
import { ElMessage } from 'element-plus'
const store=useUserStore()
const profile=reactive({...store.userInfo})
const pwd=reactive({oldPassword:'',newPassword:'',confirmNewPassword:'',captchaCode:'',captchaKey:'',captchaView:''})
const saveProfile=async()=>{await updateProfileApi(profile);ElMessage.success('个人信息更新成功');await store.fetchUserInfo();Object.assign(profile,store.userInfo)}
const refreshCaptcha=async()=>{const data=await captchaApi();pwd.captchaView=data.captchaCode;pwd.captchaKey=data.captchaKey;pwd.captchaCode=''}
const savePwd=async()=>{
  if(pwd.newPassword!==pwd.confirmNewPassword){ElMessage.error('两次新密码不一致');return}
  await updatePasswordApi(pwd)
  ElMessage.success('密码修改成功')
  Object.assign(pwd,{oldPassword:'',newPassword:'',confirmNewPassword:'',captchaCode:''})
  await refreshCaptcha()
}
onMounted(refreshCaptcha)
</script>
