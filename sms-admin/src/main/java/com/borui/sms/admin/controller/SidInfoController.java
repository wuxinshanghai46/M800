package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.SidInfoService;
import com.borui.sms.admin.vo.req.SidInfoReq;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.SidInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SID管理")
@RestController
@RequestMapping("/v1/admin/sid")
@RequiredArgsConstructor
public class SidInfoController {

    private final SidInfoService sidInfoService;

    @Operation(summary = "SID列表")
    @GetMapping("/list")
    public R<PageResult<SidInfo>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String sidType,
            @RequestParam(required = false) String smsType) {
        return R.ok(sidInfoService.list(page, size, keyword, vendorId, countryCode, sidType, smsType));
    }

    @Operation(summary = "SID详情")
    @GetMapping("/{id}")
    public R<SidInfo> get(@PathVariable Long id) {
        return R.ok(sidInfoService.getById(id));
    }

    @Operation(summary = "新增SID")
    @PostMapping
    public R<SidInfo> create(@Valid @RequestBody SidInfoReq req) {
        return R.ok(sidInfoService.create(req));
    }

    @Operation(summary = "修改SID")
    @PutMapping("/{id}")
    public R<SidInfo> update(@PathVariable Long id, @Valid @RequestBody SidInfoReq req) {
        return R.ok(sidInfoService.update(id, req));
    }

    @Operation(summary = "删除SID")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sidInfoService.delete(id);
        return R.ok();
    }
}
