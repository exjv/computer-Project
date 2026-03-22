<template>
  <div class="login-page">
    <el-row :gutter="16" style="width: 1100px">
      <el-col :span="14">
        <el-card class="intro-card">
          <h2>校园网络设备管理与故障报修系统</h2>
          <p>面向校园网络运维场景，支持报修、派单、维修协作、日志审计与统计分析。</p>
          <el-alert :closable="false" type="success" show-icon style="margin: 10px 0">
            所属单位：{{ home.campusInfo || 'XX大学网络与信息中心' }}
          </el-alert>
          <el-divider>公告信息</el-divider>
          <el-timeline>
            <el-timeline-item v-for="n in home.notices" :key="n.id" :timestamp="n.createTime">
              {{ n.title }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="login-card">
          <h2>登录入口</h2>
          <template v-if="step===1">
            <el-alert :closable="false" type="info" show-icon style="margin-bottom: 12px">
              第一步：请选择登录角色
            </el-alert>
            <div class="role-box">
              <el-button type="primary" @click="selectRole('admin')">系统/业务管理员</el-button>
              <el-button type="warning" @click="selectRole('maintainer')">维修人员</el-button>
              <el-button type="info" @click="selectRole('user')">报修用户</el-button>
            </div>
            <el-button link @click="router.push('/portal')">返回门户首页</el-button>
          </template>

          <template v-else>
            <el-alert :closable="false" type="info" show-icon style="margin-bottom: 12px">
              第二步：{{ roleMap[form.role] }}登录
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
            <el-button link @click="step=1">返回角色选择</el-button>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { captchaApi, oauthUrlApi, portalHomeApi } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const step = ref(1)
const home = reactive({ campusInfo: '', notices: [] })

const roleMap = { admin: '系统/业务管理员', maintainer: '维修人员', user: '报修用户' }
const form = reactive({ account: 'admin', password: '123456', role: 'admin', captchaCode: '', captchaKey: '' })
const captcha = reactive({ captchaCode: '' })

const refreshCaptcha = async () => {
  const data = await captchaApi()
  captcha.captchaCode = data.captchaCode
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

const selectRole = async (role) => {
  form.role = role
  step.value = 2
  await refreshCaptcha()
}

const onLogin = async () => {
  await store.login(form)
  ElMessage.success('登录成功')
  router.push('/')
}

const onThirdParty = async (provider) => {
  const data = await oauthUrlApi(provider)
  ElMessage.info(`${provider} 授权地址已生成：${data.authorizeUrl}`)
}

onMounted(async () => {
  Object.assign(home, await portalHomeApi())
  if (route.query.role && roleMap[route.query.role]) {
    await selectRole(route.query.role)
  }
})
</script>

<style scoped>
.login-page { display: flex; justify-content: center; padding-top: 40px; }
.intro-card { min-height: 650px; }
.login-card { min-height: 650px; }
.oauth { margin-top: 10px; display: flex; justify-content: space-between; }
.role-box { display:flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
</style>
