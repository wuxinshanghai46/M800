package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class PortalSendBatchReq {
    @NotEmpty(message = "号码列表不能为空")
    @Size(max = 500, message = "单次最多500个号码")
    private List<String> numbers;

    @NotBlank(message = "短信内容不能为空")
    private String content;

    private String sid;
    private String clientRef;
    private Integer smsAttribute;
}
