package com.borui.sms.admin.adapter;

import com.borui.sms.common.domain.entity.Channel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP protocol adapter — sends SMS via vendor HTTP API.
 * Uses channel's httpUrl, httpMethod, httpHeaders, httpBodyTemplate fields.
 *
 * Template variables (replaced in URL and body):
 *   ${to}, ${content}, ${sid}, ${messageId}, ${encoding}, ${segments}, ${channelCode}
 */
@Slf4j
@Component
public class HttpChannelAdapter implements ChannelAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpChannelAdapter(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChannelSendResult send(Channel channel, ChannelSendRequest request) {
        try {
            String url = replaceTemplateVars(channel.getHttpUrl(), request);

            HttpHeaders headers = new HttpHeaders();
            if (channel.getHttpHeaders() != null && !channel.getHttpHeaders().isBlank()) {
                Map<String, String> headerMap = objectMapper.readValue(
                        channel.getHttpHeaders(), new TypeReference<Map<String, String>>() {});
                headerMap.forEach(headers::set);
            }

            String method = channel.getHttpMethod() != null ? channel.getHttpMethod().toUpperCase() : "POST";
            ResponseEntity<String> response;

            if ("GET".equals(method)) {
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } else {
                String body = "";
                if (channel.getHttpBodyTemplate() != null && !channel.getHttpBodyTemplate().isBlank()) {
                    body = replaceTemplateVars(channel.getHttpBodyTemplate(), request);
                }
                if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            }

            if (response.getStatusCode().is2xxSuccessful()) {
                String vendorMsgId = parseVendorMsgId(response.getBody());
                log.info("HTTP send OK: channel={}, to={}, vendorMsgId={}, httpStatus={}",
                        request.getChannelCode(), request.getToNumber(), vendorMsgId, response.getStatusCode());
                return ChannelSendResult.builder()
                        .submitted(true)
                        .vendorMsgId(vendorMsgId)
                        .build();
            } else {
                log.warn("HTTP send failed: channel={}, to={}, httpStatus={}, body={}",
                        request.getChannelCode(), request.getToNumber(),
                        response.getStatusCode(), truncate(response.getBody(), 200));
                return ChannelSendResult.builder()
                        .submitted(false)
                        .errorCode("HTTP_" + response.getStatusCode().value())
                        .errorMessage("HTTP error: " + response.getStatusCode())
                        .build();
            }

        } catch (RestClientException e) {
            log.error("HTTP send error: channel={}, to={}, error={}",
                    request.getChannelCode(), request.getToNumber(), e.getMessage());
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("HTTP_CLIENT_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("HTTP send unexpected error: channel={}, to={}",
                    request.getChannelCode(), request.getToNumber(), e);
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("HTTP_INTERNAL_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private String replaceTemplateVars(String template, ChannelSendRequest request) {
        if (template == null) return "";
        return template
                .replace("${to}", request.getToNumber() != null ? request.getToNumber() : "")
                .replace("${content}", request.getContent() != null ? request.getContent() : "")
                .replace("${sid}", request.getSid() != null ? request.getSid() : "")
                .replace("${messageId}", request.getMessageId() != null ? request.getMessageId() : "")
                .replace("${encoding}", String.valueOf(request.getEncoding()))
                .replace("${segments}", String.valueOf(request.getSegments()))
                .replace("${channelCode}", request.getChannelCode() != null ? request.getChannelCode() : "");
    }

    private String parseVendorMsgId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "HTTP_" + System.currentTimeMillis();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            for (String field : new String[]{"messageId", "msgId", "id", "message_id", "msg_id"}) {
                JsonNode node = root.get(field);
                if (node != null && !node.isNull()) {
                    return node.asText();
                }
                JsonNode data = root.get("data");
                if (data != null) {
                    node = data.get(field);
                    if (node != null && !node.isNull()) {
                        return node.asText();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Cannot parse vendor msgId from response: {}", truncate(responseBody, 100));
        }
        return "HTTP_" + System.currentTimeMillis();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    @Override
    public String getName() {
        return "HttpChannelAdapter";
    }
}
