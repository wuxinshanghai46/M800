package com.borui.sms.admin.smpp;

import com.borui.sms.common.domain.entity.Channel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SessionStateListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages SMPP sessions for all channels.
 * Each channel gets one session (MVP), expandable to connection pool later.
 * Handles bind, heartbeat (enquire_link), and auto-reconnect with exponential backoff.
 */
@Slf4j
@Component
public class SmppSessionManager {

    private final SmppDlrListener dlrListener;

    private final Map<Long, SmppSessionWrapper> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2,
            r -> { Thread t = new Thread(r, "smpp-mgr"); t.setDaemon(true); return t; });

    public SmppSessionManager(SmppDlrListener dlrListener) {
        this.dlrListener = dlrListener;
    }

    /**
     * Get or create an SMPP session for the given channel.
     * Thread-safe: uses computeIfAbsent + double-check on connection state.
     */
    public SMPPSession getSession(Channel channel) throws IOException {
        SmppSessionWrapper wrapper = sessions.computeIfAbsent(channel.getId(), id -> new SmppSessionWrapper(channel));

        if (wrapper.isConnected()) {
            return wrapper.getSession();
        }

        // Need to (re)connect
        synchronized (wrapper) {
            if (wrapper.isConnected()) {
                return wrapper.getSession();
            }
            return connect(wrapper);
        }
    }

    /**
     * Establish SMPP bind_transceiver connection.
     */
    private SMPPSession connect(SmppSessionWrapper wrapper) throws IOException {
        Channel channel = wrapper.getChannel();
        log.info("SMPP connecting: channel={}, host={}:{}, systemId={}",
                channel.getChannelCode(), channel.getSmppHost(), channel.getSmppPort(), channel.getSmppSystemId());

        SMPPSession session = new SMPPSession();

        // Set enquire_link timer (heartbeat)
        int enquireLinkInterval = channel.getSmppEnquireLinkInterval() != null ? channel.getSmppEnquireLinkInterval() : 30;
        session.setEnquireLinkTimer(enquireLinkInterval * 1000);

        // Set transaction timer (response timeout)
        session.setTransactionTimer(30000);

        // Register DLR listener (for deliver_sm callbacks)
        session.setMessageReceiverListener(dlrListener);

        // Add session state listener for auto-reconnect
        session.addSessionStateListener(new SmppStateListener(wrapper));

        // Bind using configured bind type (defaults to BIND_TRX)
        BindType bindType = resolveBindType(channel.getSmppBindType());
        session.connectAndBind(
                channel.getSmppHost(),
                channel.getSmppPort(),
                bindType,
                channel.getSmppSystemId(),
                channel.getSmppPassword(),
                channel.getSmppSystemType() != null ? channel.getSmppSystemType() : "",
                TypeOfNumber.INTERNATIONAL,
                NumberingPlanIndicator.ISDN,
                null  // address range
        );

        log.info("SMPP connected: channel={}, systemId={}", channel.getChannelCode(), channel.getSmppSystemId());

        wrapper.setSession(session);
        wrapper.resetReconnectDelay();
        return session;
    }

    /**
     * Schedule async reconnect with exponential backoff.
     */
    void scheduleReconnect(SmppSessionWrapper wrapper) {
        long delay = wrapper.getNextReconnectDelay();
        log.info("SMPP scheduling reconnect: channel={}, delay={}s",
                wrapper.getChannel().getChannelCode(), delay);

        scheduler.schedule(() -> {
            try {
                synchronized (wrapper) {
                    if (!wrapper.isConnected()) {
                        connect(wrapper);
                    }
                }
            } catch (IOException e) {
                log.warn("SMPP reconnect failed: channel={}, error={}, will retry",
                        wrapper.getChannel().getChannelCode(), e.getMessage());
                scheduleReconnect(wrapper);
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Disconnect a specific channel session.
     */
    public void disconnect(Long channelId) {
        SmppSessionWrapper wrapper = sessions.remove(channelId);
        if (wrapper != null) {
            wrapper.close();
            log.info("SMPP disconnected: channelId={}", channelId);
        }
    }

    /**
     * Disconnect all sessions on shutdown.
     */
    @PreDestroy
    public void shutdown() {
        log.info("SMPP shutting down {} sessions", sessions.size());
        sessions.values().forEach(SmppSessionWrapper::close);
        sessions.clear();
        scheduler.shutdown();
    }

    /**
     * Check if a channel has an active session.
     */
    public boolean isConnected(Long channelId) {
        SmppSessionWrapper wrapper = sessions.get(channelId);
        return wrapper != null && wrapper.isConnected();
    }

    /**
     * Test SMPP connection with a temporary session (does not affect managed sessions).
     */
    public Map<String, Object> testConnection(Channel channel) {
        Map<String, Object> result = new HashMap<>();
        SMPPSession testSession = new SMPPSession();
        testSession.setEnquireLinkTimer(10000);
        testSession.setTransactionTimer(10000);
        testSession.setMessageReceiverListener(dlrListener);
        try {
            BindType bindType = resolveBindType(channel.getSmppBindType());
            testSession.connectAndBind(
                    channel.getSmppHost(),
                    channel.getSmppPort(),
                    bindType,
                    channel.getSmppSystemId(),
                    channel.getSmppPassword(),
                    channel.getSmppSystemType() != null ? channel.getSmppSystemType() : "",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.ISDN,
                    null
            );
            String sessionId = testSession.getSessionId();
            testSession.unbindAndClose();
            result.put("success", true);
            result.put("errorCode", "");
            result.put("message", "连接成功 (sessionId=" + sessionId + ")");
        } catch (UnknownHostException e) {
            log.warn("SMPP test DNS failure: channel={}, host={}", channel.getChannelCode(), channel.getSmppHost());
            result.put("success", false);
            result.put("errorCode", "DNS_FAILURE");
            result.put("message", "域名解析失败: 无法解析服务器地址 \"" + channel.getSmppHost() + "\"");
            result.put("hint", "请检查 SMPP 服务器地址是否填写正确，或确认网络 DNS 是否正常");
        } catch (ConnectException e) {
            log.warn("SMPP test connection refused: channel={}, host={}:{}",
                    channel.getChannelCode(), channel.getSmppHost(), channel.getSmppPort());
            result.put("success", false);
            result.put("errorCode", "CONNECTION_REFUSED");
            result.put("message", "连接被拒绝: 服务器 " + channel.getSmppHost() + ":" + channel.getSmppPort() + " 不可达");
            result.put("hint", "请检查 SMPP 服务器地址和端口是否正确，以及防火墙是否允许该端口");
        } catch (SocketTimeoutException e) {
            log.warn("SMPP test timeout: channel={}, host={}:{}",
                    channel.getChannelCode(), channel.getSmppHost(), channel.getSmppPort());
            result.put("success", false);
            result.put("errorCode", "CONNECT_TIMEOUT");
            result.put("message", "连接超时: 服务器 " + channel.getSmppHost() + ":" + channel.getSmppPort() + " 无响应");
            result.put("hint", "服务器不可达或被防火墙拦截，请检查网络连通性及端口开放情况");
        } catch (IOException e) {
            log.warn("SMPP test IO error: channel={}, error={}", channel.getChannelCode(), e.getMessage());
            result.put("success", false);
            result.put("errorCode", "IO_ERROR");
            result.put("message", "连接失败: " + e.getMessage());
            result.put("hint", "请检查 SMPP 服务器配置是否正确");
        }
        return result;
    }

    private BindType resolveBindType(String bindTypeStr) {
        if ("BIND_TX".equals(bindTypeStr)) return BindType.BIND_TX;
        if ("BIND_RX".equals(bindTypeStr)) return BindType.BIND_RX;
        return BindType.BIND_TRX;
    }

    /**
     * Get connection status info for monitoring.
     */
    public Map<Long, Boolean> getConnectionStatus() {
        Map<Long, Boolean> status = new ConcurrentHashMap<>();
        sessions.forEach((id, wrapper) -> status.put(id, wrapper.isConnected()));
        return status;
    }

    /**
     * Session state listener that triggers reconnect on disconnect.
     */
    private class SmppStateListener implements SessionStateListener {
        private final SmppSessionWrapper wrapper;

        SmppStateListener(SmppSessionWrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public void onStateChange(org.jsmpp.extra.SessionState newState,
                                  org.jsmpp.extra.SessionState oldState,
                                  org.jsmpp.session.Session source) {
            log.info("SMPP state change: channel={}, {} -> {}",
                    wrapper.getChannel().getChannelCode(), oldState, newState);

            if (newState.equals(org.jsmpp.extra.SessionState.CLOSED)) {
                log.warn("SMPP session closed: channel={}, scheduling reconnect",
                        wrapper.getChannel().getChannelCode());
                scheduleReconnect(wrapper);
            }
        }
    }
}
