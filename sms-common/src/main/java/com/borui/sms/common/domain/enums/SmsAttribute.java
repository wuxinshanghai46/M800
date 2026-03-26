package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SmsAttribute {
    OTP(1, "OTP验证码"),
    TRANSACTION(2, "事务通知"),
    NOTIFICATION(3, "通知"),
    MARKETING(4, "营销");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    @JsonCreator
    public static SmsAttribute fromCode(int code) {
        for (SmsAttribute a : values()) {
            if (a.code == code) return a;
        }
        return null;
    }
}
