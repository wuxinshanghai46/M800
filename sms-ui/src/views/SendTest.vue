<template>
  <el-row :gutter="24">
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header>
          <div style="display: flex; align-items: center; gap: 10px;">
            <div style="width: 36px; height: 36px; border-radius: 10px; background: linear-gradient(135deg, #409eff, #66b1ff); display: flex; align-items: center; justify-content: center;">
              <el-icon :size="18" color="#fff"><Position /></el-icon>
            </div>
            <div>
              <div style="font-weight: 600; font-size: 15px; color: #1a1a2e;">发送测试</div>
              <div style="font-size: 12px; color: #909399;">运营后台测试发送短信</div>
            </div>
          </div>
        </template>
        <el-form :model="form" label-width="90px" label-position="top">
          <el-form-item label="客户ID">
            <el-input v-model.number="form.customerId" placeholder="输入客户ID（数字）" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="目标号码">
                <el-input v-model="form.to" placeholder="+8613800138000">
                  <template #prefix><el-icon><Phone /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="Sender ID">
                <el-input v-model="form.sid" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="短信内容">
            <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="输入短信内容..." />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSend" :loading="sending" size="large" style="width: 100%; height: 44px; font-size: 15px;">
              <el-icon style="margin-right: 8px;"><Promotion /></el-icon>发送短信
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="hover" style="min-height: 540px;">
        <template #header>
          <div style="display: flex; align-items: center; gap: 10px;">
            <div style="width: 36px; height: 36px; border-radius: 10px; background: linear-gradient(135deg, #67c23a, #85ce61); display: flex; align-items: center; justify-content: center;">
              <el-icon :size="18" color="#fff"><Document /></el-icon>
            </div>
            <div>
              <div style="font-weight: 600; font-size: 15px; color: #1a1a2e;">发送结果</div>
              <div style="font-size: 12px; color: #909399;">API 响应详情</div>
            </div>
          </div>
        </template>
        <div v-if="result">
          <div :class="['result-banner', result.success ? 'success' : 'error']">
            <el-icon :size="28"><CircleCheck v-if="result.success" /><CircleClose v-else /></el-icon>
            <span>{{ result.success ? '发送成功' : '发送失败' }}</span>
          </div>
          <el-descriptions :column="1" border style="margin-top: 20px;">
            <el-descriptions-item label="消息ID">
              <code style="font-size: 13px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px;">{{ result.messageId || '-' }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="段数">{{ result.segments || '-' }}</el-descriptions-item>
            <el-descriptions-item label="编码">{{ result.encoding || '-' }}</el-descriptions-item>
            <el-descriptions-item label="费用">{{ result.price || '-' }} USD</el-descriptions-item>
            <el-descriptions-item label="错误信息" v-if="result.errorMessage">
              <span style="color: #f56c6c;">{{ result.errorMessage }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-else style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 380px; color: #c0c4cc;">
          <el-icon :size="64" style="margin-bottom: 16px;"><Promotion /></el-icon>
          <p style="font-size: 15px;">填写左侧表单发送测试短信</p>
          <p style="font-size: 13px; margin-top: 4px;">结果将在此处显示</p>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/request'

const sending = ref(false)
const form = ref({ customerId: '', to: '', sid: '', content: '' })
const result = ref(null)

const handleSend = async () => {
  if (!form.value.customerId || !form.value.to || !form.value.content) {
    ElMessage.warning('请填写客户ID、目标号码和短信内容')
    return
  }
  sending.value = true
  result.value = null
  try {
    const { customerId, ...body } = form.value
    const res = await http.post(`/api/sms/admin-send?customerId=${customerId}`, body)
    result.value = { success: true, ...(res.data || res) }
  } catch (e) {
    result.value = { success: false, errorMessage: e.message || '发送失败' }
  } finally { sending.value = false }
}
</script>

<style scoped>
.result-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
}
.result-banner.success { background: #f0f9eb; color: #67c23a; }
.result-banner.error { background: #fef0f0; color: #f56c6c; }
</style>
