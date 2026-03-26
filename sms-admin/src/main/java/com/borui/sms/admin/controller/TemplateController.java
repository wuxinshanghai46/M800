package com.borui.sms.admin.controller;

import com.borui.sms.admin.security.LoginUser;
import com.borui.sms.admin.service.AuthService;
import com.borui.sms.admin.service.TemplateService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.SmsTemplate;
import com.borui.sms.common.domain.entity.TemplateVariable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "模板管理")
@RestController
@RequestMapping("/v1/admin/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final AuthService authService;

    // ==================== Tab1: 模板列表 ====================

    @Operation(summary = "模板列表")
    @GetMapping("/page")
    public R<PageResult<SmsTemplate>> templatePage(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(templateService.templatePage(customerId, templateType, status, keyword, page, size));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public R<SmsTemplate> getTemplate(@PathVariable Long id) {
        return R.ok(templateService.getTemplate(id));
    }

    @Operation(summary = "保存模板")
    @PostMapping
    public R<Void> saveTemplate(@RequestBody SmsTemplate template) {
        templateService.saveTemplate(template);
        return R.ok();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return R.ok();
    }

    // ==================== Tab2: 变量管理 ====================

    @Operation(summary = "变量列表")
    @GetMapping("/variable/list")
    public R<List<TemplateVariable>> listVariables() {
        return R.ok(templateService.listVariables());
    }

    @Operation(summary = "保存变量")
    @PostMapping("/variable")
    public R<Void> saveVariable(@RequestBody TemplateVariable variable) {
        templateService.saveVariable(variable);
        return R.ok();
    }

    @Operation(summary = "删除变量")
    @DeleteMapping("/variable/{id}")
    public R<Void> deleteVariable(@PathVariable Long id) {
        templateService.deleteVariable(id);
        return R.ok();
    }

    // ==================== Tab3: 审核队列 ====================

    @Operation(summary = "审核队列")
    @GetMapping("/review/queue")
    public R<PageResult<SmsTemplate>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(templateService.reviewQueue(page, size));
    }

    @Operation(summary = "审核通过")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPS_ADMIN')")
    @PutMapping("/{id}/approve")
    public R<Void> approveTemplate(@PathVariable Long id) {
        LoginUser loginUser = authService.getCurrentUser();
        templateService.approveTemplate(id, loginUser.getUsername());
        return R.ok();
    }

    @Operation(summary = "审核拒绝")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPS_ADMIN')")
    @PutMapping("/{id}/reject")
    public R<Void> rejectTemplate(@PathVariable Long id,
                                   @RequestParam(required = false) String reason) {
        LoginUser loginUser = authService.getCurrentUser();
        templateService.rejectTemplate(id, loginUser.getUsername(), reason);
        return R.ok();
    }

    @Operation(summary = "运营商审核通过")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPS_ADMIN')")
    @PutMapping("/{id}/carrier-approve")
    public R<Void> carrierApprove(@PathVariable Long id) {
        LoginUser loginUser = authService.getCurrentUser();
        templateService.approveCarrier(id, loginUser.getUsername());
        return R.ok();
    }

    @Operation(summary = "运营商审核拒绝")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPS_ADMIN')")
    @PutMapping("/{id}/carrier-reject")
    public R<Void> carrierReject(@PathVariable Long id,
                                  @RequestParam(required = false) String reason) {
        LoginUser loginUser = authService.getCurrentUser();
        templateService.rejectCarrier(id, loginUser.getUsername(), reason);
        return R.ok();
    }
}
