package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.vo.req.BlacklistReq;
import com.borui.sms.admin.vo.req.SensitiveWordReq;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.Blacklist;
import com.borui.sms.common.domain.entity.RateLimitRule;
import com.borui.sms.common.domain.entity.SensitiveWord;
import com.borui.sms.mapper.BlacklistMapper;
import com.borui.sms.mapper.RateLimitRuleMapper;
import com.borui.sms.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final BlacklistMapper blacklistMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final RateLimitRuleMapper rateLimitRuleMapper;

    // ---- Blacklist ----

    public PageResult<Blacklist> listBlacklist(int page, int size, String type, String scope, String countryCode, String keyword) {
        Page<Blacklist> p = new Page<>(page, size);
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(type)) wrapper.eq(Blacklist::getType, type);
        if (StringUtils.isNotBlank(scope)) wrapper.eq(Blacklist::getScope, scope);
        if (StringUtils.isNotBlank(countryCode)) wrapper.eq(Blacklist::getCountryCode, countryCode);
        if (StringUtils.isNotBlank(keyword)) wrapper.like(Blacklist::getValue, keyword);
        wrapper.orderByDesc(Blacklist::getCreatedAt);
        blacklistMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Blacklist createBlacklist(BlacklistReq req) {
        Blacklist bl = new Blacklist();
        bl.setType(req.getType());
        bl.setValue(req.getValue());
        bl.setScope(req.getScope());
        bl.setCustomerId(req.getCustomerId());
        bl.setCountryCode(req.getCountryCode());
        bl.setReason(req.getReason());
        bl.setIsActive(req.getIsActive());
        blacklistMapper.insert(bl);
        return bl;
    }

    public Blacklist updateBlacklist(Long id, BlacklistReq req) {
        Blacklist bl = blacklistMapper.selectById(id);
        if (bl == null) throw new BizException(ErrorCode.NOT_FOUND);
        bl.setType(req.getType());
        bl.setValue(req.getValue());
        bl.setScope(req.getScope());
        bl.setCustomerId(req.getCustomerId());
        bl.setCountryCode(req.getCountryCode());
        bl.setReason(req.getReason());
        bl.setIsActive(req.getIsActive());
        blacklistMapper.updateById(bl);
        return bl;
    }

    public void deleteBlacklist(Long id) {
        blacklistMapper.deleteById(id);
    }

    // ---- Sensitive Word ----

    public PageResult<SensitiveWord> listSensitiveWords(int page, int size, String matchType, String action, String scope, String keyword) {
        Page<SensitiveWord> p = new Page<>(page, size);
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(matchType)) wrapper.eq(SensitiveWord::getMatchType, matchType);
        if (StringUtils.isNotBlank(action)) wrapper.eq(SensitiveWord::getAction, action);
        if (StringUtils.isNotBlank(scope)) wrapper.eq(SensitiveWord::getScope, scope);
        if (StringUtils.isNotBlank(keyword)) wrapper.like(SensitiveWord::getWord, keyword);
        wrapper.orderByDesc(SensitiveWord::getCreatedAt);
        sensitiveWordMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public SensitiveWord createSensitiveWord(SensitiveWordReq req) {
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(req.getWord());
        sw.setMatchType(req.getMatchType());
        sw.setAction(req.getAction());
        sw.setCategory(req.getCategory());
        sw.setScope(req.getScope());
        sw.setCustomerId(req.getCustomerId());
        sw.setCountryCode(req.getCountryCode());
        sw.setIsActive(req.getIsActive());
        sensitiveWordMapper.insert(sw);
        return sw;
    }

    public SensitiveWord updateSensitiveWord(Long id, SensitiveWordReq req) {
        SensitiveWord sw = sensitiveWordMapper.selectById(id);
        if (sw == null) throw new BizException(ErrorCode.NOT_FOUND);
        sw.setWord(req.getWord());
        sw.setMatchType(req.getMatchType());
        sw.setAction(req.getAction());
        sw.setCategory(req.getCategory());
        sw.setScope(req.getScope());
        sw.setCustomerId(req.getCustomerId());
        sw.setCountryCode(req.getCountryCode());
        sw.setIsActive(req.getIsActive());
        sensitiveWordMapper.updateById(sw);
        return sw;
    }

    public void deleteSensitiveWord(Long id) {
        sensitiveWordMapper.deleteById(id);
    }

    // ---- Rate Limit Rule ----

    public PageResult<RateLimitRule> listRateLimitRules(int page, int size) {
        Page<RateLimitRule> p = new Page<>(page, size);
        rateLimitRuleMapper.selectPage(p, new LambdaQueryWrapper<RateLimitRule>().orderByDesc(RateLimitRule::getCreatedAt));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public RateLimitRule createRateLimitRule(RateLimitRule rule) {
        rateLimitRuleMapper.insert(rule);
        return rule;
    }

    public RateLimitRule updateRateLimitRule(Long id, RateLimitRule rule) {
        RateLimitRule existing = rateLimitRuleMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        rule.setId(id);
        rateLimitRuleMapper.updateById(rule);
        return rule;
    }

    public void deleteRateLimitRule(Long id) {
        rateLimitRuleMapper.deleteById(id);
    }
}
