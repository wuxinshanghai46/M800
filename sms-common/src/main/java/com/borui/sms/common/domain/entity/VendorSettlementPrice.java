package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("vendor_settlement_price")
public class VendorSettlementPrice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vendorId;
    private String countryCode;
    private Long channelId;
    private Integer smsAttribute;
    private BigDecimal price;
    private String currency;
    private LocalDate effectiveDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
