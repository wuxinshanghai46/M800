package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.borui.sms.common.domain.enums.ChannelStatus;
import com.borui.sms.common.domain.enums.ChannelType;
import com.borui.sms.common.domain.enums.SmsAttribute;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("channel")
public class Channel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelCode;
    private String channelName;
    private Long vendorId;
    private String countryCode;
    private ChannelType channelType;     // SMPP / HTTP
    private ChannelStatus status;
    private SmsAttribute smsAttribute;   // OTP / TRANSACTION / NOTIFICATION / MARKETING
    private Integer tps;                 // max TPS
    private Integer priority;            // lower = higher priority

    // SMPP config
    private String smppHost;
    private Integer smppPort;
    private String smppSystemId;
    private String smppPassword;
    private String smppSystemType;
    private Integer smppWindowSize;
    private Integer smppEnquireLinkInterval;  // seconds
    private String smppBindType;              // BIND_TRX / BIND_TX / BIND_RX, default BIND_TRX
    private String smppDestTon;               // dest TypeOfNumber: INTERNATIONAL/NATIONAL/UNKNOWN/ALPHANUMERIC
    private String smppDestNpi;               // dest NPI: ISDN/UNKNOWN/NATIONAL/PRIVATE

    // HTTP config
    private String httpUrl;
    private String httpMethod;           // POST / GET
    private String httpHeaders;          // JSON string
    private String httpBodyTemplate;     // velocity/freemarker template

    // SMS params
    private String defaultEncoding;      // GSM7 / UCS2 / AUTO
    private Integer maxSegments;
    private Integer dlrWaitTimeout;      // seconds
    private Integer retryCount;

    private BigDecimal costPrice;        // vendor cost per segment
    private String costCurrency;
    private Boolean isActive;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
