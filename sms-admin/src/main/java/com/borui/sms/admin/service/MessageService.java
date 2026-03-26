package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.Message;
import com.borui.sms.common.domain.enums.MessageStatus;
import com.borui.sms.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public PageResult<Message> list(int page, int size,
                                    Long customerId, String countryCode,
                                    Long vendorId, Long channelId,
                                    Integer status, String keyword,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        Page<Message> p = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();

        if (customerId != null) wrapper.eq(Message::getCustomerId, customerId);
        if (StringUtils.isNotBlank(countryCode)) wrapper.eq(Message::getCountryCode, countryCode);
        if (vendorId != null) wrapper.eq(Message::getVendorId, vendorId);
        if (channelId != null) wrapper.eq(Message::getChannelId, channelId);
        if (status != null) wrapper.eq(Message::getStatus, MessageStatus.values()[status]);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Message::getMessageId, keyword)
                    .or().like(Message::getToNumber, keyword)
                    .or().like(Message::getVendorMsgId, keyword));
        }
        if (startTime != null) wrapper.ge(Message::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(Message::getCreatedAt, endTime);

        wrapper.orderByDesc(Message::getCreatedAt);
        messageMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Message getByMessageId(String messageId) {
        return messageMapper.selectOne(
                new LambdaQueryWrapper<Message>().eq(Message::getMessageId, messageId));
    }
}
