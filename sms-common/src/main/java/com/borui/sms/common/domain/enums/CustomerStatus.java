package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerStatus {
    DISABLED(0, "禁用"),
    ACTIVE(1, "正常"),
    FROZEN(2, "冻结"),
    CLOSED(3, "销户");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;
}
