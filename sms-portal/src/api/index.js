import http from './request'

// ==================== 认证 ====================
export const portalLogin  = d  => http.post('/portal/auth/login', d)
export const portalLogout = () => http.post('/portal/auth/logout')

// ==================== 概览 ====================
export const dashboardStats = () => http.get('/portal/dashboard/stats')

// ==================== 发送 ====================
export const sendSms   = d => http.post('/portal/sms/send', d)
export const sendBatch = d => http.post('/portal/sms/send-batch', d)

// ==================== 消息记录 ====================
export const messageList   = p  => http.get('/portal/messages', { params: p })
export const messageDetail = id => http.get(`/portal/messages/${id}`)
export const moList        = p  => http.get('/portal/mo', { params: p })

// ==================== SID ====================
export const sidList = () => http.get('/portal/sids')

// ==================== 模板 ====================
export const templateList   = p       => http.get('/portal/templates', { params: p })
export const templateSave   = d       => http.post('/portal/templates', d)
export const templateUpdate = (id, d) => http.put(`/portal/templates/${id}`, d)
export const templateDel    = id      => http.delete(`/portal/templates/${id}`)

// ==================== 账单 ====================
export const billingAccount  = ()  => http.get('/portal/billing/account')
export const billingTxList   = p   => http.get('/portal/billing/transactions', { params: p })
export const billingBillList = p   => http.get('/portal/billing/bills', { params: p })

// ==================== 账户 ====================
export const accountInfo        = ()  => http.get('/portal/account/info')
export const accountChangePwd   = d   => http.put('/portal/account/password', d)
export const accountCredential  = ()  => http.get('/portal/account/credential')
export const accountRegenKey    = d   => http.post('/portal/account/credential/regen-key', d)
export const accountAddIp       = ip  => http.post('/portal/account/credential/ip', { ip })
export const accountRemoveIp    = ip  => http.delete('/portal/account/credential/ip', { params: { ip } })
export const accountCallbackGet = ()  => http.get('/portal/account/callback')
export const accountCallbackSave = d  => http.put('/portal/account/callback', d)
export const accountCallbackTest = type => http.post('/portal/account/callback/test', null, { params: { type } })
export const accountOpLog       = p   => http.get('/portal/account/logs', { params: p })
