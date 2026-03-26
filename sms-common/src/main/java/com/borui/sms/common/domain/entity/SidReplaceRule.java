package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sid_replace_rule")
public class SidReplaceRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String originalSid;
    private Long targetChannelId;
    private String replacementSid;
    private String replaceReason;
    private Integer triggerCount;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
