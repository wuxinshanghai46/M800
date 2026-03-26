package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.vo.req.AccountOperateReq;
import com.borui.sms.admin.vo.req.CustomerCountryPriceReq;
import com.borui.sms.admin.vo.req.CustomerReq;
import com.borui.sms.admin.vo.resp.CustomerDetailResp;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.util.IdGenerator;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.common.domain.enums.CustomerStatus;
import com.borui.sms.common.domain.enums.PaymentType;
import com.borui.sms.common.domain.enums.SmsAttribute;
import com.borui.sms.common.domain.entity.BillingTransaction;
import com.borui.sms.mapper.*;
import com.borui.sms.mapper.BillingTransactionMapper;
import com.borui.sms.mapper.CountryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerCountryMapper customerCountryMapper;
    private final CustomerCountryPriceMapper customerCountryPriceMapper;
    private final CustomerApiCredentialMapper customerApiCredentialMapper;
    private final CustomerSidMapper customerSidMapper;
    private final SidInfoMapper sidInfoMapper;
    private final CountryMapper countryMapper;
    private final BillingTransactionMapper billingTransactionMapper;
    private final PasswordEncoder passwordEncoder;

    // ========== Tab 1: Basic Info ==========

    public PageResult<Customer> list(int page, int size, String keyword, Integer status) {
        Page<Customer> p = new Page<>(page, size);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Customer::getCustomerCode, keyword)
                    .or().like(Customer::getCustomerName, keyword)
                    .or().like(Customer::getCompanyName, keyword));
        }
        if (status != null) {
            wrapper.eq(Customer::getStatus, CustomerStatus.values()[status]);
        }
        wrapper.orderByDesc(Customer::getCreatedAt);
        customerMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND);
        return customer;
    }

    public CustomerDetailResp getDetail(Long id) {
        CustomerDetailResp resp = new CustomerDetailResp();
        resp.setCustomer(getById(id));
        resp.setAccount(getAccount(id));
        resp.setCountries(listCountries(id));
        resp.setPrices(listPrices(id));
        resp.setCredentials(listCredentials(id));
        resp.setSids(listCustomerSids(id));
        return resp;
    }

    @Transactional
    public Customer create(CustomerReq req) {
        Long exists = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerCode, req.getCustomerCode()));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "客户代码已存在");

        Customer customer = new Customer();
        customer.setCustomerCode(req.getCustomerCode());
        customer.setCustomerName(req.getCustomerName());
        customer.setCompanyName(req.getCompanyName());
        customer.setStatus(CustomerStatus.values()[req.getStatus()]);
        customer.setPaymentType(PaymentType.values()[req.getPaymentType() - 1]);
        customer.setSalesManager(req.getSalesManager());
        customer.setContactName(req.getContactName());
        customer.setContactEmail(req.getContactEmail());
        customer.setContactPhone(req.getContactPhone());
        customer.setTimezone(req.getTimezone());
        customer.setBillCurrency(req.getBillCurrency());
        customer.setBillAutoSend(req.getBillAutoSend());
        customer.setBillEmail(req.getBillEmail());
        customer.setSidSelectable(req.getSidSelectable());
        customer.setPreferredSidFormat(req.getPreferredSidFormat());
        customer.setAllowedSmsAttributes(req.getAllowedSmsAttributes());
        customer.setAddress(req.getAddress());
        // Portal account: auto-generate if not provided
        if (req.getPortalAccount() != null && !req.getPortalAccount().isBlank()) {
            customer.setPortalAccount(req.getPortalAccount());
        } else {
            customer.setPortalAccount(req.getCustomerCode() + "@portal");
        }
        String rawPassword = req.getPortalPassword() != null && !req.getPortalPassword().isBlank()
                ? req.getPortalPassword() : "Sms@" + req.getCustomerCode() + "2024";
        customer.setPortalPassword(passwordEncoder.encode(rawPassword));
        customer.setRemark(req.getRemark());
        customerMapper.insert(customer);

        // Auto-create account
        CustomerAccount account = new CustomerAccount();
        account.setCustomerId(customer.getId());
        account.setBalance(BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setCreditLimit(BigDecimal.ZERO);
        account.setCurrency(req.getBillCurrency());
        customerAccountMapper.insert(account);

        return customer;
    }

    public Customer update(Long id, CustomerReq req) {
        Customer customer = getById(id);
        customer.setCustomerName(req.getCustomerName());
        customer.setCompanyName(req.getCompanyName());
        customer.setStatus(CustomerStatus.values()[req.getStatus()]);
        customer.setPaymentType(PaymentType.values()[req.getPaymentType() - 1]);
        customer.setSalesManager(req.getSalesManager());
        customer.setContactName(req.getContactName());
        customer.setContactEmail(req.getContactEmail());
        customer.setContactPhone(req.getContactPhone());
        customer.setTimezone(req.getTimezone());
        customer.setBillCurrency(req.getBillCurrency());
        customer.setBillAutoSend(req.getBillAutoSend());
        customer.setBillEmail(req.getBillEmail());
        customer.setSidSelectable(req.getSidSelectable());
        customer.setPreferredSidFormat(req.getPreferredSidFormat());
        customer.setAllowedSmsAttributes(req.getAllowedSmsAttributes());
        customer.setAddress(req.getAddress());
        if (req.getPortalPassword() != null && !req.getPortalPassword().isBlank()) {
            customer.setPortalPassword(passwordEncoder.encode(req.getPortalPassword()));
        }
        customer.setRemark(req.getRemark());
        customerMapper.updateById(customer);
        return customer;
    }

    public Customer updateStatus(Long id, Integer status) {
        Customer customer = getById(id);
        customer.setStatus(CustomerStatus.values()[status]);
        customerMapper.updateById(customer);
        return customer;
    }

    // ========== Tab 2: Account ==========

    public CustomerAccount getAccount(Long customerId) {
        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getCustomerId, customerId));
        if (account == null) throw new BizException(ErrorCode.NOT_FOUND, "客户账户不存在");
        return account;
    }

    @Transactional
    public CustomerAccount recharge(Long customerId, AccountOperateReq req) {
        CustomerAccount account = getAccount(customerId);
        account.setBalance(account.getBalance().add(req.getAmount()));
        customerAccountMapper.updateById(account);
        BillingTransaction tx = new BillingTransaction();
        tx.setCustomerId(customerId);
        tx.setType("RECHARGE");
        tx.setAmount(req.getAmount());
        tx.setBalanceAfter(account.getBalance());
        tx.setRemark(req.getRemark() != null ? req.getRemark() : "管理员充值");
        billingTransactionMapper.insert(tx);
        return account;
    }

    @Transactional
    public CustomerAccount deduct(Long customerId, AccountOperateReq req) {
        CustomerAccount account = getAccount(customerId);
        if (account.getBalance().compareTo(req.getAmount()) < 0) {
            throw new BizException(ErrorCode.SEND_BALANCE_INSUFFICIENT);
        }
        account.setBalance(account.getBalance().subtract(req.getAmount()));
        customerAccountMapper.updateById(account);
        return account;
    }

    public CustomerAccount updateCreditLimit(Long customerId, BigDecimal creditLimit) {
        CustomerAccount account = getAccount(customerId);
        account.setCreditLimit(creditLimit);
        customerAccountMapper.updateById(account);
        return account;
    }

    // ========== Tab 3: Country & Price ==========

    public List<CustomerCountry> listCountries(Long customerId) {
        return customerCountryMapper.selectList(
                new LambdaQueryWrapper<CustomerCountry>().eq(CustomerCountry::getCustomerId, customerId));
    }

    public CustomerCountry enableCountry(Long customerId, String countryCode) {
        // 校验国家代码存在
        Long countryExists = countryMapper.selectCount(
                new LambdaQueryWrapper<Country>().eq(Country::getCountryCode, countryCode));
        if (countryExists == 0) throw new BizException(ErrorCode.NOT_FOUND, "国家代码不存在: " + countryCode);

        Long exists = customerCountryMapper.selectCount(new LambdaQueryWrapper<CustomerCountry>()
                .eq(CustomerCountry::getCustomerId, customerId)
                .eq(CustomerCountry::getCountryCode, countryCode));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "该国家已开通");

        CustomerCountry cc = new CustomerCountry();
        cc.setCustomerId(customerId);
        cc.setCountryCode(countryCode);
        cc.setIsActive(true);
        customerCountryMapper.insert(cc);
        return cc;
    }

    @Transactional
    public void disableCountry(Long customerId, String countryCode) {
        // 同时清理该国家的报价数据
        customerCountryPriceMapper.delete(new LambdaQueryWrapper<CustomerCountryPrice>()
                .eq(CustomerCountryPrice::getCustomerId, customerId)
                .eq(CustomerCountryPrice::getCountryCode, countryCode));
        customerCountryMapper.delete(new LambdaQueryWrapper<CustomerCountry>()
                .eq(CustomerCountry::getCustomerId, customerId)
                .eq(CustomerCountry::getCountryCode, countryCode));
    }

    public List<CustomerCountryPrice> listPrices(Long customerId) {
        return customerCountryPriceMapper.selectList(
                new LambdaQueryWrapper<CustomerCountryPrice>()
                        .eq(CustomerCountryPrice::getCustomerId, customerId)
                        .orderByAsc(CustomerCountryPrice::getCountryCode));
    }

    public CustomerCountryPrice setPrice(Long customerId, CustomerCountryPriceReq req) {
        // 校验客户已开通该国家
        Long countryEnabled = customerCountryMapper.selectCount(new LambdaQueryWrapper<CustomerCountry>()
                .eq(CustomerCountry::getCustomerId, customerId)
                .eq(CustomerCountry::getCountryCode, req.getCountryCode()));
        if (countryEnabled == 0) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "请先开通国家 " + req.getCountryCode() + " 再设置报价");
        }

        LambdaQueryWrapper<CustomerCountryPrice> wrapper = new LambdaQueryWrapper<CustomerCountryPrice>()
                .eq(CustomerCountryPrice::getCustomerId, customerId)
                .eq(CustomerCountryPrice::getCountryCode, req.getCountryCode());
        if (req.getSmsAttribute() != null) {
            wrapper.eq(CustomerCountryPrice::getSmsAttribute, SmsAttribute.values()[req.getSmsAttribute() - 1]);
        } else {
            wrapper.isNull(CustomerCountryPrice::getSmsAttribute);
        }

        CustomerCountryPrice existing = customerCountryPriceMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setPrice(req.getPrice());
            existing.setCurrency(req.getCurrency());
            customerCountryPriceMapper.updateById(existing);
            return existing;
        }

        CustomerCountryPrice price = new CustomerCountryPrice();
        price.setCustomerId(customerId);
        price.setCountryCode(req.getCountryCode());
        if (req.getSmsAttribute() != null) {
            price.setSmsAttribute(SmsAttribute.values()[req.getSmsAttribute() - 1]);
        }
        price.setPrice(req.getPrice());
        price.setCurrency(req.getCurrency());
        customerCountryPriceMapper.insert(price);
        return price;
    }

    public void deletePrice(Long priceId) {
        customerCountryPriceMapper.deleteById(priceId);
    }

    // ========== Tab 4: API Credential ==========

    public List<CustomerApiCredential> listCredentials(Long customerId) {
        return customerApiCredentialMapper.selectList(
                new LambdaQueryWrapper<CustomerApiCredential>()
                        .eq(CustomerApiCredential::getCustomerId, customerId));
    }

    public CustomerApiCredential createCredential(Long customerId) {
        CustomerApiCredential cred = new CustomerApiCredential();
        cred.setCustomerId(customerId);
        cred.setApiKey(IdGenerator.apiKey());
        cred.setApiSecret(IdGenerator.apiSecret());
        cred.setIsActive(true);
        customerApiCredentialMapper.insert(cred);
        return cred;
    }

    public CustomerApiCredential resetCredentialSecret(Long credentialId) {
        CustomerApiCredential cred = customerApiCredentialMapper.selectById(credentialId);
        if (cred == null) throw new BizException(ErrorCode.NOT_FOUND, "凭证不存在");
        cred.setApiSecret(IdGenerator.apiSecret());
        customerApiCredentialMapper.updateById(cred);
        return cred;
    }

    public void toggleCredential(Long credentialId, boolean active) {
        CustomerApiCredential cred = customerApiCredentialMapper.selectById(credentialId);
        if (cred == null) throw new BizException(ErrorCode.NOT_FOUND, "凭证不存在");
        cred.setIsActive(active);
        customerApiCredentialMapper.updateById(cred);
    }

    public void updateIpWhitelist(Long credentialId, String ipWhitelist) {
        CustomerApiCredential cred = customerApiCredentialMapper.selectById(credentialId);
        if (cred == null) throw new BizException(ErrorCode.NOT_FOUND, "凭证不存在");
        cred.setIpWhitelist(ipWhitelist);
        customerApiCredentialMapper.updateById(cred);
    }

    public void deleteCredential(Long credentialId) {
        customerApiCredentialMapper.deleteById(credentialId);
    }

    // ========== Customer SID ==========

    public List<SidInfo> listCustomerSids(Long customerId) {
        List<CustomerSid> assignments = customerSidMapper.selectList(
                new LambdaQueryWrapper<CustomerSid>().eq(CustomerSid::getCustomerId, customerId));
        if (assignments.isEmpty()) return List.of();
        List<Long> sidIds = assignments.stream().map(CustomerSid::getSidId).toList();
        return sidInfoMapper.selectBatchIds(sidIds);
    }

    public CustomerSid assignSid(Long customerId, Long sidId) {
        Long exists = customerSidMapper.selectCount(
                new LambdaQueryWrapper<CustomerSid>()
                        .eq(CustomerSid::getCustomerId, customerId)
                        .eq(CustomerSid::getSidId, sidId));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "该SID已授权给此客户");
        CustomerSid cs = new CustomerSid();
        cs.setCustomerId(customerId);
        cs.setSidId(sidId);
        customerSidMapper.insert(cs);
        return cs;
    }

    public void unassignSid(Long customerId, Long sidId) {
        customerSidMapper.delete(new LambdaQueryWrapper<CustomerSid>()
                .eq(CustomerSid::getCustomerId, customerId)
                .eq(CustomerSid::getSidId, sidId));
    }
}
