package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.borui.sms.common.domain.enums.ScheduleMode;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("channel_group")
public class ChannelGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private ScheduleMode scheduleMode;
    private Boolean isActive;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
