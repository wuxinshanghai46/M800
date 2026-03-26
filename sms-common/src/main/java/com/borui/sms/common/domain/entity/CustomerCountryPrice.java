package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.borui.sms.common.domain.enums.BillingMode;
import com.borui.sms.common.domain.enums.SmsAttribute;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("customer_country_price")
public class CustomerCountryPrice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String countryCode;
    private SmsAttribute smsAttribute;
    private BigDecimal price;            // per segment
    private BillingMode billingMode;     // SUBMIT / DELIVERED / REACHED
    private String currency;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
