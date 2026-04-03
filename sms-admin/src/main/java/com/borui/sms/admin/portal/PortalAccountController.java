package com.borui.sms.admin.portal;

import com.borui.sms.admin.security.PortalUser;
import com.borui.sms.admin.service.CustomerCallbackService;
import com.borui.sms.admin.vo.req.PortalChangePwdReq;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.CustomerApiCredential;
import com.borui.sms.common.domain.entity.CustomerCallbackConfig;
import com.borui.sms.common.domain.entity.OperationLog;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/portal/account")
@RequiredArgsConstructor
public class PortalAccountController {

    private final PortalService portalService;
    private final CustomerCallbackService customerCallbackService;

    // ===== 基本信息 =====

    @GetMapping("/info")
    public R<Map<String, Object>> info(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.getAccountInfo(user.getCustomerId()));
    }

    @PutMapping("/password")
    public R<Void> changePassword(@AuthenticationPrincipal PortalUser user,
                                   @Valid @RequestBody PortalChangePwdReq req) {
        portalService.changePassword(user.getCustomerId(), req);
        return R.ok();
    }

    // ===== API 凭证 =====

    @GetMapping("/credential")
    public R<CustomerApiCredential> credential(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.getCredential(user.getCustomerId()));
    }

    @PostMapping("/credential/regen-key")
    public R<CustomerApiCredential> regenKey(@AuthenticationPrincipal PortalUser user,
                                              @RequestBody RegenKeyReq req) {
        return R.ok(portalService.regenApiKey(user.getCustomerId(), req.getPassword()));
    }

    @PostMapping("/credential/ip")
    public R<Void> addIp(@AuthenticationPrincipal PortalUser user,
                          @RequestBody IpReq req) {
        portalService.addIp(user.getCustomerId(), req.getIp());
        return R.ok();
    }

    @DeleteMapping("/credential/ip")
    public R<Void> removeIp(@AuthenticationPrincipal PortalUser user,
                              @RequestParam String ip) {
        portalService.removeIp(user.getCustomerId(), ip);
        return R.ok();
    }

    // ===== 回调配置 =====

    @GetMapping("/callback")
    public R<CustomerCallbackConfig> getCallback(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.getCallbackConfig(user.getCustomerId()));
    }

    @PutMapping("/callback")
    public R<Void> saveCallback(@AuthenticationPrincipal PortalUser user,
                                 @RequestBody CustomerCallbackConfig req) {
        portalService.saveCallbackConfig(user.getCustomerId(), req);
        return R.ok();
    }

    @PostMapping("/callback/test")
    public R<Map<String, Object>> testCallback(@AuthenticationPrincipal PortalUser user,
                                                @RequestParam String type) {
        CustomerCallbackConfig config = portalService.getCallbackConfig(user.getCustomerId());
        String url = "dlr".equalsIgnoreCase(type) ? config.getDlrUrl() : config.getMoUrl();
        String secret = "dlr".equalsIgnoreCase(type) ? config.getDlrSecret() : config.getMoSecret();

        if (url == null || url.isBlank()) {
            throw new BizException(ErrorCode.OPERATION_FAILED,
                    "未配置 " + type.toUpperCase() + " 回调地址，请先保存配置");
        }

        Map<String, Object> result = customerCallbackService.testWebhook(url, secret, type);
        return R.ok(result);
    }

    // ===== 国家单价 =====

    @GetMapping("/country-prices")
    public R<java.util.List<Map<String, Object>>> countryPrices(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.getCountryPrices(user.getCustomerId()));
    }

    // ===== 操作日志 =====

    @GetMapping("/logs")
    public R<PageResult<OperationLog>> logs(@AuthenticationPrincipal PortalUser user,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return R.ok(portalService.opLogs(user.getCustomerId(), page, size));
    }

    // ===== Inner Request Classes =====

    @Data
    static class RegenKeyReq {
        private String password;
    }

    @Data
    static class IpReq {
        private String ip;
    }
}
