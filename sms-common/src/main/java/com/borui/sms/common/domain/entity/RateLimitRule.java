package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rate_limit_rule")
public class RateLimitRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dimension;      // NUMBER / CUSTOMER / CUSTOMER_COUNTRY / CHANNEL
    private Long customerId;       // null for global
    private String countryCode;    // null for all countries
    private Integer maxCount;      // max messages
    private Integer windowSeconds; // time window in seconds
    private String action;         // BLOCK / DELAY / WARN
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
