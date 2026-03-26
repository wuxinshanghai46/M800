<template>
  <div>
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-title">概览 Dashboard</div>
      <div class="page-actions">
        <el-button
          v-for="r in ranges" :key="r.key"
          :type="activeRange === r.key ? 'primary' : ''"
          size="small"
          @click="activeRange = r.key"
        >{{ r.label }}</el-button>
      </div>
    </div>

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-title">今日发送量</div>
        <div class="stat-value">{{ formatNum(stats.totalSent ?? 128456) }}</div>
        <div class="stat-footer"><span class="up">+12.5%</span> 较昨日同期</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">当前 TPS</div>
        <div class="stat-value">
          {{ stats.currentTps ?? 86 }}<span class="stat-sub"> / {{ stats.maxTps ?? 200 }}</span>
        </div>
        <div class="stat-footer">峰值 TPS: {{ stats.peakTps ?? 156 }} ({{ stats.peakTpsTime ?? '10:32' }})</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">整体送达率</div>
        <div class="stat-value" style="color: #16a34a;">{{ stats.deliveryRate ?? 96.5 }}%</div>
        <div class="stat-footer"><span class="up">+0.3%</span> 较昨日</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">活跃通道</div>
        <div class="stat-value">
          {{ stats.activeChannels ?? 12 }}<span class="stat-sub"> / {{ stats.totalChannels ?? 15 }}</span>
        </div>
        <div class="stat-footer">
          <span class="down">{{ stats.offlineChannels ?? 3 }}个通道离线</span>
        </div>
      </div>
    </div>

    <!-- Charts 2x2 Grid -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-title">近24小时发送趋势</div>
        <div ref="trendChartRef" style="height: 280px;"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">送达率趋势（按通道）</div>
        <div ref="deliveryChartRef" style="height: 280px;"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">国家发送分布 Top15</div>
        <div ref="countryChartRef" style="height: 280px;"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">错误码分布</div>
        <div ref="errorChartRef" style="height: 280px;"></div>
      </div>
    </div>

    <!-- Alerts -->
    <div class="card">
      <div class="card-title">最新告警</div>
      <div class="alert-list">
        <div
          v-for="(a, i) in alerts" :key="i"
          class="alert-row"
        >
          <span :class="['alert-level', a.level]"></span>
          <el-tag
            :type="a.level === 'critical' ? 'danger' : 'warning'"
            size="small"
            effect="light"
            style="font-size: 11px;"
          >{{ a.level === 'critical' ? 'Critical' : 'Warning' }}</el-tag>
          <span class="alert-msg">{{ a.msg }}</span>
          <span class="alert-source">{{ a.source }}</span>
          <span class="alert-time">{{ a.time }}</span>
        </div>
      </div>
    </div>

    <!-- Finance Cards -->
    <div class="finance-cards">
      <div class="finance-card">
        <div class="fc-label">今日收入</div>
        <div class="fc-value income">${{ formatMoney(stats.totalRevenue ?? 4521.30) }}</div>
      </div>
      <div class="finance-card">
        <div class="fc-label">今日成本</div>
        <div class="fc-value cost">${{ formatMoney(stats.totalCost ?? 2876.50) }}</div>
      </div>
      <div class="finance-card">
        <div class="fc-label">今日利润</div>
        <div class="fc-value profit">${{ formatMoney((stats.totalRevenue ?? 4521.30) - (stats.totalCost ?? 2876.50)) }}</div>
      </div>
      <div class="finance-card">
        <div class="fc-label">利润率</div>
        <div class="fc-value rate">{{ stats.profitRate ?? 36.4 }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statsDashboard, statsByCountry, statsByCustomer } from '../api'

/* ---- reactive state ---- */
const activeRange = ref('today')
const ranges = [
  { key: 'today', label: '今日' },
  { key: '7d', label: '近7天' },
  { key: '30d', label: '近30天' },
]

const stats = ref({})

const alerts = ref([
  { level: 'critical', msg: '通道 CN-CMPP-01 送达率降至 82.3%，低于阈值 90%', source: '通道监控', time: '2 分钟前' },
  { level: 'warning', msg: '供应商 GlobalConnect 账户余额不足 $500，请及时充值', source: '财务预警', time: '15 分钟前' },
  { level: 'critical', msg: '通道 ID-HTTP-03 连接超时，已自动切换至备用通道', source: '通道监控', time: '28 分钟前' },
  { level: 'warning', msg: '客户 TechCorp 当前 TPS 达到限速阈值 80%（48/60）', source: '流控预警', time: '45 分钟前' },
  { level: 'warning', msg: '印度尼西亚地区错误码 EC_1003 占比上升至 8.2%', source: '质量监控', time: '1 小时前' },
])

/* ---- chart refs ---- */
const trendChartRef = ref(null)
const deliveryChartRef = ref(null)
const countryChartRef = ref(null)
const errorChartRef = ref(null)
let charts = []

/* ---- helpers ---- */
const formatNum = v => {
  if (v === undefined || v === null) return '0'
  return Number(v).toLocaleString()
}
const formatMoney = v => {
  if (v === undefined || v === null) return '0.00'
  return Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/* ---- colors ---- */
const C = { primary: '#2563eb', success: '#16a34a', warning: '#d97706', danger: '#dc2626' }

/* ---- chart builders ---- */
function buildTrendChart(el) {
  const c = echarts.init(el)
  const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
  const data = [3200, 2800, 2100, 1800, 1500, 1900, 3500, 5800, 7200, 8600, 9100, 8800, 8200, 7600, 7900, 8400, 9200, 9800, 8900, 7400, 6100, 5200, 4300, 3600]
  c.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hours, axisLabel: { fontSize: 11, color: '#6b7280' }, axisLine: { lineStyle: { color: '#e5e7eb' } } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#6b7280' }, splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }, axisLine: { show: false } },
    series: [{ type: 'line', data, smooth: true, symbol: 'none', lineStyle: { width: 2, color: C.primary }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(37,99,235,0.25)' }, { offset: 1, color: 'rgba(37,99,235,0.02)' }]) } }],
    grid: { left: 50, right: 16, bottom: 30, top: 12 },
  })
  return c
}

function buildDeliveryChart(el) {
  const c = echarts.init(el)
  const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
  const channels = [
    { name: 'CN-CMPP-01', color: C.primary, data: Array.from({ length: 24 }, () => 90 + Math.random() * 8) },
    { name: 'ID-HTTP-03', color: C.success, data: Array.from({ length: 24 }, () => 88 + Math.random() * 10) },
    { name: 'IN-SMPP-02', color: C.warning, data: Array.from({ length: 24 }, () => 85 + Math.random() * 12) },
  ]
  c.setOption({
    tooltip: { trigger: 'axis', valueFormatter: v => v.toFixed(1) + '%' },
    legend: { data: channels.map(ch => ch.name), top: 0, textStyle: { fontSize: 11, color: '#6b7280' } },
    xAxis: { type: 'category', data: hours, axisLabel: { fontSize: 11, color: '#6b7280' }, axisLine: { lineStyle: { color: '#e5e7eb' } } },
    yAxis: { type: 'value', min: 80, max: 100, axisLabel: { fontSize: 11, color: '#6b7280', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }, axisLine: { show: false } },
    series: channels.map(ch => ({ name: ch.name, type: 'line', data: ch.data.map(v => +v.toFixed(1)), smooth: true, symbol: 'none', lineStyle: { width: 2, color: ch.color } })),
    grid: { left: 50, right: 16, bottom: 30, top: 36 },
  })
  return c
}

function buildCountryChart(el, apiData) {
  const c = echarts.init(el)
  let names, vals
  if (apiData && apiData.length) {
    const top = apiData.slice(0, 15)
    names = top.map(d => d.dimensionValue || '-')
    vals = top.map(d => d.segments || 0)
  } else {
    names = ['中国', '印尼', '印度', '菲律宾', '泰国', '马来西亚', '越南', '巴西', '尼日利亚', '墨西哥', '巴基斯坦', '孟加拉', '埃及', '南非', '肯尼亚']
    vals = [38200, 22400, 18600, 12800, 11200, 9800, 8400, 7200, 6800, 5400, 4200, 3800, 3200, 2800, 2100]
  }
  c.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 35, fontSize: 10, color: '#6b7280' }, axisLine: { lineStyle: { color: '#e5e7eb' } } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#6b7280' }, splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }, axisLine: { show: false } },
    series: [{ type: 'bar', data: vals, barWidth: '55%', itemStyle: { borderRadius: [4, 4, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#2563eb' }, { offset: 1, color: '#93c5fd' }]) } }],
    grid: { left: 55, right: 16, bottom: 60, top: 12 },
  })
  return c
}

function buildErrorChart(el) {
  const c = echarts.init(el)
  const data = [
    { name: 'EC_1001 号码无效', value: 320 },
    { name: 'EC_1002 运营商拒绝', value: 180 },
    { name: 'EC_1003 超时', value: 140 },
    { name: 'EC_1004 内容违规', value: 90 },
    { name: 'EC_1005 频率超限', value: 60 },
    { name: '其他', value: 45 },
  ]
  c.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 11, color: '#6b7280' } },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['35%', '50%'], label: { show: false },
      data, itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      color: [C.danger, C.warning, '#f59e0b', '#8b5cf6', C.primary, '#9ca3af'],
    }],
  })
  return c
}

/* ---- lifecycle ---- */
onMounted(async () => {
  // fetch dashboard stats
  try { const res = await statsDashboard(); stats.value = res.data || {} } catch (e) { /* use defaults */ }

  // fetch country data for bar chart
  let countryData = []
  try {
    const today = new Date()
    const end = today.toISOString().slice(0, 10)
    const start = new Date(today.getTime() - 30 * 86400000).toISOString().slice(0, 10)
    const r = await statsByCountry({ start, end })
    countryData = r.data || []
  } catch (e) { /* use placeholder */ }

  await nextTick()

  // init all 4 charts
  if (trendChartRef.value) charts.push(buildTrendChart(trendChartRef.value))
  if (deliveryChartRef.value) charts.push(buildDeliveryChart(deliveryChartRef.value))
  if (countryChartRef.value) charts.push(buildCountryChart(countryChartRef.value, countryData))
  if (errorChartRef.value) charts.push(buildErrorChart(errorChartRef.value))

  // resize handler
  window.addEventListener('resize', handleResize)
})

function handleResize() { charts.forEach(c => c?.resize()) }

onBeforeUnmount(() => {
  charts.forEach(c => c?.dispose())
  charts = []
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* Page Header */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}
.page-actions {
  display: flex;
  gap: 8px;
}

/* Stat Cards */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.stat-title {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #111827;
}
.stat-sub {
  font-size: 16px;
  color: #9ca3af;
  font-weight: 400;
}
.stat-footer {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}
.stat-footer .up {
  color: #16a34a;
}
.stat-footer .down {
  color: #dc2626;
}

/* Chart Grid */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.chart-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 20px;
}
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

/* Alert Card */
.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 20px;
  margin-bottom: 24px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
  color: #111827;
}
.alert-list .alert-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
  font-size: 13px;
  gap: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.alert-list .alert-row:last-child {
  border-bottom: none;
}
.alert-list .alert-row:hover {
  background: #f9fafb;
}
.alert-level {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.alert-level.critical {
  background: #dc2626;
}
.alert-level.warning {
  background: #d97706;
}
.alert-msg {
  flex: 1;
  color: #374151;
}
.alert-source {
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}
.alert-time {
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}

/* Finance Cards */
.finance-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.finance-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.fc-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
.fc-value {
  font-size: 22px;
  font-weight: 600;
}
.fc-value.income {
  color: #2563eb;
}
.fc-value.cost {
  color: #d97706;
}
.fc-value.profit {
  color: #16a34a;
}
.fc-value.rate {
  color: #7c3aed;
}

/* Responsive */
@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
  .finance-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
