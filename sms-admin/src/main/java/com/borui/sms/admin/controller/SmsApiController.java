package com.borui.sms.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.admin.engine.SendEngine;
import com.borui.sms.admin.vo.req.SendSmsReq;
import com.borui.sms.admin.vo.resp.SendSmsResp;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.CustomerApiCredential;
import com.borui.sms.mapper.CustomerApiCredentialMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * External SMS API — the entry point for customer sending.
 * MVP: simple API key auth via header.
 * Production: Basic Auth / Bearer Token + IP whitelist.
 */
@Tag(name = "发送API")
@RestController
@RequestMapping("/v1/api/sms")
@RequiredArgsConstructor
public class SmsApiController {

    private final SendEngine sendEngine;
    private final CustomerApiCredentialMapper credentialMapper;

    @Operation(summary = "发送短信")
    @PostMapping("/send")
    public R<SendSmsResp> send(@RequestHeader("X-API-Key") String apiKey,
                               @Valid @RequestBody SendSmsReq req) {
        // Simple auth: look up API key
        CustomerApiCredential cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<CustomerApiCredential>()
                        .eq(CustomerApiCredential::getApiKey, apiKey)
                        .eq(CustomerApiCredential::getIsActive, true));
        if (cred == null) {
            throw new BizException(ErrorCode.AUTH_FAILED, "API Key无效");
        }

        SendSmsResp resp = sendEngine.send(cred.getCustomerId(), req);
        return R.ok(resp);
    }

    @Operation(summary = "运营后台测试发送")
    @PostMapping("/admin-send")
    public R<SendSmsResp> adminSend(@RequestParam Long customerId,
                                    @Valid @RequestBody SendSmsReq req) {
        SendSmsResp resp = sendEngine.send(customerId, req);
        return R.ok(resp);
    }
}
