<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <el-icon :size="28" color="#fff"><Message /></el-icon>
        </div>
        <h2 class="login-title">SMS 运营管理后台</h2>
        <p class="login-subtitle">International SMS SaaS Platform</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" style="margin-top: 32px;">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%;" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="login-footer">SMS Platform v2.0</div>
  </div>
</template>

<script setup>
import { ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import http from '../api/request'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = ref({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await http.post('/auth/login', form.value)
    authStore.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // error already shown by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100%; height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #001529 0%, #003a70 50%, #001d3d 100%);
  position: relative; overflow: hidden;
}
.login-bg {
  position: absolute; inset: 0;
  background: radial-gradient(circle at 30% 40%, rgba(64,158,255,0.15), transparent 50%),
              radial-gradient(circle at 70% 60%, rgba(64,158,255,0.1), transparent 50%);
}
.login-card {
  width: 400px; padding: 48px 40px 36px; background: #fff; border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3); position: relative; z-index: 1;
}
.login-header { text-align: center; }
.login-logo {
  width: 56px; height: 56px; border-radius: 14px; margin: 0 auto 16px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  display: flex; align-items: center; justify-content: center;
}
.login-title { margin: 0; font-size: 22px; font-weight: 700; color: #1a1a1a; }
.login-subtitle { margin: 4px 0 0; font-size: 13px; color: #909399; }
.login-footer {
  position: absolute; bottom: 24px; left: 50%; transform: translateX(-50%);
  color: rgba(255,255,255,0.3); font-size: 12px;
}
</style>
