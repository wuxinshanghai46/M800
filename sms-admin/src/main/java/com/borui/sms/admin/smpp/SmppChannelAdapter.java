package com.borui.sms.admin.smpp;

import com.borui.sms.admin.adapter.ChannelAdapter;
import com.borui.sms.admin.adapter.ChannelSendRequest;
import com.borui.sms.admin.adapter.ChannelSendResult;
import com.borui.sms.common.domain.entity.Channel;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SubmitSmResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SMPP protocol adapter — sends SMS via SMPP submit_sm.
 * Handles encoding, long message splitting (UDH), TPS rate limiting, and error mapping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmppChannelAdapter implements ChannelAdapter {

    private final SmppSessionManager sessionManager;

    // Per-channel rate limiters for TPS control
    private final Map<Long, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // UDH reference counter for long message concatenation
    private static final AtomicInteger udhRefCounter = new AtomicInteger(0);

    @Override
    public ChannelSendResult send(Channel channel, ChannelSendRequest request) {
        try {
            // TPS rate limiting
            acquireRateLimit(channel);

            // Resolve dest TON/NPI from channel config
            TypeOfNumber destTon = resolveTypeOfNumber(channel.getSmppDestTon());
            NumberingPlanIndicator destNpi = resolveNpi(channel.getSmppDestNpi());

            // Get SMPP session
            SMPPSession session = sessionManager.getSession(channel);

            // Determine encoding
            Alphabet alphabet;
            byte[] messageBytes;
            if (request.getEncoding() == 1) {
                alphabet = Alphabet.ALPHA_UCS2;
                messageBytes = request.getContent().getBytes(StandardCharsets.UTF_16BE);
            } else {
                alphabet = Alphabet.ALPHA_DEFAULT;
                messageBytes = request.getContent().getBytes(StandardCharsets.ISO_8859_1);
            }

            String vendorMsgId;

            if (request.getSegments() > 1) {
                vendorMsgId = sendLongMessage(session, request, alphabet, messageBytes, destTon, destNpi);
            } else {
                vendorMsgId = submitSm(session, request, alphabet, messageBytes, null, destTon, destNpi);
            }

            log.info("SMPP submit OK: channel={}, to={}, vendorMsgId={}",
                    request.getChannelCode(), request.getToNumber(), vendorMsgId);

            return ChannelSendResult.builder()
                    .submitted(true)
                    .vendorMsgId(vendorMsgId)
                    .build();

        } catch (NegativeResponseException e) {
            log.warn("SMPP submit rejected: channel={}, to={}, commandStatus=0x{}",
                    request.getChannelCode(), request.getToNumber(),
                    Integer.toHexString(e.getCommandStatus()));
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("SMPP_" + Integer.toHexString(e.getCommandStatus()))
                    .errorMessage("SMPP negative response: " + e.getMessage())
                    .build();

        } catch (ResponseTimeoutException e) {
            log.warn("SMPP submit timeout: channel={}, to={}", request.getChannelCode(), request.getToNumber());
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("SMPP_TIMEOUT")
                    .errorMessage("SMPP response timeout")
                    .build();

        } catch (PDUException | InvalidResponseException e) {
            log.error("SMPP PDU error: channel={}, to={}, error={}",
                    request.getChannelCode(), request.getToNumber(), e.getMessage());
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("SMPP_PDU_ERROR")
                    .errorMessage(e.getMessage())
                    .build();

        } catch (IOException e) {
            log.error("SMPP IO error: channel={}, to={}, error={}",
                    request.getChannelCode(), request.getToNumber(), e.getMessage());
            return ChannelSendResult.builder()
                    .submitted(false)
                    .errorCode("SMPP_IO_ERROR")
                    .errorMessage("Connection error: " + e.getMessage())
                    .build();
        }
    }

    private String submitSm(SMPPSession session, ChannelSendRequest request,
                            Alphabet alphabet, byte[] messageBytes, byte[] udh,
                            TypeOfNumber destTon, NumberingPlanIndicator destNpi)
            throws PDUException, ResponseTimeoutException, InvalidResponseException,
            NegativeResponseException, IOException {

        ESMClass esmClass = udh != null
                ? new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.UDHI)
                : new ESMClass();
        byte[] payload = udh != null ? concat(udh, messageBytes) : messageBytes;

        // Determine source TON based on SID format
        TypeOfNumber sourceTon = isNumericSid(request.getSid())
                ? TypeOfNumber.INTERNATIONAL : TypeOfNumber.ALPHANUMERIC;

        SubmitSmResult result = session.submitShortMessage(
                "CMT",
                sourceTon,
                NumberingPlanIndicator.UNKNOWN,
                request.getSid(),
                destTon,
                destNpi,
                request.getToNumber(),
                esmClass,
                (byte) 0,
                (byte) 0,
                null,
                null,
                new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                (byte) 0,
                new GeneralDataCoding(alphabet),
                (byte) 0,
                payload
        );
        return result.getMessageId();
    }

    private String sendLongMessage(SMPPSession session, ChannelSendRequest request,
                                   Alphabet alphabet, byte[] fullMessageBytes,
                                   TypeOfNumber destTon, NumberingPlanIndicator destNpi)
            throws PDUException, ResponseTimeoutException, InvalidResponseException,
            NegativeResponseException, IOException {

        int maxSegmentSize = request.getEncoding() == 1 ? 134 : 153;
        int totalSegments = request.getSegments();
        int refNum = udhRefCounter.incrementAndGet() & 0xFF;

        String firstVendorMsgId = null;

        for (int i = 0; i < totalSegments; i++) {
            byte[] udh = new byte[]{
                    0x05, 0x00, 0x03,
                    (byte) refNum,
                    (byte) totalSegments,
                    (byte) (i + 1)
            };

            int start = i * maxSegmentSize;
            int end = Math.min(start + maxSegmentSize, fullMessageBytes.length);
            byte[] segmentBytes = new byte[end - start];
            System.arraycopy(fullMessageBytes, start, segmentBytes, 0, end - start);

            String msgId = submitSm(session, request, alphabet, segmentBytes, udh, destTon, destNpi);

            if (i == 0) {
                firstVendorMsgId = msgId;
            }

            log.debug("SMPP long msg segment {}/{} sent: vendorMsgId={}", i + 1, totalSegments, msgId);
        }

        return firstVendorMsgId;
    }

    private void acquireRateLimit(Channel channel) {
        if (channel.getTps() != null && channel.getTps() > 0) {
            RateLimiter limiter = rateLimiters.computeIfAbsent(channel.getId(),
                    id -> RateLimiter.create(channel.getTps()));
            limiter.acquire();
        }
    }

    private TypeOfNumber resolveTypeOfNumber(String ton) {
        if (ton == null) return TypeOfNumber.INTERNATIONAL;
        return switch (ton.toUpperCase()) {
            case "NATIONAL" -> TypeOfNumber.NATIONAL;
            case "UNKNOWN" -> TypeOfNumber.UNKNOWN;
            case "ALPHANUMERIC" -> TypeOfNumber.ALPHANUMERIC;
            case "ABBREVIATED" -> TypeOfNumber.ABBREVIATED;
            default -> TypeOfNumber.INTERNATIONAL;
        };
    }

    private NumberingPlanIndicator resolveNpi(String npi) {
        if (npi == null) return NumberingPlanIndicator.ISDN;
        return switch (npi.toUpperCase()) {
            case "UNKNOWN" -> NumberingPlanIndicator.UNKNOWN;
            case "NATIONAL" -> NumberingPlanIndicator.NATIONAL;
            case "PRIVATE" -> NumberingPlanIndicator.PRIVATE;
            default -> NumberingPlanIndicator.ISDN;
        };
    }

    private boolean isNumericSid(String sid) {
        if (sid == null || sid.isEmpty()) return false;
        for (char c : sid.toCharArray()) {
            if (c != '+' && !Character.isDigit(c)) return false;
        }
        return true;
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    @Override
    public String getName() {
        return "SmppChannelAdapter";
    }
}
