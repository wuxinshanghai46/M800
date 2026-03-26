<template>
  <div class="template-page">
    <!-- Page Header -->
    <div class="page-header">
      <span class="page-title">模板管理</span>
      <div class="page-actions">
        <el-button type="primary" @click="handleCreate">+ 新建模板</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab">
      <!-- Tab 1: 模板列表 -->
      <el-tab-pane label="模板列表" name="list">
        <el-card shadow="never">
          <div class="filter-bar">
            <el-select v-model="filters.customer" placeholder="全部客户" clearable filterable style="width: 180px">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
            <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 180px">
              <el-option label="OTP" value="OTP" />
              <el-option label="Transactional" value="TRANSACTIONAL" />
              <el-option label="Marketing" value="MARKETING" />
              <el-option label="Service" value="SERVICE" />
              <el-option label="动态模板" value="DYNAMIC" />
            </el-select>
            <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 180px">
              <el-option label="Draft" value="Draft" />
              <el-option label="Pending" value="Pending" />
              <el-option label="Approved" value="Approved" />
              <el-option label="Rejected" value="Rejected" />
              <el-option label="Disabled" value="Disabled" />
            </el-select>
            <el-input v-model="filters.keyword" placeholder="模板编号/名称关键字" clearable style="width: 200px" />
            <el-button type="primary" size="small" @click="handleSearch">查询</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
          </div>

          <el-table :data="templateList" :header-cell-style="headerCellStyle" style="font-size: 13px">
            <el-table-column prop="code" label="模板编号" min-width="150" />
            <el-table-column prop="name" label="模板名称" min-width="140" />
            <el-table-column prop="customer" label="客户" min-width="100" />
            <el-table-column label="类型" min-width="120">
              <template #default="{ row }">
                <el-tag :type="typeTagMap[row.type]" size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="language" label="语言" width="70" />
            <el-table-column prop="encoding" label="编码" width="80" />
            <el-table-column prop="segments" label="段数" width="60" />
            <el-table-column label="平台审核" min-width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagMap[row.platformStatus || row.status]" size="small">
                  {{ platformStatusLabel[row.platformStatus || row.status] || row.platformStatus || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="运营商审核" min-width="100">
              <template #default="{ row }">
                <el-tag :type="carrierStatusTagMap[row.carrierStatus || 'pending']" size="small">
                  {{ carrierStatusLabel[row.carrierStatus || 'pending'] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="150" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openViewDialog(row)">查看</el-button>
                <el-button v-if="canReview" link type="primary" size="small" @click="handleAuditFromList(row)">平台审核</el-button>
                <el-button v-if="canReview" link type="warning" size="small" @click="openCarrierAuditDialog(row)">运营商审核</el-button>
                <el-button link type="danger" size="small" @click="handleDisable(row)">禁用</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="pagination.page"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              layout="total, prev, pager, next"
              background
              small
            />
          </div>
        </el-card>
      </el-tab-pane>


      <!-- Tab 3: 模板统计 -->
      <el-tab-pane label="模板统计" name="stats">
        <div class="stat-cards">
          <el-card shadow="never" class="stat-card">
            <div class="stat-title">模板总数</div>
            <div class="stat-value">328</div>
            <div class="stat-footer"><span class="up">+12</span> 较上月</div>
          </el-card>
          <el-card shadow="never" class="stat-card">
            <div class="stat-title">已审核</div>
            <div class="stat-value" style="color: #52c41a">285</div>
            <div class="stat-footer">审核通过率 86.9%</div>
          </el-card>
          <el-card shadow="never" class="stat-card">
            <div class="stat-title">待审核</div>
            <div class="stat-value" style="color: #faad14">18</div>
            <div class="stat-footer">平均审核耗时 2.3h</div>
          </el-card>
          <el-card shadow="never" class="stat-card">
            <div class="stat-title">拒绝率</div>
            <div class="stat-value" style="color: #ff4d4f">7.6%</div>
            <div class="stat-footer"><span class="down">+1.2%</span> 较上月</div>
          </el-card>
        </div>

        <div class="charts-grid">
          <el-card shadow="never">
            <template #header><span style="font-weight: 600">按类型分布</span></template>
            <div class="chart-placeholder">[ 饼图 - OTP: 42% | Transactional: 31% | Marketing: 18% | Service: 9% ]</div>
          </el-card>
          <el-card shadow="never">
            <template #header><span style="font-weight: 600">使用量排行 Top10</span></template>
            <div class="chart-placeholder">[ 柱状图 - 模板使用量排行，按发送次数降序 ]</div>
          </el-card>
          <el-card shadow="never" class="chart-full">
            <template #header><span style="font-weight: 600">审核趋势（近30天）</span></template>
            <div class="chart-placeholder">[ 折线图 - 每日提交数 / 通过数 / 拒绝数 趋势 ]</div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- Tab 4: DLT管理 -->
      <el-tab-pane label="DLT管理" name="dlt">
        <el-alert
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        >
          <template #title>
            DLT (Distributed Ledger Technology) 注册仅适用于印度市场。根据TRAI规定，所有发往印度的商业短信必须完成DLT注册并关联DLT模板ID。
          </template>
        </el-alert>

        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header><span style="font-weight: 600">Entity 列表</span></template>
          <el-table :data="dltEntityList" :header-cell-style="headerCellStyle" style="font-size: 13px">
            <el-table-column prop="customer" label="客户" width="120" />
            <el-table-column prop="entityId" label="Entity ID" min-width="180" />
            <el-table-column prop="entityName" label="Entity 名称" min-width="200" />
            <el-table-column label="注册状态" width="110">
              <template #default="{ row }">
                <el-tag :type="dltStatusTagMap[row.regStatus]" size="small">{{ row.regStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="regTime" label="注册时间" width="130" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="row.regStatus === '未注册'">
                  <el-button link type="primary" size="small" @click="handleDltRegister(row)">注册</el-button>
                </template>
                <template v-else>
                  <el-button link type="primary" size="small" @click="handleDltEdit(row)">编辑</el-button>
                  <el-button link type="primary" size="small" @click="handleDltViewTemplates(row)">查看模板</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header><span style="font-weight: 600">DLT 模板关联</span></template>
          <el-table :data="dltTemplateList" :header-cell-style="headerCellStyle" style="font-size: 13px">
            <el-table-column prop="code" label="模板编号" width="160" />
            <el-table-column prop="name" label="模板名称" min-width="140" />
            <el-table-column prop="customer" label="客户" width="110" />
            <el-table-column prop="dltTemplateId" label="DLT Template ID" min-width="180" />
            <el-table-column label="DLT 状态" width="110">
              <template #default="{ row }">
                <el-tag :type="dltStatusTagMap[row.dltStatus]" size="small">{{ row.dltStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <template v-if="row.dltStatus === '未关联'">
                  <el-button link type="primary" size="small" @click="handleDltLink(row)">关联DLT</el-button>
                </template>
                <template v-else>
                  <el-button link type="primary" size="small" @click="handleDltEditLink(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDltUnlink(row)">解绑</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- View Dialog -->
    <el-dialog v-model="viewDialogVisible" title="模板详情" width="680px" destroy-on-close>
      <template #header>
        <span style="font-size: 16px; font-weight: 600">模板详情 - {{ viewData.code }}</span>
      </template>
      <div class="detail-rows">
        <div class="detail-row"><div class="detail-label">模板编号</div><div class="detail-value">{{ viewData.code }}</div></div>
        <div class="detail-row"><div class="detail-label">模板名称</div><div class="detail-value">{{ viewData.name }}</div></div>
        <div class="detail-row"><div class="detail-label">客户</div><div class="detail-value">{{ viewData.customer }}</div></div>
        <div class="detail-row">
          <div class="detail-label">类型</div>
          <div class="detail-value"><el-tag :type="typeTagMap[viewData.type]" size="small">{{ viewData.type }}</el-tag></div>
        </div>
        <div class="detail-row"><div class="detail-label">语言 / 编码</div><div class="detail-value">{{ viewData.language }} / {{ viewData.encoding }}</div></div>
        <div class="detail-row"><div class="detail-label">段数</div><div class="detail-value">{{ viewData.segments }}</div></div>
        <div class="detail-row">
          <div class="detail-label">平台审核</div>
          <div class="detail-value">
            <el-tag :type="statusTagMap[viewData.platformStatus || viewData.status]" size="small">
              {{ platformStatusLabel[viewData.platformStatus || viewData.status] || viewData.status }}
            </el-tag>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-label">模板内容</div>
          <div class="detail-value">
            <div class="content-block">{{ viewData.content }}</div>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-label">变量列表</div>
          <div class="detail-value">
            <el-tag v-for="v in viewData.variables" :key="v" type="primary" size="small" style="margin-right: 6px">{{ v }}</el-tag>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-label">适用国家</div>
          <div class="detail-value">
            <el-tag v-for="c in viewData.countries" :key="c" type="info" size="small" style="margin-right: 4px">{{ c }}</el-tag>
          </div>
        </div>
      </div>
      <div style="margin-top: 16px">
        <div class="detail-label" style="margin-bottom: 8px">审核记录</div>
        <div class="audit-records">
          <div v-for="(record, idx) in viewData.auditRecords" :key="idx" class="audit-record">
            <strong>{{ record.time }}</strong> -
            <el-tag :type="record.tagType" size="small" style="font-size: 10px">{{ record.action }}</el-tag> -
            操作人: {{ record.operator }} - 备注: {{ record.remark }}
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Audit Dialog -->
    <el-dialog v-model="auditDialogVisible" title="模板审核" width="740px" destroy-on-close
               @closed="auditLoading = false">
      <template #header>
        <span style="font-size: 16px; font-weight: 600">模板审核 - {{ auditData.code }}</span>
      </template>
      <div class="detail-rows">
        <div class="detail-row"><div class="detail-label">模板编号</div><div class="detail-value">{{ auditData.code }}</div></div>
        <div class="detail-row"><div class="detail-label">客户</div><div class="detail-value">{{ auditData.customer }}</div></div>
        <div class="detail-row">
          <div class="detail-label">类型</div>
          <div class="detail-value"><el-tag :type="typeTagMap[auditData.type]" size="small">{{ auditData.type }}</el-tag></div>
        </div>
        <div class="detail-row">
          <div class="detail-label">模板内容</div>
          <div class="detail-value">
            <div class="content-block">{{ auditData.content }}</div>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-label">变量列表</div>
          <div class="detail-value">
            <el-tag v-for="v in auditData.variables" :key="v" type="primary" size="small" style="margin-right: 6px">{{ v }}</el-tag>
          </div>
        </div>
      </div>

      <div style="margin-top: 16px; margin-bottom: 16px">
        <div class="detail-label" style="margin-bottom: 8px">预检结果</div>
        <div class="precheck-box">
          <div class="precheck-item">
            <el-icon color="#52c41a"><CircleCheckFilled /></el-icon>
            <span>敏感词扫描：未检测到违禁词</span>
          </div>
          <div class="precheck-item">
            <el-icon color="#52c41a"><CircleCheckFilled /></el-icon>
            <span>去重检测：未发现重复模板</span>
          </div>
          <div class="precheck-item">
            <el-icon color="#52c41a"><CircleCheckFilled /></el-icon>
            <span>退订标识检查：包含STOP退订指引</span>
          </div>
          <div class="precheck-item">
            <el-icon color="#faad14"><WarningFilled /></el-icon>
            <span>Marketing类型提醒：需确认客户已获得收件人营销授权</span>
          </div>
        </div>
      </div>

      <el-form-item label="审核备注">
        <el-input v-model="auditRemark" type="textarea" :rows="3" placeholder="请输入审核意见或备注..." />
      </el-form-item>

      <template #footer>
        <el-button @click="auditDialogVisible = false" :disabled="auditLoading">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="auditLoading" :disabled="auditLoading">拒绝</el-button>
        <el-button type="success" @click="handleApprove" :loading="auditLoading" :disabled="auditLoading">通过</el-button>
      </template>
    </el-dialog>

    <!-- Carrier Audit Dialog -->
    <el-dialog v-model="carrierAuditVisible" title="运营商审核" width="500px" destroy-on-close>
      <template #header>
        <span style="font-size:16px;font-weight:600">运营商审核 - {{ carrierAuditData.code }}</span>
      </template>
      <div class="detail-rows" style="margin-bottom:16px">
        <div class="detail-row"><div class="detail-label">模板名称</div><div class="detail-value">{{ carrierAuditData.name }}</div></div>
        <div class="detail-row"><div class="detail-label">客户</div><div class="detail-value">{{ carrierAuditData.customer }}</div></div>
        <div class="detail-row">
          <div class="detail-label">当前运营商状态</div>
          <div class="detail-value">
            <el-tag :type="carrierStatusTagMap[carrierAuditData.carrierStatus || 'pending']" size="small">
              {{ carrierStatusLabel[carrierAuditData.carrierStatus || 'pending'] }}
            </el-tag>
          </div>
        </div>
      </div>
      <el-form-item label="审核备注">
        <el-input v-model="carrierAuditRemark" type="textarea" :rows="3" placeholder="请输入运营商审核意见..." />
      </el-form-item>
      <template #footer>
        <el-button @click="carrierAuditVisible = false">取消</el-button>
        <el-button type="danger" :loading="carrierAuditLoading" @click="handleCarrierReject">未通过</el-button>
        <el-button type="success" :loading="carrierAuditLoading" @click="handleCarrierApprove">已通过</el-button>
      </template>
    </el-dialog>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="editDialogVisible" :title="editDialogTitle" width="600px" destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editForm.type" placeholder="请选择" style="width: 100%">
            <el-option label="OTP" value="OTP" />
            <el-option label="Transactional" value="Transactional" />
            <el-option label="Marketing" value="Marketing" />
            <el-option label="Service" value="Service" />
            <el-option label="动态模板 (Dynamic)" value="Dynamic" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" :rows="4" placeholder="请输入模板内容" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import {
  templatePage,
  templateSave,
  templateDel,
  templateReviewQueue,
  templateApprove,
  templateReject,
  templateCarrierApprove,
  templateCarrierReject,
  customerList
} from '../api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const canReview = computed(() => ['SUPER_ADMIN', 'OPS_ADMIN'].includes(authStore.roleCode))

// --- Header cell style ---
const headerCellStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

// --- Active tab ---
const activeTab = ref('list')

// --- Reference data ---
const customerOptions = ref([])
const loadCustomers = async () => {
  try {
    const r = await customerList({ page: 1, size: 9999 })
    customerOptions.value = (r.data?.list || r.data || [])
  } catch { /* ignore */ }
}

// --- Tag type maps (keys match DB uppercase values) ---
const typeTagMap = {
  OTP: '',
  TRANSACTIONAL: 'warning',
  MARKETING: 'warning',
  SERVICE: 'info',
  DYNAMIC: 'success'
}
const statusTagMap = {
  Approved: 'success',
  Pending: 'warning',
  Rejected: 'danger',
  Draft: 'info',
  Disabled: 'info',
  approved: 'success',
  pending: 'warning',
  rejected: 'danger',
  draft: 'info',
  disabled: 'info'
}
const platformStatusLabel = {
  pending: '审核中',
  approved: '已通过',
  rejected: '未通过',
  draft: '草稿',
  disabled: '已禁用'
}
const carrierStatusTagMap = {
  not_required: 'info',
  pending: 'warning',
  approved: 'success',
  rejected: 'danger'
}
const carrierStatusLabel = {
  not_required: '无需审核',
  pending: '审核中',
  approved: '已通过',
  rejected: '未通过'
}
const dltStatusTagMap = {
  Active: 'success',
  Pending: 'warning',
  Approved: 'success',
  '\u672A\u6CE8\u518C': 'info',
  '\u672A\u5173\u8054': 'info'
}

// --- Status mapping from API to display ---
const statusDisplayMap = {
  pending: 'Pending',
  approved: 'Approved',
  rejected: 'Rejected',
  draft: 'Draft',
  disabled: 'Disabled'
}

// --- Filters ---
const filters = reactive({
  customer: '',
  type: '',
  status: '',
  keyword: ''
})

// --- Pagination ---
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// --- Tab 1: Template list (from API) ---
const templateList = ref([])

// Map API row to table display format
function mapTemplateRow(item) {
  let variables = []
  if (item.variables) {
    try {
      variables = typeof item.variables === 'string' ? JSON.parse(item.variables) : item.variables
    } catch { variables = [] }
  }
  return {
    id: item.id,
    code: item.templateCode || '',
    name: item.templateName || '',
    customer: (() => {
      const opt = customerOptions.value.find(c => c.id === item.customerId)
      return (opt && (opt.companyName || opt.customerName)) || item.customerName || (item.customerId != null ? String(item.customerId) : '')
    })(),
    type: item.templateType || '',
    language: item.language || '',
    encoding: '',
    segments: '',
    status: statusDisplayMap[item.status] || item.status || '',
    platformStatus: item.platformStatus || item.status || '',
    carrierStatus: item.carrierStatus || 'pending',
    createdAt: item.createdAt || '',
    content: item.content || '',
    variables: variables,
    countries: item.countryCode ? [item.countryCode] : [],
    rejectReason: item.rejectReason || '',
    reviewer: item.reviewer || '',
    reviewAt: item.reviewAt || '',
    dltTemplateId: item.dltTemplateId || '',
    dltEntityId: item.dltEntityId || '',
    auditRecords: buildAuditRecords(item)
  }
}

function buildAuditRecords(item) {
  const records = []
  if (item.createdAt) {
    records.push({ time: item.createdAt, tagType: 'warning', action: '\u63D0\u4EA4\u5BA1\u6838', operator: item.customerId != null ? String(item.customerId) : 'customer', remark: '' })
  }
  if (item.status === 'approved' && item.reviewAt) {
    records.push({ time: item.reviewAt, tagType: 'success', action: '\u5BA1\u6838\u901A\u8FC7', operator: item.reviewer || 'admin', remark: '' })
  }
  if (item.status === 'rejected' && item.reviewAt) {
    records.push({ time: item.reviewAt, tagType: 'danger', action: '\u5BA1\u6838\u62D2\u7EDD', operator: item.reviewer || 'admin', remark: item.rejectReason || '' })
  }
  return records
}

async function loadTemplateList() {
  try {
    const params = {
      page: pagination.page,
      size: pagination.pageSize
    }
    if (filters.customer) params.customerId = filters.customer
    if (filters.type) params.templateType = filters.type
    if (filters.status) {
      // Map display status back to API status
      const reverseMap = { Pending: 'pending', Approved: 'approved', Rejected: 'rejected', Draft: 'draft', Disabled: 'disabled' }
      params.status = reverseMap[filters.status] || filters.status
    }
    if (filters.keyword) params.keyword = filters.keyword

    const res = await templatePage(params)
    const data = res.data || res
    const list = data.list || data.records || []
    templateList.value = list.map(mapTemplateRow)
    pagination.total = data.total || 0
  } catch (e) {
    console.error('Failed to load template list:', e)
    ElMessage.error('\u52A0\u8F7D\u6A21\u677F\u5217\u8868\u5931\u8D25')
  }
}

// --- Tab 2: Review queue (from API) ---
const reviewList = ref([])
const reviewTotal = ref(0)

async function loadReviewQueue() {
  try {
    const res = await templateReviewQueue({ page: 1, size: 50 })
    const data = res.data || res
    const list = data.list || data.records || []
    reviewList.value = list.map(item => {
      const mapped = mapTemplateRow(item)
      return {
        id: mapped.id,
        submitTime: mapped.createdAt,
        code: mapped.code,
        customer: mapped.customer,
        type: mapped.type,
        contentPreview: (mapped.content || '').substring(0, 80) + ((mapped.content || '').length > 80 ? '...' : ''),
        content: mapped.content,
        variables: mapped.variables
      }
    })
    reviewTotal.value = data.total || list.length
  } catch (e) {
    console.error('Failed to load review queue:', e)
    ElMessage.error('\u52A0\u8F7D\u5BA1\u6838\u961F\u5217\u5931\u8D25')
  }
}

// --- Tab 4: DLT mock data (no backend API yet) ---
const dltEntityList = ref([
  { customer: 'TechCorp', entityId: '110145678901****', entityName: 'TechCorp India Pvt Ltd', regStatus: 'Active', regTime: '2025-11-15' },
  { customer: 'GlobalPay', entityId: '110145678905****', entityName: 'GlobalPay Solutions India', regStatus: 'Active', regTime: '2025-12-03' },
  { customer: 'ShopMax', entityId: '110145678909****', entityName: 'ShopMax Commerce India', regStatus: 'Pending', regTime: '2026-02-20' },
  { customer: 'FinSecure', entityId: '--', entityName: '--', regStatus: '\u672A\u6CE8\u518C', regTime: '--' }
])

const dltTemplateList = ref([
  { code: 'TPL-20260301001', name: 'OTP\u9A8C\u8BC1\u7801\u901A\u77E5', customer: 'TechCorp', dltTemplateId: '110716123456****', dltStatus: 'Approved' },
  { code: 'TPL-20260305005', name: '\u8D26\u6237\u4F59\u989D\u63D0\u9192', customer: 'GlobalPay', dltTemplateId: '110716123450****', dltStatus: 'Approved' },
  { code: 'TPL-20260302003', name: '\u4FC3\u9500\u6D3B\u52A8\u901A\u77E5', customer: 'ShopMax', dltTemplateId: '110716123454****', dltStatus: 'Pending' },
  { code: 'TPL-20260310006', name: '\u4F1A\u5458\u6CE8\u518C\u6B22\u8FCE', customer: 'TechCorp', dltTemplateId: '--', dltStatus: '\u672A\u5173\u8054' }
])

// --- Dialog states ---
const viewDialogVisible = ref(false)
const viewData = ref({})
const auditDialogVisible = ref(false)
const auditData = ref({})
const auditRemark = ref('')
const auditLoading = ref(false)
const carrierAuditVisible = ref(false)
const carrierAuditData = ref({})
const carrierAuditRemark = ref('')
const carrierAuditLoading = ref(false)
const editDialogVisible = ref(false)
const editDialogTitle = ref('\u65B0\u5EFA\u6A21\u677F')
const editForm = reactive({ id: null, name: '', type: '', content: '', remark: '' })

// --- Handlers ---
function handleSearch() {
  pagination.page = 1
  loadTemplateList()
}

function handleReset() {
  filters.customer = ''
  filters.type = ''
  filters.status = ''
  filters.keyword = ''
  pagination.page = 1
  loadTemplateList()
}

function handleCreate() {
  editDialogTitle.value = '\u65B0\u5EFA\u6A21\u677F'
  editForm.id = null
  editForm.name = ''
  editForm.type = ''
  editForm.content = ''
  editForm.remark = ''
  editDialogVisible.value = true
}

function handleExport() {
  ElMessage.success('\u5BFC\u51FA\u4EFB\u52A1\u5DF2\u63D0\u4EA4')
}

function openViewDialog(row) {
  viewData.value = row
  viewDialogVisible.value = true
}

function handleAuditFromList(row) {
  auditData.value = {
    id: row.id,
    code: row.code,
    customer: row.customer,
    type: row.type,
    content: row.content || '',
    variables: row.variables || []
  }
  auditRemark.value = ''
  auditDialogVisible.value = true
}

function handleDisable(row) {
  ElMessageBox.confirm(`\u786E\u8BA4\u8981\u7981\u7528 "${row.name}" \u5417\uFF1F`, '\u786E\u8BA4\u64CD\u4F5C', { type: 'warning' })
    .then(async () => {
      try {
        await templateDel(row.id)
        ElMessage.success('\u7981\u7528\u6210\u529F')
        loadTemplateList()
      } catch {
        ElMessage.error('\u64CD\u4F5C\u5931\u8D25')
      }
    })
    .catch(() => {})
}

function openAuditDialog(row) {
  auditData.value = {
    id: row.id,
    code: row.code,
    customer: row.customer,
    type: row.type,
    content: row.content || row.contentPreview,
    variables: row.variables || []
  }
  auditRemark.value = ''
  auditDialogVisible.value = true
}

async function handleApprove() {
  if (auditLoading.value) return
  auditLoading.value = true
  try {
    await templateApprove(auditData.value.id)
    auditDialogVisible.value = false
    ElMessage.success('审核通过成功')
    loadTemplateList()
    if (canReview.value) loadReviewQueue()
  } catch {
    ElMessage.error('审核操作失败')
  } finally {
    auditLoading.value = false
  }
}

async function handleReject() {
  if (auditLoading.value) return
  try {
    await ElMessageBox.confirm('确认要拒绝该模板吗？', '确认操作', { type: 'warning' })
  } catch {
    return
  }
  auditLoading.value = true
  try {
    await templateReject(auditData.value.id, auditRemark.value || '')
    auditDialogVisible.value = false
    ElMessage.success('拒绝成功')
    loadTemplateList()
    if (canReview.value) loadReviewQueue()
  } catch {
    ElMessage.error('拒绝操作失败')
  } finally {
    auditLoading.value = false
  }
}

function openCarrierAuditDialog(row) {
  carrierAuditData.value = {
    id: row.id,
    code: row.code,
    name: row.name,
    customer: row.customer,
    carrierStatus: row.carrierStatus || 'pending'
  }
  carrierAuditRemark.value = ''
  carrierAuditVisible.value = true
}

async function handleCarrierApprove() {
  if (carrierAuditLoading.value) return
  carrierAuditLoading.value = true
  try {
    await templateCarrierApprove(carrierAuditData.value.id)
    carrierAuditVisible.value = false
    ElMessage.success('运营商审核通过')
    loadTemplateList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    carrierAuditLoading.value = false
  }
}

async function handleCarrierReject() {
  if (carrierAuditLoading.value) return
  try {
    await ElMessageBox.confirm('确认运营商审核不通过？', '确认操作', { type: 'warning' })
  } catch {
    return
  }
  carrierAuditLoading.value = true
  try {
    await templateCarrierReject(carrierAuditData.value.id, carrierAuditRemark.value || '')
    carrierAuditVisible.value = false
    ElMessage.success('运营商审核已拒绝')
    loadTemplateList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    carrierAuditLoading.value = false
  }
}

async function handleSave() {
  try {
    const payload = {
      templateName: editForm.name,
      templateType: editForm.type,
      content: editForm.content
    }
    if (editForm.id) payload.id = editForm.id
    await templateSave(payload)
    editDialogVisible.value = false
    ElMessage.success('\u4FDD\u5B58\u6210\u529F')
    loadTemplateList()
  } catch {
    ElMessage.error('\u4FDD\u5B58\u5931\u8D25')
  }
}

function handleDltRegister(row) {
  editDialogTitle.value = `DLT\u6CE8\u518C - ${row.customer}`
  editDialogVisible.value = true
}
function handleDltEdit(row) {
  editDialogTitle.value = `DLT Entity - ${row.entityName}`
  editDialogVisible.value = true
}
function handleDltViewTemplates(row) {
  ElMessage.info(`\u67E5\u770B ${row.customer} \u7684DLT\u6A21\u677F`)
}
function handleDltLink(row) {
  editDialogTitle.value = `\u5173\u8054DLT - ${row.name}`
  editDialogVisible.value = true
}
function handleDltEditLink(row) {
  editDialogTitle.value = `DLT\u5173\u8054 - ${row.name}`
  editDialogVisible.value = true
}
function handleDltUnlink(row) {
  ElMessageBox.confirm(`\u786E\u8BA4\u8981\u89E3\u7ED1 "${row.name}" \u5417\uFF1F`, '\u786E\u8BA4\u64CD\u4F5C', { type: 'warning' })
    .then(() => ElMessage.success('\u89E3\u7ED1\u6210\u529F'))
    .catch(() => {})
}

// --- Watch pagination changes ---
watch(() => pagination.page, () => {
  loadTemplateList()
})

// --- Watch tab changes to load review queue ---
watch(activeTab, (val) => {
  if (val === 'review') {
    loadReviewQueue()
  }
})

// --- Load data on mount ---
onMounted(() => {
  loadCustomers()
  loadTemplateList()
  if (canReview.value) {
    loadReviewQueue()
  }
})
</script>

<style scoped>
.template-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: flex-end;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Tab badge */
.tab-badge {
  margin-left: 6px;
}
.tab-badge :deep(.el-badge__content) {
  font-size: 10px;
}

/* Content preview truncation */
.content-preview {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Stat cards */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
}

.stat-footer {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.stat-footer .up {
  color: #52c41a;
}

.stat-footer .down {
  color: #ff4d4f;
}

/* Charts grid */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.charts-grid .chart-full {
  grid-column: 1 / -1;
}

.chart-placeholder {
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 14px;
}

/* Detail rows in dialogs */
.detail-rows {
  display: flex;
  flex-direction: column;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  font-size: 13px;
}

.detail-label {
  width: 100px;
  color: #999;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #333;
}

.content-block {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
  line-height: 1.6;
}

/* Audit records */
.audit-records {
  background: #fafafa;
  border-radius: 4px;
  padding: 12px;
}

.audit-record {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 12px;
  color: #666;
}

.audit-record:last-child {
  border-bottom: none;
}

/* Precheck */
.precheck-box {
  background: #fafafa;
  border-radius: 4px;
  padding: 12px;
}

.precheck-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}

/* Responsive */
@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-grid {
    grid-template-columns: 1fr;
  }
  .charts-grid .chart-full {
    grid-column: 1;
  }
}
</style>
