package com.borui.sms.admin.vo.resp;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SendSmsResp {
    private String messageId;
    private String to;
    private String countryCode;
    private Integer segments;
    private String encoding;
    private BigDecimal price;
    private String currency;
    private String status;
}
