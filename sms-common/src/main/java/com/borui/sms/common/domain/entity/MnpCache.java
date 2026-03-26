package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mnp_cache")
public class MnpCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phoneNumber;
    private String countryCode;
    private String originalOperator;
    private String currentOperator;
    private Boolean ported;
    private String querySource;
    private LocalDateTime expireAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
