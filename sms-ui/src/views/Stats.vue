<template>
  <div class="stats-page">
    <!-- Page Header -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h2 style="margin: 0; font-size: 20px; font-weight: 600;">发送统计</h2>
      <el-button type="primary" plain :icon="Download">导出报表</el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ==================== Tab 1: 国家统计 ==================== -->
        <el-tab-pane label="国家统计" name="country">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="countryFilter.dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
              style="width: 280px;" />
            <el-select v-model="countryFilter.country" placeholder="全部国家" clearable style="width: 160px;">
              <el-option v-for="c in countryOptions" :key="c.code" :label="c.name + ' (' + c.code + ')'" :value="c.code" />
            </el-select>
            <el-select v-model="countryFilter.type" placeholder="全部类型" clearable style="width: 140px;">
              <el-option label="OTP" value="OTP" />
              <el-option label="通知" value="NOTIFICATION" />
              <el-option label="营销" value="MARKETING" />
            </el-select>
            <el-button type="primary" @click="loadCountry">查询</el-button>
            <el-button @click="resetCountryFilter">重置</el-button>
          </div>

          <!-- Stat Summary Cards -->
          <div class="stat-cards">
            <div class="stat-card">
              <div class="stat-label">总发送量</div>
              <div class="stat-value" style="color: #409eff;">{{ countrySummary.totalSent }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">总送达量</div>
              <div class="stat-value" style="color: #67c23a;">{{ countrySummary.totalDelivered }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">整体送达率</div>
              <div class="stat-value" style="color: #e6a23c;">{{ countrySummary.deliveryRate }}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">涉及国家数</div>
              <div class="stat-value" style="color: #909399;">{{ countrySummary.countryCount }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">总收入</div>
              <div class="stat-value" style="color: #f56c6c;">${{ countrySummary.totalRevenue }}</div>
            </div>
          </div>

          <!-- Chart Placeholders -->
          <div class="chart-row">
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">发送量趋势图（按日期）</span>
            </div>
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">国家发送量TOP10</span>
            </div>
          </div>

          <!-- Data Table -->
          <el-table :data="countryData" stripe v-loading="loading" :header-cell-style="headerStyle"
            style="margin-top: 16px;">
            <el-table-column prop="dimensionValue" label="国家" min-width="100" />
            <el-table-column prop="sent" label="发送量" width="100" />
            <el-table-column prop="delivered" label="送达量" width="100" />
            <el-table-column prop="failed" label="失败量" width="100" />
            <el-table-column prop="deliveryRate" label="送达率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.deliveryRate >= 90 ? '#67c23a' : row.deliveryRate >= 70 ? '#e6a23c' : '#f56c6c' }">
                  {{ row.deliveryRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="segments" label="段数" width="90" />
            <el-table-column prop="avgLatency" label="平均延迟" width="100">
              <template #default="{ row }">{{ row.avgLatency || '-' }}ms</template>
            </el-table-column>
            <el-table-column prop="revenue" label="收入" width="110">
              <template #default="{ row }">${{ row.revenue || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="cost" label="成本" width="110">
              <template #default="{ row }">${{ row.cost || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="profitRate" label="利润率" width="100">
              <template #default="{ row }">
                <span :style="{ color: (row.profitRate || 0) >= 0 ? '#67c23a' : '#f56c6c' }">
                  {{ row.profitRate || '0.0' }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== Tab 2: 供应商统计 ==================== -->
        <el-tab-pane label="供应商统计" name="vendor">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="vendorFilter.dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
              style="width: 280px;" />
            <el-select v-model="vendorFilter.vendor" placeholder="全部供应商" clearable style="width: 160px;">
              <el-option v-for="v in vendorOptions" :key="v.id" :label="v.name" :value="v.id" />
            </el-select>
            <el-select v-model="vendorFilter.country" placeholder="全部国家" clearable style="width: 160px;">
              <el-option v-for="c in countryOptions" :key="c.code" :label="c.name + ' (' + c.code + ')'" :value="c.code" />
            </el-select>
            <el-button type="primary" @click="loadVendor">查询</el-button>
            <el-button @click="resetVendorFilter">重置</el-button>
          </div>

          <!-- Chart Placeholders -->
          <div class="chart-row">
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">供应商发送量对比</span>
            </div>
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">供应商送达率对比</span>
            </div>
          </div>

          <!-- Data Table -->
          <el-table :data="vendorData" stripe v-loading="loading" :header-cell-style="headerStyle"
            style="margin-top: 16px;">
            <el-table-column prop="vendorName" label="供应商" min-width="120" />
            <el-table-column prop="channelCount" label="通道数" width="90" />
            <el-table-column prop="countryCount" label="覆盖国家" width="100" />
            <el-table-column prop="sent" label="发送量" width="100" />
            <el-table-column prop="delivered" label="送达量" width="100" />
            <el-table-column prop="deliveryRate" label="送达率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.deliveryRate >= 90 ? '#67c23a' : row.deliveryRate >= 70 ? '#e6a23c' : '#f56c6c' }">
                  {{ row.deliveryRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="avgLatency" label="平均延迟" width="100">
              <template #default="{ row }">{{ row.avgLatency || '-' }}ms</template>
            </el-table-column>
            <el-table-column prop="cost" label="成本" width="110">
              <template #default="{ row }">${{ row.cost || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="unitPrice" label="均价/条" width="100">
              <template #default="{ row }">${{ row.unitPrice || '0.000' }}</template>
            </el-table-column>
            <el-table-column prop="faultCount" label="故障次数" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.faultCount > 0" type="danger" size="small">{{ row.faultCount }}</el-tag>
                <span v-else>0</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="vendorData.length === 0 && !loading" description="暂无供应商统计数据" />
        </el-tab-pane>

        <!-- ==================== Tab 3: 通道统计 ==================== -->
        <el-tab-pane label="通道统计" name="channel">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="channelFilter.dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
              style="width: 280px;" />
            <el-select v-model="channelFilter.vendor" placeholder="全部供应商" clearable style="width: 160px;">
              <el-option v-for="v in vendorOptions" :key="v.id" :label="v.name" :value="v.id" />
            </el-select>
            <el-select v-model="channelFilter.country" placeholder="全部国家" clearable style="width: 160px;">
              <el-option v-for="c in countryOptions" :key="c.code" :label="c.name + ' (' + c.code + ')'" :value="c.code" />
            </el-select>
            <el-button type="primary" @click="loadChannel">查询</el-button>
            <el-button @click="resetChannelFilter">重置</el-button>
          </div>

          <!-- Chart Placeholders -->
          <div class="chart-row">
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">通道发送量趋势（按日期）</span>
            </div>
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">通道送达率对比</span>
            </div>
          </div>

          <!-- Data Table -->
          <el-table :data="channelData" stripe :header-cell-style="headerStyle" style="margin-top: 16px;">
            <el-table-column prop="channelName" label="通道名" min-width="130">
              <template #default="{ row }"><strong>{{ row.channelName }}</strong></template>
            </el-table-column>
            <el-table-column prop="vendorName" label="供应商" width="110" />
            <el-table-column prop="country" label="国家" width="80" />
            <el-table-column prop="sent" label="发送量" width="110" />
            <el-table-column prop="delivered" label="送达量" width="110" />
            <el-table-column prop="deliveryRate" label="送达率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.deliveryRate >= 93 ? '#67c23a' : row.deliveryRate >= 85 ? '#e6a23c' : '#f56c6c', fontWeight: 600 }">
                  {{ row.deliveryRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="failed" label="失败量" width="90" />
            <el-table-column prop="avgLatency" label="平均延迟" width="100">
              <template #default="{ row }">{{ row.avgLatency }}s</template>
            </el-table-column>
            <el-table-column prop="cost" label="成本" width="110">
              <template #default="{ row }">${{ row.cost }}</template>
            </el-table-column>
            <el-table-column prop="unitPrice" label="均价/条" width="100">
              <template #default="{ row }">${{ row.unitPrice }}</template>
            </el-table-column>
            <el-table-column prop="tpsPeak" label="TPS峰值" width="100" />
            <el-table-column prop="faultCount" label="故障次数" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.faultCount > 0" type="danger" size="small">{{ row.faultCount }}</el-tag>
                <span v-else>0</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== Tab 4: 客户统计 ==================== -->
        <el-tab-pane label="客户统计" name="customer">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="customerFilter.dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
              style="width: 280px;" />
            <el-select v-model="customerFilter.customer" placeholder="全部客户" clearable filterable
              style="width: 180px;">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName" :value="c.id" />
            </el-select>
            <el-select v-model="customerFilter.type" placeholder="全部类型" clearable style="width: 140px;">
              <el-option label="OTP" value="OTP" />
              <el-option label="通知" value="NOTIFICATION" />
              <el-option label="营销" value="MARKETING" />
            </el-select>
            <el-button type="primary" @click="loadCustomer">查询</el-button>
            <el-button @click="resetCustomerFilter">重置</el-button>
          </div>

          <!-- Chart Placeholders -->
          <div class="chart-row">
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">客户发送量排行TOP10</span>
            </div>
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">客户利润贡献占比</span>
            </div>
          </div>

          <!-- Data Table -->
          <el-table :data="customerData" stripe v-loading="loading" :header-cell-style="headerStyle"
            style="margin-top: 16px;">
            <el-table-column prop="dimensionValue" label="客户" min-width="120" />
            <el-table-column prop="sent" label="发送量" width="100" />
            <el-table-column prop="delivered" label="送达量" width="100" />
            <el-table-column prop="failed" label="失败量" width="100" />
            <el-table-column prop="deliveryRate" label="送达率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.deliveryRate >= 90 ? '#67c23a' : row.deliveryRate >= 70 ? '#e6a23c' : '#f56c6c' }">
                  {{ row.deliveryRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="revenue" label="收入" width="110">
              <template #default="{ row }">${{ row.revenue || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="cost" label="成本" width="110">
              <template #default="{ row }">${{ row.cost || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="profit" label="利润" width="110">
              <template #default="{ row }">
                <span :style="{ color: (row.profit || 0) >= 0 ? '#67c23a' : '#f56c6c' }">
                  ${{ row.profit || '0.00' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="profitRate" label="利润率" width="100">
              <template #default="{ row }">
                <span :style="{ color: (row.profitRate || 0) >= 0 ? '#67c23a' : '#f56c6c' }">
                  {{ row.profitRate || '0.0' }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== Tab 5: SID 统计 ==================== -->
        <el-tab-pane label="SID统计" name="sid">
          <!-- Filter Bar -->
          <div class="filter-bar">
            <el-date-picker v-model="sidFilter.dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
              style="width: 280px;" />
            <el-select v-model="sidFilter.sid" placeholder="全部SID" clearable filterable style="width: 160px;">
              <el-option v-for="s in sidOptions" :key="s" :label="s" :value="s" />
            </el-select>
            <el-select v-model="sidFilter.customer" placeholder="全部客户" clearable filterable style="width: 160px;">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
            <el-select v-model="sidFilter.country" placeholder="全部国家" clearable style="width: 140px;">
              <el-option v-for="c in countryOptions" :key="c.code" :label="c.name + ' (' + c.code + ')'" :value="c.code" />
            </el-select>
            <el-button type="primary" @click="loadSid">查询</el-button>
            <el-button @click="resetSidFilter">重置</el-button>
          </div>

          <!-- Stat Cards -->
          <div class="stat-cards">
            <div class="stat-card">
              <div class="stat-label">活跃 SID 数</div>
              <div class="stat-value" style="color: #409eff;">38</div>
              <div class="stat-sub">总 SID: 52</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">SID 替换次数</div>
              <div class="stat-value" style="color: #e6a23c;">2,340</div>
              <div class="stat-sub">替换率 0.18%</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">替换成功率</div>
              <div class="stat-value" style="color: #67c23a;">98.5%</div>
              <div class="stat-sub">失败 35 次</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">被投诉 SID</div>
              <div class="stat-value" style="color: #f56c6c;">2</div>
              <div class="stat-sub">MarketBoom, BulkPromo</div>
            </div>
          </div>

          <!-- Granularity -->
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
            <span style="font-size: 13px; color: #6b7280;">时间粒度：</span>
            <el-radio-group v-model="sidGranularity" size="small">
              <el-radio-button value="day">天</el-radio-button>
              <el-radio-button value="week">周</el-radio-button>
              <el-radio-button value="month">月</el-radio-button>
            </el-radio-group>
          </div>

          <!-- Chart Placeholders -->
          <div class="chart-placeholder" style="margin-bottom: 16px;">
            <span class="chart-placeholder-text">SID 发送量趋势 — Top 10 SID 每日发送量，可多选 SID 对比</span>
          </div>
          <div class="chart-row">
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">SID 发送量排行 Top 10（水平柱状图）</span>
            </div>
            <div class="chart-placeholder">
              <span class="chart-placeholder-text">SID 替换趋势 — 每日替换次数 + 替换成功率双Y轴</span>
            </div>
          </div>

          <!-- SID Detail Table -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin: 16px 0 12px;">
            <span style="font-size: 14px; font-weight: 600;">SID 发送明细</span>
            <el-button size="small" @click="$message.success('导出中...')">导出 CSV</el-button>
          </div>
          <el-table :data="sidData" stripe :header-cell-style="headerStyle">
            <el-table-column prop="sid" label="SID" min-width="120">
              <template #default="{ row }"><strong>{{ row.sid }}</strong></template>
            </el-table-column>
            <el-table-column prop="customer" label="归属客户" width="120" />
            <el-table-column prop="sent" label="发送量" width="110" />
            <el-table-column prop="deliveryRate" label="送达率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.deliveryRate >= 93 ? '#67c23a' : row.deliveryRate >= 88 ? '#e6a23c' : '#f56c6c', fontWeight: 600 }">
                  {{ row.deliveryRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="countries" label="覆盖国家" width="100" />
            <el-table-column prop="channels" label="绑定通道" width="100" />
            <el-table-column prop="replaceCount" label="替换次数" width="100" />
            <el-table-column prop="complaintCount" label="投诉次数" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.complaintCount > 10 ? '#f56c6c' : row.complaintCount > 0 ? '#e6a23c' : '' }">{{ row.complaintCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="complaintRate" label="投诉率" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="sidStatusType[row.status]" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div style="display: flex; justify-content: flex-end; margin-top: 12px;">
            <el-pagination :total="sidTotal" :page-size="10" layout="total, prev, pager, next" small background />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { statsByCountry, statsByCustomer, statsByVendor, statsByChannel } from '../api'
import { useRefData } from '../stores/refData'

const refData = useRefData()

/* ---- shared ---- */
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const activeTab = ref('country')
const loading = ref(false)

const today = new Date().toISOString().slice(0, 10)
const day30ago = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10)
const defaultRange = () => [day30ago, today]

/* ---- dropdown options ---- */
const countryOptions = ref([])
const customerOptions = ref([])
const vendorOptions = ref([])

const loadOptions = async () => {
  const [c, cu, v] = await Promise.all([refData.loadCountries(), refData.loadCustomers(), refData.loadVendors()])
  countryOptions.value = c; customerOptions.value = cu; vendorOptions.value = v
}

/* ============ Tab 1: 国家统计 ============ */
const countryFilter = reactive({ dateRange: defaultRange(), country: '', type: '' })
const countryData = ref([])

const countrySummary = computed(() => {
  const list = countryData.value
  if (!list.length) return { totalSent: 0, totalDelivered: 0, deliveryRate: '0.0', countryCount: 0, totalRevenue: '0.00' }
  const totalSent = list.reduce((s, r) => s + (r.sent || r.segments || 0), 0)
  const totalDelivered = list.reduce((s, r) => s + (r.delivered || 0), 0)
  const totalRevenue = list.reduce((s, r) => s + parseFloat(r.revenue || 0), 0)
  return {
    totalSent,
    totalDelivered,
    deliveryRate: totalSent ? (totalDelivered / totalSent * 100).toFixed(1) : '0.0',
    countryCount: list.length,
    totalRevenue: totalRevenue.toFixed(2)
  }
})

const loadCountry = async () => {
  loading.value = true
  try {
    const p = { start: countryFilter.dateRange?.[0], end: countryFilter.dateRange?.[1] }
    if (countryFilter.country) p.country = countryFilter.country
    if (countryFilter.type) p.type = countryFilter.type
    const res = await statsByCountry(p)
    const list = res.data || []
    // 构建国家名称映射
    const countryMap = {}
    countryOptions.value.forEach(c => { countryMap[c.code || c.countryCode] = c.name || c.countryName })
    countryData.value = list.map(r => ({
      ...r,
      dimensionValue: `${r.dimensionValue} ${countryMap[r.dimensionValue] || ''}`.trim(),
      sent: r.sent || r.segments || 0,
      failed: r.failed || ((r.sent || r.segments || 0) - (r.delivered || 0)),
      deliveryRate: r.deliveryRate ?? (r.segments ? (r.delivered / r.segments * 100).toFixed(1) : '0.0'),
      profitRate: r.profitRate ?? (r.revenue && parseFloat(r.revenue) > 0
        ? ((parseFloat(r.profit || 0) / parseFloat(r.revenue)) * 100).toFixed(1) : '0.0'),
      avgLatency: r.avgLatency || null
    }))
  } finally { loading.value = false }
}

const resetCountryFilter = () => {
  countryFilter.dateRange = defaultRange()
  countryFilter.country = ''
  countryFilter.type = ''
  loadCountry()
}

/* ============ Tab 2: 供应商统计 ============ */
const vendorFilter = reactive({ dateRange: defaultRange(), vendor: '', country: '' })
const vendorData = ref([])

const loadVendor = async () => {
  loading.value = true
  try {
    const p = { start: vendorFilter.dateRange?.[0], end: vendorFilter.dateRange?.[1] }
    const [res, channels] = await Promise.all([statsByVendor(p), refData.loadChannels()])
    const list = res.data || []
    // 构建供应商名称映射
    const vendorMap = {}
    vendorOptions.value.forEach(v => { vendorMap[v.id] = v.vendorName || v.name })
    vendorData.value = list.map(r => {
      const vid = Number(r.dimensionValue)
      const vendorChannels = channels.filter(c => c.vendorId === vid)
      const countryCodes = new Set(vendorChannels.map(c => c.countryCode))
      return {
        ...r,
        vendorName: vendorMap[vid] || r.dimensionLabel || r.dimensionValue || '-',
        channelCount: vendorChannels.length,
        countryCount: countryCodes.size,
        sent: r.sent || r.segments || 0,
        delivered: r.delivered || 0,
        failed: r.failed || ((r.sent || r.segments || 0) - (r.delivered || 0)),
        deliveryRate: r.deliveryRate ?? (r.segments ? (r.delivered / r.segments * 100).toFixed(1) : '0.0'),
      }
    })
  } finally { loading.value = false }
}

const resetVendorFilter = () => {
  vendorFilter.dateRange = defaultRange()
  vendorFilter.vendor = ''
  vendorFilter.country = ''
  loadVendor()
}

/* ============ Tab 3: 通道统计 ============ */
const channelFilter = reactive({ dateRange: defaultRange(), vendor: '', country: '' })
const channelData = ref([])

const loadChannel = async () => {
  loading.value = true
  try {
    const p = { start: channelFilter.dateRange?.[0], end: channelFilter.dateRange?.[1] }
    const [res, channels] = await Promise.all([statsByChannel(p), refData.loadChannels()])
    const list = res.data || []
    // 构建通道信息映射
    const channelMap = {}
    channels.forEach(c => { channelMap[c.id] = c })
    const vendorMap = {}
    vendorOptions.value.forEach(v => { vendorMap[v.id] = v.vendorName || v.name })
    channelData.value = list.map(r => {
      const chId = Number(r.dimensionValue)
      const ch = channelMap[chId] || {}
      return {
        ...r,
        channelName: ch.channelName || r.dimensionLabel || r.dimensionValue || '-',
        vendorName: vendorMap[ch.vendorId] || '-',
        country: ch.countryCode || '-',
        sent: r.sent || r.segments || 0,
        delivered: r.delivered || 0,
        failed: r.failed || ((r.sent || r.segments || 0) - (r.delivered || 0)),
        deliveryRate: r.deliveryRate ?? (r.segments ? (r.delivered / r.segments * 100).toFixed(1) : '0.0'),
        unitPrice: r.sent > 0 ? (parseFloat(r.cost || 0) / r.sent).toFixed(3) : '0.000',
        tpsPeak: '-',
        faultCount: 0,
      }
    })
  } finally { loading.value = false }
}

const resetChannelFilter = () => {
  channelFilter.dateRange = defaultRange()
  channelFilter.vendor = ''
  channelFilter.country = ''
}

/* ============ Tab 3: 客户统计 ============ */
const customerFilter = reactive({ dateRange: defaultRange(), customer: '', type: '' })
const customerData = ref([])

const loadCustomer = async () => {
  loading.value = true
  try {
    const p = { start: customerFilter.dateRange?.[0], end: customerFilter.dateRange?.[1] }
    if (customerFilter.customer) p.customerId = customerFilter.customer
    if (customerFilter.type) p.type = customerFilter.type
    const res = await statsByCustomer(p)
    const list = res.data || []
    // 构建客户名称映射
    const custMap = {}
    customerOptions.value.forEach(c => { custMap[c.id] = c.companyName || c.customerName })
    customerData.value = list.map(r => {
      const cid = Number(r.dimensionValue)
      return {
        ...r,
        dimensionValue: custMap[cid] || r.dimensionLabel || r.dimensionValue || '-',
        sent: r.sent || r.segments || 0,
        failed: r.failed || ((r.sent || r.segments || 0) - (r.delivered || 0)),
        deliveryRate: r.deliveryRate ?? (r.segments ? (r.delivered / r.segments * 100).toFixed(1) : '0.0'),
        profit: r.profit ?? (parseFloat(r.revenue || 0) - parseFloat(r.cost || 0)).toFixed(2),
        profitRate: r.profitRate ?? (r.revenue && parseFloat(r.revenue) > 0
          ? (((parseFloat(r.revenue) - parseFloat(r.cost || 0)) / parseFloat(r.revenue)) * 100).toFixed(1) : '0.0')
      }
    })
  } finally { loading.value = false }
}

const resetCustomerFilter = () => {
  customerFilter.dateRange = defaultRange()
  customerFilter.customer = ''
  customerFilter.type = ''
  loadCustomer()
}

/* ============ Tab 5: SID 统计 ============ */
const sidFilter = reactive({ dateRange: defaultRange(), sid: '', customer: '', country: '' })
const sidGranularity = ref('day')
const sidOptions = ref([])
const sidStatusType = { '正常': 'success', '关注': 'warning', '高风险': 'danger', '已暂停': 'info' }
const sidData = ref([])
const sidTotal = ref(0)

const loadSid = () => { /* Mock — no API yet */ }
const resetSidFilter = () => {
  sidFilter.dateRange = defaultRange(); sidFilter.sid = ''; sidFilter.customer = ''; sidFilter.country = ''
}

/* ============ Tab switch ============ */
const handleTabChange = (tab) => {
  if (tab === 'country') loadCountry()
  else if (tab === 'vendor') loadVendor()
  else if (tab === 'channel') loadChannel()
  else if (tab === 'customer') loadCustomer()
  else if (tab === 'sid') loadSid()
}

/* ============ Init ============ */
onMounted(() => {
  loadOptions()
  loadCountry()
})
</script>

<style scoped>
.stats-page {
  padding: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 150px;
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px 20px;
  text-align: center;
  border: 1px solid #f0f0f0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.chart-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.chart-placeholder {
  flex: 1;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.chart-placeholder-text {
  color: #c0c4cc;
  font-size: 14px;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.table-card {
  border-radius: 8px;
}
</style>
