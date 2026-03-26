package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer_api_credential")
public class CustomerApiCredential {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String apiKey;
    private String apiSecret;            // bcrypt hashed
    private String ipWhitelist;          // comma separated IPs
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
