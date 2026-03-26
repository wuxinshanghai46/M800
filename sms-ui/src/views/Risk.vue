<template>
  <div class="risk-page">
    <div class="page-header"><h2>风控管理</h2></div>

    <!-- Sub-nav pill buttons -->
    <div class="sub-nav">
      <el-radio-group v-model="activeTab" size="default" @change="onTabChange">
        <el-radio-button value="blacklist">黑名单管理</el-radio-button>
        <el-radio-button value="sensitive">敏感词管理</el-radio-button>
        <el-radio-button value="rateLimit">频次限制</el-radio-button>
        <el-radio-button value="ipWhitelist">IP白名单</el-radio-button>
        <el-radio-button value="interceptLog">拦截日志</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ==================== Section 1: 黑名单管理 ==================== -->
    <div v-if="activeTab === 'blacklist'">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>优先级：全局黑名单 > 客户级黑名单</template>
      </el-alert>

      <!-- Stat cards -->
      <el-row :gutter="16" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">号码黑名单</div>
            <div class="stat-value" style="color: #e74c3c;">{{ blStats.number }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">前缀黑名单</div>
            <div class="stat-value" style="color: #f39c12;">{{ blStats.prefix }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">退订号码</div>
            <div class="stat-value" style="color: #3498db;">{{ blStats.optout }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">发件人ID黑名单</div>
            <div class="stat-value" style="color: #9b59b6;">{{ blStats.sid }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- Sub-tabs: 号码 / 前缀 / 退订号码 / 发件人ID -->
      <div class="sub-tab-bar">
        <el-radio-group v-model="blSubTab" size="small" @change="loadBl">
          <el-radio-button value="NUMBER">号码</el-radio-button>
          <el-radio-button value="PREFIX">前缀</el-radio-button>
          <el-radio-button value="OPTOUT">退订号码</el-radio-button>
          <el-radio-button value="SID">发件人ID</el-radio-button>
        </el-radio-group>
      </div>

      <!-- Filter bar -->
      <div class="filter-bar">
        <el-select v-model="blQuery.scope" placeholder="归属范围" clearable size="default" style="width: 140px;">
          <el-option label="全局" value="GLOBAL" />
          <el-option label="客户级" value="CUSTOMER" />
        </el-select>
        <el-select v-model="blQuery.countryCode" placeholder="国家" clearable filterable size="default" style="width: 140px;">
          <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} (${c.code})`" :value="c.code" />
        </el-select>
        <el-input v-model="blQuery.keyword" placeholder="号码 / 备注" clearable size="default" style="width: 200px;" @keyup.enter="loadBl" />
        <el-button type="primary" size="default" @click="loadBl">查询</el-button>
        <div style="flex: 1;" />
        <el-button size="default" @click="ElMessage.info('批量导入功能开发中')">批量导入</el-button>
        <el-button type="primary" size="default" @click="openBl()">
          <el-icon><Plus /></el-icon>{{ blSubTab === 'NUMBER' ? '添加号码' : blSubTab === 'PREFIX' ? '添加前缀' : blSubTab === 'SID' ? '添加发件人ID' : '添加退订号码' }}
        </el-button>
      </div>

      <!-- Table -->
      <el-table :data="blList" stripe v-loading="blLoading" :header-cell-style="headerStyle"
                @selection-change="blSelection = $event" style="font-size: 13px;">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="value" :label="blSubTab === 'SID' ? '发件人ID' : blSubTab === 'PREFIX' ? '前缀' : '号码'" min-width="180" />
        <el-table-column prop="scope" label="归属" width="100">
          <template #default="{ row }">
            <el-tag :type="row.scope === 'GLOBAL' ? 'danger' : ''" size="small" round>
              {{ row.scope === 'GLOBAL' ? '全局' : '客户级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerId" label="客户" width="130">
          <template #default="{ row }">
            {{ row.scope === 'CUSTOMER' ? (customerNameMap[row.customerId] || row.customerId || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="countryCode" label="国家" width="100" />
        <el-table-column prop="reason" label="原因" show-overflow-tooltip min-width="150" />
        <el-table-column prop="source" label="来源" width="100">
          <template #default="{ row }">{{ row.source || '手动添加' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="添加时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editBl(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="delBl(row.id)">
              <template #reference><el-button link type="danger" size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination v-model:current-page="blQuery.page" v-model:page-size="blQuery.size"
                       :total="blTotal" layout="total, prev, pager, next" @change="loadBl" />
      </div>
    </div>

    <!-- ==================== Section 2: 敏感词管理 ==================== -->
    <div v-else-if="activeTab === 'sensitive'">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>敏感词按全局 > 客户级优先级检测</template>
      </el-alert>

      <!-- Filter bar -->
      <div class="filter-bar">
        <el-select v-model="swQuery.matchType" placeholder="匹配类型" clearable size="default" style="width: 140px;">
          <el-option label="包含" value="CONTAINS" />
          <el-option label="精确" value="EXACT" />
          <el-option label="正则" value="REGEX" />
        </el-select>
        <el-select v-model="swQuery.action" placeholder="命中动作" clearable size="default" style="width: 140px;">
          <el-option label="拦截" value="BLOCK" />
          <el-option label="警告" value="WARN" />
          <el-option label="记录" value="LOG" />
        </el-select>
        <el-select v-model="swQuery.scope" placeholder="归属" clearable size="default" style="width: 140px;">
          <el-option label="全局" value="GLOBAL" />
          <el-option label="客户级" value="CUSTOMER" />
        </el-select>
        <el-input v-model="swQuery.keyword" placeholder="关键词" clearable size="default" style="width: 200px;" @keyup.enter="loadSw" />
        <el-button type="primary" size="default" @click="loadSw">查询</el-button>
        <div style="flex: 1;" />
        <el-button size="default" @click="ElMessage.info('批量导入功能开发中')">批量导入</el-button>
        <el-button type="primary" size="default" @click="openSw()">
          <el-icon><Plus /></el-icon>新增敏感词
        </el-button>
      </div>

      <!-- Table -->
      <el-table :data="swList" stripe v-loading="swLoading" :header-cell-style="headerStyle" style="font-size: 13px;">
        <el-table-column prop="word" label="关键词" min-width="160" />
        <el-table-column prop="matchType" label="匹配类型" width="100">
          <template #default="{ row }">{{ matchTypeMap[row.matchType] || row.matchType }}</template>
        </el-table-column>
        <el-table-column prop="action" label="命中动作" width="100">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small">{{ actionMap[row.action] || row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scope" label="归属" width="100">
          <template #default="{ row }">
            <el-tag :type="row.scope === 'GLOBAL' ? 'danger' : ''" size="small" round>
              {{ row.scope === 'GLOBAL' ? '全局' : '客户级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerId" label="客户" width="130">
          <template #default="{ row }">
            {{ row.scope === 'CUSTOMER' ? (customerNameMap[row.customerId] || row.customerId || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="countryCode" label="适用国家" width="100">
          <template #default="{ row }">{{ row.countryCode ? getCountryLabel(row.countryCode) : '全部' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip min-width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editSw(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="delSw(row.id)">
              <template #reference><el-button link type="danger" size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination v-model:current-page="swQuery.page" v-model:page-size="swQuery.size"
                       :total="swTotal" layout="total, prev, pager, next" @change="loadSw" />
      </div>
    </div>

    <!-- ==================== Section 3: 频次限制 ==================== -->
    <div v-else-if="activeTab === 'rateLimit'">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>频次规则按维度叠加检测，任意一条命中即触发动作。时间窗口使用 Redis 滑动窗口计数。</template>
      </el-alert>

      <div class="rule-grid">
        <!-- Left: Rule list -->
        <div class="rule-list-panel">
          <el-card shadow="never" class="rule-list-card">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 600;">规则列表</span>
                <el-button type="primary" size="small" @click="addRlRule">
                  <el-icon><Plus /></el-icon>新建
                </el-button>
              </div>
            </template>
            <div class="rule-items">
              <div v-for="(rule, idx) in rlRules" :key="idx"
                   class="rule-item" :class="{ active: rlSelectedIdx === idx }"
                   @click="rlSelectedIdx = idx">
                <div class="rule-item-name">{{ rule.name }}</div>
                <div class="rule-item-meta">
                  <el-tag size="small" :type="rule.level === '全局' ? 'danger' : rule.level === '客户级' ? '' : 'warning'" round>{{ rule.level }}</el-tag>
                  <span v-if="rule.bindLabel" style="color: #6b7280; font-size: 12px; margin-left: 4px;">{{ rule.bindLabel }}</span>
                </div>
                <div class="rule-item-bottom">
                  <span style="font-size: 12px; color: #374151;">{{ rule.limitDesc }}</span>
                  <el-tag :type="rule.enabled ? 'success' : 'info'" size="small">{{ rule.enabled ? '启用' : '禁用' }}</el-tag>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <!-- Right: Rule detail -->
        <div class="rule-detail-panel">
          <el-card shadow="never" v-if="rlSelected">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 600; font-size: 15px;">{{ rlSelected.name }}</span>
                <div style="display: flex; gap: 8px;">
                  <el-button :type="rlSelected.enabled ? 'warning' : 'success'" size="small"
                             @click="rlSelected.enabled = !rlSelected.enabled">
                    {{ rlSelected.enabled ? '禁用' : '启用' }}
                  </el-button>
                  <el-button type="primary" size="small" @click="ElMessage.success('保存成功')">保存</el-button>
                </div>
              </div>
            </template>
            <el-form :model="rlSelected" label-width="100px" label-position="right">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="规则名称">
                    <el-input v-model="rlSelected.name" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="规则维度">
                    <el-select v-model="rlSelected.dimension" style="width: 100%;">
                      <el-option label="单号码" value="单号码" />
                      <el-option label="客户整体" value="客户整体" />
                      <el-option label="客户×国家" value="客户×国家" />
                      <el-option label="通道" value="通道" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="归属层级">
                    <el-select v-model="rlSelected.level" style="width: 100%;">
                      <el-option label="全局" value="全局" />
                      <el-option label="客户级" value="客户级" />
                      <el-option label="通道级" value="通道级" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="绑定客户">
                    <el-select v-model="rlSelected.bindCustomer" placeholder="选择客户（客户级规则）" clearable filterable style="width: 100%;">
                      <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.name" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="时间窗口">
                    <el-select v-model="rlSelected.timeWindow" style="width: 100%;">
                      <el-option label="1 分钟" value="1min" />
                      <el-option label="5 分钟" value="5min" />
                      <el-option label="1 小时" value="1hour" />
                      <el-option label="1 天" value="1day" />
                      <el-option label="自定义秒数" value="custom" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="上限数量">
                    <el-input-number v-model="rlSelected.maxCount" :min="1" style="width: 100%;" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="命中动作">
                    <el-select v-model="rlSelected.hitAction" style="width: 100%;">
                      <el-option label="拦截" value="拦截" />
                      <el-option label="延迟排队" value="延迟排队" />
                      <el-option label="告警并继续" value="告警并继续" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="计数维度">
                    <el-select v-model="rlSelected.countMode" style="width: 100%;">
                      <el-option label="所有短信含分段" value="所有短信含分段" />
                      <el-option label="每条消息不拆段" value="每条消息不拆段" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="备注说明">
                    <el-input v-model="rlSelected.remark" type="textarea" :rows="3" placeholder="规则备注信息" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>
        </div>
      </div>
    </div>

    <!-- ==================== Section 4: IP白名单 ==================== -->
    <div v-else-if="activeTab === 'ipWhitelist'">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>IP 白名单对 HTTP API 和 SMPP 连接分别生效。未配置白名单的客户默认不限制来源 IP（建议生产环境强制配置）。</template>
      </el-alert>

      <!-- Platform-level whitelist -->
      <el-card shadow="never" style="margin-bottom: 20px;">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 600;">平台级白名单（运营团队访问）</span>
            <el-button type="primary" size="small" @click="ElMessage.info('添加平台 IP 白名单')">
              <el-icon><Plus /></el-icon>添加
            </el-button>
          </div>
        </template>
        <el-table :data="ipPlatformList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
          <el-table-column prop="ip" label="IP / CIDR" min-width="180" />
          <el-table-column prop="protocol" label="协议" width="120">
            <template #default="{ row }">
              <el-tag :type="row.protocol === 'HTTP API' ? '' : 'warning'" size="small">{{ row.protocol }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="160" />
          <el-table-column prop="addedBy" label="添加人" width="100" />
          <el-table-column prop="addedAt" label="添加时间" width="140" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default>
              <el-popconfirm title="确认删除?" @confirm="ElMessage.success('删除成功')">
                <template #reference><el-button link type="danger" size="small">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Customer-level whitelist -->
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 600;">客户级 IP 白名单</span>
            <el-button type="primary" size="small" @click="ElMessage.info('为客户添加 IP 白名单')">
              <el-icon><Plus /></el-icon>为客户添加 IP
            </el-button>
          </div>
        </template>
        <!-- Filter bar -->
        <div class="filter-bar">
          <el-select v-model="ipCustQuery.customer" placeholder="客户" clearable filterable size="default" style="width: 160px;">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
          <el-select v-model="ipCustQuery.protocol" placeholder="协议" clearable size="default" style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="HTTP API" value="HTTP API" />
            <el-option label="SMPP" value="SMPP" />
          </el-select>
          <el-input v-model="ipCustQuery.ip" placeholder="IP 地址" clearable size="default" style="width: 180px;" />
          <el-button type="primary" size="default" @click="ElMessage.info('查询')">查询</el-button>
        </div>
        <el-table :data="ipCustList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
          <el-table-column prop="customer" label="客户" min-width="140" />
          <el-table-column prop="ip" label="IP / CIDR" min-width="180" />
          <el-table-column prop="protocol" label="协议" width="120">
            <template #default="{ row }">
              <el-tag :type="row.protocol === 'HTTP API' ? '' : 'warning'" size="small">{{ row.protocol }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="160" show-overflow-tooltip />
          <el-table-column prop="addedAt" label="添加时间" width="140" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default>
              <el-popconfirm title="确认删除?" @confirm="ElMessage.success('删除成功')">
                <template #reference><el-button link type="danger" size="small">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination :total="18" layout="total, prev, pager, next" :page-size="10" />
        </div>
      </el-card>
    </div>

    <!-- ==================== Section 5: 拦截日志 ==================== -->
    <div v-else-if="activeTab === 'interceptLog'">
      <!-- Stats row -->
      <el-row :gutter="16" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">今日拦截</div>
            <div class="stat-value" style="color: #e74c3c;">1,402</div>
            <div class="stat-extra" style="color: #e74c3c;">较昨日 +18%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">今日转人工</div>
            <div class="stat-value" style="color: #f39c12;">67</div>
            <div class="stat-extra" style="color: #f39c12;">待审核 23 条</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">本月拦截</div>
            <div class="stat-value" style="color: #1f2937;">38,210</div>
            <div class="stat-extra">节省成本约 ¥4,800</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">拦截命中率</div>
            <div class="stat-value" style="color: #10b981;">2.3%</div>
            <div class="stat-extra" style="color: #10b981;">低于行业均值 5%</div>
          </div>
        </el-col>
      </el-row>

      <!-- Filter bar -->
      <div class="filter-bar">
        <el-select v-model="logQuery.timeRange" placeholder="时间范围" size="default" style="width: 140px;">
          <el-option label="今天" value="today" />
          <el-option label="近 7 天" value="7d" />
          <el-option label="近 30 天" value="30d" />
          <el-option label="自定义" value="custom" />
        </el-select>
        <el-select v-model="logQuery.type" placeholder="拦截类型" clearable size="default" style="width: 160px;">
          <el-option label="全部" value="" />
          <el-option label="黑名单" value="黑名单" />
          <el-option label="敏感词" value="敏感词" />
          <el-option label="频次超限" value="频次超限" />
          <el-option label="IP不在白名单" value="IP白名单" />
        </el-select>
        <el-select v-model="logQuery.action" placeholder="处理动作" clearable size="default" style="width: 140px;">
          <el-option label="全部" value="" />
          <el-option label="拦截" value="拦截" />
          <el-option label="转人工" value="转人工" />
          <el-option label="告警" value="告警" />
        </el-select>
        <el-select v-model="logQuery.customer" placeholder="客户" clearable filterable size="default" style="width: 160px;">
          <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.name" />
        </el-select>
        <el-input v-model="logQuery.keyword" placeholder="号码 / 内容" clearable size="default" style="width: 180px;" />
        <el-button type="primary" size="default" @click="ElMessage.info('查询')">查询</el-button>
        <el-button size="default" @click="ElMessage.info('导出功能开发中')">导出</el-button>
      </div>

      <!-- Table -->
      <el-table :data="logList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column prop="customer" label="客户" width="120" />
        <el-table-column prop="phone" label="目标号码" width="150" />
        <el-table-column prop="sid" label="SID" width="100" />
        <el-table-column prop="type" label="拦截类型" width="130">
          <template #default="{ row }">
            <span class="intercept-chip" :class="logTypeClass(row.type)">
              <span class="intercept-dot"></span>{{ row.type }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="rule" label="触发规则" min-width="200" show-overflow-tooltip />
        <el-table-column prop="action" label="动作" width="100">
          <template #default="{ row }">
            <el-tag :type="logActionType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容摘要" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="ElMessage.info('查看详情')">详情</el-button>
            <el-button v-if="row.action === '转人工'" link type="success" size="small" @click="ElMessage.info('放行操作')">放行</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <span style="color: #6b7280; font-size: 13px; margin-right: 16px;">共 1,402 条（今日），23 条待人工审核</span>
        <el-pagination :total="1402" layout="prev, pager, next" :page-size="20" />
      </div>
    </div>

    <!-- ==================== Blacklist Dialog ==================== -->
    <el-dialog v-model="blDialog" :title="editingBlId ? '编辑黑名单' : '新增黑名单'" width="500px" destroy-on-close>
      <el-form :model="blForm" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="blForm.type" style="width: 100%;">
            <el-option label="号码 (NUMBER)" value="NUMBER" />
            <el-option label="前缀 (PREFIX)" value="PREFIX" />
            <el-option label="发件人ID (SID)" value="SID" />
          </el-select>
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="blForm.value" :placeholder="blForm.type === 'PREFIX' ? '如 +861' : blForm.type === 'SID' ? '发件人ID' : '手机号码'" />
        </el-form-item>
        <el-form-item label="归属范围">
          <el-select v-model="blForm.scope" style="width: 100%;" @change="v => { if (v === 'GLOBAL') blForm.customerId = null }">
            <el-option label="全局" value="GLOBAL" />
            <el-option label="客户级" value="CUSTOMER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="blForm.scope === 'CUSTOMER'" label="客户">
          <el-select v-model="blForm.customerId" placeholder="选择客户" filterable style="width: 100%;">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="blForm.countryCode" placeholder="选择国家" clearable filterable style="width: 100%;">
            <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} (${c.code})`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="blForm.reason" type="textarea" :rows="2" placeholder="添加原因（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBl" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== Sensitive Word Dialog ==================== -->
    <el-dialog v-model="swDialog" :title="editingSwId ? '编辑敏感词' : '新增敏感词'" width="500px" destroy-on-close>
      <el-form :model="swForm" label-width="100px">
        <el-form-item label="敏感词">
          <el-input v-model="swForm.word" placeholder="输入敏感词" />
        </el-form-item>
        <el-form-item label="匹配类型">
          <el-select v-model="swForm.matchType" style="width: 100%;">
            <el-option label="包含 (CONTAINS)" value="CONTAINS" />
            <el-option label="精确 (EXACT)" value="EXACT" />
            <el-option label="正则 (REGEX)" value="REGEX" />
          </el-select>
        </el-form-item>
        <el-form-item label="命中动作">
          <el-select v-model="swForm.action" style="width: 100%;">
            <el-option label="拦截 (BLOCK)" value="BLOCK" />
            <el-option label="警告 (WARN)" value="WARN" />
            <el-option label="记录 (LOG)" value="LOG" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属范围">
          <el-select v-model="swForm.scope" style="width: 100%;" @change="v => { if (v === 'GLOBAL') swForm.customerId = null }">
            <el-option label="全局" value="GLOBAL" />
            <el-option label="客户级" value="CUSTOMER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="swForm.scope === 'CUSTOMER'" label="客户">
          <el-select v-model="swForm.customerId" placeholder="选择客户" filterable style="width: 100%;">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用国家">
          <el-select v-model="swForm.countryCode" placeholder="全部国家" clearable filterable style="width: 100%;">
            <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} (${c.code})`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="swForm.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="swDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSw" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCountryLabel } from '../utils/country'
import {
  blacklistList, blacklistSave, blacklistUpdate, blacklistDel,
  sensitiveWordList, sensitiveWordSave, sensitiveWordUpdate, sensitiveWordDel,
  countryAll, customerList
} from '../api'

/* ---- constants ---- */
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

const matchTypeMap = { CONTAINS: '包含', EXACT: '精确', REGEX: '正则' }
const actionMap = { BLOCK: '拦截', WARN: '警告', LOG: '记录' }
const actionTagType = action => ({ BLOCK: 'danger', WARN: 'warning', LOG: 'info' }[action] || 'info')

/* ---- shared state ---- */
const activeTab = ref('blacklist')
const saving = ref(false)
const countryOptions = ref([])
const customerOptions = ref([])
const customerNameMap = computed(() => {
  const m = {}
  customerOptions.value.forEach(c => { m[c.id] = c.name })
  return m
})

/* ---- blacklist state ---- */
const blSubTab = ref('NUMBER')
const blList = ref([])
const blTotal = ref(0)
const blLoading = ref(false)
const blQuery = reactive({ page: 1, size: 10, scope: '', countryCode: '', keyword: '' })
const blDialog = ref(false)
const blForm = ref({})
const editingBlId = ref(null)
const blSelection = ref([])
const blStats = reactive({ number: 128, prefix: 35, optout: 892, sid: 12 })

/* ---- sensitive word state ---- */
const swList = ref([])
const swTotal = ref(0)
const swLoading = ref(false)
const swQuery = reactive({ page: 1, size: 10, matchType: '', action: '', scope: '', keyword: '' })
const swDialog = ref(false)
const swForm = ref({})
const editingSwId = ref(null)

/* ---- rate limit state ---- */
const rlSelectedIdx = ref(0)
const rlRules = ref([
  { name: '单号码限频', dimension: '单号码', level: '全局', bindCustomer: '', bindLabel: '', timeWindow: '1hour', maxCount: 60, hitAction: '拦截', countMode: '所有短信含分段', enabled: true, limitDesc: '60条/小时', remark: '防止对单个号码发送过多短信' },
  { name: '客户整体 TPS', dimension: '客户整体', level: '客户级', bindCustomer: 'BKK Express', bindLabel: 'BKK Express', timeWindow: '1min', maxCount: 100, hitAction: '延迟排队', countMode: '每条消息不拆段', enabled: true, limitDesc: '100条/分钟', remark: '' },
  { name: '客户×国家 日限', dimension: '客户×国家', level: '客户级', bindCustomer: 'EasyPay TH', bindLabel: 'EasyPay TH · TH', timeWindow: '1day', maxCount: 10000, hitAction: '拦截', countMode: '所有短信含分段', enabled: true, limitDesc: '10000条/天', remark: '' },
  { name: '通道 TPS 限制', dimension: '通道', level: '通道级', bindCustomer: '', bindLabel: 'TH-DTAC-01', timeWindow: '1min', maxCount: 50, hitAction: '延迟排队', countMode: '每条消息不拆段', enabled: false, limitDesc: '50条/分钟', remark: '' },
  { name: '单号码日限（全局）', dimension: '单号码', level: '全局', bindCustomer: '', bindLabel: '', timeWindow: '1day', maxCount: 5, hitAction: '拦截', countMode: '所有短信含分段', enabled: true, limitDesc: '5条/天', remark: '全局单号码每天最多5条' }
])
const rlSelected = computed(() => rlRules.value[rlSelectedIdx.value] || null)
const addRlRule = () => {
  rlRules.value.push({ name: '新规则', dimension: '单号码', level: '全局', bindCustomer: '', bindLabel: '', timeWindow: '1hour', maxCount: 10, hitAction: '拦截', countMode: '所有短信含分段', enabled: true, limitDesc: '10条/小时', remark: '' })
  rlSelectedIdx.value = rlRules.value.length - 1
}

/* ---- IP whitelist state ---- */
const ipPlatformList = ref([
  { ip: '203.0.113.10', protocol: 'HTTP API', remark: '运营VPN出口', addedBy: 'admin', addedAt: '2025-09-01' },
  { ip: '198.51.100.0/24', protocol: 'SMPP', remark: '测试环境网段', addedBy: 'admin', addedAt: '2025-10-15' }
])
const ipCustList = ref([
  { customer: 'BKK Express', ip: '185.199.108.0/24', protocol: 'HTTP API', remark: '生产服务器网段', addedAt: '2025-11-03' },
  { customer: 'BKK Express', ip: '192.0.2.55', protocol: 'SMPP', remark: 'SMPP 专线', addedAt: '2025-11-03' },
  { customer: 'EasyPay TH', ip: '203.0.113.88', protocol: 'HTTP API', remark: '泰国数据中心', addedAt: '2026-01-10' },
  { customer: 'ID MallApp', ip: '10.0.0.0/8', protocol: 'HTTP API', remark: '内网测试（临时）', addedAt: '2026-02-28' }
])
const ipCustQuery = reactive({ customer: '', protocol: '', ip: '' })

/* ---- intercept log state ---- */
const logQuery = reactive({ timeRange: 'today', type: '', action: '', customer: '', keyword: '' })
const logList = ref([
  { time: '2026-03-13 10:42:18', customer: 'BKK Express', phone: '+6681234xxxx', sid: 'BKKAlert', type: '黑名单', rule: '全局号码黑名单', action: '拦截', content: '您的账户已完成验证，点击...' },
  { time: '2026-03-13 10:38:05', customer: 'EasyPay TH', phone: '+6689876xxxx', sid: 'EasyOTP', type: '敏感词', rule: '包含"casino"(全局规则)', action: '转人工', content: 'Casino VIP membership offer...' },
  { time: '2026-03-13 10:31:44', customer: 'BKK Express', phone: '+6681111xxxx', sid: 'BKKAlert', type: '频次超限', rule: '单号码60条/小时→已发61条', action: '拦截', content: '您的快递已到达...' },
  { time: '2026-03-13 10:15:22', customer: 'ID MallApp', phone: '+6281987xxxx', sid: 'IDMall', type: '敏感词', rule: '包含"立即点击"(全局规则)', action: '告警', content: '限时优惠，立即点击领取...' },
  { time: '2026-03-13 09:58:10', customer: 'BKK Express', phone: '+6688888xxxx', sid: '—', type: 'IP白名单', rule: '来源IP 203.1.2.3不在白名单', action: '拦截', content: '—' }
])
const logTypeClass = type => ({ '黑名单': 'chip-danger', '敏感词': 'chip-warning', '频次超限': 'chip-danger', 'IP白名单': 'chip-danger' }[type] || 'chip-danger')
const logActionType = action => ({ '拦截': 'danger', '转人工': 'warning', '告警': '' }[action] || 'info')

/* ---- load countries ---- */
const loadCountries = async () => {
  try {
    const r = await countryAll()
    countryOptions.value = r.data || []
  } catch { /* ignore */ }
}

/* ---- load customers ---- */
const loadCustomers = async () => {
  try {
    const r = await customerList({ page: 1, size: 9999 })
    customerOptions.value = (r.data?.list || r.data || []).map(c => ({ id: c.id, name: c.companyName || c.customerName }))
  } catch { /* ignore */ }
}

/* ---- blacklist methods ---- */
const loadBl = async () => {
  blLoading.value = true
  try {
    const params = { ...blQuery, type: blSubTab.value }
    // Clean empty strings
    Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })
    const r = await blacklistList(params)
    blList.value = r.data?.list || r.data || []
    blTotal.value = r.data?.total || 0
    // Update stats from response if available
    updateBlStats()
  } finally {
    blLoading.value = false
  }
}

const updateBlStats = () => {
  // Stats are mock counts; in production these would come from a dedicated API
  const list = blList.value
  if (blSubTab.value === 'NUMBER') blStats.number = blTotal.value || list.length
  else if (blSubTab.value === 'PREFIX') blStats.prefix = blTotal.value || list.length
  else if (blSubTab.value === 'OPTOUT') blStats.optout = blTotal.value || list.length
  else if (blSubTab.value === 'SID') blStats.sid = blTotal.value || list.length
}

const openBl = () => {
  editingBlId.value = null
  blForm.value = { type: blSubTab.value, scope: 'GLOBAL', customerId: null, value: '', countryCode: '', reason: '', isActive: true }
  blDialog.value = true
}

const editBl = row => {
  editingBlId.value = row.id
  blForm.value = { type: row.type, scope: row.scope || 'GLOBAL', customerId: row.customerId, value: row.value, countryCode: row.countryCode || '', reason: row.reason || '', isActive: row.isActive }
  blDialog.value = true
}

const saveBl = async () => {
  if (!blForm.value.value) { ElMessage.warning('请输入值'); return }
  if (blForm.value.scope === 'CUSTOMER' && !blForm.value.customerId) { ElMessage.warning('请选择客户'); return }
  saving.value = true
  try {
    if (editingBlId.value) {
      await blacklistUpdate(editingBlId.value, blForm.value)
    } else {
      await blacklistSave(blForm.value)
    }
    ElMessage.success('保存成功')
    blDialog.value = false
    loadBl()
  } finally { saving.value = false }
}

const delBl = async id => {
  await blacklistDel(id)
  ElMessage.success('删除成功')
  loadBl()
}

/* ---- sensitive word methods ---- */
const loadSw = async () => {
  swLoading.value = true
  try {
    const params = { ...swQuery }
    Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })
    const r = await sensitiveWordList(params)
    swList.value = r.data?.list || r.data || []
    swTotal.value = r.data?.total || 0
  } finally {
    swLoading.value = false
  }
}

const openSw = () => {
  editingSwId.value = null
  swForm.value = { word: '', matchType: 'CONTAINS', action: 'BLOCK', scope: 'GLOBAL', customerId: null, countryCode: '', remark: '', isActive: true }
  swDialog.value = true
}

const editSw = row => {
  editingSwId.value = row.id
  swForm.value = { word: row.word, matchType: row.matchType, action: row.action, scope: row.scope || 'GLOBAL', customerId: row.customerId, countryCode: row.countryCode || '', remark: row.remark || '', isActive: row.isActive }
  swDialog.value = true
}

const saveSw = async () => {
  if (!swForm.value.word) { ElMessage.warning('请输入敏感词'); return }
  if (swForm.value.scope === 'CUSTOMER' && !swForm.value.customerId) { ElMessage.warning('请选择客户'); return }
  saving.value = true
  try {
    if (editingSwId.value) {
      await sensitiveWordUpdate(editingSwId.value, swForm.value)
    } else {
      await sensitiveWordSave(swForm.value)
    }
    ElMessage.success('保存成功')
    swDialog.value = false
    loadSw()
  } finally { saving.value = false }
}

const delSw = async id => {
  await sensitiveWordDel(id)
  ElMessage.success('删除成功')
  loadSw()
}

/* ---- tab change ---- */
const onTabChange = tab => {
  if (tab === 'blacklist') loadBl()
  else if (tab === 'sensitive') loadSw()
}

/* ---- init ---- */
onMounted(() => {
  loadCountries()
  loadCustomers()
  loadBl()
})
</script>

<style scoped>
.risk-page {
  padding: 0;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

/* Sub-nav pill style */
.sub-nav {
  margin-bottom: 24px;
}
.sub-nav :deep(.el-radio-group) {
  display: flex;
  gap: 0;
}
.sub-nav :deep(.el-radio-button__inner) {
  border-radius: 20px;
  padding: 8px 20px;
  font-size: 13px;
  border: 1px solid #e5e7eb;
  margin-right: 8px;
}
.sub-nav :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-left: 1px solid #e5e7eb;
  border-radius: 20px;
}
.sub-nav :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 20px;
}
.sub-nav :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #409eff;
  border-color: #409eff;
  color: #fff;
  box-shadow: none;
}

/* Stat cards */
.stat-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px 20px;
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
}

/* Sub-tab bar */
.sub-tab-bar {
  margin-bottom: 16px;
}
.sub-tab-bar :deep(.el-radio-button__inner) {
  border-radius: 4px;
  padding: 6px 16px;
  font-size: 12px;
}

/* Filter bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

/* Pagination */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Stat extra line */
.stat-extra {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

/* Rule grid (rate limit) */
.rule-grid {
  display: flex;
  gap: 20px;
}
.rule-list-panel {
  width: 280px;
  flex-shrink: 0;
}
.rule-detail-panel {
  flex: 1;
  min-width: 0;
}
.rule-list-card :deep(.el-card__body) {
  padding: 0;
}
.rule-items {
  max-height: 520px;
  overflow-y: auto;
}
.rule-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.15s;
}
.rule-item:hover {
  background: #f9fafb;
}
.rule-item.active {
  background: #eff6ff;
  border-left: 3px solid #409eff;
}
.rule-item-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 6px;
}
.rule-item-meta {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.rule-item-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Intercept log chips */
.intercept-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}
.intercept-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.chip-danger {
  background: #fef2f2;
  color: #dc2626;
}
.chip-danger .intercept-dot {
  background: #dc2626;
}
.chip-warning {
  background: #fffbeb;
  color: #d97706;
}
.chip-warning .intercept-dot {
  background: #d97706;
}

/* Empty section */
.empty-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style>
