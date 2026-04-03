<template>
  <el-container style="height: 100vh;">
    <!-- 顶部导航 -->
    <el-header class="portal-header">
      <div class="header-logo">
        <span class="logo-primary">Borui</span><span class="logo-accent">SMS</span>
        <span class="logo-divider">|</span>
        <span class="logo-sub">Portal</span>
      </div>
      <div class="header-right">
        <div class="balance-badge">
          <el-icon><Wallet /></el-icon>
          ${{ formatMoney(authStore.balance) }}
        </div>
        <el-dropdown @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="28" style="background: #1890ff; font-size: 12px;">
              {{ companyInitial }}
            </el-avatar>
            <span>{{ authStore.companyName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account">
                <el-icon><Setting /></el-icon>账户设置
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧导航 -->
      <el-aside width="220px" class="portal-sidebar">
        <el-menu :default-active="$route.path" router background-color="transparent"
          text-color="rgba(255,255,255,.65)" active-text-color="#fff"
          style="border: none; padding: 8px 0;">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容 -->
      <el-main class="portal-main">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { portalLogout } from '../api'
import { ElMessageBox } from 'element-plus'
import {
  DataAnalysis, Promotion, List, Stamp, Document,
  Wallet, Setting, ArrowDown, SwitchButton, Money
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const menuItems = [
  { path: '/dashboard', label: '概览 Dashboard', icon: DataAnalysis },
  { path: '/send',      label: '发送 Send',       icon: Promotion },
  { path: '/records',   label: '消息记录',         icon: List },
  { path: '/sid',       label: 'SID 管理',        icon: Stamp },
  { path: '/template',  label: '模板管理',         icon: Document },
  { path: '/billing',    label: '账单',            icon: Wallet },
  { path: '/price-calc', label: '发送量估算',       icon: Money },
  { path: '/account',   label: '账户设置',         icon: Setting },
]

const companyInitial = computed(() => {
  const name = authStore.companyName
  return name ? name.charAt(0).toUpperCase() : 'C'
})

const formatMoney = (v) => Number(v || 0).toFixed(2)

const handleCommand = async (cmd) => {
  if (cmd === 'account') {
    router.push('/account')
  } else if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    try { await portalLogout() } catch { /* ignore */ }
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.portal-header {
  background: #0d2137;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-logo { display: flex; align-items: center; gap: 0; font-size: 20px; font-weight: 700; }
.logo-primary { color: #fff; }
.logo-accent  { color: #1890ff; }
.logo-divider { color: rgba(255,255,255,.3); margin: 0 10px; font-weight: 200; }
.logo-sub     { color: rgba(255,255,255,.6); font-size: 13px; font-weight: 400; }

.header-right { display: flex; align-items: center; gap: 20px; }

.balance-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #52c41a;
  font-weight: 600;
  font-size: 14px;
  background: rgba(82,196,26,.12);
  padding: 4px 12px;
  border-radius: 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255,255,255,.85);
  font-size: 14px;
  cursor: pointer;
}

.portal-sidebar {
  background: #0d2137;
  height: calc(100vh - 56px);
  overflow-y: auto;
  border-right: none;
}
.portal-sidebar::-webkit-scrollbar { width: 4px; }
.portal-sidebar::-webkit-scrollbar-thumb { background: rgba(255,255,255,.2); border-radius: 2px; }

:deep(.el-menu-item.is-active) {
  background: #1890ff !important;
  color: #fff !important;
}
:deep(.el-menu-item:hover) {
  background: rgba(255,255,255,.06) !important;
  color: #fff !important;
}

.portal-main {
  background: #f0f2f5;
  padding: 20px 24px;
  overflow-y: auto;
  height: calc(100vh - 56px);
}

.breadcrumb { margin-bottom: 16px; }

.fade-enter-active, .fade-leave-active { transition: opacity .15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
