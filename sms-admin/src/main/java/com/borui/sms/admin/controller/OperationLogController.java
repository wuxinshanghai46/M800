package com.borui.sms.admin.controller;

import com.borui.sms.admin.service.LoginLogService;
import com.borui.sms.admin.service.OperationLogService;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.common.result.R;
import com.borui.sms.common.domain.entity.LoginLog;
import com.borui.sms.common.domain.entity.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "审计日志")
@RestController
@RequestMapping("/v1/admin/audit")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final LoginLogService loginLogService;

    @Operation(summary = "操作日志列表")
    @GetMapping("/operation")
    public R<PageResult<OperationLog>> listOperation(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String operatorType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(operationLogService.list(page, size, module, operatorName, operatorType, result, startTime, endTime));
    }

    @Operation(summary = "登录日志列表")
    @GetMapping("/login")
    public R<PageResult<LoginLog>> listLogin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(loginLogService.list(page, size, userId, userType, result, startTime, endTime));
    }
}
