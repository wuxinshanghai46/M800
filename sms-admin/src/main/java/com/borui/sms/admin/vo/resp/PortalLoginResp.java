package com.borui.sms.admin.vo.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PortalLoginResp {
    private String token;
    private Long customerId;
    private String account;
    private String companyName;
    private BigDecimal balance;
    private String currency;
}
