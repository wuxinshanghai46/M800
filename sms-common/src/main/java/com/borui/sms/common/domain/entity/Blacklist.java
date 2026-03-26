package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("blacklist")
public class Blacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;           // NUMBER / PREFIX / SID / KEYWORD
    private String value;          // the blacklisted value
    private String scope;          // GLOBAL / customer_id
    private Long customerId;       // null for global
    private String countryCode;    // null for all countries
    private String reason;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
