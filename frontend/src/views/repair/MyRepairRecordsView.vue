<template>
  <div>
    <h2>我的报修记录</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工单编号"><el-input v-model="query.orderNo"/></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable><el-option v-for="s in statusList" :key="s" :label="s" :value="s"/></el-select></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>
    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="orderNo" label="工单编号" width="180"/>
      <el-table-column prop="title" label="故障标题"/>
      <el-table-column prop="status" label="状态" width="160"/>
      <el-table-column prop="reportTime" label="报修时间" width="180"/>
      <el-table-column label="操作" width="320">
        <template #default="s">
          <el-button link @click="goProgress(s.row)">进度跟踪</el-button>
          <el-button v-if="canCancel(s.row)" link type="danger" @click="cancelOrder(s.row)">撤销报修</el-button>
          <el-button v-if="s.row.status==='待验收/待确认'" link type="primary" @click="openAcceptance(s.row)">验收确认</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" @current-change="p=>{page.current=p;load()}"/>

    <el-dialog v-model="acceptDialog" title="验收确认/满意度评价" width="520px">
      <el-form :model="acceptForm" label-width="110px">
        <el-form-item label="确认结果">
          <el-radio-group v-model="acceptForm.action">
            <el-radio label="USER_CONFIRM_RESOLVED">已解决</el-radio>
            <el-radio label="USER_CONFIRM_UNRESOLVED">未解决（退回处理）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="满意度评分">
          <el-rate v-model="acceptForm.satisfactionScore" :max="5"/>
        </el-form-item>
        <el-form-item label="反馈意见">
          <el-input type="textarea" v-model="acceptForm.feedbackContent"/>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input type="textarea" v-model="acceptForm.remark"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptDialog=false">取消</el-button>
        <el-button type="primary" @click="submitAcceptance">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPage, putApi } from '../../api'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const statusList = ['已提交/待审核','审核通过','审核驳回','待分配','待接单','维修人员已接单','维修中','待验收/待确认','已完成','已关闭','已取消']
const query = reactive({ orderNo: '', status: '' })
const page = reactive({ current: 1, size: 10 })
const list = ref([])
const total = ref(0)

const acceptDialog = ref(false)
const acceptForm = reactive({
  id: null,
  action: 'USER_CONFIRM_RESOLVED',
  satisfactionScore: 5,
  feedbackContent: '',
  remark: ''
})

const load = async () => {
  const r = await getPage('/repair-orders/my', { ...query, ...page })
  list.value = r.records || []
  total.value = r.total || 0
}

const canCancel = row => ['已提交/待审核', '审核驳回'].includes(row.status)

const cancelOrder = async row => {
  await ElMessageBox.confirm('确认撤销该报修工单吗？', '提示', { type: 'warning' })
  await putApi(`/repair-orders/${row.id}/action`, { action: 'USER_CANCEL', remark: '用户主动撤销' })
  ElMessage.success('撤销成功')
  await load()
}

const goProgress = row => router.push(`/my-repairs/${row.id}/progress`)

const openAcceptance = row => {
  Object.assign(acceptForm, {
    id: row.id,
    action: 'USER_CONFIRM_RESOLVED',
    satisfactionScore: 5,
    feedbackContent: '',
    remark: ''
  })
  acceptDialog.value = true
}

const submitAcceptance = async () => {
  await putApi(`/repair-orders/${acceptForm.id}/action`, {
    action: acceptForm.action,
    satisfactionScore: acceptForm.satisfactionScore,
    feedbackContent: acceptForm.feedbackContent,
    remark: acceptForm.remark
  })
  ElMessage.success('提交成功')
  acceptDialog.value = false
  await load()
}

onMounted(load)
</script>
