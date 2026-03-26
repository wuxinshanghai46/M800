package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingMode {
    SUBMIT(1, "提交计费"),
    DELIVERED(2, "成功计费"),
    REACHED(3, "到达计费");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    @JsonCreator
    public static BillingMode fromCode(int code) {
        for (BillingMode m : values()) {
            if (m.code == code) return m;
        }
        return null;
    }
}
