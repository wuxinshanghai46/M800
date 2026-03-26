package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.borui.sms.common.domain.enums.CustomerStatus;
import com.borui.sms.common.domain.enums.CustomerType;
import com.borui.sms.common.domain.enums.PaymentType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String customerName;
    private String companyName;
    private CustomerStatus status;
    private PaymentType paymentType;
    private CustomerType customerType;   // DIRECT / AGENT
    private Long parentId;               // parent customer ID for agent hierarchy
    private String salesManager;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String timezone;
    private String billCurrency;         // USD / HKD / CNY
    private Boolean billAutoSend;
    private String billEmail;
    private Boolean sidSelectable;       // allow customer to select SID
    private String preferredSidFormat;   // ALPHA / NUMERIC / SHORTCODE
    private String allowedSmsAttributes;
    private String address;
    private String portalAccount;
    private String portalPassword;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
