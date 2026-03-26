<template>
  <div class="finance-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-title">财务结算</div>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- Tab1: 利润报表 -->
        <el-tab-pane label="利润报表" name="profit">
          <div class="filter-bar">
            <el-date-picker v-model="profitFilter.startDate" type="date" placeholder="开始时间" style="width: 180px" />
            <el-date-picker v-model="profitFilter.endDate" type="date" placeholder="结束时间" style="width: 180px" />
            <el-select v-model="profitFilter.customer" placeholder="全部客户" clearable filterable style="width: 180px">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
            <el-select v-model="profitFilter.country" placeholder="全部国家" clearable filterable style="width: 180px">
              <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} (${c.code})`" :value="c.code" />
            </el-select>
            <el-select v-model="profitFilter.vendor" placeholder="全部供应商" clearable filterable style="width: 180px">
              <el-option v-for="v in vendorOptions" :key="v.id" :label="v.name" :value="v.id" />
            </el-select>
            <el-select v-model="profitFilter.granularity" style="width: 120px">
              <el-option label="天" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
            </el-select>
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="handleResetProfit">重置</el-button>
          </div>

          <!-- Stat Cards -->
          <div class="stat-cards">
            <div class="stat-card">
              <div class="stat-title">总收入</div>
              <div class="stat-value" style="color: #1890ff">${{ profitSummaryData.totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
              <div class="stat-footer"><span class="up"></span> 查询区间</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">总成本</div>
              <div class="stat-value" style="color: #ff4d4f">${{ profitSummaryData.totalCost.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
              <div class="stat-footer"><span class="up"></span> 查询区间</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">总利润</div>
              <div class="stat-value" style="color: #52c41a">${{ profitSummaryData.totalProfit.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
              <div class="stat-footer"><span class="up"></span> 查询区间</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">利润率</div>
              <div class="stat-value" style="color: #722ed1">{{ profitSummaryData.profitRate }}%</div>
              <div class="stat-footer"><span class="up"></span> 查询区间</div>
            </div>
          </div>

          <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
            <el-button @click="handleExportReport">导出报表</el-button>
          </div>

          <el-table :data="profitData" style="width: 100%; font-size: 13px" :header-cell-style="headerCellStyle">
            <el-table-column prop="date" label="日期" min-width="100" />
            <el-table-column prop="customer" label="客户" min-width="100" />
            <el-table-column prop="country" label="国家" min-width="70" />
            <el-table-column prop="vendor" label="供应商" min-width="90" />
            <el-table-column prop="channel" label="通道" min-width="120" />
            <el-table-column prop="type" label="类型" min-width="70" />
            <el-table-column prop="sendCount" label="发送量" min-width="90" />
            <el-table-column prop="segments" label="段数" min-width="90" />
            <el-table-column prop="revenue" label="收入" min-width="90" />
            <el-table-column prop="cost" label="成本" min-width="90" />
            <el-table-column prop="profit" label="利润" min-width="90" />
            <el-table-column label="利润率" min-width="80">
              <template #default="{ row }">
                <span class="profit-high">{{ row.profitRate }}</span>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="profitPage.current"
            v-model:page-size="profitPage.size"
            :total="profitPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- Tab2: 账单管理 -->
        <el-tab-pane label="账单管理" name="billing">
          <div class="filter-bar">
            <el-select v-model="billingFilter.customer" placeholder="全部" clearable filterable style="width: 180px">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.companyName || c.customerName" :value="c.id" />
            </el-select>
            <el-select v-model="billingFilter.status" placeholder="全部" clearable style="width: 180px">
              <el-option label="draft" value="draft" />
              <el-option label="issued" value="issued" />
              <el-option label="paid" value="paid" />
              <el-option label="overdue" value="overdue" />
            </el-select>
            <el-button type="primary" @click="handleQuery">查询</el-button>
          </div>

          <el-table :data="billingData" style="width: 100%; font-size: 13px" :header-cell-style="headerCellStyle">
            <el-table-column prop="invoiceNo" label="账单号" min-width="150" />
            <el-table-column prop="customer" label="客户" min-width="100" />
            <el-table-column prop="period" label="账期" min-width="200" />
            <el-table-column prop="sendCount" label="发送量" min-width="100" />
            <el-table-column prop="amount" label="金额" min-width="110" />
            <el-table-column prop="currency" label="币种" min-width="70" />
            <el-table-column label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag :type="billingStatusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="220">
              <template #default="{ row }">
                <el-button size="small" @click="handleViewBill(row)">查看</el-button>
                <el-button v-if="row.status === 'issued' || row.status === 'overdue'" size="small" @click="handleResendBill(row)">重发</el-button>
                <el-button v-if="row.status === 'draft'" size="small" type="primary" @click="handleSendBill(row)">发送账单</el-button>
                <el-button size="small" @click="handleExportPdf(row)">导出PDF</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="billingPage.current"
            v-model:page-size="billingPage.size"
            :total="billingPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- Tab3: 客户账户 -->
        <el-tab-pane label="客户账户" name="account">
          <div class="filter-bar">
            <el-select v-model="accountFilter.payType" placeholder="全部" clearable style="width: 180px">
              <el-option label="预付费" value="prepaid" />
              <el-option label="后付费" value="postpaid" />
            </el-select>
            <el-button type="primary" @click="handleQuery">查询</el-button>
          </div>

          <el-table :data="accountData" style="width: 100%; font-size: 13px" :header-cell-style="headerCellStyle">
            <el-table-column prop="customer" label="客户" min-width="100" />
            <el-table-column label="付费类型" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.payType === '预付费' ? '' : 'warning'" size="small">{{ row.payType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="余额" min-width="120">
              <template #default="{ row }">
                <span :style="{ fontWeight: 600, color: row.balanceColor }">{{ row.balance }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="frozen" label="冻结金额" min-width="100" />
            <el-table-column prop="creditLimit" label="信用额度" min-width="110" />
            <el-table-column prop="currency" label="币种" min-width="70" />
            <el-table-column prop="lowThreshold" label="低余额阈值" min-width="110" />
            <el-table-column label="操作" min-width="160">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openRechargeDialog(row)">充值</el-button>
                <el-button size="small" @click="handleAdjust(row)">调账</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab4: 供应商结算价 -->
        <el-tab-pane label="供应商结算价" name="supplierPrice">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px">
            <div class="filter-bar" style="margin-bottom: 0">
              <el-select v-model="supplierFilter.vendor" placeholder="全部" clearable filterable style="width: 180px">
                <el-option v-for="v in vendorOptions" :key="v.id" :label="v.name" :value="v.id" />
              </el-select>
              <el-select v-model="supplierFilter.country" placeholder="全部" clearable filterable style="width: 180px">
                <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} (${c.code})`" :value="c.code" />
              </el-select>
              <el-button type="primary" @click="handleQuery">查询</el-button>
              <el-button @click="handleResetSupplier">重置</el-button>
            </div>
            <el-button @click="importCostVisible = true">导入成本 (Rate Card)</el-button>
          </div>

          <el-table :data="supplierPriceData" style="width: 100%; font-size: 13px" :header-cell-style="headerCellStyle">
            <el-table-column prop="vendor" label="供应商" min-width="100" />
            <el-table-column prop="country" label="国家" min-width="70" />
            <el-table-column prop="channel" label="通道" min-width="130" />
            <el-table-column prop="type" label="类型" min-width="70" />
            <el-table-column prop="price" label="结算价" min-width="90" />
            <el-table-column prop="currency" label="币种" min-width="70" />
            <el-table-column prop="effectDate" label="生效日" min-width="110" />
            <el-table-column label="操作" min-width="140">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditPrice(row)">编辑</el-button>
                <el-button size="small" @click="handlePriceHistory(row)">历史</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="supplierPage.current"
            v-model:page-size="supplierPage.size"
            :total="supplierPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- Tab5: 汇率管理 -->
        <el-tab-pane label="汇率管理" name="exchange">
          <div style="display: flex; justify-content: flex-end; margin-bottom: 16px">
            <el-button type="primary" @click="handleAddExchange">+ 新增汇率</el-button>
          </div>

          <el-table :data="exchangeData" style="width: 100%; font-size: 13px" :header-cell-style="headerCellStyle">
            <el-table-column prop="sourceCurrency" label="源币种" min-width="90" />
            <el-table-column prop="targetCurrency" label="目标币种" min-width="90" />
            <el-table-column prop="rate" label="汇率" min-width="120" />
            <el-table-column prop="effectDate" label="生效日" min-width="110" />
            <el-table-column prop="updatedBy" label="更新人" min-width="110" />
            <el-table-column label="操作" min-width="140">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditExchange(row)">编辑</el-button>
                <el-button size="small" @click="handleExchangeHistory(row)">历史</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Recharge Dialog -->
    <el-dialog v-model="rechargeVisible" title="账户充值" width="480px" :close-on-click-modal="true">
      <el-form label-width="100px">
        <el-form-item label="客户">
          <el-input v-model="rechargeForm.customer" disabled />
        </el-form-item>
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeForm.amount" :min="0" :precision="2" placeholder="请输入充值金额" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币种">
          <el-select v-model="rechargeForm.currency" style="width: 100%" disabled>
            <el-option label="USD — 美元" value="USD" />
            <el-option label="HKD — 港币" value="HKD" />
            <el-option label="CNY — 人民币" value="CNY" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="rechargeForm.payMethod" style="width: 100%">
            <el-option label="银行转账" value="bank" />
            <el-option label="PayPal" value="paypal" />
            <el-option label="信用卡" value="credit" />
            <el-option label="线下支付" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易凭证号">
          <el-input v-model="rechargeForm.transactionNo" placeholder="请输入银行流水号或交易凭证" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="3" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- Edit Dialog (generic) -->
    <el-dialog v-model="editVisible" :title="editTitle" width="520px" :close-on-click-modal="true">
      <el-form label-width="100px">
        <el-form-item label="值">
          <el-input v-model="editForm.value" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="editForm.effectDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmEdit">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="600px" :close-on-click-modal="true">
      <p style="color: #999; text-align: center; padding: 40px 0">详细信息展示区域（接入真实数据后动态渲染）</p>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Import Cost Dialog -->
    <el-dialog
      v-model="importCostVisible"
      title="导入供应商成本"
      width="780px"
      :close-on-click-modal="false"
      @close="handleCloseCostImport"
    >
      <template #header>
        <div>
          <span style="font-size: 16px; font-weight: 600">导入供应商成本</span>
          <div style="font-size: 12px; color: #999; margin-top: 2px">上传供应商 Rate Card（MCC/MNC 格式），自动映射通道成本价并立即生效</div>
        </div>
      </template>

      <!-- Steps -->
      <div class="cost-steps">
        <div :class="['cost-step', costStep === 1 ? 'active' : costStep > 1 ? 'done' : '']">
          <div class="cost-step-circle">1</div>
          <div class="cost-step-label">上传文件</div>
        </div>
        <div :class="['cost-step-line', costStep > 1 ? 'done' : '']"></div>
        <div :class="['cost-step', costStep === 2 ? 'active' : costStep > 2 ? 'done' : '']">
          <div class="cost-step-circle">2</div>
          <div class="cost-step-label">预览确认</div>
        </div>
        <div :class="['cost-step-line', costStep > 2 ? 'done' : '']"></div>
        <div :class="['cost-step', costStep === 3 ? 'active' : '']">
          <div class="cost-step-circle">3</div>
          <div class="cost-step-label">导入结果</div>
        </div>
      </div>

      <!-- Step 1: Upload -->
      <div v-show="costStep === 1">
        <div style="display: flex; gap: 12px; align-items: flex-end; margin-bottom: 16px; padding: 14px; background: #fafafa; border-radius: 8px; border: 1px solid #e8e8e8">
          <div style="flex: 1">
            <div class="form-label-custom"><span style="color: #ff4d4f">*</span> 供应商</div>
            <el-select v-model="costForm.vendor" placeholder="— 请选择供应商 —" filterable style="width: 100%">
              <el-option v-for="v in vendorOptions" :key="v.id" :label="v.name" :value="v.id" />
            </el-select>
          </div>
          <div style="flex: 1">
            <div class="form-label-custom">生效日期（留空取文件中日期）</div>
            <el-date-picker v-model="costForm.effectDate" type="date" style="width: 100%" />
          </div>
          <div style="flex: 1">
            <div class="form-label-custom">冲突策略</div>
            <el-select v-model="costForm.conflict" style="width: 100%">
              <el-option label="覆盖（用文件中的新价格）" value="overwrite" />
              <el-option label="跳过（保留现有成本价）" value="skip" />
            </el-select>
          </div>
        </div>

        <!-- Upload Zone -->
        <el-upload
          ref="costUploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleCostFileChange"
          :on-remove="handleCostFileRemove"
        >
          <div style="font-size: 36px; margin-bottom: 8px">&#128202;</div>
          <div style="font-size: 14px; font-weight: 500; color: #333">点击或拖拽上传 Rate Card 文件</div>
          <div style="font-size: 12px; color: #bbb; margin-top: 4px">支持 .xlsx / .xls 格式 / 最大 10 MB</div>
        </el-upload>

        <!-- Rate Card Format Reference -->
        <div style="margin-top: 16px; border: 1px solid #e8e8e8; border-radius: 8px; overflow: hidden">
          <div style="padding: 10px 14px; background: #fafafa; border-bottom: 1px solid #e8e8e8; font-size: 12px; font-weight: 600; color: #666">Rate Card 标准列格式</div>
          <el-table :data="rateCardSample" style="font-size: 12px" :header-cell-style="headerCellStyle" :show-overflow-tooltip="true">
            <el-table-column prop="mcc" label="MCC" width="60" />
            <el-table-column prop="mnc" label="MNC" width="60" />
            <el-table-column prop="dialCode" label="Dial code" width="80" />
            <el-table-column prop="country" label="Country" width="100" />
            <el-table-column prop="network" label="Network" min-width="120" />
            <el-table-column prop="effectiveDate" label="Effective date" width="110" />
            <el-table-column label="Rate" width="90">
              <template #default="{ row }">
                <span style="font-family: monospace; color: #52c41a; font-weight: 600">{{ row.rate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Change type" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.changeType === 'Decrease' ? '#ff4d4f' : '#333', fontWeight: row.changeType === 'Decrease' ? 500 : 400 }">{{ row.changeType }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div style="padding: 8px 14px; font-size: 11px; color: #999; background: #fafafa; border-top: 1px solid #e8e8e8">
            支持 Rate List / Timemasks 多 Sheet . MCC+MNC 将自动匹配平台中的通道，无法匹配的行将标注「未映射」
          </div>
        </div>
        <el-button link type="primary" style="margin-top: 10px; font-size: 12px" @click="ElMessage.success('模板下载中...')">下载标准模板</el-button>
      </div>

      <!-- Step 2: Preview -->
      <div v-show="costStep === 2">
        <div style="padding: 12px 0 12px 0; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px">
          <div style="font-size: 13px; color: #666">
            解析完成 . 共 <strong>{{ costMockRows.length }}</strong> 条 .
            <span style="color: #ff4d4f">涨价 <strong>{{ costStats.up }}</strong></span> .
            <span style="color: #52c41a">降价 <strong>{{ costStats.down }}</strong></span> .
            <span style="color: #999">不变 <strong>{{ costStats.same }}</strong></span> .
            <span style="color: #faad14">未映射 <strong>{{ costStats.unmap }}</strong></span>
          </div>
          <div style="display: flex; gap: 6px">
            <el-button size="small" :type="costPreviewFilter === 'all' ? 'primary' : ''" @click="costPreviewFilter = 'all'">全部</el-button>
            <el-button size="small" :type="costPreviewFilter === 'change' ? 'primary' : ''" @click="costPreviewFilter = 'change'">仅变化</el-button>
            <el-button size="small" :type="costPreviewFilter === 'unmap' ? 'primary' : ''" @click="costPreviewFilter = 'unmap'">未映射</el-button>
          </div>
        </div>
        <el-table :data="filteredCostRows" style="font-size: 12px" max-height="380" :header-cell-style="headerCellStyle">
          <el-table-column label="MCC/MNC" width="90">
            <template #default="{ row }">
              <span style="font-family: monospace; color: #666">{{ row.mcc }}/{{ row.mnc }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="country" label="国家" width="80" />
          <el-table-column prop="network" label="运营商" min-width="130">
            <template #default="{ row }">
              <span style="color: #666">{{ row.network }}</span>
            </template>
          </el-table-column>
          <el-table-column label="文件成本价" width="110" align="right">
            <template #default="{ row }">
              <span style="font-family: monospace; font-weight: 600">{{ row.fileRate }}</span>
            </template>
          </el-table-column>
          <el-table-column label="当前成本价" width="110" align="right">
            <template #default="{ row }">
              <span style="font-family: monospace; color: #999">{{ row.curRate || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="变动" width="90" align="center">
            <template #default="{ row }">
              <span :style="{ fontSize: '12px', fontWeight: 500, color: changeColor(row.changeType) }">{{ changeLabel(row.changeType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="映射通道" min-width="120">
            <template #default="{ row }">
              <span v-if="row.mapped" style="font-family: monospace; color: #1890ff; font-size: 11px">{{ row.channel }}</span>
              <span v-else style="color: #faad14; font-size: 11px">未映射</span>
            </template>
          </el-table-column>
          <el-table-column prop="effectDate" label="生效日期" width="100">
            <template #default="{ row }">
              <span style="color: #999; font-size: 11px">{{ row.effectDate }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="padding: 10px 0; font-size: 12px; color: #faad14; background: #fffbe6; border-radius: 4px; margin-top: 8px; padding-left: 12px">
          确认后成本价立即更新至对应通道，影响后续利润计算，不影响历史账单。
        </div>
      </div>

      <!-- Step 3: Result -->
      <div v-show="costStep === 3" style="text-align: center; padding: 40px 0">
        <div style="font-size: 48px; margin-bottom: 12px">&#9989;</div>
        <div style="font-size: 16px; font-weight: 600; margin-bottom: 6px">成本导入成功</div>
        <div style="font-size: 13px; color: #999; margin-bottom: 20px">以下通道成本价已更新并立即生效</div>
        <div style="display: flex; gap: 12px; justify-content: center; flex-wrap: wrap">
          <div style="background: #f6ffed; border-radius: 8px; padding: 14px 22px; text-align: center; min-width: 90px">
            <div style="font-size: 24px; font-weight: 700; color: #52c41a">{{ costResultMapped }}</div>
            <div style="font-size: 12px; color: #999; margin-top: 2px">通道已更新</div>
          </div>
          <div style="background: #fffbe6; border-radius: 8px; padding: 14px 22px; text-align: center; min-width: 90px">
            <div style="font-size: 24px; font-weight: 700; color: #faad14">{{ costResultUnmap }}</div>
            <div style="font-size: 12px; color: #999; margin-top: 2px">未映射跳过</div>
          </div>
          <div style="background: #e6f7ff; border-radius: 8px; padding: 14px 22px; text-align: center; min-width: 90px">
            <div style="font-size: 24px; font-weight: 700; color: #1890ff">{{ costResultChanged }}</div>
            <div style="font-size: 12px; color: #999; margin-top: 2px">价格有变化</div>
          </div>
        </div>
        <div style="margin-top: 16px">
          <el-button link type="warning" @click="ElMessage.success('未映射明细已下载')">下载未映射明细</el-button>
        </div>
      </div>

      <template #footer>
        <el-button v-if="costStep > 1 && costStep < 3" @click="costStep--">返回</el-button>
        <el-button v-if="costStep < 3" @click="handleCloseCostImport">取消</el-button>
        <el-button
          type="primary"
          :disabled="costStep === 1 && (!costForm.vendor || !costFileReady)"
          @click="handleCostNext"
        >
          {{ costStep === 1 ? '下一步' : costStep === 2 ? '确认导入' : '完成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  profitSummary, profitReport,
  billPage, billIssue, billPay, billDetail,
  accountList,
  vendorPricePage, vendorPriceSave, vendorPriceUpdate, vendorPriceDel,
  exchangeRateList, exchangeRateSave, exchangeRateUpdate, exchangeRateDel,
} from '../api'
import { useRefData } from '../stores/refData'

const refData = useRefData()

// Header cell style
const headerCellStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

// Reference data
const customerOptions = ref([])
const countryOptions = ref([])
const vendorOptions = ref([])
const loadRefData = async () => {
  const [cu, co, v] = await Promise.all([refData.loadCustomers(), refData.loadCountries(), refData.loadVendors()])
  customerOptions.value = cu; countryOptions.value = co; vendorOptions.value = v
}

// Helper: format date to YYYY-MM-DD
const fmtDate = (d) => {
  if (!d) return ''
  const dt = new Date(d)
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// Helper: format number as $x,xxx.xx
const fmtMoney = (v, prefix = '$') => {
  if (v == null) return `${prefix}0.00`
  const n = Number(v)
  return prefix + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// Active tab
const activeTab = ref('profit')

// ==================== Tab1: Profit Report ====================
const profitFilter = reactive({
  startDate: '2026-03-01',
  endDate: '2026-03-14',
  customer: '',
  country: '',
  vendor: '',
  granularity: 'day'
})

const profitPage = reactive({ current: 1, size: 10, total: 0 })

// Stat cards data
const profitSummaryData = reactive({
  totalRevenue: 0,
  totalCost: 0,
  totalProfit: 0,
  profitRate: 0
})

const profitData = ref([])

const loadProfitSummary = async () => {
  try {
    const res = await profitSummary({
      start: fmtDate(profitFilter.startDate),
      end: fmtDate(profitFilter.endDate),
      customerId: profitFilter.customer || undefined,
      countryCode: profitFilter.country || undefined,
      vendorId: profitFilter.vendor || undefined
    })
    const d = res.data || {}
    profitSummaryData.totalRevenue = d.totalRevenue || 0
    profitSummaryData.totalCost = d.totalCost || 0
    profitSummaryData.totalProfit = d.totalProfit || 0
    profitSummaryData.profitRate = d.profitRate || 0
  } catch (e) {
    console.error('loadProfitSummary error', e)
  }
}

const loadProfitReport = async () => {
  try {
    const res = await profitReport({
      start: fmtDate(profitFilter.startDate),
      end: fmtDate(profitFilter.endDate),
      customerId: profitFilter.customer || undefined,
      countryCode: profitFilter.country || undefined,
      vendorId: profitFilter.vendor || undefined,
      granularity: profitFilter.granularity,
      page: profitPage.current,
      size: profitPage.size
    })
    const d = res.data || {}
    const list = d.list || []
    // Map API fields to template prop names
    profitData.value = list.map(r => ({
      date: r.date,
      customer: r.customerId,
      country: r.countryCode,
      vendor: r.vendorId,
      channel: r.channelId,
      type: r.smsAttribute || '',
      sendCount: (r.msgCount ?? 0).toLocaleString(),
      segments: (r.segCount ?? 0).toLocaleString(),
      revenue: fmtMoney(r.revenue),
      cost: fmtMoney(r.cost),
      profit: fmtMoney(r.profit),
      profitRate: r.profitRate != null ? r.profitRate + '%' : '0%'
    }))
    profitPage.total = d.total || 0
  } catch (e) {
    console.error('loadProfitReport error', e)
  }
}

const loadProfit = async () => {
  await Promise.all([loadProfitSummary(), loadProfitReport()])
}

const handleResetProfit = () => {
  profitFilter.customer = ''
  profitFilter.country = ''
  profitFilter.vendor = ''
  profitFilter.granularity = 'day'
  profitPage.current = 1
  loadProfit()
  ElMessage.success('已重置筛选条件')
}

const handleExportReport = () => {
  ElMessage.success('导出任务已提交，稍后将通过邮件通知')
}

// Watch profit pagination
watch(() => profitPage.current, () => loadProfitReport())
watch(() => profitPage.size, () => { profitPage.current = 1; loadProfitReport() })

// ==================== Tab2: Billing ====================
const billingFilter = reactive({ customer: '', status: '' })
const billingPage = reactive({ current: 1, size: 10, total: 0 })

const billingData = ref([])

const loadBilling = async () => {
  try {
    const res = await billPage({
      customerId: billingFilter.customer || undefined,
      status: billingFilter.status || undefined,
      page: billingPage.current,
      size: billingPage.size
    })
    const d = res.data || {}
    const list = d.list || []
    billingData.value = list.map(r => ({
      id: r.id,
      invoiceNo: r.billNo,
      customer: r.customerId,
      period: (r.periodStart || '') + ' ~ ' + (r.periodEnd || ''),
      sendCount: ((r.totalMessages ?? 0) + (r.totalSegments ?? 0)).toLocaleString(),
      amount: fmtMoney(r.amount),
      currency: r.currency || 'USD',
      status: r.status
    }))
    billingPage.total = d.total || 0
  } catch (e) {
    console.error('loadBilling error', e)
  }
}

const billingStatusType = (status) => {
  const map = { paid: 'success', issued: '', overdue: 'danger', draft: 'info' }
  return map[status] || 'info'
}

const handleViewBill = async (row) => {
  detailTitle.value = '账单详情 - ' + row.invoiceNo
  detailVisible.value = true
  if (row.id) {
    try {
      await billDetail(row.id)
      // Detail dialog currently shows placeholder; data can be rendered later
    } catch (e) {
      console.error('billDetail error', e)
    }
  }
}

const handleResendBill = (row) => {
  ElMessageBox.confirm('确认要重新发送账单 "' + row.invoiceNo + '" 吗？', '提示').then(async () => {
    try {
      await billIssue(row.id)
      ElMessage.success('重新发送账单操作成功')
      loadBilling()
    } catch (e) {
      ElMessage.error('操作失败: ' + (e.message || '未知错误'))
    }
  }).catch(() => {})
}

const handleSendBill = (row) => {
  ElMessageBox.confirm('确认要发送账单 "' + row.invoiceNo + '" 吗？', '提示').then(async () => {
    try {
      await billIssue(row.id)
      ElMessage.success('发送账单操作成功')
      loadBilling()
    } catch (e) {
      ElMessage.error('操作失败: ' + (e.message || '未知错误'))
    }
  }).catch(() => {})
}

const handleExportPdf = () => {
  ElMessage.success('PDF导出中，稍后将自动下载')
}

// Watch billing pagination
watch(() => billingPage.current, () => loadBilling())
watch(() => billingPage.size, () => { billingPage.current = 1; loadBilling() })

// ==================== Tab3: Account ====================
const accountFilter = reactive({ payType: '' })

const accountData = ref([])

const loadAccount = async () => {
  try {
    const res = await accountList({
      paymentType: accountFilter.payType || undefined
    })
    const list = res.data || []
    accountData.value = list.map(r => {
      const bal = Number(r.balance) || 0
      const payLabel = r.paymentType === 'prepaid' ? '预付费' : r.paymentType === 'postpaid' ? '后付费' : (r.paymentType || '')
      return {
        customerId: r.customerId,
        customer: r.customerName || r.customerId,
        payType: payLabel,
        balance: fmtMoney(bal),
        balanceColor: bal < 0 ? '#333' : bal < 500 ? '#ff4d4f' : '#52c41a',
        frozen: fmtMoney(r.frozenAmount),
        creditLimit: r.creditLimit ? fmtMoney(r.creditLimit) : '-',
        currency: r.currency || 'USD',
        lowThreshold: r.lowBalanceThreshold ? fmtMoney(r.lowBalanceThreshold) : '-'
      }
    })
  } catch (e) {
    console.error('loadAccount error', e)
  }
}

// Recharge Dialog
const rechargeVisible = ref(false)
const rechargeForm = reactive({
  customer: '',
  amount: null,
  currency: 'USD',
  payMethod: 'bank',
  transactionNo: '',
  remark: ''
})

const openRechargeDialog = (row) => {
  rechargeForm.customer = row.customer
  rechargeForm.amount = null
  rechargeForm.currency = row.currency || 'USD'
  rechargeForm.payMethod = 'bank'
  rechargeForm.transactionNo = ''
  rechargeForm.remark = ''
  rechargeVisible.value = true
}

const handleConfirmRecharge = () => {
  rechargeVisible.value = false
  ElMessage.success('充值成功')
  loadAccount()
}

const handleAdjust = (row) => {
  editTitle.value = '调账 - ' + row.customer
  editContext.value = { type: 'adjust', customerId: row.customerId }
  editForm.value = ''
  editForm.effectDate = ''
  editForm.remark = ''
  editVisible.value = true
}

// ==================== Tab4: Supplier Price ====================
const supplierFilter = reactive({ vendor: '', country: '' })
const supplierPage = reactive({ current: 1, size: 10, total: 0 })

const supplierPriceData = ref([])

const loadSupplierPrice = async () => {
  try {
    const res = await vendorPricePage({
      vendorId: supplierFilter.vendor || undefined,
      countryCode: supplierFilter.country || undefined,
      page: supplierPage.current,
      size: supplierPage.size
    })
    const d = res.data || {}
    const list = d.list || []
    supplierPriceData.value = list.map(r => ({
      id: r.id,
      vendor: r.vendorId,
      country: r.countryCode,
      channel: r.channelId,
      type: r.smsAttribute || '',
      price: fmtMoney(r.price),
      currency: r.currency || 'USD',
      effectDate: r.effectiveDate || ''
    }))
    supplierPage.total = d.total || 0
  } catch (e) {
    console.error('loadSupplierPrice error', e)
  }
}

const handleResetSupplier = () => {
  supplierFilter.vendor = ''
  supplierFilter.country = ''
  supplierPage.current = 1
  loadSupplierPrice()
  ElMessage.success('已重置筛选条件')
}

// Track which row is being edited for supplier price
const editContext = ref(null)

const handleEditPrice = (row) => {
  editTitle.value = '编辑 - ' + row.vendor + ': ' + row.channel
  editContext.value = { type: 'vendorPrice', id: row.id, vendorId: row.vendor, countryCode: row.country, channelId: row.channel }
  editForm.value = ''
  editForm.effectDate = ''
  editForm.remark = ''
  editVisible.value = true
}

const handlePriceHistory = (row) => {
  detailTitle.value = '历史记录 - ' + row.vendor
  detailVisible.value = true
}

// Watch supplier pagination
watch(() => supplierPage.current, () => loadSupplierPrice())
watch(() => supplierPage.size, () => { supplierPage.current = 1; loadSupplierPrice() })

// ==================== Tab5: Exchange Rate ====================
const exchangeData = ref([])

const loadExchangeRate = async () => {
  try {
    const res = await exchangeRateList()
    const list = res.data || []
    exchangeData.value = list.map(r => ({
      id: r.id,
      sourceCurrency: r.sourceCurrency,
      targetCurrency: r.targetCurrency,
      rate: r.rate,
      effectDate: r.effectiveDate || '',
      updatedBy: r.updatedBy || ''
    }))
  } catch (e) {
    console.error('loadExchangeRate error', e)
  }
}

const handleAddExchange = () => {
  editTitle.value = '新增汇率'
  editContext.value = { type: 'exchangeRate', id: null }
  editForm.value = ''
  editForm.effectDate = ''
  editForm.remark = ''
  editVisible.value = true
}

const handleEditExchange = (row) => {
  editTitle.value = '编辑 - 汇率: ' + row.sourceCurrency + '/' + row.targetCurrency
  editContext.value = { type: 'exchangeRate', id: row.id, sourceCurrency: row.sourceCurrency, targetCurrency: row.targetCurrency }
  editForm.value = String(row.rate)
  editForm.effectDate = row.effectDate
  editForm.remark = ''
  editVisible.value = true
}

const handleExchangeHistory = (row) => {
  detailTitle.value = '历史记录 - ' + row.sourceCurrency + '/' + row.targetCurrency
  detailVisible.value = true
}

// ==================== Generic Dialogs ====================
const editVisible = ref(false)
const editTitle = ref('编辑')
const editForm = reactive({ value: '', effectDate: '', remark: '' })

const handleConfirmEdit = async () => {
  const ctx = editContext.value
  try {
    if (ctx && ctx.type === 'exchangeRate') {
      const payload = {
        sourceCurrency: ctx.sourceCurrency || '',
        targetCurrency: ctx.targetCurrency || '',
        rate: Number(editForm.value),
        effectiveDate: fmtDate(editForm.effectDate),
        updatedBy: 'admin'
      }
      if (ctx.id) {
        await exchangeRateUpdate(ctx.id, payload)
      } else {
        await exchangeRateSave(payload)
      }
      loadExchangeRate()
    } else if (ctx && ctx.type === 'vendorPrice') {
      const payload = {
        vendorId: ctx.vendorId,
        countryCode: ctx.countryCode,
        channelId: ctx.channelId,
        price: Number(editForm.value),
        effectiveDate: fmtDate(editForm.effectDate)
      }
      if (ctx.id) {
        await vendorPriceUpdate(ctx.id, payload)
      } else {
        await vendorPriceSave(payload)
      }
      loadSupplierPrice()
    }
    editVisible.value = false
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

const detailVisible = ref(false)
const detailTitle = ref('详情')

// ==================== Tab-aware query ====================
const handleQuery = () => {
  if (activeTab.value === 'profit') {
    profitPage.current = 1
    loadProfit()
  } else if (activeTab.value === 'billing') {
    billingPage.current = 1
    loadBilling()
  } else if (activeTab.value === 'account') {
    loadAccount()
  } else if (activeTab.value === 'supplierPrice') {
    supplierPage.current = 1
    loadSupplierPrice()
  } else if (activeTab.value === 'exchange') {
    loadExchangeRate()
  }
}

// ==================== Import Cost (Rate Card) ====================
const importCostVisible = ref(false)
const costStep = ref(1)
const costFileReady = ref(false)
const costPreviewFilter = ref('all')

const costForm = reactive({
  vendor: '',
  effectDate: '',
  conflict: 'overwrite'
})

const costMockRows = ref([
  { mcc: '520', mnc: '18', country: '泰国', network: 'DTAC Thailand', fileRate: '0.019000', curRate: '0.020000', changeType: 'Decrease', effectDate: '2026.02.26', channel: 'TH-DTAC-01', mapped: true },
  { mcc: '520', mnc: '01', country: '泰国', network: 'AIS Thailand', fileRate: '0.021000', curRate: '0.021000', changeType: 'Same', effectDate: '2026.02.26', channel: 'TH-AIS-01', mapped: true },
  { mcc: '520', mnc: '15', country: '泰国', network: 'TRUE Move', fileRate: '0.022000', curRate: '0.021000', changeType: 'Increase', effectDate: '2026.02.26', channel: 'TH-TRUE-01', mapped: true },
  { mcc: '515', mnc: '01', country: '菲律宾', network: 'Globe Telecom', fileRate: '0.004000', curRate: '0.004000', changeType: 'Same', effectDate: '2025.11.27', channel: null, mapped: false },
  { mcc: '724', mnc: '00', country: '巴西', network: 'Nextel (Claro)', fileRate: '0.007500', curRate: '0.008000', changeType: 'Decrease', effectDate: '2026.02.26', channel: null, mapped: false },
  { mcc: '724', mnc: '02', country: '巴西', network: 'TIM', fileRate: '0.007500', curRate: '0.008000', changeType: 'Decrease', effectDate: '2026.02.26', channel: null, mapped: false },
  { mcc: '510', mnc: '01', country: '印度尼西亚', network: 'Telkomsel', fileRate: '0.023000', curRate: '0.024000', changeType: 'Decrease', effectDate: '2026.02.26', channel: 'ID-TKS-01', mapped: true },
  { mcc: '502', mnc: '12', country: '马来西亚', network: 'Maxis', fileRate: '0.031000', curRate: '0.030000', changeType: 'Increase', effectDate: '2026.02.26', channel: 'MY-MAX-01', mapped: true }
])

const costStats = computed(() => {
  const rows = costMockRows.value
  return {
    up: rows.filter(r => r.changeType === 'Increase').length,
    down: rows.filter(r => r.changeType === 'Decrease').length,
    same: rows.filter(r => r.changeType === 'Same').length,
    unmap: rows.filter(r => !r.mapped).length
  }
})

const filteredCostRows = computed(() => {
  if (costPreviewFilter.value === 'change') return costMockRows.value.filter(r => r.changeType !== 'Same')
  if (costPreviewFilter.value === 'unmap') return costMockRows.value.filter(r => !r.mapped)
  return costMockRows.value
})

const costResultMapped = computed(() => costMockRows.value.filter(r => r.mapped).length)
const costResultUnmap = computed(() => costMockRows.value.filter(r => !r.mapped).length)
const costResultChanged = computed(() => costMockRows.value.filter(r => r.mapped && r.changeType !== 'Same').length)

const changeColor = (type) => {
  const map = { Increase: '#ff4d4f', Decrease: '#52c41a', Same: '#999' }
  return map[type] || '#999'
}

const changeLabel = (type) => {
  const map = { Increase: '涨价', Decrease: '降价', Same: '不变' }
  return map[type] || type
}

const rateCardSample = ref([
  { mcc: '515', mnc: '01', dialCode: '+63', country: 'Philippines', network: 'Globe Telecom', effectiveDate: '2025.11.27', rate: '0.004000', changeType: 'Same' },
  { mcc: '724', mnc: '00', dialCode: '+55', country: 'Brazil', network: 'Nextel (Claro)', effectiveDate: '2026.02.26', rate: '0.007500', changeType: 'Decrease' }
])

const handleCostFileChange = (file) => {
  if (file) costFileReady.value = true
}

const handleCostFileRemove = () => {
  costFileReady.value = false
}

const handleCostNext = () => {
  if (costStep.value === 1) {
    costStep.value = 2
  } else if (costStep.value === 2) {
    costStep.value = 3
  } else {
    handleCloseCostImport()
  }
}

const handleCloseCostImport = () => {
  importCostVisible.value = false
  costStep.value = 1
  costFileReady.value = false
  costForm.vendor = ''
  costForm.effectDate = ''
  costForm.conflict = 'overwrite'
  costPreviewFilter.value = 'all'
}

// ==================== Init ====================
onMounted(() => {
  loadRefData()
  loadProfit()
  loadBilling()
  loadAccount()
  loadSupplierPrice()
  loadExchangeRate()
})
</script>

<style scoped>
.finance-page {
  padding: 0;
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: flex-end;
}

/* Stat Cards */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.stat-card .stat-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.stat-card .stat-value {
  font-size: 28px;
  font-weight: 600;
}

.stat-card .stat-footer {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.stat-card .stat-footer .up {
  color: #52c41a;
}

.stat-card .stat-footer .down {
  color: #ff4d4f;
}

/* Profit rate highlight */
.profit-high {
  color: #52c41a;
  font-weight: 600;
}

.profit-low {
  color: #faad14;
  font-weight: 600;
}

.profit-neg {
  color: #ff4d4f;
  font-weight: 600;
}

/* Cost import steps */
.cost-steps {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 16px 0;
}

.cost-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.cost-step-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e8e8e8;
  color: #999;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.cost-step-label {
  font-size: 11px;
  color: #bbb;
  white-space: nowrap;
  transition: all 0.2s;
}

.cost-step.active .cost-step-circle {
  background: #1890ff;
  color: #fff;
}

.cost-step.active .cost-step-label {
  color: #1890ff;
  font-weight: 500;
}

.cost-step.done .cost-step-circle {
  background: #52c41a;
  color: #fff;
}

.cost-step.done .cost-step-label {
  color: #52c41a;
}

.cost-step-line {
  flex: 1;
  height: 2px;
  background: #e8e8e8;
  margin: 0 8px;
  margin-bottom: 16px;
  transition: background 0.2s;
  min-width: 60px;
}

.cost-step-line.done {
  background: #52c41a;
}

.form-label-custom {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  font-weight: 500;
}
</style>
