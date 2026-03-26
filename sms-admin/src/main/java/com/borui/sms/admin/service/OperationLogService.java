package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.OperationLog;
import com.borui.sms.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public PageResult<OperationLog> list(int page, int size,
                                         String module, String operatorName,
                                         String operatorType, String result,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        Page<OperationLog> p = new Page<>(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(module)) wrapper.eq(OperationLog::getModule, module);
        if (StringUtils.isNotBlank(operatorName)) wrapper.like(OperationLog::getOperatorName, operatorName);
        if (StringUtils.isNotBlank(operatorType)) wrapper.eq(OperationLog::getOperatorType, operatorType);
        if (StringUtils.isNotBlank(result)) wrapper.eq(OperationLog::getResult, result);
        if (startTime != null) wrapper.ge(OperationLog::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(OperationLog::getCreatedAt, endTime);
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        operationLogMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    /**
     * 记录操作日志（异步，不阻塞主流程）。
     *
     * @param operatorId   操作人 ID
     * @param operatorName 操作人姓名
     * @param operatorType admin / customer
     * @param module       模块名
     * @param action       动作名
     * @param targetType   目标类型
     * @param targetId     目标 ID
     * @param targetName   目标名称
     * @param summary      人可读摘要
     * @param beforeData   变更前 JSON（仅关键字段）
     * @param afterData    变更后 JSON
     * @param ip           来源 IP
     * @param userAgent    User-Agent
     * @param result       success / failed
     * @param errorMessage 失败原因（success 时传 null）
     */
    @Async
    public void log(Long operatorId, String operatorName, String operatorType,
                    String module, String action,
                    String targetType, String targetId, String targetName,
                    String summary, String beforeData, String afterData,
                    String ip, String userAgent,
                    String result, String errorMessage) {
        OperationLog record = new OperationLog();
        record.setOperatorId(operatorId);
        record.setOperatorName(StringUtils.defaultString(operatorName));
        record.setOperatorType(StringUtils.defaultString(operatorType, "admin"));
        record.setModule(module);
        record.setAction(action);
        record.setTargetType(StringUtils.defaultString(targetType));
        record.setTargetId(StringUtils.defaultString(targetId));
        record.setTargetName(StringUtils.defaultString(targetName));
        record.setSummary(StringUtils.defaultString(summary));
        record.setBeforeData(beforeData);
        record.setAfterData(afterData);
        record.setIp(StringUtils.defaultString(ip));
        record.setUserAgent(StringUtils.defaultString(userAgent));
        record.setResult(StringUtils.defaultString(result, "success"));
        record.setErrorMessage(StringUtils.defaultString(errorMessage));
        operationLogMapper.insert(record);
    }
}
