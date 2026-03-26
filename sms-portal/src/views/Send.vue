<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">发送短信</h2>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: 快速发送 -->
        <el-tab-pane label="快速发送" name="quick">
          <el-form :model="quickForm" :rules="quickRules" ref="quickRef" label-width="90px" style="max-width: 560px;">
            <el-form-item label="目标号码" prop="to">
              <el-input v-model="quickForm.to" placeholder="+8613800138000（E.164 格式）" />
            </el-form-item>
            <el-form-item label="SID" prop="sid">
              <el-select v-model="quickForm.sid" placeholder="留空使用默认" clearable style="width: 100%;" filterable>
                <el-option label="（系统自动分配）" value="" />
                <el-option v-for="s in sidOptions" :key="s.id"
                  :label="`${s.sid}（${s.sidType} · ${s.countryCode}）`" :value="s.sid" />
              </el-select>
            </el-form-item>
            <el-form-item label="短信类型">
              <el-select v-model="quickForm.smsAttribute" placeholder="默认" style="width: 200px;">
                <el-option label="默认" :value="null" />
                <el-option label="OTP 验证码" :value="1" />
                <el-option label="Transaction" :value="2" />
                <el-option label="Notification" :value="3" />
                <el-option label="Marketing" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="内容" prop="content">
              <el-input v-model="quickForm.content" type="textarea" :rows="4"
                placeholder="请输入短信内容" maxlength="1000" show-word-limit
                @input="onContentChange" />
              <div class="encode-info">
                编码: <strong>{{ encodeInfo.encoding }}</strong> &nbsp;|&nbsp;
                字符数: <strong>{{ encodeInfo.chars }}</strong> &nbsp;|&nbsp;
                段数: <strong>{{ encodeInfo.segments }}</strong>
              </div>
            </el-form-item>
            <el-form-item label="回调引用">
              <el-input v-model="quickForm.clientRef" placeholder="可选，用于标识本次请求" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="sending" @click="handleQuickSend">发送</el-button>
              <el-button @click="quickRef.resetFields()">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 发送结果 -->
          <el-alert v-if="sendResult" :type="sendResult.success ? 'success' : 'error'"
            :closable="true" @close="sendResult = null" style="margin-top: 16px; max-width: 560px;">
            <template #title>
              <span v-if="sendResult.success">
                发送成功 &nbsp;|&nbsp; 消息ID: <strong>{{ sendResult.messageId }}</strong>
                &nbsp;|&nbsp; 状态: {{ sendResult.status }}
                &nbsp;|&nbsp; 费用: ${{ sendResult.price }}
              </span>
              <span v-else>发送失败: {{ sendResult.error }}</span>
            </template>
          </el-alert>
        </el-tab-pane>

        <!-- Tab 2: 批量发送 -->
        <el-tab-pane label="批量发送" name="batch">
          <el-form :model="batchForm" ref="batchRef" label-width="100px" style="max-width: 640px;">
            <el-form-item label="SID">
              <el-select v-model="batchForm.sid" placeholder="留空使用默认" clearable style="width: 100%;" filterable>
                <el-option label="（系统自动分配）" value="" />
                <el-option v-for="s in sidOptions" :key="s.id"
                  :label="`${s.sid}（${s.sidType} · ${s.countryCode}）`" :value="s.sid" />
              </el-select>
            </el-form-item>
            <el-form-item label="短信类型">
              <el-select v-model="batchForm.smsAttribute" placeholder="默认" style="width: 200px;">
                <el-option label="默认" :value="null" />
                <el-option label="OTP 验证码" :value="1" />
                <el-option label="Transaction" :value="2" />
                <el-option label="Notification" :value="3" />
                <el-option label="Marketing" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="选择模板">
              <el-select v-model="selectedTemplateId" placeholder="从已审核模板选择（可选）" clearable
                style="width: 100%;" filterable @change="onTemplateSelect">
                <el-option v-for="t in approvedTemplates" :key="t.id"
                  :label="t.templateName" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="短信内容" prop="content">
              <el-input v-model="batchForm.content" type="textarea" :rows="4"
                placeholder="请输入短信内容，或从上方选择模板自动填入" />
            </el-form-item>
            <el-form-item label="号码列表">
              <div style="display:flex; gap:8px; margin-bottom:8px;">
                <el-upload :auto-upload="false" :show-file-list="false" accept=".csv,.txt"
                  :on-change="onFileChange" style="display:inline-block;">
                  <el-button size="small" plain>
                    <el-icon style="margin-right:4px;"><Upload /></el-icon>导入 CSV/TXT
                  </el-button>
                </el-upload>
                <span style="font-size:12px; color:#999; line-height:28px;">每行一个号码，E.164格式，上限 500 条</span>
              </div>
              <el-input v-model="batchForm.numbers" type="textarea" :rows="6"
                placeholder="每行一个号码，E.164 格式&#10;+8613800138000&#10;+8613900139000" />
              <div style="font-size: 12px; color: #999; margin-top: 4px;">
                已输入 {{ parsedNumbers.length }} 个号码（上限 500）
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="batchSending" @click="handleBatchSend"
                :disabled="parsedNumbers.length === 0">
                批量发送（{{ parsedNumbers.length }} 个号码）
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { sendSms, sendBatch, sidList, templateList } from '../api'

const activeTab = ref('quick')
const quickRef = ref()
const batchRef = ref()
const sending = ref(false)
const batchSending = ref(false)
const sendResult = ref(null)
const sidOptions = ref([])
const approvedTemplates = ref([])
const selectedTemplateId = ref(null)

const quickForm = reactive({ to: '', sid: '', content: '', smsAttribute: null, clientRef: '' })
const quickRules = {
  to:      [{ required: true, message: '请输入目标号码', trigger: 'blur' }],
  content: [{ required: true, message: '请输入短信内容', trigger: 'blur' }]
}

const batchForm = reactive({ sid: '', content: '', numbers: '', smsAttribute: null })

// Encoding detection
const encodeInfo = computed(() => {
  const text = quickForm.content
  if (!text) return { encoding: 'GSM-7', chars: 0, segments: 0 }
  const gsm7 = '@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞ ÆæßÉ !"#¤%&\'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà'
  const ext  = '^{}\\[~]|€'
  let isGsm7 = true
  let len = 0
  for (const c of text) {
    if (!gsm7.includes(c) && !ext.includes(c)) { isGsm7 = false; break }
    len += ext.includes(c) ? 2 : 1
  }
  if (!isGsm7) {
    const uLen = text.length
    return { encoding: 'UCS-2', chars: uLen, segments: uLen <= 70 ? 1 : Math.ceil(uLen / 67) }
  }
  return { encoding: 'GSM-7', chars: len, segments: len <= 160 ? 1 : Math.ceil(len / 153) }
})

const parsedNumbers = computed(() =>
  batchForm.numbers.split('\n').map(s => s.trim()).filter(s => s.startsWith('+')).slice(0, 500)
)

const onContentChange = () => {}

const onTemplateSelect = (id) => {
  const tpl = approvedTemplates.value.find(t => t.id === id)
  if (tpl) batchForm.content = tpl.content
}

const onFileChange = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    const lines = (e.target.result || '').split(/\r?\n/).map(l => l.trim()).filter(l => l.startsWith('+'))
    batchForm.numbers = lines.join('\n')
    ElMessage.success(`已导入 ${Math.min(lines.length, 500)} 个号码`)
  }
  reader.readAsText(file.raw)
}

onMounted(async () => {
  try {
    const [sidRes, tplRes] = await Promise.all([
      sidList(),
      templateList({ page: 1, size: 100, status: 'approved' })
    ])
    sidOptions.value = sidRes.data || []
    approvedTemplates.value = tplRes.data?.list || []
  } catch { /* ignore */ }
})

const handleQuickSend = async () => {
  await quickRef.value.validate()
  sending.value = true
  sendResult.value = null
  try {
    const res = await sendSms({
      to: quickForm.to,
      content: quickForm.content,
      sid: quickForm.sid || undefined,
      smsAttribute: quickForm.smsAttribute || undefined,
      clientRef: quickForm.clientRef || undefined
    })
    sendResult.value = { success: true, ...res.data }
  } catch (e) {
    sendResult.value = { success: false, error: e.message || '发送失败' }
  } finally {
    sending.value = false
  }
}

const handleBatchSend = async () => {
  if (!batchForm.content) { ElMessage.warning('请输入短信内容'); return }
  batchSending.value = true
  try {
    await sendBatch({
      numbers: parsedNumbers.value,
      content: batchForm.content,
      sid: batchForm.sid || undefined,
      smsAttribute: batchForm.smsAttribute || undefined
    })
    ElMessage.success(`批量发送任务已提交，共 ${parsedNumbers.value.length} 条`)
    batchForm.numbers = ''
    batchForm.content = ''
  } finally {
    batchSending.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 600; margin: 0; }
.encode-info { font-size: 12px; color: #999; margin-top: 6px; }
</style>
