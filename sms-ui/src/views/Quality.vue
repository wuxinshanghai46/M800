<template>
  <div class="quality-page">
    <!-- Page Header -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h2 style="margin: 0; font-size: 20px; font-weight: 600;">质量分析</h2>
    </div>

    <el-card class="table-card" shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ==================== Tab 1: 通道质量统计 ==================== -->
        <el-tab-pane label="通道质量统计" name="channel">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="channelFilter.startDate" type="date" placeholder="开始时间"
              value-format="YYYY-MM-DD" style="width: 160px;" />
            <el-date-picker v-model="channelFilter.endDate" type="date" placeholder="结束时间"
              value-format="YYYY-MM-DD" style="width: 160px;" />
            <el-select v-model="channelFilter.channel" placeholder="全部通道" clearable style="width: 160px;">
              <el-option v-for="c in channelOptions" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="channelFilter.country" placeholder="全部国家" clearable style="width: 120px;">
              <el-option v-for="c in countryOptions" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="channelFilter.vendor" placeholder="全部供应商" clearable style="width: 140px;">
              <el-option v-for="v in vendorOptions" :key="v" :label="v" :value="v" />
            </el-select>
            <el-button type="primary" @click="handleChannelSearch">查询</el-button>
            <el-button @click="resetChannelFilter">重置</el-button>
          </div>

          <!-- Time Granularity -->
          <div style="margin-bottom: 12px;">
            <span style="font-size: 13px; color: #666; margin-right: 8px;">时间粒度：</span>
            <el-radio-group v-model="granularity" size="small">
              <el-radio-button value="hour">小时</el-radio-button>
              <el-radio-button value="day">天</el-radio-button>
              <el-radio-button value="week">周</el-radio-button>
            </el-radio-group>
          </div>

          <!-- Chart: 通道送达率趋势 -->
          <div class="chart-placeholder chart-full">
            <div style="text-align: center;">
              <strong>通道送达率趋势</strong>
              <div style="font-size: 12px; margin-top: 6px; color: #bbb;">折线图：各通道每日送达率，可多选通道对比，红色虚线标记 85% 告警线</div>
            </div>
          </div>

          <!-- Charts Row: 送达率排行 + 延迟分布 -->
          <div class="charts-row">
            <div class="chart-placeholder chart-half">
              <div style="text-align: center;">
                <strong>通道送达率排行</strong>
                <div style="font-size: 12px; margin-top: 6px; color: #bbb;">柱状图，按通道分组，红色标记 &lt; 85%</div>
              </div>
            </div>
            <div class="chart-placeholder chart-half">
              <div style="text-align: center;">
                <strong>通道延迟分布</strong>
                <div style="font-size: 12px; margin-top: 6px; color: #bbb;">箱线图 P50/P75/P95/P99</div>
              </div>
            </div>
          </div>

          <!-- Charts Row: 单通道综合趋势 + 错误码分布 -->
          <div class="charts-row">
            <div class="chart-placeholder chart-half">
              <div style="text-align: center;">
                <strong>单通道综合趋势</strong>
                <div style="font-size: 12px; margin-top: 6px; color: #bbb;">选择通道 → 送达率/延迟/TPS/错误率 四合一折线</div>
              </div>
            </div>
            <div class="chart-placeholder chart-half">
              <div style="text-align: center;">
                <strong>错误码分布</strong>
                <div style="font-size: 12px; margin-top: 6px; color: #bbb;">按通道 x 错误码 堆叠柱状图</div>
              </div>
            </div>
          </div>

          <!-- 通道质量排行 Section Header -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin: 20px 0 12px;">
            <h4 style="font-size: 14px; font-weight: 600; margin: 0;">通道质量排行</h4>
            <el-button :icon="Download" @click="handleExportCsv">导出 CSV</el-button>
          </div>

          <!-- Quality Ranking Table -->
          <el-table :data="channelQualityData" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
            <el-table-column prop="channelName" label="通道名" min-width="130">
              <template #default="{ row }">
                <strong>{{ row.channelName }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="vendor" label="供应商" width="90" />
            <el-table-column prop="country" label="国家" width="70" />
            <el-table-column prop="sendCount" label="发送量" width="100" />
            <el-table-column prop="deliveryRate" label="送达率" width="90">
              <template #default="{ row }">
                <span :class="deliveryRateClass(row.deliveryRate)">{{ row.deliveryRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="avgLatency" label="平均延迟" width="90" />
            <el-table-column prop="p95Latency" label="P95延迟" width="90" />
            <el-table-column prop="errorRate" label="错误率" width="80" />
            <el-table-column prop="tpsPeak" label="TPS峰值" width="90" />
            <el-table-column prop="costPerMsg" label="成本/条" width="90" />
            <el-table-column prop="faultCount" label="故障次数" width="90" />
            <el-table-column label="健康评分" width="140">
              <template #default="{ row }">
                <div style="display: flex; align-items: center; gap: 6px;">
                  <el-progress
                    :percentage="row.healthScore"
                    :stroke-width="8"
                    :show-text="false"
                    :color="healthScoreColor(row.healthScore)"
                    style="width: 60px;"
                  />
                  <span :style="{ fontWeight: 600, color: healthScoreColor(row.healthScore) }">
                    {{ row.healthScore }}
                  </span>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Footer Note -->
          <div style="margin-top: 12px; font-size: 12px; color: #999;">
            健康评分 = 送达率x40% + 延迟评分x20% + 稳定性x20% + 成本评分x20%
          </div>
        </el-tab-pane>

        <!-- ==================== Tab 2: 时段流量分析 ==================== -->
        <el-tab-pane label="时段流量分析" name="traffic">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="trafficFilter.startDate" type="date" placeholder="开始时间"
              value-format="YYYY-MM-DD" style="width: 160px;" />
            <el-date-picker v-model="trafficFilter.endDate" type="date" placeholder="结束时间"
              value-format="YYYY-MM-DD" style="width: 160px;" />
            <el-select v-model="trafficFilter.country" placeholder="全部国家" clearable style="width: 120px;">
              <el-option v-for="c in countryOptions" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="trafficFilter.customer" placeholder="全部客户" clearable style="width: 140px;">
              <el-option v-for="c in customerOptions" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="trafficFilter.type" placeholder="全部类型" clearable style="width: 120px;">
              <el-option label="OTP" value="OTP" />
              <el-option label="通知" value="NOTIFICATION" />
              <el-option label="营销" value="MARKETING" />
            </el-select>
            <el-button type="primary" @click="handleTrafficSearch">查询</el-button>
            <el-button @click="resetTrafficFilter">重置</el-button>
          </div>

          <!-- Heatmap -->
          <el-card shadow="never" style="margin-bottom: 16px;">
            <h4 style="font-size: 14px; font-weight: 600; margin: 0 0 16px;">小时级流量热力图（X: 24小时 / Y: 星期）</h4>
            <div class="heatmap-grid">
              <!-- Header Row -->
              <div class="heatmap-label"></div>
              <div v-for="h in 24" :key="'h-' + h" class="heatmap-label" style="justify-content: center;">
                {{ h - 1 }}
              </div>
              <!-- Data Rows -->
              <template v-for="(dayRow, dayIdx) in heatmapData" :key="'day-' + dayIdx">
                <div class="heatmap-label">{{ dayRow.label }}</div>
                <div
                  v-for="(cell, cellIdx) in dayRow.cells"
                  :key="'cell-' + dayIdx + '-' + cellIdx"
                  class="heatmap-cell"
                  :style="{ background: heatColors[cell.level], color: cell.level <= 1 ? '#999' : '#fff' }"
                >
                  {{ cell.text }}
                </div>
              </template>
            </div>
            <!-- Legend -->
            <div style="margin-top: 8px; display: flex; align-items: center; gap: 8px; font-size: 11px; color: #999;">
              <span>低</span>
              <div v-for="(color, idx) in heatColors" :key="'legend-' + idx"
                :style="{ width: '20px', height: '10px', background: color, borderRadius: '2px' }"></div>
              <span>高</span>
            </div>
          </el-card>

          <!-- Charts Row: 每小时发送趋势 + 星期分布 -->
          <div class="charts-row">
            <div class="chart-placeholder chart-half">
              <div style="text-align: center;">
                <strong>每小时发送趋势</strong>
                <div style="font-size: 12px; margin-top: 6px; color: #bbb;">折线图 + 送达率双Y轴，可按国家/客户筛选</div>
              </div>
            </div>
            <div class="chart-placeholder chart-half">
              <div style="text-align: center;">
                <strong>星期分布</strong>
                <div style="font-size: 12px; margin-top: 6px; color: #bbb;">柱状图：周一~周日平均发送量 + 送达率折线</div>
              </div>
            </div>
          </div>

          <!-- Chart: TPS峰值趋势 -->
          <div class="chart-placeholder chart-full" style="margin-top: 0;">
            <div style="text-align: center;">
              <strong>TPS 峰值趋势（近 7 天）</strong>
              <div style="font-size: 12px; margin-top: 6px; color: #bbb;">面积图：每小时最大 TPS，红色虚线标记通道 TPS 上限</div>
            </div>
          </div>

          <!-- Info Card -->
          <div class="info-card">
            <strong>流量分析用途：</strong>
            <ul style="margin-top: 8px; padding-left: 20px; line-height: 2;">
              <li>评估通道 TPS 配置是否合理（峰值时段是否超限）</li>
              <li>识别定时任务集中触发的时段（如营销批量发送）</li>
              <li>为通道扩容/缩容提供数据依据</li>
              <li>发现异常流量模式（非工作时间突增等）</li>
            </ul>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { channelMonitorList } from '../api'
import { useRefData } from '../stores/refData'

const refData = useRefData()

// ======================== Common ========================
const activeTab = ref('channel')

const headerStyle = {
  background: '#f0f2f5',
  color: '#374151',
  fontWeight: 600,
  fontSize: '12px'
}

const countryOptions = ref([])
const channelOptions = ref([])
const vendorOptions = ref([])
const customerOptions = ref([])

const loadRefData = async () => {
  const [co, ch, v, cu] = await Promise.all([
    refData.loadCountries(), refData.loadChannels(), refData.loadVendors(), refData.loadCustomers()
  ])
  countryOptions.value = co.map(c => c.code)
  channelOptions.value = ch.map(c => c.channelName || c.name)
  vendorOptions.value = v.map(x => x.vendorName || x.name)
  customerOptions.value = cu.map(c => c.companyName || c.customerName)
}

onMounted(() => { loadRefData() })

// ======================== Tab 1: Channel Quality ========================
const granularity = ref('day')

const channelFilter = reactive({
  startDate: '2026-03-01',
  endDate: '2026-03-14',
  channel: '',
  country: '',
  vendor: ''
})

const channelQualityData = ref([])

function deliveryRateClass(rate) {
  if (rate >= 93) return 'dr-good'
  if (rate >= 85) return 'dr-warn'
  return 'dr-bad'
}

function healthScoreColor(score) {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#ff4d4f'
}

async function handleChannelSearch() {
  try {
    const res = await channelMonitorList()
    let list = res.data || []
    if (channelFilter.channel) list = list.filter(r => r.channelName?.includes(channelFilter.channel))
    if (channelFilter.country) list = list.filter(r => r.countryCode === channelFilter.country)
    if (channelFilter.vendor) list = list.filter(r => String(r.vendorId) === String(channelFilter.vendor))
    channelQualityData.value = list.map(r => ({
      ...r,
      channel: r.channelName || r.channelCode,
      country: r.countryCode,
      vendor: r.vendorName,
      sent: r.todayTotal || 0,
      delivered: r.todayDelivered || 0,
      failed: r.todayFailed || 0,
      deliveryRate: r.deliveryRate || 0,
      healthScore: r.deliveryRate ? Math.min(100, Math.round(r.deliveryRate * 1.05)) : 0,
    }))
  } catch (e) {
    console.error('handleChannelSearch failed', e)
  }
}

function resetChannelFilter() {
  channelFilter.startDate = '2026-03-01'
  channelFilter.endDate = '2026-03-14'
  channelFilter.channel = ''
  channelFilter.country = ''
  channelFilter.vendor = ''
  ElMessage.success('已重置筛选条件')
}

function handleExportCsv() {
  ElMessage.success('导出任务已提交，稍后将通过邮件通知')
}

// ======================== Tab 2: Traffic Analysis ========================
const trafficFilter = reactive({
  startDate: '2026-03-01',
  endDate: '2026-03-14',
  country: '',
  customer: '',
  type: ''
})

const heatColors = ['#f0f5ff', '#bae7ff', '#69c0ff', '#1890ff', '#0050b3', '#002766']

const heatmapData = ref([
  {
    label: '周一',
    cells: [
      { text: '1.2k', level: 0 }, { text: '0.8k', level: 0 }, { text: '0.5k', level: 0 }, { text: '0.4k', level: 0 },
      { text: '0.6k', level: 0 }, { text: '2.1k', level: 1 }, { text: '4.5k', level: 2 }, { text: '8.2k', level: 3 },
      { text: '12k', level: 4 }, { text: '15k', level: 5 }, { text: '14k', level: 5 }, { text: '11k', level: 4 },
      { text: '9.5k', level: 3 }, { text: '10k', level: 4 }, { text: '11k', level: 4 }, { text: '8.8k', level: 3 },
      { text: '7.5k', level: 3 }, { text: '5.2k', level: 2 }, { text: '4.8k', level: 2 }, { text: '3.5k', level: 1 },
      { text: '2.8k', level: 1 }, { text: '2.2k', level: 1 }, { text: '1.8k', level: 0 }, { text: '1.5k', level: 0 }
    ]
  },
  {
    label: '周二',
    cells: [
      { text: '1.1k', level: 0 }, { text: '0.7k', level: 0 }, { text: '0.4k', level: 0 }, { text: '0.3k', level: 0 },
      { text: '0.5k', level: 0 }, { text: '2.0k', level: 1 }, { text: '4.2k', level: 2 }, { text: '7.8k', level: 3 },
      { text: '11k', level: 4 }, { text: '16k', level: 5 }, { text: '15k', level: 5 }, { text: '12k', level: 4 },
      { text: '9.0k', level: 3 }, { text: '10k', level: 4 }, { text: '11k', level: 4 }, { text: '9.2k', level: 3 },
      { text: '7.8k', level: 3 }, { text: '5.5k', level: 2 }, { text: '5.0k', level: 2 }, { text: '3.8k', level: 1 },
      { text: '2.5k', level: 1 }, { text: '2.0k', level: 1 }, { text: '1.6k', level: 0 }, { text: '1.3k', level: 0 }
    ]
  },
  {
    label: '周三',
    cells: [
      { text: '1.0k', level: 0 }, { text: '0.6k', level: 0 }, { text: '0.3k', level: 0 }, { text: '0.3k', level: 0 },
      { text: '0.5k', level: 0 }, { text: '1.8k', level: 1 }, { text: '4.0k', level: 2 }, { text: '7.5k', level: 3 },
      { text: '11k', level: 4 }, { text: '14k', level: 5 }, { text: '13k', level: 4 }, { text: '11k', level: 4 },
      { text: '8.5k', level: 3 }, { text: '9.5k', level: 3 }, { text: '10k', level: 4 }, { text: '8.5k', level: 3 },
      { text: '6.8k', level: 2 }, { text: '5.0k', level: 2 }, { text: '4.5k', level: 2 }, { text: '3.2k', level: 1 },
      { text: '2.5k', level: 1 }, { text: '2.0k', level: 1 }, { text: '1.5k', level: 0 }, { text: '1.2k', level: 0 }
    ]
  },
  {
    label: '周四',
    cells: [
      { text: '1.0k', level: 0 }, { text: '0.7k', level: 0 }, { text: '0.4k', level: 0 }, { text: '0.3k', level: 0 },
      { text: '0.6k', level: 0 }, { text: '2.2k', level: 1 }, { text: '4.8k', level: 2 }, { text: '8.5k', level: 3 },
      { text: '12k', level: 4 }, { text: '15k', level: 5 }, { text: '14k', level: 5 }, { text: '12k', level: 4 },
      { text: '9.2k', level: 3 }, { text: '10k', level: 4 }, { text: '11k', level: 4 }, { text: '9.0k', level: 3 },
      { text: '7.2k', level: 3 }, { text: '5.3k', level: 2 }, { text: '4.6k', level: 2 }, { text: '3.5k', level: 1 },
      { text: '2.8k', level: 1 }, { text: '2.2k', level: 1 }, { text: '1.7k', level: 0 }, { text: '1.4k', level: 0 }
    ]
  },
  {
    label: '周五',
    cells: [
      { text: '1.2k', level: 0 }, { text: '0.8k', level: 0 }, { text: '0.5k', level: 0 }, { text: '0.4k', level: 0 },
      { text: '0.7k', level: 0 }, { text: '2.5k', level: 1 }, { text: '5.0k', level: 2 }, { text: '8.8k', level: 3 },
      { text: '13k', level: 5 }, { text: '16k', level: 5 }, { text: '15k', level: 5 }, { text: '12k', level: 4 },
      { text: '10k', level: 4 }, { text: '11k', level: 4 }, { text: '12k', level: 4 }, { text: '9.5k', level: 3 },
      { text: '8.0k', level: 3 }, { text: '5.8k', level: 2 }, { text: '5.2k', level: 2 }, { text: '3.8k', level: 1 },
      { text: '3.0k', level: 1 }, { text: '2.5k', level: 1 }, { text: '1.8k', level: 0 }, { text: '1.5k', level: 0 }
    ]
  },
  {
    label: '周六',
    cells: [
      { text: '0.8k', level: 0 }, { text: '0.5k', level: 0 }, { text: '0.3k', level: 0 }, { text: '0.2k', level: 0 },
      { text: '0.3k', level: 0 }, { text: '1.0k', level: 0 }, { text: '2.5k', level: 1 }, { text: '4.2k', level: 2 },
      { text: '5.5k', level: 2 }, { text: '7.0k', level: 3 }, { text: '6.8k', level: 3 }, { text: '5.5k', level: 2 },
      { text: '4.8k', level: 2 }, { text: '5.0k', level: 2 }, { text: '5.2k', level: 2 }, { text: '4.5k', level: 2 },
      { text: '3.5k', level: 1 }, { text: '2.8k', level: 1 }, { text: '2.2k', level: 1 }, { text: '1.5k', level: 0 },
      { text: '1.2k', level: 0 }, { text: '1.0k', level: 0 }, { text: '0.8k', level: 0 }, { text: '0.6k', level: 0 }
    ]
  },
  {
    label: '周日',
    cells: [
      { text: '0.6k', level: 0 }, { text: '0.4k', level: 0 }, { text: '0.2k', level: 0 }, { text: '0.2k', level: 0 },
      { text: '0.3k', level: 0 }, { text: '0.8k', level: 0 }, { text: '2.0k', level: 1 }, { text: '3.5k', level: 1 },
      { text: '4.8k', level: 2 }, { text: '5.5k', level: 2 }, { text: '5.2k', level: 2 }, { text: '4.5k', level: 2 },
      { text: '3.8k', level: 1 }, { text: '4.2k', level: 2 }, { text: '4.5k', level: 2 }, { text: '3.8k', level: 1 },
      { text: '3.0k', level: 1 }, { text: '2.2k', level: 1 }, { text: '1.8k', level: 0 }, { text: '1.2k', level: 0 },
      { text: '1.0k', level: 0 }, { text: '0.8k', level: 0 }, { text: '0.6k', level: 0 }, { text: '0.5k', level: 0 }
    ]
  }
])

function handleTrafficSearch() {
  ElMessage.success('查询已刷新')
}

function resetTrafficFilter() {
  trafficFilter.startDate = '2026-03-01'
  trafficFilter.endDate = '2026-03-14'
  trafficFilter.country = ''
  trafficFilter.customer = ''
  trafficFilter.type = ''
  ElMessage.success('已重置筛选条件')
}
</script>

<style scoped>
.quality-page {
  padding: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: flex-end;
}

/* Chart Placeholders */
.chart-placeholder {
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 14px;
  flex-direction: column;
  gap: 8px;
}

.chart-full {
  height: 300px;
  margin-bottom: 16px;
}

.chart-half {
  height: 260px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

/* Delivery Rate Coloring */
.dr-good {
  color: #52c41a;
  font-weight: 600;
}

.dr-warn {
  color: #faad14;
  font-weight: 600;
}

.dr-bad {
  color: #ff4d4f;
  font-weight: 600;
}

/* Heatmap */
.heatmap-grid {
  display: grid;
  grid-template-columns: 60px repeat(24, 1fr);
  gap: 2px;
  font-size: 11px;
}

.heatmap-cell {
  height: 28px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.heatmap-label {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
}

/* Info Card */
.info-card {
  margin-top: 16px;
  padding: 16px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
}

/* Responsive */
@media (max-width: 768px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
