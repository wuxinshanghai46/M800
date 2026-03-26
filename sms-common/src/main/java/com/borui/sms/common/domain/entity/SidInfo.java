package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sid_info")
public class SidInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sid;              // Sender ID value
    private String sidType;          // ALPHA / NUMERIC / FIXED / VIRTUAL
    private String smsType;          // NOTIFICATION / MARKETING / ALL
    private Long vendorId;
    private String countryCode;
    private Integer validityMonths;  // 有效期（月），仅 FIXED/VIRTUAL 类型需填
    private Boolean isActive;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
