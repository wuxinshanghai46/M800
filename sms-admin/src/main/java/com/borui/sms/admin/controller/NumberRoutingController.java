package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.NumberRoutingService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.DefaultRoute;
import com.borui.sms.common.domain.entity.MnpCache;
import com.borui.sms.common.domain.entity.NumberFilterRule;
import com.borui.sms.common.domain.entity.NumberSegment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "号码路由")
@RestController
@RequestMapping("/v1/admin/number-routing")
@RequiredArgsConstructor
public class NumberRoutingController {

    private final NumberRoutingService numberRoutingService;

    // ==================== Tab1: 号段归属表 ====================

    @Operation(summary = "号段列表")
    @GetMapping("/segment/page")
    public R<PageResult<NumberSegment>> segmentPage(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String operator,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(numberRoutingService.segmentPage(countryCode, operator, page, size));
    }

    @Operation(summary = "新增号段")
    @PostMapping("/segment")
    public R<Void> addSegment(@RequestBody NumberSegment segment) {
        numberRoutingService.addSegment(segment);
        return R.ok();
    }

    @Operation(summary = "删除号段")
    @DeleteMapping("/segment/{id}")
    public R<Void> deleteSegment(@PathVariable Long id) {
        numberRoutingService.deleteSegment(id);
        return R.ok();
    }

    // ==================== Tab2: MNP 缓存表 ====================

    @Operation(summary = "MNP缓存列表")
    @GetMapping("/mnp/page")
    public R<PageResult<MnpCache>> mnpCachePage(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(numberRoutingService.mnpCachePage(countryCode, phoneNumber, page, size));
    }

    @Operation(summary = "清理过期MNP缓存")
    @DeleteMapping("/mnp/expired")
    public R<Void> clearExpiredMnpCache() {
        numberRoutingService.clearExpiredMnpCache();
        return R.ok();
    }

    // ==================== Tab3: 默认路由表 ====================

    @Operation(summary = "默认路由列表")
    @GetMapping("/route/list")
    public R<List<DefaultRoute>> listDefaultRoutes(
            @RequestParam(required = false) String countryCode) {
        return R.ok(numberRoutingService.listDefaultRoutes(countryCode));
    }

    @Operation(summary = "保存默认路由")
    @PostMapping("/route")
    public R<Void> saveDefaultRoute(@RequestBody DefaultRoute route) {
        numberRoutingService.saveDefaultRoute(route);
        return R.ok();
    }

    @Operation(summary = "删除默认路由")
    @DeleteMapping("/route/{id}")
    public R<Void> deleteDefaultRoute(@PathVariable Long id) {
        numberRoutingService.deleteDefaultRoute(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用路由")
    @PutMapping("/route/{id}/toggle")
    public R<Void> toggleDefaultRoute(@PathVariable Long id, @RequestParam Boolean active) {
        numberRoutingService.toggleDefaultRoute(id, active);
        return R.ok();
    }

    // ==================== Tab4/5: 号码过滤规则 ====================

    @Operation(summary = "号码过滤规则列表")
    @GetMapping("/filter/list")
    public R<List<NumberFilterRule>> listFilterRules(
            @RequestParam(required = false) String countryCode) {
        return R.ok(numberRoutingService.listFilterRules(countryCode));
    }

    @Operation(summary = "保存号码过滤规则")
    @PostMapping("/filter")
    public R<Void> saveFilterRule(@RequestBody NumberFilterRule rule) {
        numberRoutingService.saveFilterRule(rule);
        return R.ok();
    }

    @Operation(summary = "删除号码过滤规则")
    @DeleteMapping("/filter/{id}")
    public R<Void> deleteFilterRule(@PathVariable Long id) {
        numberRoutingService.deleteFilterRule(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用过滤规则")
    @PutMapping("/filter/{id}/toggle")
    public R<Void> toggleFilterRule(@PathVariable Long id, @RequestParam Boolean active) {
        numberRoutingService.toggleFilterRule(id, active);
        return R.ok();
    }
}
