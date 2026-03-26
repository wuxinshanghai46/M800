import http from './request'

// Auth
export const login = d => http.post('/auth/login', d)

// Country — POST /admin/country, PUT /admin/country/{id}, DELETE /admin/country/{id}
export const countryList = p => http.get('/admin/country/list', { params: p })
export const countryAll = () => http.get('/admin/country/all')
export const countrySave = d => http.post('/admin/country', d)
export const countryUpdate = (id, d) => http.put(`/admin/country/${id}`, d)
export const countryDel = id => http.delete(`/admin/country/${id}`)

// Vendor — POST /admin/vendor, PUT /admin/vendor/{id}, DELETE /admin/vendor/{id}
export const vendorList = p => http.get('/admin/vendor/list', { params: p })
export const vendorSave = d => http.post('/admin/vendor', d)
export const vendorUpdate = (id, d) => http.put(`/admin/vendor/${id}`, d)
export const vendorDel = id => http.delete(`/admin/vendor/${id}`)

// Channel — POST /admin/channel, PUT /admin/channel/{id}, DELETE /admin/channel/{id}
export const channelList = p => http.get('/admin/channel/list', { params: p })
export const channelSave = d => http.post('/admin/channel', d)
export const channelUpdate = (id, d) => http.put(`/admin/channel/${id}`, d)
export const channelDel = id => http.delete(`/admin/channel/${id}`)

// SID — POST /admin/sid, PUT /admin/sid/{id}, DELETE /admin/sid/{id}
export const sidList = p => http.get('/admin/sid/list', { params: p })
export const sidSave = d => http.post('/admin/sid', d)
export const sidUpdate = (id, d) => http.put(`/admin/sid/${id}`, d)
export const sidDel = id => http.delete(`/admin/sid/${id}`)

// Customer — POST /admin/customer, PUT /admin/customer/{id}
export const customerList = p => http.get('/admin/customer/list', { params: p })
export const customerSave = d => http.post('/admin/customer', d)
export const customerUpdate = (id, d) => http.put(`/admin/customer/${id}`, d)
export const customerDetail = id => http.get(`/admin/customer/${id}/detail`)

// Message
export const messageList = p => http.get('/admin/message/list', { params: p })

// Stats
export const statsDashboard = () => http.get('/admin/stats/dashboard')
export const statsByCountry = p => http.get('/admin/stats/country', { params: p })
export const statsByCustomer = p => http.get('/admin/stats/customer', { params: p })
export const statsByVendor = p => http.get('/admin/stats/vendor', { params: p })
export const statsByChannel = p => http.get('/admin/stats/channel', { params: p })

// Risk — POST /admin/risk/blacklist, DELETE /admin/risk/blacklist/{id}
export const blacklistList = p => http.get('/admin/risk/blacklist/list', { params: p })
export const blacklistSave = d => http.post('/admin/risk/blacklist', d)
export const blacklistUpdate = (id, d) => http.put(`/admin/risk/blacklist/${id}`, d)
export const blacklistDel = id => http.delete(`/admin/risk/blacklist/${id}`)
export const sensitiveWordList = p => http.get('/admin/risk/sensitive-word/list', { params: p })
export const sensitiveWordSave = d => http.post('/admin/risk/sensitive-word', d)
export const sensitiveWordUpdate = (id, d) => http.put(`/admin/risk/sensitive-word/${id}`, d)
export const sensitiveWordDel = id => http.delete(`/admin/risk/sensitive-word/${id}`)

// Audit
export const auditOperationList = p => http.get('/admin/audit/operation', { params: p })
export const auditLoginList    = p => http.get('/admin/audit/login', { params: p })

// MO Record (上行记录)
export const moRecordList = p => http.get('/admin/mo/list', { params: p })

// Send SMS
export const sendSms = d => http.post('/api/sms/send', d)

// ==================== Finance ====================
// Tab1: Profit
export const profitSummary = p => http.get('/admin/finance/profit/summary', { params: p })
export const profitReport = p => http.get('/admin/finance/profit/report', { params: p })
// Tab2: Bill
export const billPage = p => http.get('/admin/finance/bill/page', { params: p })
export const billGenerate = p => http.post('/admin/finance/bill/generate', null, { params: p })
export const billIssue = id => http.put(`/admin/finance/bill/${id}/issue`)
export const billPay = id => http.put(`/admin/finance/bill/${id}/pay`)
export const billDetail = id => http.get(`/admin/finance/bill/${id}`)
// Tab3: Account
export const accountList = p => http.get('/admin/finance/account/list', { params: p })
// Tab4: Vendor Price
export const vendorPricePage = p => http.get('/admin/finance/vendor-price/page', { params: p })
export const vendorPriceSave = d => http.post('/admin/finance/vendor-price', d)
export const vendorPriceUpdate = (id, d) => http.put(`/admin/finance/vendor-price/${id}`, d)
export const vendorPriceDel = id => http.delete(`/admin/finance/vendor-price/${id}`)
// Tab5: Exchange Rate
export const exchangeRateList = () => http.get('/admin/finance/exchange-rate/list')
export const exchangeRateSave = d => http.post('/admin/finance/exchange-rate', d)
export const exchangeRateUpdate = (id, d) => http.put(`/admin/finance/exchange-rate/${id}`, d)
export const exchangeRateDel = id => http.delete(`/admin/finance/exchange-rate/${id}`)

// ==================== OpsStrategy ====================
// Tab1: Review
export const reviewRuleList = () => http.get('/admin/ops-strategy/review-rule/list')
export const reviewRuleSave = d => http.post('/admin/ops-strategy/review-rule', d)
export const reviewRuleToggle = (id, active) => http.put(`/admin/ops-strategy/review-rule/${id}/toggle`, null, { params: { active } })
export const reviewTicketPage = p => http.get('/admin/ops-strategy/review-ticket/page', { params: p })
export const reviewTicketReview = (id, action, reviewer) => http.put(`/admin/ops-strategy/review-ticket/${id}/review`, null, { params: { action, reviewer } })
// Tab2: Routing Strategy
export const routingStrategyList = () => http.get('/admin/ops-strategy/routing-strategy/list')
export const routingStrategySave = d => http.post('/admin/ops-strategy/routing-strategy', d)
export const routingStrategyDel = id => http.delete(`/admin/ops-strategy/routing-strategy/${id}`)
// Tab3: Channel Group
export const channelGroupList = () => http.get('/admin/ops-strategy/channel-group/list')
export const channelGroupSave = d => http.post('/admin/ops-strategy/channel-group', d)
export const channelGroupToggle = (id, active) => http.put(`/admin/ops-strategy/channel-group/${id}/toggle`, null, { params: { active } })
export const channelGroupMembers = groupId => http.get(`/admin/ops-strategy/channel-group/${groupId}/members`)
export const channelGroupMemberAdd = (groupId, d) => http.post(`/admin/ops-strategy/channel-group/${groupId}/member`, d)
export const channelGroupMemberUpdate = (memberId, d) => http.put(`/admin/ops-strategy/channel-group/member/${memberId}`, d)
export const channelGroupMemberDel = memberId => http.delete(`/admin/ops-strategy/channel-group/member/${memberId}`)
// Tab4: Template Replace
export const tplReplaceList = p => http.get('/admin/ops-strategy/template-replace/list', { params: p })
export const tplReplaceSave = d => http.post('/admin/ops-strategy/template-replace', d)
export const tplReplaceToggle = (id, active) => http.put(`/admin/ops-strategy/template-replace/${id}/toggle`, null, { params: { active } })
// Tab5: SID Replace
export const sidReplaceList = () => http.get('/admin/ops-strategy/sid-replace/list')
export const sidReplaceSave = d => http.post('/admin/ops-strategy/sid-replace', d)
export const sidReplaceToggle = (id, active) => http.put(`/admin/ops-strategy/sid-replace/${id}/toggle`, null, { params: { active } })

// ==================== SystemConfig ====================
export const configGet = group => http.get(`/admin/system/config/${group}`)
export const configGetDetail = group => http.get(`/admin/system/config/${group}/detail`)
export const configBatchUpdate = (group, d) => http.put(`/admin/system/config/${group}`, d)
export const roleList = () => http.get('/admin/system/role/list')
export const roleSave = d => http.post('/admin/system/role', d)
export const roleDel = id => http.delete(`/admin/system/role/${id}`)
export const userPage = p => http.get('/admin/system/user/page', { params: p })
export const userSave = d => http.post('/admin/system/user', d)
export const userToggle = (id, active) => http.put(`/admin/system/user/${id}/toggle`, null, { params: { active } })
export const userResetPwd = (id, password) => http.put(`/admin/system/user/${id}/reset-password`, null, { params: { password } })

// ==================== Monitoring ====================
export const monitorDashboard = () => http.get('/admin/monitoring/dashboard')
export const alertRecordPage = p => http.get('/admin/monitoring/alert/page', { params: p })
export const alertAcknowledge = (id, user) => http.put(`/admin/monitoring/alert/${id}/acknowledge`, null, { params: { user } })
export const alertResolve = id => http.put(`/admin/monitoring/alert/${id}/resolve`)
export const channelMonitorList = () => http.get('/admin/monitoring/channel/list')
export const alertRuleList = () => http.get('/admin/monitoring/alert-rule/list')
export const alertRuleSave = d => http.post('/admin/monitoring/alert-rule', d)
export const alertRuleToggle = (id, active) => http.put(`/admin/monitoring/alert-rule/${id}/toggle`, null, { params: { active } })
export const alertRuleDel = id => http.delete(`/admin/monitoring/alert-rule/${id}`)
export const queueStats = () => http.get('/admin/monitoring/queue/stats')

// ==================== Template ====================
export const templatePage = p => http.get('/admin/template/page', { params: p })
export const templateDetail = id => http.get(`/admin/template/${id}`)
export const templateSave = d => http.post('/admin/template', d)
export const templateDel = id => http.delete(`/admin/template/${id}`)
export const templateVarList = () => http.get('/admin/template/variable/list')
export const templateVarSave = d => http.post('/admin/template/variable', d)
export const templateVarDel = id => http.delete(`/admin/template/variable/${id}`)
export const templateReviewQueue = p => http.get('/admin/template/review/queue', { params: p })
export const templateApprove = (id) => http.put(`/admin/template/${id}/approve`)
export const templateReject = (id, reason) => http.put(`/admin/template/${id}/reject`, null, { params: { reason } })
export const templateCarrierApprove = (id) => http.put(`/admin/template/${id}/carrier-approve`)
export const templateCarrierReject = (id, reason) => http.put(`/admin/template/${id}/carrier-reject`, null, { params: { reason } })
export const channelTestSmpp = (id) => http.post(`/admin/channel/${id}/test-smpp`)
export const channelTestHttp = (id) => http.post(`/admin/channel/${id}/test-http`)
export const channelSmppStatus = () => http.get('/admin/channel/smpp/status')

// ==================== NumberRouting ====================
export const segmentPage = p => http.get('/admin/number-routing/segment/page', { params: p })
export const segmentSave = d => http.post('/admin/number-routing/segment', d)
export const segmentDel = id => http.delete(`/admin/number-routing/segment/${id}`)
export const mnpCachePage = p => http.get('/admin/number-routing/mnp/page', { params: p })
export const mnpClearExpired = () => http.delete('/admin/number-routing/mnp/expired')
export const defaultRouteList = p => http.get('/admin/number-routing/route/list', { params: p })
export const defaultRouteSave = d => http.post('/admin/number-routing/route', d)
export const defaultRouteDel = id => http.delete(`/admin/number-routing/route/${id}`)
export const defaultRouteToggle = (id, active) => http.put(`/admin/number-routing/route/${id}/toggle`, null, { params: { active } })
export const filterRuleList = p => http.get('/admin/number-routing/filter/list', { params: p })
export const filterRuleSave = d => http.post('/admin/number-routing/filter', d)
export const filterRuleDel = id => http.delete(`/admin/number-routing/filter/${id}`)
export const filterRuleToggle = (id, active) => http.put(`/admin/number-routing/filter/${id}/toggle`, null, { params: { active } })
