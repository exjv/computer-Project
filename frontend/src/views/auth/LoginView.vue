<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2 class="title">校园网络设备管理与故障报修系统</h2>
      <p class="subtitle">先选择角色，再进行账号认证</p>

      <el-steps :active="step" finish-status="success" simple>
        <el-step title="选择角色" />
        <el-step title="账号认证" />
      </el-steps>

      <div v-if="step === 0" class="role-panel">
        <p class="tip">请选择你要进入的系统身份：</p>
        <el-row :gutter="12">
          <el-col :span="8" v-for="item in roleOptions" :key="item.value">
            <el-card class="role-card" @click="selectRole(item.value)">
              <h4>{{ item.label }}</h4>
              <p>{{ item.desc }}</p>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div v-else class="form-panel">
        <el-alert :closable="false" type="info" show-icon style="margin-bottom: 12px">
          当前角色：{{ roleMap[form.role] }}
        </el-alert>

        <el-form :model="form" label-width="96px" @keyup.enter="onLogin">
          <el-form-item label="工号/用户名">
            <el-input v-model="form.account" placeholder="请输入工号或用户名" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" show-password type="password" placeholder="请输入密码" clearable />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="输入验证码" clearable />
              <img
                v-if="captcha.captchaImage"
                :src="captcha.captchaImage"
                class="captcha-img"
                @click="refreshCaptcha"
                alt="captcha" />
              <el-button v-else @click="refreshCaptcha">刷新验证码</el-button>
            </div>
            <div class="captcha-tip">点击验证码可刷新</div>
          </el-form-item>

          <el-button type="primary" style="width: 100%" :loading="submitting" @click="onLogin">登录</el-button>
          <el-button style="width: 100%; margin-top: 8px" @click="backToRole">返回角色选择</el-button>
        </el-form>

        <div class="oauth">
          <el-button link type="success" @click="onThirdParty('wechat')">微信登录（预留）</el-button>
          <el-button link type="primary" @click="onThirdParty('qq')">QQ 登录（预留）</el-button>
        </div>
      </div>

      <el-button link @click="router.push('/portal')">返回门户首页</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { captchaApi, oauthCallbackApi, oauthUrlApi } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const step = ref(0)
const submitting = ref(false)

const roleMap = { admin: '系统/业务管理员', maintainer: '维修人员', user: '报修用户' }
const roleOptions = [
  { value: 'admin', label: roleMap.admin, desc: '用户/角色/设备/工单审批分配/公告/统计/报表/日志' },
  { value: 'maintainer', label: roleMap.maintainer, desc: '查看分配工单、接拒单、更新进度、维修记录、延期/配件申请' },
  { value: 'user', label: roleMap.user, desc: '发起报修、跟踪进度、接收通知、确认维修结果并评价反馈' }
]

const form = reactive({ account: '', password: '', role: '', captchaCode: '', captchaKey: '' })
const captcha = reactive({ captchaImage: '' })

const selectRole = async role => {
  form.role = role
  step.value = 1
  await refreshCaptcha()
}

const backToRole = () => {
  step.value = 0
  form.password = ''
  form.captchaCode = ''
}

const refreshCaptcha = async () => {
  const data = await captchaApi()
  captcha.captchaImage = data.captchaImage
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

const onLogin = async () => {
  if (!form.role) return ElMessage.warning('请先选择角色')
  if (!form.account || !form.password || !form.captchaCode) return ElMessage.warning('请完整填写登录信息')
  submitting.value = true
  try {
    await store.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e?.message || '登录失败')
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

const onThirdParty = async provider => {
  const data = await oauthUrlApi(provider)
  const callback = await oauthCallbackApi(provider, 'demo-code')
  ElMessage.info(`${provider} 登录预留已触发：${callback.tip || callback.openId || 'placeholder'}`)
  console.log('OAuth placeholder authorizeUrl:', data.authorizeUrl)
}

onMounted(async () => {
  const preRole = route.query.role
  if (preRole && roleMap[preRole]) {
    await selectRole(preRole)
  }
})
</script>

<style scoped>
.login-page { display: flex; justify-content: center; padding-top: 60px; }
.login-card { width: 820px; }
.title { margin: 0 0 8px; }
.subtitle { margin: 0 0 16px; color: #909399; }
.role-panel { margin-top: 16px; }
.tip { color: #606266; }
.role-card { cursor: pointer; min-height: 140px; }
.role-card:hover { border-color: #409eff; }
.form-panel { margin-top: 16px; }
.captcha-row { display: flex; gap: 8px; width: 100%; align-items: center; }
.captcha-img { width: 140px; height: 44px; border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; }
.captcha-tip { font-size: 12px; color: #909399; margin-top: 4px; }
.oauth { margin-top: 10px; display: flex; justify-content: space-between; }
</style>
