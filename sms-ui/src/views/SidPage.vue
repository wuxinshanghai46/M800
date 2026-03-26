<template>
  <div>
    <!-- Stats Row — 原型 stats-row -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">SID 总数</div>
          <div class="stat-value blue">{{ total }}</div>
          <div class="stat-sub">跨 {{ countryCount }} 个国家</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">已生效</div>
          <div class="stat-value green">{{ activeCount }}</div>
          <div class="stat-sub">可正常使用</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">审核中</div>
          <div class="stat-value orange">0</div>
          <div class="stat-sub">等待运营商审核</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">30天内到期</div>
          <div class="stat-value red">0</div>
          <div class="stat-sub">需及时续期</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <div class="page-header">
        <div>
          <h2>SID 管理</h2>
          <div class="page-desc">SID 是平台公共资源，由供应商向运营商注册，同一SID可在多条通道分别注册</div>
        </div>
        <el-button plain @click="openBatchEntry">批量录入号码</el-button>
        <el-button type="primary" @click="openForm()">＋ 新建 SID</el-button>
      </div>

      <el-tabs v-model="subTab">
        <!-- Tab 1: SID 主库 -->
        <el-tab-pane label="SID 主库" name="master">
          <div class="filter-bar">
            <div class="filter-item">
              <label>SID 值</label>
              <el-input v-model="query.keyword" placeholder="如 NOTICE" clearable style="width: 160px;" @keyup.enter="loadData" />
            </div>
            <div class="filter-item">
              <label>所属国家</label>
              <el-select v-model="query.countryCode" placeholder="全部" clearable style="width: 140px;">
                <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.code}`" :value="c.code" />
              </el-select>
            </div>
            <div class="filter-item">
              <label>所属供应商</label>
              <el-select v-model="query.vendorId" placeholder="全部" clearable filterable style="width: 160px;">
                <el-option v-for="v in vendors" :key="v.id" :label="v.vendorName" :value="v.id" />
              </el-select>
            </div>
            <div class="filter-item">
              <label>SID 类型</label>
              <el-select v-model="query.sidType" placeholder="全部" clearable style="width: 130px;">
                <el-option label="字母数字型" value="ALPHA" />
                <el-option label="数字长号" value="NUMERIC" />
                <el-option label="固定号码" value="FIXED" />
                <el-option label="虚拟号码" value="VIRTUAL" />
              </el-select>
            </div>
            <div class="filter-item">
              <label>短信属性</label>
              <el-select v-model="query.smsType" placeholder="全部" clearable style="width: 110px;">
                <el-option label="营销" value="MARKETING" />
                <el-option label="通知/OTP" value="NOTIFICATION" />
                <el-option label="ALL" value="ALL" />
              </el-select>
            </div>
            <div class="filter-actions">
              <el-button type="primary" @click="loadData">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </div>
          </div>

          <el-card shadow="never" style="border:1px solid #e5e7eb;">
            <template #header>
              <span style="font-size:14px;font-weight:600;color:#1f2937;">SID 主库</span>
            </template>
            <el-table :data="list" v-loading="loading" :header-cell-style="headerStyle">
              <el-table-column prop="sid" label="SID 值" width="150">
                <template #default="{row}"><span class="mono-code">{{ row.sid }}</span></template>
              </el-table-column>
              <el-table-column prop="countryCode" label="所属国家" width="140" align="center">
                <template #default="{row}">{{ getCountryLabel(row.countryCode) }}</template>
              </el-table-column>
              <el-table-column label="供应商" min-width="140">
                <template #default="{row}">{{ getVendorName(row.vendorId) }}</template>
              </el-table-column>
              <el-table-column prop="sidType" label="格式类型" width="120" align="center">
                <template #default="{row}">
                  <el-tag size="small" effect="plain" :type="row.sidType==='ALPHA'?'primary':row.sidType==='NUMERIC'?'info':row.sidType==='FIXED'?'success':'warning'">{{ sidTypeLabel(row.sidType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="smsType" label="短信属性" width="110" align="center">
                <template #default="{row}">
                  <el-tag size="small" effect="plain" :class="smsTypeBadgeClass(row.smsType)">{{ smsTypeLabel(row.smsType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="通道注册数" width="100" align="center">
                <template #default>0</template>
              </el-table-column>
              <el-table-column label="已生效通道" width="100" align="center">
                <template #default><span style="color:#d97706;font-weight:500;">0 条</span></template>
              </el-table-column>
              <el-table-column prop="isActive" label="主表状态" width="90" align="center">
                <template #default="{row}">
                  <el-tag :type="row.isActive?'success':'warning'" size="small" effect="light" round>{{ row.isActive?'正常':'禁用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="170" fixed="right">
                <template #default="{row}">
                  <el-button link type="primary" size="small" @click="openRegModal(row)">注册管理</el-button>
                  <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;" />
          </el-card>
        </el-tab-pane>

        <!-- Tab 2: 通道注册管理 -->
        <el-tab-pane name="registration">
          <template #label>通道注册管理</template>
          <div class="filter-bar">
            <div class="filter-item">
              <label>SID 值</label>
              <el-input v-model="regQuery.keyword" placeholder="搜索 SID" clearable style="width: 140px;" />
            </div>
            <div class="filter-item">
              <label>通道</label>
              <el-select v-model="regQuery.channelId" placeholder="全部通道" clearable style="width: 160px;">
                <el-option v-for="ch in channels" :key="ch.id" :label="ch.channelName" :value="ch.id" />
              </el-select>
            </div>
            <div class="filter-item">
              <label>注册状态</label>
              <el-select v-model="regQuery.regStatus" placeholder="全部" clearable style="width: 130px;">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="待提交" value="PENDING" />
                <el-option label="审核中" value="REVIEWING" />
                <el-option label="已生效" value="ACTIVE" />
                <el-option label="已拒绝" value="REJECTED" />
                <el-option label="已过期" value="EXPIRED" />
                <el-option label="已暂停" value="SUSPENDED" />
              </el-select>
            </div>
            <div class="filter-actions">
              <el-button type="primary">查询</el-button>
              <el-button @click="regQuery.keyword='';regQuery.channelId='';regQuery.regStatus=''">重置</el-button>
            </div>
          </div>
          <el-card shadow="never" style="border:1px solid #e5e7eb;">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span style="font-size:14px;font-weight:600;color:#1f2937;">通道注册记录</span>
                <el-button size="small" @click="openAddRegForm">＋ 添加注册记录</el-button>
              </div>
            </template>
            <el-empty description="通道注册管理功能，后续版本开放" :image-size="80" />
            <!-- 状态流转说明 -->
            <div class="status-flow-section">
              <div style="font-size:12px;color:#6b7280;margin-bottom:6px;font-weight:500;">注册状态流转</div>
              <div class="status-flow">
                <span class="status-step pending">草稿</span><span class="flow-arrow">→</span>
                <span class="status-step pending">待提交</span><span class="flow-arrow">→</span>
                <span class="status-step pending">审核中</span><span class="flow-arrow">→</span>
                <span class="status-step done">已生效</span><span class="flow-arrow">/</span>
                <span class="status-step rejected">已拒绝</span><span class="flow-arrow">→</span>
                <span class="status-step pending">待提交</span><span class="flow-arrow">⟳</span>
                <span style="margin:0 8px;color:#d1d5db;">|</span>
                <span class="status-step done">已生效</span><span class="flow-arrow">→</span>
                <span class="status-step pending">已过期</span><span class="flow-arrow">/</span>
                <span class="status-step pending">已暂停</span><span class="flow-arrow">→</span>
                <span class="status-step pending">审核中</span>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <!-- Tab 3: 到期预警看板 -->
        <el-tab-pane name="expiry">
          <template #label>到期预警看板 <el-badge :value="0" :max="99" type="danger" style="margin-left:4px;" /></template>
          <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
            以下 SID 将在 30 天内到期，请及时联系供应商续期，避免影响客户发送。
          </el-alert>
          <el-card shadow="never" style="border:1px solid #e5e7eb;">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span style="font-size:14px;font-weight:600;color:#1f2937;">即将到期 SID（30天内）</span>
                <el-button size="small">导出清单</el-button>
              </div>
            </template>
            <el-empty description="暂无到期预警数据" :image-size="80" />
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建/编辑 SID Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑 SID' : '新建 SID'" width="620px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top">

        <!-- 行1：SID值 / 所属国家 / 归属供应商 -->
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="SID 值" prop="sid">
              <el-input v-model="form.sid" :disabled="!!form.id" placeholder="如 NOTICE 或 +6512345678" />
              <div class="form-hint" v-if="!form.id">创建后不可修改</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属国家" prop="countryCode">
              <el-select v-model="form.countryCode" filterable placeholder="请选择" style="width:100%;" @change="onCountryChange">
                <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.code} - ${c.name}`" :value="c.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="归属供应商" prop="vendorId">
              <el-select v-model="form.vendorId" filterable placeholder="先选国家" style="width:100%;" :disabled="!form.countryCode">
                <el-option v-for="v in filteredVendors" :key="v.id" :label="v.vendorName" :value="v.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 行2：SID类型（全宽，按钮组） -->
        <el-form-item label="SID 类型" prop="sidType">
          <el-radio-group v-model="form.sidType" @change="onSidTypeChange" style="display:flex;gap:8px;flex-wrap:wrap;">
            <el-radio-button value="ALPHA">
              <div style="line-height:1.4;padding:2px 0;">
                <div style="font-weight:600;">Alpha</div>
                <div style="font-size:11px;color:#6b7280;">字母数字，≤11位</div>
              </div>
            </el-radio-button>
            <el-radio-button value="NUMERIC">
              <div style="line-height:1.4;padding:2px 0;">
                <div style="font-weight:600;">Numeric</div>
                <div style="font-size:11px;color:#6b7280;">纯数字长号，≤15位</div>
              </div>
            </el-radio-button>
            <el-radio-button value="FIXED">
              <div style="line-height:1.4;padding:2px 0;">
                <div style="font-weight:600;">Fixed 固定号码</div>
                <div style="font-size:11px;color:#6b7280;">运营商分配，有效期</div>
              </div>
            </el-radio-button>
            <el-radio-button value="VIRTUAL">
              <div style="line-height:1.4;padding:2px 0;">
                <div style="font-weight:600;">Virtual 虚拟号码</div>
                <div style="font-size:11px;color:#6b7280;">共享池租用，有效期</div>
              </div>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 行3：有效期（仅 FIXED/VIRTUAL） -->
        <el-row :gutter="16" v-if="form.sidType === 'FIXED' || form.sidType === 'VIRTUAL'">
          <el-col :span="12">
            <el-form-item label="有效期（月）">
              <el-input-number v-model="form.validityMonths" :min="1" :max="120" style="width:100%;" />
              <div class="form-hint">{{ form.sidType === 'VIRTUAL' ? '共享池租用期限' : '到期需向运营商续签' }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到期（自动计算）">
              <el-input :model-value="calcExpireDate(form.validityMonths)" disabled placeholder="填写月数后自动计算" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 行4：短信属性 + 状态 -->
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="短信属性" prop="smsType">
              <el-radio-group v-model="form.smsType" style="display:flex;gap:8px;">
                <el-radio-button value="NOTIFICATION">通知 / OTP</el-radio-button>
                <el-radio-button value="MARKETING">营销</el-radio-button>
                <el-radio-button value="ALL">ALL（通知+营销）</el-radio-button>
              </el-radio-group>
              <div class="form-hint">注册时锁定，营销SID不可用于通知类客户</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用状态">
              <el-switch v-model="form.isActive" :active-value="true" :inactive-value="false" active-text="正常" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>

      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ form.id ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- SID 通道注册管理弹窗 — 原型 sidRegModal -->
    <el-dialog v-model="regModalVisible" :title="`通道注册管理 — ${regRow.sid}（${regRow.countryCode}/${getVendorName(regRow.vendorId)}）`" width="800px" destroy-on-close>
      <div style="font-size:13px;color:#6b7280;margin-bottom:16px;">
        该 SID 在各通道的注册状态独立维护。<strong style="color:#1f2937;">SID 实际可用 = 主表状态正常 + 通道注册已生效 + 未超过有效期</strong>
      </div>
      <el-empty description="暂无注册记录" :image-size="60" />
      <div style="margin-top:12px;">
        <el-button size="small" @click="openAddRegForm">＋ 在新通道注册</el-button>
      </div>
      <template #footer><el-button @click="regModalVisible=false">关闭</el-button></template>
    </el-dialog>

    <!-- 批量录入固定/虚拟号码 Dialog -->
    <el-dialog v-model="batchEntryVisible" title="批量录入固定/虚拟号码 SID" width="600px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom:16px;">
        适用于<strong>固定号码（FIXED）</strong>和<strong>虚拟号码（VIRTUAL）</strong>的批量录入，每行一个号码。
      </el-alert>
      <el-form :model="batchEntryForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SID 类型">
              <el-select v-model="batchEntryForm.sidType" style="width:100%;">
                <el-option label="固定号码（FIXED）" value="FIXED" />
                <el-option label="虚拟号码（VIRTUAL）" value="VIRTUAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="短信属性">
              <el-select v-model="batchEntryForm.smsType" style="width:100%;">
                <el-option label="通知 / OTP" value="NOTIFICATION" />
                <el-option label="营销" value="MARKETING" />
                <el-option label="ALL" value="ALL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="归属供应商">
              <el-select v-model="batchEntryForm.vendorId" filterable placeholder="请选择" style="width:100%;">
                <el-option v-for="v in vendors" :key="v.id" :label="v.vendorName" :value="v.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属国家">
              <el-select v-model="batchEntryForm.countryCode" filterable placeholder="请选择" style="width:100%;">
                <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.code} - ${c.name}`" :value="c.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="有效期（月）">
          <el-input-number v-model="batchEntryForm.validityMonths" :min="1" :max="120" style="width:160px;" />
          <span style="font-size:12px;color:#9ca3af;margin-left:8px;">固定/虚拟号码需填写有效期</span>
        </el-form-item>
        <el-form-item label="号码列表">
          <el-input v-model="batchEntryForm.numbers" type="textarea" :rows="8"
            placeholder="每行一个号码（E.164 格式或本地格式均可）&#10;+6581234567&#10;+6598765432" />
          <div style="font-size:12px;color:#6b7280;margin-top:4px;">
            已输入 {{ batchEntryNumbers.length }} 个号码
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchEntryVisible=false">取消</el-button>
        <el-button type="primary" :loading="batchEntrySaving" @click="doBatchEntry"
          :disabled="batchEntryNumbers.length===0 || !batchEntryForm.vendorId || !batchEntryForm.countryCode">
          批量创建（{{ batchEntryNumbers.length }} 个）
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑注册记录 Dialog — 原型 addRegModal -->
    <el-dialog v-model="addRegVisible" title="添加通道注册记录" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SID">
              <el-input v-if="regModalVisible" :model-value="`${regRow.sid} (${regRow.countryCode})`" disabled style="width:100%;" />
              <el-select v-else v-model="regForm.sidId" filterable placeholder="请选择 SID" style="width:100%;">
                <el-option v-for="s in allSids" :key="s.id" :label="`${s.sid} (${s.countryCode})`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册通道">
              <el-select v-model="regForm.channelId" filterable placeholder="请选择通道" style="width:100%;">
                <el-option v-for="ch in channels" :key="ch.id" :label="ch.channelName" :value="ch.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="注册状态">
              <el-select v-model="regForm.regStatus" style="width:100%;">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="待提交" value="PENDING" />
                <el-option label="审核中" value="REVIEWING" />
                <el-option label="已生效" value="ACTIVE" />
                <el-option label="已拒绝" value="REJECTED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所需资料">
              <el-select v-model="regForm.material" style="width:100%;">
                <el-option label="无需资料" value="NONE" />
                <el-option label="授权书" value="AUTH" />
                <el-option label="营业执照+授权书" value="LICENSE_AUTH" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申请日期">
              <el-date-picker v-model="regForm.applyDate" type="date" placeholder="选择日期" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审核通过日期">
              <el-date-picker v-model="regForm.approveDate" type="date" placeholder="生效后填写" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="有效期至">
              <el-date-picker v-model="regForm.expireDate" type="date" placeholder="生效后填写" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注（拒绝原因/进度备注）">
          <el-input v-model="regForm.remark" type="textarea" :rows="2" placeholder="如：已邮件提交授权书，等待回复" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRegVisible=false">取消</el-button>
        <el-button type="primary" @click="addRegVisible=false;ElMessage.success('注册记录已保存')">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sidList, sidSave, sidUpdate, sidDel } from '../api'
import { useRefData } from '../stores/refData'
import isoCountries from '../data/countries'
import { getCountryLabel } from '../utils/country'

const route = useRoute()

const refData = useRefData()

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const loading = ref(false); const saving = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', countryCode: '', vendorId: '', sidType: '', smsType: '' })
const dialogVisible = ref(false); const form = ref({}); const formRef = ref(null)
const subTab = ref('master')
const regModalVisible = ref(false); const regRow = ref({})
const addRegVisible = ref(false)
const regForm = ref({ regStatus: 'REVIEWING', material: 'NONE' })
const regQuery = reactive({ keyword: '', channelId: '', regStatus: '' })
const vendors = ref([]); const channels = ref([]); const allSids = ref([])

// 批量录入
const batchEntryVisible = ref(false)
const batchEntrySaving = ref(false)
const batchEntryForm = ref({ sidType: 'FIXED', smsType: 'NOTIFICATION', vendorId: null, countryCode: '', validityMonths: 12, numbers: '' })
const batchEntryNumbers = computed(() =>
  (batchEntryForm.value.numbers || '').split('\n').map(l => l.trim()).filter(l => l.length > 0)
)
const doBatchEntry = async () => {
  const nums = batchEntryNumbers.value
  if (nums.length === 0) return
  batchEntrySaving.value = true
  try {
    let ok = 0; let fail = 0
    for (const num of nums) {
      try {
        await sidSave({
          sid: num,
          sidType: batchEntryForm.value.sidType,
          smsType: batchEntryForm.value.smsType,
          vendorId: batchEntryForm.value.vendorId,
          countryCode: batchEntryForm.value.countryCode,
          validityMonths: batchEntryForm.value.validityMonths,
          isActive: true
        })
        ok++
      } catch { fail++ }
    }
    ElMessage.success(`批量录入完成：成功 ${ok} 条${fail ? `，失败 ${fail} 条` : ''}`)
    batchEntryVisible.value = false
    batchEntryForm.value.numbers = ''
    loadData()
  } finally { batchEntrySaving.value = false }
}

// 统计
const activeCount = computed(() => list.value.filter(s => s.isActive).length)
const countryCount = computed(() => new Set(list.value.map(s => s.countryCode)).size)

// 国家选项：从已有的 isoCountries 库中获取
const countryOptions = isoCountries

// 联动：按国家过滤供应商
const filteredVendors = computed(() => {
  if (!form.value.countryCode) return []
  return vendors.value.filter(v => v.countryCode === form.value.countryCode)
})
const onCountryChange = () => { form.value.vendorId = undefined }

const getVendorName = (vendorId) => {
  const v = vendors.value.find(x => x.id === vendorId)
  return v ? v.vendorName : vendorId || '—'
}

const sidTypeLabel = (t) => ({ ALPHA: '字母数字型', NUMERIC: '数字长号', FIXED: '固定号码', VIRTUAL: '虚拟号码' }[t] || t || '—')

const onSidTypeChange = () => {
  if (form.value.sidType !== 'FIXED' && form.value.sidType !== 'VIRTUAL') {
    form.value.validityMonths = undefined
  }
}

const calcExpireDate = (months) => {
  if (!months || months < 1) return ''
  const d = new Date()
  d.setMonth(d.getMonth() + months)
  return d.toISOString().substring(0, 7)  // YYYY-MM
}
const smsTypeLabel = (t) => ({ NOTIFICATION: '通知/OTP', MARKETING: '营销', ALL: 'ALL' }[t] || t || '—')
const smsTypeBadgeClass = (t) => ({ NOTIFICATION: 'sms-badge-purple', MARKETING: 'sms-badge-warning', ALL: 'sms-badge-gray' }[t] || '')

const formRules = {
  sid: [{ required: true, message: 'SID不能为空', trigger: 'blur' }],
  countryCode: [{ required: true, message: '请选择国家', trigger: 'change' }],
  vendorId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  sidType: [{ required: true, message: '请选择SID格式', trigger: 'change' }],
  smsType: [{ required: true, message: '请选择短信属性', trigger: 'change' }],
}

const loadOptions = async () => {
  const [v, c] = await Promise.all([refData.loadVendors(), refData.loadChannels()])
  vendors.value = v; channels.value = c
}

const resetQuery = () => {
  query.keyword = ''; query.countryCode = ''; query.vendorId = ''; query.sidType = ''; query.smsType = ''
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.vendorId) params.vendorId = query.vendorId
    if (query.countryCode) params.countryCode = query.countryCode
    if (query.sidType) params.sidType = query.sidType
    if (query.smsType) params.smsType = query.smsType
    const r = await sidList(params)
    list.value = r.data?.list || []; total.value = r.data?.total || 0
    allSids.value = list.value
  } finally { loading.value = false }
}

const openForm = row => {
  form.value = row ? { ...row } : { isActive: true, sidType: 'ALPHA', smsType: 'NOTIFICATION' }
  loadOptions()
  dialogVisible.value = true
}

const handleSave = async () => {
  if (formRef.value) { const valid = await formRef.value.validate().catch(() => false); if (!valid) return }
  // 强校验：供应商必须属于所选国家
  if (form.value.vendorId && form.value.countryCode) {
    const vendor = vendors.value.find(v => v.id === form.value.vendorId)
    if (vendor && vendor.countryCode && vendor.countryCode !== form.value.countryCode) {
      ElMessage.warning(`供应商 ${vendor.vendorName} 所属国家为 ${vendor.countryCode}，与所选国家 ${form.value.countryCode} 不一致`)
      return
    }
  }
  // 强校验：创建时检查是否已存在相同 SID + 国家 + 供应商
  if (!form.value.id) {
    const dup = list.value.find(s => s.sid === form.value.sid && s.countryCode === form.value.countryCode && s.vendorId === form.value.vendorId)
    if (dup) { ElMessage.warning('该 SID + 国家 + 供应商组合已存在'); return }
  }
  saving.value = true
  try {
    form.value.id ? await sidUpdate(form.value.id, form.value) : await sidSave(form.value)
    ElMessage.success(form.value.id ? '已保存' : 'SID已创建，请添加通道注册记录')
    dialogVisible.value = false; loadData()
  } finally { saving.value = false }
}

const handleDel = async id => { await sidDel(id); ElMessage.success('删除成功'); loadData() }

const openBatchEntry = () => {
  loadOptions()
  batchEntryForm.value = { sidType: 'FIXED', smsType: 'NOTIFICATION', vendorId: null, countryCode: '', validityMonths: 12, numbers: '' }
  batchEntryVisible.value = true
}

const openRegModal = row => { regRow.value = { ...row }; regModalVisible.value = true }
const openAddRegForm = () => {
  regForm.value = { regStatus: 'REVIEWING', material: 'NONE' }
  if (regModalVisible.value && regRow.value?.id) {
    regForm.value.sidId = regRow.value.id
  }
  addRegVisible.value = true
}

onMounted(() => {
  if (route.query.vendorId) query.vendorId = route.query.vendorId
  if (route.query.countryCode) query.countryCode = route.query.countryCode
  loadData(); loadOptions()
})
</script>

<style scoped>
.stat-card { text-align: center; padding: 16px 20px; }
.stat-label { font-size: 13px; color: #6b7280; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-value.blue { color: #2563eb; }
.stat-value.green { color: #16a34a; }
.stat-value.orange { color: #d97706; }
.stat-value.red { color: #dc2626; }
.stat-sub { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.page-desc { font-size: 13px; color: #6b7280; margin-top: 2px; }
.mono-code { font-family: 'Courier New', monospace; font-size: 13px; font-weight: 600; }
.form-hint { font-size: 12px; color: #9ca3af; margin-top: 4px; }
.filter-bar { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-item label { font-size: 12px; color: #6b7280; font-weight: 500; }
.filter-actions { display: flex; gap: 8px; margin-left: auto; padding-top: 20px; }
.status-flow-section { padding: 14px 20px; border-top: 1px solid #e5e7eb; background: #f9fafb; margin-top: 16px; border-radius: 0 0 8px 8px; }
.status-flow { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.status-step { padding: 4px 12px; border-radius: 6px; font-size: 12px; font-weight: 500; }
.status-step.pending { background: #f3f4f6; color: #6b7280; }
.status-step.done { background: #f0fdf4; color: #16a34a; }
.status-step.rejected { background: #fef2f2; color: #dc2626; }
.flow-arrow { color: #9ca3af; font-size: 14px; }
/* 短信属性 badge 样式 */
:deep(.sms-badge-purple) { --el-tag-bg-color: #f3e8ff; --el-tag-border-color: #d8b4fe; --el-tag-text-color: #7c3aed; }
:deep(.sms-badge-warning) { --el-tag-bg-color: #fef3c7; --el-tag-border-color: #fcd34d; --el-tag-text-color: #d97706; }
:deep(.sms-badge-gray) { --el-tag-bg-color: #f3f4f6; --el-tag-border-color: #d1d5db; --el-tag-text-color: #6b7280; }
</style>
