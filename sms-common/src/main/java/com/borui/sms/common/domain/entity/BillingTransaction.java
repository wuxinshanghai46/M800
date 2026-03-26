package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("billing_transaction")
public class BillingTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String type;            // RECHARGE / DEDUCT / REFUND
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
