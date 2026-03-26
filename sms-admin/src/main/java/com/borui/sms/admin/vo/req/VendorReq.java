package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorReq {
    @NotBlank(message = "供应商代码不能为空")
    private String vendorCode;

    @NotBlank(message = "供应商名称不能为空")
    private String vendorName;

    private String countryCode;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String apiType = "SMPP";
    private Boolean isActive = true;
    private String remark;
}
