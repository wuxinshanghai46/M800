package com.borui.sms.admin.portal;

import com.borui.sms.admin.security.PortalUser;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.SmsTemplate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/portal/templates")
@RequiredArgsConstructor
public class PortalTemplateController {

    private final PortalService portalService;

    @GetMapping
    public R<PageResult<SmsTemplate>> list(
            @AuthenticationPrincipal PortalUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return R.ok(portalService.templateList(user.getCustomerId(), page, size, keyword, status));
    }

    @PostMapping
    public R<SmsTemplate> create(@AuthenticationPrincipal PortalUser user,
                                  @RequestBody TemplateReq req) {
        return R.ok(portalService.createTemplate(
                user.getCustomerId(), req.getTemplateName(), req.getTemplateType(), req.getContent()));
    }

    @PutMapping("/{id}")
    public R<SmsTemplate> update(@AuthenticationPrincipal PortalUser user,
                                  @PathVariable Long id,
                                  @RequestBody TemplateReq req) {
        return R.ok(portalService.updateTemplate(
                user.getCustomerId(), id, req.getTemplateName(), req.getTemplateType(), req.getContent()));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal PortalUser user,
                           @PathVariable Long id) {
        portalService.deleteTemplate(user.getCustomerId(), id);
        return R.ok();
    }

    @Data
    static class TemplateReq {
        private String templateName;
        private String templateType;   // OTP / TRANSACTIONAL / MARKETING / SERVICE / DYNAMIC
        private String content;
        private String remark;
    }
}
