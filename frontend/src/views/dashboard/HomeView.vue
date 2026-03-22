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

    <el-row :gutter="16" style="margin-top:16px" v-if="isAdmin">
      <el-col :span="8"><el-card>设备总数<div class="num">{{ deviceStats.total || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card>正常设备数<div class="num">{{ deviceStats.normal || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card>故障设备数<div class="num">{{ deviceStats.fault || 0 }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px" v-if="isAdmin">
      <el-col :span="8"><el-card><div class="chart-title">设备状态分布</div><div ref="deviceChartRef" class="chart"/></el-card></el-col>
      <el-col :span="8"><el-card><div class="chart-title">工单趋势</div><div ref="trendChartRef" class="chart"/></el-card></el-col>
      <el-col :span="8"><el-card><div class="chart-title">完成率统计</div><div ref="rateChartRef" class="chart"/></el-card></el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>
        <div class="notice-head">
          <span>公告</span>
          <span class="notice-tip">公告数据实时来自后端</span>
          <el-select v-if="isAdmin" v-model="noticeQuery.status" size="small" style="width:120px" @change="loadNotices">
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="上线" value="ONLINE" />
            <el-option label="下线" value="OFFLINE" />
          </el-select>
          <el-select v-model="noticeQuery.sortBy" size="small" style="width:140px" @change="loadNotices">
            <el-option label="按发布时间" value="publishTime" />
            <el-option label="按创建时间" value="createTime" />
          </el-select>
          <el-button v-if="isAdmin" type="primary" size="small" @click="openAdd">发布公告</el-button>
        </div>
      </template>
      <el-empty v-if="!notices.length" description="暂无公告" />
      <el-table v-else :data="notices">
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="摘要" min-width="300">
          <template #default="s">{{ summary(s.row.content) }}</template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="260">
          <template #default="s">
            <el-button link @click="viewNotice(s.row)">详情</el-button>
            <template v-if="isAdmin">
              <el-button link @click="editNotice(s.row)">编辑</el-button>
              <el-button link @click="switchStatus(s.row)">{{ s.row.status === 'ONLINE' ? '下线' : '上线' }}</el-button>
              <el-button link type="danger" @click="deleteNotice(s.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailDialog" title="公告详情" width="700px">
      <h3>{{ detail.title }}</h3>
      <p style="color:#909399">状态：{{ detail.status }} ｜ 发布时间：{{ detail.publishTime || '-' }}</p>
      <div class="notice-content">{{ detail.content }}</div>
    </el-dialog>

    <el-dialog v-model="editDialog" :title="noticeForm.id ? '编辑公告' : '发布公告'" width="700px">
      <el-form :model="noticeForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="noticeForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="7" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="noticeForm.status">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="上线" value="ONLINE" />
            <el-option label="下线" value="OFFLINE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog=false">取消</el-button>
        <el-button type="primary" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref, nextTick, computed } from 'vue'
import { delApi, getPage, postApi, putApi } from '../../api'
import { useUserStore } from '../../stores/user'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'

const user = useUserStore()
const isAdmin = computed(() => user.userInfo.role === 'admin')
const orderStats = reactive({})
const deviceStats = reactive({})
const notices = ref([])
const noticeQuery = reactive({ status: "", sortBy: "publishTime" })
const deviceChartRef = ref()
const trendChartRef = ref()
const rateChartRef = ref()

const detailDialog = ref(false)
const detail = reactive({})

const editDialog = ref(false)
const noticeForm = reactive({ id: null, title: '', content: '', status: 'DRAFT' })

const renderCharts = () => {
  const d = echarts.init(deviceChartRef.value)
  d.setOption({ tooltip: {}, series: [{ type: 'pie', data: [{ name: '正常', value: deviceStats.normal || 0 }, { name: '故障', value: deviceStats.fault || 0 }], radius: '65%' }] })
  const t = echarts.init(trendChartRef.value)
  t.setOption({ xAxis: { type: 'category', data: ['待处理', '处理中', '已完成'] }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: [orderStats.pending || 0, orderStats.processing || 0, orderStats.finished || 0] }] })
  const r = echarts.init(rateChartRef.value)
  const finishRate = (orderStats.total ? ((orderStats.finished || 0) / orderStats.total * 100) : 0).toFixed(1)
  r.setOption({ series: [{ type: 'gauge', center: ['50%', '65%'], radius: '78%', min: 0, max: 100, axisLine: { lineStyle: { width: 14 } }, progress: { show: true, width: 14 }, splitLine: { length: 10, distance: -16 }, axisTick: { distance: -18, length: 5 }, axisLabel: { distance: 16, fontSize: 10 }, title: { offsetCenter: [0, '45%'], fontSize: 14 }, detail: { fontSize: 24, offsetCenter: [0, '68%'], formatter: v => `${v}%` }, data: [{ value: Number(finishRate), name: '完成率' }] }] })
}

const loadNotices = async () => {
  if (isAdmin.value) {
    const r = await getPage('/notices/page', { current: 1, size: 10, sortBy: noticeQuery.sortBy, status: noticeQuery.status || undefined })
    notices.value = r.records || []
    return
  }
  notices.value = await getPage('/notices/home', { limit: 8 })
}

const summary = content => (content || '').slice(0, 70) + ((content || '').length > 70 ? '...' : '')
const viewNotice = row => {
  Object.assign(detail, row)
  detailDialog.value = true
}

const openAdd = () => {
  Object.assign(noticeForm, { id: null, title: '', content: '', status: 'DRAFT' })
  editDialog.value = true
}

const editNotice = row => {
  Object.assign(noticeForm, { ...row })
  editDialog.value = true
}

const saveNotice = async () => {
  if (noticeForm.id) await putApi(`/notices/${noticeForm.id}`, noticeForm)
  else await postApi('/notices', noticeForm)
  ElMessage.success('公告保存成功')
  editDialog.value = false
  await loadNotices()
}

const switchStatus = async row => {
  const next = row.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'
  await putApi(`/notices/${row.id}/status`, { status: next })
  ElMessage.success('公告状态已更新')
  await loadNotices()
}

const deleteNotice = async row => {
  await ElMessageBox.confirm('确认删除该公告吗？', '删除确认')
  await delApi(`/notices/${row.id}`)
  ElMessage.success('公告已删除')
  await loadNotices()
}

const editNotice = row => {
  Object.assign(noticeForm, { ...row })
  editDialog.value = true
}

const saveNotice = async () => {
  if (noticeForm.id) await putApi(`/notices/${noticeForm.id}`, noticeForm)
  else await postApi('/notices', noticeForm)
  ElMessage.success('公告保存成功')
  editDialog.value = false
  await loadNotices()
}

const switchStatus = async row => {
  const next = row.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'
  await putApi(`/notices/${row.id}/status`, { status: next })
  ElMessage.success('公告状态已更新')
  await loadNotices()
}

const deleteNotice = async row => {
  await ElMessageBox.confirm('确认删除该公告吗？', '删除确认')
  await delApi(`/notices/${row.id}`)
  ElMessage.success('公告已删除')
  await loadNotices()
}

const deleteNotice = async row => {
  await ElMessageBox.confirm('确认删除该公告吗？', '删除确认')
  await delApi(`/notices/${row.id}`)
  ElMessage.success('公告已删除')
  await loadNotices()
}

const switchStatus = async row => {
  const next = row.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'
  await putApi(`/notices/${row.id}/status`, { status: next })
  ElMessage.success('公告状态已更新')
  await loadNotices()
}

const deleteNotice = async row => {
  await ElMessageBox.confirm('确认删除该公告吗？', '删除确认')
  await delApi(`/notices/${row.id}`)
  ElMessage.success('公告已删除')
  await loadNotices()
}

const todoItems = computed(() => Object.keys(workbench.todo || {}).map(key => ({ key, label: todoLabelMap[key] || key, value: workbench.todo[key] })))
const statItems = computed(() => Object.keys(workbench.stats || {}).map(key => ({ key, label: statLabelMap[key] || key, value: workbench.stats[key] })))

onMounted(async () => {
  Object.assign(orderStats, await getPage('/repair-orders/statistics', {}))
  await loadNotices()
  if (isAdmin.value) {
    Object.assign(deviceStats, await getPage('/devices/statistics', {}))
    await nextTick(); renderCharts()
  }
})
</script>

<style scoped>
.num { font-size: 28px; font-weight: 700; color: #409eff; margin-top: 8px }
.chart { height: 220px }
.chart-title { font-weight: 600; margin-bottom: 8px }
.notice-head { display:flex; align-items:center; gap:12px }
.notice-tip { color:#909399; font-size:12px; flex:1 }
.notice-content { white-space: pre-wrap; line-height: 1.8; }
</style>
