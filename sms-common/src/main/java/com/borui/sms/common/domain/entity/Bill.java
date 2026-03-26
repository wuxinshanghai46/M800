package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("bill")
public class Bill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;
    private Long customerId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalMessages;
    private Integer totalSegments;
    private BigDecimal amount;
    private String currency;
    private String status;           // draft / issued / paid / overdue
    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
