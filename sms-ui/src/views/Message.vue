<template>
  <el-card class="table-card">
    <div class="page-header"><h2>发送管理</h2></div>

    <el-tabs v-model="activeTab">
      <!-- Tab 1: 发送记录 -->
      <el-tab-pane label="发送记录" name="send">
        <div class="filter-bar">
          <div class="filter-item">
            <span class="filter-label">开始时间</span>
            <el-date-picker v-model="query.startTime" type="date" placeholder="开始时间" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
          </div>
          <div class="filter-item">
            <span class="filter-label">结束时间</span>
            <el-date-picker v-model="query.endTime" type="date" placeholder="结束时间" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
          </div>
          <div class="filter-item">
            <span class="filter-label">客户</span>
            <el-select v-model="query.customerId" placeholder="全部客户" clearable filterable style="width: 160px;">
              <el-option v-for="c in customers" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">国家</span>
            <el-select v-model="query.countryCode" placeholder="全部国家" clearable filterable style="width: 140px;">
              <el-option v-for="c in countries" :key="c.code" :label="`${c.code} - ${c.nameZh || c.name}`" :value="c.code" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">通道</span>
            <el-select v-model="query.channelId" placeholder="全部通道" clearable filterable style="width: 160px;">
              <el-option v-for="c in channels" :key="c.id" :label="c.channelName || c.name" :value="c.id" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">SID</span>
            <el-input v-model="query.sid" placeholder="SID" clearable style="width: 120px;" />
          </div>
          <div class="filter-item">
            <span class="filter-label">状态</span>
            <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px;">
              <el-option label="已送达 DELIVERED" :value="2" />
              <el-option label="失败 FAILED" :value="3" />
              <el-option label="待发送 PENDING" :value="0" />
              <el-option label="已提交 SUBMITTED" :value="1" />
              <el-option label="被拒绝 REJECTED" :value="4" />
              <el-option label="已过期 EXPIRED" :value="5" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">号码搜索</span>
            <el-input v-model="query.toNumber" placeholder="目标号码" clearable style="width: 150px;" />
          </div>
          <div class="filter-item filter-buttons">
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>

        <el-table
          :data="list"
          stripe
          v-loading="loading"
          :header-cell-style="headerStyle"
          row-key="id"
          @row-click="toggleExpand"
          highlight-current-row
          style="margin-top: 12px;"
        >
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-detail">
                <div class="detail-grid">
                  <div class="detail-item"><span class="detail-label">完整消息ID</span><span class="detail-value"><code>{{ row.messageId }}</code></span></div>
                  <div class="detail-item"><span class="detail-label">编码类型</span><span class="detail-value">{{ row.encoding === 0 ? 'GSM-7' : row.encoding === 1 ? 'UCS-2' : '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">段数</span><span class="detail-value">{{ row.segments ?? '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">提交时间</span><span class="detail-value">{{ row.createdAt || '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">送达时间</span><span class="detail-value">{{ row.deliveredAt || '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">DLR状态</span><span class="detail-value">{{ row.dlrStatus || '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">错误码</span><span class="detail-value" :style="row.errorCode ? 'color:#f56c6c' : ''">{{ row.errorCode || '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">客户单价</span><span class="detail-value">{{ row.price != null ? `$${row.price}` : '-' }}</span></div>
                  <div class="detail-item"><span class="detail-label">client_ref</span><span class="detail-value">{{ row.clientRef || '-' }}</span></div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="messageId" label="消息ID" width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <code class="msg-id">{{ row.messageId }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="customerId" label="客户" width="120">
            <template #default="{ row }">{{ customerMap[row.customerId] || row.customerId || '-' }}</template>
          </el-table-column>
          <el-table-column prop="toNumber" label="目标号码" width="150" />
          <el-table-column prop="countryCode" label="国家" width="70" align="center" />
          <el-table-column prop="sid" label="SID" width="110" />
          <el-table-column prop="channelId" label="通道" width="120">
            <template #default="{ row }">{{ channelMap[row.channelId] || row.channelId || '-' }}</template>
          </el-table-column>
          <el-table-column label="内容" width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content || 'N/A' }}</template>
          </el-table-column>
          <el-table-column prop="encoding" label="编码" width="70" align="center">
            <template #default="{ row }">{{ row.encoding === 0 ? 'GSM' : row.encoding === 1 ? 'UCS2' : '-' }}</template>
          </el-table-column>
          <el-table-column prop="segments" label="段数" width="60" align="center" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small" effect="light">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="费用" width="90" align="right">
            <template #default="{ row }">{{ row.price != null ? `$${row.price}` : '-' }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadData"
          style="margin-top: 16px; justify-content: flex-end;"
        />
      </el-tab-pane>

      <!-- Tab 2: 上行记录 MO -->
      <el-tab-pane label="上行记录 MO" name="mo">
        <div class="filter-bar">
          <div class="filter-item">
            <span class="filter-label">开始时间</span>
            <el-date-picker v-model="moQuery.startTime" type="date" placeholder="开始时间" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
          </div>
          <div class="filter-item">
            <span class="filter-label">结束时间</span>
            <el-date-picker v-model="moQuery.endTime" type="date" placeholder="结束时间" value-format="YYYY-MM-DD" clearable style="width: 150px;" />
          </div>
          <div class="filter-item">
            <span class="filter-label">国家</span>
            <el-select v-model="moQuery.countryCode" placeholder="全部国家" clearable filterable style="width: 140px;">
              <el-option v-for="c in countries" :key="c.code" :label="`${c.code} - ${c.nameZh || c.name}`" :value="c.code" />
            </el-select>
          </div>
          <div class="filter-item filter-buttons">
            <el-button type="primary" @click="loadMoData">查询</el-button>
            <el-button @click="resetMoQuery">重置</el-button>
          </div>
        </div>

        <el-table :data="moList" stripe v-loading="moLoading" :header-cell-style="headerStyle" style="margin-top: 12px;">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="moMessageId" label="MO消息ID" width="200" show-overflow-tooltip>
            <template #default="{ row }"><code class="msg-id">{{ row.moMessageId }}</code></template>
          </el-table-column>
          <el-table-column prop="fromNumber" label="来源号码" width="150" />
          <el-table-column prop="toSid" label="目标SID" width="120" />
          <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
          <el-table-column prop="countryCode" label="国家" width="70" align="center" />
          <el-table-column prop="channelName" label="通道" width="120" />
          <el-table-column prop="matchKeyword" label="命中关键词" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.matchKeyword" size="small" :type="moKeywordType(row.matchKeyword)">{{ row.matchKeyword }}</el-tag>
              <span v-else style="color:#c0c4cc;">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="action" label="处理动作" width="140" />
          <el-table-column prop="pushStatus" label="推送状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.pushStatus === 'PUSHED' ? 'success' : row.pushStatus === 'FAILED' ? 'danger' : 'info'">
                {{ row.pushStatus === 'PUSHED' ? '已推送' : row.pushStatus === 'FAILED' ? '推送失败' : '待推送' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="moQuery.page"
          v-model:page-size="moQuery.size"
          :total="moTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadMoData"
          style="margin-top: 16px; justify-content: flex-end;"
        />
      </el-tab-pane>

      <!-- Tab 3: 定时任务 -->
      <el-tab-pane label="定时任务" name="schedule">
        <div class="filter-bar">
          <div class="filter-item">
            <span class="filter-label">客户</span>
            <el-select v-model="scheduleQuery.customer" placeholder="全部客户" clearable filterable style="width: 160px;">
              <el-option v-for="c in customers" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">状态</span>
            <el-select v-model="scheduleQuery.status" placeholder="全部状态" clearable style="width: 130px;">
              <el-option label="待发送" value="pending" />
              <el-option label="发送中" value="running" />
              <el-option label="已完成" value="completed" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">时间范围</span>
            <el-date-picker v-model="scheduleQuery.dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" clearable style="width: 260px;" />
          </div>
          <div class="filter-item filter-buttons">
            <el-button type="primary" @click="searchSchedule">查询</el-button>
            <el-button @click="resetScheduleQuery">重置</el-button>
          </div>
        </div>

        <el-table
          :data="scheduleList"
          stripe
          :header-cell-style="headerStyle"
          style="margin-top: 12px;"
        >
          <el-table-column prop="taskId" label="任务ID" width="180">
            <template #default="{ row }">
              <code class="msg-id">{{ row.taskId }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="customer" label="客户" width="120" />
          <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="targetCount" label="目标数量" width="110" align="right">
            <template #default="{ row }">{{ row.targetCount.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="sentCount" label="已发送" width="110" align="right">
            <template #default="{ row }">{{ row.sentCount.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="planTime" label="计划时间" width="170" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="scheduleStatusType(row.status)" size="small" effect="light" :class="{ 'tag-cancelled': row.status === 'cancelled' }">
                {{ scheduleStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleScheduleView(row)">查看</el-button>
              <el-button v-if="row.status === 'running'" link type="warning" size="small" @click="handleSchedulePause(row)">暂停</el-button>
              <el-button v-if="row.status === 'pending'" link type="danger" size="small" @click="handleScheduleCancel(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="scheduleQuery.page"
          v-model:page-size="scheduleQuery.size"
          :total="scheduleTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          style="margin-top: 16px; justify-content: flex-end;"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { messageList, moRecordList } from '../api'
import { useRefData } from '../stores/refData'

const refData = useRefData()

const activeTab = ref('send')

/* ---------- header style ---------- */
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

/* ---------- status helpers ---------- */
const STATUS_MAP = {
  0: { label: '待发送', type: 'info' },
  1: { label: '已提交', type: 'primary' },
  2: { label: '已送达', type: 'success' },
  3: { label: '失败', type: 'danger' },
  4: { label: '被拒绝', type: 'danger' },
  5: { label: '已过期', type: 'warning' }
}
const statusLabel = s => STATUS_MAP[s]?.label || '-'
const statusType = s => STATUS_MAP[s]?.type || 'info'

/* ---------- reference data ---------- */
const customers = ref([])
const countries = ref([])
const channels = ref([])

const customerMap = computed(() => {
  const m = {}
  customers.value.forEach(c => { m[c.id] = c.companyName || c.customerName || c.id })
  return m
})
const channelMap = computed(() => {
  const m = {}
  channels.value.forEach(c => { m[c.id] = c.channelName || c.name || c.id })
  return m
})

const loadRefData = async () => {
  const [cu, co, ch] = await Promise.all([refData.loadCustomers(), refData.loadCountries(), refData.loadChannels()])
  customers.value = cu; countries.value = co; channels.value = ch
}

/* ---------- send records (Tab 1) ---------- */
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 20,
  startTime: '',
  endTime: '',
  customerId: null,
  countryCode: '',
  channelId: null,
  sid: '',
  status: null,
  toNumber: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach(k => {
      if (params[k] === null || params[k] === '' || params[k] === undefined) delete params[k]
    })
    const res = await messageList(params)
    list.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.page = 1
  query.startTime = ''
  query.endTime = ''
  query.customerId = null
  query.countryCode = ''
  query.channelId = null
  query.sid = ''
  query.status = null
  query.toNumber = ''
  loadData()
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

/* ---------- expand row ---------- */
const tableRef = ref(null)
const toggleExpand = (row, column, event) => {
  // auto-handled by el-table expand type
}

/* ---------- MO records (Tab 2) ---------- */
const moLoading = ref(false)
const moList = ref([])
const moTotal = ref(0)
const moQuery = reactive({ page: 1, size: 20, startTime: '', endTime: '', countryCode: '' })

const moKeywordType = kw => {
  const k = (kw || '').toUpperCase()
  if (k === 'STOP' || k === 'UNSUBSCRIBE') return 'danger'
  if (k === 'HELP') return 'warning'
  if (k === 'YES') return ''
  return 'info'
}

const loadMoData = async () => {
  moLoading.value = true
  try {
    const params = { ...moQuery }
    Object.keys(params).forEach(k => {
      if (params[k] === null || params[k] === '' || params[k] === undefined) delete params[k]
    })
    const res = await moRecordList(params)
    moList.value = res.data?.list || res.data || []
    moTotal.value = res.data?.total || 0
  } finally {
    moLoading.value = false
  }
}

const resetMoQuery = () => {
  moQuery.page = 1
  moQuery.startTime = ''
  moQuery.endTime = ''
  moQuery.countryCode = ''
  loadMoData()
}

/* ---------- 定时任务 (Tab 3) ---------- */
const scheduleQuery = reactive({ customer: '', status: '', dateRange: null, page: 1, size: 10 })

const SCHEDULE_STATUS_MAP = {
  pending:   { label: '待发送', type: 'info' },
  running:   { label: '发送中', type: 'warning' },
  completed: { label: '已完成', type: 'success' },
  cancelled: { label: '已取消', type: 'info' }
}
const scheduleStatusLabel = s => SCHEDULE_STATUS_MAP[s]?.label || '-'
const scheduleStatusType  = s => SCHEDULE_STATUS_MAP[s]?.type || 'info'

const scheduleTotal = ref(23)
const scheduleList = ref([
  { taskId: 'TASK-20260314001', customer: 'TechCorp',   taskName: 'OTP活动推送-泰国',   targetCount: 50000,  sentCount: 50000, planTime: '2026-03-14 10:00', status: 'completed', createdAt: '2026-03-13 15:30' },
  { taskId: 'TASK-20260314002', customer: 'ShopMax',    taskName: '促销短信-印尼',       targetCount: 120000, sentCount: 78450, planTime: '2026-03-14 14:00', status: 'running',   createdAt: '2026-03-14 09:00' },
  { taskId: 'TASK-20260315001', customer: 'GlobalPay',  taskName: '账单提醒-马来',       targetCount: 35000,  sentCount: 0,     planTime: '2026-03-15 09:00', status: 'pending',   createdAt: '2026-03-14 16:20' },
  { taskId: 'TASK-20260315002', customer: 'FinSecure',  taskName: '安全通知-全球',       targetCount: 200000, sentCount: 0,     planTime: '2026-03-15 12:00', status: 'pending',   createdAt: '2026-03-15 08:00' },
  { taskId: 'TASK-20260312001', customer: 'TechCorp',   taskName: '节日祝福-泰国',       targetCount: 80000,  sentCount: 0,     planTime: '2026-03-12 08:00', status: 'cancelled', createdAt: '2026-03-11 14:00' }
])

const searchSchedule = () => { ElMessage.info('查询已刷新') }
const resetScheduleQuery = () => {
  scheduleQuery.customer = ''
  scheduleQuery.status = ''
  scheduleQuery.dateRange = null
  scheduleQuery.page = 1
}
const handleScheduleView   = row => { ElMessage.info(`查看任务: ${row.taskId}`) }
const handleSchedulePause  = row => { ElMessage.warning(`暂停任务: ${row.taskId}`) }
const handleScheduleCancel = row => { ElMessage.warning(`取消任务: ${row.taskId}`) }

/* ---------- init ---------- */
onMounted(() => {
  loadRefData()
  loadData()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 4px;
}
.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.filter-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}
.filter-buttons {
  flex-direction: row;
  align-items: flex-end;
  gap: 8px;
  margin-left: 4px;
}
.msg-id {
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.expand-detail {
  padding: 16px 24px;
  background: #fafbfc;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px 24px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.detail-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}
.detail-value {
  font-size: 13px;
  color: #374151;
}
.detail-value code {
  font-size: 12px;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  word-break: break-all;
}
.tag-cancelled :deep(.el-tag__content) {
  color: #909399;
}
.tag-cancelled {
  --el-tag-bg-color: #f4f4f5 !important;
  --el-tag-border-color: #e9e9eb !important;
  color: #909399 !important;
}
:deep(.el-tabs__header) {
  margin-bottom: 16px;
}
:deep(.el-pagination) {
  display: flex;
}
</style>
