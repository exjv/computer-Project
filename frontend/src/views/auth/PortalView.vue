<template>
  <div class="portal-page">
    <section class="hero">
      <div class="hero-left">
        <h1>{{ home.systemName }}</h1>
        <p class="desc">{{ home.systemDesc }}</p>
        <div class="meta-list">
          <el-tag type="success">{{ home.campusInfo }}</el-tag>
          <el-tag type="info">{{ home.networkStatus }}</el-tag>
        </div>
        <div class="action-group">
          <el-button type="primary" size="large" @click="goLogin()">进入系统登录</el-button>
          <el-button size="large" @click="scrollToNotice">查看最新公告</el-button>
        </div>
      </div>
      <div class="hero-right">
        <el-card shadow="never" class="unit-card">
          <template #header>校园网服务信息</template>
          <p>{{ home.unitMeta?.campus }}</p>
          <p>{{ home.unitMeta?.servicePhone }}</p>
          <p>{{ home.unitMeta?.serviceTime }}</p>
        </el-card>
      </div>
    </section>

    <section class="section-box">
      <h3>适用场景</h3>
      <el-row :gutter="12">
        <el-col :span="12" v-for="(s, idx) in home.scenarios || []" :key="idx">
          <el-card shadow="hover" class="scenario-item">{{ s }}</el-card>
        </el-col>
      </el-row>
    </section>

    <section class="section-box">
      <h3>登录入口区域</h3>
      <p class="hint">请先选择角色，再进入对应认证入口。</p>
      <div class="role-entry">
        <el-button type="primary" @click="goLogin('admin')">系统/业务管理员入口</el-button>
        <el-button type="warning" @click="goLogin('maintainer')">维修人员入口</el-button>
        <el-button type="info" @click="goLogin('user')">报修用户入口</el-button>
      </div>
      <div class="oauth-entry">
        <el-button link type="success" @click="oauth('wechat')">微信登录入口（预留）</el-button>
        <el-button link type="primary" @click="oauth('qq')">QQ 登录入口（预留）</el-button>
      </div>
    </section>

    <section class="section-box" ref="noticeRef">
      <h3>校园公告</h3>
      <el-empty v-if="!home.notices?.length" description="暂无公告" />
      <el-timeline v-else>
        <el-timeline-item v-for="n in home.notices" :key="n.id" :timestamp="n.createTime">
          <el-card shadow="never">
            <h4>{{ n.title }}</h4>
            <div>{{ n.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { oauthCallbackApi, oauthUrlApi, portalHomeApi } from '../../api'

const router = useRouter()
const noticeRef = ref()
const home = reactive({
  systemName: '',
  systemDesc: '',
  campusInfo: '',
  networkStatus: '',
  scenarios: [],
  unitMeta: {},
  notices: []
})

const goLogin = (role) => {
  if (role) return router.push({ path: '/login', query: { role } })
  return router.push({ path: '/login' })
}

const oauth = async (provider) => {
  const data = await oauthUrlApi(provider)
  const callback = await oauthCallbackApi(provider, 'demo-code')
  ElMessage.info(`${provider} 登录预留已触发：${callback.tip}`)
  console.log('OAuth placeholder authorizeUrl:', data.authorizeUrl)
}

const scrollToNotice = () => noticeRef.value?.scrollIntoView({ behavior: 'smooth' })

onMounted(async () => {
  Object.assign(home, await portalHomeApi())
})
</script>

<style scoped>
.portal-page { max-width: 1200px; margin: 24px auto; display: flex; flex-direction: column; gap: 16px; }
.hero { display: flex; gap: 16px; background: linear-gradient(135deg, #f0f6ff, #f5f7fa); border-radius: 12px; padding: 24px; }
.hero-left { flex: 2; }
.hero-right { flex: 1; }
.desc { color: #606266; line-height: 1.7; }
.meta-list { display: flex; gap: 8px; margin: 12px 0; }
.action-group { display: flex; gap: 10px; margin-top: 12px; }
.section-box { background: #fff; border-radius: 12px; padding: 18px; box-shadow: 0 2px 10px rgba(0,0,0,.04); }
.scenario-item { margin-bottom: 10px; }
.role-entry { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 8px; }
.oauth-entry { margin-top: 8px; display: flex; gap: 14px; }
.hint { color: #909399; }
.unit-card p { margin: 8px 0; color: #606266; }
</style>
