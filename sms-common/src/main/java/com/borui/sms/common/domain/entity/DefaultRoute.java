package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("default_route")
public class DefaultRoute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String countryCode;
    private String operator;
    private Integer smsAttribute;
    private Long channelId;
    private Integer priority;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
