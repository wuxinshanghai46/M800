<template>
  <div>
    <!-- ========== List View ========== -->
    <div v-if="!detailMode">
      <div class="page-header">
        <div>
          <h2 class="page-title">客户管理</h2>
          <p class="page-desc">管理所有企业客户账户，配置国际短信权限与报价</p>
        </div>
        <el-button type="primary" @click="openAddDialog"><el-icon><Plus /></el-icon>新建客户</el-button>
      </div>

      <!-- Filter Bar -->
      <div class="filter-bar">
        <div class="filter-row">
          <div class="filter-item">
            <label>搜索</label>
            <el-input v-model="query.keyword" placeholder="客户编码 / 企业名称" clearable style="width: 220px;" @keyup.enter="loadData"><template #prefix><el-icon><Search /></el-icon></template></el-input>
          </div>
          <div class="filter-item">
            <label>付费类型</label>
            <el-select v-model="query.paymentType" placeholder="全部" clearable style="width: 120px;">
              <el-option label="预付费" :value="1" />
              <el-option label="后付费" :value="2" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>状态</label>
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px;">
              <el-option label="正常" :value="1" />
              <el-option label="已冻结" :value="2" />
              <el-option label="已销户" :value="3" />
            </el-select>
          </div>
          <div class="filter-actions">
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </div>
        </div>
      </div>

      <!-- Table Card -->
      <el-card class="table-card" shadow="never">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div class="card-title">客户列表 <span style="font-weight:400; color:#9ca3af; font-size:12px; margin-left:6px;">共 {{ total }} 个客户</span></div>
          </div>
        </template>
        <el-table :data="filteredList" stripe v-loading="loading" :header-cell-style="headerStyle">
          <el-table-column prop="customerCode" label="客户编码" width="100">
            <template #default="{row}"><code class="mono-code">{{ row.customerCode }}</code></template>
          </el-table-column>
          <el-table-column prop="customerName" label="企业名称" min-width="160">
            <template #default="{row}"><span style="font-weight:600;">{{ row.companyName || row.customerName }}</span></template>
          </el-table-column>
          <el-table-column prop="paymentType" label="付费类型" width="100" align="center">
            <template #default="{row}"><el-tag :type="row.paymentType===1?'':'warning'" size="small" effect="plain">{{ row.paymentType===1?'预付费':'后付费' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="账号属性" width="120" align="center">
            <template #default="{row}"><span style="font-size:12px;">{{ formatSmsAttrs(row.allowedSmsAttributes) }}</span></template>
          </el-table-column>
          <el-table-column prop="salesManager" label="销售经理" width="100" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{row}"><el-tag :type="statusTagType[row.status]" size="small" effect="light">{{ statusMap[row.status] }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="开户时间" width="170">
            <template #default="{row}"><span style="font-size:12px; color:#9ca3af;">{{ row.createdAt }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{row}">
              <el-button link type="primary" size="small" @click="enterDetail(row)">详情</el-button>
              <el-dropdown trigger="click" @command="cmd => handleCommand(cmd, row)">
                <el-button link type="primary" size="small" @click.stop>更多<el-icon style="margin-left:2px;"><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="freeze" divided>{{ row.status===2?'解冻':'冻结' }}账户</el-dropdown-item>
                    <el-dropdown-item command="close"><span style="color:#dc2626;">销户</span></el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top: 12px;" />
      </el-card>
    </div>

    <!-- ========== Detail View (Tabs) ========== -->
    <div v-else>
      <div class="detail-header">
        <div style="display: flex; align-items: center; gap: 12px;">
          <el-button @click="detailMode=false; loadData()" plain size="small">
            <el-icon><ArrowLeft /></el-icon> 返回列表
          </el-button>
          <div>
            <h2 class="detail-title">{{ detail.companyName || detail.customerName }}</h2>
            <div class="detail-subtitle">客户编码: {{ detail.customerCode }} &nbsp;&middot;&nbsp; 开户时间: {{ detail.createdAt || '-' }}</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-tag :type="statusTagType[detail.status]" effect="dark" size="large">{{ statusMap[detail.status] }}</el-tag>
          <el-dropdown trigger="click" @command="cmd => handleDetailCommand(cmd)">
            <el-button plain size="small">更多操作 <el-icon style="margin-left:4px;"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="freeze">{{ detail.status===2?'解冻':'冻结' }}账户</el-dropdown-item>
                <el-dropdown-item command="close"><span style="color:#dc2626;">销户</span></el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <el-tabs v-model="detailTab" type="border-card" class="detail-tabs">

        <!-- ===== Tab 0: 基本信息 ===== -->
        <el-tab-pane label="基本信息" name="basic">
          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>账户信息</div>
            <el-form :model="detail" label-width="130px" style="max-width: 900px;">
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="企业名称"><el-input v-model="detail.companyName" placeholder="企业全称" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="客户名称"><el-input v-model="detail.customerName" placeholder="客户显示名称" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="客户编码">
                    <el-input v-model="detail.customerCode" disabled />
                    <div class="form-hint">唯一标识，创建后不可修改</div>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>客户端账号</div>
            <el-form label-width="100px" style="max-width: 1100px;">
              <el-row :gutter="24">
                <el-col :span="9">
                  <el-form-item label="登录账号">
                    <el-input :model-value="detail.portalAccount || (detail.customerCode + '@portal')" readonly class="readonly-input">
                      <template #suffix><el-icon style="cursor:pointer;" @click="copyToClipboard(detail.portalAccount || (detail.customerCode + '@portal'))"><DocumentCopy /></el-icon></template>
                    </el-input>
                    <div class="form-hint">系统自动生成，客户使用此账号登录客户端</div>
                  </el-form-item>
                </el-col>
                <el-col :span="9">
                  <el-form-item label="当前密码">
                    <el-input :model-value="'Sms@' + detail.customerCode + '2024'" type="text" readonly class="readonly-input">
                      <template #suffix>
                        <el-icon style="cursor:pointer;" @click="copyToClipboard('Sms@' + detail.customerCode + '2024')"><DocumentCopy /></el-icon>
                      </template>
                    </el-input>
                    <div class="form-hint">默认密码：Sms@客户编码2024</div>
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="修改密码">
                    <div style="display:flex; gap:8px;">
                      <el-input v-model="resetPassword" type="password" placeholder="输入新密码（至少6位）" show-password style="flex:1;" />
                      <el-button size="small" type="warning" plain @click="doResetPassword">修改</el-button>
                    </div>
                    <div class="form-hint">修改后客户需使用新密码登录</div>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>商务信息</div>
            <el-form :model="detail" label-width="130px" style="max-width: 900px;">
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="销售经理">
                    <el-input v-model="detail.salesManager" placeholder="销售经理姓名" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="联系人"><el-input v-model="detail.contactName" placeholder="联系人姓名" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="联系邮箱"><el-input v-model="detail.contactEmail" placeholder="contact@example.com" /></el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="联系电话"><el-input v-model="detail.contactPhone" placeholder="+86 手机号" /></el-form-item>
                </el-col>
                <el-col :span="16">
                  <el-form-item label="客户地址"><el-input v-model="detail.address" placeholder="公司地址" /></el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="16">
                  <el-form-item label="备注"><el-input v-model="detail.remark" placeholder="内部备注" /></el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-actions">
            <el-button type="primary" @click="saveDetail" :loading="saving">保存修改</el-button>
            <el-button @click="enterDetail(detail)">取消</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 1: 账户管理 ===== -->
        <el-tab-pane label="账户管理" name="account">
          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>计费配置</div>
            <el-form :model="detail" label-width="130px" style="max-width: 900px;">
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="付费类型">
                    <el-radio-group v-model="detail.paymentType">
                      <el-radio :value="1">预付费</el-radio>
                      <el-radio :value="2">后付费</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="出账币种">
                    <el-select v-model="detail.billCurrency" style="width: 200px;">
                      <el-option label="USD — 美元" value="USD" />
                      <el-option label="HKD — 港币" value="HKD" />
                      <el-option label="CNY — 人民币" value="CNY" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="时区">
                    <el-input v-model="detail.timezone" placeholder="如 UTC, Asia/Shanghai" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="自动发送账单">
                    <el-switch v-model="detail.billAutoSend" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="账单接收邮箱">
                    <el-input v-model="detail.billEmail" placeholder="bill@example.com" />
                    <div class="form-hint">多邮箱用逗号分隔</div>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <!-- 预付费 -->
          <template v-if="detail.paymentType === 1">
            <div class="form-section">
              <div class="form-section-title"><span class="section-bar"></span>账户概览</div>
              <el-descriptions :column="3" border style="max-width: 800px;">
                <el-descriptions-item label="可用余额">
                  <span style="font-size:18px; font-weight:700; color:#16a34a;">{{ account.currency || 'USD' }} {{ formatMoney(account.balance) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="冻结金额">
                  <span style="font-weight:600; color:#f59e0b;">{{ formatMoney(account.frozenAmount) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="币种">
                  <span>{{ account.currency || 'USD' }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="form-section">
              <div class="form-section-title"><span class="section-bar"></span>充值 / 扣费</div>
              <el-form label-width="100px" style="max-width: 500px;">
                <el-form-item label="操作类型">
                  <el-radio-group v-model="accountOp.type">
                    <el-radio value="recharge">充值</el-radio>
                    <el-radio value="deduct">扣费</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="金额">
                  <el-input-number v-model="accountOp.amount" :min="0.01" :precision="2" style="width: 200px;" />
                </el-form-item>
                <el-form-item label="备注">
                  <el-input v-model="accountOp.remark" placeholder="操作备注（可选）" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="doAccountOp" :loading="saving">
                    {{ accountOp.type === 'recharge' ? '确认充值' : '确认扣费' }}
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </template>

          <!-- 后付费 -->
          <template v-else>
            <div class="form-section">
              <div class="form-section-title"><span class="section-bar"></span>账户概览</div>
              <el-descriptions :column="3" border style="max-width: 800px;">
                <el-descriptions-item label="信用额度">
                  <span style="font-size:18px; font-weight:700; color:#2563eb;">{{ account.currency || 'USD' }} {{ formatMoney(account.creditLimit) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="已用额度">
                  <span style="font-weight:600; color:#f59e0b;">{{ formatMoney(account.frozenAmount) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="币种">
                  <span>{{ account.currency || 'USD' }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="form-section">
              <div class="form-section-title"><span class="section-bar"></span>信用额度设置</div>
              <el-form label-width="100px" style="max-width: 500px;">
                <el-form-item label="信用额度">
                  <el-input-number v-model="creditLimitVal" :min="0" :precision="2" style="width: 200px;" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="setCreditLimit" :loading="saving">保存</el-button>
                </el-form-item>
              </el-form>
            </div>
          </template>

          <div class="form-actions">
            <el-button type="primary" @click="saveDetail" :loading="saving">保存</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab: 属性 & SID ===== -->
        <el-tab-pane label="属性 & SID" name="attr-sid">
          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>账号属性</div>
            <el-form label-width="160px" style="max-width: 900px;">
              <el-form-item label="账号属性">
                <el-radio-group v-model="detail.allowedSmsAttributes">
                  <el-radio value="1">OTP</el-radio>
                  <el-radio value="2">事务通知</el-radio>
                  <el-radio value="3">通知</el-radio>
                  <el-radio value="4">营销</el-radio>
                  <el-radio value="1,2,3">通知 + OTP</el-radio>
                  <el-radio value="1,2,3,4">ALL（全部）</el-radio>
                </el-radio-group>
                <div class="form-hint">决定该客户可以发送哪些类型的短信，影响通道匹配</div>
              </el-form-item>
            </el-form>
          </div>

          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>SID 配置</div>
            <el-form :model="detail" label-width="160px" style="max-width: 900px;">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="是否允许客户自选 SID">
                    <el-radio-group v-model="detail.sidSelectable">
                      <el-radio :value="false">否（系统自动分配）</el-radio>
                      <el-radio :value="true">是</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="SID 格式偏好">
                    <el-radio-group v-model="detail.preferredSidFormat">
                      <el-radio value="">不限</el-radio>
                      <el-radio value="ALPHA">字母数字型</el-radio>
                      <el-radio value="NUMERIC">数字型</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-section">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;">
              <div class="form-section-title" style="margin-bottom:0; border-bottom:none; padding-bottom:0;"><span class="section-bar"></span>已授权 SID</div>
              <el-button type="primary" size="small" plain @click="openAssignSidDialog"><el-icon><Plus /></el-icon>分配 SID</el-button>
            </div>
            <div class="form-hint" style="margin-bottom:10px;">资源池中的 SID 均可分配给该客户，分配后 API 即可使用此 SID 发送。</div>
            <el-table :data="assignedSids" stripe :header-cell-style="headerStyle" style="margin-bottom: 16px;">
              <el-table-column prop="sid" label="SID" width="140">
                <template #default="{row}"><code class="mono-code">{{ row.sid }}</code></template>
              </el-table-column>
              <el-table-column prop="sidType" label="类型" width="100">
                <template #default="{row}"><el-tag size="small" effect="plain">{{ row.sidType }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="smsType" label="短信类型" width="100">
                <template #default="{row}"><el-tag size="small" :type="row.smsType==='MARKETING'?'warning':''" effect="plain">{{ row.smsType }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="countryCode" label="国家" width="80" />
              <el-table-column prop="isActive" label="状态" width="80">
                <template #default="{row}"><el-tag :type="row.isActive?'success':'warning'" size="small" effect="light">{{ row.isActive?'启用':'禁用' }}</el-tag></template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{row}">
                  <el-popconfirm title="确认取消授权?" @confirm="unassignSid(row.id)">
                    <template #reference><el-button link type="danger" size="small">解除授权</el-button></template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="assignedSids.length === 0" description="暂无已授权SID" :image-size="60" />
          </div>

          <!-- 上行/下行通道配置 -->
          <div class="form-section">
            <div class="form-section-title"><span class="section-bar"></span>上行 / 下行通道配置</div>
            <div class="form-hint" style="margin-bottom:12px;">为该客户指定专用的下行（MT发送）通道和上行（MO接收）通道，留空则走系统默认路由。</div>
            <el-form :model="detail" label-width="160px" style="max-width: 800px;">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="下行通道（MT）">
                    <el-select v-model="detail.mtChannelId" placeholder="系统自动路由" clearable filterable style="width:100%;">
                      <el-option v-for="ch in mtChannels" :key="ch.id"
                        :label="`${ch.channelName}（${ch.countryCode}）`" :value="ch.id" />
                    </el-select>
                    <div class="form-hint">仅显示双向或下行MT通道</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="上行通道（MO）">
                    <el-select v-model="detail.moChannelId" placeholder="系统自动路由" clearable filterable style="width:100%;">
                      <el-option v-for="ch in moChannels" :key="ch.id"
                        :label="`${ch.channelName}（${ch.countryCode}）`" :value="ch.id" />
                    </el-select>
                    <div class="form-hint">仅显示双向或上行MO通道</div>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-actions">
            <el-button type="primary" @click="saveDetail" :loading="saving">保存修改</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 2: 国家开通 & 报价 ===== -->
        <el-tab-pane label="国家开通 & 报价" name="country-price">
          <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:16px;">
            <div style="font-size:13px; color:#6b7280;">点击左侧国家查看或编辑该国报价</div>
            <div style="display:flex; gap:8px;">
              <el-button size="small" plain @click="batchImportDialog=true"><el-icon><Upload /></el-icon>批量导入</el-button>
              <el-button type="primary" size="small" @click="openCountryDialog"><el-icon><Plus /></el-icon>开通国家</el-button>
            </div>
          </div>
          <div class="country-price-layout">
            <!-- Left: Country List -->
            <div class="country-list-panel">
              <div class="panel-header">
                <span>已开通国家 ({{ customerCountries.length }})</span>
              </div>
              <el-scrollbar max-height="420px">
                <div v-if="customerCountries.length === 0" style="padding: 40px 0; text-align: center; color: #d1d5db; font-size: 13px;">暂未开通国家</div>
                <div v-for="cc in customerCountries" :key="cc.id"
                  :class="['country-item', { active: selectedCountry === cc.countryCode }]"
                  @click="selectCountry(cc)">
                  <span style="font-weight:500;">{{ cc.countryCode }}</span>
                  <div style="display:flex; gap:6px; align-items:center;">
                    <el-tag :type="cc.isActive?'success':'warning'" size="small" effect="light">{{ cc.isActive?'启用':'禁用' }}</el-tag>
                    <el-popconfirm title="确认关闭该国?" @confirm="closeCountry(cc)">
                      <template #reference><el-button link type="danger" size="small">关闭</el-button></template>
                    </el-popconfirm>
                  </div>
                </div>
              </el-scrollbar>
            </div>

            <!-- Right: Price Table -->
            <div class="price-panel">
              <div v-if="selectedCountry">
                <div class="price-panel-header">
                  <span style="font-size:13px; font-weight:600; color:#374151;">{{ selectedCountry }} — 报价配置</span>
                  <el-button size="small" plain @click="openPriceForm"><el-icon><Plus /></el-icon>添加报价</el-button>
                </div>
                <div style="padding: 16px;">
                  <el-table :data="countryPrices" stripe :header-cell-style="headerStyle">
                    <el-table-column label="短信类型" width="120">
                      <template #default="{row}"><el-tag size="small" effect="plain">{{ smsTypeMap[row.smsAttribute] || '默认' }}</el-tag></template>
                    </el-table-column>
                    <el-table-column label="单价" width="140">
                      <template #default="{row}"><span style="font-weight: 600; color: #1f2937;">{{ row.currency || 'USD' }} {{ row.price }}</span></template>
                    </el-table-column>
                    <el-table-column label="更新时间" width="170">
                      <template #default="{row}"><span style="font-size:12px; color:#9ca3af;">{{ row.updatedAt || row.createdAt || '-' }}</span></template>
                    </el-table-column>
                    <el-table-column label="操作" width="120" align="center">
                      <template #default="{row}">
                        <el-button link type="primary" size="small" @click="editPrice(row)">编辑</el-button>
                        <el-popconfirm title="确认删除?" @confirm="deletePrice(row.id)"><template #reference><el-button link type="danger" size="small">删除</el-button></template></el-popconfirm>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-if="countryPrices.length === 0" description="暂无报价配置" :image-size="60" />
                </div>
              </div>
              <div v-else class="empty-placeholder">
                <el-icon :size="48" color="#d1d5db"><Location /></el-icon>
                <p>请从左侧选择国家查看报价</p>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 3: API 凭证 ===== -->
        <el-tab-pane label="API 凭证" name="api">
          <div class="form-section">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;">
              <div class="form-section-title" style="margin-bottom:0; border-bottom:none; padding-bottom:0;"><span class="section-bar"></span>API 凭证列表</div>
              <el-button type="primary" size="small" @click="createCredential"><el-icon><Plus /></el-icon>新建凭证</el-button>
            </div>
            <el-table :data="credentials" stripe :header-cell-style="headerStyle" style="max-width: 900px;">
              <el-table-column label="API Key" width="260">
                <template #default="{row}"><code class="mono-code">{{ row.apiKey }}</code></template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{row}"><el-tag :type="row.isActive?'success':'info'" size="small" effect="light">{{ row.isActive?'启用':'禁用' }}</el-tag></template>
              </el-table-column>
              <el-table-column label="IP 白名单" min-width="160">
                <template #default="{row}"><span style="font-size:12px; color:#6b7280;">{{ row.ipWhitelist || '未设置（不限制）' }}</span></template>
              </el-table-column>
              <el-table-column label="创建时间" width="170">
                <template #default="{row}"><span style="font-size:12px; color:#9ca3af;">{{ row.createdAt }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="240" align="center">
                <template #default="{row}">
                  <el-button link type="primary" size="small" @click="openIpDialog(row)">IP白名单</el-button>
                  <el-popconfirm title="重置后旧Secret立即失效，确认?" @confirm="resetSecret(row)">
                    <template #reference><el-button link type="warning" size="small">重置Secret</el-button></template>
                  </el-popconfirm>
                  <el-button link :type="row.isActive?'warning':'success'" size="small" @click="toggleCredential(row)">{{ row.isActive?'禁用':'启用' }}</el-button>
                  <el-popconfirm title="确认删除?" @confirm="deleteCredential(row.id)">
                    <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="credentials.length === 0" description="暂无 API 凭证" :image-size="60" />
          </div>

          <!-- Show new secret after create/reset -->
          <el-alert v-if="newSecretDisplay" type="warning" :closable="true" @close="newSecretDisplay=''" style="max-width:900px; margin-top:16px;">
            <template #title>
              <span style="font-weight:600;">新生成的 API Secret（仅显示一次，请妥善保存）：</span>
            </template>
            <code class="mono-code" style="font-size:14px;">{{ newSecretDisplay }}</code>
            <el-button size="small" style="margin-left:12px;" @click="copyToClipboard(newSecretDisplay)">复制</el-button>
          </el-alert>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ========== Add Customer Dialog ========== -->
    <el-dialog v-model="addDialog" title="新建客户" width="600px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px;">填写基本信息后创建，短信配置、报价、凭证等可在详情页继续完善。</el-alert>
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="企业名称" prop="companyName"><el-input v-model="addForm.companyName" placeholder="企业全称" @blur="autoGenCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户编码" prop="customerCode"><el-input v-model="addForm.customerCode" disabled /><div class="form-hint">系统自动分配，创建后不可修改</div></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="付费类型" prop="paymentType">
            <el-radio-group v-model="addForm.paymentType"><el-radio :value="1">预付费</el-radio><el-radio :value="2">后付费</el-radio></el-radio-group>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售经理">
            <el-input v-model="addForm.salesManager" placeholder="销售经理姓名" />
          </el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="addForm.contactName" placeholder="联系人姓名" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系邮箱"><el-input v-model="addForm.contactEmail" placeholder="contact@example.com" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="可选" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="addDialog=false">取消</el-button><el-button type="primary" @click="saveNewCustomer" :loading="saving">创建并进入详情</el-button></template>
    </el-dialog>

    <!-- Open Country Dialog -->
    <el-dialog v-model="countryDialog" title="开通国家" width="420px">
      <el-form label-width="80px">
        <el-form-item label="选择国家">
          <el-select v-model="newCountrySelection" filterable placeholder="搜索国家" style="width:100%;" @change="onCountrySelect">
            <el-option v-for="c in availableCountries" :key="c.id" :label="`${c.countryCode} — ${c.countryName}`" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" style="margin-top: 8px;">开通后需配置报价，才可正常发送。</el-alert>
      <template #footer><el-button @click="countryDialog=false">取消</el-button><el-button type="primary" @click="openCountryForCustomer" :disabled="!newCountrySelection">确认开通</el-button></template>
    </el-dialog>

    <!-- Price Dialog -->
    <el-dialog v-model="priceDialog" :title="priceForm.id ? '编辑报价' : '添加报价'" width="480px">
      <el-form :model="priceForm" label-width="120px">
        <el-form-item label="国家代码">
          <el-input :model-value="priceForm.countryCode" disabled />
        </el-form-item>
        <el-form-item label="短信类型">
          <el-radio-group v-model="priceForm.smsAttribute">
            <el-radio :value="1">OTP</el-radio>
            <el-radio :value="2">事务通知</el-radio>
            <el-radio :value="3">通知</el-radio>
            <el-radio :value="4">营销</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="单价"><el-input v-model="priceForm.price" placeholder="如: 0.030000" /></el-form-item>
        <el-form-item label="币种">
          <el-select v-model="priceForm.currency" style="width:160px;">
            <el-option label="USD" value="USD" />
            <el-option label="HKD" value="HKD" />
            <el-option label="CNY" value="CNY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="priceDialog=false">取消</el-button><el-button type="primary" @click="savePrice">保存</el-button></template>
    </el-dialog>

    <!-- Assign SID Dialog -->
    <el-dialog v-model="assignSidDialog" title="分配 SID" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px;" v-if="customerCountries.length > 0">
        仅显示客户已开通国家（{{ customerCountries.map(c=>c.countryCode).join(', ') }}）的 SID
      </el-alert>
      <el-table :data="filteredAvailableSids" stripe :header-cell-style="headerStyle" max-height="400px">
        <el-table-column prop="sid" label="SID" width="140">
          <template #default="{row}"><code class="mono-code">{{ row.sid }}</code></template>
        </el-table-column>
        <el-table-column prop="sidType" label="类型" width="100" />
        <el-table-column prop="smsType" label="账号属性" width="100" />
        <el-table-column prop="countryCode" label="国家" width="80" />
        <el-table-column prop="isActive" label="状态" width="80">
          <template #default="{row}"><el-tag :type="row.isActive?'success':'info'" size="small">{{ row.isActive?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{row}">
            <el-button v-if="!assignedSidIds.has(row.id)" link type="primary" size="small" @click="doAssignSid(row.id)">分配</el-button>
            <span v-else style="font-size:12px; color:#9ca3af;">已分配</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="filteredAvailableSids.length === 0" description="暂无可用 SID（请先开通国家）" :image-size="60" />
      <template #footer><el-button @click="assignSidDialog=false">关闭</el-button></template>
    </el-dialog>

    <!-- IP Whitelist Dialog -->
    <el-dialog v-model="ipDialog" title="设置 IP 白名单" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px;">未配置白名单时不限制来源 IP。多个 IP 用逗号分隔。</el-alert>
      <el-input v-model="ipEditValue" type="textarea" :rows="4" placeholder="如: 1.2.3.4,5.6.7.8,10.0.0.0/24" />
      <template #footer><el-button @click="ipDialog=false">取消</el-button><el-button type="primary" @click="saveIpWhitelist">保存</el-button></template>
    </el-dialog>

    <!-- Batch Import Dialog -->
    <el-dialog v-model="batchImportDialog" title="批量导入国家 & 报价" width="720px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom:12px;">
        <template #title>每行一条，格式：<code style="background:#f3f4f6;padding:2px 6px;border-radius:4px;">国家代码,短信类型,单价,币种</code></template>
        只填国家代码则仅开通国家不设报价。币种可省略默认 USD。
      </el-alert>
      <div style="margin-bottom:12px; padding:10px 14px; background:#f9fafb; border-radius:6px; border:1px solid #e5e7eb;">
        <div style="font-size:12px; font-weight:600; color:#4b5563; margin-bottom:6px;">短信类型对照：</div>
        <div style="display:flex; gap:20px; font-size:12px; color:#6b7280;">
          <span><code style="background:#e0e7ff;padding:1px 6px;border-radius:3px;color:#4338ca;">1</code> = OTP</span>
          <span><code style="background:#e0e7ff;padding:1px 6px;border-radius:3px;color:#4338ca;">2</code> = 事务通知</span>
          <span><code style="background:#e0e7ff;padding:1px 6px;border-radius:3px;color:#4338ca;">3</code> = 通知</span>
          <span><code style="background:#e0e7ff;padding:1px 6px;border-radius:3px;color:#4338ca;">4</code> = 营销</span>
        </div>
      </div>
      <el-input v-model="batchImportText" type="textarea" :rows="12" placeholder="CN,1,0.035,USD
CN,3,0.030,USD
CN,4,0.025,USD
US,1,0.008,USD
TH,4,0.025
IN,1,0.005
PH
ID,4,0.018,USD" style="font-family:'Courier New',monospace; font-size:13px;" />
      <div v-if="batchImportResult" style="margin-top:12px;">
        <el-alert :type="batchImportResult.type" :closable="false">{{ batchImportResult.msg }}</el-alert>
      </div>
      <template #footer>
        <el-button @click="batchImportDialog=false">关闭</el-button>
        <el-button type="primary" @click="doBatchImport" :loading="batchImporting">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerList, customerSave, customerUpdate, customerDetail, countryList as countryListApi, sidList, channelList } from '../api'
import http from '../api/request'

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const statusMap = { 0: '禁用', 1: '正常', 2: '已冻结', 3: '已销户' }
const statusTagType = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
const smsTypeMap = { 1: 'OTP', 2: '事务通知', 3: '通知', 4: '营销' }
const formatSmsAttrs = v => {
  if (!v || v === '1,2,3,4') return 'ALL'
  return v.split(',').map(c => smsTypeMap[c] || c).join('/')
}

// ===== List =====
const loading = ref(false); const saving = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: null, paymentType: null })

const filteredList = computed(() => {
  let data = list.value
  if (query.paymentType) data = data.filter(c => c.paymentType === query.paymentType)
  return data
})

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status) params.status = query.status
    const r = await customerList(params)
    list.value = r.data?.list || []; total.value = r.data?.total || 0
  } finally { loading.value = false }
}
const resetQuery = () => { query.keyword = ''; query.status = null; query.paymentType = null; query.page = 1; loadData() }

// ===== Add Customer =====
const addDialog = ref(false); const addForm = ref({}); const addFormRef = ref(null)
const addRules = {
  customerCode: [{ required: true, message: '客户编码不能为空', trigger: 'blur' }],
  companyName: [{ required: true, message: '企业名称不能为空', trigger: 'blur' }],
  paymentType: [{ required: true, message: '请选择付费类型', trigger: 'change' }],
}
const genCode = () => 'C' + String(Math.floor(1000 + Math.random() * 9000))
const openAddDialog = () => { addForm.value = { status: 1, paymentType: 1, customerCode: genCode() }; addDialog.value = true }

const autoGenCode = () => {}

const saveNewCustomer = async () => {
  if (addFormRef.value) { const valid = await addFormRef.value.validate().catch(() => false); if (!valid) return }
  saving.value = true
  try {
    const payload = {
      customerCode: addForm.value.customerCode,
      customerName: addForm.value.companyName,
      companyName: addForm.value.companyName,
      paymentType: addForm.value.paymentType,
      salesManager: addForm.value.salesManager || null,
      contactName: addForm.value.contactName || null,
      contactEmail: addForm.value.contactEmail || null,
      remark: addForm.value.remark || null,
    }
    const res = await customerSave(payload)
    ElMessage.success('创建成功'); addDialog.value = false
    if (res.data) enterDetail(res.data)
    else loadData()
  } finally { saving.value = false }
}

// ===== Detail =====
const detailMode = ref(false)
const detail = ref({}); const detailTab = ref('basic')
const account = ref({})
const countryPricesAll = ref([])
const credentials = ref([])
const assignedSids = ref([])
const allChannels = ref([])
const mtChannels = computed(() => allChannels.value.filter(c => !c.smppBindType || c.smppBindType === 'BIND_TRX' || c.smppBindType === 'BIND_TX'))
const moChannels = computed(() => allChannels.value.filter(c => !c.smppBindType || c.smppBindType === 'BIND_TRX' || c.smppBindType === 'BIND_RX'))
const resetPassword = ref('')
const showPortalPwd = ref(false)
const newSecretDisplay = ref('')


const enterDetail = async row => {
  if (allChannels.value.length === 0) {
    try { const r = await channelList({ page: 1, size: 200 }); allChannels.value = r.data?.list || [] } catch { /* ignore */ }
  }
  try {
    const r = await customerDetail(row.id)
    const d = r.data || {}
    detail.value = d.customer || d || row
    account.value = d.account || {}
    countryPricesAll.value = d.prices || []
    credentials.value = d.credentials || []
    assignedSids.value = d.sids || []
    customerCountries.value = d.countries || []
  } catch (e) {
    detail.value = { ...row }
    account.value = {}
    countryPricesAll.value = []
    credentials.value = []
    assignedSids.value = []
    customerCountries.value = []
  }
  // defaults
  if (!detail.value.billCurrency) detail.value.billCurrency = 'USD'
  if (!detail.value.timezone) detail.value.timezone = 'UTC'
  if (detail.value.billAutoSend === undefined || detail.value.billAutoSend === null) detail.value.billAutoSend = false
  if (detail.value.sidSelectable === undefined || detail.value.sidSelectable === null) detail.value.sidSelectable = false
  if (!detail.value.preferredSidFormat) detail.value.preferredSidFormat = ''
  creditLimitVal.value = account.value.creditLimit || 0
  newSecretDisplay.value = ''
  selectedCountry.value = null
  countryPrices.value = []
  detailMode.value = true; detailTab.value = 'basic'
}

const saveDetail = async () => {
  saving.value = true
  try {
    const payload = {
      customerCode: detail.value.customerCode,
      customerName: detail.value.customerName,
      companyName: detail.value.companyName,
      paymentType: detail.value.paymentType,
      salesManager: detail.value.salesManager,
      contactName: detail.value.contactName,
      contactEmail: detail.value.contactEmail,
      contactPhone: detail.value.contactPhone,
      timezone: detail.value.timezone,
      billCurrency: detail.value.billCurrency,
      billAutoSend: detail.value.billAutoSend,
      billEmail: detail.value.billEmail,
      sidSelectable: detail.value.sidSelectable,
      preferredSidFormat: detail.value.preferredSidFormat,
      allowedSmsAttributes: detail.value.allowedSmsAttributes,
      address: detail.value.address,
      remark: detail.value.remark,
    }
    await customerUpdate(detail.value.id, payload)
    ElMessage.success('保存成功')
  } finally { saving.value = false }
}

// ===== Account Operations =====
const accountOp = reactive({ type: 'recharge', amount: 0, remark: '' })
const creditLimitVal = ref(0)

const doAccountOp = async () => {
  if (!accountOp.amount || accountOp.amount <= 0) { ElMessage.warning('请输入有效金额'); return }
  saving.value = true
  try {
    const url = `/admin/customer/${detail.value.id}/account/${accountOp.type}`
    const r = await http.post(url, { amount: accountOp.amount, remark: accountOp.remark })
    account.value = r.data || account.value
    ElMessage.success(accountOp.type === 'recharge' ? '充值成功' : '扣费成功')
    accountOp.amount = 0; accountOp.remark = ''
  } finally { saving.value = false }
}

const setCreditLimit = async () => {
  saving.value = true
  try {
    const r = await http.put(`/admin/customer/${detail.value.id}/account/credit-limit`, null, { params: { creditLimit: creditLimitVal.value } })
    account.value = r.data || account.value
    ElMessage.success('信用额度已更新')
  } finally { saving.value = false }
}

const formatMoney = v => (v === null || v === undefined) ? '0.00' : Number(v).toFixed(2)

// ===== Commands =====
const doResetPassword = async () => {
  if (!resetPassword.value || resetPassword.value.length < 6) { ElMessage.warning('密码至少6位'); return }
  try {
    await ElMessageBox.confirm('确认修改该客户的登录密码?', '提示', { type: 'warning' })
    const payload = {
      customerCode: detail.value.customerCode,
      customerName: detail.value.customerName,
      companyName: detail.value.companyName,
      paymentType: detail.value.paymentType,
      portalPassword: resetPassword.value,
    }
    await customerUpdate(detail.value.id, payload)
    detail.value.portalPassword = resetPassword.value
    ElMessage.success('密码已修改')
    resetPassword.value = ''
  } catch { /* cancelled */ }
}

const handleCommand = async (cmd, row) => {
  if (cmd === 'edit') { enterDetail(row); return }
  if (cmd === 'freeze') {
    const newStatus = row.status === 2 ? 1 : 2
    const label = newStatus === 2 ? '冻结' : '解冻'
    try {
      await ElMessageBox.confirm(`确认${label}客户 ${row.customerName || row.companyName}?`, '提示', { type: 'warning' })
      await http.put(`/admin/customer/${row.id}/status`, null, { params: { status: newStatus } })
      ElMessage.success(`${label}成功`); loadData()
    } catch { /* cancelled */ }
  }
  if (cmd === 'close') {
    try {
      await ElMessageBox.confirm(`确认销户客户 ${row.customerName || row.companyName}? 此操作不可恢复。`, '销户确认', { type: 'error' })
      await http.put(`/admin/customer/${row.id}/status`, null, { params: { status: 3 } })
      ElMessage.success('销户成功'); loadData()
    } catch { /* cancelled */ }
  }
}

const handleDetailCommand = async cmd => {
  if (cmd === 'freeze') {
    const newStatus = detail.value.status === 2 ? 1 : 2
    const label = newStatus === 2 ? '冻结' : '解冻'
    try {
      await ElMessageBox.confirm(`确认${label}该客户?`, '提示', { type: 'warning' })
      const r = await http.put(`/admin/customer/${detail.value.id}/status`, null, { params: { status: newStatus } })
      detail.value.status = (r.data || {}).status || newStatus
      ElMessage.success(`${label}成功`)
    } catch { /* cancelled */ }
  }
  if (cmd === 'close') {
    try {
      await ElMessageBox.confirm('确认销户? 此操作不可恢复。', '销户确认', { type: 'error' })
      await http.put(`/admin/customer/${detail.value.id}/status`, null, { params: { status: 3 } })
      detail.value.status = 3
      ElMessage.success('销户成功')
    } catch { /* cancelled */ }
  }
}

// ===== Country & Price (Tab 2) =====
const customerCountries = ref([]); const selectedCountry = ref(null); const countryPrices = ref([])
const countryDialog = ref(false)
const priceDialog = ref(false); const priceForm = ref({})
const allCountries = ref([])
const newCountrySelection = ref(null)
const selectedCountryObj = ref(null)

const loadAllCountries = async () => {
  try { const r = await countryListApi({ page: 1, size: 500 }); allCountries.value = r.data?.list || r.data || [] } catch { allCountries.value = [] }
}

// 过滤掉已开通的国家，避免重复开通
const availableCountries = computed(() => {
  const enabledCodes = new Set(customerCountries.value.map(c => c.countryCode))
  return allCountries.value.filter(c => !enabledCodes.has(c.countryCode))
})

const onCountrySelect = id => {
  selectedCountryObj.value = allCountries.value.find(c => c.id === id) || null
}

const loadCustomerCountries = async () => {
  if (!detail.value.id) return
  try { const r = await http.get(`/admin/customer/${detail.value.id}/countries`); customerCountries.value = r.data || [] } catch { customerCountries.value = [] }
}

const loadAllPrices = async () => {
  if (!detail.value.id) return
  try { const r = await http.get(`/admin/customer/${detail.value.id}/prices`); countryPricesAll.value = r.data || [] } catch { countryPricesAll.value = [] }
}

const selectCountry = cc => {
  selectedCountry.value = cc.countryCode
  countryPrices.value = countryPricesAll.value.filter(p => p.countryCode === cc.countryCode)
}

const openCountryDialog = () => { newCountrySelection.value = null; selectedCountryObj.value = null; countryDialog.value = true; if (allCountries.value.length === 0) loadAllCountries() }

const openCountryForCustomer = async () => {
  if (!selectedCountryObj.value) { ElMessage.warning('请选择国家'); return }
  // 校验：检查是否已开通
  const alreadyEnabled = customerCountries.value.some(c => c.countryCode === selectedCountryObj.value.countryCode)
  if (alreadyEnabled) { ElMessage.warning('该国家已开通'); return }
  try {
    await http.post(`/admin/customer/${detail.value.id}/countries`, null, {
      params: { countryCode: selectedCountryObj.value.countryCode }
    })
    ElMessage.success('开通成功'); countryDialog.value = false; loadCustomerCountries()
  } catch { /* error handled by interceptor */ }
}

const closeCountry = async cc => {
  // 校验：检查该国家下是否还有报价，提醒用户
  const hasPrices = countryPricesAll.value.some(p => p.countryCode === cc.countryCode)
  if (hasPrices) {
    try {
      await ElMessageBox.confirm(
        `关闭国家 ${cc.countryCode} 后，该国下的报价配置将不再生效。确认关闭？`,
        '确认关闭', { type: 'warning' })
    } catch { return }
  }
  try {
    await http.delete(`/admin/customer/${detail.value.id}/countries/${cc.countryCode}`)
    ElMessage.success('已关闭'); loadCustomerCountries()
    if (selectedCountry.value === cc.countryCode) { selectedCountry.value = null; countryPrices.value = [] }
  } catch { /* */ }
}

const openPriceForm = () => { priceForm.value = { countryCode: selectedCountry.value, smsAttribute: 1, currency: 'USD' }; priceDialog.value = true }
const editPrice = row => { priceForm.value = { ...row }; priceDialog.value = true }
const savePrice = async () => {
  if (!priceForm.value.smsAttribute) { ElMessage.warning('请选择短信类型'); return }
  if (!priceForm.value.price) { ElMessage.warning('请输入单价'); return }
  if (!priceForm.value.currency) { ElMessage.warning('请选择币种'); return }
  try {
    await http.post(`/admin/customer/${detail.value.id}/prices`, {
      countryCode: priceForm.value.countryCode,
      smsAttribute: priceForm.value.smsAttribute,
      price: priceForm.value.price,
      currency: priceForm.value.currency || 'USD',
    })
    ElMessage.success('保存成功'); priceDialog.value = false
    await loadAllPrices(); selectCountry({ countryCode: selectedCountry.value })
  } catch { /* */ }
}
const deletePrice = async id => {
  try { await http.delete(`/admin/customer/prices/${id}`); ElMessage.success('删除成功'); await loadAllPrices(); selectCountry({ countryCode: selectedCountry.value }) } catch { /* */ }
}

// ===== API Credentials (Tab 3) =====
const ipDialog = ref(false); const ipEditValue = ref(''); const ipEditCredentialId = ref(null)

const loadCredentials = async () => {
  if (!detail.value.id) return
  try { const r = await http.get(`/admin/customer/${detail.value.id}/credentials`); credentials.value = r.data || [] } catch { credentials.value = [] }
}

const createCredential = async () => {
  try {
    const r = await http.post(`/admin/customer/${detail.value.id}/credentials`)
    ElMessage.success('凭证已创建')
    if (r.data && r.data.apiSecret) newSecretDisplay.value = r.data.apiSecret
    loadCredentials()
  } catch { /* */ }
}

const resetSecret = async row => {
  try {
    const r = await http.put(`/admin/customer/credentials/${row.id}/reset-secret`)
    ElMessage.success('Secret 已重置')
    if (r.data && r.data.apiSecret) newSecretDisplay.value = r.data.apiSecret
    loadCredentials()
  } catch { /* */ }
}

const toggleCredential = async row => {
  try {
    await http.put(`/admin/customer/credentials/${row.id}/toggle`, null, { params: { active: !row.isActive } })
    ElMessage.success(row.isActive ? '已禁用' : '已启用')
    loadCredentials()
  } catch { /* */ }
}

const deleteCredential = async id => {
  try { await http.delete(`/admin/customer/credentials/${id}`); ElMessage.success('已删除'); loadCredentials() } catch { /* */ }
}

const openIpDialog = row => {
  ipEditCredentialId.value = row.id
  ipEditValue.value = row.ipWhitelist || ''
  ipDialog.value = true
}

const saveIpWhitelist = async () => {
  try {
    await http.put(`/admin/customer/credentials/${ipEditCredentialId.value}/ip-whitelist`, null, { params: { ipWhitelist: ipEditValue.value } })
    ElMessage.success('IP 白名单已更新'); ipDialog.value = false; loadCredentials()
  } catch { /* */ }
}

// ===== SID Assignment =====
const assignSidDialog = ref(false)
const availableSids = ref([])
const assignedSidIds = computed(() => new Set(assignedSids.value.map(s => s.id)))

// 仅显示客户已开通国家的 SID，避免分配无法使用的 SID
const filteredAvailableSids = computed(() => {
  const enabledCodes = new Set(customerCountries.value.map(c => c.countryCode))
  if (enabledCodes.size === 0) return availableSids.value // 未开通任何国家时显示全部
  return availableSids.value.filter(s => enabledCodes.has(s.countryCode))
})

const openAssignSidDialog = async () => {
  assignSidDialog.value = true
  try { const r = await sidList({ page: 1, size: 500 }); availableSids.value = r.data?.list || r.data || [] } catch { availableSids.value = [] }
}

const doAssignSid = async sidId => {
  try {
    await http.post(`/admin/customer/${detail.value.id}/sids`, null, { params: { sidId } })
    ElMessage.success('分配成功')
    loadCustomerSids()
  } catch { /* */ }
}

const unassignSid = async sidId => {
  try {
    await http.delete(`/admin/customer/${detail.value.id}/sids/${sidId}`)
    ElMessage.success('已取消授权')
    loadCustomerSids()
  } catch { /* */ }
}

const loadCustomerSids = async () => {
  if (!detail.value.id) return
  try { const r = await http.get(`/admin/customer/${detail.value.id}/sids`); assignedSids.value = r.data || [] } catch { assignedSids.value = [] }
}

const copyToClipboard = text => {
  const fallback = () => {
    const el = document.createElement('textarea')
    el.value = text
    el.style.position = 'fixed'
    el.style.opacity = '0'
    document.body.appendChild(el)
    el.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(el)
    if (ok) ElMessage.success('已复制')
    else ElMessage.error('复制失败，请手动复制')
  }
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制')).catch(fallback)
  } else {
    fallback()
  }
}

// ===== Batch Import Countries & Prices =====
const batchImportDialog = ref(false)
const batchImportText = ref('')
const batchImporting = ref(false)
const batchImportResult = ref(null)

const doBatchImport = async () => {
  const lines = batchImportText.value.trim().split('\n').filter(l => l.trim())
  if (lines.length === 0) { ElMessage.warning('请输入导入数据'); return }

  batchImporting.value = true
  batchImportResult.value = null
  let successCount = 0; let failCount = 0; const errors = []

  // 加载全部国家用于校验
  try { if (allCountries.value.length === 0) await loadAllCountries() } catch {}

  for (const line of lines) {
    const parts = line.trim().split(',').map(s => s.trim())
    const countryCode = parts[0]?.toUpperCase()
    if (!countryCode) { failCount++; errors.push(`"${line}" — 国家代码为空`); continue }

    // 校验国家代码是否有效
    const country = allCountries.value.find(c => c.countryCode === countryCode)
    if (!country) { failCount++; errors.push(`"${countryCode}" — 未找到该国家`); continue }

    // 开通国家（忽略已开通的重复错误）
    try {
      await http.post(`/admin/customer/${detail.value.id}/countries`, null, {
        params: { countryCode }
      })
    } catch { /* 可能已开通，忽略 */ }

    // 如果有报价数据
    if (parts.length >= 3 && parts[1] && parts[2]) {
      const smsAttribute = parseInt(parts[1])
      const price = parts[2]
      const currency = parts[3] || 'USD'
      if (isNaN(smsAttribute) || smsAttribute < 1 || smsAttribute > 4) {
        failCount++; errors.push(`"${line}" — 短信类型无效(1-4)`); continue
      }
      try {
        await http.post(`/admin/customer/${detail.value.id}/prices`, { countryCode, smsAttribute, price, currency })
        successCount++
      } catch { failCount++; errors.push(`"${line}" — 报价保存失败`) }
    } else {
      successCount++
    }
  }

  loadCustomerCountries()
  loadAllPrices()
  const msg = `导入完成：成功 ${successCount} 条` + (failCount > 0 ? `，失败 ${failCount} 条\n${errors.join('\n')}` : '')
  batchImportResult.value = { type: failCount > 0 ? 'warning' : 'success', msg }
  batchImporting.value = false
}

onMounted(loadData)
</script>

<style scoped>
/* ── Page Header ── */
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 700; color: #111827; margin: 0; }
.page-desc { font-size: 13px; color: #6b7280; margin-top: 2px; }
.card-title { font-size: 14px; font-weight: 600; color: #1f2937; }

/* ── Filter Bar ── */
.filter-bar { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; }
.filter-row { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-item label { font-size: 12px; color: #6b7280; font-weight: 500; }
.filter-actions { display: flex; gap: 8px; margin-left: auto; padding-top: 20px; }

/* ── Table ── */
.table-card { border-radius: 8px; }
.mono-code { font-family: 'Courier New', monospace; font-size: 13px; font-weight: 600; background: #f3f4f6; padding: 2px 8px; border-radius: 4px; color: #1f2937; }

/* ── Detail Header ── */
.detail-header { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 8px; padding: 16px 24px; margin-bottom: 16px; border: 1px solid #e5e7eb; }
.detail-title { font-size: 18px; font-weight: 700; color: #111827; margin: 0; }
.detail-subtitle { font-size: 13px; color: #6b7280; margin-top: 2px; }

/* ── Detail Tabs ── */
.detail-tabs { border-radius: 8px; min-height: 500px; }
:deep(.detail-tabs .el-tabs__header) { background: #f9fafb; border-radius: 8px 8px 0 0; }
:deep(.detail-tabs .el-tabs__item) { font-weight: 500; height: 48px; line-height: 48px; }
:deep(.detail-tabs .el-tabs__content) { padding: 24px; }

/* ── Form Sections ── */
.form-section { margin-bottom: 32px; padding-bottom: 4px; border-bottom: 1px dashed #e5e7eb; }
.form-section:last-child { border-bottom: none; }
.form-section-title { font-size: 14px; font-weight: 700; color: #1f2937; padding-bottom: 12px; border-bottom: 2px solid #e5e7eb; margin-bottom: 18px; display: flex; align-items: center; gap: 8px; }
.section-bar { display: inline-block; width: 4px; height: 16px; background: #2563eb; border-radius: 2px; }
.form-hint { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.readonly-input :deep(.el-input__wrapper) { background-color: #f5f7fa; box-shadow: 0 0 0 1px #dcdfe6 inset; cursor: default; }
.readonly-input :deep(.el-input__inner) { color: #606266; cursor: default; }
.form-actions { display: flex; gap: 10px; padding-top: 20px; border-top: 2px solid #e5e7eb; margin-top: 8px; }

/* ── Country Price Layout ── */
.country-price-layout { display: flex; gap: 16px; min-height: 400px; }
.country-list-panel { width: 260px; border: 1px solid #e5e7eb; border-radius: 8px; flex-shrink: 0; overflow: hidden; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #f9fafb; border-bottom: 1px solid #e5e7eb; font-weight: 600; font-size: 12px; color: #4b5563; }
.country-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; cursor: pointer; border-bottom: 1px solid #f3f4f6; transition: background .1s; font-size: 13px; }
.country-item:hover { background: #f9fafb; }
.country-item.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
.price-panel { flex: 1; min-width: 0; border: 1px solid #e5e7eb; border-radius: 8px; }
.price-panel-header { padding: 12px 16px; background: #f9fafb; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; }
.empty-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 300px; color: #d1d5db; }
.empty-placeholder p { margin-top: 12px; font-size: 14px; }
</style>
