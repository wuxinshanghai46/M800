<template>
  <div class="system-config-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-title">系统配置</div>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- ========== Tab1: 基础设置 ========== -->
      <el-tab-pane label="基础设置" name="basic">
        <!-- 平台基础信息 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">平台基础信息</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="平台名称" required>
                <el-input v-model="basicForm.platformName" />
              </el-form-item>
              <el-form-item label="平台域名" required>
                <el-input v-model="basicForm.platformDomain" />
              </el-form-item>
              <el-form-item label="默认时区">
                <el-select v-model="basicForm.timezone" style="width: 100%">
                  <el-option label="Asia/Shanghai (UTC+8)" value="Asia/Shanghai" />
                  <el-option label="Asia/Hong_Kong (UTC+8)" value="Asia/Hong_Kong" />
                  <el-option label="UTC" value="UTC" />
                  <el-option label="America/New_York (UTC-5)" value="America/New_York" />
                </el-select>
              </el-form-item>
              <el-form-item label="默认语言">
                <el-select v-model="basicForm.language" style="width: 100%">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en" />
                  <el-option label="繁體中文" value="zh-TW" />
                </el-select>
              </el-form-item>
              <el-form-item label="平台简介" class="full-width">
                <el-input v-model="basicForm.description" type="textarea" :rows="2" />
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <!-- 安全设置 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">安全设置</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="登录失败锁定次数">
                <div class="form-row">
                  <el-input-number v-model="securityForm.lockAttempts" :min="1" :max="20" controls-position="right" style="flex: 1" />
                  <span class="form-unit">次后锁定账户</span>
                </div>
              </el-form-item>
              <el-form-item label="账户锁定时长">
                <div class="form-row">
                  <el-input-number v-model="securityForm.lockDuration" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">分钟</span>
                </div>
              </el-form-item>
              <el-form-item label="密码有效期">
                <div class="form-row">
                  <el-input-number v-model="securityForm.passwordExpiry" :min="0" controls-position="right" style="flex: 1" />
                  <span class="form-unit">天（0 = 永不过期）</span>
                </div>
              </el-form-item>
              <el-form-item label="Session 超时">
                <div class="form-row">
                  <el-input-number v-model="securityForm.sessionTimeout" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">分钟</span>
                </div>
              </el-form-item>
              <el-form-item label="强制 MFA 登录">
                <div class="switch-wrap">
                  <el-switch v-model="securityForm.forceMfa" />
                  <span class="switch-label-text">{{ securityForm.forceMfa ? '已开启' : '关闭（建议开启）' }}</span>
                </div>
              </el-form-item>
              <el-form-item label="管理员 IP 白名单">
                <el-input v-model="securityForm.ipWhitelist" placeholder="多个 IP 用逗号分隔，留空不限制" />
                <div class="form-hint">示例：192.168.1.0/24, 203.0.113.5</div>
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <!-- 通知预警阈值 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">通知预警阈值</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="余额不足预警阈值">
                <div class="form-row">
                  <el-select v-model="alertForm.currency" style="width: 90px">
                    <el-option label="USD" value="USD" />
                    <el-option label="HKD" value="HKD" />
                    <el-option label="CNY" value="CNY" />
                  </el-select>
                  <el-input-number v-model="alertForm.balanceThreshold" :min="0" controls-position="right" style="flex: 1" />
                </div>
              </el-form-item>
              <el-form-item label="SID 到期预警天数">
                <div class="form-row">
                  <el-input-number v-model="alertForm.sidExpiryDays" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">天前提醒</span>
                </div>
              </el-form-item>
              <el-form-item label="通道成功率告警阈值">
                <div class="form-row">
                  <el-input-number v-model="alertForm.channelSuccessRate" :min="0" :max="100" controls-position="right" style="flex: 1" />
                  <span class="form-unit">% 以下告警</span>
                </div>
              </el-form-item>
              <el-form-item label="投诉率阈值">
                <div class="form-row">
                  <el-input v-model="alertForm.complaintRate" style="flex: 1" />
                  <span class="form-unit">% 超过则告警</span>
                </div>
              </el-form-item>
              <el-form-item label="告警通知接收邮箱">
                <el-input v-model="alertForm.alertEmail" placeholder="ops@boruiworld.com" />
              </el-form-item>
              <el-form-item label="审核超时自动处理">
                <div class="form-row">
                  <el-input-number v-model="alertForm.auditTimeout" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">分钟后自动拒绝</span>
                </div>
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <div class="form-actions">
          <el-button @click="handleReset('基础设置')">重置</el-button>
          <el-button type="primary" @click="handleSave('基础设置已保存')">保存设置</el-button>
        </div>
      </el-tab-pane>

      <!-- ========== Tab2: 短信参数 ========== -->
      <el-tab-pane label="短信参数" name="sms">
        <!-- 短信编码与分段 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">短信编码与分段</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="默认编码方式">
                <el-select v-model="smsForm.defaultEncoding" style="width: 100%">
                  <el-option label="自动检测（推荐）" value="auto" />
                  <el-option label="GSM7（仅 ASCII）" value="gsm7" />
                  <el-option label="UCS-2（Unicode）" value="ucs2" />
                </el-select>
                <div class="form-hint">自动检测：含中文/特殊字符时切换为 UCS-2</div>
              </el-form-item>
              <el-form-item label="强制 GSM7 编码">
                <div class="switch-wrap">
                  <el-switch v-model="smsForm.forceGsm7" />
                  <span class="switch-label-text">{{ smsForm.forceGsm7 ? '已开启' : '关闭' }}</span>
                </div>
                <div class="form-hint">开启后特殊字符将被替换为近似字符</div>
              </el-form-item>
              <el-form-item label="GSM7 单段最大字符数">
                <el-input-number v-model="smsForm.gsm7SingleMax" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="GSM7 多段每段字符数">
                <el-input-number v-model="smsForm.gsm7MultiMax" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="UCS-2 单段最大字符数">
                <el-input-number v-model="smsForm.ucs2SingleMax" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="UCS-2 多段每段字符数">
                <el-input-number v-model="smsForm.ucs2MultiMax" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="最大分段数">
                <el-input-number v-model="smsForm.maxSegments" :min="1" controls-position="right" style="width: 100%" />
                <div class="form-hint">超过此段数的消息将被拒绝</div>
              </el-form-item>
              <el-form-item label="长短信拼接方式">
                <el-select v-model="smsForm.concatMethod" style="width: 100%">
                  <el-option label="UDH（推荐）" value="udh" />
                  <el-option label="GSM TP-SAR" value="tp-sar" />
                </el-select>
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <!-- DLR 与重试策略 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">DLR 与重试策略</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="DLR 超时时间">
                <div class="form-row">
                  <el-input-number v-model="dlrForm.dlrTimeout" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">小时</span>
                </div>
                <div class="form-hint">超时后标记为 EXPIRED</div>
              </el-form-item>
              <el-form-item label="虚假 DLR 处理">
                <el-select v-model="dlrForm.fakeDlrHandling" style="width: 100%">
                  <el-option label="标记并告警" value="mark_alert" />
                  <el-option label="仅标记" value="mark" />
                  <el-option label="忽略" value="ignore" />
                </el-select>
              </el-form-item>
              <el-form-item label="发送失败最大重试次数">
                <el-input-number v-model="dlrForm.maxRetries" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="重试间隔">
                <div class="form-row">
                  <el-input-number v-model="dlrForm.retryInterval" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">分钟（递增）</span>
                </div>
                <div class="form-hint">间隔 x 重试次数</div>
              </el-form-item>
              <el-form-item label="回调推送重试次数">
                <el-input-number v-model="dlrForm.callbackRetries" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="回调推送超时">
                <div class="form-row">
                  <el-input-number v-model="dlrForm.callbackTimeout" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">秒</span>
                </div>
              </el-form-item>
              <el-form-item label="回调重试间隔策略">
                <el-select v-model="dlrForm.callbackRetryStrategy" style="width: 100%">
                  <el-option label="指数退避（1s, 2s, 4s, 8s, 16s）" value="exponential" />
                  <el-option label="固定间隔（5s）" value="fixed" />
                  <el-option label="线性递增（5s, 10s, 15s...）" value="linear" />
                </el-select>
              </el-form-item>
              <el-form-item label="消息默认有效期">
                <div class="form-row">
                  <el-input-number v-model="dlrForm.msgValidity" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">秒（默认 24h）</span>
                </div>
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <!-- 发送限制 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">发送限制</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="全局每秒发送上限（TPS）">
                <el-input-number v-model="sendLimitForm.globalTps" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="单客户默认每秒上限">
                <el-input-number v-model="sendLimitForm.customerTps" :min="1" controls-position="right" style="width: 100%" />
                <div class="form-hint">客户级别可单独覆盖</div>
              </el-form-item>
              <el-form-item label="单号码每分钟上限">
                <el-input-number v-model="sendLimitForm.numberPerMinute" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="单号码每天上限">
                <el-input-number v-model="sendLimitForm.numberPerDay" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="队列超时">
                <div class="form-row">
                  <el-input-number v-model="sendLimitForm.queueTimeout" :min="1" controls-position="right" style="flex: 1" />
                  <span class="form-unit">秒（默认 1h）</span>
                </div>
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <!-- 数据保留策略 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">数据保留策略</span>
          </template>
          <div class="param-grid">
            <div class="param-card" v-for="item in retentionItems" :key="item.key">
              <div class="param-name">{{ item.label }}</div>
              <el-input-number v-model="item.value" :min="1" controls-position="right" style="width: 100%" />
              <div class="param-unit">{{ item.unit }}</div>
            </div>
          </div>
        </el-card>

        <div class="form-actions">
          <el-button @click="handleReset('短信参数')">重置</el-button>
          <el-button type="primary" @click="handleSave('短信参数已保存')">保存参数</el-button>
        </div>
      </el-tab-pane>

      <!-- ========== Tab3: 邮件配置 ========== -->
      <el-tab-pane label="邮件配置" name="email">
        <!-- SMTP 邮件服务器 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-title-row">
              <span class="card-title-text">SMTP 邮件服务器</span>
              <el-tag type="success" size="small">已连接</el-tag>
            </div>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="SMTP 主机" required>
                <el-input v-model="smtpForm.host" />
              </el-form-item>
              <el-form-item label="端口" required>
                <el-select v-model="smtpForm.port" style="width: 100%">
                  <el-option label="25" value="25" />
                  <el-option label="465 (SSL)" value="465" />
                  <el-option label="587 (TLS)" value="587" />
                </el-select>
              </el-form-item>
              <el-form-item label="账户名" required>
                <el-input v-model="smtpForm.username" />
              </el-form-item>
              <el-form-item label="密码" required>
                <el-input v-model="smtpForm.password" type="password" show-password />
              </el-form-item>
              <el-form-item label="发件人名称">
                <el-input v-model="smtpForm.senderName" />
              </el-form-item>
              <el-form-item label="加密方式">
                <el-select v-model="smtpForm.encryption" style="width: 100%">
                  <el-option label="SSL/TLS" value="ssl" />
                  <el-option label="STARTTLS" value="starttls" />
                  <el-option label="无" value="none" />
                </el-select>
              </el-form-item>
            </div>
          </el-form>
          <div class="smtp-actions">
            <el-button @click="testSmtp">测试连接</el-button>
            <el-button type="primary" @click="saveSmtpConfig">保存配置</el-button>
          </div>
        </el-card>

        <!-- 邮件通知开关 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">邮件通知开关</span>
          </template>
          <div class="notify-grid">
            <div class="switch-wrap" v-for="item in notifyItems" :key="item.key">
              <el-switch v-model="item.value" />
              <span class="switch-label-text">{{ item.label }}</span>
            </div>
          </div>
          <div class="form-actions" style="margin-top: 16px">
            <el-button type="primary" @click="saveNotifyConfig">保存通知设置</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ========== Tab4: 用户与角色 ========== -->
      <el-tab-pane label="用户与角色" name="role">
        <!-- 角色列表 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-title-row">
              <span class="card-title-text">角色列表</span>
              <el-button type="primary" size="small" @click="roleDialogVisible = true">+ 新建角色</el-button>
            </div>
          </template>
          <el-table :data="roleList" :header-cell-style="headerCellStyle" style="width: 100%">
            <el-table-column prop="name" label="角色名称" min-width="120">
              <template #default="{ row }">
                <strong>{{ row.name }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column prop="memberCount" label="成员数" width="80" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === '系统内置' ? 'danger' : 'info'" size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="120" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <template v-if="row.type === '系统内置'">
                  <span style="color: #999; font-size: 12px">不可编辑</span>
                </template>
                <template v-else>
                  <el-button link type="primary" size="small" @click="openPermDialog(row)">编辑权限</el-button>
                  <el-button link type="primary" size="small">成员({{ row.memberCount }})</el-button>
                  <el-button link type="danger" size="small" @click="confirmDelete('角色', row.name)">删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 管理员账户 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-title-row">
              <span class="card-title-text">管理员账户</span>
              <el-button type="primary" size="small" @click="adminDialogVisible = true">+ 新增账户</el-button>
            </div>
          </template>
          <el-table :data="adminList" :header-cell-style="headerCellStyle" style="width: 100%">
            <el-table-column prop="username" label="用户名" min-width="100">
              <template #default="{ row }">
                <strong>{{ row.username }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="role" label="角色" width="110">
              <template #default="{ row }">
                <el-tag :type="getRoleTagType(row.role)" size="small">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLogin" label="最后登录" width="150" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '正常' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <template v-if="row.username === 'admin'">
                  <span style="color: #999; font-size: 12px">不可编辑</span>
                </template>
                <template v-else>
                  <el-button link type="primary" size="small">编辑</el-button>
                  <el-button link type="primary" size="small" @click="resetUserPwd(row)">重置密码</el-button>
                  <el-button v-if="row.status === '正常'" link type="danger" size="small" @click="toggleUser(row)">禁用</el-button>
                  <el-button v-else link size="small" style="color: #52c41a" @click="toggleUser(row)">启用</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ========== Tab5: API 限流 ========== -->
      <el-tab-pane label="API 限流" name="apirate">
        <!-- 全局限流概览 -->
        <div class="info-row">
          <div class="info-cell" v-for="item in apiOverview" :key="item.label">
            <div class="info-cell-label">{{ item.label }}</div>
            <div class="info-cell-value">{{ item.value }}</div>
          </div>
        </div>

        <!-- 全局限流规则 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">全局限流规则</span>
          </template>
          <el-form label-position="top" class="form-grid">
            <div class="form-grid-inner">
              <el-form-item label="HTTP API 全局 TPS">
                <el-input-number v-model="apiLimitForm.httpTps" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="单 IP 每分钟上限">
                <el-input-number v-model="apiLimitForm.ipPerMinute" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="查询接口每分钟上限">
                <el-input-number v-model="apiLimitForm.queryPerMinute" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="限流后返回状态码">
                <el-select v-model="apiLimitForm.rateLimitCode" style="width: 100%">
                  <el-option label="429 Too Many Requests" value="429" />
                  <el-option label="503 Service Unavailable" value="503" />
                </el-select>
              </el-form-item>
              <el-form-item label="SMPP 最大并发连接">
                <el-input-number v-model="apiLimitForm.smppMaxConn" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="单客户 SMPP 最大连接">
                <el-input-number v-model="apiLimitForm.smppPerCustomer" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="批量发送最大号码数">
                <el-input-number v-model="apiLimitForm.batchMax" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="CSV 文件最大行数">
                <el-input-number v-model="apiLimitForm.csvMaxRows" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
            </div>
          </el-form>
        </el-card>

        <!-- 客户级别限流覆盖 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-title-row">
              <span class="card-title-text">客户级别限流覆盖</span>
              <el-button type="primary" size="small" @click="rateLimitDialogVisible = true">+ 添加客户规则</el-button>
            </div>
          </template>
          <el-table :data="customerRateLimits" :header-cell-style="headerCellStyle" style="width: 100%">
            <el-table-column prop="customerName" label="客户名称" min-width="150" />
            <el-table-column prop="httpTps" label="HTTP TPS" width="100" />
            <el-table-column prop="smppConn" label="SMPP 连接数" width="120" />
            <el-table-column prop="batchLimit" label="批量上限" width="100" />
            <el-table-column prop="remark" label="备注" min-width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" size="small">编辑</el-button>
                <el-button link type="danger" size="small" @click="confirmDelete('限流规则', row.customerName)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <div class="form-actions">
          <el-button @click="handleReset('API 限流配置')">重置</el-button>
          <el-button type="primary" @click="handleSave('API 限流配置已保存')">保存设置</el-button>
        </div>
      </el-tab-pane>

      <!-- ========== Tab6: 系统信息 ========== -->
      <el-tab-pane label="系统信息" name="sysinfo">
        <el-card shadow="never" class="config-card">
          <template #header>
            <span class="card-title-text">系统运行信息</span>
          </template>
          <div class="sys-info-grid">
            <div class="info-item" v-for="item in sysInfoItems" :key="item.label">
              <span class="info-label">{{ item.label }}</span>
              <span class="info-value">
                <el-tag v-if="item.tag" :type="item.tagType" size="small" style="margin-right: 6px">{{ item.tag }}</el-tag>
                {{ item.value }}
              </span>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 弹窗区域 ===== -->

    <!-- 权限编辑弹窗 -->
    <el-dialog v-model="permDialogVisible" :title="`编辑角色权限 - ${currentRole}`" width="740px" destroy-on-close>
      <div class="perm-section" v-for="section in permissionSections" :key="section.title">
        <div class="perm-section-title">{{ section.title }}</div>
        <div class="perm-grid">
          <el-checkbox v-for="perm in section.items" :key="perm.label" v-model="perm.checked">{{ perm.label }}</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存权限</el-button>
      </template>
    </el-dialog>

    <!-- 新建角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="新建角色" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="角色名称" required>
          <el-input v-model="newRoleForm.name" placeholder="如：客服专员" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="newRoleForm.description" type="textarea" placeholder="简要描述该角色的职责范围" />
        </el-form-item>
        <el-form-item label="复制权限自">
          <el-select v-model="newRoleForm.copyFrom" style="width: 100%" placeholder="- 从空白开始 -" clearable>
            <el-option label="运营主管" value="运营主管" />
            <el-option label="运营专员" value="运营专员" />
            <el-option label="财务人员" value="财务人员" />
            <el-option label="技术支持" value="技术支持" />
          </el-select>
          <div class="form-hint">创建后可在权限编辑中细调</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createRole">创建并设置权限</el-button>
      </template>
    </el-dialog>

    <!-- 新增管理员弹窗 -->
    <el-dialog v-model="adminDialogVisible" title="新增管理员账户" width="560px" destroy-on-close>
      <el-form label-position="top" class="form-grid">
        <div class="form-grid-inner">
          <el-form-item label="用户名" required>
            <el-input v-model="newAdminForm.username" placeholder="仅限字母、数字、下划线" />
          </el-form-item>
          <el-form-item label="姓名" required>
            <el-input v-model="newAdminForm.realName" placeholder="真实姓名" />
          </el-form-item>
          <el-form-item label="邮箱" required>
            <el-input v-model="newAdminForm.email" type="email" placeholder="接收通知邮件" />
          </el-form-item>
          <el-form-item label="分配角色" required>
            <el-select v-model="newAdminForm.role" style="width: 100%" placeholder="请选择">
              <el-option label="运营主管" value="运营主管" />
              <el-option label="运营专员" value="运营专员" />
              <el-option label="财务人员" value="财务人员" />
              <el-option label="技术支持" value="技术支持" />
              <el-option label="只读审计" value="只读审计" />
            </el-select>
          </el-form-item>
          <el-form-item label="初始密码" class="full-width">
            <el-input v-model="newAdminForm.password" type="password" show-password placeholder="留空则发送邮件激活链接" />
            <div class="form-hint">建议留空，系统将发送激活邮件，首次登录强制改密</div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createAdmin">创建账户</el-button>
      </template>
    </el-dialog>

    <!-- 客户限流规则弹窗 -->
    <el-dialog v-model="rateLimitDialogVisible" title="添加客户限流规则" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="选择客户" required>
          <el-select v-model="newRateLimitForm.customer" style="width: 100%" placeholder="请选择客户">
            <el-option label="ShopeePay TH" value="ShopeePay TH" />
            <el-option label="Lazada SG" value="Lazada SG" />
            <el-option label="Grab MY" value="Grab MY" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <div class="form-grid-inner">
            <el-form-item label="HTTP TPS">
              <el-input-number v-model="newRateLimitForm.httpTps" controls-position="right" style="width: 100%" placeholder="默认 100" />
            </el-form-item>
            <el-form-item label="SMPP 连接数">
              <el-input-number v-model="newRateLimitForm.smppConn" controls-position="right" style="width: 100%" placeholder="默认 10" />
            </el-form-item>
            <el-form-item label="批量上限">
              <el-input-number v-model="newRateLimitForm.batchLimit" controls-position="right" style="width: 100%" placeholder="默认 10000" />
            </el-form-item>
          </div>
        </div>
        <el-form-item label="备注">
          <el-input v-model="newRateLimitForm.remark" placeholder="如：VIP 客户、高频场景等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rateLimitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addCustomerRateLimit">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  configGet, configBatchUpdate,
  roleList as apiRoleList, roleSave, roleDel,
  userPage, userSave, userToggle, userResetPwd
} from '../api'

// Active tab
const activeTab = ref('basic')

// Table header style
const headerCellStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

// ==================== Tab1: 基础设置 ====================
const basicForm = reactive({
  platformName: '',
  platformDomain: '',
  timezone: 'Asia/Shanghai',
  language: 'zh-CN',
  description: ''
})

const securityForm = reactive({
  lockAttempts: 5,
  lockDuration: 30,
  passwordExpiry: 90,
  sessionTimeout: 120,
  forceMfa: false,
  ipWhitelist: ''
})

const alertForm = reactive({
  currency: 'USD',
  balanceThreshold: 500,
  sidExpiryDays: 30,
  channelSuccessRate: 80,
  complaintRate: '0.1',
  alertEmail: '',
  auditTimeout: 30
})

async function loadBasicConfig() {
  try {
    const res = await configGet('platform')
    const d = res.data || {}
    basicForm.platformName = d.platform_name || ''
    basicForm.platformDomain = d.platform_domain || ''
    basicForm.timezone = d.timezone || 'Asia/Shanghai'
    basicForm.language = d.language || 'zh-CN'
    basicForm.description = d.description || ''
    securityForm.lockAttempts = Number(d.lock_attempts) || 5
    securityForm.lockDuration = Number(d.lock_duration) || 30
    securityForm.passwordExpiry = Number(d.password_expiry) || 90
    securityForm.sessionTimeout = Number(d.session_timeout) || 120
    securityForm.forceMfa = d.force_mfa === 'true'
    securityForm.ipWhitelist = d.ip_whitelist || ''
    alertForm.currency = d.alert_currency || 'USD'
    alertForm.balanceThreshold = Number(d.balance_threshold) || 500
    alertForm.sidExpiryDays = Number(d.sid_expiry_days) || 30
    alertForm.channelSuccessRate = Number(d.channel_success_rate) || 80
    alertForm.complaintRate = d.complaint_rate || '0.1'
    alertForm.alertEmail = d.alarm_email || ''
    alertForm.auditTimeout = Number(d.audit_timeout) || 30
  } catch { /* ignore */ }
}

async function saveBasicConfig() {
  try {
    await configBatchUpdate('platform', {
      platform_name: basicForm.platformName,
      platform_domain: basicForm.platformDomain,
      timezone: basicForm.timezone,
      language: basicForm.language,
      description: basicForm.description,
      lock_attempts: String(securityForm.lockAttempts),
      lock_duration: String(securityForm.lockDuration),
      password_expiry: String(securityForm.passwordExpiry),
      session_timeout: String(securityForm.sessionTimeout),
      force_mfa: String(securityForm.forceMfa),
      ip_whitelist: securityForm.ipWhitelist,
      alert_currency: alertForm.currency,
      balance_threshold: String(alertForm.balanceThreshold),
      sid_expiry_days: String(alertForm.sidExpiryDays),
      channel_success_rate: String(alertForm.channelSuccessRate),
      complaint_rate: alertForm.complaintRate,
      alarm_email: alertForm.alertEmail,
      audit_timeout: String(alertForm.auditTimeout)
    })
    ElMessage.success('基础设置已保存')
  } catch { ElMessage.error('保存失败') }
}

// ==================== Tab2: 短信参数 ====================
const smsForm = reactive({
  defaultEncoding: 'auto',
  forceGsm7: false,
  gsm7SingleMax: 160,
  gsm7MultiMax: 153,
  ucs2SingleMax: 70,
  ucs2MultiMax: 67,
  maxSegments: 6,
  concatMethod: 'udh'
})

const dlrForm = reactive({
  dlrTimeout: 24,
  fakeDlrHandling: 'mark_alert',
  maxRetries: 3,
  retryInterval: 5,
  callbackRetries: 5,
  callbackTimeout: 10,
  callbackRetryStrategy: 'exponential',
  msgValidity: 86400
})

const sendLimitForm = reactive({
  globalTps: 1000,
  customerTps: 100,
  numberPerMinute: 5,
  numberPerDay: 20,
  queueTimeout: 3600
})

const retentionItems = reactive([
  { key: 'sendLog', label: '发送记录保留', value: 90, unit: '单位：天' },
  { key: 'smsContent', label: '短信内容保留', value: 30, unit: '单位：天' },
  { key: 'auditLog', label: '审计日志保留', value: 365, unit: '单位：天' },
  { key: 'statsData', label: '统计数据保留', value: 730, unit: '单位：天（默认 2 年）' }
])

async function loadSmsConfig() {
  try {
    const res = await configGet('sms')
    const d = res.data || {}
    smsForm.defaultEncoding = d.default_encoding || 'auto'
    smsForm.forceGsm7 = d.force_gsm7 === 'true'
    smsForm.gsm7SingleMax = Number(d.gsm7_single_max) || 160
    smsForm.gsm7MultiMax = Number(d.gsm7_multi_max) || 153
    smsForm.ucs2SingleMax = Number(d.ucs2_single_max) || 70
    smsForm.ucs2MultiMax = Number(d.ucs2_multi_max) || 67
    smsForm.maxSegments = Number(d.max_segments) || 6
    smsForm.concatMethod = d.concat_method || 'udh'
    dlrForm.dlrTimeout = Number(d.dlr_timeout) || 24
    dlrForm.fakeDlrHandling = d.fake_dlr_handling || 'mark_alert'
    dlrForm.maxRetries = Number(d.max_retry) || 3
    dlrForm.retryInterval = Number(d.retry_interval) || 5
    dlrForm.callbackRetries = Number(d.callback_retries) || 5
    dlrForm.callbackTimeout = Number(d.callback_timeout) || 10
    dlrForm.callbackRetryStrategy = d.callback_retry_strategy || 'exponential'
    dlrForm.msgValidity = Number(d.msg_validity) || 86400
    sendLimitForm.globalTps = Number(d.max_tps) || 1000
    sendLimitForm.customerTps = Number(d.customer_tps) || 100
    sendLimitForm.numberPerMinute = Number(d.number_per_minute) || 5
    sendLimitForm.numberPerDay = Number(d.number_per_day) || 20
    sendLimitForm.queueTimeout = Number(d.queue_timeout) || 3600
    const retMap = { sendLog: 'log_retention', smsContent: 'content_retention', auditLog: 'audit_retention', statsData: 'data_retention' }
    retentionItems.forEach(item => {
      if (d[retMap[item.key]]) item.value = Number(d[retMap[item.key]])
    })
  } catch { /* ignore */ }
}

async function saveSmsConfig() {
  const retMap = { sendLog: 'log_retention', smsContent: 'content_retention', auditLog: 'audit_retention', statsData: 'data_retention' }
  const retObj = {}
  retentionItems.forEach(item => { retObj[retMap[item.key]] = String(item.value) })
  try {
    await configBatchUpdate('sms', {
      default_encoding: smsForm.defaultEncoding,
      force_gsm7: String(smsForm.forceGsm7),
      gsm7_single_max: String(smsForm.gsm7SingleMax),
      gsm7_multi_max: String(smsForm.gsm7MultiMax),
      ucs2_single_max: String(smsForm.ucs2SingleMax),
      ucs2_multi_max: String(smsForm.ucs2MultiMax),
      max_segments: String(smsForm.maxSegments),
      concat_method: smsForm.concatMethod,
      dlr_timeout: String(dlrForm.dlrTimeout),
      fake_dlr_handling: dlrForm.fakeDlrHandling,
      max_retry: String(dlrForm.maxRetries),
      retry_interval: String(dlrForm.retryInterval),
      callback_retries: String(dlrForm.callbackRetries),
      callback_timeout: String(dlrForm.callbackTimeout),
      callback_retry_strategy: dlrForm.callbackRetryStrategy,
      msg_validity: String(dlrForm.msgValidity),
      max_tps: String(sendLimitForm.globalTps),
      customer_tps: String(sendLimitForm.customerTps),
      number_per_minute: String(sendLimitForm.numberPerMinute),
      number_per_day: String(sendLimitForm.numberPerDay),
      queue_timeout: String(sendLimitForm.queueTimeout),
      ...retObj
    })
    ElMessage.success('短信参数已保存')
  } catch { ElMessage.error('保存失败') }
}

// ==================== Tab3: 邮件配置 ====================
const smtpForm = reactive({
  host: '',
  port: '465',
  username: '',
  password: '',
  senderName: '',
  encryption: 'ssl'
})

const notifyItems = reactive([
  { key: 'autoBill', label: '账单自动发送', value: false },
  { key: 'lowBalance', label: '余额不足通知', value: false },
  { key: 'sidExpiry', label: 'SID 到期预警', value: false },
  { key: 'channelAlert', label: '通道异常告警', value: false },
  { key: 'newCustomer', label: '新客户注册通知', value: false },
  { key: 'resetPassword', label: '密码重置邮件', value: false },
  { key: 'riskBlock', label: '风控拦截通知', value: false },
  { key: 'dailyReport', label: '每日运营报表', value: false }
])

async function loadNotificationConfig() {
  try {
    const res = await configGet('notification')
    const d = res.data || {}
    smtpForm.host = d.email_smtp || ''
    smtpForm.port = d.email_port || '465'
    smtpForm.username = d.email_sender || ''
    smtpForm.password = d.email_password || ''
    smtpForm.senderName = d.sender_name || ''
    smtpForm.encryption = d.encryption || 'ssl'
    // Notification switches
    notifyItems.forEach(item => {
      if (d['notify_' + item.key] !== undefined) item.value = d['notify_' + item.key] === 'true'
    })
  } catch { /* ignore */ }
}

async function saveSmtpConfig() {
  try {
    const notifyObj = {}
    notifyItems.forEach(item => { notifyObj['notify_' + item.key] = String(item.value) })
    await configBatchUpdate('notification', {
      email_smtp: smtpForm.host,
      email_port: smtpForm.port,
      email_sender: smtpForm.username,
      email_password: smtpForm.password,
      sender_name: smtpForm.senderName,
      encryption: smtpForm.encryption,
      ...notifyObj
    })
    ElMessage.success('SMTP 配置已保存')
  } catch { ElMessage.error('保存失败') }
}

async function saveNotifyConfig() {
  try {
    const notifyObj = {}
    notifyItems.forEach(item => { notifyObj['notify_' + item.key] = String(item.value) })
    await configBatchUpdate('notification', notifyObj)
    ElMessage.success('通知设置已保存')
  } catch { ElMessage.error('保存失败') }
}

// ==================== Tab4: 用户与角色 ====================
const roleList = ref([])
const adminList = ref([])

function getRoleTagType(role) {
  const map = { '超级管理员': 'danger', '运营主管': '', '运营专员': '', '财务人员': 'warning', '技术支持': '' }
  return map[role] ?? 'info'
}

async function loadRoles() {
  try {
    const res = await apiRoleList()
    roleList.value = (res.data || []).map(r => ({
      id: r.id,
      name: r.roleName,
      code: r.roleCode,
      description: r.description || '',
      memberCount: r.memberCount || 0,
      type: r.roleCode === 'super_admin' ? '系统内置' : '自定义',
      createdAt: r.createdAt ? r.createdAt.substring(0, 10) : '-',
      permissions: r.permissions || '',
      isActive: r.isActive
    }))
  } catch { /* ignore */ }
}

async function loadUsers() {
  try {
    const res = await userPage({ page: 1, size: 100 })
    const data = res.data || {}
    adminList.value = (data.list || []).map(u => ({
      id: u.id,
      username: u.username,
      realName: u.realName || '',
      email: u.email || '',
      role: u.roleName || '',
      roleId: u.roleId,
      lastLogin: u.lastLoginAt || '-',
      status: u.isActive ? '正常' : '已禁用',
      isActive: u.isActive
    }))
  } catch { /* ignore */ }
}

// Permission dialog
const permDialogVisible = ref(false)
const currentRole = ref('')
const currentRoleId = ref(null)
const permissionSections = reactive([
  {
    title: '客户管理',
    items: [
      { label: '查看客户列表', checked: false },
      { label: '新建客户', checked: false },
      { label: '编辑客户信息', checked: false },
      { label: '删除/销户', checked: false },
      { label: '冻结/解冻', checked: false },
      { label: '查看 API 凭证', checked: false },
      { label: '重置 API Key', checked: false },
      { label: '管理客户报价', checked: false }
    ]
  },
  {
    title: '资源管理',
    items: [
      { label: '查看通道', checked: false },
      { label: '新增/编辑通道', checked: false },
      { label: '查看供应商', checked: false },
      { label: '管理供应商', checked: false },
      { label: '查看 SID', checked: false },
      { label: '管理 SID', checked: false }
    ]
  },
  {
    title: '发送管理',
    items: [
      { label: '查看发送记录', checked: false },
      { label: '导出发送记录', checked: false },
      { label: '查看 MO 上行', checked: false },
      { label: '管理定时任务', checked: false }
    ]
  },
  {
    title: '财务结算 / 统计分析',
    items: [
      { label: '查看利润报表', checked: false },
      { label: '导出利润报表', checked: false },
      { label: '查看账单', checked: false },
      { label: '发送账单', checked: false },
      { label: '查看发送统计', checked: false },
      { label: '查看质量分析', checked: false }
    ]
  },
  {
    title: '风控与运营策略',
    items: [
      { label: '查看黑名单', checked: false },
      { label: '管理黑名单', checked: false },
      { label: '查看拦截日志', checked: false },
      { label: '修改频次规则', checked: false },
      { label: '管理运营策略', checked: false },
      { label: '审核工单处理', checked: false }
    ]
  },
  {
    title: '运维中心',
    items: [
      { label: '查看监控告警', checked: false },
      { label: '管理告警规则', checked: false },
      { label: '查看号码路由', checked: false },
      { label: '管理路由规则', checked: false }
    ]
  },
  {
    title: '系统配置',
    items: [
      { label: '修改系统配置', checked: false },
      { label: '管理角色权限', checked: false },
      { label: '查看审计日志', checked: false },
      { label: '管理 API 限流', checked: false }
    ]
  }
])

function openPermDialog(row) {
  currentRole.value = row.name
  currentRoleId.value = row.id
  // Parse permissions JSON and apply to checkboxes
  let permSet = new Set()
  try {
    const perms = JSON.parse(row.permissions || '[]')
    permSet = new Set(Array.isArray(perms) ? perms : [])
  } catch { /* ignore */ }
  permissionSections.forEach(section => {
    section.items.forEach(item => {
      item.checked = permSet.has(item.label)
    })
  })
  permDialogVisible.value = true
}

async function savePermissions() {
  const perms = []
  permissionSections.forEach(section => {
    section.items.forEach(item => {
      if (item.checked) perms.push(item.label)
    })
  })
  try {
    const role = roleList.value.find(r => r.id === currentRoleId.value)
    if (role) {
      await roleSave({ id: role.id, roleName: role.name, roleCode: role.code, description: role.description, permissions: JSON.stringify(perms), isActive: role.isActive })
      ElMessage.success('权限已保存')
      permDialogVisible.value = false
      await loadRoles()
    }
  } catch { ElMessage.error('保存权限失败') }
}

// New role dialog
const roleDialogVisible = ref(false)
const newRoleForm = reactive({ name: '', description: '', copyFrom: '' })

async function createRole() {
  if (!newRoleForm.name) { ElMessage.warning('请输入角色名称'); return }
  try {
    let permissions = '[]'
    if (newRoleForm.copyFrom) {
      const src = roleList.value.find(r => r.name === newRoleForm.copyFrom)
      if (src) permissions = src.permissions || '[]'
    }
    await roleSave({ roleName: newRoleForm.name, roleCode: newRoleForm.name.toLowerCase().replace(/\s+/g, '_'), description: newRoleForm.description, permissions, isActive: true })
    ElMessage.success('角色已创建，请设置权限')
    roleDialogVisible.value = false
    newRoleForm.name = ''
    newRoleForm.description = ''
    newRoleForm.copyFrom = ''
    await loadRoles()
  } catch { ElMessage.error('创建角色失败') }
}

async function deleteRole(row) {
  try {
    await ElMessageBox.confirm(`确定要删除「${row.name}」角色吗？`, '确认删除', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await roleDel(row.id)
    ElMessage.success('删除成功')
    await loadRoles()
  } catch { /* cancelled or error */ }
}

// New admin dialog
const adminDialogVisible = ref(false)
const newAdminForm = reactive({ username: '', realName: '', email: '', role: '', password: '' })

async function createAdmin() {
  if (!newAdminForm.username || !newAdminForm.realName || !newAdminForm.email || !newAdminForm.role) {
    ElMessage.warning('请填写必填字段'); return
  }
  try {
    await userSave({
      username: newAdminForm.username,
      realName: newAdminForm.realName,
      email: newAdminForm.email,
      roleId: newAdminForm.role,
      password: newAdminForm.password || undefined,
      isActive: true
    })
    ElMessage.success('账户已创建')
    adminDialogVisible.value = false
    Object.assign(newAdminForm, { username: '', realName: '', email: '', role: '', password: '' })
    await loadUsers()
  } catch { ElMessage.error('创建账户失败') }
}

async function toggleUser(row) {
  const newActive = !row.isActive
  try {
    await userToggle(row.id, newActive)
    ElMessage.success(newActive ? '已启用' : '已禁用')
    await loadUsers()
  } catch { ElMessage.error('操作失败') }
}

async function resetUserPwd(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'password',
      inputValidator: v => v && v.length >= 6 ? true : '密码至少 6 位'
    })
    await userResetPwd(row.id, value)
    ElMessage.success('密码已重置')
  } catch { /* cancelled */ }
}

// ==================== Tab5: API 限流 ====================
const apiOverview = ref([
  { label: 'HTTP API 总并发', value: '-' },
  { label: '单 IP 每分钟请求', value: '-' },
  { label: 'SMPP 最大连接数', value: '-' },
  { label: '批量发送最大号码数', value: '-' }
])

const apiLimitForm = reactive({
  httpTps: 500,
  ipPerMinute: 600,
  queryPerMinute: 120,
  rateLimitCode: '429',
  smppMaxConn: 200,
  smppPerCustomer: 10,
  batchMax: 10000,
  csvMaxRows: 50000
})

const customerRateLimits = ref([])

async function loadApiConfig() {
  try {
    const res = await configGet('api')
    const d = res.data || {}
    apiLimitForm.httpTps = Number(d.api_rate_limit) || 500
    apiLimitForm.ipPerMinute = Number(d.ip_per_minute) || 600
    apiLimitForm.queryPerMinute = Number(d.query_per_minute) || 120
    apiLimitForm.rateLimitCode = d.rate_limit_code || '429'
    apiLimitForm.smppMaxConn = Number(d.smpp_max_conn) || 200
    apiLimitForm.smppPerCustomer = Number(d.smpp_per_customer) || 10
    apiLimitForm.batchMax = Number(d.batch_max) || 10000
    apiLimitForm.csvMaxRows = Number(d.csv_max_rows) || 50000
    // Update overview cards
    apiOverview.value = [
      { label: 'HTTP API 总并发', value: `${apiLimitForm.httpTps} req/s` },
      { label: '单 IP 每分钟请求', value: `${apiLimitForm.ipPerMinute} req/min` },
      { label: 'SMPP 最大连接数', value: String(apiLimitForm.smppMaxConn) },
      { label: '批量发送最大号码数', value: apiLimitForm.batchMax.toLocaleString() }
    ]
    // Parse customer rate limits if stored
    if (d.customer_rate_limits) {
      try { customerRateLimits.value = JSON.parse(d.customer_rate_limits) } catch { /* ignore */ }
    }
    // IP whitelist
    if (d.ip_whitelist_enabled !== undefined) {
      apiLimitForm.ipWhitelistEnabled = d.ip_whitelist_enabled === 'true'
    }
    if (d.allowed_ips) {
      apiLimitForm.allowedIps = d.allowed_ips
    }
  } catch { /* ignore */ }
}

async function saveApiConfig() {
  try {
    await configBatchUpdate('api', {
      api_rate_limit: String(apiLimitForm.httpTps),
      ip_per_minute: String(apiLimitForm.ipPerMinute),
      query_per_minute: String(apiLimitForm.queryPerMinute),
      rate_limit_code: apiLimitForm.rateLimitCode,
      smpp_max_conn: String(apiLimitForm.smppMaxConn),
      smpp_per_customer: String(apiLimitForm.smppPerCustomer),
      batch_max: String(apiLimitForm.batchMax),
      csv_max_rows: String(apiLimitForm.csvMaxRows),
      customer_rate_limits: JSON.stringify(customerRateLimits.value)
    })
    ElMessage.success('API 限流配置已保存')
    // Refresh overview cards
    apiOverview.value = [
      { label: 'HTTP API 总并发', value: `${apiLimitForm.httpTps} req/s` },
      { label: '单 IP 每分钟请求', value: `${apiLimitForm.ipPerMinute} req/min` },
      { label: 'SMPP 最大连接数', value: String(apiLimitForm.smppMaxConn) },
      { label: '批量发送最大号码数', value: apiLimitForm.batchMax.toLocaleString() }
    ]
  } catch { ElMessage.error('保存失败') }
}

// Rate limit dialog
const rateLimitDialogVisible = ref(false)
const newRateLimitForm = reactive({ customer: '', httpTps: null, smppConn: null, batchLimit: null, remark: '' })

function addCustomerRateLimit() {
  if (!newRateLimitForm.customer) { ElMessage.warning('请选择客户'); return }
  customerRateLimits.value.push({
    customerName: newRateLimitForm.customer,
    httpTps: newRateLimitForm.httpTps || 100,
    smppConn: newRateLimitForm.smppConn || 10,
    batchLimit: newRateLimitForm.batchLimit || 10000,
    remark: newRateLimitForm.remark || '-'
  })
  rateLimitDialogVisible.value = false
  Object.assign(newRateLimitForm, { customer: '', httpTps: null, smppConn: null, batchLimit: null, remark: '' })
  ElMessage.success('限流规则已添加')
}

// ==================== Tab6: 系统信息 ====================
const sysInfoItems = ref([
  { label: '系统版本', value: 'BoruiSMS v2.4.1' },
  { label: '构建编号', value: 'build-20260312-a8f3c2e' },
  { label: '启动时间', value: '2026-03-12 03:00:15' },
  { label: '运行时长', value: '2天 6小时 32分' },
  { label: '数据库状态', value: 'MySQL 8.0  延迟 2ms', tag: '正常', tagType: 'success' },
  { label: 'Redis 状态', value: 'Redis 7.2  内存 1.2GB', tag: '正常', tagType: 'success' },
  { label: '队列状态', value: 'RabbitMQ 3.12  积压 0', tag: '正常', tagType: 'success' },
  { label: 'API 节点', value: '', tag: '3/3 在线', tagType: 'success' },
  { label: 'JVM 堆内存', value: '1.8 GB / 4.0 GB (45%)' },
  { label: '磁盘空间', value: '128 GB / 500 GB (25.6%)' },
  { label: 'SMPP 活跃连接', value: '42 / 200' },
  { label: '今日发送量', value: '1,284,350 条' }
])

// ==================== Common actions ====================
function handleSave(msg) {
  // Route to proper save function based on active tab
  if (activeTab.value === 'basic') { saveBasicConfig(); return }
  if (activeTab.value === 'sms') { saveSmsConfig(); return }
  if (activeTab.value === 'email') { saveSmtpConfig(); return }
  if (activeTab.value === 'apirate') { saveApiConfig(); return }
  ElMessage.success(msg)
}

function handleReset(section) {
  ElMessageBox.confirm(`确定要重置「${section}」吗？`, '确认重置', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // Reload config from server
    if (activeTab.value === 'basic') loadBasicConfig()
    else if (activeTab.value === 'sms') loadSmsConfig()
    else if (activeTab.value === 'apirate') loadApiConfig()
    ElMessage.info(`${section}已重置`)
  }).catch(() => {})
}

function confirmDelete(type, name) {
  // This is used for role delete and rate limit delete from template
  if (type === '角色') {
    const row = roleList.value.find(r => r.name === name)
    if (row) deleteRole(row)
    return
  }
  if (type === '限流规则') {
    ElMessageBox.confirm(`确定要删除「${name}」${type}吗？`, '确认删除', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    }).then(() => {
      customerRateLimits.value = customerRateLimits.value.filter(r => r.customerName !== name)
      ElMessage.success('删除成功')
    }).catch(() => {})
    return
  }
  ElMessageBox.confirm(`确定要删除「${name}」${type}吗？`, '确认删除', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function testSmtp() {
  ElMessage.success('SMTP 连接测试成功')
}

// ==================== Tab switching & init ====================
function loadTabData(tab) {
  if (tab === 'basic') loadBasicConfig()
  else if (tab === 'sms') loadSmsConfig()
  else if (tab === 'email') loadNotificationConfig()
  else if (tab === 'role') { loadRoles(); loadUsers() }
  else if (tab === 'apirate') loadApiConfig()
}

watch(activeTab, (val) => { loadTabData(val) })

onMounted(() => { loadTabData(activeTab.value) })
</script>

<style scoped>
.system-config-page {
  font-size: 13px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.config-card {
  margin-bottom: 16px;
}

.config-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title-text {
  font-size: 16px;
  font-weight: 600;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Form grid - 2 columns */
.form-grid-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.form-grid-inner .full-width {
  grid-column: 1 / -1;
}

.form-grid-inner :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-grid-inner :deep(.el-form-item__label) {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.form-row {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.form-unit {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.form-hint {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.form-actions {
  text-align: right;
  margin-top: 8px;
}

/* Switch wrap */
.switch-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.switch-label-text {
  font-size: 13px;
  color: #666;
}

/* Notify grid */
.notify-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 28px;
}

/* Param grid (data retention) */
.param-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.param-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 16px;
}

.param-name {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.param-unit {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

/* SMTP actions */
.smtp-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

/* Permission grid */
.perm-section {
  margin-bottom: 12px;
}

.perm-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  padding: 6px 0;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 8px;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.perm-grid :deep(.el-checkbox) {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  margin-right: 0;
}

.perm-grid :deep(.el-checkbox:hover) {
  background: #f0f5ff;
}

/* Info row - API overview */
.info-row {
  display: flex;
  gap: 0;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.info-cell {
  flex: 1;
  padding: 12px 16px;
  border-right: 1px solid #e8e8e8;
}

.info-cell:last-child {
  border-right: none;
}

.info-cell-label {
  font-size: 11px;
  color: #999;
  margin-bottom: 3px;
}

.info-cell-value {
  font-size: 13px;
  font-weight: 600;
}

/* System info grid */
.sys-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.info-label {
  color: #666;
  font-size: 13px;
}

.info-value {
  font-weight: 500;
  font-size: 13px;
}

/* Responsive */
@media (max-width: 768px) {
  .form-grid-inner {
    grid-template-columns: 1fr;
  }
  .param-grid {
    grid-template-columns: 1fr;
  }
  .notify-grid {
    grid-template-columns: 1fr;
  }
  .sys-info-grid {
    grid-template-columns: 1fr;
  }
  .perm-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
