package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageStatus {
    ACCEPTED(0, "已接收"),
    SUBMITTED(1, "已提交"),
    DELIVERED(2, "已送达"),
    FAILED(3, "失败"),
    REJECTED(4, "拒绝"),
    EXPIRED(5, "过期"),
    UNDELIVERABLE(6, "不可达"),
    UNKNOWN(7, "未知");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    public boolean isTerminal() {
        return this.code >= 2 && this.code <= 6;
    }

    public boolean canTransitTo(MessageStatus target) {
        if (this.isTerminal()) return false;
        if (this == ACCEPTED) return target == SUBMITTED || target.isTerminal();
        if (this == SUBMITTED) return target.isTerminal() || target == UNKNOWN;
        if (this == UNKNOWN) return target.isTerminal();
        return false;
    }
}
