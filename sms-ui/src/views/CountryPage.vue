<template>
  <el-card class="table-card">
    <div class="page-header">
      <div>
        <h2>国家/地区管理</h2>
        <div class="page-desc">维护平台支持的国家，国家是供应商、通道、SID 的顶层维度</div>
      </div>
      <el-button type="primary" @click="openForm()">＋ 添加国家</el-button>
    </div>
    <div class="filter-bar">
      <div class="filter-item">
        <label>国家代码</label>
        <el-input v-model="query.countryCode" placeholder="如 TH" clearable style="width: 100px;" @keyup.enter="loadData" />
      </div>
      <div class="filter-item">
        <label>国家名称</label>
        <el-input v-model="query.keyword" placeholder="如 Thailand" clearable style="width: 180px;" @keyup.enter="loadData" />
      </div>
      <div class="filter-item">
        <label>状态</label>
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px;">
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>资源筛选</label>
        <div style="display:flex;gap:12px;height:32px;align-items:center;">
          <el-checkbox v-model="query.hasVendor" label="有供应商" />
          <el-checkbox v-model="query.hasChannel" label="有通道" />
          <el-checkbox v-model="query.hasSid" label="有SID" />
        </div>
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="query.countryCode='';query.keyword='';query.status='';query.hasVendor=false;query.hasChannel=false;query.hasSid=false;loadData()">重置</el-button>
      </div>
    </div>
    <el-card shadow="never" style="border:1px solid #e5e7eb;">
      <template #header>
        <span style="font-size:14px;font-weight:600;color:#1f2937;">国家列表 <span style="color:#9ca3af;font-weight:400;font-size:12px;">共 {{ total }} 个</span></span>
      </template>
      <el-table :data="pagedList" v-loading="loading" :header-cell-style="headerStyle">
        <el-table-column prop="countryCode" label="国家代码" width="100">
          <template #default="{row}"><code class="mono-code">{{ row.countryCode }}</code></template>
        </el-table-column>
        <el-table-column prop="countryName" label="国家名称" min-width="180">
          <template #default="{row}">{{ row.countryName }} {{ row.countryNameEn }}</template>
        </el-table-column>
        <el-table-column label="供应商数" width="90" align="center">
          <template #default="{row}">
            <el-button v-if="row.vendorCount" link type="primary" size="small" @click="$router.push({ path: '/vendor', query: { serviceCountry: row.countryCode } })">{{ row.vendorCount }}</el-button>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column label="通道数" width="80" align="center">
          <template #default="{row}">
            <el-button v-if="row.channelCount" link type="primary" size="small" @click="$router.push({ path: '/channel', query: { countryCode: row.countryCode } })">{{ row.channelCount }}</el-button>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column label="SID 数" width="80" align="center">
          <template #default="{row}">
            <el-button v-if="row.sidCount" link type="primary" size="small" @click="$router.push({ path: '/sid', query: { countryCode: row.countryCode } })">{{ row.sidCount }}</el-button>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="80" align="center">
          <template #default="{row}"><el-tag :type="row.isActive?'success':'warning'" size="small" effect="light" round>{{ row.isActive?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="$router.push({ path: '/vendor', query: { serviceCountry: row.countryCode } })">查看供应商</el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" v-if="!row.vendorCount && !row.channelCount && !row.sidCount" @click="handleDel(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" style="margin-top:12px;" />
    </el-card>

    <!-- 添加/编辑国家 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? `编辑国家 — ${form.countryCode}` : '添加国家'" width="440px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="auto" label-position="top">
        <el-form-item label="国家代码（ISO 3166-1 alpha-2）" prop="countryCode" v-if="!form.id">
          <el-select v-model="form.countryCode" filterable placeholder="输入搜索国家" style="width:100%;" @change="onIsoSelect">
            <el-option v-for="c in isoCountries" :key="c.code" :label="`${c.code} — ${c.name} (${c.nameEn})`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家代码" v-if="form.id">
          <el-input :model-value="form.countryCode" disabled />
        </el-form-item>
        <el-form-item label="国家名称（中文）" prop="countryName">
          <el-input v-model="form.countryName" placeholder="如 泰国" />
        </el-form-item>
        <el-form-item label="国家名称（英文）" prop="countryNameEn">
          <el-input v-model="form.countryNameEn" placeholder="如 Thailand" />
        </el-form-item>
        <el-form-item label="区号" prop="phoneCode">
          <el-input v-model="form.phoneCode" placeholder="+66" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" :active-value="true" :inactive-value="false" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ form.id ? '保存' : '确认添加' }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { countryList, countrySave, countryUpdate, countryDel } from '../api'
import { useRefData } from '../stores/refData'
import isoCountries from '../data/countries'

const refData = useRefData()

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const loading = ref(false); const saving = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, countryCode: '', keyword: '', status: '', hasVendor: false, hasChannel: false, hasSid: false })
const dialogVisible = ref(false); const form = ref({}); const formRef = ref(null)

const rules = {
  countryCode: [{ required: true, message: '请选择国家', trigger: 'change' }],
  countryName: [{ required: true, message: '国家名称不能为空', trigger: 'blur' }],
  countryNameEn: [{ required: true, message: '英文名称不能为空', trigger: 'blur' }],
  phoneCode: [{ required: true, message: '区号不能为空', trigger: 'blur' }],
}

const onIsoSelect = (code) => {
  const c = isoCountries.find(x => x.code === code)
  if (c) { form.value.countryName = c.name; form.value.countryNameEn = c.nameEn; form.value.phoneCode = c.phoneCode }
}

// 前端分页：当前页数据切片
const pagedList = computed(() => {
  const start = (query.page - 1) * query.size
  return list.value.slice(start, start + query.size)
})

const loadData = async () => {
  loading.value = true
  try {
    // 始终加载全部国家（213条，数据量很小）
    const r = await countryList({ page: 1, size: 500, keyword: query.keyword || undefined })
    let allData = r.data?.list || []
    const [channels, sids] = await Promise.all([
      refData.loadChannels(true),
      refData.loadSids(true)
    ])
    allData.forEach(c => {
      const countryChannels = channels.filter(ch => ch.countryCode === c.countryCode)
      c.vendorCount = new Set(countryChannels.map(ch => ch.vendorId).filter(Boolean)).size
      c.channelCount = countryChannels.length
      c.sidCount = sids.filter(s => s.countryCode === c.countryCode).length
    })
    // 前端过滤
    if (query.countryCode) allData = allData.filter(c => c.countryCode && c.countryCode.toUpperCase().includes(query.countryCode.toUpperCase()))
    if (query.status !== '' && query.status !== null) allData = allData.filter(c => c.isActive === query.status)
    if (query.hasVendor) allData = allData.filter(c => c.vendorCount > 0)
    if (query.hasChannel) allData = allData.filter(c => c.channelCount > 0)
    if (query.hasSid) allData = allData.filter(c => c.sidCount > 0)
    // 排序：有资源的排前面，按资源总数降序
    allData.sort((a, b) => {
      const aHas = (a.vendorCount || 0) + (a.channelCount || 0) + (a.sidCount || 0)
      const bHas = (b.vendorCount || 0) + (b.channelCount || 0) + (b.sidCount || 0)
      return bHas - aHas
    })
    list.value = allData
    total.value = allData.length
    query.page = 1
  } finally { loading.value = false }
}
const openForm = row => { form.value = row ? { ...row } : { isActive: false }; dialogVisible.value = true }
const handleSave = async () => {
  if (formRef.value) { const valid = await formRef.value.validate().catch(() => false); if (!valid) return }
  saving.value = true
  try {
    if (form.value.countryCode) form.value.countryCode = form.value.countryCode.toUpperCase()
    form.value.id ? await countryUpdate(form.value.id, form.value) : await countrySave(form.value)
    ElMessage.success('保存成功'); dialogVisible.value = false; loadData()
  } finally { saving.value = false }
}
const handleDel = async id => { await countryDel(id); ElMessage.success('删除成功'); loadData() }
onMounted(loadData)
</script>

<style scoped>
.mono-code { font-family: 'Courier New', monospace; font-size: 13px; font-weight: 600; }
.page-desc { font-size: 13px; color: #6b7280; margin-top: 2px; }
.filter-bar { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-item label { font-size: 12px; color: #6b7280; font-weight: 500; }
.filter-actions { display: flex; gap: 8px; margin-left: auto; padding-top: 20px; }
</style>
