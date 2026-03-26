package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer_sid")
public class CustomerSid {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long sidId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
