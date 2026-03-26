package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerType {
    DIRECT(1, "直客"),
    AGENT(2, "代理商");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    @JsonCreator
    public static CustomerType fromCode(int code) {
        for (CustomerType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }
}
