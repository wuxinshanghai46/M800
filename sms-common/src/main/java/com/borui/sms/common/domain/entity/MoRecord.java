package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mo_record")
public class MoRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String moMessageId;
    private String fromNumber;
    private String toSid;
    private String content;
    private String countryCode;
    private Long channelId;
    private String matchKeyword;
    private String action;
    private String pushStatus;
    private String pushUrl;
    private String pushResponse;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
