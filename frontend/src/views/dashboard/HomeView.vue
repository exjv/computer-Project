<template>
  <div class="home-page">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card class="intro-card" shadow="hover">
          <h2>{{ workbench.systemName }}</h2>
          <p>业务首页聚焦工单协同、维修调度、设备治理与服务质量闭环。</p>
          <el-tag type="success">所属单位：{{ workbench.campusInfo }}</el-tag>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>快速入口</template>
          <div class="entry-grid">
            <el-button
              v-for="entry in filteredEntries"
              :key="entry.name"
              type="primary"
              plain
              @click="router.push(entry.path)">
              {{ entry.name }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>待办工单</template>
          <el-empty v-if="!todoItems.length" description="暂无待办"/>
          <div v-else class="todo-list">
            <div class="todo-item" v-for="item in todoItems" :key="item.key">
              <span>{{ item.label }}</span>
              <el-tag type="warning">{{ item.value }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>统计摘要</template>
          <el-empty v-if="!statItems.length" description="暂无统计"/>
          <div v-else class="todo-list">
            <div class="todo-item" v-for="item in statItems" :key="item.key">
              <span>{{ item.label }}</span>
              <el-tag type="success">{{ item.value }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>当前通知公告</template>
          <el-timeline>
            <el-timeline-item v-for="n in workbench.notices || []" :key="n.id" :timestamp="n.createTime">
              <el-card shadow="never">
                <h4>{{ n.title }}</h4>
                <div>{{ n.content }}</div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getPage } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const store = useUserStore()
const workbench = reactive({ systemName: '', campusInfo: '', notices: [], quickEntries: [], todo: {}, stats: {} })

const permissionMap = {
  '工单审批': 'repair:order:approve',
  '设备管理': 'device:manage',
  '用户管理': 'user:manage',
  '日志审计': 'log:operation:view',
  '我的待接单': 'repair:order:accept',
  '维修记录': 'repair:record:write',
  '设备档案': 'device:manage',
  '发起报修': 'repair:order:create',
  '我的工单': 'repair:order:view:self',
  '个人中心': ''
}

const filteredEntries = computed(() =>
  (workbench.quickEntries || []).filter(e => {
    const code = permissionMap[e.name]
    return !code || store.permissions.includes(code)
  })
)

const todoLabelMap = {
  pendingApprove: '待审批工单',
  pendingAssign: '待分配工单',
  pendingAccept: '待接单工单',
  processing: '处理中工单',
  myOpenOrders: '我的进行中工单',
  myPendingConfirm: '我的待验收工单'
}

const statLabelMap = {
  deviceTotal: '设备总数',
  deviceFault: '故障设备数',
  orderTotal: '工单总数',
  orderFinished: '已完成工单',
  myAccepted: '我已接单',
  myFinished: '我已完成',
  myReported: '我发起工单'
}

const todoItems = computed(() => Object.keys(workbench.todo || {}).map(key => ({ key, label: todoLabelMap[key] || key, value: workbench.todo[key] })))
const statItems = computed(() => Object.keys(workbench.stats || {}).map(key => ({ key, label: statLabelMap[key] || key, value: workbench.stats[key] })))

onMounted(async () => {
  Object.assign(workbench, await getPage('/portal/workbench', {}))
})
</script>

<style scoped>
.home-page { min-height: 100%; }
.intro-card h2 { margin: 0 0 8px; }
.entry-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.todo-list { display: flex; flex-direction: column; gap: 10px; }
.todo-item { display: flex; justify-content: space-between; align-items: center; }
</style>
