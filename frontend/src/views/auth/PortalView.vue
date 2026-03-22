<template>
  <div class="portal">
    <el-card>
      <h1>{{ home.systemName }}</h1>
      <p>{{ home.systemDesc }}</p>
      <el-tag type="success">{{ home.campusInfo }}</el-tag>

      <el-divider>角色入口</el-divider>
      <div class="roles">
        <el-button type="primary" @click="toLogin()">进入企业级登录门户</el-button>
      </div>

      <el-divider>通知公告</el-divider>
      <el-timeline>
        <el-timeline-item v-for="n in home.notices" :key="n.id" :timestamp="n.createTime">
          <el-card shadow="never">
            <h4>{{ n.title }}</h4>
            <div>{{ n.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { portalHomeApi } from '../../api'

const router = useRouter()
const home = reactive({ systemName: '', systemDesc: '', campusInfo: '', notices: [] })

const toLogin = () => router.push({ path: '/login' })

onMounted(async () => {
  Object.assign(home, await portalHomeApi())
})
</script>

<style scoped>
.portal { max-width: 1100px; margin: 30px auto; }
.roles { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
