package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vendor")
public class Vendor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String vendorCode;       // unique code
    private String vendorName;
    private String countryCode;          // 供应商注册国家
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String apiType;          // SMPP / HTTP
    private Boolean isActive;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
