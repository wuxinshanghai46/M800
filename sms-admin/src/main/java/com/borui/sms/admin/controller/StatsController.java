package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.StatsService;
import com.borui.sms.admin.vo.resp.SendStatsResp;
import com.borui.sms.common.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "发送统计")
@RestController
@RequestMapping("/v1/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Dashboard概览（今日汇总）")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(statsService.dashboardSummary());
    }

    @Operation(summary = "国家统计")
    @GetMapping("/country")
    public R<List<SendStatsResp>> statsByCountry(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        LocalDateTime s = (start != null ? start : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime e = (end != null ? end.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay();
        return R.ok(statsService.statsByCountry(s, e));
    }

    @Operation(summary = "供应商统计")
    @GetMapping("/vendor")
    public R<List<SendStatsResp>> statsByVendor(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        LocalDateTime s = (start != null ? start : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime e = (end != null ? end.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay();
        return R.ok(statsService.statsByVendor(s, e));
    }

    @Operation(summary = "通道统计")
    @GetMapping("/channel")
    public R<List<SendStatsResp>> statsByChannel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        LocalDateTime s = (start != null ? start : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime e = (end != null ? end.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay();
        return R.ok(statsService.statsByChannel(s, e));
    }

    @Operation(summary = "客户统计")
    @GetMapping("/customer")
    public R<List<SendStatsResp>> statsByCustomer(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        LocalDateTime s = (start != null ? start : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime e = (end != null ? end.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay();
        return R.ok(statsService.statsByCustomer(s, e));
    }
}
