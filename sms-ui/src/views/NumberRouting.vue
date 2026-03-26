<template>
  <div class="number-routing-page">
    <div class="page-header"><h2>号码路由</h2></div>

    <!-- Sub-nav pill buttons -->
    <div class="sub-nav">
      <el-radio-group v-model="activeTab" size="default">
        <el-radio-button value="segment">号段归属表</el-radio-button>
        <el-radio-button value="mnp">MNP 缓存表</el-radio-button>
        <el-radio-button value="route">默认路由表</el-radio-button>
        <el-radio-button value="fallback">兜底通道配置</el-radio-button>
        <el-radio-button value="filter">号码过滤规则</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ==================== Tab 1: 号段归属表 ==================== -->
    <div v-if="activeTab === 'segment'">
      <div class="section-header">
        <div>
          <div class="section-title">号段归属表</div>
          <div class="section-desc">基于 libphonenumber 的国家码 + 运营商号段数据，支持人工补充特殊号段</div>
        </div>
        <div style="display: flex; gap: 10px;">
          <el-button @click="syncDialog = true">↻ 同步 libphonenumber</el-button>
          <el-button type="primary" @click="addSegmentDialog = true">+ 手动补充号段</el-button>
        </div>
      </div>

      <!-- Stat cards -->
      <el-row :gutter="16" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">已覆盖国家</div>
            <div class="stat-value">47</div>
            <div class="stat-sub">libphonenumber 主数据</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">运营商号段条目</div>
            <div class="stat-value">3,821</div>
            <div class="stat-sub">含 MCC/MNC 映射</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">人工补充条目</div>
            <div class="stat-value" style="color: #d97706;">28</div>
            <div class="stat-sub">覆盖特殊/未收录号段</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">上次版本同步</div>
            <div class="stat-value" style="font-size: 18px;">v8.13.29</div>
            <div class="stat-sub">2026-03-10，建议每月一次</div>
          </div>
        </el-col>
      </el-row>

      <!-- Process flow -->
      <el-card shadow="never" style="margin-bottom: 20px;">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span style="font-size: 14px; font-weight: 600;">号码完整处理流程（8步）</span>
            <span style="font-size: 12px; color: #9ca3af;">消息到达后按此顺序执行，任一步过滤则终止</span>
          </div>
        </template>
        <div class="flow-wrap">
          <div v-for="(step, idx) in flowSteps" :key="idx" style="display: flex; align-items: flex-start;">
            <div class="flow-step">
              <div class="flow-circle" :class="step.type">
                {{ idx + 1 }}
                <div v-if="step.reject" class="flow-reject">{{ step.reject }}</div>
              </div>
              <div class="flow-step-label">{{ step.label }}</div>
              <div class="flow-step-desc" v-html="step.desc"></div>
            </div>
            <div v-if="idx < flowSteps.length - 1" class="flow-arrow">&#8250;</div>
          </div>
        </div>
      </el-card>

      <!-- Segment query card -->
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span style="font-size: 14px; font-weight: 600;">号段归属查询</span>
            <div style="display: flex; gap: 10px; align-items: center;">
              <el-input v-model="segLookup" placeholder="输入手机号查询归属（如 +66812345678）" style="width: 300px;" size="default" />
              <el-button type="primary" size="default">查询</el-button>
            </div>
          </div>
        </template>

        <!-- Filter bar -->
        <div class="filter-bar">
          <el-select v-model="segQuery.country" placeholder="国家" clearable size="default" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="泰国" value="TH" />
            <el-option label="印尼" value="ID" />
            <el-option label="马来西亚" value="MY" />
            <el-option label="菲律宾" value="PH" />
            <el-option label="新加坡" value="SG" />
          </el-select>
          <el-input v-model="segQuery.operator" placeholder="运营商名称/MCC" clearable size="default" style="width: 160px;" />
          <el-select v-model="segQuery.source" placeholder="数据来源" clearable size="default" style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="libphonenumber" value="lib" />
            <el-option label="人工补充" value="manual" />
          </el-select>
          <el-button type="primary" size="default" @click="segQuery.page = 1; loadSegments()">搜索</el-button>
          <el-button size="default" @click="segQuery.country = ''; segQuery.operator = ''; segQuery.source = ''; segQuery.page = 1; loadSegments()">重置</el-button>
        </div>

        <!-- Table -->
        <el-table :data="segmentList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
          <el-table-column prop="country" label="国家" min-width="110" />
          <el-table-column prop="countryCode" label="国家码" width="80" />
          <el-table-column prop="mcc" label="MCC" width="70" />
          <el-table-column prop="mnc" label="MNC" width="70" />
          <el-table-column prop="operator" label="运营商" min-width="120">
            <template #default="{ row }">
              <strong>{{ row.operator }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="prefix" label="号段前缀" min-width="140" />
          <el-table-column prop="numType" label="号码类型" width="90">
            <template #default="{ row }">
              <el-tag type="success" size="small" round>{{ row.numType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="source" label="数据来源" width="120">
            <template #default="{ row }">
              <el-tag :type="row.source === 'libphonenumber' ? '' : 'warning'" size="small" round>{{ row.source }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="120">
            <template #default="{ row }">
              <span style="font-size: 12px; color: #9ca3af;">{{ row.updatedAt }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.source === '人工补充'" link type="primary" size="small">编辑</el-button>
              <el-button v-if="row.source === '人工补充'" link type="danger" size="small" @click="handleDeleteSegment(row)">删除</el-button>
              <el-button v-if="row.source !== '人工补充'" link type="primary" size="small">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination v-model:current-page="segQuery.page" v-model:page-size="segQuery.size"
                         :total="segmentTotal" layout="total, prev, pager, next" />
        </div>
      </el-card>
    </div>

    <!-- ==================== Tab 2: MNP 缓存表 ==================== -->
    <div v-else-if="activeTab === 'mnp'">
      <div class="section-header">
        <div>
          <div class="section-title">MNP 缓存表</div>
          <div class="section-desc">携号转网本地缓存，人工维护，发送前优先查此表覆盖号段库识别结果（V2 将接入实时 HLR 查询）</div>
        </div>
        <div style="display: flex; gap: 10px;">
          <el-button @click="importMnpDialog = true">导入批量 MNP</el-button>
          <el-button type="primary" @click="addMnpDialog = true">+ 新增 MNP 记录</el-button>
        </div>
      </div>

      <!-- Info banner -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>
          <strong>V1 限制说明：</strong>当前为本地缓存表，需运营人员手动维护。触发时机：发送失败且错误码为"号码不属于该运营商"、客户反馈发送异常、收到运营商批量通知。V2 将接入实时 HLR 自动更新，届时本表仅作兜底。
        </template>
      </el-alert>

      <!-- Stat cards -->
      <el-row :gutter="16" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">MNP 记录总数</div>
            <div class="stat-value">1,247</div>
            <div class="stat-sub">覆盖 5 个国家</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">本月新增</div>
            <div class="stat-value" style="color: #4361ee;">83</div>
            <div class="stat-sub">较上月 +12</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">客户反馈来源</div>
            <div class="stat-value">621</div>
            <div class="stat-sub">占比 49.8%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">30天未验证</div>
            <div class="stat-value" style="color: #d97706;">142</div>
            <div class="stat-sub">建议重新核实</div>
          </div>
        </el-col>
      </el-row>

      <el-card shadow="never">
        <!-- Filter bar -->
        <div class="filter-bar" style="margin-bottom: 0;">
          <el-input v-model="mnpQuery.phone" placeholder="E.164格式" clearable size="default" style="width: 160px;" />
          <el-select v-model="mnpQuery.country" placeholder="国家" clearable size="default" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="泰国" value="TH" />
            <el-option label="印尼" value="ID" />
            <el-option label="马来西亚" value="MY" />
          </el-select>
          <el-select v-model="mnpQuery.source" placeholder="来源" clearable size="default" style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="客户反馈" value="customer" />
            <el-option label="运营商通知" value="carrier" />
            <el-option label="发送失败分析" value="analysis" />
          </el-select>
          <el-select v-model="mnpQuery.status" placeholder="状态" clearable size="default" style="width: 110px;">
            <el-option label="全部" value="" />
            <el-option label="已验证" value="verified" />
            <el-option label="待验证" value="pending" />
          </el-select>
          <el-button type="primary" size="default" @click="mnpQuery.page = 1; loadMnp()">搜索</el-button>
          <el-button size="default" @click="mnpQuery.phone = ''; mnpQuery.country = ''; mnpQuery.source = ''; mnpQuery.status = ''; mnpQuery.page = 1; loadMnp()">重置</el-button>
          <div style="flex: 1;" />
          <el-button size="default" style="color: #d97706;">导出</el-button>
        </div>

        <!-- Table -->
        <el-table :data="mnpList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
          <el-table-column prop="phone" label="号码（E.164）" min-width="150">
            <template #default="{ row }">
              <strong>{{ row.phone }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="country" label="国家" width="120" />
          <el-table-column prop="originalOp" label="原始运营商（号段库）" min-width="180">
            <template #default="{ row }">
              <span style="color: #9ca3af; text-decoration: line-through;">{{ row.originalOp }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="actualOp" label="实际运营商（MNP后）" min-width="180">
            <template #default="{ row }">
              <strong style="color: #16a34a;">{{ row.actualOp }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="110">
            <template #default="{ row }">
              <el-tag :type="mnpSourceTagType(row.source)" size="small">{{ row.source }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="120">
            <template #default="{ row }">
              <span style="font-size: 12px; color: #9ca3af;">{{ row.updatedAt }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === '已验证' ? 'success' : 'warning'" size="small" round>{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === '待验证'" link type="success" size="small">验证</el-button>
              <el-button link type="primary" size="small">编辑</el-button>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination v-model:current-page="mnpQuery.page" v-model:page-size="mnpQuery.size"
                         :total="mnpTotal" layout="total, prev, pager, next" />
        </div>
      </el-card>
    </div>

    <!-- ==================== Tab 3: 默认路由表 ==================== -->
    <div v-else-if="activeTab === 'route'">
      <div class="section-header">
        <div>
          <div class="section-title">默认路由表</div>
          <div class="section-desc">配置「国家 + 运营商」到「通道/通道组」的映射，作为第 2 级通道分配逻辑（变更实时生效）</div>
        </div>
        <el-button type="primary" @click="addRouteDialog = true">+ 新增路由规则</el-button>
      </div>

      <!-- Priority explanation -->
      <el-card shadow="never" style="margin-bottom: 20px;">
        <template #header>
          <span style="font-size: 14px; font-weight: 600;">通道分配三级优先级</span>
        </template>
        <div class="route-tiers">
          <div class="route-tier t1">
            <div class="rt-num" style="background: #4361ee;">1</div>
            <div class="rt-label">客户 SID 绑定的通道 / 通道组</div>
            <div class="rt-desc">客户在 API 请求中指定 SID，或系统按 SID 偏好匹配。绑定单通道直接使用，绑定通道组则由调度引擎选通道。</div>
            <el-tag size="small" effect="light" style="flex-shrink: 0;">最高优先</el-tag>
          </div>
          <div class="route-tier t2">
            <div class="rt-num" style="background: #16a34a;">2</div>
            <div class="rt-label">系统默认路由表（本页配置）</div>
            <div class="rt-desc">根据解析出的国家码 + 运营商（MCC/MNC）在此表中查找对应通道或通道组，未命中则进入第 3 级。</div>
            <el-tag type="success" size="small" effect="light" style="flex-shrink: 0;">次优先</el-tag>
          </div>
          <div class="route-tier t3">
            <div class="rt-num" style="background: #d97706;">3</div>
            <div class="rt-label">全局兜底通道（兜底通道配置 Tab 设置）</div>
            <div class="rt-desc">前两级均无法匹配时使用兜底通道。兜底通道不可为空，否则消息发送失败。</div>
            <el-tag type="warning" size="small" effect="light" style="flex-shrink: 0;">最终兜底</el-tag>
          </div>
          <div class="route-tier tfail">
            <div class="rt-num" style="background: #e63946;">&#10007;</div>
            <div class="rt-label" style="color: #e63946;">无法分配</div>
            <div class="rt-desc" style="color: #e63946;">三级均无法匹配且兜底通道未配置，消息发送失败，记录失败原因"无可用通道"。</div>
            <el-tag type="danger" size="small" effect="light" style="flex-shrink: 0;">发送失败</el-tag>
          </div>
        </div>
      </el-card>

      <!-- Route table -->
      <el-card shadow="never">
        <div class="filter-bar">
          <el-select v-model="routeQuery.country" placeholder="国家" clearable size="default" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="泰国" value="TH" />
            <el-option label="印尼" value="ID" />
            <el-option label="马来西亚" value="MY" />
            <el-option label="菲律宾" value="PH" />
          </el-select>
          <el-input v-model="routeQuery.operator" placeholder="运营商名或MCC/MNC" clearable size="default" style="width: 170px;" />
          <el-input v-model="routeQuery.channel" placeholder="通道/通道组名称" clearable size="default" style="width: 170px;" />
          <el-button type="primary" size="default" @click="loadRoutes()">搜索</el-button>
          <el-button size="default" @click="routeQuery.country = ''; routeQuery.operator = ''; routeQuery.channel = ''; loadRoutes()">重置</el-button>
        </div>

        <el-table :data="routeList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
          <el-table-column prop="country" label="国家" width="110" />
          <el-table-column prop="operator" label="运营商" min-width="130">
            <template #default="{ row }">
              <strong v-if="row.operator && !row.operator.startsWith('—')">{{ row.operator }}</strong>
              <span v-else style="color: #9ca3af;">{{ row.operator }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="mccMnc" label="MCC / MNC" width="110">
            <template #default="{ row }">
              <span :style="row.mccMnc.includes('*') ? 'color: #9ca3af;' : ''">{{ row.mccMnc }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="matchScope" label="匹配范围" width="120">
            <template #default="{ row }">
              <el-tag :type="row.matchScope === '精确MCC/MNC' ? '' : 'warning'" size="small" round>{{ row.matchScope }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetChannel" label="目标通道 / 通道组" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.targetChannel }}</strong>
              <span v-if="row.channelNote" style="color: #9ca3af;">{{ row.channelNote }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="schedMode" label="调度模式" width="110">
            <template #default="{ row }">
              <el-tag :type="schedModeTagType(row.schedMode)" size="small" round>{{ row.schedMode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <span>
                <span class="status-dot" :class="row.status === '启用' ? 'dot-green' : 'dot-yellow'"></span>
                {{ row.status }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="120">
            <template #default="{ row }">
              <span style="font-size: 12px; color: #9ca3af;">{{ row.updatedAt }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === '停用'" link type="success" size="small" @click="handleToggleRoute(row)">启用</el-button>
              <el-button link type="primary" size="small">编辑</el-button>
              <el-button v-if="!row.matchScope.includes('兜底')" link type="danger" size="small" @click="handleDeleteRoute(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination v-model:current-page="routeQuery.page" v-model:page-size="routeQuery.size"
                         :total="routeList.length" layout="total, prev, pager, next" />
        </div>
      </el-card>
    </div>

    <!-- ==================== Tab 4: 兜底通道配置 ==================== -->
    <div v-else-if="activeTab === 'fallback'">
      <div class="section-header">
        <div>
          <div class="section-title">兜底通道配置</div>
          <div class="section-desc">当 SID 绑定和路由表均无法匹配时的最终兜底通道，必须配置以避免发送失败</div>
        </div>
      </div>

      <!-- Warning banner -->
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>
          <strong>配置要求：</strong>全局兜底通道不可为空。如当前兜底通道健康度下降，请及时更换，否则所有路由未命中的消息将全部失败，记录"无可用通道"。
        </template>
      </el-alert>

      <!-- Global fallback card -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span style="font-size: 14px; font-weight: 600;">全局兜底通道</span>
            <el-button size="small" @click="editGlobalFallbackDialog = true">修改</el-button>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="8">
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px;">当前兜底通道组</div>
            <div style="font-size: 16px; font-weight: 700; color: #1f2937;">GLOBAL-FALLBACK-01</div>
            <div style="font-size: 12px; color: #9ca3af; margin-top: 2px;">包含 3 条通道，主备模式</div>
          </el-col>
          <el-col :span="8">
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px;">健康状态</div>
            <div style="font-size: 16px; font-weight: 700; color: #16a34a;">
              <span class="status-dot dot-green"></span>健康
            </div>
            <div style="font-size: 12px; color: #9ca3af; margin-top: 2px;">3/3 通道正常</div>
          </el-col>
          <el-col :span="8">
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px;">本月触发次数</div>
            <div style="font-size: 16px; font-weight: 700; color: #d97706;">4,821</div>
            <div style="font-size: 12px; color: #9ca3af; margin-top: 2px;">占总发送量 0.17%</div>
          </el-col>
        </el-row>
      </el-card>

      <!-- Country-level fallback -->
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span style="font-size: 14px; font-weight: 600;">国家级兜底通道（可选覆盖全局兜底）</span>
            <el-button type="primary" size="small" @click="addCountryFallbackDialog = true">+ 新增国家兜底</el-button>
          </div>
        </template>
        <div style="padding: 10px 0 12px 0; font-size: 12px; color: #6b7280;">
          当路由表无法命中特定国家的规则时，优先使用该国专属兜底（优先级高于全局兜底）
        </div>
        <el-table :data="countryFallbackList" stripe :header-cell-style="headerStyle" style="font-size: 13px;">
          <el-table-column prop="country" label="国家" width="120" />
          <el-table-column prop="channel" label="兜底通道 / 通道组" min-width="200">
            <template #default="{ row }">
              <strong>{{ row.channel }}</strong>
              <span v-if="row.channelNote" style="color: #9ca3af; font-size: 12px;"> {{ row.channelNote }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="schedMode" label="调度模式" width="110">
            <template #default="{ row }">
              <el-tag :type="schedModeTagType(row.schedMode)" size="small" round>{{ row.schedMode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="health" label="健康状态" width="150">
            <template #default="{ row }">
              <span>
                <span class="status-dot" :class="row.healthDot"></span>
                <span :style="{ color: row.healthColor }">{{ row.health }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="triggerCount" label="本月触发次数" width="120" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default>
              <el-button link type="primary" size="small">编辑</el-button>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- ==================== Tab 5: 号码过滤规则 ==================== -->
    <div v-else-if="activeTab === 'filter'">
      <div class="section-header">
        <div>
          <div class="section-title">号码过滤规则</div>
          <div class="section-desc">配置号码类型过滤开关、退订关键词及全局过滤行为</div>
        </div>
      </div>

      <el-row :gutter="16">
        <!-- Number type filter -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <span style="font-size: 14px; font-weight: 600;">号码类型过滤</span>
            </template>
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 14px;">按号码类型决定是否过滤（基于 libphonenumber 识别结果）</div>
            <div style="display: flex; flex-direction: column; gap: 16px;">
              <div v-for="(rule, idx) in filterRules" :key="idx" style="display: flex; align-items: center; justify-content: space-between;">
                <div>
                  <div style="font-size: 13px; color: #374151;">{{ rule.label }}</div>
                  <div style="font-size: 11px; color: #9ca3af;">{{ rule.desc }}</div>
                </div>
                <el-switch v-model="rule.enabled" />
              </div>
            </div>
            <div style="padding-top: 14px; margin-top: 14px; border-top: 1px solid #e5e7eb;">
              <el-button type="primary" size="small">保存配置</el-button>
            </div>
          </el-card>
        </el-col>

        <!-- Opt-out keywords -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span style="font-size: 14px; font-weight: 600;">退订关键词配置</span>
                <el-button type="primary" size="small" @click="addKeywordDialog = true">+ 新增</el-button>
              </div>
            </template>
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 12px;">用户回复以下任一关键词后自动加入退订名单，后续发送永久过滤</div>
            <div style="display: flex; flex-wrap: wrap; gap: 6px;">
              <el-tag v-for="kw in optoutKeywords" :key="kw" closable size="default"
                      @close="removeKeyword(kw)">{{ kw }}</el-tag>
            </div>
            <div style="margin-top: 16px; padding: 10px 12px; background: #f9fafb; border-radius: 6px; font-size: 12px; color: #6b7280;">
              <strong>注意：</strong>退订名单由用户触发自动写入，运营人员只能查看，不可删除（合规要求）。退订记录可在 <span style="color: #4361ee; cursor: pointer;">风控管理 -> 黑名单</span> 中查询。
            </div>
          </el-card>
        </el-col>

        <!-- Filter statistics -->
        <el-col :span="24" style="margin-top: 16px;">
          <el-card shadow="never">
            <template #header>
              <span style="font-size: 14px; font-weight: 600;">今日过滤统计</span>
            </template>
            <el-row :gutter="12">
              <el-col v-for="(stat, idx) in filterStats" :key="idx" :span="4">
                <div style="text-align: center; padding: 12px; background: #f9fafb; border-radius: 8px;">
                  <div style="font-size: 10px; color: #6b7280; margin-bottom: 4px;">{{ stat.label }}</div>
                  <div style="font-size: 20px; font-weight: 700;" :style="{ color: stat.color }">{{ stat.value }}</div>
                </div>
              </el-col>
            </el-row>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- ==================== Dialogs ==================== -->

    <!-- Sync libphonenumber dialog -->
    <el-dialog v-model="syncDialog" title="同步 libphonenumber 数据库" width="460px" destroy-on-close>
      <div style="padding: 16px; background: #f9fafb; border-radius: 8px; margin-bottom: 16px;">
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <span style="font-size: 12px; color: #6b7280;">当前版本</span><strong>v8.13.29</strong>
        </div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <span style="font-size: 12px; color: #6b7280;">最新版本</span><strong style="color: #16a34a;">v8.13.31</strong>
        </div>
        <div style="display: flex; justify-content: space-between;">
          <span style="font-size: 12px; color: #6b7280;">更新内容</span><span style="font-size: 12px; color: #374151;">泰国 True Move 新号段 (520/04)</span>
        </div>
      </div>
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>同步期间不影响正在发送的消息，但号段识别结果可能在同步完成前出现短暂不一致（通常 &lt; 30s）。</template>
      </el-alert>
      <template #footer>
        <el-button @click="syncDialog = false">取消</el-button>
        <el-button type="primary" @click="doSync" :loading="syncing">确认同步</el-button>
      </template>
    </el-dialog>

    <!-- Add segment dialog -->
    <el-dialog v-model="addSegmentDialog" title="手动补充号段" width="560px" destroy-on-close>
      <el-form :model="segmentForm" label-width="110px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="国家" required>
              <el-select v-model="segmentForm.country" style="width: 100%;">
                <el-option label="泰国" value="TH" /><el-option label="印尼" value="ID" />
                <el-option label="马来西亚" value="MY" /><el-option label="菲律宾" value="PH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="国家码" required>
              <el-input v-model="segmentForm.countryCode" placeholder="如 +66" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MCC" required>
              <el-input v-model="segmentForm.mcc" placeholder="如 520" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MNC" required>
              <el-input v-model="segmentForm.mnc" placeholder="如 01" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运营商名称" required>
              <el-input v-model="segmentForm.operator" placeholder="运营商英文名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="号码类型" required>
              <el-select v-model="segmentForm.numType" style="width: 100%;">
                <el-option label="移动" value="移动" /><el-option label="固话" value="固话" />
                <el-option label="VoIP" value="VoIP" /><el-option label="付费" value="付费" />
                <el-option label="免费" value="免费" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="号段前缀描述">
              <el-input v-model="segmentForm.prefix" placeholder="如 66 845xx，多个用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="补充原因/来源">
              <el-input v-model="segmentForm.reason" type="textarea" :rows="3"
                        placeholder="说明此号段未被 libphonenumber 收录的原因，或数据来源" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addSegmentDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSegment">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add MNP dialog -->
    <el-dialog v-model="addMnpDialog" title="新增 MNP 携号转网记录" width="560px" destroy-on-close>
      <el-form :model="mnpForm" label-width="150px">
        <el-form-item label="手机号（E.164格式）" required>
          <el-input v-model="mnpForm.phone" placeholder="如 66812345678（不含+）" />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="原始运营商" required>
              <el-input v-model="mnpForm.originalOp" placeholder="如 DTAC (520/05)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际运营商" required>
              <el-input v-model="mnpForm.actualOp" placeholder="如 AIS (520/01)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源" required>
              <el-select v-model="mnpForm.source" style="width: 100%;">
                <el-option label="客户反馈" value="客户反馈" />
                <el-option label="运营商通知" value="运营商通知" />
                <el-option label="发送失败分析" value="发送失败分析" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="mnpForm.status" style="width: 100%;">
                <el-option label="待验证" value="待验证" />
                <el-option label="已验证" value="已验证" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="mnpForm.remark" type="textarea" :rows="2" placeholder="触发原因、工单号等补充信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMnpDialog = false">取消</el-button>
        <el-button type="primary" @click="addMnpDialog = false; ElMessage.success('保存成功')">保存</el-button>
      </template>
    </el-dialog>

    <!-- Import MNP dialog -->
    <el-dialog v-model="importMnpDialog" title="批量导入 MNP 记录" width="480px" destroy-on-close>
      <div style="border: 2px dashed #d1d5db; border-radius: 8px; padding: 32px; text-align: center; margin-bottom: 16px; cursor: pointer;">
        <div style="font-size: 28px; margin-bottom: 8px;">&#128194;</div>
        <div style="font-size: 13px; color: #374151;">点击选择 CSV 文件，或拖拽到此处</div>
        <div style="font-size: 11px; color: #9ca3af; margin-top: 4px;">格式：号码, 原始运营商, 实际运营商, 来源</div>
      </div>
      <a href="#" style="font-size: 12px; color: #4361ee;">下载导入模板 CSV</a>
      <template #footer>
        <el-button @click="importMnpDialog = false">取消</el-button>
        <el-button type="primary" @click="importMnpDialog = false; ElMessage.success('导入成功')">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- Add route dialog -->
    <el-dialog v-model="addRouteDialog" title="新增默认路由规则" width="720px" destroy-on-close>
      <el-form :model="routeForm" label-width="140px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="目标国家" required>
              <el-select v-model="routeForm.country" style="width: 100%;">
                <el-option label="泰国 (+66)" value="TH" /><el-option label="印尼 (+62)" value="ID" />
                <el-option label="马来西亚 (+60)" value="MY" /><el-option label="菲律宾 (+63)" value="PH" />
                <el-option label="新加坡 (+65)" value="SG" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="匹配范围" required>
              <el-select v-model="routeForm.matchScope" style="width: 100%;">
                <el-option label="精确 MCC/MNC（指定运营商）" value="exact" />
                <el-option label="国家级（该国所有运营商）" value="country" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MCC" required>
              <el-input v-model="routeForm.mcc" placeholder="如 520" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MNC" required>
              <el-input v-model="routeForm.mnc" placeholder="如 01，留空=该国全部" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运营商名称">
              <el-input v-model="routeForm.operator" placeholder="备注用（选填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标通道/通道组" required>
              <el-select v-model="routeForm.channel" filterable style="width: 100%;">
                <el-option v-for="o in channelAndGroupOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调度模式">
              <el-select v-model="routeForm.schedMode" style="width: 100%;">
                <el-option label="主备切换（自动）" value="主备切换" />
                <el-option label="加权分流" value="加权分流" />
                <el-option label="直连" value="直连" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="routeForm.status" style="width: 100%;">
                <el-option label="启用" value="启用" />
                <el-option label="停用" value="停用" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addRouteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoute">保存</el-button>
      </template>
    </el-dialog>

    <!-- Edit global fallback dialog -->
    <el-dialog v-model="editGlobalFallbackDialog" title="修改全局兜底通道" width="440px" destroy-on-close>
      <el-form label-width="160px">
        <el-form-item label="全局兜底通道/通道组" required>
          <el-select v-model="globalFallbackChannel" style="width: 100%;">
            <el-option label="GLOBAL-FALLBACK-01（主备，3通道）" value="GLOBAL-FALLBACK-01" />
            <el-option label="TH-兜底组" value="TH-兜底组" />
            <el-option label="ID-主备组" value="ID-主备组" />
          </el-select>
        </el-form-item>
        <div style="font-size: 11px; color: #d97706; margin-top: -8px; padding-left: 160px;">
          &#9888; 修改立即生效，请确保所选通道组健康
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editGlobalFallbackDialog = false">取消</el-button>
        <el-button type="primary" @click="editGlobalFallbackDialog = false; ElMessage.success('修改成功')">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- Add country fallback dialog -->
    <el-dialog v-model="addCountryFallbackDialog" title="新增国家级兜底通道" width="440px" destroy-on-close>
      <el-form :model="countryFallbackForm" label-width="130px">
        <el-form-item label="国家" required>
          <el-select v-model="countryFallbackForm.country" style="width: 100%;">
            <el-option label="泰国" value="TH" /><el-option label="印尼" value="ID" />
            <el-option label="马来西亚" value="MY" /><el-option label="菲律宾" value="PH" />
          </el-select>
        </el-form-item>
        <el-form-item label="兜底通道/通道组" required>
          <el-select v-model="countryFallbackForm.channel" style="width: 100%;">
            <el-option label="TH-兜底组" value="TH-兜底组" />
            <el-option label="ID-主备组" value="ID-主备组" />
            <el-option label="GLOBAL-FALLBACK-01" value="GLOBAL-FALLBACK-01" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addCountryFallbackDialog = false">取消</el-button>
        <el-button type="primary" @click="addCountryFallbackDialog = false; ElMessage.success('保存成功')">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add keyword dialog -->
    <el-dialog v-model="addKeywordDialog" title="新增退订关键词" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="关键词" required>
          <el-input v-model="newKeyword" placeholder="如 CANCEL、取消" />
          <div style="font-size: 11px; color: #9ca3af; margin-top: 4px;">不区分大小写；用户回复后自动加入退订名单</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addKeywordDialog = false">取消</el-button>
        <el-button type="primary" @click="addKeyword">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  segmentPage, segmentSave, segmentDel,
  mnpCachePage, mnpClearExpired,
  defaultRouteList, defaultRouteSave, defaultRouteDel, defaultRouteToggle,
  filterRuleList, filterRuleSave, filterRuleDel, filterRuleToggle,
  channelGroupList
} from '../api'
import { useRefData } from '../stores/refData'

const refData = useRefData()

/* ---- constants ---- */
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

/* ---- shared state ---- */
const activeTab = ref('segment')

// Reference data
const channelAndGroupOptions = ref([])
const loadChannelAndGroups = async () => {
  const [channels, grRes] = await Promise.all([
    refData.loadChannels(),
    channelGroupList().catch(() => ({ data: [] }))
  ])
  const opts = []
  const groups = grRes.data || grRes || []
  if (Array.isArray(groups)) groups.forEach(g => opts.push({ label: g.groupName, value: g.groupName }))
  channels.forEach(c => opts.push({ label: c.channelName || c.name, value: c.channelName || c.name }))
  channelAndGroupOptions.value = opts
}

/* ---- Flow steps ---- */
const flowSteps = [
  { type: 'process', label: '号码解析', desc: 'E.164 标准化<br>清洗非数字字符', reject: '格式错误->丢弃' },
  { type: 'filter', label: '格式校验', desc: '长度+国家码<br>合法性验证', reject: '格式非法->过滤' },
  { type: 'filter', label: '号码类型', desc: '识别移动/固话<br>/VoIP/免费', reject: '固话/VoIP->按配置过滤' },
  { type: 'filter', label: '黑名单/退订', desc: '平台级+客户级<br>黑名单 & 退订', reject: '命中->拦截' },
  { type: 'process', label: '号段匹配', desc: '提取国家+运营商<br>MCC/MNC', reject: null },
  { type: 'process', label: 'MNP 修正', desc: '查本地缓存表<br>命中则覆盖运营商', reject: null },
  { type: 'process', label: '通道分配', desc: '3级优先级匹配<br>SID绑定->路由表->兜底', reject: '无可用通道->失败' },
  { type: 'ok', label: '进入发送队列', desc: '通道调度引擎<br>接管后续发送', reject: null }
]

/* ======================================================================
   Tab 1: Segment — 号段归属表
   ====================================================================== */
const segLookup = ref('')
const segQuery = reactive({ country: '', operator: '', source: '', page: 1, size: 20 })
const segmentList = ref([])
const segmentTotal = ref(0)

const loadSegments = async () => {
  try {
    const res = await segmentPage({
      countryCode: segQuery.country || undefined,
      operator: segQuery.operator || undefined,
      source: segQuery.source || undefined,
      page: segQuery.page,
      size: segQuery.size
    })
    const data = res.data
    segmentList.value = (data.list || []).map(item => ({
      id: item.id,
      country: item.countryCode,
      countryCode: item.countryCode,
      mcc: '',
      mnc: '',
      operator: item.operator,
      prefix: item.prefix,
      numType: item.numberType,
      source: item.source,
      updatedAt: item.createdAt ? item.createdAt.substring(0, 10) : ''
    }))
    segmentTotal.value = data.total || 0
  } catch {
    ElMessage.error('加载号段数据失败')
  }
}

/* ---- Tab 1: Dialogs ---- */
const syncDialog = ref(false)
const syncing = ref(false)
const addSegmentDialog = ref(false)
const segmentForm = reactive({ country: '', countryCode: '', mcc: '', mnc: '', operator: '', numType: '移动', prefix: '', reason: '' })

const doSync = () => {
  syncing.value = true
  setTimeout(() => {
    syncing.value = false
    syncDialog.value = false
    ElMessage.success('同步完成，已更新至 v8.13.31')
  }, 2000)
}

const handleSaveSegment = async () => {
  try {
    await segmentSave({
      countryCode: segmentForm.countryCode || segmentForm.country,
      prefix: segmentForm.prefix,
      operator: segmentForm.operator,
      numberType: segmentForm.numType,
      source: '人工补充'
    })
    addSegmentDialog.value = false
    ElMessage.success('保存成功')
    loadSegments()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDeleteSegment = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该号段记录？', '提示', { type: 'warning' })
    await segmentDel(row.id)
    ElMessage.success('删除成功')
    loadSegments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

/* Pagination watcher for segment */
watch(() => segQuery.page, () => loadSegments())

/* ======================================================================
   Tab 2: MNP 缓存表
   ====================================================================== */
const mnpQuery = reactive({ phone: '', country: '', source: '', status: '', page: 1, size: 20 })
const mnpList = ref([])
const mnpTotal = ref(0)

const loadMnp = async () => {
  try {
    const res = await mnpCachePage({
      countryCode: mnpQuery.country || undefined,
      phoneNumber: mnpQuery.phone || undefined,
      page: mnpQuery.page,
      size: mnpQuery.size
    })
    const data = res.data
    mnpList.value = (data.list || []).map(item => ({
      id: item.id,
      phone: item.phoneNumber,
      country: item.countryCode,
      originalOp: item.originalOperator,
      actualOp: item.currentOperator,
      ported: item.ported,
      source: item.querySource,
      updatedAt: item.updatedAt ? item.updatedAt.substring(0, 10) : '',
      expireAt: item.expireAt ? item.expireAt.substring(0, 10) : '',
      status: item.ported ? '已验证' : '待验证'
    }))
    mnpTotal.value = data.total || 0
  } catch {
    ElMessage.error('加载 MNP 缓存失败')
  }
}

const mnpSourceTagType = (source) => {
  if (source === '客户反馈') return ''
  if (source === '运营商通知') return 'success'
  if (source === '发送失败分析') return 'warning'
  return 'info'
}

const handleClearExpiredMnp = async () => {
  try {
    await ElMessageBox.confirm('确认清理所有过期 MNP 缓存记录？', '提示', { type: 'warning' })
    await mnpClearExpired()
    ElMessage.success('清理完成')
    loadMnp()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('清理失败')
  }
}

/* ---- Tab 2: Dialogs ---- */
const addMnpDialog = ref(false)
const importMnpDialog = ref(false)
const mnpForm = reactive({ phone: '', originalOp: '', actualOp: '', source: '客户反馈', status: '待验证', remark: '' })

/* Pagination watcher for MNP */
watch(() => mnpQuery.page, () => loadMnp())

/* ======================================================================
   Tab 3: 默认路由表
   ====================================================================== */
const routeQuery = reactive({ country: '', operator: '', channel: '', page: 1, size: 20 })
const routeList = ref([])

const loadRoutes = async () => {
  try {
    const res = await defaultRouteList({
      countryCode: routeQuery.country || undefined
    })
    routeList.value = (res.data || []).map(item => ({
      id: item.id,
      country: item.countryCode,
      operator: item.operator,
      mccMnc: '',
      matchScope: '',
      targetChannel: item.channelId,
      channelNote: '',
      schedMode: '',
      smsAttribute: item.smsAttribute,
      priority: item.priority,
      status: item.isActive ? '启用' : '停用',
      updatedAt: ''
    }))
  } catch {
    ElMessage.error('加载路由表失败')
  }
}

const schedModeTagType = (mode) => {
  if (mode === '主备切换') return 'warning'
  if (mode === '直连') return 'info'
  if (mode === '加权分流') return 'danger'
  return 'info'
}

/* ---- Tab 3: Dialog ---- */
const addRouteDialog = ref(false)
const routeForm = reactive({ country: '', matchScope: 'exact', mcc: '', mnc: '', operator: '', channel: '', schedMode: '主备切换', status: '启用' })

const handleSaveRoute = async () => {
  try {
    await defaultRouteSave({
      countryCode: routeForm.country,
      operator: routeForm.operator,
      smsAttribute: '',
      channelId: routeForm.channel,
      priority: 10,
      isActive: routeForm.status === '启用'
    })
    addRouteDialog.value = false
    ElMessage.success('保存成功')
    loadRoutes()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleToggleRoute = async (row) => {
  try {
    const newActive = row.status !== '启用'
    await defaultRouteToggle(row.id, newActive)
    ElMessage.success(newActive ? '已启用' : '已停用')
    loadRoutes()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDeleteRoute = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该路由规则？', '提示', { type: 'warning' })
    await defaultRouteDel(row.id)
    ElMessage.success('删除成功')
    loadRoutes()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

/* ======================================================================
   Tab 4: Fallback — 兜底通道配置 (keep mock — no backend API)
   ====================================================================== */
const editGlobalFallbackDialog = ref(false)
const addCountryFallbackDialog = ref(false)
const globalFallbackChannel = ref('GLOBAL-FALLBACK-01')
const countryFallbackForm = reactive({ country: '', channel: '' })

const countryFallbackList = ref([
  { country: '泰国', channel: 'TH-兜底组', channelNote: '', schedMode: '主备切换', health: '健康（2/2）', healthDot: 'dot-green', healthColor: '#16a34a', triggerCount: '1,203' },
  { country: '印尼', channel: 'ID-主备组', channelNote: '（同路由表默认组）', schedMode: '主备切换', health: '健康（3/3）', healthDot: 'dot-green', healthColor: '#16a34a', triggerCount: '2,817' },
  { country: '新加坡', channel: 'SG-通用-01', channelNote: '', schedMode: '直连', health: '警告（1/2）', healthDot: 'dot-yellow', healthColor: '#d97706', triggerCount: '401' }
])

/* ======================================================================
   Tab 5: Filter — 号码过滤规则
   ====================================================================== */
const filterRulesData = ref([])

const loadFilterRules = async () => {
  try {
    const res = await filterRuleList({
      countryCode: undefined
    })
    filterRulesData.value = (res.data || []).map(item => ({
      id: item.id,
      name: item.ruleName,
      type: item.filterType,
      value: item.filterValue,
      action: item.action,
      targetChannelId: item.targetChannelId,
      countryCode: item.countryCode,
      isActive: item.isActive
    }))
  } catch {
    // silent — use local defaults
  }
}

const handleToggleFilterRule = async (rule) => {
  try {
    await filterRuleToggle(rule.id, !rule.isActive)
    ElMessage.success('状态已更新')
    loadFilterRules()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDeleteFilterRule = async (rule) => {
  try {
    await ElMessageBox.confirm('确认删除该过滤规则？', '提示', { type: 'warning' })
    await filterRuleDel(rule.id)
    ElMessage.success('删除成功')
    loadFilterRules()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

/* Local UI-only filter rules (number type toggles — no separate API) */
const filterRules = reactive([
  { label: '过滤固话号码', desc: '号码类型为 FIXED_LINE 时过滤', enabled: true },
  { label: '过滤 VoIP 号码', desc: '号码类型为 VOIP 时过滤', enabled: true },
  { label: '过滤付费号码', desc: '号码类型为 PREMIUM_RATE 时过滤', enabled: true },
  { label: '过滤免费号码', desc: '号码类型为 TOLL_FREE 时过滤', enabled: false },
  { label: '批次内去重', desc: '同一批次中的重复号码，保留第一条', enabled: true }
])

const optoutKeywords = ref(['TD', '退订', 'T', 'STOP', 'UNSUBSCRIBE', '取消订阅', 'QUIT'])
const addKeywordDialog = ref(false)
const newKeyword = ref('')

const addKeyword = () => {
  if (!newKeyword.value.trim()) { ElMessage.warning('请输入关键词'); return }
  optoutKeywords.value.push(newKeyword.value.trim())
  newKeyword.value = ''
  addKeywordDialog.value = false
  ElMessage.success('添加成功')
}

const removeKeyword = (kw) => {
  optoutKeywords.value = optoutKeywords.value.filter(k => k !== kw)
  ElMessage.success('已删除')
}

const filterStats = [
  { label: '格式错误', value: '1,203', color: '#e63946' },
  { label: '未知国家码', value: '87', color: '#e63946' },
  { label: '固话/VoIP', value: '342', color: '#d97706' },
  { label: '黑名单命中', value: '4,219', color: '#e63946' },
  { label: '退订名单', value: '1,841', color: '#d97706' },
  { label: '批次内去重', value: '623', color: '#6b7280' }
]

/* ======================================================================
   Tab switching — load data for the active tab
   ====================================================================== */
watch(activeTab, (tab) => {
  if (tab === 'segment') loadSegments()
  else if (tab === 'mnp') loadMnp()
  else if (tab === 'route') loadRoutes()
  else if (tab === 'filter') loadFilterRules()
})

/* ---- Mount: load initial tab ---- */
onMounted(() => {
  loadChannelAndGroups()
  loadSegments()
})
</script>

<style scoped>
.number-routing-page {
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

/* Section header */
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
}
.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}
.section-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 3px;
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
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}
.stat-sub {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
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

/* Flow visualization */
.flow-wrap {
  display: flex;
  align-items: flex-start;
  gap: 0;
  overflow-x: auto;
  padding-bottom: 24px;
}
.flow-step {
  flex-shrink: 0;
  width: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.flow-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  position: relative;
}
.flow-circle.ok { background: #16a34a; }
.flow-circle.filter { background: #e63946; }
.flow-circle.process { background: #4361ee; }
.flow-step-label {
  font-size: 11px;
  font-weight: 600;
  color: #374151;
  margin-top: 8px;
  line-height: 1.4;
}
.flow-step-desc {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 3px;
  line-height: 1.4;
}
.flow-arrow {
  display: flex;
  align-items: center;
  padding: 0 2px;
  margin-top: 14px;
  color: #d1d5db;
  font-size: 18px;
  flex-shrink: 0;
}
.flow-reject {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 9px;
  color: #e63946;
  white-space: nowrap;
  background: #fff;
  border: 1px solid #fef2f2;
  border-radius: 4px;
  padding: 1px 5px;
}

/* Route tiers */
.route-tiers {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.route-tier {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.route-tier.t1 { background: #eef1fd; border-color: #c7d2fe; }
.route-tier.t2 { background: #f0fdf4; border-color: #bbf7d0; }
.route-tier.t3 { background: #fffbeb; border-color: #fde68a; }
.route-tier.tfail { background: #fff5f5; border-color: #fecaca; }
.rt-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.rt-label {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  min-width: 200px;
}
.rt-desc {
  font-size: 12px;
  color: #6b7280;
  flex: 1;
}

/* Status dot */
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 5px;
}
.dot-green { background: #16a34a; }
.dot-yellow { background: #d97706; }
.dot-red { background: #e63946; }
.dot-gray { background: #9ca3af; }
</style>
