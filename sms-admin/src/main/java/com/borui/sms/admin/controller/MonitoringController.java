package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.MonitoringService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.AlertRecord;
import com.borui.sms.common.domain.entity.AlertRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "监控告警")
@RestController
@RequestMapping("/v1/admin/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    // ==================== Tab1: 实时看板 ====================

    @Operation(summary = "实时看板数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboardStats() {
        return R.ok(monitoringService.dashboardStats());
    }

    // ==================== Tab2: 告警中心 ====================

    @Operation(summary = "告警记录列表")
    @GetMapping("/alert/page")
    public R<PageResult<AlertRecord>> alertRecordPage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(monitoringService.alertRecordPage(status, severity, page, size));
    }

    @Operation(summary = "确认告警")
    @PutMapping("/alert/{id}/acknowledge")
    public R<Void> acknowledgeAlert(@PathVariable Long id,
                                     @RequestParam(defaultValue = "admin") String user) {
        monitoringService.acknowledgeAlert(id, user);
        return R.ok();
    }

    @Operation(summary = "解决告警")
    @PutMapping("/alert/{id}/resolve")
    public R<Void> resolveAlert(@PathVariable Long id) {
        monitoringService.resolveAlert(id);
        return R.ok();
    }

    // ==================== Tab3: 通道监控 ====================

    @Operation(summary = "通道监控列表")
    @GetMapping("/channel/list")
    public R<List<Map<String, Object>>> channelMonitorList() {
        return R.ok(monitoringService.channelMonitorList());
    }

    // ==================== Tab4: 告警规则 ====================

    @Operation(summary = "告警规则列表")
    @GetMapping("/alert-rule/list")
    public R<List<AlertRule>> listAlertRules() {
        return R.ok(monitoringService.listAlertRules());
    }

    @Operation(summary = "保存告警规则")
    @PostMapping("/alert-rule")
    public R<Void> saveAlertRule(@RequestBody AlertRule rule) {
        monitoringService.saveAlertRule(rule);
        return R.ok();
    }

    @Operation(summary = "切换告警规则状态")
    @PutMapping("/alert-rule/{id}/toggle")
    public R<Void> toggleAlertRule(@PathVariable Long id, @RequestParam Boolean active) {
        monitoringService.toggleAlertRule(id, active);
        return R.ok();
    }

    @Operation(summary = "删除告警规则")
    @DeleteMapping("/alert-rule/{id}")
    public R<Void> deleteAlertRule(@PathVariable Long id) {
        monitoringService.deleteAlertRule(id);
        return R.ok();
    }

    // ==================== Tab5: 队列监控 ====================

    @Operation(summary = "队列监控数据")
    @GetMapping("/queue/stats")
    public R<Map<String, Object>> queueStats() {
        return R.ok(monitoringService.queueStats());
    }
}
