<template>
  <el-card class="table-card">
    <div class="page-header">
      <div>
        <h2>供应商管理</h2>
        <div class="page-desc">供应商须绑定所属国家，不可跨国提供服务</div>
      </div>
      <el-button type="primary" @click="openForm()">＋ 新建供应商</el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label>供应商 ID</label>
        <el-input v-model="query.vendorCode" placeholder="如 V001" clearable style="width:120px;" @keyup.enter="loadData" />
      </div>
      <div class="filter-item">
        <label>供应商名称</label>
        <el-input v-model="query.keyword" placeholder="搜索名称" clearable style="width:180px;" @keyup.enter="loadData" />
      </div>
      <div class="filter-item">
        <label>所属国家</label>
        <el-select v-model="query.countryCode" placeholder="全部国家" clearable style="width:150px;">
          <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} ${c.code}`" :value="c.code" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>状态</label>
        <el-select v-model="query.status" placeholder="全部" clearable style="width:100px;">
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="query.vendorCode='';query.keyword='';query.countryCode='';query.status='';query.serviceCountry='';loadData()">重置</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <el-card shadow="never" style="border:1px solid #e5e7eb;">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span style="font-size:14px;font-weight:600;color:#1f2937;">供应商列表 <span style="color:#9ca3af;font-weight:400;font-size:12px;">共 {{ total }} 个</span></span>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" :header-cell-style="headerStyle">
        <el-table-column prop="vendorCode" label="供应商 ID" width="110">
          <template #default="{row}"><code class="mono-code gray">{{ row.vendorCode }}</code></template>
        </el-table-column>
        <el-table-column prop="vendorName" label="供应商名称" width="160">
          <template #default="{row}"><span style="font-weight:500;">{{ row.vendorName }}</span></template>
        </el-table-column>
        <el-table-column prop="countryCode" label="所属国家" width="130" align="center">
          <template #default="{row}">{{ getCountryLabel(row.countryCode) }}</template>
        </el-table-column>
        <el-table-column label="通道数" width="80" align="center">
          <template #default="{row}">{{ row.channelCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="SID 数" width="80" align="center">
          <template #default="{row}">{{ row.sidCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="isActive" label="状态" width="80" align="center">
          <template #default="{row}"><el-tag :type="row.isActive?'success':'warning'" size="small" effect="light" round>{{ row.isActive?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-dropdown trigger="click" style="margin-left:4px;" @command="cmd => onDropdown(cmd, row)">
              <el-button link size="small" style="color:#374151;">···</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="channel">查看通道</el-dropdown-item>
                  <el-dropdown-item command="sid">查看 SID</el-dropdown-item>
                  <el-dropdown-item command="toggle" divided>{{ row.isActive ? '禁用' : '启用' }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;" />
    </el-card>

    <!-- 新建/编辑供应商 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑供应商' : '新建供应商'" width="620px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="供应商名称" prop="vendorName" class="span-2">
          <el-input v-model="form.vendorName" placeholder="如 DTAC Thailand" />
        </el-form-item>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="所属国家" prop="countryCode">
              <el-select v-model="form.countryCode" filterable placeholder="请选择" style="width:100%;" :disabled="!!form.id && !!form.countryCode">
                <el-option v-for="c in allCountries" :key="c.code" :label="`${c.name} ${c.code}`" :value="c.code" />
              </el-select>
              <div class="form-hint" v-if="!form.id">供应商绑定后不可更换国家</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商代码" prop="vendorCode">
              <el-input v-model="form.vendorCode" :disabled="!!form.id" placeholder="唯一代码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="联系人"><el-input v-model="form.contactName" placeholder="联系人姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱"><el-input v-model="form.contactEmail" placeholder="contact@vendor.com" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" placeholder="供应商备注信息" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" :active-value="true" :inactive-value="false" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ form.id ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 供应商详情 -->
    <el-dialog v-model="detailVisible" :title="`供应商详情 — ${detailData?.vendorName||''}`" width="620px" destroy-on-close>
      <template v-if="detailData">
        <div class="detail-grid">
          <div><span class="detail-label">供应商ID：</span><code class="mono-code gray">{{ detailData.vendorCode }}</code></div>
          <div><span class="detail-label">所属国家：</span>{{ getCountryLabel(detailData.countryCode) }}</div>
          <div><span class="detail-label">状态：</span><el-tag :type="detailData.isActive?'success':'warning'" size="small" round>{{ detailData.isActive?'启用':'禁用' }}</el-tag></div>
          <div><span class="detail-label">联系人：</span>{{ detailData.contactName || '-' }}</div>
          <div><span class="detail-label">联系邮箱：</span>{{ detailData.contactEmail || '-' }}</div>
        </div>

        <div style="font-size:13px;font-weight:600;color:#374151;margin:20px 0 8px;">旗下通道（{{ detailChannels.length }}条）</div>
        <div v-for="ch in detailChannels" :key="ch.id" class="channel-row">
          <span>{{ ch.channelName || ch.channelCode }}</span>
          <span style="display:flex;gap:8px;">
            <el-tag size="small" :type="ch.channelType===1?'primary':'warning'" effect="plain">{{ ch.channelType===1?'SMPP':'HTTP' }}</el-tag>
            <el-tag size="small" :type="ch.status===1?'success':'danger'" effect="light" round>{{ ch.status===1?'启用':'禁用' }}</el-tag>
          </span>
        </div>
        <el-empty v-if="detailChannels.length===0" description="暂无旗下通道" :image-size="50" />
      </template>
      <template #footer>
        <el-button @click="detailVisible=false">关闭</el-button>
        <el-button type="primary" @click="detailVisible=false;openForm(detailData)">编辑供应商</el-button>
      </template>
    </el-dialog>

  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { vendorList, vendorSave, vendorUpdate, vendorDel, channelSave, sidSave } from '../api'
import { useRefData } from '../stores/refData'
import isoCountries from '../data/countries'
import { getCountryLabel } from '../utils/country'

const router = useRouter()
const route = useRoute()
const refData = useRefData()
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const loading = ref(false); const saving = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, vendorCode: '', keyword: '', countryCode: '', status: '', serviceCountry: '' })
const dialogVisible = ref(false); const form = ref({}); const formRef = ref(null)
const detailVisible = ref(false); const detailData = ref(null); const detailChannels = ref([])
const countryOptions = ref([]); const allCountries = ref([])


const rules = {
  vendorName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  vendorCode: [{ required: true, message: '代码不能为空', trigger: 'blur' }],
  countryCode: [{ required: true, message: '请选择所属国家', trigger: 'change' }],
}

const loadCountries = async () => {
  const d = await refData.loadCountries()
  countryOptions.value = d; allCountries.value = d.length > 0 ? d : isoCountries
}

const loadData = async () => {
  loading.value = true
  try {
    const r = await vendorList(query); list.value = r.data?.list || []; total.value = r.data?.total || 0
    const [channels, sids] = await Promise.all([
      refData.loadChannels(),
      refData.loadSids()
    ])
    list.value.forEach(v => {
      v.channelCount = channels.filter(c => c.vendorId === v.id).length
      v.sidCount = sids.filter(s => s.vendorId === v.id).length
    })
    // 前端过滤
    if (query.vendorCode) list.value = list.value.filter(v => v.vendorCode && v.vendorCode.toLowerCase().includes(query.vendorCode.toLowerCase()))
    if (query.countryCode) list.value = list.value.filter(v => v.countryCode === query.countryCode)
    if (query.serviceCountry) {
      // 按服务国家过滤：有通道服务该国家的供应商
      const vendorIdsServingCountry = new Set(channels.filter(c => c.countryCode === query.serviceCountry).map(c => c.vendorId))
      list.value = list.value.filter(v => vendorIdsServingCountry.has(v.id))
    }
    if (query.status !== '' && query.status !== null) list.value = list.value.filter(v => v.isActive === query.status)
  } finally { loading.value = false }
}

const openForm = row => { form.value = row ? { ...row } : { isActive: true }; loadCountries(); dialogVisible.value = true }
const handleSave = async () => {
  if (formRef.value) { const v = await formRef.value.validate().catch(() => false); if (!v) return }
  // 编辑时如果从启用改为禁用，提示级联影响
  if (form.value.id && !form.value.isActive) {
    const original = list.value.find(v => v.id === form.value.id)
    if (original && original.isActive && original.channelCount > 0) {
      try {
        await ElMessageBox.confirm(
          `禁用供应商将同时禁用其下 ${original.channelCount} 条通道，确认继续？`,
          '确认禁用', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
      } catch { return }
    }
  }
  saving.value = true
  try { form.value.id ? await vendorUpdate(form.value.id, form.value) : await vendorSave(form.value); refData.refresh('vendors'); ElMessage.success('保存成功'); dialogVisible.value = false; loadData() } finally { saving.value = false }
}

const onDropdown = (cmd, row) => {
  const extraQuery = query.serviceCountry ? { countryCode: query.serviceCountry } : {}
  if (cmd === 'channel') router.push({ path: '/channel', query: { vendorId: row.id, ...extraQuery } })
  else if (cmd === 'sid') router.push({ path: '/sid', query: { vendorId: row.id, ...extraQuery } })
  else if (cmd === 'toggle') toggleVendor(row)
}
const toggleVendor = async row => {
  if (row.isActive) {
    // 禁用时需要确认，因为会级联禁用通道
    const channelCount = row.channelCount || 0
    const msg = channelCount > 0
      ? `禁用供应商「${row.vendorName}」将同时禁用其下 ${channelCount} 条通道，确认继续？`
      : `确认禁用供应商「${row.vendorName}」？`
    try {
      await ElMessageBox.confirm(msg, '确认禁用', { type: 'warning', confirmButtonText: '确认禁用', cancelButtonText: '取消' })
    } catch { return }
  }
  try {
    await vendorUpdate(row.id, { ...row, isActive: !row.isActive })
    ElMessage.success(row.isActive ? '已禁用（下属通道已同步禁用）' : '已启用')
    loadData()
  } catch(e) {/* */}
}

const showDetail = async row => {
  detailData.value = row; detailChannels.value = []; detailVisible.value = true
  try { const r = await channelList({ page: 1, size: 100 }); detailChannels.value = (r.data?.list || []).filter(c => c.vendorId === row.id) } catch(e) {/* */}
}


onMounted(() => {
  if (route.query.countryCode) query.countryCode = route.query.countryCode
  if (route.query.serviceCountry) query.serviceCountry = route.query.serviceCountry
  loadData(); loadCountries()
})
</script>

<style scoped>
.mono-code { font-family: 'Courier New', monospace; font-size: 13px; }
.mono-code.gray { color: #6b7280; }
.page-desc { font-size: 13px; color: #6b7280; margin-top: 2px; }
.filter-bar { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-item label { font-size: 12px; color: #6b7280; font-weight: 500; }
.filter-actions { display: flex; gap: 8px; margin-left: auto; padding-top: 20px; }
.form-hint { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 24px; font-size: 13px; margin-bottom: 20px; }
.detail-label { color: #6b7280; }
.channel-row { padding: 8px 12px; background: #f9fafb; border-radius: 6px; display: flex; justify-content: space-between; align-items: center; font-size: 13px; margin-bottom: 6px; }

</style>
