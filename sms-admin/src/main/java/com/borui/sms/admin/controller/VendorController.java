package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.VendorService;
import com.borui.sms.admin.vo.req.VendorReq;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Vendor;
import com.borui.sms.common.domain.entity.VendorCountry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/v1/admin/vendor")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @Operation(summary = "供应商列表")
    @GetMapping("/list")
    public R<PageResult<Vendor>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(vendorService.list(page, size, keyword));
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/{id}")
    public R<Vendor> get(@PathVariable Long id) {
        return R.ok(vendorService.getById(id));
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    public R<Vendor> create(@Valid @RequestBody VendorReq req) {
        return R.ok(vendorService.create(req));
    }

    @Operation(summary = "修改供应商")
    @PutMapping("/{id}")
    public R<Vendor> update(@PathVariable Long id, @Valid @RequestBody VendorReq req) {
        return R.ok(vendorService.update(id, req));
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return R.ok();
    }

    @Operation(summary = "供应商覆盖国家列表")
    @GetMapping("/{id}/countries")
    public R<List<VendorCountry>> listCountries(@PathVariable Long id) {
        return R.ok(vendorService.listCountries(id));
    }

    @Operation(summary = "绑定国家")
    @PostMapping("/{id}/countries")
    public R<VendorCountry> bindCountry(@PathVariable Long id,
                                        @RequestParam String countryCode) {
        return R.ok(vendorService.bindCountry(id, countryCode));
    }

    @Operation(summary = "解绑国家")
    @DeleteMapping("/{id}/countries/{countryCode}")
    public R<Void> unbindCountry(@PathVariable Long id, @PathVariable String countryCode) {
        vendorService.unbindCountry(id, countryCode);
        return R.ok();
    }
}
