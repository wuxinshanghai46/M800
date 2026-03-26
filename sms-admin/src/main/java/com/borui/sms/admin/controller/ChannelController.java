package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.ChannelService;
import com.borui.sms.admin.vo.req.ChannelGroupReq;
import com.borui.sms.admin.vo.req.ChannelReq;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.entity.ChannelGroup;
import com.borui.sms.common.domain.entity.ChannelGroupMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "通道管理")
@RestController
@RequestMapping("/v1/admin/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    // ---- Channel ----

    @Operation(summary = "通道列表")
    @GetMapping("/list")
    public R<PageResult<Channel>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String countryCode) {
        return R.ok(channelService.list(page, size, keyword, vendorId, countryCode));
    }

    @Operation(summary = "通道详情")
    @GetMapping("/{id}")
    public R<Channel> get(@PathVariable Long id) {
        return R.ok(channelService.getById(id));
    }

    @Operation(summary = "新增通道")
    @PostMapping
    public R<Channel> create(@Valid @RequestBody ChannelReq req) {
        return R.ok(channelService.create(req));
    }

    @Operation(summary = "修改通道")
    @PutMapping("/{id}")
    public R<Channel> update(@PathVariable Long id, @Valid @RequestBody ChannelReq req) {
        return R.ok(channelService.update(id, req));
    }

    @Operation(summary = "删除通道")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return R.ok();
    }

    @Operation(summary = "测试 SMPP 连接")
    @PostMapping("/{id}/test-smpp")
    public R<Map<String, Object>> testSmpp(@PathVariable Long id) {
        return R.ok(channelService.testSmppConnection(id));
    }

    @Operation(summary = "测试 HTTP 通道连通性")
    @PostMapping("/{id}/test-http")
    public R<Map<String, Object>> testHttp(@PathVariable Long id) {
        return R.ok(channelService.testHttpConnection(id));
    }

    @Operation(summary = "SMPP 连接状态")
    @GetMapping("/smpp/status")
    public R<Map<Long, Boolean>> smppStatus() {
        return R.ok(channelService.getSmppConnectionStatus());
    }

    // ---- Channel Group ----

    @Operation(summary = "通道组列表")
    @GetMapping("/group/list")
    public R<PageResult<ChannelGroup>> listGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(channelService.listGroups(page, size));
    }

    @Operation(summary = "新增通道组")
    @PostMapping("/group")
    public R<ChannelGroup> createGroup(@Valid @RequestBody ChannelGroupReq req) {
        return R.ok(channelService.createGroup(req));
    }

    @Operation(summary = "修改通道组")
    @PutMapping("/group/{id}")
    public R<ChannelGroup> updateGroup(@PathVariable Long id, @Valid @RequestBody ChannelGroupReq req) {
        return R.ok(channelService.updateGroup(id, req));
    }

    @Operation(summary = "删除通道组")
    @DeleteMapping("/group/{id}")
    public R<Void> deleteGroup(@PathVariable Long id) {
        channelService.deleteGroup(id);
        return R.ok();
    }

    @Operation(summary = "通道组成员列表")
    @GetMapping("/group/{groupId}/members")
    public R<List<ChannelGroupMember>> listGroupMembers(@PathVariable Long groupId) {
        return R.ok(channelService.listGroupMembers(groupId));
    }

    @Operation(summary = "添加通道组成员")
    @PostMapping("/group/{groupId}/members")
    public R<ChannelGroupMember> addGroupMember(
            @PathVariable Long groupId,
            @RequestParam Long channelId,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Integer weight) {
        return R.ok(channelService.addGroupMember(groupId, channelId, priority, weight));
    }

    @Operation(summary = "移除通道组成员")
    @DeleteMapping("/group/{groupId}/members/{channelId}")
    public R<Void> removeGroupMember(@PathVariable Long groupId, @PathVariable Long channelId) {
        channelService.removeGroupMember(groupId, channelId);
        return R.ok();
    }
}
