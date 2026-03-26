package com.borui.sms.admin.dlr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.admin.service.CustomerCallbackService;
import com.borui.sms.common.domain.entity.CustomerSid;
import com.borui.sms.common.domain.entity.MoRecord;
import com.borui.sms.common.domain.entity.OptoutRecord;
import com.borui.sms.common.domain.entity.SidInfo;
import com.borui.sms.common.common.util.IdGenerator;
import com.borui.sms.mapper.CustomerSidMapper;
import com.borui.sms.mapper.MoRecordMapper;
import com.borui.sms.mapper.OptoutRecordMapper;
import com.borui.sms.mapper.SidInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles MO (Mobile-Originated) messages received via SMPP deliver_sm.
 * Responsibilities:
 *   1. Save to mo_record table
 *   2. Detect optout keywords (STOP, TD, QUIT, CANCEL, UNSUBSCRIBE, etc.) and record in optout_list
 *   3. Look up which customer owns the destination SID and trigger their MO webhook
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoProcessor {

    private static final Set<String> OPTOUT_KEYWORDS = Set.of(
            "stop", "td", "quit", "cancel", "unsubscribe", "opt out", "optout",
            "退订", "取消", "拒收", "n"
    );

    private final MoRecordMapper moRecordMapper;
    private final SidInfoMapper sidInfoMapper;
    private final CustomerSidMapper customerSidMapper;
    private final OptoutRecordMapper optoutRecordMapper;
    private final CustomerCallbackService customerCallbackService;

    /**
     * Process an inbound MO message.
     *
     * @param fromNumber  sender's phone number
     * @param toSid       destination SID (our virtual number/sender ID)
     * @param content     message text
     * @param channelId   the channel that received this MO
     * @param countryCode detected country code
     */
    public void process(String fromNumber, String toSid, String content,
                         Long channelId, String countryCode) {
        log.info("MO received: from={}, to={}, channel={}, content={}", fromNumber, toSid, channelId, content);

        // 1. Save to mo_record
        MoRecord mo = new MoRecord();
        mo.setMoMessageId(IdGenerator.messageId());
        mo.setFromNumber(fromNumber);
        mo.setToSid(toSid);
        mo.setContent(content);
        mo.setCountryCode(countryCode);
        mo.setChannelId(channelId);

        // 2. Check for optout keyword
        boolean isOptout = isOptoutMessage(content);
        if (isOptout) {
            mo.setMatchKeyword(extractKeyword(content));
            mo.setAction("OPTOUT");
            recordOptout(fromNumber, countryCode, toSid, content);
            log.info("MO optout: from={}, keyword={}", fromNumber, mo.getMatchKeyword());
        } else {
            mo.setAction("NORMAL");
        }

        // 3. Find customer that owns this SID
        Long customerId = findCustomerBySid(toSid);

        // 4. Trigger customer MO webhook
        if (customerId != null) {
            mo.setPushStatus("pending");
            moRecordMapper.insert(mo);

            Map<String, Object> payload = buildMoPayload(mo, customerId);
            boolean sent = customerCallbackService.sendMoCallback(customerId, payload);
            mo.setPushStatus(sent ? "success" : "failed");
            moRecordMapper.updateById(mo);
        } else {
            mo.setPushStatus("no_customer");
            moRecordMapper.insert(mo);
            log.warn("MO: no customer found for SID={}", toSid);
        }
    }

    private boolean isOptoutMessage(String content) {
        if (content == null) return false;
        String trimmed = content.trim().toLowerCase();
        return OPTOUT_KEYWORDS.stream().anyMatch(keyword ->
                trimmed.equals(keyword) || trimmed.startsWith(keyword + " "));
    }

    private String extractKeyword(String content) {
        if (content == null) return null;
        String word = content.trim().split("\\s+")[0].toLowerCase();
        return OPTOUT_KEYWORDS.contains(word) ? word : content.trim();
    }

    private void recordOptout(String phoneNumber, String countryCode, String sid, String keyword) {
        // Find customer associated with this SID for customer-scoped optout
        Long customerId = findCustomerBySid(sid);

        // Check if optout already exists
        Long existing = optoutRecordMapper.selectCount(
                new LambdaQueryWrapper<OptoutRecord>()
                        .eq(OptoutRecord::getPhoneNumber, phoneNumber)
                        .eq(OptoutRecord::getIsActive, true)
                        .and(w -> w.isNull(OptoutRecord::getCustomerId)
                                .or().eq(OptoutRecord::getCustomerId, customerId)));
        if (existing > 0) {
            log.debug("Optout already recorded for number={}", phoneNumber);
            return;
        }

        OptoutRecord record = new OptoutRecord();
        record.setPhoneNumber(phoneNumber);
        record.setCountryCode(countryCode);
        record.setCustomerId(customerId);
        record.setSource("MO_REPLY");
        record.setKeyword(extractKeyword(keyword));
        record.setIsActive(true);
        optoutRecordMapper.insert(record);
        log.info("Optout recorded: number={}, customerId={}, keyword={}", phoneNumber, customerId, keyword);
    }

    private Long findCustomerBySid(String sid) {
        SidInfo sidInfo = sidInfoMapper.selectOne(
                new LambdaQueryWrapper<SidInfo>().eq(SidInfo::getSid, sid).last("LIMIT 1"));
        if (sidInfo == null) return null;

        CustomerSid assignment = customerSidMapper.selectOne(
                new LambdaQueryWrapper<CustomerSid>().eq(CustomerSid::getSidId, sidInfo.getId()).last("LIMIT 1"));
        return assignment != null ? assignment.getCustomerId() : null;
    }

    private Map<String, Object> buildMoPayload(MoRecord mo, Long customerId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("moMessageId", mo.getMoMessageId());
        payload.put("fromNumber", mo.getFromNumber());
        payload.put("toSid", mo.getToSid());
        payload.put("content", mo.getContent());
        payload.put("countryCode", mo.getCountryCode());
        payload.put("channelId", mo.getChannelId());
        payload.put("action", mo.getAction());
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
