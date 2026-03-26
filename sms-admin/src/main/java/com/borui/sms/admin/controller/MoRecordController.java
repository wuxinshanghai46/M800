package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.MoRecordService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.MoRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "上行记录")
@RestController
@RequestMapping("/v1/admin/mo")
@RequiredArgsConstructor
public class MoRecordController {

    private final MoRecordService moRecordService;

    @Operation(summary = "上行记录列表")
    @GetMapping("/list")
    public R<PageResult<MoRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(moRecordService.list(page, size, countryCode, keyword, startTime, endTime));
    }
}
