package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.common.domain.entity.CustomerCallbackConfig;
import com.borui.sms.mapper.CustomerCallbackConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

/**
 * Sends DLR and MO event notifications to customer-configured webhook URLs.
 * Signs each payload with HMAC-SHA256 using the customer's configured secret.
 * Retries once on failure (total 2 attempts).
 */
@Slf4j
@Service
public class CustomerCallbackService {

    private final CustomerCallbackConfigMapper callbackConfigMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomerCallbackService(CustomerCallbackConfigMapper callbackConfigMapper,
                                   RestTemplateBuilder restTemplateBuilder) {
        this.callbackConfigMapper = callbackConfigMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Send DLR status update to customer's dlrUrl webhook.
     *
     * @param customerId the customer whose webhook to call
     * @param payload    the DLR event data
     * @return true if the webhook returned a 2xx response
     */
    public boolean sendDlrCallback(Long customerId, Map<String, Object> payload) {
        CustomerCallbackConfig config = getConfig(customerId);
        if (config == null || isBlank(config.getDlrUrl())) {
            return false;
        }
        return postWithRetry(config.getDlrUrl(), config.getDlrSecret(), payload, "DLR", customerId);
    }

    /**
     * Send MO (mobile-originated) message to customer's moUrl webhook.
     *
     * @param customerId the customer whose webhook to call
     * @param payload    the MO event data
     * @return true if the webhook returned a 2xx response
     */
    public boolean sendMoCallback(Long customerId, Map<String, Object> payload) {
        CustomerCallbackConfig config = getConfig(customerId);
        if (config == null || isBlank(config.getMoUrl())) {
            return false;
        }
        return postWithRetry(config.getMoUrl(), config.getMoSecret(), payload, "MO", customerId);
    }

    /**
     * Send a test webhook to verify URL reachability.
     * Used by the portal "Test Callback" button.
     *
     * @param url    the URL to test
     * @param secret optional signing secret
     * @param type   "dlr" or "mo"
     * @return map with success flag and message
     */
    public Map<String, Object> testWebhook(String url, String secret, String type) {
        Map<String, Object> testPayload = Map.of(
                "event", "test",
                "type", type != null ? type : "test",
                "message", "This is a test webhook from BorUI SMS Platform",
                "timestamp", System.currentTimeMillis()
        );
        try {
            boolean ok = postWithRetry(url, secret, testPayload, "TEST", null);
            return Map.of(
                    "success", ok,
                    "message", ok ? "Webhook endpoint responded successfully" : "Webhook endpoint returned non-2xx status"
            );
        } catch (Exception e) {
            log.warn("Test webhook failed: url={}, error={}", url, e.getMessage());
            return Map.of(
                    "success", false,
                    "message", "Connection failed: " + e.getMessage()
            );
        }
    }

    // ===== Internal =====

    private boolean postWithRetry(String url, String secret, Map<String, Object> payload,
                                   String type, Long customerId) {
        int attempts = 2;
        for (int i = 1; i <= attempts; i++) {
            try {
                String body = objectMapper.writeValueAsString(payload);
                HttpHeaders headers = buildHeaders(secret, body, type);
                ResponseEntity<String> response = restTemplate.postForEntity(
                        url, new HttpEntity<>(body, headers), String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("{} callback OK: customerId={}, url={}, status={}, attempt={}",
                            type, customerId, url, response.getStatusCode().value(), i);
                    return true;
                } else {
                    log.warn("{} callback non-2xx: customerId={}, url={}, status={}, attempt={}",
                            type, customerId, url, response.getStatusCode().value(), i);
                }
            } catch (Exception e) {
                log.warn("{} callback attempt {}/{} failed: customerId={}, url={}, error={}",
                        type, i, attempts, customerId, url, e.getMessage());
                if (i < attempts) {
                    sleep(1500); // 1.5s delay before retry
                }
            }
        }
        return false;
    }

    private HttpHeaders buildHeaders(String secret, String body, String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "BorUI-SMS-Platform/2.0");
        headers.set("X-Event-Type", type.toLowerCase());
        headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));

        if (secret != null && !secret.isBlank()) {
            String signature = hmacSha256(secret, body);
            headers.set("X-Signature", "sha256=" + signature);
        }
        return headers;
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("HMAC-SHA256 signing error", e);
            return "";
        }
    }

    private CustomerCallbackConfig getConfig(Long customerId) {
        return callbackConfigMapper.selectOne(
                new LambdaQueryWrapper<CustomerCallbackConfig>()
                        .eq(CustomerCallbackConfig::getCustomerId, customerId));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
}
