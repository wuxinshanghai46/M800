package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("customer_account")
public class CustomerAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private BigDecimal balance;          // available balance
    private BigDecimal frozenAmount;     // frozen for pending messages
    private BigDecimal creditLimit;      // for postpaid customers
    private String currency;             // USD
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
