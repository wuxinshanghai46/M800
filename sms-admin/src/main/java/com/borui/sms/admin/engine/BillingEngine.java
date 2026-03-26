package com.borui.sms.admin.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.domain.entity.CustomerAccount;
import com.borui.sms.common.domain.entity.CustomerCountryPrice;
import com.borui.sms.common.domain.entity.VendorSettlementPrice;
import com.borui.sms.common.domain.enums.BillingMode;
import com.borui.sms.common.domain.enums.PaymentType;
import com.borui.sms.common.domain.enums.SmsAttribute;
import com.borui.sms.common.domain.entity.BillingTransaction;
import com.borui.sms.mapper.BillingTransactionMapper;
import com.borui.sms.mapper.CustomerAccountMapper;
import com.borui.sms.mapper.CustomerCountryPriceMapper;
import com.borui.sms.mapper.VendorSettlementPriceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Billing engine - MVP version (DB transaction).
 * Production version will use Redis DECRBY atomic freeze.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEngine {

    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerCountryPriceMapper customerCountryPriceMapper;
    private final VendorSettlementPriceMapper vendorSettlementPriceMapper;
    private final BillingTransactionMapper billingTransactionMapper;

    /**
     * Pricing result containing unit price and billing mode.
     */
    @lombok.Value
    public static class PriceResult {
        BigDecimal unitPrice;
        BillingMode billingMode;
    }

    /**
     * Look up customer price for the given country.
     * Priority: customer+country+attribute > customer+country+null > null (not found)
     * Returns PriceResult with unitPrice and billingMode.
     */
    public PriceResult resolvePrice(Long customerId, String countryCode, Integer smsAttribute) {
        // 1. Try exact match: customer + country + attribute
        if (smsAttribute != null) {
            CustomerCountryPrice price = customerCountryPriceMapper.selectOne(
                    new LambdaQueryWrapper<CustomerCountryPrice>()
                            .eq(CustomerCountryPrice::getCustomerId, customerId)
                            .eq(CustomerCountryPrice::getCountryCode, countryCode)
                            .eq(CustomerCountryPrice::getSmsAttribute, SmsAttribute.values()[smsAttribute - 1]));
            if (price != null) {
                return new PriceResult(price.getPrice(), resolveBillingMode(price.getBillingMode()));
            }
        }

        // 2. Fallback: customer + country + null attribute (default price)
        CustomerCountryPrice defaultPrice = customerCountryPriceMapper.selectOne(
                new LambdaQueryWrapper<CustomerCountryPrice>()
                        .eq(CustomerCountryPrice::getCustomerId, customerId)
                        .eq(CustomerCountryPrice::getCountryCode, countryCode)
                        .isNull(CustomerCountryPrice::getSmsAttribute));
        if (defaultPrice != null) {
            return new PriceResult(defaultPrice.getPrice(), resolveBillingMode(defaultPrice.getBillingMode()));
        }

        return null;
    }

    private BillingMode resolveBillingMode(BillingMode mode) {
        return mode != null ? mode : BillingMode.DELIVERED; // default: charge on delivery
    }

    /**
     * Look up vendor cost for the given channel/country.
     * Priority: vendor+country+channel+attribute > vendor+country+attribute > vendor+country > channel.costPrice
     */
    public BigDecimal resolveCost(Long vendorId, String countryCode, Long channelId, Integer smsAttribute, BigDecimal channelCostPrice) {
        // 1. Try exact: vendor + country + channel + attribute
        if (channelId != null && smsAttribute != null) {
            VendorSettlementPrice vsp = vendorSettlementPriceMapper.selectOne(
                    new LambdaQueryWrapper<VendorSettlementPrice>()
                            .eq(VendorSettlementPrice::getVendorId, vendorId)
                            .eq(VendorSettlementPrice::getCountryCode, countryCode)
                            .eq(VendorSettlementPrice::getChannelId, channelId)
                            .eq(VendorSettlementPrice::getSmsAttribute, smsAttribute)
                            .orderByDesc(VendorSettlementPrice::getEffectiveDate)
                            .last("LIMIT 1"));
            if (vsp != null) return vsp.getPrice();
        }

        // 2. vendor + country + attribute (any channel)
        if (smsAttribute != null) {
            VendorSettlementPrice vsp = vendorSettlementPriceMapper.selectOne(
                    new LambdaQueryWrapper<VendorSettlementPrice>()
                            .eq(VendorSettlementPrice::getVendorId, vendorId)
                            .eq(VendorSettlementPrice::getCountryCode, countryCode)
                            .isNull(VendorSettlementPrice::getChannelId)
                            .eq(VendorSettlementPrice::getSmsAttribute, smsAttribute)
                            .orderByDesc(VendorSettlementPrice::getEffectiveDate)
                            .last("LIMIT 1"));
            if (vsp != null) return vsp.getPrice();
        }

        // 3. vendor + country (any attribute, any channel)
        VendorSettlementPrice vsp = vendorSettlementPriceMapper.selectOne(
                new LambdaQueryWrapper<VendorSettlementPrice>()
                        .eq(VendorSettlementPrice::getVendorId, vendorId)
                        .eq(VendorSettlementPrice::getCountryCode, countryCode)
                        .isNull(VendorSettlementPrice::getChannelId)
                        .isNull(VendorSettlementPrice::getSmsAttribute)
                        .orderByDesc(VendorSettlementPrice::getEffectiveDate)
                        .last("LIMIT 1"));
        if (vsp != null) return vsp.getPrice();

        // 4. Fallback: channel.cost_price
        return channelCostPrice != null ? channelCostPrice : BigDecimal.ZERO;
    }

    /**
     * Freeze balance: deduct from available, add to frozen.
     * MVP: DB transaction. Production: Redis Lua atomic.
     */
    @Transactional
    public void freeze(Long customerId, BigDecimal amount, PaymentType paymentType) {
        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getCustomerId, customerId));
        if (account == null) throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND, "账户不存在");

        if (paymentType == PaymentType.PREPAID) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new BizException(ErrorCode.SEND_BALANCE_INSUFFICIENT);
            }
            account.setBalance(account.getBalance().subtract(amount));
            account.setFrozenAmount(account.getFrozenAmount().add(amount));
        } else {
            // Postpaid: check credit limit
            BigDecimal totalUsed = account.getFrozenAmount().add(amount);
            if (account.getCreditLimit().compareTo(BigDecimal.ZERO) > 0
                    && totalUsed.compareTo(account.getCreditLimit()) > 0) {
                throw new BizException(ErrorCode.SEND_BALANCE_INSUFFICIENT, "超出信用额度");
            }
            account.setFrozenAmount(account.getFrozenAmount().add(amount));
        }

        customerAccountMapper.updateById(account);
        log.info("Freeze: customerId={}, amount={}, newBalance={}, frozen={}",
                customerId, amount, account.getBalance(), account.getFrozenAmount());
    }

    /**
     * Confirm: frozen -> actual charge (message delivered)
     */
    @Transactional
    public void confirm(Long customerId, BigDecimal amount) {
        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getCustomerId, customerId));
        if (account == null) return;

        account.setFrozenAmount(account.getFrozenAmount().subtract(amount));
        customerAccountMapper.updateById(account);
        logTx(customerId, "DEDUCT", amount, account.getBalance(), "短信扣费");
        log.info("Confirm: customerId={}, amount={}, frozen={}", customerId, amount, account.getFrozenAmount());
    }

    /**
     * Release: frozen -> return to available (message failed)
     */
    @Transactional
    public void release(Long customerId, BigDecimal amount, PaymentType paymentType) {
        CustomerAccount account = customerAccountMapper.selectOne(
                new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getCustomerId, customerId));
        if (account == null) return;

        account.setFrozenAmount(account.getFrozenAmount().subtract(amount));
        if (paymentType == PaymentType.PREPAID) {
            account.setBalance(account.getBalance().add(amount));
        }

        customerAccountMapper.updateById(account);
        if (paymentType == PaymentType.PREPAID) {
            logTx(customerId, "REFUND", amount, account.getBalance(), "短信发送失败退款");
        }
        log.info("Release: customerId={}, amount={}, newBalance={}, frozen={}",
                customerId, amount, account.getBalance(), account.getFrozenAmount());
    }

    private void logTx(Long customerId, String type, BigDecimal amount,
                        BigDecimal balanceAfter, String remark) {
        try {
            BillingTransaction tx = new BillingTransaction();
            tx.setCustomerId(customerId);
            tx.setType(type);
            tx.setAmount(amount);
            tx.setBalanceAfter(balanceAfter != null ? balanceAfter : BigDecimal.ZERO);
            tx.setRemark(remark);
            billingTransactionMapper.insert(tx);
        } catch (Exception e) {
            log.warn("Failed to log billing transaction: {}", e.getMessage());
        }
    }
}
