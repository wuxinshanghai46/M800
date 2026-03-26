package com.borui.sms.admin.dlr;

import com.borui.sms.common.domain.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DLR event object — represents a delivery receipt from any channel (SMPP or HTTP).
 * Consumed by DlrProcessor to update message status and trigger billing.
 */
@Data
@AllArgsConstructor
public class DlrEvent {
    private String vendorMsgId;
    private MessageStatus status;
    private String errorCode;
    private long timestamp;

    public DlrEvent(String vendorMsgId, MessageStatus status) {
        this.vendorMsgId = vendorMsgId;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
}
