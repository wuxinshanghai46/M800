package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.FinanceService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Bill;
import com.borui.sms.common.domain.entity.ExchangeRate;
import com.borui.sms.common.domain.entity.VendorSettlementPrice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "财务结算")
@RestController
@RequestMapping("/v1/admin/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    // ==================== Tab1: 利润报表 ====================

    @Operation(summary = "利润汇总")
    @GetMapping("/profit/summary")
    public R<Map<String, Object>> profitSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Long vendorId) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        return R.ok(financeService.profitSummary(start, end, customerId, countryCode, vendorId));
    }

    @Operation(summary = "利润报表明细（分页）")
    @GetMapping("/profit/report")
    public R<PageResult<Map<String, Object>>> profitReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        return R.ok(financeService.profitReport(start, end, customerId, countryCode, vendorId, granularity, page, size));
    }

    // ==================== Tab2: 账单管理 ====================

    @Operation(summary = "账单列表")
    @GetMapping("/bill/page")
    public R<PageResult<Bill>> billPage(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(financeService.billPage(customerId, status, page, size));
    }

    @Operation(summary = "生成账单")
    @PostMapping("/bill/generate")
    public R<Bill> generateBill(
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate periodStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate periodEnd) {
        return R.ok(financeService.generateBill(customerId, periodStart, periodEnd));
    }

    @Operation(summary = "发送账单（draft→issued）")
    @PutMapping("/bill/{id}/issue")
    public R<Void> issueBill(@PathVariable Long id) {
        financeService.issueBill(id);
        return R.ok();
    }

    @Operation(summary = "标记已付（issued→paid）")
    @PutMapping("/bill/{id}/pay")
    public R<Void> payBill(@PathVariable Long id) {
        financeService.payBill(id);
        return R.ok();
    }

    @Operation(summary = "账单详情")
    @GetMapping("/bill/{id}")
    public R<Bill> getBill(@PathVariable Long id) {
        return R.ok(financeService.getBill(id));
    }

    // ==================== Tab3: 客户账户 ====================

    @Operation(summary = "客户账户列表")
    @GetMapping("/account/list")
    public R<List<Map<String, Object>>> accountList(
            @RequestParam(required = false) String paymentType) {
        return R.ok(financeService.accountList(paymentType));
    }

    // ==================== Tab4: 供应商结算价 ====================

    @Operation(summary = "供应商结算价列表")
    @GetMapping("/vendor-price/page")
    public R<PageResult<VendorSettlementPrice>> vendorPricePage(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(financeService.vendorPricePage(vendorId, countryCode, page, size));
    }

    @Operation(summary = "新增供应商结算价")
    @PostMapping("/vendor-price")
    public R<Void> addVendorPrice(@RequestBody VendorSettlementPrice price) {
        financeService.addVendorPrice(price);
        return R.ok();
    }

    @Operation(summary = "编辑供应商结算价")
    @PutMapping("/vendor-price/{id}")
    public R<Void> updateVendorPrice(@PathVariable Long id, @RequestBody VendorSettlementPrice price) {
        price.setId(id);
        financeService.updateVendorPrice(price);
        return R.ok();
    }

    @Operation(summary = "删除供应商结算价")
    @DeleteMapping("/vendor-price/{id}")
    public R<Void> deleteVendorPrice(@PathVariable Long id) {
        financeService.deleteVendorPrice(id);
        return R.ok();
    }

    // ==================== Tab5: 汇率管理 ====================

    @Operation(summary = "汇率列表")
    @GetMapping("/exchange-rate/list")
    public R<List<ExchangeRate>> exchangeRateList() {
        return R.ok(financeService.exchangeRateList());
    }

    @Operation(summary = "新增汇率")
    @PostMapping("/exchange-rate")
    public R<Void> addExchangeRate(@RequestBody ExchangeRate rate) {
        financeService.addExchangeRate(rate);
        return R.ok();
    }

    @Operation(summary = "编辑汇率")
    @PutMapping("/exchange-rate/{id}")
    public R<Void> updateExchangeRate(@PathVariable Long id, @RequestBody ExchangeRate rate) {
        rate.setId(id);
        financeService.updateExchangeRate(rate);
        return R.ok();
    }

    @Operation(summary = "删除汇率")
    @DeleteMapping("/exchange-rate/{id}")
    public R<Void> deleteExchangeRate(@PathVariable Long id) {
        financeService.deleteExchangeRate(id);
        return R.ok();
    }
}
