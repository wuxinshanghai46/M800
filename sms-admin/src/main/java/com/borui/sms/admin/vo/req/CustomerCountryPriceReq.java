package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerCountryPriceReq {
    @NotBlank(message = "国家代码不能为空")
    private String countryCode;

    private Integer smsAttribute;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private String currency = "USD";
}
