package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("number_filter_rule")
public class NumberFilterRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String filterType;          // prefix / regex / range
    private String filterValue;
    private String action;              // block / allow / route_to
    private Long targetChannelId;
    private String countryCode;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
