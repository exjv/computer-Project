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
            <el-form-item label="旧密码"><el-input v-model="pwd.oldPassword" type="password" show-password/></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password/></el-form-item>
            <el-form-item label="确认新密码"><el-input v-model="pwd.confirmPassword" type="password" show-password/></el-form-item>
            <el-form-item label="验证码">
              <div class="captcha-row">
                <el-input v-model="pwd.captchaCode" placeholder="输入验证码" />
                <img v-if="captcha.captchaImage" :src="captcha.captchaImage" class="captcha-img" @click="refreshCaptcha" />
                <el-button v-else @click="refreshCaptcha">刷新验证码</el-button>
              </div>
              <div class="captcha-tip">点击验证码可刷新；密码需至少8位且包含字母+数字</div>
            </el-form-item>
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
import { computed, onMounted, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { bindThirdPartyApi, captchaApi, unbindThirdPartyApi, updatePasswordApi, updateProfileApi } from '../../api'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const profile = reactive({ ...store.userInfo })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '', captchaCode: '', captchaKey: '' })
const captcha = reactive({ captchaImage: '' })
const bindPlatforms = computed(() => (store.userInfo.thirdPartyBinds || []))

const saveProfile = async () => {
  await updateProfileApi(profile)
  ElMessage.success('个人信息更新成功')
  await store.fetchUserInfo()
  Object.assign(profile, store.userInfo)
}

const refreshCaptcha = async () => {
  const data = await captchaApi()
  captcha.captchaImage = data.captchaImage
  pwd.captchaKey = data.captchaKey
  pwd.captchaCode = ''
}

const strongPassword = value => /^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value || '')

const savePwd = async () => {
  if (!pwd.oldPassword || !pwd.newPassword || !pwd.confirmPassword) return ElMessage.warning('请完整填写密码项')
  if (pwd.newPassword !== pwd.confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  if (!strongPassword(pwd.newPassword)) return ElMessage.warning('密码至少8位且必须包含字母和数字')
  if (!pwd.captchaKey || !pwd.captchaCode) return ElMessage.warning('请先输入验证码')
  try {
    await updatePasswordApi(pwd)
    ElMessage.success('密码修改成功')
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirmPassword = ''
    pwd.captchaCode = ''
  } finally {
    await refreshCaptcha()
  }
}

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

onMounted(async () => {
  await refreshCaptcha()
})
</script>

<style scoped>
.captcha-row { display: flex; gap: 8px; width: 100%; align-items: center; }
.captcha-img { width: 140px; height: 44px; border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; }
.captcha-tip { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
