package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_ticket")
public class ReviewTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String messageId;
    private String contentPreview;
    private String triggerRule;
    private String status;          // pending / approved / rejected
    private String reviewer;
    private LocalDateTime reviewAt;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
