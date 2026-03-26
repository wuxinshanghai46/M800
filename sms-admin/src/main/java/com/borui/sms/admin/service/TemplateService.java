package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.SmsTemplate;
import com.borui.sms.common.domain.entity.TemplateVariable;
import com.borui.sms.mapper.SmsTemplateMapper;
import com.borui.sms.mapper.TemplateVariableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final SmsTemplateMapper smsTemplateMapper;
    private final TemplateVariableMapper templateVariableMapper;

    // ==================== Tab1: 模板列表 ====================

    public PageResult<SmsTemplate> templatePage(Long customerId, String templateType,
                                                 String status, String keyword,
                                                 int page, int size) {
        LambdaQueryWrapper<SmsTemplate> wrapper = new LambdaQueryWrapper<>();
        if (customerId != null) wrapper.eq(SmsTemplate::getCustomerId, customerId);
        if (templateType != null && !templateType.isBlank()) wrapper.eq(SmsTemplate::getTemplateType, templateType);
        if (status != null && !status.isBlank()) wrapper.eq(SmsTemplate::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SmsTemplate::getTemplateName, keyword)
                    .or().like(SmsTemplate::getContent, keyword));
        }
        wrapper.orderByDesc(SmsTemplate::getCreatedAt);
        Page<SmsTemplate> p = smsTemplateMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public SmsTemplate getTemplate(Long id) {
        return smsTemplateMapper.selectById(id);
    }

    public void saveTemplate(SmsTemplate template) {
        if (template.getId() != null) {
            smsTemplateMapper.updateById(template);
        } else {
            smsTemplateMapper.insert(template);
        }
    }

    public void deleteTemplate(Long id) {
        smsTemplateMapper.deleteById(id);
    }

    // ==================== Tab2: 变量管理 ====================

    public List<TemplateVariable> listVariables() {
        return templateVariableMapper.selectList(
                new LambdaQueryWrapper<TemplateVariable>().orderByAsc(TemplateVariable::getId));
    }

    public void saveVariable(TemplateVariable variable) {
        if (variable.getId() != null) {
            templateVariableMapper.updateById(variable);
        } else {
            templateVariableMapper.insert(variable);
        }
    }

    public void deleteVariable(Long id) {
        templateVariableMapper.deleteById(id);
    }

    // ==================== Tab3: 审核队列 ====================

    public PageResult<SmsTemplate> reviewQueue(int page, int size) {
        LambdaQueryWrapper<SmsTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsTemplate::getPlatformStatus, "pending");
        wrapper.orderByAsc(SmsTemplate::getCreatedAt);
        Page<SmsTemplate> p = smsTemplateMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void approveTemplate(Long id, String reviewer) {
        SmsTemplate template = smsTemplateMapper.selectById(id);
        if (template != null && "pending".equals(template.getPlatformStatus())) {
            template.setPlatformStatus("approved");
            template.setStatus("approved");
            template.setReviewer(reviewer);
            template.setReviewAt(LocalDateTime.now());
            smsTemplateMapper.updateById(template);
        }
    }

    public void rejectTemplate(Long id, String reviewer, String reason) {
        SmsTemplate template = smsTemplateMapper.selectById(id);
        if (template != null && "pending".equals(template.getPlatformStatus())) {
            template.setPlatformStatus("rejected");
            template.setStatus("rejected");
            template.setReviewer(reviewer);
            template.setReviewAt(LocalDateTime.now());
            template.setRejectReason(reason);
            smsTemplateMapper.updateById(template);
        }
    }

    public void approveCarrier(Long id, String reviewer) {
        SmsTemplate template = smsTemplateMapper.selectById(id);
        if (template != null && !"approved".equals(template.getCarrierStatus())) {
            template.setCarrierStatus("approved");
            smsTemplateMapper.updateById(template);
        }
    }

    public void rejectCarrier(Long id, String reviewer, String reason) {
        SmsTemplate template = smsTemplateMapper.selectById(id);
        if (template != null && !"rejected".equals(template.getCarrierStatus())) {
            template.setCarrierStatus("rejected");
            template.setRejectReason(reason);
            smsTemplateMapper.updateById(template);
        }
    }
}
