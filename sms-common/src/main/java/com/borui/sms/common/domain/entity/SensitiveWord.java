package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sensitive_word")
public class SensitiveWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private String matchType;      // EXACT / CONTAINS / REGEX
    private String action;         // BLOCK / WARN / LOG
    private String category;       // SPAM / FRAUD / POLITICAL / OTHER
    private String scope;          // GLOBAL / CUSTOMER
    private Long customerId;       // null for global
    private String countryCode;    // null for all countries
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
