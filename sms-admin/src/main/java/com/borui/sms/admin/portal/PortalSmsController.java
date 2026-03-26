package com.borui.sms.admin.portal;

import com.borui.sms.admin.engine.SendEngine;
import com.borui.sms.admin.security.PortalUser;
import com.borui.sms.admin.vo.req.PortalSendBatchReq;
import com.borui.sms.admin.vo.req.SendSmsReq;
import com.borui.sms.admin.vo.resp.SendSmsResp;
import com.borui.sms.common.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/portal/sms")
@RequiredArgsConstructor
public class PortalSmsController {

    private final SendEngine sendEngine;

    @PostMapping("/send")
    public R<SendSmsResp> send(@AuthenticationPrincipal PortalUser user,
                               @Valid @RequestBody SendSmsReq req) {
        return R.ok(sendEngine.send(user.getCustomerId(), req));
    }

    @PostMapping("/send-batch")
    public R<Map<String, Object>> sendBatch(@AuthenticationPrincipal PortalUser user,
                                             @Valid @RequestBody PortalSendBatchReq req) {
        Long customerId = user.getCustomerId();
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        for (String number : req.getNumbers()) {
            SendSmsReq singleReq = new SendSmsReq();
            singleReq.setTo(number.trim());
            singleReq.setContent(req.getContent());
            singleReq.setSid(req.getSid());
            singleReq.setClientRef(req.getClientRef());
            singleReq.setSmsAttribute(req.getSmsAttribute());

            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("to", number.trim());
            try {
                SendSmsResp resp = sendEngine.send(customerId, singleReq);
                row.put("messageId", resp.getMessageId());
                row.put("status", resp.getStatus());
                success++;
            } catch (Exception e) {
                row.put("status", "FAILED");
                row.put("error", e.getMessage());
                failed++;
            }
            results.add(row);
        }

        return R.ok(Map.of(
                "total", req.getNumbers().size(),
                "success", success,
                "failed", failed,
                "items", results));
    }
}
