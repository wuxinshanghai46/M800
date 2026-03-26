package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("routing_strategy_assignment")
public class RoutingStrategyAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Integer smsAttribute;
    private String countryCode;
    private Long channelGroupId;
    private Long channelId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
