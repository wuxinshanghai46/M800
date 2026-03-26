package com.borui.sms.admin.mock;

import com.borui.sms.admin.adapter.ChannelAdapter;
import com.borui.sms.admin.adapter.ChannelSendRequest;
import com.borui.sms.admin.adapter.ChannelSendResult;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.enums.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

/**
 * Mock channel adapter for MVP testing.
 * Implements ChannelAdapter interface so SendEngine can use it via ChannelAdapterFactory.
 * Simulates channel send + DLR with configurable success rate.
 */
@Slf4j
@Component
public class MockChannelAdapter implements ChannelAdapter {

    private final Random random = new Random();

    private static final int DELIVERED_RATE = 80;
    private static final int FAILED_RATE = 12;
    private static final int REJECTED_RATE = 5;

    @Override
    public ChannelSendResult send(Channel channel, ChannelSendRequest request) {
        // Simulate latency 20~100ms
        try {
            Thread.sleep(20 + random.nextInt(80));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5% chance of submit failure
        if (random.nextInt(100) < 5) {
            log.warn("Mock send SUBMIT_FAIL: channel={}, to={}", request.getChannelCode(), request.getToNumber());
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("SUBMIT_FAIL")
                    .errorMessage("Mock channel submit failure")
                    .build();
        }

        String vendorMsgId = "vmsg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Mock send OK: channel={}, to={}, vendorMsgId={}", request.getChannelCode(), request.getToNumber(), vendorMsgId);
        return ChannelSendResult.builder()
                .submitted(true)
                .vendorMsgId(vendorMsgId)
                .build();
    }

    @Override
    public String getName() {
        return "MockChannelAdapter";
    }

    @Override
    public boolean isAsyncDlr() {
        return false; // Mock uses sync DLR simulation
    }

    /**
     * Simulate DLR result (called after successful submit).
     * MVP only — production DLR will be async via SMPP deliver_sm or HTTP callback.
     */
    public MessageStatus simulateDlr() {
        try {
            Thread.sleep(50 + random.nextInt(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int roll = random.nextInt(100);
        if (roll < DELIVERED_RATE) {
            return MessageStatus.DELIVERED;
        } else if (roll < DELIVERED_RATE + FAILED_RATE) {
            return MessageStatus.FAILED;
        } else if (roll < DELIVERED_RATE + FAILED_RATE + REJECTED_RATE) {
            return MessageStatus.REJECTED;
        } else {
            return MessageStatus.UNDELIVERABLE;
        }
    }
}
