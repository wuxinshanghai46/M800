package com.borui.sms.admin.portal;

import com.borui.sms.admin.vo.req.PortalLoginReq;
import com.borui.sms.admin.vo.resp.PortalLoginResp;
import com.borui.sms.common.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final PortalService portalService;

    @PostMapping("/login")
    public R<PortalLoginResp> login(@Valid @RequestBody PortalLoginReq req) {
        return R.ok(portalService.login(req));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}
