package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("number_segment")
public class NumberSegment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String countryCode;
    private String prefix;
    private String operator;
    private String numberType;          // mobile / fixed / voip
    private String source;              // libphonenumber / manual
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
