package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CountryReq {
    @NotBlank(message = "国家代码不能为空")
    @Size(min = 2, max = 5, message = "国家代码长度2-5")
    private String countryCode;

    @NotBlank(message = "国家名称不能为空")
    private String countryName;

    @NotBlank(message = "英文名称不能为空")
    private String countryNameEn;

    @NotBlank(message = "电话区号不能为空")
    private String phoneCode;

    private String mccMnc;
    private Boolean isActive = true;
    private String remark;
}
