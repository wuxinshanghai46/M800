package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.borui.sms.common.domain.enums.BillingMode;
import com.borui.sms.common.domain.enums.MessageStatus;
import com.borui.sms.common.domain.enums.SmsAttribute;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String messageId;            // platform message ID (msg_xxxx)
    private Long customerId;
    private String countryCode;
    private Long vendorId;
    private Long channelId;
    private String sid;
    private String toNumber;
    private SmsAttribute smsAttribute;   // OTP / TRANSACTION / NOTIFICATION / MARKETING
    private BillingMode billingMode;     // billing mode snapshot at send time
    private String contentHash;          // SHA-256 of content
    private Integer encoding;            // 0=GSM7, 1=UCS2
    private Integer segments;
    private MessageStatus status;
    private String vendorMsgId;
    private LocalDateTime submitAt;
    private LocalDateTime deliverAt;
    private BigDecimal price;            // customer price per segment
    private BigDecimal cost;             // vendor cost per segment
    private String currency;
    private String errorCode;
    private String clientRef;            // customer reference
    private Integer callbackStatus;      // 0=pending, 1=success, 2=failed
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
