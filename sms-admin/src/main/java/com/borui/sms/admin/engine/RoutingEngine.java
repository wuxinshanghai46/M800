package com.borui.sms.admin.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.entity.ChannelGroupMember;
import com.borui.sms.common.domain.entity.RoutingStrategyAssignment;
import com.borui.sms.common.domain.entity.SidInfo;
import com.borui.sms.common.domain.enums.ChannelStatus;
import com.borui.sms.common.domain.enums.SmsAttribute;
import com.borui.sms.mapper.ChannelGroupMemberMapper;
import com.borui.sms.mapper.ChannelMapper;
import com.borui.sms.mapper.RoutingStrategyAssignmentMapper;
import com.borui.sms.mapper.SidInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Routing engine - two-layer routing:
 * Layer 1: Manual override (routing_strategy_assignment)
 * Layer 2: Auto-match by country_code on channel table
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingEngine {

    private final ChannelMapper channelMapper;
    private final SidInfoMapper sidInfoMapper;
    private final RoutingStrategyAssignmentMapper assignmentMapper;
    private final ChannelGroupMemberMapper groupMemberMapper;

    /**
     * Find best available channel for the given customer, country and SMS attribute.
     * Step 1: Check manual overrides (most specific first)
     * Step 2: Auto-match by country_code
     */
    public Channel selectChannel(Long customerId, String countryCode, Integer smsAttribute) {
        // Step 1: Check manual overrides
        Channel override = findOverrideChannel(customerId, countryCode, smsAttribute);
        if (override != null) {
            log.info("Route(override): customer={}, country={}, channel={}", customerId, countryCode, override.getChannelCode());
            return override;
        }

        // Step 2: Auto-match by country
        return autoMatchChannel(countryCode, smsAttribute);
    }

    /**
     * Backward-compatible method for existing SendEngine call.
     */
    public Channel selectChannel(String countryCode, Integer smsAttribute) {
        return selectChannel(null, countryCode, smsAttribute);
    }

    /**
     * Find override channel from routing_strategy_assignment.
     * Priority: customer+country > customer+all > global+country > global
     */
    private Channel findOverrideChannel(Long customerId, String countryCode, Integer smsAttribute) {
        LambdaQueryWrapper<RoutingStrategyAssignment> wrapper = new LambdaQueryWrapper<>();

        // customerId condition: match specific customer OR global (null)
        if (customerId != null) {
            wrapper.and(w -> w.eq(RoutingStrategyAssignment::getCustomerId, customerId)
                    .or().isNull(RoutingStrategyAssignment::getCustomerId));
        } else {
            wrapper.isNull(RoutingStrategyAssignment::getCustomerId);
        }

        // countryCode condition: match specific country OR all (null)
        wrapper.and(w -> w.eq(RoutingStrategyAssignment::getCountryCode, countryCode)
                .or().isNull(RoutingStrategyAssignment::getCountryCode));

        // Sort: customer-specific first (non-null before null), country-specific first
        wrapper.last("ORDER BY customer_id IS NULL ASC, country_code IS NULL ASC");

        List<RoutingStrategyAssignment> assignments = assignmentMapper.selectList(wrapper);

        for (RoutingStrategyAssignment a : assignments) {
            // smsAttribute filter: 0 or null means all types
            if (a.getSmsAttribute() != null && a.getSmsAttribute() != 0
                    && smsAttribute != null && !a.getSmsAttribute().equals(smsAttribute)) {
                continue;
            }

            Channel ch = resolveAssignmentToChannel(a, countryCode);
            if (ch != null) return ch;
        }
        return null;
    }

    /**
     * Resolve an assignment to an active channel.
     */
    private Channel resolveAssignmentToChannel(RoutingStrategyAssignment assignment, String countryCode) {
        // Direct channel
        if (assignment.getChannelId() != null) {
            Channel ch = channelMapper.selectById(assignment.getChannelId());
            if (ch != null && ch.getIsActive() && ChannelStatus.ONLINE.equals(ch.getStatus())
                    && countryCode.equals(ch.getCountryCode())) {
                return ch;
            }
            return null;
        }

        // Channel group: find members, pick active channel matching country
        if (assignment.getChannelGroupId() != null) {
            List<ChannelGroupMember> members = groupMemberMapper.selectList(
                    new LambdaQueryWrapper<ChannelGroupMember>()
                            .eq(ChannelGroupMember::getGroupId, assignment.getChannelGroupId())
                            .eq(ChannelGroupMember::getIsActive, true)
                            .orderByAsc(ChannelGroupMember::getPriority));

            for (ChannelGroupMember m : members) {
                Channel ch = channelMapper.selectById(m.getChannelId());
                if (ch != null && ch.getIsActive() && ChannelStatus.ONLINE.equals(ch.getStatus())
                        && countryCode.equals(ch.getCountryCode())) {
                    return ch;
                }
            }
        }
        return null;
    }

    /**
     * Auto-match: find active channels for the country, sorted by priority.
     */
    private Channel autoMatchChannel(String countryCode, Integer smsAttribute) {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<Channel>()
                .eq(Channel::getCountryCode, countryCode)
                .eq(Channel::getStatus, ChannelStatus.ONLINE)
                .eq(Channel::getIsActive, true);

        if (smsAttribute != null) {
            SmsAttribute attr = SmsAttribute.fromCode(smsAttribute);
            if (attr != null) {
                wrapper.and(w -> w
                        .eq(Channel::getSmsAttribute, attr)
                        .or().isNull(Channel::getSmsAttribute));
            }
        }

        wrapper.orderByAsc(Channel::getPriority);
        List<Channel> channels = channelMapper.selectList(wrapper);

        if (channels.isEmpty()) {
            log.warn("No available channel for country={}, attribute={}", countryCode, smsAttribute);
            return null;
        }

        Channel selected = channels.get(0);
        log.info("Route(auto): country={}, channel={}, vendor={}", countryCode, selected.getChannelCode(), selected.getVendorId());
        return selected;
    }

    /**
     * Find SID for the given country and vendor.
     */
    public SidInfo selectSid(String countryCode, Long vendorId, String preferredSid) {
        if (preferredSid != null && !preferredSid.isEmpty()) {
            SidInfo sid = sidInfoMapper.selectOne(new LambdaQueryWrapper<SidInfo>()
                    .eq(SidInfo::getSid, preferredSid)
                    .eq(SidInfo::getCountryCode, countryCode)
                    .eq(SidInfo::getVendorId, vendorId)
                    .eq(SidInfo::getIsActive, true));
            if (sid != null) return sid;
        }

        return sidInfoMapper.selectOne(new LambdaQueryWrapper<SidInfo>()
                .eq(SidInfo::getCountryCode, countryCode)
                .eq(SidInfo::getVendorId, vendorId)
                .eq(SidInfo::getIsActive, true)
                .last("LIMIT 1"));
    }
}
