<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">发送量估算</h2>
      <p class="page-desc">输入预算金额，查看各国家可发送的短信数量</p>
    </div>

    <!-- 预算输入 -->
    <el-card shadow="never" class="budget-card">
      <div class="budget-row">
        <span class="budget-label">预算金额</span>
        <el-input-number
          v-model="budget"
          :min="0"
          :precision="2"
          :step="100"
          style="width: 220px;"
          placeholder="请输入预算"
          @change="calculate"
        />
        <span class="currency-tag">{{ mainCurrency }}</span>
        <el-divider direction="vertical" />
        <span class="balance-hint">
          账户余额：<strong>${{ formatMoney(authStore.balance) }}</strong>
          <el-button link type="primary" style="margin-left:8px;" @click="useBalance">用余额估算</el-button>
        </span>
      </div>
    </el-card>

    <!-- 结果表格 -->
    <el-card shadow="never" style="margin-top: 16px;">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span style="font-weight:600;">各国家可发送数量</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索国家"
            clearable
            style="width: 200px;"
            :prefix-icon="Search"
          />
        </div>
      </template>

      <el-table
        :data="filteredRows"
        v-loading="loading"
        :header-cell-style="headerStyle"
        stripe
        style="font-size: 13px;"
      >
        <el-table-column label="国家" min-width="160">
          <template #default="{ row }">
            <span class="country-flag">{{ row.countryCode }}</span>
            <span style="margin-left:8px;">{{ row.countryName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="短信类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ row.smsAttribute || '通用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单价 / 条" width="130" align="right">
          <template #default="{ row }">
            <span class="price-text">{{ row.currency }} {{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="计费方式" width="120" align="center">
          <template #default="{ row }">
            <span>{{ billingModeLabel(row.billingMode) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可发送数量" width="160" align="right">
          <template #default="{ row }">
            <span v-if="budget > 0 && row.price > 0" class="count-text">
              {{ formatCount(row.count) }} 条
            </span>
            <span v-else style="color:#d1d5db;">—</span>
          </template>
        </el-table-column>
        <el-table-column label="预计消耗" width="150" align="right">
          <template #default="{ row }">
            <span v-if="budget > 0 && row.price > 0" style="color:#6b7280; font-size:12px;">
              {{ row.currency }} {{ formatMoney(Math.min(budget, row.count * row.price)) }}
            </span>
            <span v-else style="color:#d1d5db;">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rows.length === 0" description="暂无国家报价，请联系客户经理开通" :image-size="80" />
    </el-card>

    <!-- 汇总 -->
    <el-card v-if="budget > 0 && rows.length > 0" shadow="never" style="margin-top: 16px;">
      <div class="summary-row">
        <div class="summary-item">
          <div class="summary-num">{{ rows.length }}</div>
          <div class="summary-label">已开通国家数</div>
        </div>
        <div class="summary-item">
          <div class="summary-num highlight">{{ formatCount(totalCount) }}</div>
          <div class="summary-label">总可发送条数</div>
        </div>
        <div class="summary-item">
          <div class="summary-num">{{ mainCurrency }} {{ formatMoney(budget) }}</div>
          <div class="summary-label">预算金额</div>
        </div>
        <div class="summary-item">
          <div class="summary-num">{{ avgPrice }}</div>
          <div class="summary-label">平均单价</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { countryPrices } from '../api'
import { useAuthStore } from '../stores/auth'
import isoCountries from '../data/countries'

const authStore = useAuthStore()
const loading = ref(false)
const budget = ref(0)
const searchKeyword = ref('')
const rawPrices = ref([])

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

const localMap = Object.fromEntries(isoCountries.map(c => [c.code, c]))

const rows = computed(() => {
  return rawPrices.value.map(p => {
    const local = localMap[p.countryCode]
    const price = Number(p.price) || 0
    const count = price > 0 ? Math.floor(budget.value / price) : 0
    return {
      countryCode: p.countryCode,
      countryName: local ? local.name : p.countryCode,
      price,
      currency: p.currency || 'USD',
      smsAttribute: p.smsAttribute,
      billingMode: p.billingMode,
      count,
    }
  })
})

const filteredRows = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter(r =>
    r.countryCode.toLowerCase().includes(kw) ||
    r.countryName.toLowerCase().includes(kw)
  )
})

const totalCount = computed(() => rows.value.reduce((s, r) => s + r.count, 0))

const mainCurrency = computed(() => {
  if (rawPrices.value.length === 0) return 'USD'
  return rawPrices.value[0].currency || 'USD'
})

const avgPrice = computed(() => {
  if (rows.value.length === 0) return '—'
  const sum = rows.value.reduce((s, r) => s + r.price, 0)
  return `${mainCurrency.value} ${(sum / rows.value.length).toFixed(4)}`
})

const billingModeLabel = mode => ({ SUBMIT: '提交计费', DELIVERED: '送达计费', REACHED: '到达计费' }[mode] || mode || '—')

const formatMoney = v => Number(v || 0).toFixed(2)
const formatCount = v => v.toLocaleString()

const calculate = () => { /* reactive, no-op */ }

const useBalance = () => {
  budget.value = Number(authStore.balance || 0)
}

const loadPrices = async () => {
  loading.value = true
  try {
    const res = await countryPrices()
    rawPrices.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadPrices)
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0 0 4px; }
.page-desc   { font-size: 13px; color: #6b7280; margin: 0; }

.budget-card { margin-bottom: 0; }
.budget-row  { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.budget-label { font-size: 14px; font-weight: 500; color: #374151; white-space: nowrap; }
.currency-tag { font-size: 14px; font-weight: 600; color: #1890ff; }
.balance-hint { font-size: 13px; color: #6b7280; }

.country-flag { display: inline-block; background: #f0f2f5; border-radius: 3px; padding: 0 6px; font-size: 12px; font-weight: 600; color: #374151; }
.price-text   { font-weight: 500; color: #374151; }
.count-text   { font-weight: 700; color: #1890ff; font-size: 14px; }

.summary-row  { display: flex; gap: 0; }
.summary-item { flex: 1; text-align: center; padding: 12px 0; border-right: 1px solid #f0f2f5; }
.summary-item:last-child { border-right: none; }
.summary-num  { font-size: 22px; font-weight: 700; color: #374151; }
.summary-num.highlight { color: #1890ff; }
.summary-label { font-size: 12px; color: #9ca3af; margin-top: 4px; }
</style>
