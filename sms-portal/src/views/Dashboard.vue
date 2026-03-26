<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">概览 Dashboard</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="color: #1890ff;"><el-icon :size="32"><Promotion /></el-icon></div>
          <div class="stat-info">
            <div class="stat-title">今日发送量</div>
            <div class="stat-value">{{ formatNum(stats.todaySent) }}</div>
            <div class="stat-footer">
              <span :class="stats.sentGrowth >= 0 ? 'up' : 'down'">
                {{ stats.sentGrowth >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.sentGrowth ?? 0) }}%
              </span>
              较昨日
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="color: #52c41a;"><el-icon :size="32"><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-title">今日送达率</div>
            <div class="stat-value" style="color: #52c41a;">{{ stats.deliveryRate ?? '—' }}%</div>
            <div class="stat-footer">近7天平均: {{ stats.avgDeliveryRate ?? '—' }}%</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="color: #1890ff;"><el-icon :size="32"><Wallet /></el-icon></div>
          <div class="stat-info">
            <div class="stat-title">账户余额</div>
            <div class="stat-value" style="color: #1890ff;">${{ formatMoney(stats.balance) }}</div>
            <div class="stat-footer">预估可发 ~{{ formatNum(stats.estimatedRemain) }} 条</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="color: #faad14;"><el-icon :size="32"><CreditCard /></el-icon></div>
          <div class="stat-info">
            <div class="stat-title">今日消费</div>
            <div class="stat-value" style="color: #faad14;">${{ formatMoney(stats.todayCost) }}</div>
            <div class="stat-footer">
              <span :class="(stats.costGrowth ?? 0) <= 0 ? 'up' : 'down'">
                {{ (stats.costGrowth ?? 0) <= 0 ? '↓' : '↑' }} {{ Math.abs(stats.costGrowth ?? 0) }}%
              </span>
              较昨日
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card shadow="never" style="margin-bottom: 20px;">
      <div class="card-title">快捷入口</div>
      <div class="quick-actions">
        <router-link to="/send" class="quick-action">
          <el-icon :size="28" color="#1890ff"><Promotion /></el-icon>
          <span>快速发送</span>
        </router-link>
        <router-link to="/records" class="quick-action">
          <el-icon :size="28" color="#52c41a"><List /></el-icon>
          <span>发送记录</span>
        </router-link>
        <router-link to="/billing" class="quick-action">
          <el-icon :size="28" color="#faad14"><Wallet /></el-icon>
          <span>查看账单</span>
        </router-link>
        <router-link to="/account" class="quick-action">
          <el-icon :size="28" color="#722ed1"><Setting /></el-icon>
          <span>账户设置</span>
        </router-link>
      </div>
    </el-card>

    <!-- 近期发送记录 -->
    <el-card shadow="never">
      <div class="card-title">近期发送记录</div>
      <el-table :data="recentMessages" stripe style="font-size: 13px;"
        :header-cell-style="headerStyle" v-loading="loading">
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="toNumber" label="目标号码" width="150" />
        <el-table-column prop="countryCode" label="国家" width="70" />
        <el-table-column prop="sid" label="SID" width="120" />
        <el-table-column label="内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '（已加密）' }}</template>
        </el-table-column>
        <el-table-column prop="segments" label="段数" width="60" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="费用" width="90">
          <template #default="{ row }">
            ${{ row.price ? (row.price * row.segments).toFixed(4) : '—' }}
          </template>
        </el-table-column>
      </el-table>
      <div style="text-align: right; margin-top: 12px;">
        <router-link to="/records" style="font-size: 13px; color: #1890ff;">查看全部记录 →</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Promotion, CircleCheck, Wallet, CreditCard, List, Setting } from '@element-plus/icons-vue'
import { dashboardStats, messageList } from '../api'

const loading = ref(false)
const stats = ref({})
const recentMessages = ref([])

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

const formatNum = v => v != null ? Number(v).toLocaleString() : '—'
const formatMoney = v => v != null ? Number(v).toFixed(2) : '0.00'

const statusType = s => ({ DELIVERED: 'success', SUBMITTED: 'warning', FAILED: 'danger', ACCEPTED: 'info' }[s] || 'info')
const statusLabel = s => ({ DELIVERED: '已送达', SUBMITTED: '发送中', FAILED: '失败', ACCEPTED: '待处理' }[s] || s)

const loadData = async () => {
  loading.value = true
  try {
    const [s, m] = await Promise.all([
      dashboardStats().catch(() => ({ data: {} })),
      messageList({ page: 1, size: 10 }).catch(() => ({ data: { list: [] } }))
    ])
    stats.value = s.data || {}
    recentMessages.value = m.data?.list || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 600; margin: 0; }

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.stat-icon { opacity: .85; }
.stat-info { flex: 1; }
.stat-title { font-size: 13px; color: #999; margin-bottom: 6px; }
.stat-value { font-size: 26px; font-weight: 600; line-height: 1.2; }
.stat-footer { font-size: 12px; color: #999; margin-top: 6px; }
.up { color: #52c41a; }
.down { color: #ff4d4f; }

.card-title { font-size: 15px; font-weight: 600; margin-bottom: 16px; }

.quick-actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  text-decoration: none;
  color: #333;
  border: 1px solid #f0f0f0;
  font-size: 13px;
  transition: all .2s;
}
.quick-action:hover { background: #e6f7ff; border-color: #91d5ff; }
</style>
