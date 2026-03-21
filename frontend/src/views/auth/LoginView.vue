<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>角色登录</h2>
      <el-alert :closable="false" type="info" show-icon style="margin-bottom: 12px">
        当前角色：{{ roleMap[form.role] }}
      </el-alert>
      <el-form :model="form" label-width="92px">
        <el-form-item label="工号/用户名">
          <el-input v-model="form.account" placeholder="请输入工号或用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="验证码">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input v-model="form.captchaCode" placeholder="输入验证码" />
            <el-button @click="refreshCaptcha">{{ captcha.captchaCode || '加载中' }}</el-button>
          </div>
        </el-form-item>
        <el-button type="primary" style="width: 100%" @click="onLogin">登录</el-button>
      </el-form>
      <div class="oauth">
        <el-button link type="success" @click="onThirdParty('wechat')">微信登录（预留）</el-button>
        <el-button link type="primary" @click="onThirdParty('qq')">QQ 登录（预留）</el-button>
      </div>
      <el-button link @click="router.push('/portal')">返回门户首页</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { captchaApi, oauthUrlApi } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const roleMap = { admin: '系统/业务管理员', maintainer: '维修人员', user: '报修用户' }
const form = reactive({ account: 'admin', password: '123456', role: 'admin', captchaCode: '', captchaKey: '' })
const captcha = reactive({ captchaCode: '' })

const refreshCaptcha = async () => {
  const data = await captchaApi()
  captcha.captchaCode = data.captchaCode
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

const onLogin = async () => {
  await store.login(form)
  router.push('/')
}

const onThirdParty = async (provider) => {
  const data = await oauthUrlApi(provider)
  ElMessage.info(`${provider} 授权地址已生成：${data.authorizeUrl}`)
}

onMounted(async () => {
  if (route.query.role && roleMap[route.query.role]) form.role = route.query.role
  await refreshCaptcha()
})
</script>

<style scoped>
.login-page { display: flex; justify-content: center; padding-top: 80px; }
.login-card { width: 460px; }
.oauth { margin-top: 10px; display: flex; justify-content: space-between; }
</style>
