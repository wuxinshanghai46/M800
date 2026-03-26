package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.CustomerService;
import com.borui.sms.admin.vo.req.AccountOperateReq;
import com.borui.sms.admin.vo.req.CustomerCountryPriceReq;
import com.borui.sms.admin.vo.req.CustomerReq;
import com.borui.sms.admin.vo.resp.CustomerDetailResp;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/v1/admin/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ========== Basic Info ==========

    @Operation(summary = "客户列表")
    @GetMapping("/list")
    public R<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(customerService.list(page, size, keyword, status));
    }

    @Operation(summary = "客户详情（全部Tab数据）")
    @GetMapping("/{id}/detail")
    public R<CustomerDetailResp> getDetail(@PathVariable Long id) {
        return R.ok(customerService.getDetail(id));
    }

    @Operation(summary = "客户基本信息")
    @GetMapping("/{id}")
    public R<Customer> get(@PathVariable Long id) {
        return R.ok(customerService.getById(id));
    }

    @Operation(summary = "新增客户")
    @PostMapping
    public R<Customer> create(@Valid @RequestBody CustomerReq req) {
        return R.ok(customerService.create(req));
    }

    @Operation(summary = "修改客户")
    @PutMapping("/{id}")
    public R<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerReq req) {
        return R.ok(customerService.update(id, req));
    }

    @Operation(summary = "切换客户状态")
    @PutMapping("/{id}/status")
    public R<Customer> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return R.ok(customerService.updateStatus(id, status));
    }

    // ========== Account ==========

    @Operation(summary = "客户账户信息")
    @GetMapping("/{id}/account")
    public R<CustomerAccount> getAccount(@PathVariable Long id) {
        return R.ok(customerService.getAccount(id));
    }

    @Operation(summary = "充值")
    @PostMapping("/{id}/account/recharge")
    public R<CustomerAccount> recharge(@PathVariable Long id, @Valid @RequestBody AccountOperateReq req) {
        return R.ok(customerService.recharge(id, req));
    }

    @Operation(summary = "扣费")
    @PostMapping("/{id}/account/deduct")
    public R<CustomerAccount> deduct(@PathVariable Long id, @Valid @RequestBody AccountOperateReq req) {
        return R.ok(customerService.deduct(id, req));
    }

    @Operation(summary = "设置信用额度")
    @PutMapping("/{id}/account/credit-limit")
    public R<CustomerAccount> updateCreditLimit(@PathVariable Long id, @RequestParam BigDecimal creditLimit) {
        return R.ok(customerService.updateCreditLimit(id, creditLimit));
    }

    // ========== Country & Price ==========

    @Operation(summary = "客户开通国家列表")
    @GetMapping("/{id}/countries")
    public R<List<CustomerCountry>> listCountries(@PathVariable Long id) {
        return R.ok(customerService.listCountries(id));
    }

    @Operation(summary = "开通国家")
    @PostMapping("/{id}/countries")
    public R<CustomerCountry> enableCountry(@PathVariable Long id,
                                            @RequestParam String countryCode) {
        return R.ok(customerService.enableCountry(id, countryCode));
    }

    @Operation(summary = "关闭国家")
    @DeleteMapping("/{id}/countries/{countryCode}")
    public R<Void> disableCountry(@PathVariable Long id, @PathVariable String countryCode) {
        customerService.disableCountry(id, countryCode);
        return R.ok();
    }

    @Operation(summary = "客户报价列表")
    @GetMapping("/{id}/prices")
    public R<List<CustomerCountryPrice>> listPrices(@PathVariable Long id) {
        return R.ok(customerService.listPrices(id));
    }

    @Operation(summary = "设置报价（新增或更新）")
    @PostMapping("/{id}/prices")
    public R<CustomerCountryPrice> setPrice(@PathVariable Long id,
                                            @Valid @RequestBody CustomerCountryPriceReq req) {
        return R.ok(customerService.setPrice(id, req));
    }

    @Operation(summary = "删除报价")
    @DeleteMapping("/prices/{priceId}")
    public R<Void> deletePrice(@PathVariable Long priceId) {
        customerService.deletePrice(priceId);
        return R.ok();
    }

    // ========== API Credential ==========

    @Operation(summary = "凭证列表")
    @GetMapping("/{id}/credentials")
    public R<List<CustomerApiCredential>> listCredentials(@PathVariable Long id) {
        return R.ok(customerService.listCredentials(id));
    }

    @Operation(summary = "新建凭证")
    @PostMapping("/{id}/credentials")
    public R<CustomerApiCredential> createCredential(@PathVariable Long id) {
        return R.ok(customerService.createCredential(id));
    }

    @Operation(summary = "重置Secret")
    @PutMapping("/credentials/{credentialId}/reset-secret")
    public R<CustomerApiCredential> resetSecret(@PathVariable Long credentialId) {
        return R.ok(customerService.resetCredentialSecret(credentialId));
    }

    @Operation(summary = "启用/禁用凭证")
    @PutMapping("/credentials/{credentialId}/toggle")
    public R<Void> toggleCredential(@PathVariable Long credentialId, @RequestParam boolean active) {
        customerService.toggleCredential(credentialId, active);
        return R.ok();
    }

    @Operation(summary = "设置IP白名单")
    @PutMapping("/credentials/{credentialId}/ip-whitelist")
    public R<Void> updateIpWhitelist(@PathVariable Long credentialId, @RequestParam String ipWhitelist) {
        customerService.updateIpWhitelist(credentialId, ipWhitelist);
        return R.ok();
    }

    @Operation(summary = "删除凭证")
    @DeleteMapping("/credentials/{credentialId}")
    public R<Void> deleteCredential(@PathVariable Long credentialId) {
        customerService.deleteCredential(credentialId);
        return R.ok();
    }

    // ========== Customer SID ==========

    @Operation(summary = "客户已授权SID列表")
    @GetMapping("/{id}/sids")
    public R<List<SidInfo>> listCustomerSids(@PathVariable Long id) {
        return R.ok(customerService.listCustomerSids(id));
    }

    @Operation(summary = "授权SID给客户")
    @PostMapping("/{id}/sids")
    public R<CustomerSid> assignSid(@PathVariable Long id, @RequestParam Long sidId) {
        return R.ok(customerService.assignSid(id, sidId));
    }

    @Operation(summary = "取消SID授权")
    @DeleteMapping("/{id}/sids/{sidId}")
    public R<Void> unassignSid(@PathVariable Long id, @PathVariable Long sidId) {
        customerService.unassignSid(id, sidId);
        return R.ok();
    }
}
