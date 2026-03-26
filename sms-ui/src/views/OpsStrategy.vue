<template>
  <div class="ops-strategy-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-title">运营策略</div>
    </div>

    <!-- Sub-nav Tabs -->
    <el-radio-group v-model="activeTab" class="tab-radio-group" size="default">
      <el-radio-button value="review">人工审核</el-radio-button>
      <el-radio-button value="routing">路由策略</el-radio-button>
      <el-radio-button value="dispatch">通道调度</el-radio-button>
      <el-radio-button value="tpl-replace">模板替换</el-radio-button>
      <el-radio-button value="sid-replace">SID 替换</el-radio-button>
    </el-radio-group>

    <!-- ========== Tab1: 人工审核 ========== -->
    <div v-if="activeTab === 'review'">
      <!-- 全局开关 -->
      <div class="global-switch-bar">
        <span class="switch-label">人工审核全局开关</span>
        <el-switch v-model="reviewGlobalSwitch" />
        <span class="switch-desc">开启后，符合触发规则的发送请求将进入人工审核队列</span>
      </div>

      <!-- 触发规则 -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <span class="card-title-text">触发规则</span>
        </template>
        <el-table :data="reviewRules" :header-cell-style="headerCellStyle" style="font-size: 13px;" stripe>
          <el-table-column prop="name" label="规则名" min-width="130">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="描述" min-width="280" />
          <el-table-column label="开关" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEditDialog('触发规则', row.name)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 审核工单 -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span class="card-title-text">审核工单</span>
            <el-select v-model="reviewStatusFilter" placeholder="全部状态" style="width: 120px;" size="small" clearable>
              <el-option label="全部状态" value="" />
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
          </div>
        </template>
        <el-table :data="filteredReviewOrders" :header-cell-style="headerCellStyle" style="font-size: 13px;" stripe>
          <el-table-column prop="time" label="时间" width="160" />
          <el-table-column prop="customer" label="客户" width="140" />
          <el-table-column prop="preview" label="内容预览" min-width="220" show-overflow-tooltip />
          <el-table-column label="触发规则" width="120">
            <template #default="{ row }">
              <el-tag :type="ruleTagType(row.rule)" size="small">{{ row.rule }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewer" label="审核人" width="80" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button link type="success" size="small" @click="handleTicketActionById(row._id, 'approve', row.customer)">通过</el-button>
                <el-button link type="danger" size="small" @click="handleTicketActionById(row._id, 'reject', row.customer)">拒绝</el-button>
              </template>
              <template v-else>
                <el-button link type="primary" size="small" @click="openDetailDialog('审核工单', row.customer)">详情</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="reviewPage"
            :page-size="10"
            :total="reviewTicketTotal"
            layout="total, prev, pager, next"
            small
          />
        </div>
      </el-card>
    </div>

    <!-- ========== Tab2: 路由策略 ========== -->
    <div v-if="activeTab === 'routing'">
      <!-- 路由机制说明 -->
      <el-alert type="success" :closable="false" style="margin-bottom: 16px;">
        <template #title>
          <strong>路由机制说明</strong>
        </template>
        <template #default>
          <div style="line-height: 1.8; font-size: 13px;">
            <strong>第1步 — 手动覆盖（本页配置）：</strong>检查是否有针对该客户/国家的覆盖规则，如有则走指定的通道或通道组。<br/>
            <strong>第2步 — 自动匹配（无需配置）：</strong>如果没有手动覆盖，系统自动按目标国家匹配活跃通道，按优先级选择最优通道。<br/>
            <br/>
            <strong>基础用法：</strong>只需在「通道管理」中维护好通道的国家、优先级和启用状态，路由自动生效，无需在此配置。<br/>
            <strong>高级用法：</strong>如需为特定客户指定通道，或指向「通道调度」中的通道组实现容灾/负载均衡，在下方添加覆盖规则。
          </div>
        </template>
      </el-alert>

      <!-- 手动覆盖规则 -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span class="card-title-text">手动覆盖规则</span>
            <el-button type="primary" size="small" @click="openAssignDialog">+ 新增覆盖</el-button>
          </div>
        </template>
        <div style="font-size: 13px; color: #6b7280; margin-bottom: 12px;">
          指定某客户/某国家走特定通道或通道组，优先级高于默认路由。不配置则走默认自动匹配。
        </div>
        <el-table :data="strategyAssignments" :header-cell-style="headerCellStyle" style="font-size: 13px;" stripe empty-text="暂无覆盖规则，所有客户走默认自动路由">
          <el-table-column label="客户" min-width="130">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault" type="info" size="small">全部客户</el-tag>
              <span v-else>{{ row.customer }}</span>
            </template>
          </el-table-column>
          <el-table-column label="国家" width="100">
            <template #default="{ row }">
              <span>{{ row.country || '全部' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="smsType" label="短信类型" width="100" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag :type="row.targetType === '通道组' ? '' : 'success'" size="small">{{ row.targetType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="指定目标" min-width="150">
            <template #default="{ row }">
              <span>{{ row.targetName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="handleStrategyDeleteById(row._id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- ========== Tab3: 通道调度 ========== -->
    <div v-if="activeTab === 'dispatch'">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
        <template #title>
          <strong>通道组说明</strong>
        </template>
        <template #default>
          <div style="line-height: 1.8; font-size: 13px;">
            通道组是将多条通道打包，实现容灾切换或负载均衡的<strong>可选</strong>高级功能。<br/>
            <strong>调度模式：</strong>Failover = 按优先级逐条尝试，主通道故障自动切换备用；权重 = 按百分比分配流量；优先级+权重 = 同优先级内按权重分配。<br/>
            <strong>使用场景：</strong>一个国家有多条通道时，建通道组统一管理。然后在「路由策略」中将客户/国家指向该通道组。<br/>
            <strong>注意：</strong>通道组内的通道必须是<strong>活跃状态</strong>（开关开启 + 连接正常）才会被调度。如果只有一条通道或不需要容灾，可以不建通道组。
          </div>
        </template>
      </el-alert>
      <el-card shadow="never" class="table-card">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span class="card-title-text">通道组列表</span>
            <el-button type="primary" size="small" @click="groupForm = { id: null, name: '', mode: '' }; showGroupDialog = true">+ 新增通道组</el-button>
          </div>
        </template>
        <!-- 搜索栏 -->
        <div style="margin-bottom: 12px; display: flex; gap: 8px;">
          <el-input v-model="groupSearchKey" placeholder="搜索通道组名称" clearable style="width: 220px;" />
        </div>
        <el-table
          :data="filteredChannelGroups"
          :header-cell-style="headerCellStyle"
          style="font-size: 13px;"
          stripe
        >
          <el-table-column label="通道组名" min-width="150">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="调度模式" width="140">
            <template #default="{ row }">
              <el-tag :type="dispatchModeTag(row.mode)" size="small">{{ row.modeLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="成员" width="200">
            <template #default="{ row }">
              <span>{{ row.memberCount }} 个通道</span>
              <span v-if="row.totalWeight !== undefined" :style="{ color: row.totalWeight === 100 ? '#16a34a' : '#dc2626', marginLeft: '8px', fontSize: '12px' }">
                (权重合计 {{ row.totalWeight }}%)
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '已禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openMemberMgmt(row)">管理成员</el-button>
              <el-button link type="primary" size="small" @click="openGroupEdit(row)">编辑</el-button>
              <el-button v-if="row.enabled" link type="danger" size="small" @click="confirmAction('禁用', row.name)">禁用</el-button>
              <el-button v-else link type="success" size="small" @click="confirmAction('启用', row.name)">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- ========== Tab4: 模板替换 ========== -->
    <div v-if="activeTab === 'tpl-replace'">
      <!-- 全局开关 -->
      <div class="global-switch-bar">
        <span class="switch-label">模板替换全局开关</span>
        <el-switch v-model="tplReplaceGlobalSwitch" />
        <span class="switch-desc">开启后，符合替换规则的短信内容将在发送前自动替换</span>
      </div>

      <!-- 筛选 + 新增 -->
      <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px;">
        <div class="filter-bar">
          <div class="filter-item">
            <span class="filter-label">触发条件</span>
            <el-select v-model="tplCondFilter" placeholder="全部" style="width: 140px;" size="small" clearable>
              <el-option label="全部" value="" />
              <el-option label="国家" value="国家" />
              <el-option label="通道" value="通道" />
              <el-option label="客户" value="客户" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">状态</span>
            <el-select v-model="tplStatusFilter" placeholder="全部" style="width: 120px;" size="small" clearable>
              <el-option label="全部" value="" />
              <el-option label="启用" value="启用" />
              <el-option label="禁用" value="禁用" />
            </el-select>
          </div>
          <el-button type="primary" size="small">查询</el-button>
        </div>
        <el-button type="primary" @click="showToast('新增替换规则弹窗')">+ 新增替换规则</el-button>
      </div>

      <el-card shadow="never" class="table-card">
        <el-table :data="tplReplaceRules" :header-cell-style="headerCellStyle" style="font-size: 13px;" stripe>
          <el-table-column prop="id" label="规则ID" width="90">
            <template #default="{ row }">
              <span style="font-family: monospace; color: #999;">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column label="触发条件" width="140">
            <template #default="{ row }">
              <el-tag :type="tplCondTagType(row.condType)" size="small">{{ row.condition }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchType" label="匹配方式" width="90" />
          <el-table-column label="查找内容" min-width="180">
            <template #default="{ row }">
              <code class="code-red">{{ row.findContent }}</code>
            </template>
          </el-table-column>
          <el-table-column label="替换为" width="120">
            <template #default="{ row }">
              <code class="code-green">{{ row.replaceWith }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="scope" label="作用范围" width="90" />
          <el-table-column prop="triggerCount" label="触发次数" width="90" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openTplEdit(row)">编辑</el-button>
              <el-button v-if="row.status === '启用'" size="small" type="danger" @click="confirmAction('禁用', row.id)">禁用</el-button>
              <el-button v-else size="small" type="success" @click="confirmAction('启用', row.id)">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:12px;">
          <el-button type="primary" size="small" @click="openTplEdit(null)">+ 新增规则</el-button>
        </div>
      </el-card>

      <el-alert type="info" :closable="false" style="margin-top: 16px;">
        <template #default>
          <span><strong>替换链路位置：</strong>风控检查 → 人工审核 → <strong style="color: #1890ff;">模板替换</strong> → SID 替换 → 编码分段 → 路由 → 发送</span>
        </template>
      </el-alert>
    </div>

    <!-- ========== Tab5: SID 替换 ========== -->
    <div v-if="activeTab === 'sid-replace'">
      <!-- 全局开关 -->
      <div class="global-switch-bar">
        <span class="switch-label">SID 自动替换全局开关</span>
        <el-switch v-model="sidReplaceGlobalSwitch" />
        <span class="switch-desc">开启后，客户指定的 SID 在目标通道不可用时，自动替换为可用 SID</span>
      </div>

      <!-- 替换逻辑优先级 -->
      <div class="priority-flow">
        <h4 class="section-title">替换逻辑优先级</h4>
        <div class="priority-steps">
          <div class="priority-step step-blue">
            <div class="step-num">&#9312;</div>
            <div class="step-title">使用原 SID</div>
            <div class="step-desc">目标通道已注册</div>
          </div>
          <span class="step-arrow">&rarr;</span>
          <div class="priority-step step-yellow">
            <div class="step-num">&#9313;</div>
            <div class="step-title">客户其他已注册 SID</div>
            <div class="step-desc">同通道同国家</div>
          </div>
          <span class="step-arrow">&rarr;</span>
          <div class="priority-step step-red">
            <div class="step-num">&#9314;</div>
            <div class="step-title">通道默认 SID</div>
            <div class="step-desc">共享 SID</div>
          </div>
          <span class="step-arrow">&rarr;</span>
          <div class="priority-step step-gray">
            <div class="step-num">&#9315;</div>
            <div class="step-title">路由到其他通道</div>
            <div class="step-desc">或返回错误</div>
          </div>
        </div>
      </div>

      <!-- SID 替换规则 -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h4 class="section-title" style="margin-bottom: 0;">SID 替换规则</h4>
        <el-button type="primary" @click="openSidEdit(null)">+ 新增规则</el-button>
      </div>

      <el-card shadow="never" class="table-card">
        <el-table :data="sidReplaceRules" :header-cell-style="headerCellStyle" style="font-size: 13px;" stripe>
          <el-table-column prop="id" label="规则ID" width="90">
            <template #default="{ row }">
              <span style="font-family: monospace; color: #999;">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="customer" label="客户" width="100" />
          <el-table-column label="原始 SID" width="120">
            <template #default="{ row }">
              <code class="code-blue">{{ row.originalSid }}</code>
            </template>
          </el-table-column>
          <el-table-column label="目标通道" width="130">
            <template #default="{ row }">{{ row.targetChannelName || '-' }}</template>
          </el-table-column>
          <el-table-column label="替换 SID" width="120">
            <template #default="{ row }">
              <code class="code-green">{{ row.replaceSid }}</code>
            </template>
          </el-table-column>
          <el-table-column label="替换原因" width="130">
            <template #default="{ row }">
              <el-tag :type="row.reasonType === 'warning' ? 'warning' : ''" size="small">{{ row.reason }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="triggerCount" label="触发次数" width="90" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openSidEdit(row)">编辑</el-button>
              <el-button v-if="row.status === '启用'" size="small" type="danger" @click="confirmAction('禁用', row.id)">禁用</el-button>
              <el-button v-else size="small" type="success" @click="confirmAction('启用', row.id)">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 最近 SID 替换日志 -->
      <h4 class="section-title" style="margin-top: 24px;">最近 SID 替换日志</h4>
      <el-card shadow="never" class="table-card">
        <el-table :data="sidReplaceLogs" :header-cell-style="headerCellStyle" style="font-size: 13px;" stripe>
          <el-table-column prop="time" label="时间" width="170">
            <template #default="{ row }">
              <span style="font-size: 12px; color: #999; white-space: nowrap;">{{ row.time }}</span>
            </template>
          </el-table-column>
          <el-table-column label="消息ID" width="180">
            <template #default="{ row }">
              <span style="font-family: monospace; font-size: 11px;">{{ row.msgId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="customer" label="客户" width="100" />
          <el-table-column label="原始 SID" width="110">
            <template #default="{ row }">
              <code style="text-decoration: line-through; color: #ff4d4f;">{{ row.originalSid }}</code>
            </template>
          </el-table-column>
          <el-table-column label="实际 SID" width="110">
            <template #default="{ row }">
              <code style="color: #52c41a;">{{ row.actualSid }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="channel" label="通道" width="120" />
          <el-table-column prop="country" label="国家" width="60" />
          <el-table-column prop="reason" label="替换原因" min-width="180" />
        </el-table>
      </el-card>

      <el-alert type="info" :closable="false" style="margin-top: 16px;">
        <template #default>
          <div>
            <strong>替换链路位置：</strong>风控检查 → 人工审核 → 模板替换 → <strong style="color: #1890ff;">SID 替换</strong> → 编码分段 → 路由 → 发送
            <br />
            <span style="color: #999; font-size: 12px;">替换记录保留在 message 表中：original_sid + actual_sid，可在发送记录中查看</span>
          </div>
        </template>
      </el-alert>
    </div>

    <!-- ========== 新增覆盖规则弹窗 ========== -->
    <el-dialog v-model="showAssignDialog" title="新增覆盖规则" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="客户">
          <el-select v-model="assignForm.customerId" placeholder="全部客户（不限）" style="width: 100%;" clearable filterable>
            <el-option v-for="c in allCustomers" :key="c.id" :label="c.companyName || c.customerName || c.customerCode" :value="c.id" />
          </el-select>
          <div style="font-size: 12px; color: #999; margin-top: 2px;">不选 = 对所有客户生效</div>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="assignForm.countryCode" placeholder="全部国家（不限）" style="width: 100%;" clearable filterable>
            <el-option v-for="c in allCountries" :key="c.code" :label="`${c.code} - ${c.name}`" :value="c.code" />
          </el-select>
          <div style="font-size: 12px; color: #999; margin-top: 2px;">不选 = 对所有国家生效</div>
        </el-form-item>
        <el-form-item label="短信类型">
          <el-select v-model="assignForm.smsAttribute" placeholder="全部类型" style="width: 100%;" clearable>
            <el-option label="全部类型" :value="0" />
            <el-option label="OTP 验证码" :value="1" />
            <el-option label="交易通知" :value="2" />
            <el-option label="营销推广" :value="3" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left" style="margin: 12px 0;">指定走以下通道/通道组（可多选）</el-divider>
        <el-form-item label="通道组">
          <el-select v-model="assignForm.channelGroupIds" multiple placeholder="选择通道组" style="width: 100%;" filterable>
            <el-option v-for="g in channelGroups" :key="g._id" :label="g.name" :value="g._id" />
          </el-select>
        </el-form-item>
        <el-form-item label="通道">
          <el-select v-model="assignForm.channelIds" multiple placeholder="选择通道" style="width: 100%;" filterable>
            <el-option v-for="ch in allChannels" :key="ch.id" :label="`${ch.channelName} (${ch.channelCode})`" :value="ch.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="assignForm.channelGroupIds.length === 0 && assignForm.channelIds.length === 0" style="color: #e6a23c; font-size: 12px; padding: 0 0 8px 90px;">
        请至少选择一个通道或通道组
      </div>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAssignConfirm" :disabled="assignForm.channelGroupIds.length === 0 && assignForm.channelIds.length === 0">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- ========== 新增通道组弹窗 ========== -->
    <el-dialog v-model="showGroupDialog" :title="groupForm.id ? `编辑通道组 — ${groupForm.name}` : '新增通道组'" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="通道组名称">
          <el-input v-model="groupForm.name" placeholder="如: ID-OTP-Group" />
        </el-form-item>
        <el-form-item label="调度模式">
          <el-select v-model="groupForm.mode" placeholder="请选择调度模式" style="width: 100%;">
            <el-option label="故障转移 (failover)" value="failover" />
            <el-option label="权重轮询 (weight)" value="weight" />
            <el-option label="优先级加权 (priority_weight)" value="priority_weight" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="handleGroupConfirm">{{ groupForm.id ? '保存' : '确认创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- ========== 管理通道成员弹窗 ========== -->
    <el-dialog v-model="showMemberMgmt" :title="`管理成员 — ${memberMgmtGroup?.name || ''}`" width="700px" :close-on-click-modal="false">
      <!-- 添加通道区域 -->
      <div style="display: flex; gap: 8px; margin-bottom: 16px;">
        <el-select v-model="addChannelId" filterable placeholder="搜索并选择通道" style="flex: 1;">
          <el-option v-for="ch in availableChannels" :key="ch.id" :label="`${ch.channelName} (${ch.channelCode})`" :value="ch.id" />
        </el-select>
        <el-button type="primary" :disabled="!addChannelId" @click="handleQuickAdd">添加</el-button>
      </div>

      <!-- 成员列表 -->
      <el-table :data="memberMgmtList" :header-cell-style="headerCellStyle" style="font-size: 13px;" max-height="360" stripe>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="name" label="通道名称" min-width="160" />
        <el-table-column label="优先级" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.priority" :min="1" :max="99" size="small" controls-position="right" style="width: 90px;" @change="markMemberDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="权重 (%)" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :max="100" size="small" controls-position="right" style="width: 90px;" @change="markMemberDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span :class="['health-dot', 'health-' + row.healthColor]"></span>{{ row.healthText }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleMemberRemoveInMgmt(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 权重汇总 -->
      <div style="margin-top: 12px; display: flex; justify-content: space-between; align-items: center;">
        <div style="font-size: 13px;">
          共 <strong>{{ memberMgmtList.length }}</strong> 个通道，权重合计
          <span :style="{ fontWeight: 700, color: memberWeightTotal === 100 ? '#16a34a' : '#dc2626' }">{{ memberWeightTotal }}%</span>
          <span v-if="memberWeightTotal !== 100" style="color: #dc2626; margin-left: 4px; font-size: 12px;">（建议调整为 100%）</span>
        </div>
        <el-button size="small" @click="autoDistributeWeight" :disabled="memberMgmtList.length === 0">平均分配权重</el-button>
      </div>

      <template #footer>
        <el-button @click="showMemberMgmt = false">关闭</el-button>
        <el-button type="primary" @click="saveMemberChanges" :loading="memberSaving">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- (通道组编辑已合并到新建弹窗) -->

    <!-- ========== 模板替换规则编辑弹窗 ========== -->
    <el-dialog v-model="showTplEditDialog" :title="tplEditForm.id ? '编辑模板替换规则' : '新增模板替换规则'" width="560px" destroy-on-close>
      <el-form :model="tplEditForm" label-width="100px">
        <el-form-item label="触发条件">
          <el-select v-model="tplEditForm.triggerCondition" style="width:120px;">
            <el-option label="国家" value="国家" />
            <el-option label="通道" value="通道" />
            <el-option label="客户" value="客户" />
          </el-select>
          <el-input v-model="tplEditForm.triggerValue" placeholder="条件值" style="width:200px;margin-left:8px;" />
        </el-form-item>
        <el-form-item label="匹配方式">
          <el-select v-model="tplEditForm.matchType" style="width:100%;">
            <el-option label="精确匹配" value="EXACT" />
            <el-option label="正则匹配" value="REGEX" />
            <el-option label="包含匹配" value="CONTAINS" />
          </el-select>
        </el-form-item>
        <el-form-item label="查找内容">
          <el-input v-model="tplEditForm.findContent" placeholder="要查找替换的内容" />
        </el-form-item>
        <el-form-item label="替换为">
          <el-input v-model="tplEditForm.replaceWith" placeholder="替换后的内容" />
        </el-form-item>
        <el-form-item label="作用范围">
          <el-select v-model="tplEditForm.scope" style="width:100%;">
            <el-option label="全局" value="GLOBAL" />
            <el-option label="客户级" value="CUSTOMER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="tplEditForm.isActive" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTplEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTplEditForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== SID替换规则编辑弹窗 ========== -->
    <el-dialog v-model="showSidEditDialog" :title="sidEditForm.id ? '编辑SID替换规则' : '新增SID替换规则'" width="560px" destroy-on-close>
      <el-form ref="sidEditFormRef" :model="sidEditForm" :rules="sidEditRules" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="sidEditForm.customerId" filterable placeholder="选择客户" style="width:100%;">
            <el-option v-for="c in allCustomers" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原始SID" prop="originalSid">
          <el-input v-model="sidEditForm.originalSid" placeholder="客户原始指定的SID" />
        </el-form-item>
        <el-form-item label="目标通道" prop="targetChannelId">
          <el-select v-model="sidEditForm.targetChannelId" filterable placeholder="选择目标通道" style="width:100%;">
            <el-option v-for="ch in allChannels" :key="ch.id" :label="`${ch.channelName} (${ch.channelCode})`" :value="ch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="替换SID" prop="replacementSid">
          <el-input v-model="sidEditForm.replacementSid" placeholder="替换后使用的SID" />
        </el-form-item>
        <el-form-item label="替换原因">
          <el-input v-model="sidEditForm.replaceReason" placeholder="如：原SID未在该通道注册" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="sidEditForm.isActive" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSidEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSidEditForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 通用详情弹窗 ========== -->
    <el-dialog v-model="showDetailDialog" :title="detailDialogTitle" width="500px" destroy-on-close>
      <p style="color: #666; font-size: 14px; line-height: 1.8;">此处显示详细信息内容。</p>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  reviewRuleList, reviewRuleSave, reviewRuleToggle,
  reviewTicketPage, reviewTicketReview,
  routingStrategyList, routingStrategySave, routingStrategyDel,
  channelGroupList, channelGroupSave, channelGroupToggle,
  channelGroupMembers, channelGroupMemberAdd, channelGroupMemberUpdate, channelGroupMemberDel,
  tplReplaceList, tplReplaceSave, tplReplaceToggle,
  sidReplaceList, sidReplaceSave, sidReplaceToggle,
} from '../api'
import { useRefData } from '../stores/refData'

const refData = useRefData()

// ---- Common ----
const headerCellStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const activeTab = ref('review')

// ---- Reference data ----
const allCustomers = ref([])
const customerNameMap = ref({})
const allCountries = ref([])

async function loadRefData() {
  const [custList, countries] = await Promise.all([refData.loadCustomers(), refData.loadCountries()])
  allCustomers.value = custList
  const map = {}
  custList.forEach(c => { map[c.id] = c.companyName || c.customerName || c.customerCode })
  customerNameMap.value = map
  allCountries.value = countries
}

function showToast(msg) {
  ElMessage.success(msg)
}

function confirmAction(action, target) {
  ElMessageBox.confirm(`确认要${action} "${target}" 吗？`, '确认', { type: 'warning' })
    .then(() => {
      // Dispatch to the correct handler based on action context
      if (action === '禁用' || action === '启用') {
        handleToggleAction(action, target)
      } else {
        showToast(`${action}成功`)
      }
    })
    .catch(() => {})
}

// ---- Edit / Detail dialogs ----
const showEditDialog = ref(false)
const editDialogTitle = ref('编辑')
const showDetailDialog = ref(false)
const detailDialogTitle = ref('详情')

function openEditDialog(type, name) {
  editDialogTitle.value = `编辑 - ${type}: ${name}`
  showEditDialog.value = true
}

function openDetailDialog(type, name) {
  detailDialogTitle.value = `${type} - ${name}`
  showDetailDialog.value = true
}

// ========== Tab1: 人工审核 ==========
const reviewGlobalSwitch = ref(true)
const reviewStatusFilter = ref('')
const reviewPage = ref(1)
const reviewTicketTotal = ref(0)

const reviewRules = ref([])
const reviewOrders = ref([])

async function loadReviewRules() {
  try {
    const res = await reviewRuleList()
    reviewRules.value = (res.data || []).map(item => ({
      _id: item.id,
      name: item.ruleName,
      desc: item.description,
      enabled: item.isActive,
    }))
  } catch (e) {
    console.error('loadReviewRules failed', e)
  }
}

async function loadReviewTickets() {
  try {
    const params = { page: reviewPage.value, size: 10 }
    if (reviewStatusFilter.value) params.status = reviewStatusFilter.value
    const res = await reviewTicketPage(params)
    const pageData = res.data || {}
    reviewOrders.value = (pageData.list || []).map(item => ({
      _id: item.id,
      time: item.createdAt,
      customerId: item.customerId,
      customer: customerNameMap.value[item.customerId] || `客户#${item.customerId}`,
      preview: item.contentPreview,
      rule: item.triggerRule,
      status: item.status,
      reviewer: item.reviewer || '-',
    }))
    reviewTicketTotal.value = pageData.total || 0
  } catch (e) {
    console.error('loadReviewTickets failed', e)
  }
}

// Watch review rule toggle switches
watch(reviewRules, (newVal, oldVal) => {
  if (!oldVal || oldVal.length === 0) return
  newVal.forEach((rule, idx) => {
    if (oldVal[idx] && rule.enabled !== oldVal[idx].enabled) {
      reviewRuleToggle(rule._id, rule.enabled).then(() => {
        showToast(`规则 "${rule.name}" 已${rule.enabled ? '启用' : '禁用'}`)
      }).catch(() => {
        rule.enabled = !rule.enabled
      })
    }
  })
}, { deep: true })

watch([reviewStatusFilter, reviewPage], () => {
  loadReviewTickets()
})

async function handleTicketActionById(ticketId, action, customerName) {
  try {
    await ElMessageBox.confirm(`确认要${action === 'approve' ? '通过' : '拒绝'} "${customerName}" 的工单吗？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    await reviewTicketReview(ticketId, action, 'admin')
    showToast(action === 'approve' ? '审核通过成功' : '拒绝成功')
    loadReviewTickets()
  } catch (e) {
    console.error('handleTicketActionById failed', e)
  }
}

const filteredReviewOrders = computed(() => {
  // Filtering is done server-side now, return all loaded orders
  return reviewOrders.value
})

function ruleTagType(rule) {
  const map = { '新客户首发': 'warning', '关键词命中': 'danger', '发送量突增': '', '新SID首批': '' }
  return map[rule] ?? ''
}

function statusTagType(status) {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] ?? 'info'
}

// ========== Tab2: 路由策略 ==========
const smsAttrMap = { 0: '全部', 1: 'OTP 验证码', 2: '交易通知', 3: '营销推广' }
const channelGroupNameMap = ref({})

const strategyAssignments = ref([])

async function loadStrategyAssignments() {
  try {
    const res = await routingStrategyList()
    strategyAssignments.value = (res.data || []).map(item => {
      const isGroup = !!item.channelGroupId
      return {
        _id: item.id,
        isDefault: !item.customerId,
        customerId: item.customerId,
        customer: item.customerId ? (customerNameMap.value[item.customerId] || `客户#${item.customerId}`) : '',
        country: item.countryCode || '全部',
        smsType: smsAttrMap[item.smsAttribute] || '全部',
        targetType: isGroup ? '通道组' : '通道',
        targetName: isGroup
          ? (channelGroupNameMap.value[item.channelGroupId] || `通道组#${item.channelGroupId}`)
          : (channelNameMap.value[item.channelId] || `通道#${item.channelId}`),
      }
    })
  } catch (e) {
    console.error('loadStrategyAssignments failed', e)
  }
}

const showAssignDialog = ref(false)
const assignForm = ref({ customerId: '', countryCode: '', smsAttribute: 0, channelGroupIds: [], channelIds: [] })

function openAssignDialog() {
  assignForm.value = { customerId: '', countryCode: '', smsAttribute: 0, channelGroupIds: [], channelIds: [] }
  showAssignDialog.value = true
}

async function handleAssignConfirm() {
  const { customerId, countryCode, smsAttribute, channelGroupIds, channelIds } = assignForm.value
  if (channelGroupIds.length === 0 && channelIds.length === 0) {
    ElMessage.warning('请至少选择一个通道或通道组')
    return
  }
  try {
    const tasks = []
    for (const gid of channelGroupIds) {
      tasks.push(routingStrategySave({
        customerId: customerId || null,
        countryCode: countryCode || null,
        smsAttribute: smsAttribute,
        channelGroupId: gid,
        channelId: null,
      }))
    }
    for (const cid of channelIds) {
      tasks.push(routingStrategySave({
        customerId: customerId || null,
        countryCode: countryCode || null,
        smsAttribute: smsAttribute,
        channelGroupId: null,
        channelId: cid,
      }))
    }
    await Promise.all(tasks)
    showAssignDialog.value = false
    showToast(`已添加 ${tasks.length} 条覆盖规则`)
    loadStrategyAssignments()
  } catch (e) {
    console.error('handleAssignConfirm failed', e)
  }
}

async function handleStrategyDeleteById(id) {
  try {
    await ElMessageBox.confirm('确认要删除该策略分配吗？', '确认', { type: 'warning' })
  } catch { return }
  try {
    await routingStrategyDel(id)
    showToast('删除成功')
    loadStrategyAssignments()
  } catch (e) {
    console.error('handleStrategyDeleteById failed', e)
  }
}

// ========== Tab3: 通道调度 ==========
const showGroupDialog = ref(false)
const groupForm = ref({ id: null, name: '', mode: '' })

function openGroupEdit(row) {
  groupForm.value = { id: row._id, name: row.name, mode: row.mode }
  showGroupDialog.value = true
}
const groupSearchKey = ref('')
const allChannels = ref([])
const channelNameMap = ref({})

// Member management dialog
const showMemberMgmt = ref(false)
const memberMgmtGroup = ref(null)
const memberMgmtList = ref([])
const addChannelId = ref(null)
const memberSaving = ref(false)

const channelGroups = ref([])

async function loadAllChannels() {
  const list = await refData.loadChannels()
  allChannels.value = list
  const map = {}
  list.forEach(ch => { map[ch.id] = ch.channelName || ch.channelCode || ch.id })
  channelNameMap.value = map
}

const scheduleModeMap = { 1: 'failover', 2: 'weight', 3: 'priority_weight' }
const scheduleModeLabel = { 1: '故障转移', 2: '权重轮询', 3: '优先级加权' }

async function loadChannelGroups() {
  try {
    const res = await channelGroupList()
    const groupNameMap = {}
    const groups = (res.data || []).map(item => {
      groupNameMap[item.id] = item.groupName
      return {
        id: String(item.id),
        _id: item.id,
        name: item.groupName,
        mode: scheduleModeMap[item.scheduleMode] || 'failover',
        modeLabel: scheduleModeLabel[item.scheduleMode] || '故障转移',
        memberCount: 0,
        totalWeight: 0,
        enabled: item.isActive,
        members: [],
      }
    })
    channelGroups.value = groups
    channelGroupNameMap.value = groupNameMap
    // 加载每个组的成员数和权重合计
    for (const g of groups) {
      loadGroupMembers(g)
    }
  } catch (e) {
    console.error('loadChannelGroups failed', e)
  }
}

const filteredChannelGroups = computed(() => {
  if (!groupSearchKey.value) return channelGroups.value
  const key = groupSearchKey.value.toLowerCase()
  return channelGroups.value.filter(g => g.name.toLowerCase().includes(key))
})

async function loadGroupMembers(group) {
  try {
    const res = await channelGroupMembers(group._id)
    const members = (res.data || []).map(m => ({
      _id: m.id,
      channelId: m.channelId,
      name: channelNameMap.value[m.channelId] || `Channel-${m.channelId}`,
      priority: m.priority != null ? m.priority : 1,
      weight: m.weight != null ? m.weight : 100,
      healthColor: 'green',
      healthText: '健康',
      _dirty: false,
    }))
    group.members = members
    group.memberCount = members.length
    group.totalWeight = members.reduce((s, m) => s + (m.weight || 0), 0)
    return members
  } catch (e) {
    console.error('loadGroupMembers failed', e)
    return []
  }
}

async function handleGroupConfirm() {
  const modeMap = { failover: 1, weight: 2, priority_weight: 3 }
  try {
    const payload = {
      groupName: groupForm.value.name,
      scheduleMode: modeMap[groupForm.value.mode] || 1,
      isActive: true,
    }
    if (groupForm.value.id) payload.id = groupForm.value.id
    await channelGroupSave(payload)
    showGroupDialog.value = false
    showToast(groupForm.value.id ? '通道组更新成功' : '通道组创建成功')
    groupForm.value = { id: null, name: '', mode: '' }
    loadChannelGroups()
  } catch (e) {
    console.error('handleGroupConfirm failed', e)
  }
}

// ---- 管理成员弹窗 ----
const memberWeightTotal = computed(() => memberMgmtList.value.reduce((s, m) => s + (m.weight || 0), 0))
const availableChannels = computed(() => {
  const usedIds = new Set(memberMgmtList.value.map(m => m.channelId))
  return allChannels.value.filter(ch => !usedIds.has(ch.id))
})

async function openMemberMgmt(group) {
  memberMgmtGroup.value = group
  addChannelId.value = null
  const members = await loadGroupMembers(group)
  memberMgmtList.value = members.map(m => ({ ...m, _dirty: false }))
  showMemberMgmt.value = true
}

function markMemberDirty(row) { row._dirty = true }

async function handleQuickAdd() {
  if (!addChannelId.value || !memberMgmtGroup.value) return
  try {
    await channelGroupMemberAdd(memberMgmtGroup.value._id, {
      channelId: addChannelId.value,
      priority: memberMgmtList.value.length + 1,
      weight: 0,
      isActive: true,
    })
    addChannelId.value = null
    // 重新加载成员
    const members = await loadGroupMembers(memberMgmtGroup.value)
    memberMgmtList.value = members.map(m => ({ ...m, _dirty: false }))
    showToast('添加成功')
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function handleMemberRemoveInMgmt(member) {
  try {
    await ElMessageBox.confirm(`确认移除通道"${member.name}"？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    await channelGroupMemberDel(member._id)
    memberMgmtList.value = memberMgmtList.value.filter(m => m._id !== member._id)
    if (memberMgmtGroup.value) {
      memberMgmtGroup.value.memberCount = memberMgmtList.value.length
      memberMgmtGroup.value.totalWeight = memberMgmtList.value.reduce((s, m) => s + (m.weight || 0), 0)
    }
    showToast('移除成功')
  } catch (e) {
    ElMessage.error('移除失败')
  }
}

function autoDistributeWeight() {
  const n = memberMgmtList.value.length
  if (n === 0) return
  const avg = Math.floor(100 / n)
  const remainder = 100 - avg * n
  memberMgmtList.value.forEach((m, i) => {
    m.weight = avg + (i < remainder ? 1 : 0)
    m._dirty = true
  })
}

async function saveMemberChanges() {
  const dirtyMembers = memberMgmtList.value.filter(m => m._dirty)
  if (dirtyMembers.length === 0) { showMemberMgmt.value = false; return }
  if (memberWeightTotal.value !== 100) {
    try {
      await ElMessageBox.confirm(`权重合计为 ${memberWeightTotal.value}%，不等于 100%。确定保存吗？`, '权重提醒', { type: 'warning' })
    } catch { return }
  }
  memberSaving.value = true
  try {
    for (const m of dirtyMembers) {
      await channelGroupMemberUpdate(m._id, { priority: m.priority, weight: m.weight })
    }
    showToast('保存成功')
    showMemberMgmt.value = false
    // 更新通道组列表中的权重合计
    if (memberMgmtGroup.value) {
      memberMgmtGroup.value.totalWeight = memberWeightTotal.value
      memberMgmtGroup.value.memberCount = memberMgmtList.value.length
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    memberSaving.value = false
  }
}

async function handleToggleAction(action, target) {
  const isEnable = action === '启用'

  // Check if it's a channel group toggle
  const group = channelGroups.value.find(g => g.name === target)
  if (group) {
    try {
      await channelGroupToggle(group._id, isEnable)
      showToast(`${action}成功`)
      loadChannelGroups()
    } catch (e) {
      console.error('handleToggleAction (channelGroup) failed', e)
    }
    return
  }

  // Check if it's a template replace rule toggle (target is rule id)
  const tplRule = tplReplaceRules.value.find(r => String(r.id) === String(target))
  if (tplRule) {
    try {
      await tplReplaceToggle(tplRule._id, isEnable)
      showToast(`${action}成功`)
      loadTplReplaceRules()
    } catch (e) {
      console.error('handleToggleAction (tplReplace) failed', e)
    }
    return
  }

  // Check if it's a SID replace rule toggle (target is rule id)
  const sidRule = sidReplaceRules.value.find(r => String(r.id) === String(target))
  if (sidRule) {
    try {
      await sidReplaceToggle(sidRule._id, isEnable)
      showToast(`${action}成功`)
      loadSidReplaceRules()
    } catch (e) {
      console.error('handleToggleAction (sidReplace) failed', e)
    }
    return
  }

  showToast(`${action}成功`)
}

function dispatchModeTag(mode) {
  const map = { failover: 'success', weight: '', priority_weight: 'warning' }
  return map[mode] ?? ''
}

// ========== Tab4: 模板替换 ==========
const tplReplaceGlobalSwitch = ref(true)
const tplCondFilter = ref('')
const tplStatusFilter = ref('')

const tplReplaceRules = ref([])

async function loadTplReplaceRules() {
  try {
    const params = {}
    if (tplCondFilter.value) params.triggerCondition = tplCondFilter.value
    if (tplStatusFilter.value) params.active = tplStatusFilter.value === '启用'
    const res = await tplReplaceList(params)
    tplReplaceRules.value = (res.data || []).map(item => ({
      _id: item.id,
      id: item.id,
      condType: item.triggerCondition,
      condition: `${item.triggerCondition}=${item.triggerValue}`,
      matchType: item.matchType,
      findContent: item.findContent,
      replaceWith: item.replaceWith,
      scope: item.scope,
      triggerCount: item.triggerCount != null ? item.triggerCount.toLocaleString() : '0',
      status: item.isActive ? '启用' : '禁用',
    }))
  } catch (e) {
    console.error('loadTplReplaceRules failed', e)
  }
}

function tplCondTagType(condType) {
  const map = { '国家': '', '通道': 'warning', '客户': 'info' }
  return map[condType] ?? ''
}

// ========== Tab5: SID 替换 ==========
const sidReplaceGlobalSwitch = ref(true)

const sidReplaceRules = ref([])

async function loadSidReplaceRules() {
  try {
    const res = await sidReplaceList()
    sidReplaceRules.value = (res.data || []).map(item => ({
      _id: item.id,
      id: item.id,
      customer: customerNameMap.value[item.customerId] || `客户#${item.customerId}`,
      originalSid: item.originalSid,
      targetChannelId: item.targetChannelId,
      targetChannelName: channelNameMap.value[item.targetChannelId] || `通道#${item.targetChannelId || '-'}`,
      replaceSid: item.replacementSid,
      reason: item.replaceReason,
      reasonType: (item.replaceReason || '').includes('未注册') ? 'warning' : 'info',
      triggerCount: item.triggerCount != null ? item.triggerCount.toLocaleString() : '0',
      status: item.isActive ? '启用' : '禁用',
    }))
  } catch (e) {
    console.error('loadSidReplaceRules failed', e)
  }
}

// ---- 模板替换编辑 ----
const showTplEditDialog = ref(false)
const tplEditForm = ref({ id: null, triggerCondition: '', triggerValue: '', matchType: 'EXACT', findContent: '', replaceWith: '', scope: 'GLOBAL', isActive: true })

function openTplEdit(row) {
  if (row) {
    const orig = (tplReplaceRules.value || []).find(r => r._id === row._id)
    // Need to fetch original data from API since list data is mapped
    tplEditForm.value = {
      id: row._id,
      triggerCondition: row.condType || '',
      triggerValue: row.condition ? row.condition.split('=')[1] || '' : '',
      matchType: row.matchType || 'EXACT',
      findContent: row.findContent || '',
      replaceWith: row.replaceWith || '',
      scope: row.scope || 'GLOBAL',
      isActive: row.status === '启用',
    }
  } else {
    tplEditForm.value = { id: null, triggerCondition: '', triggerValue: '', matchType: 'EXACT', findContent: '', replaceWith: '', scope: 'GLOBAL', isActive: true }
  }
  showTplEditDialog.value = true
}

async function saveTplEditForm() {
  try {
    const payload = { ...tplEditForm.value }
    if (payload.id) payload.id = payload.id
    await tplReplaceSave(payload)
    showTplEditDialog.value = false
    showToast(payload.id ? '规则已更新' : '规则已创建')
    loadTplReplaceRules()
  } catch (e) {
    console.error('saveTplEditForm failed', e)
  }
}

// ---- SID替换编辑 ----
const showSidEditDialog = ref(false)
const sidEditFormRef = ref(null)
const sidEditForm = ref({ id: null, customerId: null, originalSid: '', targetChannelId: null, replacementSid: '', replaceReason: '', isActive: true })
const sidEditRules = {
  originalSid: [{ required: true, message: '请输入原始SID', trigger: 'blur' }],
  targetChannelId: [{ required: true, message: '请选择目标通道', trigger: 'change' }],
  replacementSid: [{ required: true, message: '请输入替换SID', trigger: 'blur' }],
}

function openSidEdit(row) {
  if (row) {
    sidEditForm.value = {
      id: row._id,
      customerId: null, // need to reverse-lookup from customer name
      originalSid: row.originalSid || '',
      targetChannelId: row.targetChannelId || null,
      replacementSid: row.replaceSid || '',
      replaceReason: row.reason || '',
      isActive: row.status === '启用',
    }
    // Try to find customerId by name
    const cust = allCustomers.value.find(c => (c.companyName || c.customerName) === row.customer)
    if (cust) sidEditForm.value.customerId = cust.id
  } else {
    sidEditForm.value = { id: null, customerId: null, originalSid: '', targetChannelId: null, replacementSid: '', replaceReason: '', isActive: true }
  }
  showSidEditDialog.value = true
}

async function saveSidEditForm() {
  try {
    if (sidEditFormRef.value) {
      const valid = await sidEditFormRef.value.validate().catch(() => false)
      if (!valid) return
    }
    const payload = { ...sidEditForm.value }
    await sidReplaceSave(payload)
    showSidEditDialog.value = false
    showToast(payload.id ? '规则已更新' : '规则已创建')
    loadSidReplaceRules()
  } catch (e) {
    console.error('saveSidEditForm failed', e)
  }
}

// SID replace logs - no backend API, keep as mock data
const sidReplaceLogs = ref([
  { time: '2026-03-14 14:32:18', msgId: 'MSG-2026031400128', customer: 'TechCorp', originalSid: 'BrandA', actualSid: 'TechBR', channel: 'Sinch-BR-01', country: 'BR', reason: '原SID未在该通道注册' },
  { time: '2026-03-14 14:28:05', msgId: 'MSG-2026031400096', customer: 'GlobalPay', originalSid: 'GPay', actualSid: 'DefaultNG', channel: 'Route-NG-01', country: 'NG', reason: '使用通道默认SID' },
  { time: '2026-03-14 14:15:42', msgId: 'MSG-2026031400054', customer: 'ShopMax', originalSid: 'ShopPromo', actualSid: 'ShopIN', channel: 'Vonage-IN-02', country: 'IN', reason: '原SID未在该通道注册' },
])

// ========== Init ==========
onMounted(async () => {
  await loadRefData()
  loadReviewRules()
  loadReviewTickets()
  loadAllChannels()
  await loadChannelGroups()
  loadStrategyAssignments()
  loadTplReplaceRules()
  loadSidReplaceRules()
})
</script>

<style scoped>
.ops-strategy-page {
  padding: 0;
}

/* Page Header */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
}

/* Tab Radio Group (pill style) */
.tab-radio-group {
  margin-bottom: 20px;
}
.tab-radio-group :deep(.el-radio-button__inner) {
  font-size: 14px;
}

/* Global Switch Bar */
.global-switch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}
.global-switch-bar .switch-label {
  font-size: 15px;
  font-weight: 600;
}
.global-switch-bar .switch-desc {
  font-size: 13px;
  color: #999;
}

/* Table Card */
.table-card {
  margin-bottom: 16px;
}
.table-card :deep(.el-card__header) {
  padding: 14px 20px;
}
.card-title-text {
  font-size: 16px;
  font-weight: 600;
}

/* Pagination */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: flex-end;
}
.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.filter-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

/* Health Dot */
.health-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.health-green { background: #52c41a; }
.health-yellow { background: #faad14; }
.health-red { background: #ff4d4f; }
.health-gray { background: #d9d9d9; }

/* Member table wrap */
.member-table-wrap {
  padding: 0 20px 10px 40px;
  background: #fafafa;
}

/* Code snippets */
.code-red {
  background: #fff2f0;
  color: #ff4d4f;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 12px;
}
.code-green {
  background: #f6ffed;
  color: #52c41a;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 12px;
}
.code-blue {
  background: #e6f7ff;
  padding: 2px 8px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 12px;
}

/* Section Title */
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

/* Priority Flow */
.priority-flow {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
}
.priority-steps {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.priority-step {
  border-radius: 6px;
  padding: 10px 16px;
  text-align: center;
  min-width: 140px;
}
.step-blue {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}
.step-yellow {
  background: #fffbe6;
  border: 1px solid #ffe58f;
}
.step-red {
  background: #fff2f0;
  border: 1px solid #ffccc7;
}
.step-gray {
  background: #f0f0f0;
  border: 1px solid #d9d9d9;
}
.step-num {
  font-size: 20px;
  margin-bottom: 4px;
}
.step-title {
  font-size: 12px;
  font-weight: 500;
}
.step-desc {
  font-size: 11px;
  color: #999;
}
.step-arrow {
  color: #999;
  font-size: 16px;
}

/* Responsive */
</style>
