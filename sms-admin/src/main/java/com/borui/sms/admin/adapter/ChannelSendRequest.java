package com.borui.sms.admin.adapter;

import lombok.Builder;
import lombok.Data;

/**
 * Unified channel send request — protocol-agnostic.
 */
@Data
@Builder
public class ChannelSendRequest {
    private String channelCode;
    private String toNumber;
    private String content;
    private String sid;
    private int encoding;       // 0=GSM7, 1=UCS2
    private int segments;
    private String messageId;   // platform message ID for correlation
}
