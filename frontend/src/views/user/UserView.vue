<template>
  <div>
    <h2>用户管理</h2>
    <el-form :inline="true" :model="query">
      <el-form-item label="工号"><el-input v-model="query.employeeNo" placeholder="按工号检索" /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.role" clearable placeholder="按角色筛选">
          <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字"><el-input v-model="query.keyword" placeholder="姓名/手机号/邮箱/部门" /></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openAdd">新增用户</el-button>
      <el-button type="success" @click="batchDialog = true">批量新增</el-button>
      <el-upload :show-file-list="false" :auto-upload="false" :on-change="onImportChange" accept=".xlsx">
        <el-button type="warning">Excel 批量导入</el-button>
      </el-upload>
    </div>

    <el-table :data="list" style="margin-top: 12px">
      <el-table-column prop="id" label="用户ID" width="90" />
      <el-table-column prop="employeeNo" label="工号" width="130" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column label="角色" min-width="140">
        <template #default="s">{{ (s.row.roles || []).join(', ') || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column prop="department" label="部门" width="120" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="s">{{ s.row.status === 1 ? '启用' : '禁用' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
      <el-table-column label="第三方绑定" min-width="140">
        <template #default="s">{{ (s.row.thirdPartyBinds || []).join(', ') || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="s">
          <el-button link @click="edit(s.row)">编辑</el-button>
          <el-button link @click="openAssign(s.row)">分配角色</el-button>
          <el-button link @click="changeStatus(s.row)">{{ s.row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button link @click="resetPwd(s.row)">重置密码</el-button>
          <el-button link type="danger" @click="remove(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" :page-size="page.size" @current-change="p => { page.current = p; load() }" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="工号"><el-input v-model="form.employeeNo" /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="默认角色"><el-select v-model="form.role"><el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" /></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" placeholder="默认123456Aa" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="分配角色" width="480px">
      <el-checkbox-group v-model="assign.roles">
        <el-checkbox v-for="r in roleOptions" :key="r.roleCode" :label="r.roleCode">{{ r.roleName }} ({{ r.roleCode }})</el-checkbox>
      </el-checkbox-group>
      <template #footer><el-button @click="assignDialog=false">取消</el-button><el-button type="primary" @click="saveAssign">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="batchDialog" title="批量新增用户" width="680px">
      <p>每行一个用户：工号,姓名,角色,手机号,邮箱,部门（用户名默认工号，密码默认123456Aa）</p>
      <el-input v-model="batchText" type="textarea" :rows="10" placeholder="例如：2026001,张三,admin,13800138000,zs@test.com,信息中心" />
      <template #footer><el-button @click="batchDialog=false">取消</el-button><el-button type="primary" @click="submitBatch">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="importResultDialog" title="导入结果" width="640px">
      <el-alert type="info" :closable="false" :title="`成功 ${importResult.successCount || 0} 条，失败 ${importResult.failCount || 0} 条`" />
      <el-table :data="(importResult.errors || []).map((e,i)=>({id:i+1,msg:e}))" style="margin-top:12px" max-height="300">
        <el-table-column prop="id" label="#" width="60"/>
        <el-table-column prop="msg" label="错误信息"/>
      </el-table>
    </el-dialog>

    <el-dialog v-model="assignRoleDialog" title="分配角色">
      <el-form :model="assignRoleForm"><el-form-item label="角色"><el-select v-model="assignRoleForm.role"><el-option label="管理员" value="admin"/><el-option label="普通用户" value="user"/><el-option label="维修人员" value="maintainer"/></el-select></el-form-item></el-form>
      <template #footer><el-button @click="assignRoleDialog=false">取消</el-button><el-button type="primary" @click="saveAssignRole">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="batchDialog" title="批量新增用户(JSON数组)">
      <el-input type="textarea" v-model="batchText" :rows="10" placeholder='[{"employeeNo":"U001","username":"u001","realName":"张三","role":"user","phone":"13800138000","department":"运维"}]'/>
      <template #footer><el-button @click="batchDialog=false">取消</el-button><el-button type="primary" @click="batchAdd">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { delApi, getPage, postApi, putApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = reactive({ employeeNo: '', role: '', keyword: '', status: null })
const page = reactive({ current: 1, size: 10 })
const list = ref([])
const total = ref(0)
const roleOptions = ref([])

const dialog = ref(false)
const form = reactive({})

const assignDialog = ref(false)
const assign = reactive({ userId: null, roles: [] })

const batchDialog = ref(false)
const batchText = ref('')

const importResultDialog = ref(false)
const importResult = reactive({ successCount: 0, failCount: 0, errors: [] })

const loadRoles = async () => {
  roleOptions.value = await getPage('/users/roles')
}

const load = async () => {
  const r = await getPage('/users/page', { ...query, ...page })
  list.value = r.records || []
  total.value = r.total || 0
}

const reset = () => {
  query.employeeNo = ''
  query.role = ''
  query.keyword = ''
  query.status = null
  page.current = 1
  load()
}

const openAdd = () => {
  Object.assign(form, { id: null, employeeNo: '', username: '', realName: '', role: 'user', phone: '', email: '', department: '', status: 1, password: '123456Aa' })
  dialog.value = true
}

const edit = row => {
  Object.assign(form, { ...row, role: (row.roles || [])[0] || 'user', password: '' })
  dialog.value = true
}

const save = async () => {
  if (form.id) await putApi(`/users/${form.id}`, form)
  else await postApi('/users', form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

const remove = async row => {
  await ElMessageBox.confirm(`确认删除用户【${row.realName}】吗？`, '删除确认')
  await delApi(`/users/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

const resetPwd = async row => {
  await ElMessageBox.confirm(`确认重置【${row.realName}】密码吗？`, '重置确认')
  await putApi(`/users/${row.id}/reset-password`, {})
  ElMessage.success('密码已重置为 123456Aa')
}

const changeStatus = async row => {
  await putApi(`/users/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态更新成功')
  load()
}

const openAssign = row => {
  assign.userId = row.id
  assign.roles = [...(row.roles || [])]
  assignDialog.value = true
}

const saveAssign = async () => {
  if (!assign.roles.length) return ElMessage.warning('请至少选择一个角色')
  await putApi(`/users/${assign.userId}/roles`, { roles: assign.roles })
  ElMessage.success('角色分配成功')
  assignDialog.value = false
  load()
}

const submitBatch = async () => {
  const lines = batchText.value.split('\n').map(v => v.trim()).filter(Boolean)
  if (!lines.length) return ElMessage.warning('请先输入批量数据')
  const data = lines.map(line => {
    const arr = line.split(',').map(v => v.trim())
    return {
      employeeNo: arr[0] || '',
      username: arr[0] || '',
      realName: arr[1] || '',
      role: arr[2] || 'user',
      phone: arr[3] || '',
      email: arr[4] || '',
      department: arr[5] || '',
      status: 1,
      password: '123456Aa'
    }
  })
  const result = await postApi('/users/batch', data)
  Object.assign(importResult, result)
  importResultDialog.value = true
  ElMessage.success('批量新增已处理')
  batchDialog.value = false
  batchText.value = ''
  load()
}

const onImportChange = async uploadFile => {
  const fd = new FormData()
  fd.append('file', uploadFile.raw)
  const result = await postApi('/users/import', fd)
  Object.assign(importResult, result)
  importResultDialog.value = true
  ElMessage.success('导入完成')
  load()
}

onMounted(async () => {
  await loadRoles()
  await load()
})
</script>

<style scoped>
.toolbar { margin-top: 8px; display: flex; gap: 8px; align-items: center; }
</style>
