package com.borui.sms.admin.portal;

import com.borui.sms.admin.security.PortalUser;
import com.borui.sms.common.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/v1/portal/dashboard")
@RequiredArgsConstructor
public class PortalDashboardController {

    private final PortalService portalService;

    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.dashboardStats(user.getCustomerId()));
    }
}
