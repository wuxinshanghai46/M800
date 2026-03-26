package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelType {
    SMPP(1, "SMPP"),
    HTTP(2, "HTTP");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    @JsonCreator
    public static ChannelType fromCode(int code) {
        for (ChannelType t : values()) {
            if (t.code == code) return t;
        }
        return HTTP;
    }
}
