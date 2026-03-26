package com.borui.sms.admin.portal;

import com.borui.sms.admin.security.PortalUser;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.SidInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portal/sids")
@RequiredArgsConstructor
public class PortalSidController {

    private final PortalService portalService;

    @GetMapping
    public R<List<SidInfo>> list(@AuthenticationPrincipal PortalUser user) {
        return R.ok(portalService.sidList(user.getCustomerId()));
    }
}
