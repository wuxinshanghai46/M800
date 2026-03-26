package com.borui.sms.admin.dlr;

import com.borui.sms.common.domain.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HTTP DLR callback endpoint — vendors push delivery receipts to this API.
 * Each vendor may have different payload formats, so we support multiple parsing strategies.
 *
 * URL pattern: POST /v1/callback/dlr/{channelCode}
 */
@Slf4j
@RestController
@RequestMapping("/v1/callback/dlr")
@RequiredArgsConstructor
public class DlrCallbackController {

    private final DlrProcessor dlrProcessor;

    // Map vendor status strings to our MessageStatus
    private static final Map<String, MessageStatus> STATUS_MAP = Map.ofEntries(
            Map.entry("DELIVRD", MessageStatus.DELIVERED),
            Map.entry("DELIVERED", MessageStatus.DELIVERED),
            Map.entry("delivered", MessageStatus.DELIVERED),
            Map.entry("UNDELIV", MessageStatus.UNDELIVERABLE),
            Map.entry("UNDELIVERABLE", MessageStatus.UNDELIVERABLE),
            Map.entry("REJECTD", MessageStatus.REJECTED),
            Map.entry("REJECTED", MessageStatus.REJECTED),
            Map.entry("rejected", MessageStatus.REJECTED),
            Map.entry("EXPIRED", MessageStatus.EXPIRED),
            Map.entry("expired", MessageStatus.EXPIRED),
            Map.entry("FAILED", MessageStatus.FAILED),
            Map.entry("failed", MessageStatus.FAILED),
            Map.entry("ACCEPTD", MessageStatus.SUBMITTED),
            Map.entry("UNKNOWN", MessageStatus.UNKNOWN)
    );

    /**
     * Generic DLR callback.
     * Expects JSON body with at least: messageId/msgId/id + status/stat
     * Optional: errorCode
     *
     * Example payloads:
     *   {"messageId":"abc123","status":"DELIVERED"}
     *   {"msgId":"abc123","stat":"DELIVRD","err":"000"}
     *   {"id":"abc123","status":"failed","error_code":"EC01"}
     */
    @PostMapping("/{channelCode}")
    public ResponseEntity<String> handleDlr(
            @PathVariable String channelCode,
            @RequestBody Map<String, Object> payload) {

        log.info("HTTP DLR callback: channel={}, payload={}", channelCode, payload);

        // Extract vendor message ID
        String vendorMsgId = extractString(payload, "messageId", "msgId", "id", "message_id", "msg_id");
        if (vendorMsgId == null) {
            log.warn("HTTP DLR: missing messageId in payload from channel={}", channelCode);
            return ResponseEntity.badRequest().body("missing messageId");
        }

        // Extract status
        String statusStr = extractString(payload, "status", "stat", "delivery_status", "dlr_status");
        if (statusStr == null) {
            log.warn("HTTP DLR: missing status in payload from channel={}", channelCode);
            return ResponseEntity.badRequest().body("missing status");
        }

        MessageStatus status = STATUS_MAP.getOrDefault(statusStr, MessageStatus.UNKNOWN);

        // Extract optional error code
        String errorCode = extractString(payload, "errorCode", "error_code", "err", "error");

        // Enqueue for async processing
        DlrEvent event = new DlrEvent(vendorMsgId, status, errorCode, System.currentTimeMillis());
        dlrProcessor.enqueue(event);

        log.info("HTTP DLR queued: channel={}, vendorMsgId={}, status={}", channelCode, vendorMsgId, status);
        return ResponseEntity.ok("OK");
    }

    /**
     * Extract a string value trying multiple field names.
     */
    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return null;
    }
}
