package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelStatus {
    OFFLINE(0, "连接断开"),
    ONLINE(1, "连接正常"),
    TESTING(2, "测试中"),
    UNKNOWN(3, "未知");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;
}
