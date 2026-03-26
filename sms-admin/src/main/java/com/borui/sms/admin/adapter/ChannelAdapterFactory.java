package com.borui.sms.admin.adapter;

import com.borui.sms.admin.smpp.SmppChannelAdapter;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory to select the correct ChannelAdapter based on channel type.
 * channelType: SMPP(1), HTTP(2).
 * Channel entity is passed as parameter to adapter.send() — no ThreadLocal needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelAdapterFactory {

    private final ChannelAdapter mockChannelAdapter;
    private final SmppChannelAdapter smppChannelAdapter;
    private final HttpChannelAdapter httpChannelAdapter;

    public ChannelAdapter getAdapter(Channel channel) {
        ChannelAdapter adapter;

        if (channel.getChannelType() == ChannelType.SMPP) {
            adapter = smppChannelAdapter;
        } else if (channel.getChannelType() == ChannelType.HTTP) {
            adapter = httpChannelAdapter;
        } else {
            adapter = mockChannelAdapter;
        }

        log.debug("getAdapter: channel={}, type={}, using {}",
                channel.getChannelCode(), channel.getChannelType(), adapter.getName());
        return adapter;
    }
}
