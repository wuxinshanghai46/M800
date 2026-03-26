<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">消息记录</h2>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">

        <!-- Tab 1: MT 发送记录 -->
        <el-tab-pane label="发送记录 MT" name="mt">
          <div class="filter-bar">
            <el-date-picker v-model="mtQuery.startTime" type="datetime" placeholder="开始时间"
              value-format="YYYY-MM-DD HH:mm:ss" clearable style="width: 180px;" />
            <el-date-picker v-model="mtQuery.endTime" type="datetime" placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss" clearable style="width: 180px;" />
            <el-select v-model="mtQuery.status" placeholder="全部状态" clearable style="width: 120px;">
              <el-option label="已送达" value="DELIVERED" />
              <el-option label="发送中" value="SUBMITTED" />
              <el-option label="失败" value="FAILED" />
              <el-option label="已过期" value="EXPIRED" />
            </el-select>
            <el-select v-model="mtQuery.countryCode" placeholder="全部国家" clearable style="width: 120px;">
              <el-option v-for="c in countryOptions" :key="c" :label="c" :value="c" />
            </el-select>
            <el-input v-model="mtQuery.keyword" placeholder="号码 / 消息ID" clearable style="width: 180px;" />
            <el-button type="primary" @click="handleMtSearch">查询</el-button>
            <el-button @click="handleMtReset">重置</el-button>
          </div>

          <el-table :data="mtList" stripe v-loading="mtLoading" style="font-size: 13px;"
            :header-cell-style="headerStyle">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="toNumber" label="目标号码" width="150" />
            <el-table-column prop="countryCode" label="国家" width="70" />
            <el-table-column prop="sid" label="SID" width="120" />
            <el-table-column label="内容" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.content || '（内容已加密）' }}</template>
            </el-table-column>
            <el-table-column prop="segments" label="段数" width="60" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="费用" width="90">
              <template #default="{ row }">
                ${{ row.price ? (parseFloat(row.price) * (row.segments || 1)).toFixed(4) : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination v-model:current-page="mtQuery.page" v-model:page-size="mtQuery.size"
            :total="mtTotal" :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next" @change="loadMt"
            style="margin-top: 16px; justify-content: flex-end;" />
        </el-tab-pane>

        <!-- Tab 2: MO 上行记录 -->
        <el-tab-pane label="上行记录 MO" name="mo">
          <div class="filter-bar">
            <el-date-picker v-model="moQuery.startTime" type="datetime" placeholder="开始时间"
              value-format="YYYY-MM-DD HH:mm:ss" clearable style="width: 180px;" />
            <el-date-picker v-model="moQuery.endTime" type="datetime" placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss" clearable style="width: 180px;" />
            <el-button type="primary" @click="loadMo">查询</el-button>
            <el-button @click="moQuery.startTime = moQuery.endTime = ''">重置</el-button>
          </div>

          <el-table :data="moList" stripe v-loading="moLoading" style="font-size: 13px;"
            :header-cell-style="headerStyle">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="fromNumber" label="来源号码" width="150" />
            <el-table-column prop="toSid" label="SID" width="130" />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="countryCode" label="国家" width="70" />
            <el-table-column label="关键词" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.matchKeyword" size="small" type="info">{{ row.matchKeyword }}</el-tag>
                <span v-else style="color: #bbb;">无匹配</span>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="处理动作" width="120" />
          </el-table>

          <el-pagination v-model:current-page="moQuery.page" v-model:page-size="moQuery.size"
            :total="moTotal" :page-sizes="[20, 50]"
            layout="total, sizes, prev, pager, next" @change="loadMo"
            style="margin-top: 16px; justify-content: flex-end;" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 消息详情弹窗 -->
    <el-dialog v-model="detailVisible" title="消息详情" width="600px">
      <template v-if="detailRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="消息ID" :span="2">{{ detailRow.messageId }}</el-descriptions-item>
          <el-descriptions-item label="发送时间">{{ detailRow.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="送达时间">{{ detailRow.deliverAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="目标号码">{{ detailRow.toNumber }}</el-descriptions-item>
          <el-descriptions-item label="国家">{{ detailRow.countryCode }}</el-descriptions-item>
          <el-descriptions-item label="SID">{{ detailRow.sid }}</el-descriptions-item>
          <el-descriptions-item label="编码">{{ detailRow.encoding === 0 ? 'GSM-7' : 'UCS-2' }}</el-descriptions-item>
          <el-descriptions-item label="段数">{{ detailRow.segments }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailRow.status)" size="small">{{ statusLabel(detailRow.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="费用">
            ${{ detailRow.price ? (parseFloat(detailRow.price) * (detailRow.segments || 1)).toFixed(6) : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="错误码" v-if="detailRow.errorCode">{{ detailRow.errorCode }}</el-descriptions-item>
          <el-descriptions-item label="客户参考" v-if="detailRow.clientRef">{{ detailRow.clientRef }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { messageList, moList as apiMoList } from '../api'

const activeTab = ref('mt')
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

const statusType  = s => ({ DELIVERED: 'success', SUBMITTED: 'warning', FAILED: 'danger', ACCEPTED: 'info', EXPIRED: '' }[s] || 'info')
const statusLabel = s => ({ DELIVERED: '已送达', SUBMITTED: '发送中', FAILED: '失败', ACCEPTED: '待处理', EXPIRED: '已过期' }[s] || s)

// MT
const mtLoading = ref(false)
const mtList    = ref([])
const mtTotal   = ref(0)
const countryOptions = ref([])
const mtQuery = reactive({ page: 1, size: 20, startTime: '', endTime: '', status: '', countryCode: '', keyword: '' })

const loadMt = async () => {
  mtLoading.value = true
  try {
    const params = { page: mtQuery.page, size: mtQuery.size }
    if (mtQuery.startTime)   params.startTime   = mtQuery.startTime
    if (mtQuery.endTime)     params.endTime     = mtQuery.endTime
    if (mtQuery.status)      params.status      = mtQuery.status
    if (mtQuery.countryCode) params.countryCode = mtQuery.countryCode
    if (mtQuery.keyword)     params.keyword     = mtQuery.keyword
    const res = await messageList(params)
    mtList.value  = res.data?.list || []
    mtTotal.value = res.data?.total || 0
    // collect country options
    const codes = [...new Set(mtList.value.map(r => r.countryCode).filter(Boolean))]
    countryOptions.value = [...new Set([...countryOptions.value, ...codes])]
  } finally {
    mtLoading.value = false
  }
}

const handleMtSearch = () => { mtQuery.page = 1; loadMt() }
const handleMtReset  = () => {
  Object.assign(mtQuery, { page: 1, startTime: '', endTime: '', status: '', countryCode: '', keyword: '' })
  loadMt()
}

// MO
const moLoading = ref(false)
const moList    = ref([])
const moTotal   = ref(0)
const moQuery   = reactive({ page: 1, size: 20, startTime: '', endTime: '' })

const loadMo = async () => {
  moLoading.value = true
  try {
    const params = { page: moQuery.page, size: moQuery.size }
    if (moQuery.startTime) params.startTime = moQuery.startTime
    if (moQuery.endTime)   params.endTime   = moQuery.endTime
    const res = await apiMoList(params)
    moList.value  = res.data?.list || []
    moTotal.value = res.data?.total || 0
  } finally {
    moLoading.value = false
  }
}

const onTabChange = tab => { if (tab === 'mo' && moList.value.length === 0) loadMo() }

// Detail
const detailVisible = ref(false)
const detailRow     = ref(null)
const showDetail = row => { detailRow.value = row; detailVisible.value = true }

onMounted(loadMt)
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0; }
.filter-bar  { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
</style>
