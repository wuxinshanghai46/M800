package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.CountryService;
import com.borui.sms.admin.vo.req.CountryReq;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Country;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "国家管理")
@RestController
@RequestMapping("/v1/admin/country")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @Operation(summary = "国家列表")
    @GetMapping("/list")
    public R<PageResult<Country>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(countryService.list(page, size, keyword));
    }

    @Operation(summary = "全部启用国家（下拉）")
    @GetMapping("/all")
    public R<List<Map<String, String>>> all() {
        List<Map<String, String>> result = countryService.listAll().stream().map(c -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", c.getCountryCode());
            m.put("name", c.getCountryName());
            m.put("nameEn", c.getCountryNameEn());
            m.put("nameZh", c.getCountryName());
            m.put("phoneCode", c.getPhoneCode());
            return m;
        }).toList();
        return R.ok(result);
    }

    @Operation(summary = "国家详情")
    @GetMapping("/{id}")
    public R<Country> get(@PathVariable Long id) {
        return R.ok(countryService.getById(id));
    }

    @Operation(summary = "新增国家")
    @PostMapping
    public R<Country> create(@Valid @RequestBody CountryReq req) {
        return R.ok(countryService.create(req));
    }

    @Operation(summary = "修改国家")
    @PutMapping("/{id}")
    public R<Country> update(@PathVariable Long id, @Valid @RequestBody CountryReq req) {
        return R.ok(countryService.update(id, req));
    }

    @Operation(summary = "删除国家")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        countryService.delete(id);
        return R.ok();
    }
}
