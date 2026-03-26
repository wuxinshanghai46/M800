package com.borui.sms.admin.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.security.JwtUtils;
import com.borui.sms.admin.vo.req.PortalChangePwdReq;
import com.borui.sms.admin.vo.req.PortalLoginReq;
import com.borui.sms.admin.vo.resp.PortalLoginResp;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.util.IdGenerator;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.common.domain.enums.CustomerStatus;
import com.borui.sms.common.domain.enums.MessageStatus;
import com.borui.sms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalService {

    private final CustomerMapper customerMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerApiCredentialMapper credentialMapper;
    private final CustomerCallbackConfigMapper callbackConfigMapper;
    private final CustomerSidMapper customerSidMapper;
    private final SidInfoMapper sidInfoMapper;
    private final MessageMapper messageMapper;
    private final MoRecordMapper moRecordMapper;
    private final SmsTemplateMapper smsTemplateMapper;
    private final BillMapper billMapper;
    private final BillingTransactionMapper billingTransactionMapper;
    private final OperationLogMapper operationLogMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    // ===== Auth =====

    public PortalLoginResp login(PortalLoginReq req) {
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getPortalAccount, req.getAccount()));
        if (customer == null || !passwordEncoder.matches(req.getPassword(), customer.getPortalPassword())) {
            throw new BizException(ErrorCode.AUTH_FAILED, "账号或密码错误");
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BizException(ErrorCode.AUTH_FAILED, "账号已禁用，请联系客户经理");
        }

        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>()
                        .eq(CustomerAccount::getCustomerId, customer.getId()));
        BigDecimal balance = account != null ? account.getBalance() : BigDecimal.ZERO;
        String currency = account != null ? account.getCurrency() : "USD";

        String token = jwtUtils.generatePortalToken(
                customer.getId(),
                customer.getPortalAccount(),
                customer.getCompanyName() != null ? customer.getCompanyName() : customer.getCustomerName());

        return new PortalLoginResp(token, customer.getId(),
                customer.getPortalAccount(),
                customer.getCompanyName() != null ? customer.getCompanyName() : customer.getCustomerName(),
                balance, currency);
    }

    // ===== Account Info =====

    public Map<String, Object> getAccountInfo(Long customerId) {
        Customer customer = requireCustomer(customerId);
        Map<String, Object> info = new HashMap<>();
        info.put("companyName", customer.getCompanyName());
        info.put("contactName", customer.getContactName());
        info.put("contactEmail", customer.getContactEmail());
        info.put("contactPhone", customer.getContactPhone());
        info.put("timezone", customer.getTimezone());
        info.put("createdAt", customer.getCreatedAt());
        return info;
    }

    @Transactional
    public void changePassword(Long customerId, PortalChangePwdReq req) {
        Customer customer = requireCustomer(customerId);
        if (!passwordEncoder.matches(req.getOldPassword(), customer.getPortalPassword())) {
            throw new BizException(ErrorCode.AUTH_FAILED, "旧密码错误");
        }
        customer.setPortalPassword(passwordEncoder.encode(req.getNewPassword()));
        customerMapper.updateById(customer);
    }

    // ===== API Credential =====

    public CustomerApiCredential getCredential(Long customerId) {
        return credentialMapper.selectOne(
                new LambdaQueryWrapper<CustomerApiCredential>()
                        .eq(CustomerApiCredential::getCustomerId, customerId)
                        .eq(CustomerApiCredential::getIsActive, true)
                        .last("LIMIT 1"));
    }

    @Transactional
    public CustomerApiCredential regenApiKey(Long customerId, String password) {
        Customer customer = requireCustomer(customerId);
        if (!passwordEncoder.matches(password, customer.getPortalPassword())) {
            throw new BizException(ErrorCode.AUTH_FAILED, "密码错误");
        }
        CustomerApiCredential cred = getCredential(customerId);
        if (cred == null) {
            cred = new CustomerApiCredential();
            cred.setCustomerId(customerId);
            cred.setIsActive(true);
        }
        cred.setApiKey(IdGenerator.apiKey());
        cred.setApiSecret(IdGenerator.apiSecret());
        if (cred.getId() == null) {
            credentialMapper.insert(cred);
        } else {
            credentialMapper.updateById(cred);
        }
        return cred;
    }

    @Transactional
    public void addIp(Long customerId, String ip) {
        CustomerApiCredential cred = getOrCreateCredential(customerId);
        String existing = cred.getIpWhitelist();
        if (existing != null && existing.contains(ip)) return;
        String updated = (existing == null || existing.isBlank()) ? ip : existing + "," + ip;
        cred.setIpWhitelist(updated);
        credentialMapper.updateById(cred);
    }

    @Transactional
    public void removeIp(Long customerId, String ip) {
        CustomerApiCredential cred = getOrCreateCredential(customerId);
        if (cred.getIpWhitelist() == null) return;
        String updated = java.util.Arrays.stream(cred.getIpWhitelist().split(","))
                .map(String::trim)
                .filter(s -> !s.equals(ip) && !s.isBlank())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        cred.setIpWhitelist(updated);
        credentialMapper.updateById(cred);
    }

    private CustomerApiCredential getOrCreateCredential(Long customerId) {
        CustomerApiCredential cred = getCredential(customerId);
        if (cred == null) {
            cred = new CustomerApiCredential();
            cred.setCustomerId(customerId);
            cred.setApiKey(IdGenerator.apiKey());
            cred.setApiSecret(IdGenerator.apiSecret());
            cred.setIsActive(true);
            credentialMapper.insert(cred);
        }
        return cred;
    }

    // ===== Callback Config =====

    public CustomerCallbackConfig getCallbackConfig(Long customerId) {
        CustomerCallbackConfig config = callbackConfigMapper.selectOne(
                new LambdaQueryWrapper<CustomerCallbackConfig>()
                        .eq(CustomerCallbackConfig::getCustomerId, customerId));
        if (config == null) {
            config = new CustomerCallbackConfig();
            config.setCustomerId(customerId);
        }
        return config;
    }

    @Transactional
    public void saveCallbackConfig(Long customerId, CustomerCallbackConfig req) {
        CustomerCallbackConfig config = callbackConfigMapper.selectOne(
                new LambdaQueryWrapper<CustomerCallbackConfig>()
                        .eq(CustomerCallbackConfig::getCustomerId, customerId));
        if (config == null) {
            req.setCustomerId(customerId);
            callbackConfigMapper.insert(req);
        } else {
            config.setDlrUrl(req.getDlrUrl());
            config.setDlrSecret(req.getDlrSecret());
            config.setMoUrl(req.getMoUrl());
            config.setMoSecret(req.getMoSecret());
            callbackConfigMapper.updateById(config);
        }
    }

    // ===== Dashboard Stats =====

    public Map<String, Object> dashboardStats(Long customerId) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // Today sent count
        long todaySent = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getCustomerId, customerId)
                        .ge(Message::getCreatedAt, todayStart)
                        .lt(Message::getCreatedAt, todayEnd));

        // Today delivered count
        long todayDelivered = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getCustomerId, customerId)
                        .eq(Message::getStatus, MessageStatus.DELIVERED)
                        .ge(Message::getCreatedAt, todayStart)
                        .lt(Message::getCreatedAt, todayEnd));

        double deliveryRate = todaySent > 0 ? (double) todayDelivered / todaySent * 100 : 0;

        // Account balance
        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>()
                        .eq(CustomerAccount::getCustomerId, customerId));
        BigDecimal balance = account != null ? account.getBalance() : BigDecimal.ZERO;

        // Today cost: sum of price*segments for DELIVERED/SUBMIT billed messages today
        // Approximate: count delivered messages' costs
        BigDecimal todayCost = BigDecimal.ZERO;
        try {
            // Simple approach: sum from recent messages
            List<Message> todayMsgs = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getCustomerId, customerId)
                            .eq(Message::getStatus, MessageStatus.DELIVERED)
                            .ge(Message::getCreatedAt, todayStart)
                            .lt(Message::getCreatedAt, todayEnd)
                            .select(Message::getPrice, Message::getSegments));
            for (Message m : todayMsgs) {
                if (m.getPrice() != null) {
                    int seg = m.getSegments() != null ? m.getSegments() : 1;
                    todayCost = todayCost.add(m.getPrice().multiply(BigDecimal.valueOf(seg)));
                }
            }
        } catch (Exception e) {
            log.warn("dashboardStats cost calc error: {}", e.getMessage());
        }

        // Recent 10 messages
        List<Message> recent = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getCustomerId, customerId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT 10"));

        stats.put("todaySent", todaySent);
        stats.put("deliveryRate", Math.round(deliveryRate * 10) / 10.0);
        stats.put("balance", balance);
        stats.put("todayCost", todayCost);
        stats.put("recentMessages", recent);
        return stats;
    }

    // ===== Messages =====

    public PageResult<Message> messageList(Long customerId, int page, int size,
                                           String status, String countryCode,
                                           String keyword,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        Page<Message> p = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getCustomerId, customerId);
        if (StringUtils.isNotBlank(status)) wrapper.eq(Message::getStatus, MessageStatus.valueOf(status));
        if (StringUtils.isNotBlank(countryCode)) wrapper.eq(Message::getCountryCode, countryCode);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Message::getMessageId, keyword)
                    .or().like(Message::getToNumber, keyword));
        }
        if (startTime != null) wrapper.ge(Message::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(Message::getCreatedAt, endTime);
        wrapper.orderByDesc(Message::getCreatedAt);
        messageMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<MoRecord> moList(Long customerId, int page, int size,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        // MO records are matched to customer by the SID assigned to that customer
        List<CustomerSid> assignments = customerSidMapper.selectList(
                new LambdaQueryWrapper<CustomerSid>().eq(CustomerSid::getCustomerId, customerId));
        if (assignments.isEmpty()) return PageResult.of(List.of(), 0L, page, size);

        List<Long> sidIds = assignments.stream().map(CustomerSid::getSidId).toList();
        List<SidInfo> sids = sidInfoMapper.selectBatchIds(sidIds);
        if (sids.isEmpty()) return PageResult.of(List.of(), 0L, page, size);

        List<String> sidValues = sids.stream().map(SidInfo::getSid).toList();

        Page<MoRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<MoRecord> wrapper = new LambdaQueryWrapper<MoRecord>()
                .in(MoRecord::getToSid, sidValues);
        if (startTime != null) wrapper.ge(MoRecord::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(MoRecord::getCreatedAt, endTime);
        wrapper.orderByDesc(MoRecord::getCreatedAt);
        moRecordMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    // ===== SIDs =====

    public List<SidInfo> sidList(Long customerId) {
        List<CustomerSid> assignments = customerSidMapper.selectList(
                new LambdaQueryWrapper<CustomerSid>().eq(CustomerSid::getCustomerId, customerId));
        if (assignments.isEmpty()) return List.of();
        List<Long> sidIds = assignments.stream().map(CustomerSid::getSidId).toList();
        return sidInfoMapper.selectBatchIds(sidIds);
    }

    // ===== Templates =====

    public PageResult<SmsTemplate> templateList(Long customerId, int page, int size,
                                                  String keyword, String status) {
        Page<SmsTemplate> p = new Page<>(page, size);
        LambdaQueryWrapper<SmsTemplate> wrapper = new LambdaQueryWrapper<SmsTemplate>()
                .eq(SmsTemplate::getCustomerId, customerId);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SmsTemplate::getTemplateName, keyword)
                    .or().like(SmsTemplate::getContent, keyword));
        }
        if (StringUtils.isNotBlank(status)) wrapper.eq(SmsTemplate::getStatus, status);
        wrapper.orderByDesc(SmsTemplate::getCreatedAt);
        smsTemplateMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Transactional
    public SmsTemplate createTemplate(Long customerId, String templateName, String templateType, String content) {
        SmsTemplate template = new SmsTemplate();
        template.setCustomerId(customerId);
        template.setTemplateName(templateName);
        // template_code: NOT NULL UNIQUE — auto-generate
        template.setTemplateCode("TPL" + customerId + System.currentTimeMillis());
        // template_type: NOT NULL — default OTP if missing
        template.setTemplateType(StringUtils.isNotBlank(templateType) ? templateType : "OTP");
        template.setContent(content);
        template.setStatus("pending");
        template.setPlatformStatus("pending");
        template.setCarrierStatus("pending");
        template.setIsActive(true);
        smsTemplateMapper.insert(template);
        return template;
    }

    @Transactional
    public SmsTemplate updateTemplate(Long customerId, Long templateId, String templateName, String templateType, String content) {
        SmsTemplate template = smsTemplateMapper.selectById(templateId);
        if (template == null || !template.getCustomerId().equals(customerId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        boolean carrierRejected = "rejected".equals(template.getCarrierStatus());
        boolean platformRejected = "rejected".equals(template.getPlatformStatus());
        if (!carrierRejected && !platformRejected) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "只有审核未通过的模板可以修改重提");
        }
        template.setTemplateName(templateName);
        template.setTemplateType(StringUtils.isNotBlank(templateType) ? templateType : template.getTemplateType());
        template.setContent(content);
        template.setStatus("pending");
        template.setPlatformStatus("pending");
        template.setCarrierStatus("pending");
        template.setRejectReason(null);
        smsTemplateMapper.updateById(template);
        return template;
    }

    @Transactional
    public void deleteTemplate(Long customerId, Long templateId) {
        SmsTemplate template = smsTemplateMapper.selectById(templateId);
        if (template == null || !template.getCustomerId().equals(customerId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        if ("approved".equals(template.getStatus())) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "已通过的模板不能删除");
        }
        smsTemplateMapper.deleteById(templateId);
    }

    // ===== Billing =====

    public Map<String, Object> billingAccount(Long customerId) {
        Customer customer = requireCustomer(customerId);
        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>()
                        .eq(CustomerAccount::getCustomerId, customerId));
        Map<String, Object> result = new HashMap<>();
        result.put("balance", account != null ? account.getBalance() : BigDecimal.ZERO);
        result.put("frozenAmount", account != null ? account.getFrozenAmount() : BigDecimal.ZERO);
        result.put("currency", account != null ? account.getCurrency() : "USD");
        result.put("paymentType", customer.getPaymentType() != null
                ? customer.getPaymentType().name() : "PREPAID");
        result.put("creditLimit", account != null ? account.getCreditLimit() : BigDecimal.ZERO);
        return result;
    }

    public PageResult<BillingTransaction> transactionList(Long customerId, int page, int size,
                                                           String startTime, String endTime) {
        Page<BillingTransaction> p = new Page<>(page, size);
        LambdaQueryWrapper<BillingTransaction> wrapper = new LambdaQueryWrapper<BillingTransaction>()
                .eq(BillingTransaction::getCustomerId, customerId);
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.ge(BillingTransaction::getCreatedAt, LocalDate.parse(startTime).atStartOfDay());
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.le(BillingTransaction::getCreatedAt, LocalDate.parse(endTime).atStartOfDay().plusDays(1));
        }
        wrapper.orderByDesc(BillingTransaction::getCreatedAt);
        billingTransactionMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<Bill> billList(Long customerId, int page, int size) {
        Page<Bill> p = new Page<>(page, size);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getCustomerId, customerId)
                .orderByDesc(Bill::getCreatedAt);
        billMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    // ===== Operation Log =====

    public PageResult<OperationLog> opLogs(Long customerId, int page, int size) {
        Page<OperationLog> p = new Page<>(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getOperatorId, customerId)
                .eq(OperationLog::getOperatorType, "portal")
                .orderByDesc(OperationLog::getCreatedAt);
        operationLogMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    // ===== Helpers =====

    private Customer requireCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND);
        return customer;
    }
}
