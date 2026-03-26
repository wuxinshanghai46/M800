package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.SystemConfigService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.SysRole;
import com.borui.sms.common.domain.entity.SysUser;
import com.borui.sms.common.domain.entity.SystemConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/v1/admin/system")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    // ==================== 配置管理 ====================

    @Operation(summary = "获取配置组")
    @GetMapping("/config/{group}")
    public R<Map<String, String>> getConfigByGroup(@PathVariable String group) {
        return R.ok(systemConfigService.getConfigMapByGroup(group));
    }

    @Operation(summary = "获取配置列表（含描述）")
    @GetMapping("/config/{group}/detail")
    public R<List<SystemConfig>> getConfigDetail(@PathVariable String group) {
        return R.ok(systemConfigService.getConfigByGroup(group));
    }

    @Operation(summary = "批量更新配置")
    @PutMapping("/config/{group}")
    public R<Void> batchUpdateConfig(@PathVariable String group, @RequestBody Map<String, String> configs) {
        systemConfigService.batchUpdateConfig(group, configs);
        return R.ok();
    }

    @Operation(summary = "更新单个配置")
    @PutMapping("/config/{group}/{key}")
    public R<Void> updateConfig(@PathVariable String group, @PathVariable String key,
                                 @RequestParam String value) {
        systemConfigService.updateConfig(group, key, value);
        return R.ok();
    }

    // ==================== 角色管理 ====================

    @Operation(summary = "角色列表")
    @GetMapping("/role/list")
    public R<List<SysRole>> listRoles() {
        return R.ok(systemConfigService.listRoles());
    }

    @Operation(summary = "保存角色")
    @PostMapping("/role")
    public R<Void> saveRole(@RequestBody SysRole role) {
        systemConfigService.saveRole(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/role/{id}")
    public R<Void> deleteRole(@PathVariable Long id) {
        systemConfigService.deleteRole(id);
        return R.ok();
    }

    // ==================== 用户管理 ====================

    @Operation(summary = "用户列表")
    @GetMapping("/user/page")
    public R<PageResult<SysUser>> userPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(systemConfigService.userPage(keyword, roleId, page, size));
    }

    @Operation(summary = "保存用户")
    @PostMapping("/user")
    public R<Void> saveUser(@RequestBody SysUser user) {
        systemConfigService.saveUser(user);
        return R.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/user/{id}/toggle")
    public R<Void> toggleUser(@PathVariable Long id, @RequestParam Boolean active) {
        systemConfigService.toggleUser(id, active);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/user/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        systemConfigService.resetPassword(id, password);
        return R.ok();
    }
}
