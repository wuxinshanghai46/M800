package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlacklistReq {
    @NotBlank(message = "类型不能为空")
    private String type;         // NUMBER / PREFIX / SID / KEYWORD

    @NotBlank(message = "值不能为空")
    private String value;

    private String scope = "GLOBAL";
    private Long customerId;
    private String countryCode;
    private String reason;
    private Boolean isActive = true;
}
