package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer_callback_config")
public class CustomerCallbackConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String dlrUrl;
    private String dlrSecret;
    private String moUrl;
    private String moSecret;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
