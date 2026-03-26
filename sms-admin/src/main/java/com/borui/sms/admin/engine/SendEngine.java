package com.borui.sms.admin.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.admin.adapter.ChannelAdapter;
import com.borui.sms.admin.adapter.ChannelAdapterFactory;
import com.borui.sms.admin.adapter.ChannelSendRequest;
import com.borui.sms.admin.adapter.ChannelSendResult;
import com.borui.sms.admin.mock.MockChannelAdapter;
import com.borui.sms.admin.vo.req.SendSmsReq;
import com.borui.sms.admin.vo.resp.SendSmsResp;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.util.IdGenerator;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.common.domain.enums.BillingMode;
import com.borui.sms.common.domain.enums.MessageStatus;
import com.borui.sms.common.domain.enums.PaymentType;
import com.borui.sms.common.domain.enums.SmsAttribute;
import com.borui.sms.mapper.CustomerCountryMapper;
import com.borui.sms.mapper.CustomerMapper;
import com.borui.sms.mapper.MessageMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Send engine - the core sending pipeline (MVP sync version).
 * 12-step process: parse → auth → risk → encode → route → sid → price → freeze → write → send → dlr → return
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendEngine {

    private final CustomerMapper customerMapper;
    private final CustomerCountryMapper customerCountryMapper;
    private final MessageMapper messageMapper;
    private final EncodingEngine encodingEngine;
    private final RoutingEngine routingEngine;
    private final BillingEngine billingEngine;
    private final RiskEngine riskEngine;
    private final ChannelAdapterFactory adapterFactory;
    private final MockChannelAdapter mockChannelAdapter; // MVP only: for simulateDlr()

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    @Transactional
    public SendSmsResp send(Long customerId, SendSmsReq req) {
        long start = System.currentTimeMillis();
        String messageId = IdGenerator.messageId();

        // Step 1: Parse phone number
        String countryCode;
        String nationalNumber;
        try {
            Phonenumber.PhoneNumber phone = phoneUtil.parse(req.getTo(), null);
            countryCode = phoneUtil.getRegionCodeForNumber(phone);
            nationalNumber = String.valueOf(phone.getNationalNumber());
            if (countryCode == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "无法识别号码归属国家");
            }
        } catch (NumberParseException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "号码格式错误: " + e.getMessage());
        }

        log.info("[send] step1 parse: msgId={}, to={}, country={}", messageId, req.getTo(), countryCode);

        // Step 2: Get customer and validate
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND);

        // Step 2.5: Check SMS attribute permission
        Integer smsAttr = req.getSmsAttribute();
        if (smsAttr != null && customer.getAllowedSmsAttributes() != null
                && !customer.getAllowedSmsAttributes().isEmpty()) {
            String allowed = customer.getAllowedSmsAttributes();
            if (!allowed.contains(String.valueOf(smsAttr))) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "该客户不允许发送此类型短信(smsAttribute=" + smsAttr + ")");
            }
        }

        // Step 3: Check country permission
        Long countryEnabled = customerCountryMapper.selectCount(
                new LambdaQueryWrapper<CustomerCountry>()
                        .eq(CustomerCountry::getCustomerId, customerId)
                        .eq(CustomerCountry::getCountryCode, countryCode)
                        .eq(CustomerCountry::getIsActive, true));
        if (countryEnabled == 0) {
            throw new BizException(ErrorCode.SEND_COUNTRY_NOT_ALLOWED);
        }

        log.info("[send] step3 country OK: msgId={}", messageId);

        // Step 3.5: Risk check (blacklist + sensitive word + rate limit)
        RiskEngine.RiskResult riskResult = riskEngine.check(customerId, req.getTo(), req.getContent());
        if (!riskResult.isPassed()) {
            log.warn("[send] risk BLOCKED: msgId={}, reason={}", messageId, riskResult.getBlockReason());
            throw new BizException(ErrorCode.SEND_CONTENT_BLOCKED, riskResult.getBlockReason());
        }
        log.info("[send] step3.5 risk OK: msgId={}", messageId);

        // Step 4: Encoding detection + segment calculation (pure CPU, same as production)
        int encoding = encodingEngine.detectEncoding(req.getContent());
        int segments = encodingEngine.calculateSegments(req.getContent(), encoding);

        log.info("[send] step4 encoding: msgId={}, encoding={}, segments={}", messageId, encoding == 0 ? "GSM7" : "UCS2", segments);

        // Step 5: Route to channel
        Channel channel = routingEngine.selectChannel(countryCode, smsAttr);
        if (channel == null) {
            throw new BizException(ErrorCode.SEND_NO_CHANNEL);
        }

        log.info("[send] step5 route: msgId={}, channel={}", messageId, channel.getChannelCode());

        // Step 6: Select SID
        SidInfo sid = routingEngine.selectSid(countryCode, channel.getVendorId(), req.getSid());
        String sidValue = sid != null ? sid.getSid() : (req.getSid() != null ? req.getSid() : "DEFAULT");

        // Step 7: Resolve price + billing mode
        BillingEngine.PriceResult priceResult = billingEngine.resolvePrice(customerId, countryCode, smsAttr);
        if (priceResult == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未设置该国家报价");
        }
        BigDecimal unitPrice = priceResult.getUnitPrice();
        BillingMode billingMode = priceResult.getBillingMode();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(segments));

        log.info("[send] step7 price: msgId={}, unitPrice={}, segments={}, total={}, billingMode={}",
                messageId, unitPrice, segments, totalPrice, billingMode);

        // Step 8: Freeze balance
        billingEngine.freeze(customerId, totalPrice, customer.getPaymentType());

        log.info("[send] step8 freeze OK: msgId={}, amount={}", messageId, totalPrice);

        // Step 9: Write message record (status=ACCEPTED)
        Message message = new Message();
        message.setMessageId(messageId);
        message.setCustomerId(customerId);
        message.setCountryCode(countryCode);
        message.setVendorId(channel.getVendorId());
        message.setChannelId(channel.getId());
        message.setSid(sidValue);
        message.setToNumber(req.getTo());
        message.setSmsAttribute(smsAttr != null ? SmsAttribute.fromCode(smsAttr) : null);
        message.setBillingMode(billingMode);
        message.setContentHash(sha256(req.getContent()));
        message.setEncoding(encoding);
        message.setSegments(segments);
        message.setStatus(MessageStatus.ACCEPTED);
        message.setPrice(unitPrice);
        BigDecimal unitCost = billingEngine.resolveCost(
                channel.getVendorId(), countryCode, channel.getId(), smsAttr, channel.getCostPrice());
        message.setCost(unitCost);
        message.setCurrency("USD");
        message.setClientRef(req.getClientRef());
        message.setCallbackStatus(0);
        messageMapper.insert(message);

        log.info("[send] step9 write: msgId={}", messageId);

        // Step 10: Channel send via adapter
        ChannelAdapter adapter = adapterFactory.getAdapter(channel);
        ChannelSendResult sendResult = adapter.send(channel, ChannelSendRequest.builder()
                .channelCode(channel.getChannelCode())
                .toNumber(req.getTo())
                .content(req.getContent())
                .sid(sidValue)
                .encoding(encoding)
                .segments(segments)
                .messageId(messageId)
                .build());

        if (!sendResult.isSubmitted()) {
            // Submit failed - update status
            message.setStatus(MessageStatus.FAILED);
            message.setErrorCode(sendResult.getErrorCode());
            messageMapper.updateById(message);
            // SUBMIT mode: charge on submit, no refund even if failed
            // DELIVERED/REACHED mode: release frozen amount
            if (billingMode != BillingMode.SUBMIT) {
                billingEngine.release(customerId, totalPrice, customer.getPaymentType());
            } else {
                billingEngine.confirm(customerId, totalPrice);
            }
            log.warn("[send] step10 submit FAIL: msgId={}, error={}, billingMode={}", messageId, sendResult.getErrorMessage(), billingMode);

            return SendSmsResp.builder()
                    .messageId(messageId)
                    .to(req.getTo())
                    .countryCode(countryCode)
                    .segments(segments)
                    .encoding(encoding == 0 ? "GSM-7" : "UCS-2")
                    .price(totalPrice)
                    .currency("USD")
                    .status("FAILED")
                    .build();
        }

        // Submit success
        message.setStatus(MessageStatus.SUBMITTED);
        message.setVendorMsgId(sendResult.getVendorMsgId());
        message.setSubmitAt(LocalDateTime.now());
        messageMapper.updateById(message);

        // SUBMIT mode: confirm charge immediately on successful submit
        if (billingMode == BillingMode.SUBMIT) {
            billingEngine.confirm(customerId, totalPrice);
            log.info("[send] step10 SUBMIT billing confirmed: msgId={}, amount={}", messageId, totalPrice);
        }

        log.info("[send] step10 submit OK: msgId={}, vendorMsgId={}, adapter={}", messageId, sendResult.getVendorMsgId(), adapter.getName());

        // Step 11: DLR handling
        // Mock adapter: sync DLR simulation (for testing)
        // SMPP/HTTP adapter: async DLR via deliver_sm callback / HTTP callback, return SUBMITTED immediately
        MessageStatus finalStatus;

        if (!adapter.isAsyncDlr()) {
            // Sync DLR for mock testing
            MessageStatus dlrStatus = mockChannelAdapter.simulateDlr();
            message.setStatus(dlrStatus);
            if (dlrStatus == MessageStatus.DELIVERED) {
                message.setDeliverAt(LocalDateTime.now());
                // SUBMIT mode already confirmed at step 10; DELIVERED/REACHED confirm here
                if (billingMode != BillingMode.SUBMIT) {
                    billingEngine.confirm(customerId, totalPrice);
                }
            } else {
                message.setErrorCode(dlrStatus.name());
                // SUBMIT mode already confirmed; DELIVERED/REACHED release here
                if (billingMode != BillingMode.SUBMIT) {
                    billingEngine.release(customerId, totalPrice, customer.getPaymentType());
                }
            }
            messageMapper.updateById(message);
            finalStatus = dlrStatus;
        } else {
            // Async DLR — status stays SUBMITTED, DLR will come via DlrProcessor
            finalStatus = MessageStatus.SUBMITTED;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[send] DONE: msgId={}, status={}, elapsed={}ms", messageId, finalStatus, elapsed);

        // Step 12: Return result
        return SendSmsResp.builder()
                .messageId(messageId)
                .to(req.getTo())
                .countryCode(countryCode)
                .segments(segments)
                .encoding(encoding == 0 ? "GSM-7" : "UCS-2")
                .price(totalPrice)
                .currency("USD")
                .status(finalStatus.name())
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
