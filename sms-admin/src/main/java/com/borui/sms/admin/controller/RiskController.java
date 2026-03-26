package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.RiskService;
import com.borui.sms.admin.vo.req.BlacklistReq;
import com.borui.sms.admin.vo.req.SensitiveWordReq;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Blacklist;
import com.borui.sms.common.domain.entity.RateLimitRule;
import com.borui.sms.common.domain.entity.SensitiveWord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "风控管理")
@RestController
@RequestMapping("/v1/admin/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    // ---- Blacklist ----

    @Operation(summary = "黑名单列表")
    @GetMapping("/blacklist/list")
    public R<PageResult<Blacklist>> listBlacklist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String keyword) {
        return R.ok(riskService.listBlacklist(page, size, type, scope, countryCode, keyword));
    }

    @Operation(summary = "新增黑名单")
    @PostMapping("/blacklist")
    public R<Blacklist> createBlacklist(@Valid @RequestBody BlacklistReq req) {
        return R.ok(riskService.createBlacklist(req));
    }

    @Operation(summary = "修改黑名单")
    @PutMapping("/blacklist/{id}")
    public R<Blacklist> updateBlacklist(@PathVariable Long id, @Valid @RequestBody BlacklistReq req) {
        return R.ok(riskService.updateBlacklist(id, req));
    }

    @Operation(summary = "删除黑名单")
    @DeleteMapping("/blacklist/{id}")
    public R<Void> deleteBlacklist(@PathVariable Long id) {
        riskService.deleteBlacklist(id);
        return R.ok();
    }

    // ---- Sensitive Word ----

    @Operation(summary = "敏感词列表")
    @GetMapping("/sensitive-word/list")
    public R<PageResult<SensitiveWord>> listSensitiveWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String matchType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String keyword) {
        return R.ok(riskService.listSensitiveWords(page, size, matchType, action, scope, keyword));
    }

    @Operation(summary = "新增敏感词")
    @PostMapping("/sensitive-word")
    public R<SensitiveWord> createSensitiveWord(@Valid @RequestBody SensitiveWordReq req) {
        return R.ok(riskService.createSensitiveWord(req));
    }

    @Operation(summary = "修改敏感词")
    @PutMapping("/sensitive-word/{id}")
    public R<SensitiveWord> updateSensitiveWord(@PathVariable Long id, @Valid @RequestBody SensitiveWordReq req) {
        return R.ok(riskService.updateSensitiveWord(id, req));
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/sensitive-word/{id}")
    public R<Void> deleteSensitiveWord(@PathVariable Long id) {
        riskService.deleteSensitiveWord(id);
        return R.ok();
    }

    // ---- Rate Limit Rule ----

    @Operation(summary = "频控规则列表")
    @GetMapping("/rate-limit/list")
    public R<PageResult<RateLimitRule>> listRateLimitRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(riskService.listRateLimitRules(page, size));
    }

    @Operation(summary = "新增频控规则")
    @PostMapping("/rate-limit")
    public R<RateLimitRule> createRateLimitRule(@RequestBody RateLimitRule rule) {
        return R.ok(riskService.createRateLimitRule(rule));
    }

    @Operation(summary = "修改频控规则")
    @PutMapping("/rate-limit/{id}")
    public R<RateLimitRule> updateRateLimitRule(@PathVariable Long id, @RequestBody RateLimitRule rule) {
        return R.ok(riskService.updateRateLimitRule(id, rule));
    }

    @Operation(summary = "删除频控规则")
    @DeleteMapping("/rate-limit/{id}")
    public R<Void> deleteRateLimitRule(@PathVariable Long id) {
        riskService.deleteRateLimitRule(id);
        return R.ok();
    }
}
