package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sms_template")
public class SmsTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateCode;
    private String templateName;
    private Long customerId;
    private String templateType;        // OTP / Transactional / Marketing / Service
    private String content;
    private String variables;           // JSON array
    private String countryCode;
    private String language;
    private String status;              // pending / approved / rejected (overall)
    private String platformStatus;      // pending / approved / rejected
    private String carrierStatus;       // not_required / pending / approved / rejected
    private String reviewer;
    private LocalDateTime reviewAt;
    private String rejectReason;
    private String dltTemplateId;
    private String dltEntityId;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
