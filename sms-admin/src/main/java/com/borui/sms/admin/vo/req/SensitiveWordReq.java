package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SensitiveWordReq {
    @NotBlank(message = "敏感词不能为空")
    private String word;

    private String matchType = "CONTAINS";
    private String action = "BLOCK";
    private String category = "OTHER";
    private String scope = "GLOBAL";
    private Long customerId;
    private String countryCode;
    private Boolean isActive = true;
}
