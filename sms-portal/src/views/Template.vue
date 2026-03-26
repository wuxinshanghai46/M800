<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">模板管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建模板</el-button>
    </div>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="模板名称 / 内容" clearable style="width: 220px;" />
        <el-select v-model="query.carrierStatus" placeholder="审核状态" clearable style="width: 130px;">
          <el-option label="审核中" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="未通过" value="rejected" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="list" stripe v-loading="loading" style="font-size: 13px;"
        :header-cell-style="headerStyle">
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="tplTypeTag(row.templateType)" effect="plain">{{ tplTypeLabel(row.templateType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reviewStatusType(row)" size="small">
              {{ reviewStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="color: #ff4d4f;">{{ row.rejectReason || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.carrierStatus === 'rejected' || row.platformStatus === 'rejected'" link type="primary" size="small"
              @click="openEditDialog(row)">修改重提</el-button>
            <el-button link type="danger" size="small"
              @click="handleDel(row)" :disabled="row.status === 'approved'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[20, 50]"
        layout="total, sizes, prev, pager, next" @change="loadData"
        style="margin-top: 16px; justify-content: flex-end;" />
    </el-card>

    <!-- 新建/编辑模板弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '修改模板' : '新建模板'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="form.templateType" style="width:100%;">
                <el-option label="OTP 验证码" value="OTP" />
                <el-option label="Transactional" value="TRANSACTIONAL" />
                <el-option label="Marketing 营销" value="MARKETING" />
                <el-option label="Service 服务" value="SERVICE" />
                <el-option label="动态模板" value="DYNAMIC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="短信类型">
              <el-select v-model="form.smsType" style="width:100%;">
                <el-option label="通知 / OTP" value="NOTIFICATION" />
                <el-option label="营销" value="MARKETING" />
                <el-option label="ALL（通用）" value="ALL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5"
            placeholder="请输入模板内容，动态变量用 {变量名} 表示，如：您的验证码为 {code}" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选，用途说明" />
        </el-form-item>
      </el-form>
      <div style="font-size: 12px; color: #999; padding: 0 20px;">
        提交后进入审核流程，审核通过后方可使用。审核时间通常为 1-3 个工作日。
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ form.id ? '重新提交' : '提交审核' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { templateList, templateSave, templateUpdate, templateDel } from '../api'

const loading = ref(false)
const saving  = ref(false)
const list    = ref([])
const total   = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 20, keyword: '', carrierStatus: '' })
const form  = reactive({ id: null, templateName: '', content: '', remark: '', templateType: 'OTP', smsType: 'NOTIFICATION' })
const rules = {
  templateName:  [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  content:       [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
  templateType:  [{ required: true, message: '请选择模板类型', trigger: 'change' }]
}

const tplTypeLabel = t => ({ OTP: 'OTP', TRANSACTIONAL: 'Transactional', MARKETING: 'Marketing', SERVICE: 'Service', DYNAMIC: '动态模板' }[t] || t || '—')
const tplTypeTag   = t => ({ OTP: 'danger', TRANSACTIONAL: 'primary', MARKETING: 'warning', SERVICE: 'info', DYNAMIC: 'success' }[t] || '')

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const statusType  = s => ({ approved: 'success', rejected: 'danger', pending: 'warning' }[s] || 'info')
const statusLabel = s => ({ approved: '已通过', rejected: '已拒绝', pending: '待审核' }[s] || s)
const carrierStatusType  = s => ({ approved: 'success', rejected: 'danger', pending: 'warning' }[s] || 'info')
const carrierStatusLabel = s => ({ approved: '已通过', rejected: '未通过', pending: '审核中' }[s] || '审核中')

// Combined display: platform rejected → 未通过; platform pending → 审核中; platform approved → show carrier status
const reviewStatusType = row => {
  if (row.platformStatus === 'rejected') return 'danger'
  if (row.platformStatus === 'pending' || !row.platformStatus) return 'warning'
  return carrierStatusType(row.carrierStatus)
}
const reviewStatusLabel = row => {
  if (row.platformStatus === 'rejected') return '未通过'
  if (row.platformStatus === 'pending' || !row.platformStatus) return '审核中'
  return carrierStatusLabel(row.carrierStatus)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await templateList({ page: query.page, size: query.size, keyword: query.keyword || undefined, carrierStatus: query.carrierStatus || undefined })
    list.value  = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { query.page = 1; loadData() }
const handleReset  = () => { Object.assign(query, { page: 1, keyword: '', carrierStatus: '' }); loadData() }

const openDialog = () => {
  Object.assign(form, { id: null, templateName: '', content: '', remark: '', templateType: 'OTP', smsType: 'NOTIFICATION' })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  Object.assign(form, { id: row.id, templateName: row.templateName, content: row.content, remark: row.remark || '', templateType: row.templateType || 'OTP', smsType: row.smsType || 'NOTIFICATION' })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await templateUpdate(form.id, { templateName: form.templateName, templateType: form.templateType, content: form.content })
      ElMessage.success('模板已重新提交审核')
    } else {
      await templateSave({ ...form })
      ElMessage.success('模板已提交审核')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleDel = async (row) => {
  await ElMessageBox.confirm(`确认删除模板「${row.templateName}」？`, '确认', { type: 'warning' })
  await templateDel(row.id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0; }
.filter-bar  { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
</style>
