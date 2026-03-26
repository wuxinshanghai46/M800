<template>
  <el-container style="height: 100%">
    <el-aside :width="isCollapse ? '64px' : '240px'" style="transition: width .3s;">
      <div class="sidebar-inner">
        <div class="sidebar-logo">
          <div class="logo-icon"><el-icon :size="22"><Message /></el-icon></div>
          <transition name="fade">
            <div v-show="!isCollapse" class="logo-text">
              <span class="logo-title">M800 运营后台</span>
              <span class="logo-sub">SMS Admin Console</span>
            </div>
          </transition>
        </div>

        <el-scrollbar style="flex: 1;">
          <el-menu :default-active="$route.path" router :collapse="isCollapse" background-color="transparent" text-color="rgba(255,255,255,0.65)" active-text-color="#fff" :collapse-transition="false" style="border: none;">

            <template v-for="group in visibleMenuGroups" :key="group.label">
              <div class="menu-group-title" v-show="!isCollapse && group.label">{{ group.label }}</div>
              <el-menu-item v-for="item in group.items" :key="item.path" :index="'/' + item.path">
                <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
              </el-menu-item>
            </template>

          </el-menu>
        </el-scrollbar>

        <div class="sidebar-footer" v-show="!isCollapse">
          <div class="env-badge"><span class="env-dot"></span>v2.0</div>
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="main-header">
        <div style="display: flex; align-items: center; gap: 16px;">
          <el-icon :size="20" style="cursor: pointer; color: #606266;" @click="isCollapse = !isCollapse"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          <el-breadcrumb separator="/"><el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item><el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item></el-breadcrumb>
        </div>
        <div style="display: flex; align-items: center; gap: 20px;">
          <el-badge :value="3" :max="99"><el-icon :size="18" style="color: #606266; cursor: pointer;"><Bell /></el-icon></el-badge>
          <el-dropdown @command="handleCommand">
            <div style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
              <el-avatar :size="32" style="background: linear-gradient(135deg, #409eff, #66b1ff); font-size: 14px;">{{ avatarChar }}</el-avatar>
              <span style="font-size: 14px; color: #303133;">{{ authStore.realName }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><UserFilled /></el-icon>{{ authStore.user?.roleName || '—' }}
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main style="background: #f0f2f5; padding: 24px; overflow-y: auto;">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in"><component :is="Component" /></transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  DataAnalysis, Location, OfficeBuilding, Connection, Stamp, User, Warning,
  List, Wallet, TrendCharts, DataLine, Tickets, Setting, Monitor, Guide,
  Tools, Document, Position, Message, Bell, ArrowDown, SwitchButton,
  Fold, Expand, UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)

const avatarChar = computed(() => {
  const name = authStore.realName
  return name ? name.charAt(0).toUpperCase() : 'U'
})

// menu definition with permission keys
const menuDef = [
  {
    label: '',
    items: [{ path: 'dashboard', title: '概览', icon: 'DataAnalysis', perm: 'dashboard' }]
  },
  {
    label: '资源管理',
    items: [
      { path: 'country', title: '国家管理', icon: 'Location', perm: 'country' },
      { path: 'vendor', title: '供应商管理', icon: 'OfficeBuilding', perm: 'vendor' },
      { path: 'channel', title: '通道管理', icon: 'Connection', perm: 'channel' },
      { path: 'sid', title: 'SID管理', icon: 'Stamp', perm: 'sid' },
    ]
  },
  {
    label: '业务管理',
    items: [
      { path: 'customer', title: '客户管理', icon: 'User', perm: 'customer' },
      { path: 'risk', title: '风控管理', icon: 'Warning', perm: 'risk' },
      { path: 'message', title: '发送管理', icon: 'List', perm: 'message' },
      { path: 'finance', title: '财务结算', icon: 'Wallet', perm: 'finance' },
      { path: 'stats', title: '发送统计', icon: 'TrendCharts', perm: 'stats' },
      { path: 'quality', title: '质量分析', icon: 'DataLine', perm: 'quality' },
      { path: 'template', title: '模板管理', icon: 'Tickets', perm: 'template' },
    ]
  },
  {
    label: '运维中心',
    items: [
      { path: 'ops-strategy', title: '运营策略', icon: 'Setting', perm: 'ops-strategy' },
      { path: 'monitoring', title: '监控告警', icon: 'Monitor', perm: 'monitoring' },
      { path: 'number-routing', title: '号码路由', icon: 'Guide', perm: 'number-routing' },
    ]
  },
  {
    label: '系统',
    items: [
      { path: 'system', title: '系统配置', icon: 'Tools', perm: 'system' },
      { path: 'audit', title: '审计日志', icon: 'Document', perm: 'audit' },
      { path: 'send-test', title: '发送测试', icon: 'Position', perm: 'send-test' },
    ]
  }
]

const iconMap = {
  DataAnalysis, Location, OfficeBuilding, Connection, Stamp, User, Warning,
  List, Wallet, TrendCharts, DataLine, Tickets, Setting, Monitor, Guide,
  Tools, Document, Position
}

// filter menu by permission
const visibleMenuGroups = computed(() => {
  return menuDef.map(group => ({
    label: group.label,
    items: group.items
      .filter(item => authStore.hasPermission(item.perm))
      .map(item => ({ ...item, icon: iconMap[item.icon] || item.icon }))
  })).filter(group => group.items.length > 0)
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.sidebar-inner { height: 100%; display: flex; flex-direction: column; background: linear-gradient(180deg, #001529 0%, #001d3d 100%); overflow: hidden; }
.sidebar-logo { height: 56px; display: flex; align-items: center; padding: 0 16px; gap: 12px; border-bottom: 1px solid rgba(255,255,255,0.08); flex-shrink: 0; }
.logo-icon { width: 36px; height: 36px; border-radius: 10px; background: linear-gradient(135deg, #409eff, #66b1ff); display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.logo-text { display: flex; flex-direction: column; }
.logo-title { color: #fff; font-size: 15px; font-weight: 700; line-height: 1.2; white-space: nowrap; }
.logo-sub { color: rgba(255,255,255,0.4); font-size: 11px; white-space: nowrap; }
.sidebar-footer { padding: 12px 16px; border-top: 1px solid rgba(255,255,255,0.08); flex-shrink: 0; }
.env-badge { display: flex; align-items: center; gap: 8px; font-size: 12px; color: rgba(255,255,255,0.4); padding: 6px 10px; background: rgba(255,255,255,0.04); border-radius: 6px; }
.env-dot { width: 8px; height: 8px; background: #67c23a; border-radius: 50%; animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
.menu-group-title { padding: 16px 20px 6px; font-size: 11px; color: rgba(255,255,255,0.3); text-transform: uppercase; letter-spacing: 1px; font-weight: 600; }
.main-header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e8e8e8; background: #fff; padding: 0 24px; height: 56px; box-shadow: 0 1px 4px rgba(0,0,0,.04); z-index: 10; }
:deep(.el-menu-item.is-active) { background: rgba(64,158,255,0.15) !important; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { background: rgba(255,255,255,0.06) !important; }
:deep(.el-menu-item), :deep(.el-sub-menu__title) { height: 44px; line-height: 44px; margin: 2px 8px; border-radius: 8px; }
.fade-slide-enter-active { transition: all .2s ease-out; } .fade-slide-leave-active { transition: all .15s ease-in; }
.fade-slide-enter-from { opacity: 0; transform: translateY(6px); } .fade-slide-leave-to { opacity: 0; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; } .fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
