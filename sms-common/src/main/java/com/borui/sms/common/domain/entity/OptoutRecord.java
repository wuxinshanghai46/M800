package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("optout_list")
public class OptoutRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phoneNumber;
    private String countryCode;
    private Long customerId;
    private String source;             // MO_REPLY / MANUAL / API / COMPLAINT
    private String keyword;            // the keyword that triggered optout (e.g. STOP, TD)
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
