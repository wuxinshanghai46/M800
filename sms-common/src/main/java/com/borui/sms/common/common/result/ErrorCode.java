package com.borui.sms.common.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Common 10000~10099
    SUCCESS(0, "success"),
    PARAM_ERROR(10001, "参数校验失败"),
    AUTH_FAILED(10002, "认证失败"),
    PERMISSION_DENIED(10003, "权限不足"),
    NOT_FOUND(10004, "资源不存在"),
    DUPLICATE(10005, "数据已存在"),
    OPERATION_FAILED(10006, "操作失败"),

    // Customer 10100~10199
    CUSTOMER_NOT_FOUND(10100, "客户不存在"),
    CUSTOMER_DISABLED(10101, "客户已禁用"),
    CUSTOMER_FROZEN(10102, "客户已冻结"),

    // Channel 10200~10299
    CHANNEL_NOT_FOUND(10200, "通道不存在"),
    CHANNEL_DISABLED(10201, "通道已禁用"),
    NO_AVAILABLE_CHANNEL(10202, "无可用通道"),

    // Send 10300~10399
    SEND_BALANCE_INSUFFICIENT(10300, "余额不足"),
    SEND_COUNTRY_NOT_ALLOWED(10301, "国家未授权"),
    SEND_BLACKLISTED(10302, "号码在黑名单"),
    SEND_RATE_LIMITED(10303, "触发频控限制"),
    SEND_NO_CHANNEL(10304, "无可用通道"),
    SEND_CONTENT_BLOCKED(10305, "内容含敏感词"),

    // Billing 10500~10599
    BILLING_FREEZE_FAILED(10500, "计费冻结失败"),
    BILLING_INSUFFICIENT(10501, "余额不足以冻结"),

    // System 99999
    SYSTEM_ERROR(99999, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;
}
