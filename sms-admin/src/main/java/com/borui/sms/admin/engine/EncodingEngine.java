package com.borui.sms.admin.engine;

import com.borui.sms.common.common.constant.SmsConstants;
import org.springframework.stereotype.Component;

/**
 * Encoding detection and segment calculation.
 * Pure CPU, no IO — identical to production version.
 */
@Component
public class EncodingEngine {

    // GSM 7-bit basic character set
    private static final String GSM7_CHARS =
            "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞ ÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
                    + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";

    // GSM 7-bit extension characters (count as 2)
    private static final String GSM7_EXT = "^{}\\[~]|€";

    /**
     * Detect encoding: 0=GSM7, 1=UCS2
     */
    public int detectEncoding(String content) {
        if (content == null || content.isEmpty()) return 0;
        for (char c : content.toCharArray()) {
            if (GSM7_CHARS.indexOf(c) < 0 && GSM7_EXT.indexOf(c) < 0) {
                return 1; // UCS2
            }
        }
        return 0; // GSM7
    }

    /**
     * Calculate GSM7 char count (extension chars count as 2)
     */
    public int gsm7Length(String content) {
        int len = 0;
        for (char c : content.toCharArray()) {
            len += (GSM7_EXT.indexOf(c) >= 0) ? 2 : 1;
        }
        return len;
    }

    /**
     * Calculate segment count
     */
    public int calculateSegments(String content, int encoding) {
        if (content == null || content.isEmpty()) return 1;

        if (encoding == 0) { // GSM7
            int len = gsm7Length(content);
            if (len <= SmsConstants.GSM7_SINGLE_MAX) return 1;
            return (int) Math.ceil((double) len / SmsConstants.GSM7_MULTI_MAX);
        } else { // UCS2
            int len = content.length();
            if (len <= SmsConstants.UCS2_SINGLE_MAX) return 1;
            return (int) Math.ceil((double) len / SmsConstants.UCS2_MULTI_MAX);
        }
    }
}
