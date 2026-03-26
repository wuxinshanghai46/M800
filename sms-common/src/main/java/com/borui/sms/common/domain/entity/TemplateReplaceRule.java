package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_replace_rule")
public class TemplateReplaceRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String triggerCondition;    // country / channel / customer
    private String triggerValue;
    private String matchType;           // keyword / regex
    private String findContent;
    private String replaceWith;
    private String scope;               // global / channel / customer
    private Integer triggerCount;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
