<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">账户设置</h2>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">

        <!-- Tab 1: 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border v-loading="infoLoading" style="max-width: 700px;">
            <el-descriptions-item label="企业名称">{{ info.companyName }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ info.contactName }}</el-descriptions-item>
            <el-descriptions-item label="联系邮箱">{{ info.contactEmail }}</el-descriptions-item>
            <el-descriptions-item label="联系手机">{{ info.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="时区">{{ info.timezone }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ info.createdAt }}</el-descriptions-item>
          </el-descriptions>

          <el-divider>修改密码</el-divider>
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="100px" style="max-width: 460px;">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password
                placeholder="至少 8 位，包含大小写字母和数字" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="handleChangePwd">更新密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 2: API凭证 -->
        <el-tab-pane label="API 凭证" name="api">
          <div v-loading="credLoading" style="max-width: 580px;">
            <div class="section-title">API Key</div>
            <div class="key-row">
              <div class="key-box">
                <code>{{ keyVisible ? credential.apiKey : maskKey(credential.apiKey) }}</code>
              </div>
              <el-button size="small" @click="keyVisible = !keyVisible">
                {{ keyVisible ? '隐藏' : '显示' }}
              </el-button>
              <el-button size="small" @click="copyText(credential.apiKey)">复制</el-button>
              <el-button size="small" type="danger" @click="showRegenDialog = true">重新生成</el-button>
            </div>

            <div class="section-title" style="margin-top: 24px;">IP 白名单</div>
            <p style="font-size: 12px; color: #999; margin-bottom: 12px;">
              留空表示不限制 IP。支持单个 IP 或 CIDR 格式（如 192.168.1.0/24）。
            </p>
            <div v-for="ip in ipList" :key="ip" class="ip-item">
              <code>{{ ip }}</code>
              <el-button link type="danger" :icon="Close" @click="removeIp(ip)" />
            </div>
            <div class="ip-add">
              <el-input v-model="newIp" placeholder="输入 IP 或 CIDR" style="width: 220px;" size="small" />
              <el-button type="primary" size="small" @click="addIp">添加</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 3: 回调配置 -->
        <el-tab-pane label="回调配置" name="callback">
          <div v-loading="callbackLoading" style="max-width: 600px;">
            <div class="callback-section">
              <div class="section-title" style="margin-top: 0;">DLR 回调（送达状态报告）</div>
              <el-form-item label="回调 URL" label-width="100px">
                <el-input v-model="callback.dlrUrl" placeholder="https://your-domain.com/webhook/dlr" />
              </el-form-item>
              <el-form-item label="Secret" label-width="100px">
                <el-input v-model="callback.dlrSecret" placeholder="用于验证回调请求签名" />
              </el-form-item>
              <el-button size="small" @click="testCallback('dlr')">测试推送</el-button>
            </div>

            <div class="callback-section" style="margin-top: 20px;">
              <div class="section-title">MO 回调（上行消息）</div>
              <el-form-item label="回调 URL" label-width="100px">
                <el-input v-model="callback.moUrl" placeholder="https://your-domain.com/webhook/mo" />
              </el-form-item>
              <el-form-item label="Secret" label-width="100px">
                <el-input v-model="callback.moSecret" placeholder="用于验证回调请求签名" />
              </el-form-item>
            </div>

            <el-button type="primary" :loading="callbackSaving" @click="saveCallback" style="margin-top: 8px;">
              保存配置
            </el-button>
          </div>
        </el-tab-pane>

        <!-- Tab 4: 操作日志 -->
        <el-tab-pane label="操作日志" name="log">
          <el-table :data="opLogs" stripe v-loading="opLogLoading" style="font-size: 13px;"
            :header-cell-style="headerStyle">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="action" label="操作" width="150" />
            <el-table-column prop="summary" label="详情" min-width="250" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP" width="140" />
          </el-table>
          <el-pagination v-model:current-page="opLogQuery.page" v-model:page-size="opLogQuery.size"
            :total="opLogTotal" layout="total, prev, pager, next" @change="loadOpLog"
            style="margin-top: 16px; justify-content: flex-end;" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 重新生成 API Key 弹窗 -->
    <el-dialog v-model="showRegenDialog" title="重新生成 API Key" width="440px">
      <el-alert type="error" :closable="false" show-icon style="margin-bottom: 16px;">
        <template #title>此操作不可撤销！旧 Key 将立即失效。</template>
      </el-alert>
      <el-form label-width="80px">
        <el-form-item label="登录密码">
          <el-input v-model="regenPwd" type="password" show-password placeholder="请输入当前登录密码以确认" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegenDialog = false">取消</el-button>
        <el-button type="danger" :loading="regenLoading" @click="handleRegen">确认重新生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import {
  accountInfo, accountChangePwd,
  accountCredential, accountRegenKey, accountAddIp, accountRemoveIp,
  accountCallbackGet, accountCallbackSave, accountCallbackTest,
  accountOpLog
} from '../api'

const activeTab = ref('info')
const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }

// ===== 基本信息 =====
const infoLoading = ref(false)
const info = ref({})
const pwdRef = ref()
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword:     [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword:     [{ required: true, min: 8, message: '密码至少 8 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, trigger: 'blur' },
    { validator: (_, v, cb) => v !== pwdForm.newPassword ? cb(new Error('两次密码不一致')) : cb(), trigger: 'blur' }]
}

const handleChangePwd = async () => {
  await pwdRef.value.validate()
  pwdSaving.value = true
  try {
    await accountChangePwd({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } finally {
    pwdSaving.value = false
  }
}

// ===== API 凭证 =====
const credLoading = ref(false)
const credential  = ref({ apiKey: '' })
const keyVisible  = ref(false)
const ipList      = ref([])
const newIp       = ref('')
const showRegenDialog = ref(false)
const regenPwd    = ref('')
const regenLoading = ref(false)

const maskKey  = k => k ? k.slice(0, 4) + '****' + k.slice(-4) : ''
const copyText = t => { if (t) navigator.clipboard.writeText(t).then(() => ElMessage.success('已复制')).catch(() => {}) }

const loadCredential = async () => {
  credLoading.value = true
  try {
    const res = await accountCredential()
    credential.value = res.data || {}
    ipList.value = (res.data?.ipWhitelist || '').split(',').map(s => s.trim()).filter(Boolean)
  } finally {
    credLoading.value = false
  }
}

const addIp = async () => {
  if (!newIp.value) return
  await accountAddIp(newIp.value)
  ipList.value.push(newIp.value)
  newIp.value = ''
  ElMessage.success('IP 已添加')
}

const removeIp = async ip => {
  await ElMessageBox.confirm(`确认移除 IP: ${ip} ？`, '确认', { type: 'warning' })
  await accountRemoveIp(ip)
  ipList.value = ipList.value.filter(i => i !== ip)
  ElMessage.success('IP 已移除')
}

const handleRegen = async () => {
  if (!regenPwd.value) { ElMessage.warning('请输入登录密码'); return }
  regenLoading.value = true
  try {
    const res = await accountRegenKey({ password: regenPwd.value })
    credential.value = res.data || {}
    keyVisible.value = true
    showRegenDialog.value = false
    regenPwd.value = ''
    ElMessage.success('API Key 已重新生成')
  } finally {
    regenLoading.value = false
  }
}

// ===== 回调配置 =====
const callbackLoading = ref(false)
const callbackSaving  = ref(false)
const callback = reactive({ dlrUrl: '', dlrSecret: '', moUrl: '', moSecret: '' })

const loadCallback = async () => {
  callbackLoading.value = true
  try {
    const res = await accountCallbackGet()
    Object.assign(callback, res.data || {})
  } finally {
    callbackLoading.value = false
  }
}

const saveCallback = async () => {
  callbackSaving.value = true
  try {
    await accountCallbackSave({ ...callback })
    ElMessage.success('配置已保存')
  } finally {
    callbackSaving.value = false
  }
}

const testCallback = async type => {
  await accountCallbackTest(type)
  ElMessage.success('测试推送已发送')
}

// ===== 操作日志 =====
const opLogLoading = ref(false)
const opLogs       = ref([])
const opLogTotal   = ref(0)
const opLogQuery   = reactive({ page: 1, size: 20 })

const loadOpLog = async () => {
  opLogLoading.value = true
  try {
    const res = await accountOpLog({ page: opLogQuery.page, size: opLogQuery.size })
    opLogs.value    = res.data?.list || []
    opLogTotal.value = res.data?.total || 0
  } finally {
    opLogLoading.value = false
  }
}

const onTabChange = tab => {
  if (tab === 'api'      && !credential.value.apiKey) loadCredential()
  if (tab === 'callback' && !callback.dlrUrl)         loadCallback()
  if (tab === 'log'      && opLogs.value.length === 0) loadOpLog()
}

onMounted(async () => {
  infoLoading.value = true
  try {
    const res = await accountInfo()
    info.value = res.data || {}
  } finally {
    infoLoading.value = false
  }
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0; }
.section-title { font-size: 15px; font-weight: 600; margin: 16px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }

.key-row { display: flex; align-items: center; gap: 8px; }
.key-box { background: #f5f5f5; padding: 8px 14px; border-radius: 4px; flex: 1; font-family: monospace; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.ip-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-family: monospace; font-size: 13px; }
.ip-add  { display: flex; gap: 8px; margin-top: 8px; }

.callback-section { padding: 16px; background: #f8f9fb; border-radius: 8px; border: 1px solid #e8e8e8; }
</style>
