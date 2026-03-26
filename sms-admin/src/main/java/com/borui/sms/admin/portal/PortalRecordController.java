package com.borui.sms.admin.portal;

import com.borui.sms.admin.security.PortalUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Message;
import com.borui.sms.common.domain.entity.MoRecord;
import com.borui.sms.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/portal")
@RequiredArgsConstructor
public class PortalRecordController {

    private final PortalService portalService;
    private final MessageMapper messageMapper;

    @GetMapping("/messages/{messageId}")
    public R<Message> messageDetail(@AuthenticationPrincipal PortalUser user,
                                     @PathVariable String messageId) {
        Message msg = messageMapper.selectOne(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getMessageId, messageId)
                        .eq(Message::getCustomerId, user.getCustomerId()));
        return R.ok(msg);
    }

    @GetMapping("/messages")
    public R<PageResult<Message>> messageList(
            @AuthenticationPrincipal PortalUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(portalService.messageList(user.getCustomerId(), page, size,
                status, countryCode, keyword, startTime, endTime));
    }

    @GetMapping("/mo")
    public R<PageResult<MoRecord>> moList(
            @AuthenticationPrincipal PortalUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(portalService.moList(user.getCustomerId(), page, size, startTime, endTime));
    }
}
