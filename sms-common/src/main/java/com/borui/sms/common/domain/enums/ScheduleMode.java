package com.borui.sms.common.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScheduleMode {
    FAILOVER(1, "故障转移"),
    WEIGHT(2, "权重轮询"),
    PRIORITY_WEIGHT(3, "优先级加权");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    @JsonCreator
    public static ScheduleMode fromCode(int code) {
        for (ScheduleMode m : values()) {
            if (m.code == code) return m;
        }
        return FAILOVER;
    }
}
