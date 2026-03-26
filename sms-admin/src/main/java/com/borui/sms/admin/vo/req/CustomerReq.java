package com.borui.sms.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerReq {
    @NotBlank(message = "客户代码不能为空")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String companyName;

    private Integer status = 1;

    @NotNull(message = "付费类型不能为空")
    private Integer paymentType;

    private String salesManager;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String timezone = "UTC";
    private String billCurrency = "USD";
    private Boolean billAutoSend = false;
    private String billEmail;
    private Boolean sidSelectable = false;
    private String preferredSidFormat = "ALPHA";
    private String allowedSmsAttributes = "1,2,3,4";
    private String address;
    private String portalAccount;
    private String portalPassword;
    private String remark;
}
