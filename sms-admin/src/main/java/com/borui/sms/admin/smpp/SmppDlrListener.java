package com.borui.sms.admin.smpp;

import com.borui.sms.common.domain.enums.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for deliver_sm PDUs (DLR callbacks) from SMPP sessions.
 * Parses DLR text to extract message status and vendor message ID,
 * then delegates to registered callback for DB update + billing.
 */
@Slf4j
@Component
public class SmppDlrListener implements MessageReceiverListener {

    /**
     * DLR text format (SMSC standard):
     * id:XXXXX sub:001 dlvrd:001 submit date:2603151200 done date:2603151201 stat:DELIVRD err:000 text:...
     */
    private static final Pattern DLR_ID_PATTERN = Pattern.compile("id:([\\w-]+)");
    private static final Pattern DLR_STAT_PATTERN = Pattern.compile("stat:(\\w+)");

    // Map SMPP DLR stat values to our MessageStatus
    private static final Map<String, MessageStatus> STAT_MAP = Map.of(
            "DELIVRD", MessageStatus.DELIVERED,
            "UNDELIV", MessageStatus.UNDELIVERABLE,
            "REJECTD", MessageStatus.REJECTED,
            "EXPIRED", MessageStatus.EXPIRED,
            "ACCEPTD", MessageStatus.SUBMITTED,
            "DELETED", MessageStatus.FAILED,
            "UNKNOWN", MessageStatus.UNKNOWN
    );

    /**
     * Callback: (vendorMsgId, messageStatus) -> handle DLR in DlrProcessor.
     */
    private volatile BiConsumer<String, MessageStatus> dlrCallback;

    /**
     * Callback: (fromNumber, toSid, content, channelId) -> handle MO in MoProcessor.
     * channelId may be null if not yet known at this stage.
     */
    private volatile java.util.function.Consumer<String[]> moCallback;

    // Track last DLR per vendor message ID for dedup
    private final Map<String, Long> recentDlrTimestamps = new ConcurrentHashMap<>();

    public void setDlrCallback(BiConsumer<String, MessageStatus> callback) {
        this.dlrCallback = callback;
    }

    /**
     * Register a callback for MO messages.
     * The String[] argument is [fromNumber, toSid, content, channelIdStr].
     */
    public void setMoCallback(java.util.function.Consumer<String[]> callback) {
        this.moCallback = callback;
    }

    @Override
    public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
        if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
            // This is a DLR (delivery receipt)
            processDlr(deliverSm);
        } else {
            // This is an MO (mobile originated) message
            handleMo(deliverSm);
        }
    }

    private void processDlr(DeliverSm deliverSm) {
        String dlrText = new String(deliverSm.getShortMessage());
        log.info("SMPP DLR received: text={}", dlrText);

        String vendorMsgId = extractField(dlrText, DLR_ID_PATTERN);
        String stat = extractField(dlrText, DLR_STAT_PATTERN);

        if (vendorMsgId == null || stat == null) {
            log.warn("SMPP DLR parse failed: cannot extract id or stat from: {}", dlrText);
            return;
        }

        MessageStatus status = STAT_MAP.getOrDefault(stat.toUpperCase(), MessageStatus.UNKNOWN);

        // Dedup: ignore DLR if we received one for same vendorMsgId within 5 seconds
        Long lastTs = recentDlrTimestamps.get(vendorMsgId);
        long now = System.currentTimeMillis();
        if (lastTs != null && (now - lastTs) < 5000) {
            log.debug("SMPP DLR dedup: vendorMsgId={}, ignoring duplicate", vendorMsgId);
            return;
        }
        recentDlrTimestamps.put(vendorMsgId, now);

        log.info("SMPP DLR parsed: vendorMsgId={}, stat={}, status={}", vendorMsgId, stat, status);

        if (dlrCallback != null) {
            try {
                dlrCallback.accept(vendorMsgId, status);
            } catch (Exception e) {
                log.error("SMPP DLR callback error: vendorMsgId={}, error={}", vendorMsgId, e.getMessage(), e);
            }
        } else {
            log.warn("SMPP DLR callback not set, ignoring DLR for vendorMsgId={}", vendorMsgId);
        }

        // Cleanup old entries (simple eviction: remove entries older than 60s)
        long cutoff = now - 60_000;
        recentDlrTimestamps.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    private void handleMo(DeliverSm deliverSm) {
        String fromNumber = deliverSm.getSourceAddr();
        String toSid = deliverSm.getDestAddress();
        String content = deliverSm.getShortMessage() != null
                ? new String(deliverSm.getShortMessage()) : "";

        log.info("SMPP MO received: from={}, to={}, content={}", fromNumber, toSid, content);

        if (moCallback != null) {
            try {
                // Pass [fromNumber, toSid, content, ""] — channelId resolved by MoProcessor via SID lookup
                moCallback.accept(new String[]{fromNumber, toSid, content, ""});
            } catch (Exception e) {
                log.error("SMPP MO callback error: from={}, error={}", fromNumber, e.getMessage(), e);
            }
        } else {
            log.warn("SMPP MO callback not set, MO from={} discarded", fromNumber);
        }
    }

    private String extractField(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public void onAcceptAlertNotification(AlertNotification alertNotification) {
        log.debug("SMPP alert notification: {}", alertNotification);
    }

    @Override
    public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
        log.debug("SMPP data_sm received");
        return null;
    }
}
