<template>
  <div>
    <div class="top">
      <h2>工单进度跟踪</h2>
      <div>
        <el-button @click="router.push(`/repair-orders/${id}`)">查看详情</el-button>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>
    <el-card>
      <el-progress :percentage="order.progress || 0" :status="(order.status === '已完成' ? 'success' : undefined)" />
      <p>当前状态：<el-tag :type="statusTag(order.status)">{{ order.status }}</el-tag></p>
    </el-card>
    <el-card style="margin-top:12px">
      <template #header>状态时间轴（当前节点高亮）</template>
      <el-timeline>
        <el-timeline-item
          v-for="f in flows"
          :key="f.id"
          :timestamp="f.createTime"
          :type="f.toStatus === order.status ? 'primary' : ''"
          :hollow="f.toStatus !== order.status"
        >
          <div><b>{{ f.toStatus }}</b>（{{ f.action }}）</div>
          <div>{{ f.remark || '无备注' }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage } from '../../api'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const order = ref({})
const flows = ref([])

const statusTag = (s) => ({ '已完成':'success','已关闭':'info','已取消':'info','审核驳回':'danger','维修中':'primary','待验收/待确认':'warning' }[s] || 'warning')

const load = async () => {
  order.value = await getPage(`/repair-orders/${id}`)
  flows.value = await getPage(`/repair-orders/${id}/flows`)
}

onMounted(load)
</script>

<style scoped>
.top { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
</style>
