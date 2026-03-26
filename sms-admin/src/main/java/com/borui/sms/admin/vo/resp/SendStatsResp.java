package com.borui.sms.admin.vo.resp;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SendStatsResp {
    private String dimension;        // country / vendor / customer
    private String dimensionValue;   // US / Twilio / ShopMax
    private String dimensionLabel;   // display name
    private Long sent;
    private Long delivered;
    private Long failed;
    private Long segments;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
    private Double deliveryRate;
}
