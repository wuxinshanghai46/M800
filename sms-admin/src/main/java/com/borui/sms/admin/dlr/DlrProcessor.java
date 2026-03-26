package com.borui.sms.admin.dlr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.admin.engine.BillingEngine;
import com.borui.sms.admin.service.CustomerCallbackService;
import com.borui.sms.admin.smpp.SmppDlrListener;
import com.borui.sms.common.domain.entity.Customer;
import com.borui.sms.common.domain.entity.Message;
import com.borui.sms.common.domain.enums.BillingMode;
import com.borui.sms.common.domain.enums.MessageStatus;
import com.borui.sms.mapper.CustomerMapper;
import com.borui.sms.mapper.MessageMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Asynchronous DLR processor.
 * Consumes DLR events from an internal queue, updates message status in DB, and triggers billing confirm/release.
 * MVP: uses in-memory LinkedBlockingQueue + thread pool.
 * Production: replace with RabbitMQ / Kafka consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlrProcessor {

    private final MessageMapper messageMapper;
    private final CustomerMapper customerMapper;
    private final BillingEngine billingEngine;
    private final SmppDlrListener smppDlrListener;
    private final CustomerCallbackService customerCallbackService;
    private final MoProcessor moProcessor;

    private final BlockingQueue<DlrEvent> dlrQueue = new LinkedBlockingQueue<>(10_000);
    private ExecutorService workerPool;
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        // Register DLR callback for SMPP DLR listener
        smppDlrListener.setDlrCallback((vendorMsgId, status) ->
                enqueue(new DlrEvent(vendorMsgId, status)));

        // Register MO callback for SMPP MO messages
        // args: [fromNumber, toSid, content, channelIdStr]
        smppDlrListener.setMoCallback(args -> {
            try {
                String fromNumber = args[0];
                String toSid     = args[1];
                String content   = args[2];
                Long channelId   = (args.length > 3 && !args[3].isBlank())
                        ? Long.parseLong(args[3]) : null;
                // Route to MoProcessor on a separate thread to avoid blocking the SMPP I/O thread
                workerPool.submit(() -> moProcessor.process(fromNumber, toSid, content, channelId, null));
            } catch (Exception e) {
                log.error("MO dispatch error: {}", e.getMessage(), e);
            }
        });

        // Start consumer threads
        int workers = 2;
        workerPool = Executors.newFixedThreadPool(workers,
                r -> { Thread t = new Thread(r, "dlr-worker"); t.setDaemon(true); return t; });

        for (int i = 0; i < workers; i++) {
            workerPool.submit(this::consumeLoop);
        }
        log.info("DlrProcessor started with {} workers, queue capacity=10000", workers);
    }

    /**
     * Enqueue a DLR event for async processing.
     */
    public boolean enqueue(DlrEvent event) {
        boolean offered = dlrQueue.offer(event);
        if (!offered) {
            log.error("DLR queue full! Dropping DLR: vendorMsgId={}", event.getVendorMsgId());
        }
        return offered;
    }

    /**
     * Consumer loop: takes events from queue and processes them.
     */
    private void consumeLoop() {
        while (running) {
            try {
                DlrEvent event = dlrQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    processDlr(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("DLR processing error", e);
            }
        }
    }

    /**
     * Process a single DLR event:
     * 1. Find message by vendorMsgId
     * 2. Validate state transition
     * 3. Update message status
     * 4. Trigger billing confirm (delivered) or release (failed/rejected/expired)
     */
    private void processDlr(DlrEvent event) {
        String vendorMsgId = event.getVendorMsgId();
        MessageStatus newStatus = event.getStatus();

        // Find message by vendor message ID
        Message message = messageMapper.selectOne(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getVendorMsgId, vendorMsgId)
                        .last("LIMIT 1"));

        if (message == null) {
            log.warn("DLR: message not found for vendorMsgId={}", vendorMsgId);
            return;
        }

        // Check state transition validity
        MessageStatus currentStatus = message.getStatus();
        if (!currentStatus.canTransitTo(newStatus)) {
            log.warn("DLR: invalid transition {} -> {} for msgId={}", currentStatus, newStatus, message.getMessageId());
            return;
        }

        // Update message
        message.setStatus(newStatus);
        if (newStatus == MessageStatus.DELIVERED) {
            message.setDeliverAt(LocalDateTime.now());
        }
        if (event.getErrorCode() != null) {
            message.setErrorCode(event.getErrorCode());
        }
        messageMapper.updateById(message);

        // Billing: confirm or release based on billingMode
        // SUBMIT mode: already confirmed at submit time, skip
        BillingMode billingMode = message.getBillingMode();
        if (billingMode == BillingMode.SUBMIT) {
            log.info("DLR {}: msgId={}, vendorMsgId={}, billingMode=SUBMIT (already charged)", newStatus, message.getMessageId(), vendorMsgId);
            return;
        }

        BigDecimal totalPrice = message.getPrice().multiply(BigDecimal.valueOf(message.getSegments()));
        Customer customer = customerMapper.selectById(message.getCustomerId());

        if (newStatus == MessageStatus.DELIVERED) {
            billingEngine.confirm(message.getCustomerId(), totalPrice);
            log.info("DLR DELIVERED: msgId={}, vendorMsgId={}, amount={}, billingMode={}", message.getMessageId(), vendorMsgId, totalPrice, billingMode);
        } else if (newStatus.isTerminal()) {
            billingEngine.release(message.getCustomerId(), totalPrice, customer.getPaymentType());
            log.info("DLR {}: msgId={}, vendorMsgId={}, released={}, billingMode={}", newStatus, message.getMessageId(), vendorMsgId, totalPrice, billingMode);
        }

        // Notify customer via webhook (async fire-and-forget in this thread, already in worker pool)
        triggerDlrCallback(message, newStatus, event.getErrorCode());
    }

    private void triggerDlrCallback(Message message, MessageStatus status, String errorCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageId", message.getMessageId());
            payload.put("clientRef", message.getClientRef());
            payload.put("toNumber", message.getToNumber());
            payload.put("countryCode", message.getCountryCode());
            payload.put("status", status.name());
            payload.put("segments", message.getSegments());
            if (errorCode != null) payload.put("errorCode", errorCode);
            if (message.getDeliverAt() != null) payload.put("deliverAt", message.getDeliverAt().toString());
            payload.put("timestamp", System.currentTimeMillis());

            customerCallbackService.sendDlrCallback(message.getCustomerId(), payload);
        } catch (Exception e) {
            log.warn("DLR callback trigger failed: msgId={}, error={}", message.getMessageId(), e.getMessage());
        }
    }

    /**
     * Get current queue size for monitoring.
     */
    public int getQueueSize() {
        return dlrQueue.size();
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (workerPool != null) {
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
            }
        }
        log.info("DlrProcessor shutdown, remaining events in queue: {}", dlrQueue.size());
    }
}
