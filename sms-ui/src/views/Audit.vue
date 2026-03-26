<template>
  <div class="audit-page">
    <!-- Page Header -->
    <div class="page-header">
      <h2>审计日志</h2>
      <el-button type="primary" :icon="Download" @click="handleExport">导出日志</el-button>
    </div>

    <!-- Tabs -->
    <el-card class="table-card">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: 操作日志 -->
        <el-tab-pane label="操作日志" name="operation">
          <div class="filter-bar">
            <el-date-picker v-model="opQuery.startTime" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-date-picker v-model="opQuery.endTime" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-input v-model="opQuery.operator" placeholder="操作人" clearable style="width: 140px;" />
            <el-select v-model="opQuery.module" placeholder="模块" clearable style="width: 130px;">
              <el-option label="Customer" value="Customer" />
              <el-option label="Channel" value="Channel" />
              <el-option label="SID" value="SID" />
              <el-option label="Risk" value="Risk" />
              <el-option label="Billing" value="Billing" />
              <el-option label="System" value="System" />
            </el-select>
            <el-select v-model="opQuery.action" placeholder="操作类型" clearable style="width: 130px;">
              <el-option label="Create" value="Create" />
              <el-option label="Update" value="Update" />
              <el-option label="Delete" value="Delete" />
            </el-select>
            <el-select v-model="opQuery.result" placeholder="结果" clearable style="width: 110px;">
              <el-option label="Success" value="Success" />
              <el-option label="Failed" value="Failed" />
            </el-select>
            <el-button type="primary" @click="handleOpSearch">查询</el-button>
            <el-button @click="handleOpReset">重置</el-button>
          </div>

          <el-table :data="opList" stripe v-loading="opLoading" style="font-size: 13px;"
            :header-cell-style="headerStyle" row-key="id">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="diff-container" v-if="row.beforeData || row.afterData">
                  <div class="diff-panel diff-removed">
                    <div style="font-weight: 600; margin-bottom: 8px;">Before</div>
                    <pre>{{ formatJson(row.beforeData) }}</pre>
                  </div>
                  <div class="diff-panel diff-added">
                    <div style="font-weight: 600; margin-bottom: 8px;">After</div>
                    <pre>{{ formatJson(row.afterData) }}</pre>
                  </div>
                </div>
                <div v-else style="padding: 12px; color: #999; font-size: 13px;">
                  {{ row.summary || '无详细信息' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="operatorName" label="操作人" width="110" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.operatorType === 'admin' ? '' : 'warning'" size="small">
                  {{ row.operatorType || 'admin' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="module" label="模块" width="100" />
            <el-table-column prop="action" label="操作" width="90" />
            <el-table-column label="目标对象" width="150">
              <template #default="{ row }">
                <span v-if="row.targetType">{{ row.targetType }}#{{ row.targetId }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="摘要" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.summary || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="(row.result || 'Success') === 'Success' ? 'success' : 'danger'" size="small">
                  {{ row.result || 'Success' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="opQuery.page"
            v-model:page-size="opQuery.size"
            :total="opTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadOpData"
            style="margin-top: 16px;"
          />
        </el-tab-pane>

        <!-- Tab 2: 登录日志 -->
        <el-tab-pane label="登录日志" name="login">
          <div class="filter-bar">
            <el-date-picker v-model="loginQuery.startTime" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-date-picker v-model="loginQuery.endTime" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-select v-model="loginQuery.userType" placeholder="用户类型" clearable style="width: 130px;">
              <el-option label="admin" value="admin" />
              <el-option label="customer" value="customer" />
            </el-select>
            <el-select v-model="loginQuery.result" placeholder="结果" clearable style="width: 120px;">
              <el-option label="全部" value="" />
              <el-option label="success" value="success" />
              <el-option label="failed" value="failed" />
              <el-option label="locked" value="locked" />
            </el-select>
            <el-button type="primary" @click="handleLoginSearch">查询</el-button>
            <el-button @click="handleLoginReset">重置</el-button>
          </div>

          <el-table :data="loginList" stripe style="font-size: 13px;"
            :header-cell-style="headerStyle" :row-style="loginRowStyle" v-loading="loginLoading">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.userType === 'admin' ? '' : 'info'" size="small">{{ row.userType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="loginType" label="登录方式" width="120" />
            <el-table-column prop="ipAddress" label="IP" width="140" />
            <el-table-column prop="geoLocation" label="归属地" width="140" />
            <el-table-column label="User-Agent" min-width="200">
              <template #default="{ row }">
                <span class="ua-cell">{{ row.userAgent }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.result === 'success' ? 'success' : row.result === 'locked' ? 'warning' : 'danger'" size="small">
                  {{ row.result }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failReason" label="失败原因" min-width="130" show-overflow-tooltip />
          </el-table>

          <el-pagination
            v-model:current-page="loginQuery.page"
            v-model:page-size="loginQuery.size"
            :total="loginTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadLoginData"
            style="margin-top: 16px;"
          />
        </el-tab-pane>

        <!-- Tab 3: API访问日志 -->
        <el-tab-pane label="API访问日志" name="api">
          <div class="filter-bar">
            <el-date-picker v-model="apiQuery.startTime" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-date-picker v-model="apiQuery.endTime" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-select v-model="apiQuery.customer" placeholder="全部客户" clearable filterable style="width: 140px;">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
            <el-input v-model="apiQuery.path" placeholder="/api/v1/..." clearable style="width: 160px;" />
            <el-select v-model="apiQuery.statusCode" placeholder="全部" clearable style="width: 100px;">
              <el-option label="全部" value="" />
              <el-option label="200" value="200" />
              <el-option label="400" value="400" />
              <el-option label="401" value="401" />
              <el-option label="403" value="403" />
              <el-option label="429" value="429" />
              <el-option label="500" value="500" />
            </el-select>
            <el-button type="primary" @click="handleApiSearch">查询</el-button>
            <el-button @click="handleApiReset">重置</el-button>
          </div>

          <el-table :data="apiList" stripe style="font-size: 13px;" :header-cell-style="headerStyle">
            <el-table-column prop="time" label="时间" width="185" />
            <el-table-column prop="customer" label="客户" width="110" />
            <el-table-column prop="path" label="接口" min-width="200" show-overflow-tooltip />
            <el-table-column label="方法" width="80">
              <template #default="{ row }">
                <el-tag :type="row.method === 'POST' ? 'success' : ''" size="small">{{ row.method }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column label="响应码" width="90">
              <template #default="{ row }">
                <el-tag :type="row.statusCode === 200 ? 'success' : row.statusCode === 400 ? 'warning' : row.statusCode >= 429 ? 'danger' : ''" size="small">
                  {{ row.statusCode }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="响应时间" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.responseTime > 1000 ? '#ef4444' : row.responseTime > 200 ? '#f59e0b' : '#10b981' }">
                  {{ row.responseTime }} ms
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="errorCode" label="错误码" width="180" />
          </el-table>

          <el-pagination
            v-model:current-page="apiQuery.page"
            v-model:page-size="apiQuery.size"
            :total="apiTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px;"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { auditOperationList, auditLoginList, customerList } from '../api'

const activeTab = ref('operation')

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

// Reference data
const customerOptions = ref([])
const loadCustomers = async () => {
  try {
    const r = await customerList({ page: 1, size: 9999 })
    customerOptions.value = (r.data?.list || r.data || [])
  } catch { /* ignore */ }
}

// ========== Tab 1: 操作日志 ==========
const opLoading = ref(false)
const opList = ref([])
const opTotal = ref(0)
const opQuery = reactive({
  page: 1,
  size: 20,
  startTime: '',
  endTime: '',
  operator: '',
  module: '',
  action: '',
  result: ''
})

const loadOpData = async () => {
  opLoading.value = true
  try {
    const params = { page: opQuery.page, size: opQuery.size }
    if (opQuery.startTime) params.startTime = opQuery.startTime
    if (opQuery.endTime) params.endTime = opQuery.endTime
    if (opQuery.operator) params.operatorName = opQuery.operator
    if (opQuery.module) params.module = opQuery.module
    if (opQuery.result) params.result = opQuery.result
    const res = await auditOperationList(params)
    opList.value = res.data?.list || res.data || []
    opTotal.value = res.data?.total || 0
  } finally {
    opLoading.value = false
  }
}

const handleOpSearch = () => {
  opQuery.page = 1
  loadOpData()
}

const handleOpReset = () => {
  opQuery.startTime = ''
  opQuery.endTime = ''
  opQuery.operator = ''
  opQuery.module = ''
  opQuery.action = ''
  opQuery.result = ''
  opQuery.page = 1
  loadOpData()
}

// Format JSON string for diff display
const formatJson = (jsonStr) => {
  if (!jsonStr) return '-'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

// ========== Tab 2: 登录日志 ==========
const loginLoading = ref(false)
const loginList = ref([])
const loginTotal = ref(0)
const loginQuery = reactive({
  page: 1,
  size: 20,
  startTime: '',
  endTime: '',
  userType: '',
  result: ''
})

const loadLoginData = async () => {
  loginLoading.value = true
  try {
    const params = { page: loginQuery.page, size: loginQuery.size }
    if (loginQuery.startTime) params.startTime = loginQuery.startTime
    if (loginQuery.endTime) params.endTime = loginQuery.endTime
    if (loginQuery.userType) params.userType = loginQuery.userType
    if (loginQuery.result) params.result = loginQuery.result
    const res = await auditLoginList(params)
    loginList.value = res.data?.list || res.data || []
    loginTotal.value = res.data?.total || 0
  } finally {
    loginLoading.value = false
  }
}

const loginRowStyle = ({ row }) => {
  if (row.result === 'locked') return { background: '#fff1f0' }
  if (row.result === 'failed') return { background: '#fffbe6' }
  return {}
}

const handleLoginSearch = () => {
  loginQuery.page = 1
  loadLoginData()
}

const handleLoginReset = () => {
  loginQuery.startTime = ''
  loginQuery.endTime = ''
  loginQuery.userType = ''
  loginQuery.result = ''
  loginQuery.page = 1
  loadLoginData()
}

// ========== Tab 3: API访问日志 ==========
const apiQuery = reactive({
  page: 1,
  size: 20,
  startTime: '',
  endTime: '',
  customer: '',
  path: '',
  statusCode: ''
})
const apiTotal = ref(15382)

const apiMockData = [
  { time: '2026-03-14 09:45:12.342', customer: 'TechCorp', path: '/api/v1/sms/send', method: 'POST', ip: '203.0.113.50', statusCode: 200, responseTime: 128, errorCode: '-' },
  { time: '2026-03-14 09:44:58.105', customer: 'GlobalPay', path: '/api/v1/sms/status', method: 'GET', ip: '198.51.100.22', statusCode: 200, responseTime: 45, errorCode: '-' },
  { time: '2026-03-14 09:43:22.876', customer: 'ShopMax', path: '/api/v1/sms/send', method: 'POST', ip: '103.45.67.89', statusCode: 429, responseTime: 12, errorCode: 'RATE_LIMIT_EXCEEDED' },
  { time: '2026-03-14 09:42:15.553', customer: 'FinSecure', path: '/api/v1/template/create', method: 'POST', ip: '172.16.0.88', statusCode: 400, responseTime: 35, errorCode: 'INVALID_PARAM' },
  { time: '2026-03-14 09:41:08.211', customer: 'TechCorp', path: '/api/v1/balance', method: 'GET', ip: '203.0.113.50', statusCode: 200, responseTime: 22, errorCode: '-' },
  { time: '2026-03-14 09:40:55.009', customer: 'ShopMax', path: '/api/v1/sms/send', method: 'POST', ip: '103.45.67.89', statusCode: 500, responseTime: 5032, errorCode: 'INTERNAL_ERROR' },
  { time: '2026-03-14 09:39:31.774', customer: 'GlobalPay', path: '/api/v1/sms/send', method: 'POST', ip: '198.51.100.22', statusCode: 200, responseTime: 95, errorCode: '-' }
]

const apiList = ref([...apiMockData])

const handleApiSearch = () => {
  apiQuery.page = 1
  let filtered = [...apiMockData]
  if (apiQuery.customer) filtered = filtered.filter(r => r.customer === apiQuery.customer)
  if (apiQuery.path) filtered = filtered.filter(r => r.path.includes(apiQuery.path))
  if (apiQuery.statusCode) filtered = filtered.filter(r => String(r.statusCode) === apiQuery.statusCode)
  apiList.value = filtered
}

const handleApiReset = () => {
  apiQuery.startTime = ''
  apiQuery.endTime = ''
  apiQuery.customer = ''
  apiQuery.path = ''
  apiQuery.statusCode = ''
  apiQuery.page = 1
  apiList.value = [...apiMockData]
}

// ========== Export ==========
const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadCustomers()
  loadOpData()
  loadLoginData()
})
</script>

<style scoped>
.audit-page {
  padding: 0;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 16px;
}

/* Diff styling for expandable rows */
.diff-container {
  display: flex;
  gap: 16px;
  margin-top: 12px;
}
.diff-panel {
  flex: 1;
  background: #fafafa;
  border-radius: 4px;
  padding: 12px;
  font-size: 12px;
  font-family: monospace;
  line-height: 1.8;
}
.diff-panel pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.diff-removed {
  background: #fff1f0;
  color: #cf1322;
}
.diff-added {
  background: #f6ffed;
  color: #389e0d;
}

/* User-Agent cell truncation */
.ua-cell {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
