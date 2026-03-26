package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpsStrategyService {

    private final ReviewRuleMapper reviewRuleMapper;
    private final ReviewTicketMapper reviewTicketMapper;
    private final RoutingStrategyAssignmentMapper strategyAssignmentMapper;
    private final ChannelGroupMapper channelGroupMapper;
    private final ChannelGroupMemberMapper channelGroupMemberMapper;
    private final TemplateReplaceRuleMapper templateReplaceRuleMapper;
    private final SidReplaceRuleMapper sidReplaceRuleMapper;

    // ==================== Tab1: 人工审核 ====================

    public List<ReviewRule> listReviewRules() {
        return reviewRuleMapper.selectList(new LambdaQueryWrapper<ReviewRule>().orderByAsc(ReviewRule::getId));
    }

    public void saveReviewRule(ReviewRule rule) {
        if (rule.getId() != null) {
            reviewRuleMapper.updateById(rule);
        } else {
            reviewRuleMapper.insert(rule);
        }
    }

    public void toggleReviewRule(Long id, Boolean active) {
        ReviewRule rule = reviewRuleMapper.selectById(id);
        if (rule != null) {
            rule.setIsActive(active);
            reviewRuleMapper.updateById(rule);
        }
    }

    public PageResult<ReviewTicket> reviewTicketPage(String status, int page, int size) {
        LambdaQueryWrapper<ReviewTicket> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(ReviewTicket::getStatus, status);
        wrapper.orderByDesc(ReviewTicket::getCreatedAt);
        Page<ReviewTicket> p = reviewTicketMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Transactional
    public void reviewTicket(Long id, String action, String reviewer) {
        ReviewTicket ticket = reviewTicketMapper.selectById(id);
        if (ticket != null && "pending".equals(ticket.getStatus())) {
            ticket.setStatus("approved".equals(action) ? "approved" : "rejected");
            ticket.setReviewer(reviewer);
            ticket.setReviewAt(LocalDateTime.now());
            reviewTicketMapper.updateById(ticket);
        }
    }

    // ==================== Tab2: 路由策略 ====================

    public List<RoutingStrategyAssignment> listStrategyAssignments() {
        return strategyAssignmentMapper.selectList(
                new LambdaQueryWrapper<RoutingStrategyAssignment>().orderByAsc(RoutingStrategyAssignment::getId));
    }

    public void saveStrategyAssignment(RoutingStrategyAssignment assignment) {
        if (assignment.getId() != null) {
            strategyAssignmentMapper.updateById(assignment);
        } else {
            strategyAssignmentMapper.insert(assignment);
        }
    }

    public void deleteStrategyAssignment(Long id) {
        strategyAssignmentMapper.deleteById(id);
    }

    // ==================== Tab3: 通道调度 ====================

    public List<ChannelGroup> listChannelGroups() {
        return channelGroupMapper.selectList(
                new LambdaQueryWrapper<ChannelGroup>().orderByAsc(ChannelGroup::getId));
    }

    public void saveChannelGroup(ChannelGroup group) {
        if (group.getId() != null) {
            channelGroupMapper.updateById(group);
        } else {
            channelGroupMapper.insert(group);
        }
    }

    public void toggleChannelGroup(Long id, Boolean active) {
        ChannelGroup group = channelGroupMapper.selectById(id);
        if (group != null) {
            group.setIsActive(active);
            channelGroupMapper.updateById(group);
        }
    }

    public List<ChannelGroupMember> listGroupMembers(Long groupId) {
        return channelGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChannelGroupMember>()
                        .eq(ChannelGroupMember::getGroupId, groupId)
                        .orderByAsc(ChannelGroupMember::getPriority));
    }

    public void addGroupMember(ChannelGroupMember member) {
        channelGroupMemberMapper.insert(member);
    }

    public void updateGroupMember(ChannelGroupMember member) {
        channelGroupMemberMapper.updateById(member);
    }

    public void removeGroupMember(Long memberId) {
        channelGroupMemberMapper.deleteById(memberId);
    }

    // ==================== Tab4: 模板替换 ====================

    public List<TemplateReplaceRule> listTemplateReplaceRules(String triggerCondition, Boolean active) {
        LambdaQueryWrapper<TemplateReplaceRule> wrapper = new LambdaQueryWrapper<>();
        if (triggerCondition != null && !triggerCondition.isBlank()) {
            wrapper.eq(TemplateReplaceRule::getTriggerCondition, triggerCondition);
        }
        if (active != null) wrapper.eq(TemplateReplaceRule::getIsActive, active);
        wrapper.orderByDesc(TemplateReplaceRule::getCreatedAt);
        return templateReplaceRuleMapper.selectList(wrapper);
    }

    public void saveTemplateReplaceRule(TemplateReplaceRule rule) {
        if (rule.getId() != null) {
            templateReplaceRuleMapper.updateById(rule);
        } else {
            templateReplaceRuleMapper.insert(rule);
        }
    }

    public void toggleTemplateReplaceRule(Long id, Boolean active) {
        TemplateReplaceRule rule = templateReplaceRuleMapper.selectById(id);
        if (rule != null) {
            rule.setIsActive(active);
            templateReplaceRuleMapper.updateById(rule);
        }
    }

    // ==================== Tab5: SID 替换 ====================

    public List<SidReplaceRule> listSidReplaceRules() {
        return sidReplaceRuleMapper.selectList(
                new LambdaQueryWrapper<SidReplaceRule>().orderByDesc(SidReplaceRule::getCreatedAt));
    }

    public void saveSidReplaceRule(SidReplaceRule rule) {
        if (rule.getId() != null) {
            sidReplaceRuleMapper.updateById(rule);
        } else {
            sidReplaceRuleMapper.insert(rule);
        }
    }

    public void toggleSidReplaceRule(Long id, Boolean active) {
        SidReplaceRule rule = sidReplaceRuleMapper.selectById(id);
        if (rule != null) {
            rule.setIsActive(active);
            sidReplaceRuleMapper.updateById(rule);
        }
    }
}
