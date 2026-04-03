import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard',  component: () => import('../views/Dashboard.vue'),  meta: { title: '概览' } },
      { path: 'send',      name: 'Send',        component: () => import('../views/Send.vue'),        meta: { title: '发送' } },
      { path: 'records',   name: 'Records',     component: () => import('../views/Records.vue'),     meta: { title: '消息记录' } },
      { path: 'sid',       name: 'Sid',         component: () => import('../views/Sid.vue'),         meta: { title: 'SID 管理' } },
      { path: 'template',  name: 'Template',    component: () => import('../views/Template.vue'),    meta: { title: '模板管理' } },
      { path: 'billing',    name: 'Billing',     component: () => import('../views/Billing.vue'),     meta: { title: '账单' } },
      { path: 'price-calc', name: 'PriceCalc',  component: () => import('../views/PriceCalc.vue'),   meta: { title: '发送量估算' } },
      { path: 'account',   name: 'Account',     component: () => import('../views/Account.vue'),     meta: { title: '账户设置' } },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 短信发送平台` : '短信发送平台'
  const token = localStorage.getItem('portal_token')
  if (to.meta.public) {
    if (token && to.name === 'Login') return next('/dashboard')
    return next()
  }
  if (!token) return next('/login')
  next()
})

export default router
