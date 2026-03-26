package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SidInfoReq {
    @NotBlank(message = "SID不能为空")
    private String sid;

    private String sidType = "ALPHA";

    @NotNull(message = "供应商ID不能为空")
    private Long vendorId;

    @NotBlank(message = "国家代码不能为空")
    private String countryCode;

    private String smsType = "ALL";
    private Integer validityMonths;
    private Boolean isActive = true;
    private String remark;
}
