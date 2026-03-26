package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.LoginLog;
import com.borui.sms.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public PageResult<LoginLog> list(int page, int size,
                                     Long userId, String userType, String result,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        Page<LoginLog> p = new Page<>(page, size);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(LoginLog::getUserId, userId);
        if (StringUtils.isNotBlank(userType)) wrapper.eq(LoginLog::getUserType, userType);
        if (StringUtils.isNotBlank(result)) wrapper.eq(LoginLog::getResult, result);
        if (startTime != null) wrapper.ge(LoginLog::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(LoginLog::getCreatedAt, endTime);
        wrapper.orderByDesc(LoginLog::getCreatedAt);
        loginLogMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Async
    public void record(Long userId, String userType, String loginType,
                       String ipAddress, String userAgent,
                       String result, String failReason) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setUserType(userType);
            loginLog.setLoginType(loginType);
            loginLog.setIpAddress(StringUtils.defaultString(ipAddress));
            loginLog.setUserAgent(StringUtils.defaultString(userAgent));
            loginLog.setGeoLocation("");
            loginLog.setResult(result);
            loginLog.setFailReason(StringUtils.defaultString(failReason));
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("Failed to record login log for userId={}: {}", userId, e.getMessage());
        }
    }
}
