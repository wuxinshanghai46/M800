package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("country")
public class Country {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String countryCode;      // ISO 3166-1 alpha-2, e.g. US, CN
    private String countryName;
    private String countryNameEn;
    private String phoneCode;        // e.g. +1, +86
    private String mccMnc;           // MCC-MNC list (comma separated)
    private Boolean isActive;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
