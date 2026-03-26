package com.borui.sms.common.common.util;

import com.borui.sms.common.common.constant.SmsConstants;
import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}

    public static String messageId() {
        return SmsConstants.MSG_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    public static String apiKey() {
        return "ak_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String apiSecret() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
