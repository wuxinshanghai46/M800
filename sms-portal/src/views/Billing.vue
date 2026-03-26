<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">账单</h2>
    </div>

    <!-- 账户余额卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;" v-loading="accountLoading">
      <el-col :span="6">
        <div class="balance-card">
          <div class="bc-label">可用余额</div>
          <div class="bc-value" style="color: #52c41a;">${{ fmt(account.balance) }}</div>
          <div class="bc-sub">货币: {{ account.currency || 'USD' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="balance-card">
          <div class="bc-label">冻结金额</div>
          <div class="bc-value" style="color: #faad14;">${{ fmt(account.frozenAmount) }}</div>
          <div class="bc-sub">待确认账单中</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="balance-card">
          <div class="bc-label">付费类型</div>
          <div class="bc-value" style="font-size: 20px;">{{ paymentLabel }}</div>
          <div class="bc-sub">{{ account.paymentType === 'POSTPAID' ? `信用额度: $${fmt(account.creditLimit)}` : '预存制' }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">

        <!-- Tab 1: 充值/扣费流水 -->
        <el-tab-pane label="账单流水" name="tx">
          <div class="filter-bar">
            <el-date-picker v-model="txQuery.startTime" type="date" placeholder="开始日期"
              value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-date-picker v-model="txQuery.endTime" type="date" placeholder="结束日期"
              value-format="YYYY-MM-DD" clearable style="width: 150px;" />
            <el-button type="primary" @click="loadTx">查询</el-button>
            <el-button @click="txQuery.startTime = txQuery.endTime = ''">重置</el-button>
          </div>

          <el-table :data="txList" stripe v-loading="txLoading" style="font-size: 13px;"
            :header-cell-style="headerStyle">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === 'RECHARGE' ? 'success' : 'warning'" size="small">
                  {{ row.type === 'RECHARGE' ? '充值' : row.type === 'DEDUCT' ? '扣费' : '退款' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">
                <span :style="{ color: row.type === 'RECHARGE' ? '#52c41a' : '#ff4d4f' }">
                  {{ row.type === 'RECHARGE' ? '+' : '-' }}${{ fmt(row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="余额" width="110">
              <template #default="{ row }">${{ fmt(row.balanceAfter) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
          </el-table>

          <el-pagination v-model:current-page="txQuery.page" v-model:page-size="txQuery.size"
            :total="txTotal" layout="total, prev, pager, next" @change="loadTx"
            style="margin-top: 16px; justify-content: flex-end;" />
        </el-tab-pane>

        <!-- Tab 2: 账单列表 -->
        <el-tab-pane label="月账单" name="bill">
          <el-table :data="billList" stripe v-loading="billLoading" style="font-size: 13px;"
            :header-cell-style="headerStyle">
            <el-table-column prop="billNo" label="账单号" width="180" />
            <el-table-column label="账期" width="160">
              <template #default="{ row }">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
            </el-table-column>
            <el-table-column label="总金额" width="120">
              <template #default="{ row }">
                ${{ fmt(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="totalMessages" label="发送条数" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="billStatusType(row.status)" size="small">{{ billStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开票日期" width="120">
              <template #default="{ row }">{{ row.issuedAt ? row.issuedAt.substring(0, 10) : '—' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { billingAccount, billingTxList, billingBillList } from '../api'

const activeTab = ref('tx')
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

const fmt = v => v != null ? Number(v).toFixed(2) : '0.00'

// Account
const accountLoading = ref(false)
const account = ref({})
const paymentLabel = computed(() => account.value.paymentType === 'POSTPAID' ? '后付费' : '预付费')

// TX
const txLoading = ref(false)
const txList    = ref([])
const txTotal   = ref(0)
const txQuery   = reactive({ page: 1, size: 20, startTime: '', endTime: '' })

const loadTx = async () => {
  txLoading.value = true
  try {
    const params = { page: txQuery.page, size: txQuery.size }
    if (txQuery.startTime) params.startTime = txQuery.startTime
    if (txQuery.endTime)   params.endTime   = txQuery.endTime
    const res = await billingTxList(params)
    txList.value  = res.data?.list || []
    txTotal.value = res.data?.total || 0
  } finally {
    txLoading.value = false
  }
}

// Bill
const billLoading = ref(false)
const billList    = ref([])

const billStatusType  = s => ({ issued: 'warning', paid: 'success', draft: 'info' }[s] || 'info')
const billStatusLabel = s => ({ issued: '待付款', paid: '已付款', draft: '草稿' }[s] || s)

const loadBill = async () => {
  billLoading.value = true
  try {
    const res = await billingBillList({ page: 1, size: 50 })
    billList.value = res.data?.list || []
  } finally {
    billLoading.value = false
  }
}

const onTabChange = tab => { if (tab === 'bill' && billList.value.length === 0) loadBill() }

onMounted(async () => {
  accountLoading.value = true
  try {
    const res = await billingAccount()
    account.value = res.data || {}
  } finally {
    accountLoading.value = false
  }
  loadTx()
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0; }
.filter-bar  { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }

.balance-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);
}
.bc-label { font-size: 13px; color: #999; margin-bottom: 8px; }
.bc-value { font-size: 26px; font-weight: 600; line-height: 1.2; }
.bc-sub   { font-size: 12px; color: #bbb; margin-top: 8px; }
</style>
