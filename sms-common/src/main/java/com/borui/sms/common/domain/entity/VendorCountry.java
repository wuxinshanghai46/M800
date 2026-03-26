package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vendor_country")
public class VendorCountry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vendorId;
    private String countryCode;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
