package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChannelReq {
    @NotBlank(message = "通道代码不能为空")
    private String channelCode;

    @NotBlank(message = "通道名称不能为空")
    private String channelName;

    @NotNull(message = "供应商ID不能为空")
    private Long vendorId;

    private String countryCode;

    @NotNull(message = "通道类型不能为空")
    private Integer channelType;

    private Integer status = 1;
    private Integer smsAttribute;
    private Integer tps = 100;
    private Integer priority = 10;

    // SMPP
    private String smppHost;
    private Integer smppPort = 2775;
    @jakarta.validation.constraints.Size(max = 15, message = "SMPP system_id 最多15个字符")
    private String smppSystemId;
    @jakarta.validation.constraints.Size(max = 8, message = "SMPP 密码最多8个字符（SMPP 3.4协议限制）")
    private String smppPassword;
    private String smppSystemType;
    private Integer smppWindowSize = 50;
    private Integer smppEnquireLinkInterval = 30;
    private String smppBindType = "BIND_TRX";         // BIND_TRX / BIND_TX / BIND_RX
    private String smppDestTon = "INTERNATIONAL";     // dest TypeOfNumber
    private String smppDestNpi = "ISDN";              // dest NPI

    // HTTP
    private String httpUrl;
    private String httpMethod = "POST";
    private String httpHeaders;
    private String httpBodyTemplate;

    // SMS params
    private String defaultEncoding = "AUTO";
    private Integer maxSegments = 10;
    private Integer dlrWaitTimeout = 300;
    private Integer retryCount = 3;

    private BigDecimal costPrice;
    private String costCurrency = "USD";
    private Boolean isActive = true;
    private String remark;
}
