import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

// menu key → route path mapping for permission check
const menuRoutes = [
  { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '概览', perm: 'dashboard' } },
  { path: 'country', name: 'Country', component: () => import('../views/CountryPage.vue'), meta: { title: '国家管理', perm: 'country' } },
  { path: 'vendor', name: 'Vendor', component: () => import('../views/VendorPage.vue'), meta: { title: '供应商管理', perm: 'vendor' } },
  { path: 'channel', name: 'Channel', component: () => import('../views/ChannelPage.vue'), meta: { title: '通道管理', perm: 'channel' } },
  { path: 'sid', name: 'Sid', component: () => import('../views/SidPage.vue'), meta: { title: 'SID管理', perm: 'sid' } },
  { path: 'customer', name: 'Customer', component: () => import('../views/Customer.vue'), meta: { title: '客户管理', perm: 'customer' } },
  { path: 'risk', name: 'Risk', component: () => import('../views/Risk.vue'), meta: { title: '风控管理', perm: 'risk' } },
  { path: 'message', name: 'Message', component: () => import('../views/Message.vue'), meta: { title: '发送管理', perm: 'message' } },
  { path: 'stats', name: 'Stats', component: () => import('../views/Stats.vue'), meta: { title: '发送统计', perm: 'stats' } },
  { path: 'send-test', name: 'SendTest', component: () => import('../views/SendTest.vue'), meta: { title: '发送测试', perm: 'send-test' } },
  { path: 'audit', name: 'Audit', component: () => import('../views/Audit.vue'), meta: { title: '审计日志', perm: 'audit' } },
  { path: 'monitoring', name: 'Monitoring', component: () => import('../views/Monitoring.vue'), meta: { title: '监控告警', perm: 'monitoring' } },
  { path: 'finance', name: 'Finance', component: () => import('../views/Finance.vue'), meta: { title: '财务结算', perm: 'finance' } },
  { path: 'system', name: 'SystemConfig', component: () => import('../views/SystemConfig.vue'), meta: { title: '系统配置', perm: 'system' } },
  { path: 'ops-strategy', name: 'OpsStrategy', component: () => import('../views/OpsStrategy.vue'), meta: { title: '运营策略', perm: 'ops-strategy' } },
  { path: 'number-routing', name: 'NumberRouting', component: () => import('../views/NumberRouting.vue'), meta: { title: '号码路由', perm: 'number-routing' } },
  { path: 'template', name: 'Template', component: () => import('../views/Template.vue'), meta: { title: '模板管理', perm: 'template' } },
  { path: 'quality', name: 'Quality', component: () => import('../views/Quality.vue'), meta: { title: '质量分析', perm: 'quality' } },
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: menuRoutes
  }
]

const router = createRouter({ history: createWebHistory(), routes })

// navigation guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    // already logged in, redirect to home
    if (token && to.name === 'Login') return next('/')
    return next()
  }
  if (!token) {
    return next('/login')
  }
  // permission check
  const perm = to.meta.perm
  if (perm) {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const perms = user.permissions || []
    if (user.roleCode !== 'SUPER_ADMIN' && !perms.includes(perm)) {
      return next('/dashboard')
    }
  }
  next()
})

export { menuRoutes }
export default router
