package com.borui.sms.admin.adapter;

import com.borui.sms.common.domain.entity.Channel;

/**
 * Unified channel adapter interface.
 * All protocol implementations (Mock / SMPP / HTTP) implement this interface.
 * SendEngine only depends on this — never on concrete protocol classes.
 */
public interface ChannelAdapter {

    /**
     * Submit message to channel.
     * @param channel the channel entity with protocol-specific config
     * @param request protocol-agnostic send request
     */
    ChannelSendResult send(Channel channel, ChannelSendRequest request);

    /** Adapter name for logging / identification. */
    String getName();

    /** Whether this adapter handles DLR asynchronously (true) or synchronously (false). */
    default boolean isAsyncDlr() {
        return true;
    }
}
