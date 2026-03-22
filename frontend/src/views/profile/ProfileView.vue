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
        <el-card header="第三方账号绑定" style="margin-top: 12px">
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
            <el-tag :type="bindPlatforms.includes('WECHAT') ? 'success' : 'info'">微信 {{ bindPlatforms.includes('WECHAT') ? '已绑定' : '未绑定' }}</el-tag>
            <el-button size="small" @click="bind('wechat')" :disabled="bindPlatforms.includes('WECHAT')">绑定微信</el-button>
            <el-button size="small" type="danger" @click="unbind('wechat')" :disabled="!bindPlatforms.includes('WECHAT')">解绑微信</el-button>
          </div>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-top:10px">
            <el-tag :type="bindPlatforms.includes('QQ') ? 'success' : 'info'">QQ {{ bindPlatforms.includes('QQ') ? '已绑定' : '未绑定' }}</el-tag>
            <el-button size="small" @click="bind('qq')" :disabled="bindPlatforms.includes('QQ')">绑定QQ</el-button>
            <el-button size="small" type="danger" @click="unbind('qq')" :disabled="!bindPlatforms.includes('QQ')">解绑QQ</el-button>
          </div>
          <p style="margin-top:10px;color:#909399">当前为OAuth预留流程，使用模拟code进行绑定。</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { computed, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { bindThirdPartyApi, unbindThirdPartyApi, updatePasswordApi, updateProfileApi } from '../../api'
import { ElMessage } from 'element-plus'
const store=useUserStore()
const profile=reactive({...store.userInfo})
const pwd=reactive({oldPassword:'',newPassword:''})
const bindPlatforms = computed(() => (store.userInfo.thirdPartyBinds || []))
const saveProfile=async()=>{await updateProfileApi(profile);ElMessage.success('个人信息更新成功');await store.fetchUserInfo();Object.assign(profile,store.userInfo)}
const savePwd=async()=>{await updatePasswordApi(pwd);ElMessage.success('密码修改成功');pwd.oldPassword='';pwd.newPassword=''}
const bind = async (provider) => {
  await bindThirdPartyApi(provider, { code: 'demo-code' })
  ElMessage.success('绑定成功（预留）')
  await store.fetchUserInfo()
}
const unbind = async (provider) => {
  await unbindThirdPartyApi(provider)
  ElMessage.success('解绑成功')
  await store.fetchUserInfo()
}
</script>
