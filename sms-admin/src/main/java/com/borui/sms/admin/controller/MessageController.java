package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.MessageService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "发送记录")
@RestController
@RequestMapping("/v1/admin/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "发送记录列表")
    @GetMapping("/list")
    public R<PageResult<Message>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(messageService.list(page, size, customerId, countryCode, vendorId, channelId, status, keyword, startTime, endTime));
    }

    @Operation(summary = "消息详情")
    @GetMapping("/{messageId}")
    public R<Message> get(@PathVariable String messageId) {
        return R.ok(messageService.getByMessageId(messageId));
    }
}
