<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-logo">
        <span class="logo-primary">短信</span><span class="logo-accent">发送平台</span>
      </div>
      <div class="login-title">SMS Sending Platform</div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" style="margin-top: 24px;">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large"
            :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%; margin-top: 8px;"
          :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

      <div class="login-footer">如需帮助，请联系您的客户经理</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { portalLogin } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await portalLogin({ account: form.username, password: form.password })
    authStore.setAuth(res.data.token, res.data)
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0d2137 0%, #1a3a5c 60%, #0d2137 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-box {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  width: 400px;
  box-shadow: 0 20px 60px rgba(0,0,0,.3);
}
.login-logo {
  font-size: 26px;
  font-weight: 700;
  text-align: center;
  letter-spacing: 1px;
}
.logo-primary { color: #0d2137; }
.logo-accent  { color: #1890ff; }
.logo-sub     { font-size: 14px; color: #999; font-weight: 400; margin-left: 6px; vertical-align: middle; }
.login-title  { text-align: center; color: #666; font-size: 14px; margin-top: 6px; }
.login-footer { text-align: center; margin-top: 24px; font-size: 12px; color: #bbb; }
</style>
