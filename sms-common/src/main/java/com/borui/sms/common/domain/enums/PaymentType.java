package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentType {
    PREPAID(1, "预付费"),
    POSTPAID(2, "后付费");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;
}
