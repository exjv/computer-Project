<template>
  <div class="portal">
    <el-card>
      <h1>{{ home.systemName }}</h1>
      <p>{{ home.systemDesc }}</p>
      <el-tag type="success">{{ home.campusInfo }}</el-tag>
      <el-divider>适用场景</el-divider>
      <el-row :gutter="12">
        <el-col :span="12" v-for="(scene,idx) in home.scenes || []" :key="idx">
          <el-card shadow="never" style="margin-bottom: 8px">{{ scene }}</el-card>
        </el-col>
      </el-row>

      <el-divider>角色入口</el-divider>
      <div class="roles">
        <el-button type="primary" @click="toLogin('admin')">系统/业务管理员</el-button>
        <el-button type="warning" @click="toLogin('maintainer')">维修人员</el-button>
        <el-button type="info" @click="toLogin('user')">报修用户</el-button>
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
const home = reactive({ systemName: '', systemDesc: '', campusInfo: '', scenes: [], notices: [] })

const toLogin = (role) => router.push({ path: '/login', query: { role } })

onMounted(async () => {
  Object.assign(home, await portalHomeApi())
})
</script>

<style scoped>
.portal { max-width: 1100px; margin: 30px auto; }
.roles { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
