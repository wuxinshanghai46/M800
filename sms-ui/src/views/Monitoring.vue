<template>
  <div class="monitoring-page">
    <div class="page-header"><h2>监控告警</h2></div>

    <!-- Sub-nav pill buttons -->
    <div class="sub-nav">
      <el-radio-group v-model="activeTab" size="default">
        <el-radio-button value="dashboard">实时看板</el-radio-button>
        <el-radio-button value="alerts">
          告警中心 <span v-if="alertBadgeCount > 0" style="display:inline-block;min-width:18px;height:18px;line-height:18px;padding:0 5px;border-radius:10px;background:#f56c6c;color:#fff;font-size:11px;text-align:center;vertical-align:middle;margin-left:4px;">{{ alertBadgeCount }}</span>
        </el-radio-button>
        <el-radio-button value="channels">通道监控</el-radio-button>
        <el-radio-button value="rules">告警规则</el-radio-button>
        <el-radio-button value="queue">队列监控</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ==================== Section 1: 实时看板 ==================== -->
    <div v-if="activeTab === 'dashboard'">
      <!-- 告警汇总 -->
      <el-row :gutter="14" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="stat-card stat-critical">
            <div class="stat-label">严重告警</div>
            <div class="stat-value" style="color: #e63946;">{{ dashboardStats.criticalAlerts }}</div>
            <div class="stat-sub">TPS超限 / 通道离线</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-warning">
            <div class="stat-label">警告</div>
            <div class="stat-value" style="color: #d97706;">{{ dashboardStats.warnings }}</div>
            <div class="stat-sub">错误率偏高 / 队列积压</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-ok">
            <div class="stat-label">在线通道</div>
            <div class="stat-value" style="color: #16a34a;">
              {{ dashboardStats.onlineChannels }}
              <span style="font-size: 14px; color: #adb5bd;">/ {{ dashboardStats.totalChannels }}</span>
            </div>
            <div class="stat-sub">{{ dashboardStats.totalChannels - dashboardStats.onlineChannels }} 通道异常</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-info">
            <div class="stat-label">全局送达率</div>
            <div class="stat-value" style="color: #4361ee;">{{ dashboardStats.globalDeliveryRate }}%</div>
            <div class="stat-sub">近 15 分钟滚动</div>
          </div>
        </el-col>
      </el-row>

      <!-- 实时 KPI -->
      <el-row :gutter="14" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">全局发送速率</div>
            <div class="kpi-value">8,420<span class="kpi-unit"> 条/分</span></div>
            <div class="kpi-trend up">^ 较昨日同时段 +12%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">队列积压</div>
            <div class="kpi-value">{{ queueStatsData.pendingCount.toLocaleString() }}<span class="kpi-unit"> 条</span></div>
            <div class="kpi-trend neutral">约 5 分钟处理完</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">重试队列</div>
            <div class="kpi-value" style="color: #d97706;">{{ queueStatsData.failedCount.toLocaleString() }}<span class="kpi-unit"> 条</span></div>
            <div class="kpi-trend neutral">处理中 {{ queueStatsData.processingCount.toLocaleString() }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card">
            <div class="kpi-label">今日累计发送</div>
            <div class="kpi-value">{{ formatLargeNumber(dashboardStats.todayTotal) }}<span class="kpi-unit"> 条</span></div>
            <div class="kpi-trend up">^ 已送达 {{ formatLargeNumber(dashboardStats.todayDelivered) }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 通道健康一览 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">通道健康一览（实时）</span>
            <span style="font-size: 12px; color: #adb5bd;">每 30 秒自动刷新</span>
          </div>
        </template>
        <el-table
          :data="channelHealthData"
          :header-cell-style="headerCellStyle"
          :row-class-name="dashboardRowClass"
          size="small"
          style="width: 100%;"
        >
          <el-table-column label="通道" prop="channel" min-width="120">
            <template #default="{ row }">
              <strong>{{ row.channel }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="供应商" prop="vendor" min-width="130">
            <template #default="{ row }">
              <span style="color: #6b7280; font-size: 12px;">{{ row.vendor }}</span>
            </template>
          </el-table-column>
          <el-table-column label="国家" prop="country" min-width="90" />
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.statusType"
                size="small"
                effect="light"
              >{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="TPS 使用率" min-width="140">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-progress
                  :percentage="row.tpsUsage"
                  :stroke-width="5"
                  :show-text="false"
                  :color="getProgressColor(row.tpsUsage)"
                  style="width: 60px;"
                />
                <span :style="{ fontSize: '12px', color: row.tpsUsage >= 85 ? '#d97706' : undefined, fontWeight: row.tpsUsage >= 85 ? 600 : 400 }">{{ row.tpsUsage }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="5分钟送达率" min-width="110">
            <template #default="{ row }">
              <span :style="{ color: row.deliveryRate >= 85 ? '#16a34a' : '#e63946', fontWeight: 500 }">{{ row.deliveryRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="5分钟错误率" min-width="110">
            <template #default="{ row }">
              <span :style="{ color: row.errorRate > 5 ? (row.errorRate > 10 ? '#e63946' : '#d97706') : '#16a34a' }">{{ row.errorRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="平均时延" prop="latency" min-width="90">
            <template #default="{ row }">
              <span style="font-size: 12px;">{{ row.latency }}</span>
            </template>
          </el-table-column>
          <el-table-column label="队列积压" min-width="90">
            <template #default="{ row }">
              <span :style="{ fontSize: '12px', color: row.queueBacklog > 10000 ? '#d97706' : undefined, fontWeight: row.queueBacklog > 10000 ? 500 : 400 }">{{ row.queueBacklog.toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="showChannelDetail(row.channel)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- ==================== Section 2: 告警中心 ==================== -->
    <div v-if="activeTab === 'alerts'">
      <div class="page-header-row">
        <div>
          <div class="section-title">告警中心</div>
          <div class="section-desc">严重告警 {{ alertCounts.critical }} / 警告 {{ alertCounts.warning }} / 今日已处理 {{ alertCounts.resolved }}</div>
        </div>
        <el-button size="small">导出记录</el-button>
      </div>

      <!-- 子状态筛选 -->
      <div style="display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap;">
        <el-radio-group v-model="alertFilter" size="small">
          <el-radio-button value="all">全部 ({{ alertList.length }})</el-radio-button>
          <el-radio-button value="critical">严重 ({{ alertCounts.critical }})</el-radio-button>
          <el-radio-button value="warning">警告 ({{ alertCounts.warning }})</el-radio-button>
          <el-radio-button value="resolved">已处理</el-radio-button>
        </el-radio-group>
        <el-select v-model="alertObjectFilter" placeholder="全部对象" size="small" style="width: 130px;" clearable>
          <el-option label="全部对象" value="" />
          <el-option label="通道告警" value="channel" />
          <el-option label="业务告警" value="business" />
        </el-select>
      </div>

      <!-- 告警列表 -->
      <div
        v-for="(alert, idx) in filteredAlerts"
        :key="idx"
        :class="['alert-item', alert.level]"
        :style="alert.resolved ? { background: '#f9fafb', opacity: 0.6 } : (alert.handling ? { opacity: 0.7 } : {})"
      >
        <div :class="['alert-dot', 'dot-' + alert.level]" :style="alert.level === 'critical' && !alert.resolved ? { animation: 'pulse 1.5s infinite' } : {}"></div>
        <div class="alert-body">
          <div class="alert-name" :style="alert.resolved ? { color: '#6b7280' } : {}">{{ alert.name }}</div>
          <div class="alert-meta">
            <el-tag
              :type="alert.level === 'critical' ? 'danger' : (alert.level === 'warning' ? 'warning' : 'success')"
              size="small"
              effect="light"
            >{{ alert.levelText }}</el-tag>
            <span>{{ alert.category }}</span>
            <span>{{ alert.detail }}</span>
            <span v-if="alert.handling" style="color: #adb5bd;">{{ alert.handlingInfo }}</span>
            <span style="margin-left: auto;">{{ alert.timeInfo }}</span>
          </div>
        </div>
        <div class="alert-actions">
          <template v-if="alert.resolved">
            <span style="font-size: 12px; color: #adb5bd;">已自动关闭</span>
          </template>
          <template v-else-if="alert.handling">
            <el-button size="small" type="primary" @click="handleResolve(alert)">标记处理</el-button>
            <el-button type="primary" link size="small">详情</el-button>
          </template>
          <template v-else>
            <el-button size="small" @click="handleAcknowledge(alert)">认领</el-button>
            <el-button type="primary" link size="small">详情</el-button>
          </template>
        </div>
      </div>

      <!-- 认领弹窗 -->
      <el-dialog v-model="ackDialogVisible" title="认领告警" width="460px" destroy-on-close>
        <p style="font-size: 13px; color: #6b7280; margin-bottom: 14px;">认领后此告警将标记为"处理中"，由你负责跟进处理。</p>
        <el-form label-position="top">
          <el-form-item label="处理备注（可选）">
            <el-input type="textarea" :rows="3" placeholder="填写处理思路或备注..." />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="ackDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAcknowledge">确认认领</el-button>
        </template>
      </el-dialog>

      <!-- 标记处理弹窗 -->
      <el-dialog v-model="resolveDialogVisible" title="标记处理完成" width="460px" destroy-on-close>
        <el-form label-position="top">
          <el-form-item label="处理结果" required>
            <el-select v-model="resolveForm.result" style="width: 100%;">
              <el-option label="已解决（问题已修复）" value="resolved" />
              <el-option label="暂时忽略（不影响业务）" value="ignored" />
              <el-option label="已升级（转交技术团队）" value="escalated" />
            </el-select>
          </el-form-item>
          <el-form-item label="处理说明" required>
            <el-input type="textarea" :rows="4" v-model="resolveForm.note" placeholder="描述处理过程和结果..." />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="resolveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmResolve">确认关闭</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- ==================== Section 3: 通道监控 ==================== -->
    <div v-if="activeTab === 'channels'">
      <div class="page-header-row">
        <div class="section-title">通道监控</div>
      </div>

      <!-- 筛选栏 -->
      <el-card shadow="never" class="box-card" style="margin-bottom: 16px;">
        <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 12px;">
          <div class="filter-item">
            <label>国家</label>
            <el-select v-model="channelFilter.country" size="small" style="width: 110px;" clearable filterable placeholder="全部">
              <el-option label="全部" value="" />
              <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} (${c.code})`" :value="c.code" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>状态</label>
            <el-select v-model="channelFilter.status" size="small" style="width: 100px;" clearable placeholder="全部">
              <el-option label="全部" value="" />
              <el-option label="在线" value="online" />
              <el-option label="告警" value="alert" />
              <el-option label="熔断" value="fuse" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>协议</label>
            <el-select v-model="channelFilter.protocol" size="small" style="width: 90px;" clearable placeholder="全部">
              <el-option label="全部" value="" />
              <el-option label="SMPP" value="SMPP" />
              <el-option label="HTTP" value="HTTP" />
            </el-select>
          </div>
          <el-button type="primary" size="small" style="margin-left: auto;">搜索</el-button>
        </div>
      </el-card>

      <!-- 通道详情面板 -->
      <div v-if="channelDetailVisible" style="margin-bottom: 16px;">
        <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
          <span style="font-size: 15px; font-weight: 700; color: #343a40;">{{ channelDetailName }}</span>
          <el-tag type="danger" size="small" effect="light">送达率低</el-tag>
          <el-tag size="small" effect="plain">HTTP</el-tag>
          <span style="font-size: 12px; color: #6b7280;">DITO Philippines / 菲律宾</span>
          <el-button size="small" style="margin-left: auto;" @click="channelDetailVisible = false">关闭</el-button>
        </div>

        <el-row :gutter="16" style="margin-bottom: 16px;">
          <el-col :span="12">
            <el-card shadow="never" class="box-card">
              <template #header>
                <span class="card-title">近 1 小时指标</span>
              </template>
              <div v-for="(m, i) in channelMetrics" :key="i" class="metric-bar">
                <div class="metric-bar-label">{{ m.label }}</div>
                <div class="metric-bar-track">
                  <div class="metric-bar-fill" :style="{ width: m.pct + '%', background: m.color }"></div>
                </div>
                <div class="metric-bar-val" :style="{ color: m.valColor || undefined }">{{ m.value }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="box-card">
              <template #header>
                <span class="card-title">送达率趋势（近 1 小时）</span>
              </template>
              <div class="trend-wrap">
                <svg viewBox="0 0 400 120" preserveAspectRatio="none">
                  <defs>
                    <linearGradient id="dlrGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#e63946" stop-opacity="0.3" />
                      <stop offset="100%" stop-color="#e63946" stop-opacity="0" />
                    </linearGradient>
                  </defs>
                  <line x1="0" y1="24" x2="400" y2="24" stroke="#e9ecef" stroke-width="1" />
                  <line x1="0" y1="60" x2="400" y2="60" stroke="#e9ecef" stroke-width="1" />
                  <line x1="0" y1="96" x2="400" y2="96" stroke="#e9ecef" stroke-width="1" />
                  <line x1="0" y1="30" x2="400" y2="30" stroke="#ff9f1c" stroke-width="1" stroke-dasharray="4,3" />
                  <text x="4" y="27" font-size="9" fill="#ff9f1c">85% 阈值</text>
                  <path d="M0,90 L40,85 L80,88 L120,95 L160,100 L200,105 L240,98 L280,108 L320,110 L360,105 L400,108 L400,120 L0,120 Z" fill="url(#dlrGrad)" />
                  <path d="M0,90 L40,85 L80,88 L120,95 L160,100 L200,105 L240,98 L280,108 L320,110 L360,105 L400,108" fill="none" stroke="#e63946" stroke-width="2" />
                </svg>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 10px; color: #adb5bd; margin-top: 4px; padding: 0 4px;">
                <span>60分钟前</span><span>45m</span><span>30m</span><span>15m</span><span>现在</span>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 近期告警 -->
        <el-card shadow="never" class="box-card">
          <template #header>
            <span class="card-title">该通道近期告警</span>
          </template>
          <el-table :data="channelRecentAlerts" :header-cell-style="headerCellStyle" size="small">
            <el-table-column label="时间" prop="time" width="80">
              <template #default="{ row }"><span style="font-size: 12px;">{{ row.time }}</span></template>
            </el-table-column>
            <el-table-column label="告警名称" prop="name" min-width="160" />
            <el-table-column label="级别" width="90">
              <template #default="{ row }">
                <el-tag :type="row.level === 'critical' ? 'danger' : 'warning'" size="small" effect="light">{{ row.levelText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="触发值" prop="triggerValue" min-width="180">
              <template #default="{ row }"><span style="font-size: 12px;">{{ row.triggerValue }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'alerting' ? 'danger' : 'success'" size="small" effect="light">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 通道列表 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <span class="card-title">全部通道（{{ channelListData.length }} 条）</span>
        </template>
        <el-table
          :data="channelListData"
          :header-cell-style="headerCellStyle"
          :row-class-name="channelRowClass"
          size="small"
        >
          <el-table-column label="通道 ID" min-width="120">
            <template #default="{ row }"><strong>{{ row.channelId }}</strong></template>
          </el-table-column>
          <el-table-column label="供应商" min-width="120">
            <template #default="{ row }"><span style="font-size: 12px; color: #6b7280;">{{ row.vendor }}</span></template>
          </el-table-column>
          <el-table-column label="国家" prop="countryFlag" width="60" />
          <el-table-column label="协议" width="80">
            <template #default="{ row }">
              <el-tag :type="row.protocol === 'SMPP' ? '' : 'info'" size="small" effect="plain">{{ row.protocol }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small" effect="light">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="TPS 使用率" min-width="80">
            <template #default="{ row }">
              <span :style="{ fontSize: '12px', color: row.tpsUsage >= 85 ? '#d97706' : undefined, fontWeight: row.tpsUsage >= 85 ? 600 : 400 }">{{ row.tpsUsage }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="送达率" min-width="80">
            <template #default="{ row }">
              <span :style="{ color: row.deliveryRate >= 85 ? '#16a34a' : '#e63946', fontWeight: row.deliveryRate < 85 ? 600 : 400 }">{{ row.deliveryRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="错误率" min-width="70">
            <template #default="{ row }">
              <span :style="{ color: row.errorRate > 10 ? '#e63946' : (row.errorRate > 5 ? '#d97706' : undefined), fontWeight: row.errorRate > 5 ? 600 : 400 }">{{ row.errorRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="时延" min-width="70">
            <template #default="{ row }"><span style="font-size: 12px;">{{ row.latency }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="showChannelDetail(row.channelId)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; padding: 12px 0 0;">
          <el-pagination
            small
            layout="total, prev, pager, next"
            :total="15"
            :page-size="10"
          />
        </div>
      </el-card>
    </div>

    <!-- ==================== Section 4: 告警规则 ==================== -->
    <div v-if="activeTab === 'rules'">
      <div class="page-header-row">
        <div class="section-title">告警规则配置</div>
        <div style="display: flex; align-items: center; gap: 10px;">
          <span style="font-size: 13px; color: #6b7280;">维护窗口</span>
          <el-button size="small" @click="maintenanceDialogVisible = true">设置维护窗口</el-button>
        </div>
      </div>

      <!-- 通道告警规则 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <span class="card-title">通道告警规则</span>
        </template>
        <div class="rule-list">
          <div v-for="(rule, idx) in channelAlertRules" :key="idx" class="rule-item">
            <div class="rule-header">
              <div class="rule-name">{{ rule.name }}</div>
              <el-tag :type="rule.level === 'critical' ? 'danger' : 'warning'" size="small" effect="light">{{ rule.levelText }}</el-tag>
              <el-switch v-model="rule.enabled" style="margin-left: auto;" @change="val => onRuleToggle(rule, val)" />
              <el-button type="primary" link size="small">配置通知</el-button>
            </div>
            <div class="rule-desc">
              <template v-if="rule.hasThreshold">
                {{ rule.descPrefix }}
                <el-input-number v-model="rule.threshold" :min="0" size="small" style="width: 70px; margin: 0 4px;" controls-position="right" />
                {{ rule.descMiddle }}
                <template v-if="rule.hasDuration">
                  <el-input-number v-model="rule.duration" :min="1" size="small" style="width: 60px; margin: 0 4px;" controls-position="right" />
                  {{ rule.descSuffix }}
                </template>
                <template v-else>
                  {{ rule.descSuffix }}
                </template>
              </template>
              <template v-else>
                {{ rule.description }}
              </template>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 业务告警规则 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <span class="card-title">业务告警规则</span>
        </template>
        <div class="rule-list">
          <div v-for="(rule, idx) in businessAlertRules" :key="idx" class="rule-item">
            <div class="rule-header">
              <div class="rule-name">{{ rule.name }}</div>
              <el-tag :type="rule.tagType" size="small" effect="light">{{ rule.levelText }}</el-tag>
              <el-switch v-model="rule.enabled" style="margin-left: auto;" @change="val => onRuleToggle(rule, val)" />
              <el-button type="primary" link size="small">配置通知</el-button>
            </div>
            <div class="rule-desc">
              <template v-if="rule.hasThreshold">
                {{ rule.descPrefix }}
                <el-input-number v-model="rule.threshold" :min="0" size="small" style="width: 70px; margin: 0 4px;" controls-position="right" />
                {{ rule.descSuffix }}
              </template>
              <template v-else>
                {{ rule.description }}
              </template>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 通知渠道配置 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">通知渠道（全局默认）</span>
            <el-button type="primary" size="small">保存配置</el-button>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="notify-row">
              <span class="notify-label">站内通知</span>
              <el-switch v-model="notifyConfig.inApp" />
              <span class="notify-scope success">所有级别</span>
            </div>
            <div class="notify-row">
              <span class="notify-label">邮件通知</span>
              <el-switch v-model="notifyConfig.email" />
              <span class="notify-scope">警告 / 严重</span>
            </div>
            <div class="notify-row">
              <span class="notify-label">短信通知</span>
              <el-switch v-model="notifyConfig.sms" />
              <span class="notify-scope">仅严重</span>
            </div>
          </el-col>
          <el-col :span="12">
            <el-form label-position="top" size="small">
              <el-form-item label="通知邮箱（多个用逗号分隔）">
                <el-input v-model="notifyConfig.emailAddr" placeholder="ops@company.com, tech@company.com" />
              </el-form-item>
              <el-form-item label="通知手机号（严重告警）">
                <el-input v-model="notifyConfig.phone" placeholder="+86 138 0000 0000" />
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
        <div class="convergence-bar">
          <span style="flex: 1;">告警收敛：同一告警持续触发时，每
            <el-input-number v-model="notifyConfig.convergenceMinutes" :min="1" size="small" style="width: 60px; margin: 0 4px;" controls-position="right" />
            分钟重复通知一次
          </span>
        </div>
      </el-card>

      <!-- 维护窗口弹窗 -->
      <el-dialog v-model="maintenanceDialogVisible" title="设置维护窗口" width="640px" destroy-on-close>
        <p style="font-size: 13px; color: #6b7280; margin-bottom: 16px;">维护窗口期间告警规则仍然运行，但不发送通知，避免计划维护期间的告警轰炸。</p>
        <el-form label-position="top">
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="开始时间">
                <el-date-picker v-model="maintenanceForm.startTime" type="datetime" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束时间">
                <el-date-picker v-model="maintenanceForm.endTime" type="datetime" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="抑制范围">
                <el-select v-model="maintenanceForm.scope" style="width: 100%;">
                  <el-option label="全部告警" value="all" />
                  <el-option label="仅通道告警" value="channel" />
                  <el-option label="指定通道" value="specific" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="maintenanceForm.remark" placeholder="如：泰国AIS通道计划维护" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <el-button @click="maintenanceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="maintenanceDialogVisible = false">保存维护窗口</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- ==================== Section 5: 队列监控 ==================== -->
    <div v-if="activeTab === 'queue'">
      <!-- KPI 行 -->
      <el-row :gutter="14" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">全局队列总深度</div>
            <div class="stat-value">{{ queueStatsData.pendingCount.toLocaleString() }}</div>
            <div class="stat-sub">待处理消息</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">全局实际 TPS</div>
            <div class="stat-value">{{ queueStatsData.processingCount.toLocaleString() }}<span style="font-size: 14px; font-weight: 500; color: #6b7280;"> 处理中</span></div>
            <div class="stat-sub">正在处理的消息</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">重试队列大小</div>
            <div class="stat-value" style="color: #d97706;">{{ queueStatsData.failedCount.toLocaleString() }}</div>
            <div class="stat-sub">失败待重试</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">平均队列等待时长</div>
            <div class="stat-value">1.8<span style="font-size: 14px; font-weight: 500; color: #6b7280;">min</span></div>
            <div class="stat-sub">告警阈值：5 分钟</div>
          </div>
        </el-col>
      </el-row>

      <!-- 通道队列实时状态 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">通道队列实时状态</span>
            <span style="font-size: 12px; color: #6b7280; display: flex; align-items: center; gap: 6px;">
              <span class="live-dot"></span>
              实时刷新（30s）
            </span>
          </div>
        </template>
        <el-table :data="queueChannelData" :header-cell-style="headerCellStyle" size="small">
          <el-table-column label="通道" min-width="110">
            <template #default="{ row }">
              <span style="font-family: monospace; font-size: 11px;">{{ row.channel }}</span>
            </template>
          </el-table-column>
          <el-table-column label="供应商" prop="vendor" min-width="80" />
          <el-table-column label="国家" prop="country" min-width="70" />
          <el-table-column label="TPS上限" prop="tpsLimit" min-width="80" align="right" />
          <el-table-column label="实际速率" prop="actualTps" min-width="80" align="right" />
          <el-table-column label="TPS使用率" min-width="140">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-progress
                  :percentage="row.tpsUsagePct"
                  :stroke-width="5"
                  :show-text="false"
                  :color="getQueueProgressColor(row.tpsUsagePct, row.status)"
                  style="flex: 1;"
                />
                <span :style="{ fontSize: '11px', color: getQueueTpsColor(row.tpsUsagePct, row.status), fontWeight: 600 }">{{ row.tpsUsagePct }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="通知类积压" prop="notifyBacklog" min-width="90" align="right">
            <template #default="{ row }">{{ row.notifyBacklog.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="营销类积压" prop="marketBacklog" min-width="90" align="right">
            <template #default="{ row }">{{ row.marketBacklog.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="总队列深度" min-width="100" align="right">
            <template #default="{ row }">
              <strong :style="{ color: row.status === 'fuse' ? '#e63946' : undefined }">{{ row.totalBacklog.toLocaleString() }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="令牌桶" min-width="120">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 4px; font-size: 11px;">
                <el-progress
                  :percentage="row.tokenPct"
                  :stroke-width="6"
                  :show-text="false"
                  :color="row.status === 'fuse' ? '#e63946' : '#4361ee'"
                  style="width: 40px;"
                />
                <span>{{ row.tokenUsed }}/{{ row.tokenTotal }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small" effect="light">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 客户队列状态 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">客户队列状态（活跃客户）</span>
            <el-button size="small">刷新</el-button>
          </div>
        </template>
        <el-table :data="queueCustomerData" :header-cell-style="headerCellStyle" size="small">
          <el-table-column label="客户名称" min-width="150">
            <template #default="{ row }"><strong>{{ row.name }}</strong></template>
          </el-table-column>
          <el-table-column label="通知类队列" min-width="100" align="right">
            <template #default="{ row }">{{ row.notifyQueue.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="营销类队列" min-width="100" align="right">
            <template #default="{ row }">{{ row.marketQueue.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="当前TPS" prop="currentTps" min-width="80" align="right" />
          <el-table-column label="TPS上限" prop="tpsLimit" min-width="80" align="right" />
          <el-table-column label="使用率" min-width="130">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 5px;">
                <el-progress
                  :percentage="row.usagePct"
                  :stroke-width="4"
                  :show-text="false"
                  :color="row.usagePct >= 100 ? '#d97706' : (row.usagePct === 0 ? '#e63946' : '#2ec4b6')"
                  style="flex: 1;"
                />
                <span :style="{ fontSize: '11px', color: row.usagePct >= 100 ? '#d97706' : (row.usagePct === 0 ? '#e63946' : '#0e9488') }">{{ row.usagePct }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="今日发送量" min-width="110" align="right">
            <template #default="{ row }">{{ row.todaySent.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="日配额" min-width="110" align="right">
            <template #default="{ row }">{{ row.dailyQuota ? row.dailyQuota.toLocaleString() : '--' }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small" effect="light">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 重试队列 -->
      <el-card shadow="never" class="box-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">重试队列</span>
            <div style="display: flex; gap: 8px; align-items: center;">
              <span style="font-size: 12px; color: #d97706;">{{ queueStatsData.failedCount.toLocaleString() }} 条待重试</span>
              <el-button size="small">导出</el-button>
              <el-button size="small" type="danger" link>清空全部</el-button>
            </div>
          </div>
        </template>
        <!-- 筛选 -->
        <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #e9ecef; margin-bottom: 14px;">
          <div class="filter-item">
            <label>客户</label>
            <el-select v-model="retryFilter.customer" size="small" style="width: 140px;" clearable filterable placeholder="全部">
              <el-option label="全部" value="" />
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>通道</label>
            <el-select v-model="retryFilter.channel" size="small" style="width: 130px;" clearable filterable placeholder="全部">
              <el-option label="全部" value="" />
              <el-option v-for="ch in channelOptions" :key="ch.id" :label="ch.channelName || ch.name" :value="ch.channelCode || ch.channelName" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>失败原因</label>
            <el-select v-model="retryFilter.reason" size="small" style="width: 140px;" clearable placeholder="全部">
              <el-option label="全部" value="" />
              <el-option label="通道连接失败" value="connect_fail" />
              <el-option label="运营商拒绝" value="rejected" />
              <el-option label="超时未回执" value="timeout" />
            </el-select>
          </div>
          <el-button type="primary" size="small" style="margin-left: auto;">查询</el-button>
        </div>
        <el-table :data="retryQueueData" :header-cell-style="headerCellStyle" size="small">
          <el-table-column label="消息 ID" min-width="120">
            <template #default="{ row }"><span style="font-family: monospace; font-size: 11px;">{{ row.msgId }}</span></template>
          </el-table-column>
          <el-table-column label="客户" prop="customer" min-width="130" />
          <el-table-column label="目标号码" min-width="120">
            <template #default="{ row }"><span style="font-family: monospace; font-size: 11px;">{{ row.phone }}</span></template>
          </el-table-column>
          <el-table-column label="通道" min-width="110">
            <template #default="{ row }"><span style="font-family: monospace; font-size: 11px;">{{ row.channel }}</span></template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="130">
            <template #default="{ row }">
              <span :style="{ fontSize: '11px', color: row.reasonColor }">{{ row.reason }}</span>
            </template>
          </el-table-column>
          <el-table-column label="重试次数" prop="retryCount" min-width="80" align="right" />
          <el-table-column label="下次重试" min-width="90">
            <template #default="{ row }"><span style="font-size: 11px;">{{ row.nextRetry }}</span></template>
          </el-table-column>
          <el-table-column label="策略" min-width="100">
            <template #default="{ row }"><span style="font-size: 11px;">{{ row.strategy }}</span></template>
          </el-table-column>
          <el-table-column label="操作" min-width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.maxReached">
                <el-button type="danger" link size="small">查看详情</el-button>
              </template>
              <template v-else>
                <el-button type="primary" link size="small">立即重试</el-button>
                <el-button type="danger" link size="small">放弃</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; align-items: center; padding: 12px 0 0; gap: 10px;">
          <span style="font-size: 12px; color: #6b7280; flex: 1;">共 {{ queueStatsData.failedCount.toLocaleString() }} 条 / 等待重试</span>
          <el-pagination
            small
            layout="prev, pager, next"
            :total="queueStatsData.failedCount"
            :page-size="10"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  monitorDashboard,
  alertRecordPage,
  alertAcknowledge,
  alertResolve,
  channelMonitorList,
  alertRuleList,
  alertRuleToggle,
  queueStats,
  customerList, countryAll, channelList
} from '../api'

/* ── Tab state ── */
const activeTab = ref('dashboard')
const alertBadgeCount = ref(0)

/* ── Header cell style ── */
const headerCellStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

/* ── Reference data ── */
const customerOptions = ref([])
const countryOptions = ref([])
const channelOptions = ref([])
const loadRefData = async () => {
  try {
    const [cRes, coRes, chRes] = await Promise.allSettled([
      customerList({ page: 1, size: 9999 }),
      countryAll(),
      channelList({ page: 1, size: 9999 })
    ])
    if (cRes.status === 'fulfilled') customerOptions.value = (cRes.value.data?.list || cRes.value.data || [])
    if (coRes.status === 'fulfilled') countryOptions.value = coRes.value.data || []
    if (chRes.status === 'fulfilled') channelOptions.value = (chRes.value.data?.list || chRes.value.data || [])
  } catch { /* ignore */ }
}

/* ── 实时看板 ── */
const dashboardStats = reactive({
  criticalAlerts: 0,
  warnings: 0,
  onlineChannels: 0,
  totalChannels: 0,
  globalDeliveryRate: 0,
  todayTotal: 0,
  todayDelivered: 0
})

const channelHealthData = ref([])

/* ── 队列统计 ── */
const queueStatsData = reactive({
  pendingCount: 0,
  processingCount: 0,
  failedCount: 0
})

/* ── Format large numbers ── */
function formatLargeNumber(n) {
  if (n >= 1000000) return (n / 1000000).toFixed(2) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n)
}

/* ── Load dashboard data ── */
async function loadDashboard() {
  try {
    const res = await monitorDashboard()
    const d = res.data
    dashboardStats.criticalAlerts = d.criticalAlerts ?? 0
    dashboardStats.warnings = d.warningAlerts ?? 0
    dashboardStats.onlineChannels = d.activeChannels ?? 0
    dashboardStats.totalChannels = d.totalChannels ?? 0
    dashboardStats.globalDeliveryRate = d.deliveryRate ?? 0
    dashboardStats.todayTotal = d.todayTotal ?? 0
    dashboardStats.todayDelivered = d.todayDelivered ?? 0
    alertBadgeCount.value = (d.criticalAlerts ?? 0) + (d.warningAlerts ?? 0)
  } catch { /* silent */ }
}

/* ── Load channel health for dashboard ── */
async function loadChannelHealth() {
  try {
    const res = await channelMonitorList()
    const list = res.data || []
    channelHealthData.value = list.map(ch => {
      const rate = ch.deliveryRate ?? 0
      const errRate = ch.todayTotal > 0 ? ((ch.todayFailed / ch.todayTotal) * 100) : 0
      let statusType = 'success'
      let statusText = '在线'
      let rowHighlight = ''
      if (ch.status === 'OFFLINE' || ch.status === 'FUSED') {
        statusType = 'danger'
        statusText = ch.status === 'FUSED' ? '熔断' : '离线'
        rowHighlight = 'danger-row'
      } else if (rate < 85 || errRate > 10) {
        statusType = 'danger'
        statusText = rate < 85 ? '送达率低' : '错误率高'
        rowHighlight = 'danger-row'
      } else if (errRate > 5) {
        statusType = 'warning'
        statusText = '错误率偏高'
        rowHighlight = 'warning-row'
      }
      return {
        channel: ch.channelName || ch.channelCode || ch.channelId,
        vendor: ch.vendorName || '',
        country: ch.countryCode || '',
        statusType,
        statusText,
        tpsUsage: ch.tps > 0 ? Math.round((ch.todayTotal / (ch.tps * 86400)) * 100) : 0,
        deliveryRate: parseFloat(rate.toFixed(1)),
        errorRate: parseFloat(errRate.toFixed(1)),
        latency: (ch.avgLatency ?? 0) + 'ms',
        queueBacklog: 0,
        rowHighlight
      }
    })
  } catch { /* silent */ }
}

/* ── Load queue stats ── */
async function loadQueueStats() {
  try {
    const res = await queueStats()
    const d = res.data || {}
    queueStatsData.pendingCount = d.pendingCount ?? 0
    queueStatsData.processingCount = d.processingCount ?? 0
    queueStatsData.failedCount = d.failedCount ?? 0
  } catch { /* silent */ }
}

function dashboardRowClass({ row }) {
  return row.rowHighlight || ''
}

function getProgressColor(pct) {
  if (pct >= 85) return '#d97706'
  if (pct >= 60) return '#4361ee'
  return '#16a34a'
}

/* ── 告警中心 ── */
const alertFilter = ref('all')
const alertObjectFilter = ref('')
const ackDialogVisible = ref(false)
const resolveDialogVisible = ref(false)
const resolveForm = reactive({ result: 'resolved', note: '' })
const currentAlertItem = ref(null)

const alertList = ref([])
const alertTotal = ref(0)
const alertPage = reactive({ page: 1, size: 50 })

async function loadAlerts() {
  try {
    const params = { page: alertPage.page, size: alertPage.size }
    if (alertFilter.value === 'critical') params.severity = 'CRITICAL'
    else if (alertFilter.value === 'warning') params.severity = 'WARNING'
    if (alertFilter.value === 'resolved') params.status = 'RESOLVED'
    const res = await alertRecordPage(params)
    const d = res.data || {}
    const list = d.list || []
    alertTotal.value = d.total || 0
    alertList.value = list.map(a => {
      const sev = (a.severity || '').toUpperCase()
      const st = (a.status || '').toUpperCase()
      const isResolved = st === 'RESOLVED'
      const isHandling = st === 'ACKNOWLEDGED'
      let level = 'warning'
      let levelText = '警告'
      if (sev === 'CRITICAL') { level = 'critical'; levelText = '严重' }
      else if (isResolved) { level = 'success'; levelText = '已恢复' }
      return {
        id: a.id,
        name: a.title || a.ruleName || '',
        level,
        levelText,
        category: a.ruleName || '',
        categoryKey: 'channel',
        detail: a.content || '',
        timeInfo: a.createdAt ? ('触发时间：' + a.createdAt) : '',
        handling: isHandling,
        handlingInfo: isHandling ? ('处理中 -- ' + (a.acknowledgedBy || '')) : '',
        resolved: isResolved
      }
    })
  } catch { /* silent */ }
}

const alertCounts = computed(() => {
  const c = alertList.value.filter(a => a.level === 'critical' && !a.resolved).length
  const w = alertList.value.filter(a => a.level === 'warning' && !a.resolved).length
  const r = alertList.value.filter(a => a.resolved).length
  return { critical: c, warning: w, resolved: r }
})

const filteredAlerts = computed(() => {
  return alertList.value.filter(a => {
    if (alertFilter.value === 'critical') return a.level === 'critical' && !a.resolved
    if (alertFilter.value === 'warning') return a.level === 'warning' && !a.resolved
    if (alertFilter.value === 'resolved') return a.resolved
    if (alertObjectFilter.value && a.categoryKey !== alertObjectFilter.value) return false
    return true
  })
})

function handleAcknowledge(alert) {
  currentAlertItem.value = alert
  ackDialogVisible.value = true
}

async function confirmAcknowledge() {
  const alert = currentAlertItem.value
  if (!alert) return
  try {
    await alertAcknowledge(alert.id, 'admin')
    ElMessage.success('认领成功')
    ackDialogVisible.value = false
    await loadAlerts()
  } catch {
    ElMessage.error('认领失败')
  }
}

function handleResolve(alert) {
  currentAlertItem.value = alert
  resolveDialogVisible.value = true
}

async function confirmResolve() {
  const alert = currentAlertItem.value
  if (!alert) return
  try {
    await alertResolve(alert.id)
    ElMessage.success('已标记处理完成')
    resolveDialogVisible.value = false
    await loadAlerts()
  } catch {
    ElMessage.error('操作失败')
  }
}

/* Watch filter changes to reload alerts */
watch([alertFilter, alertObjectFilter], () => {
  loadAlerts()
})

/* ── 通道监控 ── */
const channelFilter = reactive({ country: '', status: '', protocol: '' })
const channelDetailVisible = ref(false)
const channelDetailName = ref('')

const channelMetrics = ref([
  { label: 'TPS 使用率', pct: 30, color: '#4361ee', value: '30%', valColor: undefined },
  { label: '送达率', pct: 64, color: '#e63946', value: '64.2%', valColor: '#e63946' },
  { label: '错误率', pct: 18, color: '#e63946', value: '18.4%', valColor: '#e63946' },
  { label: '平均时延', pct: 74, color: '#d97706', value: '22.1s', valColor: undefined }
])

const channelRecentAlerts = ref([
  { time: '14:48', name: '送达率严重下降', level: 'critical', levelText: '严重', triggerValue: '64.2%（阈值 70%）', status: 'alerting', statusText: '告警中' },
  { time: '13:20', name: '平均时延过高', level: 'warning', levelText: '警告', triggerValue: '28.4s（阈值 60s）', status: 'recovered', statusText: '已恢复' }
])

const channelListData = ref([])

async function loadChannelList() {
  try {
    const res = await channelMonitorList()
    const list = res.data || []
    channelListData.value = list.map(ch => {
      const rate = ch.deliveryRate ?? 0
      const errRate = ch.todayTotal > 0 ? ((ch.todayFailed / ch.todayTotal) * 100) : 0
      let statusType = 'success'
      let statusText = '在线'
      let rowHighlight = ''
      if (ch.status === 'OFFLINE' || ch.status === 'FUSED') {
        statusType = 'danger'
        statusText = ch.status === 'FUSED' ? '熔断' : '离线'
        rowHighlight = 'danger-row'
      } else if (rate < 85) {
        statusType = 'danger'
        statusText = '送达率低'
        rowHighlight = 'danger-row'
      } else if (errRate > 5) {
        statusType = 'warning'
        statusText = '错误率偏高'
        rowHighlight = 'warning-row'
      }
      return {
        channelId: ch.channelName || ch.channelCode || ch.channelId,
        vendor: ch.vendorName || '',
        countryFlag: ch.countryCode || '',
        protocol: ch.channelType === 1 ? 'SMPP' : ch.channelType === 2 ? 'HTTP' : '',
        statusType,
        statusText,
        tpsUsage: ch.tps > 0 ? Math.round((ch.todayTotal / (ch.tps * 86400)) * 100) : 0,
        deliveryRate: parseFloat(rate.toFixed(1)),
        errorRate: parseFloat(errRate.toFixed(1)),
        latency: (ch.avgLatency ?? 0) + 'ms',
        rowHighlight
      }
    })
  } catch { /* silent */ }
}

function channelRowClass({ row }) {
  return row.rowHighlight || ''
}

function showChannelDetail(name) {
  channelDetailName.value = name
  channelDetailVisible.value = true
  if (activeTab.value !== 'channels') {
    activeTab.value = 'channels'
  }
}

/* ── 告警规则 ── */
const maintenanceDialogVisible = ref(false)
const maintenanceForm = reactive({ startTime: '', endTime: '', scope: 'all', remark: '' })

const channelAlertRules = ref([])
const businessAlertRules = ref([])

async function loadAlertRules() {
  try {
    const res = await alertRuleList()
    const list = res.data || []
    const channelRules = []
    const bizRules = []
    list.forEach(r => {
      const sev = (r.severity || '').toUpperCase()
      const type = (r.ruleType || '').toLowerCase()
      const base = {
        id: r.id,
        name: r.ruleName || '',
        level: sev === 'CRITICAL' ? 'critical' : 'warning',
        levelText: sev === 'CRITICAL' ? '严重' : (sev === 'WARNING' ? '警告' : '提醒'),
        tagType: sev === 'CRITICAL' ? 'danger' : (sev === 'WARNING' ? 'warning' : ''),
        enabled: !!r.isActive,
        hasThreshold: r.threshold != null && r.threshold > 0,
        threshold: r.threshold ?? 0,
        hasDuration: false,
        descPrefix: r.conditionExpr || '',
        descMiddle: '',
        descSuffix: '',
        description: r.conditionExpr || ''
      }
      if (type === 'channel' || type === 'CHANNEL') {
        channelRules.push(base)
      } else {
        bizRules.push(base)
      }
    })
    channelAlertRules.value = channelRules
    businessAlertRules.value = bizRules
  } catch { /* silent */ }
}

async function onRuleToggle(rule, newVal) {
  try {
    await alertRuleToggle(rule.id, newVal)
    ElMessage.success(newVal ? '规则已启用' : '规则已停用')
  } catch {
    rule.enabled = !newVal
    ElMessage.error('操作失败')
  }
}

const notifyConfig = reactive({
  inApp: true,
  email: true,
  sms: true,
  emailAddr: 'ops@***.com, tech@***.com',
  phone: '+86 138****0000',
  convergenceMinutes: 30
})

/* ── 队列监控 (static placeholders for sub-tables) ── */
const queueChannelData = ref([
  { channel: 'TH-AIS-01', vendor: 'SINCH', country: '泰国', tpsLimit: 200, actualTps: 182, tpsUsagePct: 91, notifyBacklog: 12400, marketBacklog: 8200, totalBacklog: 20600, tokenPct: 98, tokenUsed: 196, tokenTotal: 200, status: 'warn', statusType: 'warning', statusText: 'TPS过高' },
  { channel: 'SG-SINCH-01', vendor: 'SINCH', country: '新加坡', tpsLimit: 100, actualTps: 68, tpsUsagePct: 68, notifyBacklog: 4200, marketBacklog: 6100, totalBacklog: 10300, tokenPct: 65, tokenUsed: 130, tokenTotal: 200, status: 'ok', statusType: 'success', statusText: '正常' },
  { channel: 'US-TWILIO-01', vendor: 'TWILIO', country: '美国', tpsLimit: 150, actualTps: 42, tpsUsagePct: 28, notifyBacklog: 800, marketBacklog: 3200, totalBacklog: 4000, tokenPct: 28, tokenUsed: 84, tokenTotal: 300, status: 'ok', statusType: 'success', statusText: '正常' },
  { channel: 'JP-SINCH-01', vendor: 'SINCH', country: '日本', tpsLimit: 80, actualTps: 0, tpsUsagePct: 0, notifyBacklog: 3600, marketBacklog: 4310, totalBacklog: 7910, tokenPct: 0, tokenUsed: 0, tokenTotal: 160, status: 'fuse', statusType: 'danger', statusText: '已熔断' },
  { channel: 'HK-CMHK-01', vendor: 'CMHK', country: '香港', tpsLimit: 120, actualTps: 98, tpsUsagePct: 82, notifyBacklog: 0, marketBacklog: 0, totalBacklog: 0, tokenPct: 82, tokenUsed: 196, tokenTotal: 240, status: 'ok', statusType: 'success', statusText: '正常' }
])

function getQueueProgressColor(pct, status) {
  if (status === 'fuse') return '#e63946'
  if (pct >= 85) return '#d97706'
  return '#2ec4b6'
}

function getQueueTpsColor(pct, status) {
  if (status === 'fuse') return '#e63946'
  if (pct >= 85) return '#d97706'
  return '#0e9488'
}

const queueCustomerData = ref([
  { name: 'Alibaba Cloud CN', notifyQueue: 820, marketQueue: 18400, currentTps: 200, tpsLimit: 200, usagePct: 100, todaySent: 8421380, dailyQuota: null, statusType: 'warning', statusText: 'TPS满载' },
  { name: 'JD Finance HK', notifyQueue: 12400, marketQueue: 3200, currentTps: 120, tpsLimit: 150, usagePct: 80, todaySent: 5203100, dailyQuota: 10000000, statusType: 'success', statusText: '正常' },
  { name: 'ByteDance SG', notifyQueue: 0, marketQueue: 4800, currentTps: 85, tpsLimit: 100, usagePct: 85, todaySent: 3870500, dailyQuota: 8000000, statusType: 'success', statusText: '正常' },
  { name: 'Grab SG', notifyQueue: 0, marketQueue: 0, currentTps: 0, tpsLimit: 50, usagePct: 0, todaySent: 1882400, dailyQuota: 3000000, statusType: 'info', statusText: '空闲' }
])

const retryFilter = reactive({ customer: '', channel: '', reason: '' })

const retryQueueData = ref([
  { msgId: 'MSG-8841120', customer: 'JD Finance HK', phone: '+81 9012xxx', channel: 'JP-SINCH-01', reason: '通道熔断', reasonColor: '#e63946', retryCount: 1, nextRetry: '09:48:22', strategy: '指数退避 x 3', maxReached: false },
  { msgId: 'MSG-8841098', customer: 'Alibaba Cloud CN', phone: '+81 8044xxx', channel: 'JP-SINCH-01', reason: '通道熔断', reasonColor: '#e63946', retryCount: 2, nextRetry: '09:50:10', strategy: '指数退避 x 3', maxReached: false },
  { msgId: 'MSG-8840882', customer: 'ByteDance SG', phone: '+44 7911xxx', channel: 'EU-NEXMO-01', reason: '运营商拒绝 #11', reasonColor: '#d97706', retryCount: 3, nextRetry: '--', strategy: '已达上限', maxReached: true },
  { msgId: 'MSG-8840710', customer: 'DBS Bank', phone: '+65 9188xxx', channel: 'SG-SINCH-01', reason: '超时未回执', reasonColor: '#6b7280', retryCount: 1, nextRetry: '09:52:00', strategy: '指数退避 x 3', maxReached: false }
])

/* ── Auto-refresh (30s) ── */
let refreshTimer = null

async function refreshAll() {
  await Promise.all([loadDashboard(), loadChannelHealth(), loadQueueStats()])
}

onMounted(async () => {
  loadRefData()
  await refreshAll()
  loadAlerts()
  loadChannelList()
  loadAlertRules()
  refreshTimer = setInterval(refreshAll, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.monitoring-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 17px;
  font-weight: 700;
  color: #343a40;
  margin: 0;
}

/* Sub-nav pill style */
.sub-nav {
  margin-bottom: 20px;
}

.sub-nav :deep(.el-radio-group) {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 4px;
}

.sub-nav :deep(.el-radio-button__inner) {
  border: none;
  border-radius: 7px !important;
  padding: 7px 18px;
  font-size: 13px;
  font-weight: 500;
  color: #6c757d;
  background: transparent;
  box-shadow: none !important;
}

.sub-nav :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #4361ee;
  color: #fff;
  box-shadow: none !important;
}

/* Stat cards */
.stat-card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 16px 20px;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}

.stat-critical::before { background: #e63946; }
.stat-warning::before { background: #d97706; }
.stat-ok::before { background: #16a34a; }
.stat-info::before { background: #4361ee; }

.stat-label {
  font-size: 12px;
  color: #adb5bd;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #343a40;
}

.stat-sub {
  font-size: 11px;
  color: #adb5bd;
  margin-top: 4px;
}

/* KPI cards */
.kpi-card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 14px 18px;
}

.kpi-label {
  font-size: 12px;
  color: #adb5bd;
  margin-bottom: 6px;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  color: #343a40;
}

.kpi-unit {
  font-size: 13px;
  color: #adb5bd;
  font-weight: 400;
}

.kpi-trend {
  font-size: 11px;
  margin-top: 4px;
}

.kpi-trend.up { color: #16a34a; }
.kpi-trend.neutral { color: #adb5bd; }

/* Card header row */
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #343a40;
}

/* Box card override */
.box-card {
  margin-bottom: 16px;
}

.box-card :deep(.el-card__header) {
  padding: 14px 20px;
}

.box-card :deep(.el-card__body) {
  padding: 20px;
}

/* Page header row */
.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 17px;
  font-weight: 700;
  color: #343a40;
}

.section-desc {
  font-size: 13px;
  color: #adb5bd;
  margin-top: 2px;
}

/* Alert items */
.alert-item {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  transition: box-shadow 0.15s;
}

.alert-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.alert-item.critical {
  border-left: 3px solid #e63946;
}

.alert-item.warning {
  border-left: 3px solid #d97706;
}

.alert-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 3px;
}

.dot-critical {
  background: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.2);
}

.dot-warning {
  background: #d97706;
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.2);
}

.dot-success {
  background: #16a34a;
}

.alert-body {
  flex: 1;
}

.alert-name {
  font-size: 13px;
  font-weight: 600;
  color: #343a40;
  margin-bottom: 3px;
}

.alert-meta {
  font-size: 12px;
  color: #adb5bd;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.alert-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  align-items: flex-start;
}

/* Row highlighting */
:deep(.warning-row) {
  background-color: #fffbeb !important;
}

:deep(.danger-row) {
  background-color: #fef2f2 !important;
}

/* Filter items */
.filter-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.filter-item label {
  font-size: 12px;
  color: #6c757d;
  white-space: nowrap;
}

/* Metric bar */
.metric-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}

.metric-bar-label {
  font-size: 12px;
  color: #6c757d;
  width: 80px;
  flex-shrink: 0;
}

.metric-bar-track {
  flex: 1;
  height: 6px;
  background: #e9ecef;
  border-radius: 3px;
  overflow: hidden;
}

.metric-bar-fill {
  height: 100%;
  border-radius: 3px;
}

.metric-bar-val {
  font-size: 12px;
  color: #495057;
  width: 50px;
  text-align: right;
  flex-shrink: 0;
}

/* Trend chart */
.trend-wrap {
  position: relative;
  width: 100%;
  height: 160px;
  background: #f9fafb;
  border-radius: 8px;
  overflow: hidden;
}

.trend-wrap svg {
  width: 100%;
  height: 100%;
}

/* Rule items */
.rule-list {
  margin: -20px;
}

.rule-item {
  padding: 16px 20px;
  border-bottom: 1px solid #e9ecef;
}

.rule-item:last-child {
  border-bottom: none;
}

.rule-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.rule-name {
  font-size: 13px;
  font-weight: 600;
  color: #343a40;
  flex: 1;
}

.rule-desc {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

/* Notify config */
.notify-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #495057;
}

.notify-label {
  min-width: 70px;
}

.notify-scope {
  font-size: 12px;
  color: #adb5bd;
}

.notify-scope.success {
  color: #16a34a;
}

.convergence-bar {
  margin-top: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #6c757d;
}

/* Live dot */
.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2ec4b6;
  display: inline-block;
  animation: pulse-green 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.2); }
  50% { box-shadow: 0 0 0 5px rgba(230, 57, 70, 0.35); }
}

@keyframes pulse-green {
  0%, 100% { box-shadow: 0 0 0 2px rgba(46, 196, 182, 0.2); }
  50% { box-shadow: 0 0 0 4px rgba(46, 196, 182, 0.35); }
}
</style>
