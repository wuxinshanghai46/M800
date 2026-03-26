package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_variable")
public class TemplateVariable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String varName;
    private String varType;             // string / number / date / phone / enum / url
    private String description;
    private String defaultValue;
    private Integer maxLength;
    private Boolean required;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
