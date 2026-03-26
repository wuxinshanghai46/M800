package com.borui.sms.admin.adapter;

import lombok.Builder;
import lombok.Data;

/**
 * Unified channel send result — returned after submit_sm / HTTP POST.
 */
@Data
@Builder
public class ChannelSendResult {
    /** true if the channel accepted the message (submit success) */
    private boolean submitted;
    /** Vendor-assigned message ID (for DLR correlation), null if submit failed */
    private String vendorMsgId;
    /** Error code from channel on submit failure */
    private String errorCode;
    /** Error description */
    private String errorMessage;
}
