package com.borui.sms.common.common.constant;

public final class SmsConstants {
    private SmsConstants() {}

    // GSM7 single SMS max chars
    public static final int GSM7_SINGLE_MAX = 160;
    // GSM7 multipart SMS max chars per segment
    public static final int GSM7_MULTI_MAX = 153;
    // UCS2 single SMS max chars
    public static final int UCS2_SINGLE_MAX = 70;
    // UCS2 multipart SMS max chars per segment
    public static final int UCS2_MULTI_MAX = 67;

    // Message ID prefix
    public static final String MSG_ID_PREFIX = "msg_";

    // Default currency
    public static final String DEFAULT_CURRENCY = "USD";

    // Default page size
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}
