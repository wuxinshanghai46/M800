package com.borui.sms.admin.smpp;

import com.borui.sms.common.domain.entity.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.session.SMPPSession;

/**
 * Wraps an SMPPSession with reconnect state for a single channel.
 */
@Slf4j
@Getter
public class SmppSessionWrapper {

    private final Channel channel;
    @Setter
    private volatile SMPPSession session;

    // Reconnect backoff: 2s, 4s, 8s, 16s, 32s, 60s (max)
    private static final long INITIAL_DELAY = 2;
    private static final long MAX_DELAY = 60;
    private long reconnectDelay = INITIAL_DELAY;

    public SmppSessionWrapper(Channel channel) {
        this.channel = channel;
    }

    public boolean isConnected() {
        return session != null && session.getSessionState().isBound();
    }

    /**
     * Get next reconnect delay with exponential backoff.
     */
    public long getNextReconnectDelay() {
        long delay = reconnectDelay;
        reconnectDelay = Math.min(reconnectDelay * 2, MAX_DELAY);
        return delay;
    }

    public void resetReconnectDelay() {
        reconnectDelay = INITIAL_DELAY;
    }

    public void close() {
        if (session != null) {
            try {
                session.unbindAndClose();
            } catch (Exception e) {
                log.warn("Error closing SMPP session: channel={}, error={}",
                        channel.getChannelCode(), e.getMessage());
            }
            session = null;
        }
    }
}
