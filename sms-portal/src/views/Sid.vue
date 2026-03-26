<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">SID 管理</h2>
    </div>

    <el-card shadow="never">
      <el-table :data="list" stripe v-loading="loading" style="font-size: 13px;"
        :header-cell-style="headerStyle">
        <el-table-column prop="sid" label="SID" width="180" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.sidType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="短信类型" width="120">
          <template #default="{ row }">{{ row.smsType || '通用' }}</template>
        </el-table-column>
        <el-table-column prop="countryCode" label="国家" width="80" />
        <el-table-column label="有效期" width="120">
          <template #default="{ row }">
            {{ row.validityMonths ? `${row.validityMonths} 个月` : '长期' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
              {{ row.isActive ? '有效' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>

      <el-empty v-if="!loading && list.length === 0"
        description="暂无 SID，请联系客户经理申请" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sidList } from '../api'

const loading = ref(false)
const list = ref([])
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

onMounted(async () => {
  loading.value = true
  try {
    const res = await sidList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0; }
</style>
