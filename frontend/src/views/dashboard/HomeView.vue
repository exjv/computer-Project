<template>
  <div class="home-page">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="hover" class="hero-card">
          <h2>{{ data.systemName }}</h2>
          <p class="desc">{{ data.systemDesc }}</p>
          <div class="meta">
            <el-tag type="success">{{ data.campusInfo }}</el-tag>
            <el-tag type="info">{{ data.networkStatus }}</el-tag>
          </div>
          <ul class="scenario-list">
            <li v-for="(s, idx) in data.scenarios || []" :key="idx">{{ s }}</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>校园网/所属单位信息</template>
          <p>{{ data.unitMeta?.campus }}</p>
          <p>{{ data.unitMeta?.servicePhone }}</p>
          <p>{{ data.unitMeta?.serviceTime }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>快速入口</template>
          <div class="entry-grid">
            <el-button
              v-for="entry in visibleEntries"
              :key="entry.path + entry.name"
              type="primary"
              plain
              @click="router.push(entry.path)">
              {{ entry.name }}
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>待办工单</template>
          <el-empty v-if="!todoItems.length" description="暂无待办" />
          <el-row v-else :gutter="12">
            <el-col :span="12" v-for="item in todoItems" :key="item.key">
              <div class="metric-card">
                <div class="label">{{ item.label }}</div>
                <div class="value">{{ item.value }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>统计摘要</template>
          <el-row :gutter="12">
            <el-col :span="6" v-for="item in statItems" :key="item.key">
              <div class="metric-card stat">
                <div class="label">{{ item.label }}</div>
                <div class="value">{{ item.value }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:16px">
      <template #header>当前通知公告</template>
      <el-empty v-if="!data.notices?.length" description="暂无公告" />
      <el-timeline v-else>
        <el-timeline-item v-for="n in data.notices" :key="n.id" :timestamp="n.publishTime || n.createTime">
          <el-card shadow="never">
            <h4>{{ n.title }}</h4>
            <p>{{ summary(n.content) }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { getPage } from '../../api'

const router = useRouter()
const store = useUserStore()

const data = reactive({
  systemName: '',
  systemDesc: '',
  campusInfo: '',
  networkStatus: '',
  unitMeta: {},
  scenarios: [],
  quickEntries: [],
  todo: {},
  stats: {},
  notices: []
})

const todoLabelMap = {
  pendingApprove: '待审核工单',
  pendingAssign: '待分配工单',
  processing: '处理中工单',
  pendingAccept: '待接单',
  inProgress: '维修中',
  pendingAcceptance: '待验收',
  myOpenOrders: '我的未完成工单',
  myPendingConfirm: '待我确认'
}

const statLabelMap = {
  deviceTotal: '设备总数',
  deviceFault: '故障设备数',
  orderTotal: '工单总数',
  orderFinished: '已完成工单',
  myAssigned: '分配给我',
  myFinished: '我已完成',
  myReported: '我发起的工单'
}

const visibleEntries = computed(() => {
  const entries = data.quickEntries || []
  return entries.filter(e => !e.perm || store.hasPerm(e.perm))
})

const todoItems = computed(() => Object.keys(data.todo || {}).map(key => ({
  key,
  label: todoLabelMap[key] || key,
  value: data.todo[key]
})))

const statItems = computed(() => Object.keys(data.stats || {}).map(key => ({
  key,
  label: statLabelMap[key] || key,
  value: data.stats[key]
})))

const summary = content => {
  const text = content || ''
  return text.length > 120 ? `${text.slice(0, 120)}...` : text
}

onMounted(async () => {
  const res = await getPage('/portal/workbench', {})
  Object.assign(data, res)
})
</script>

<style scoped>
.home-page { display: flex; flex-direction: column; gap: 12px; }
.hero-card .desc { color: #606266; line-height: 1.7; }
.meta { display: flex; gap: 8px; margin: 12px 0; }
.scenario-list { margin: 0; padding-left: 20px; color: #606266; display: grid; gap: 6px; }
.entry-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.metric-card { background: #f5f7fa; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.metric-card .label { color: #909399; font-size: 13px; }
.metric-card .value { color: #303133; font-size: 24px; font-weight: 700; margin-top: 6px; }
.metric-card.stat { min-height: 90px; }
</style>
