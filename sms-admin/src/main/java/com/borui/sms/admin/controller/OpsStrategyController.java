package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.OpsStrategyService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "运营策略")
@RestController
@RequestMapping("/v1/admin/ops-strategy")
@RequiredArgsConstructor
public class OpsStrategyController {

    private final OpsStrategyService opsStrategyService;

    // ==================== Tab1: 人工审核 ====================

    @Operation(summary = "审核规则列表")
    @GetMapping("/review-rule/list")
    public R<List<ReviewRule>> listReviewRules() {
        return R.ok(opsStrategyService.listReviewRules());
    }

    @Operation(summary = "保存审核规则")
    @PostMapping("/review-rule")
    public R<Void> saveReviewRule(@RequestBody ReviewRule rule) {
        opsStrategyService.saveReviewRule(rule);
        return R.ok();
    }

    @Operation(summary = "切换审核规则状态")
    @PutMapping("/review-rule/{id}/toggle")
    public R<Void> toggleReviewRule(@PathVariable Long id, @RequestParam Boolean active) {
        opsStrategyService.toggleReviewRule(id, active);
        return R.ok();
    }

    @Operation(summary = "审核工单列表")
    @GetMapping("/review-ticket/page")
    public R<PageResult<ReviewTicket>> reviewTicketPage(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(opsStrategyService.reviewTicketPage(status, page, size));
    }

    @Operation(summary = "审核工单（通过/拒绝）")
    @PutMapping("/review-ticket/{id}/review")
    public R<Void> reviewTicket(@PathVariable Long id,
                                 @RequestParam String action,
                                 @RequestParam(defaultValue = "admin") String reviewer) {
        opsStrategyService.reviewTicket(id, action, reviewer);
        return R.ok();
    }

    // ==================== Tab2: 路由策略 ====================

    @Operation(summary = "策略分配列表")
    @GetMapping("/routing-strategy/list")
    public R<List<RoutingStrategyAssignment>> listStrategyAssignments() {
        return R.ok(opsStrategyService.listStrategyAssignments());
    }

    @Operation(summary = "保存策略分配")
    @PostMapping("/routing-strategy")
    public R<Void> saveStrategyAssignment(@RequestBody RoutingStrategyAssignment assignment) {
        opsStrategyService.saveStrategyAssignment(assignment);
        return R.ok();
    }

    @Operation(summary = "删除策略分配")
    @DeleteMapping("/routing-strategy/{id}")
    public R<Void> deleteStrategyAssignment(@PathVariable Long id) {
        opsStrategyService.deleteStrategyAssignment(id);
        return R.ok();
    }

    // ==================== Tab3: 通道调度 ====================

    @Operation(summary = "通道组列表")
    @GetMapping("/channel-group/list")
    public R<List<ChannelGroup>> listChannelGroups() {
        return R.ok(opsStrategyService.listChannelGroups());
    }

    @Operation(summary = "保存通道组")
    @PostMapping("/channel-group")
    public R<Void> saveChannelGroup(@RequestBody ChannelGroup group) {
        opsStrategyService.saveChannelGroup(group);
        return R.ok();
    }

    @Operation(summary = "启用/禁用通道组")
    @PutMapping("/channel-group/{id}/toggle")
    public R<Void> toggleChannelGroup(@PathVariable Long id, @RequestParam Boolean active) {
        opsStrategyService.toggleChannelGroup(id, active);
        return R.ok();
    }

    @Operation(summary = "通道组成员列表")
    @GetMapping("/channel-group/{groupId}/members")
    public R<List<ChannelGroupMember>> listGroupMembers(@PathVariable Long groupId) {
        return R.ok(opsStrategyService.listGroupMembers(groupId));
    }

    @Operation(summary = "添加通道组成员")
    @PostMapping("/channel-group/{groupId}/member")
    public R<Void> addGroupMember(@PathVariable Long groupId, @RequestBody ChannelGroupMember member) {
        member.setGroupId(groupId);
        opsStrategyService.addGroupMember(member);
        return R.ok();
    }

    @Operation(summary = "更新通道组成员")
    @PutMapping("/channel-group/member/{memberId}")
    public R<Void> updateGroupMember(@PathVariable Long memberId, @RequestBody ChannelGroupMember member) {
        member.setId(memberId);
        opsStrategyService.updateGroupMember(member);
        return R.ok();
    }

    @Operation(summary = "移除通道组成员")
    @DeleteMapping("/channel-group/member/{memberId}")
    public R<Void> removeGroupMember(@PathVariable Long memberId) {
        opsStrategyService.removeGroupMember(memberId);
        return R.ok();
    }

    // ==================== Tab4: 模板替换 ====================

    @Operation(summary = "模板替换规则列表")
    @GetMapping("/template-replace/list")
    public R<List<TemplateReplaceRule>> listTemplateReplaceRules(
            @RequestParam(required = false) String triggerCondition,
            @RequestParam(required = false) Boolean active) {
        return R.ok(opsStrategyService.listTemplateReplaceRules(triggerCondition, active));
    }

    @Operation(summary = "保存模板替换规则")
    @PostMapping("/template-replace")
    public R<Void> saveTemplateReplaceRule(@RequestBody TemplateReplaceRule rule) {
        opsStrategyService.saveTemplateReplaceRule(rule);
        return R.ok();
    }

    @Operation(summary = "切换模板替换规则状态")
    @PutMapping("/template-replace/{id}/toggle")
    public R<Void> toggleTemplateReplaceRule(@PathVariable Long id, @RequestParam Boolean active) {
        opsStrategyService.toggleTemplateReplaceRule(id, active);
        return R.ok();
    }

    // ==================== Tab5: SID 替换 ====================

    @Operation(summary = "SID替换规则列表")
    @GetMapping("/sid-replace/list")
    public R<List<SidReplaceRule>> listSidReplaceRules() {
        return R.ok(opsStrategyService.listSidReplaceRules());
    }

    @Operation(summary = "保存SID替换规则")
    @PostMapping("/sid-replace")
    public R<Void> saveSidReplaceRule(@RequestBody SidReplaceRule rule) {
        opsStrategyService.saveSidReplaceRule(rule);
        return R.ok();
    }

    @Operation(summary = "切换SID替换规则状态")
    @PutMapping("/sid-replace/{id}/toggle")
    public R<Void> toggleSidReplaceRule(@PathVariable Long id, @RequestParam Boolean active) {
        opsStrategyService.toggleSidReplaceRule(id, active);
        return R.ok();
    }
}
