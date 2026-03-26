package com.borui.sms.admin.portal;

import com.borui.sms.admin.security.PortalUser;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Bill;
import com.borui.sms.common.domain.entity.BillingTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/portal/billing")
@RequiredArgsConstructor
public class PortalBillingController {

    private final PortalService portalService;

    @GetMapping("/account")
    public R<Map<String, Object>> account(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.billingAccount(user.getCustomerId()));
    }

    @GetMapping("/transactions")
    public R<PageResult<BillingTransaction>> transactions(
            @AuthenticationPrincipal PortalUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(portalService.transactionList(user.getCustomerId(), page, size, startTime, endTime));
    }

    @GetMapping("/bills")
    public R<PageResult<Bill>> bills(
            @AuthenticationPrincipal PortalUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return R.ok(portalService.billList(user.getCustomerId(), page, size));
    }
}
