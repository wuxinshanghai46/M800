package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendSmsReq {
    @NotBlank(message = "目标号码不能为空")
    private String to;

    @NotBlank(message = "短信内容不能为空")
    private String content;

    private String sid;
    private String clientRef;
    private Integer smsAttribute;  // 1=OTP, 2=Transaction, 3=Notification, 4=Marketing
}
