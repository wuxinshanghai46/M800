package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChannelGroupReq {
    @NotBlank(message = "组名不能为空")
    private String groupName;

    @NotNull(message = "调度模式不能为空")
    private Integer scheduleMode;

    private Boolean isActive = true;
    private String remark;
}
