<template>
  <div>
    <el-page-header @back="router.push('/devices')" content="设备详情" />
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="8"><el-card><template #header>基本信息</template>
        <div>设备编号：{{ d.deviceCode }}</div><div>设备名称：{{ d.deviceName }}</div><div>设备类型：{{ d.deviceType }}</div>
        <div>品牌/型号：{{ d.brand }}/{{ d.model }}</div><div>序列号：{{ d.serialNumber }}</div><div>责任人：{{ d.ownerName }}</div>
        <div>管理部门：{{ d.manageDepartment }}</div><div>位置：{{ d.campus }} {{ d.buildingLocation }}</div>
      </el-card></el-col>
      <el-col :span="8"><el-card><template #header>状态与保修</template>
        <div>当前状态：<el-tag :type="statusType(detail.currentStatus)">{{ detail.currentStatus || '-' }}</el-tag></div>
        <div style="margin-top:8px">保修截止：{{ d.warrantyExpiryDate || '-' }}</div>
        <div>是否在保修期：<el-tag :type="detail.inWarranty?'success':'danger'">{{ detail.inWarranty ? '是' : '否' }}</el-tag></div>
        <div>高故障预警：<el-tag :type="detail.highFaultWarning?'danger':'success'">{{ detail.highFaultWarning?'触发':'未触发' }}</el-tag></div>
        <div style="color:#999">阈值：{{ detail.highFaultThreshold }}</div>
      </el-card></el-col>
      <el-col :span="8"><el-card><template #header>处置建议</template>
        <div>建议：<el-tag :type="detail.suggestReplace?'danger':(detail.suggestInspect?'warning':'success')">{{ detail.recommendation || '-' }}</el-tag></div>
        <div style="margin-top:8px">历史维修次数：{{ detail.historyRepairCount || 0 }}</div>
        <div>最近维修记录：{{ detail.recentRepairRecord?.faultReason || '-' }}</div>
      </el-card></el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="6"><el-card><div>累计工单</div><h2>{{ detail.stats?.totalOrders || 0 }}</h2></el-card></el-col>
      <el-col :span="6"><el-card><div>活跃工单</div><h2>{{ detail.stats?.activeOrders || 0 }}</h2></el-card></el-col>
      <el-col :span="6"><el-card><div>近90天故障</div><h2>{{ detail.stats?.recent90dFaults || 0 }}</h2></el-card></el-col>
      <el-col :span="6"><el-card><div>累计维修</div><h2>{{ detail.stats?.totalRepairs || 0 }}</h2></el-card></el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="12"><el-card><template #header>故障原因统计</template>
        <el-empty v-if="!Object.keys(detail.faultReasonStats||{}).length" description="暂无故障原因"/>
        <el-tag v-for="(v,k) in detail.faultReasonStats || {}" :key="k" style="margin-right:8px">{{ k }}: {{ v }}</el-tag>
      </el-card></el-col>
      <el-col :span="12"><el-card><template #header>维修照片</template>
        <el-empty v-if="!(detail.repairPhotos||[]).length" description="暂无维修照片"/>
        <el-image v-for="(url,i) in detail.repairPhotos||[]" :key="i" :src="url" :preview-src-list="detail.repairPhotos" style="width:90px;height:90px;margin-right:8px" fit="cover" />
      </el-card></el-col>
    </el-row>

    <el-card style="margin-top:12px"><template #header>维修时间线</template>
      <el-timeline>
        <el-timeline-item v-for="(t,i) in detail.repairTimeline || []" :key="i" :timestamp="t.time">
          工单#{{ t.orderId }}：{{ t.fromStatus || '开始' }} → {{ t.toStatus }}（{{ t.action }}）{{ t.remark ? ' | '+t.remark : '' }}
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card style="margin-top:12px"><template #header>关联工单列表</template>
      <el-table :data="detail.relatedOrders || []">
        <el-table-column prop="orderNo" label="工单号" width="180"/>
        <el-table-column prop="title" label="标题" min-width="180"/>
        <el-table-column prop="priority" label="优先级" width="90"/>
        <el-table-column prop="status" label="状态" width="120"/>
        <el-table-column prop="reportTime" label="报修时间" width="180"/>
        <el-table-column prop="finishTime" label="完成时间" width="180"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPage } from '../../api'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const d = computed(() => detail.value.device || {})

const statusType = (s) => {
  if (s === '正常') return 'success'
  if (s === '维修中') return 'warning'
  if (s === '报废') return 'danger'
  return 'info'
}

onMounted(async () => {
  detail.value = await getPage(`/devices/${route.params.id}/detail`)
})
</script>
