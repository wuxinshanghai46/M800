package com.borui.sms.admin.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.common.domain.entity.Blacklist;
import com.borui.sms.common.domain.entity.Message;
import com.borui.sms.common.domain.entity.OptoutRecord;
import com.borui.sms.common.domain.entity.RateLimitRule;
import com.borui.sms.common.domain.entity.SensitiveWord;
import com.borui.sms.mapper.BlacklistMapper;
import com.borui.sms.mapper.MessageMapper;
import com.borui.sms.mapper.OptoutRecordMapper;
import com.borui.sms.mapper.RateLimitRuleMapper;
import com.borui.sms.mapper.SensitiveWordMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEngine {

    private final BlacklistMapper blacklistMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final RateLimitRuleMapper rateLimitRuleMapper;
    private final MessageMapper messageMapper;
    private final OptoutRecordMapper optoutRecordMapper;

    @Data
    public static class RiskResult {
        private boolean passed = true;
        private String blockReason;

        public static RiskResult pass() {
            return new RiskResult();
        }

        public static RiskResult block(String reason) {
            RiskResult r = new RiskResult();
            r.setPassed(false);
            r.setBlockReason(reason);
            return r;
        }
    }

    public RiskResult check(Long customerId, String toNumber, String content) {
        // 1. Blacklist check
        RiskResult blacklistResult = checkBlacklist(customerId, toNumber);
        if (!blacklistResult.isPassed()) return blacklistResult;

        // 2. Optout check (user has unsubscribed)
        RiskResult optoutResult = checkOptout(customerId, toNumber);
        if (!optoutResult.isPassed()) return optoutResult;

        // 3. Sensitive word check
        RiskResult sensitiveResult = checkSensitiveWords(content);
        if (!sensitiveResult.isPassed()) return sensitiveResult;

        // 4. Rate limit check (MVP: DB COUNT version)
        RiskResult rateResult = checkRateLimit(customerId, toNumber);
        if (!rateResult.isPassed()) return rateResult;

        return RiskResult.pass();
    }

    private RiskResult checkBlacklist(Long customerId, String toNumber) {
        // Check global number blacklist
        Long globalHit = blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getType, "NUMBER")
                .eq(Blacklist::getValue, toNumber)
                .eq(Blacklist::getIsActive, true)
                .and(w -> w.eq(Blacklist::getScope, "GLOBAL")
                        .or().eq(Blacklist::getCustomerId, customerId)));
        if (globalHit > 0) {
            log.warn("Blacklist hit: number={}", toNumber);
            return RiskResult.block("号码在黑名单中");
        }

        // Check prefix blacklist
        List<Blacklist> prefixes = blacklistMapper.selectList(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getType, "PREFIX")
                .eq(Blacklist::getIsActive, true));
        for (Blacklist prefix : prefixes) {
            if (toNumber.startsWith(prefix.getValue())) {
                log.warn("Blacklist prefix hit: number={}, prefix={}", toNumber, prefix.getValue());
                return RiskResult.block("号码前缀在黑名单中");
            }
        }

        return RiskResult.pass();
    }

    private RiskResult checkSensitiveWords(String content) {
        if (content == null || content.isEmpty()) return RiskResult.pass();

        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getIsActive, true)
                        .eq(SensitiveWord::getAction, "BLOCK"));

        String lowerContent = content.toLowerCase();
        for (SensitiveWord sw : words) {
            boolean hit = false;
            switch (sw.getMatchType()) {
                case "EXACT":
                    hit = lowerContent.equals(sw.getWord().toLowerCase());
                    break;
                case "CONTAINS":
                    hit = lowerContent.contains(sw.getWord().toLowerCase());
                    break;
                case "REGEX":
                    try {
                        hit = content.matches(sw.getWord());
                    } catch (Exception ignored) {}
                    break;
            }
            if (hit) {
                log.warn("Sensitive word hit: word={}, type={}", sw.getWord(), sw.getMatchType());
                return RiskResult.block("内容含敏感词: " + sw.getWord());
            }
        }

        return RiskResult.pass();
    }

    private RiskResult checkOptout(Long customerId, String toNumber) {
        // Check if the number has opted out (global or customer-specific)
        Long optoutHit = optoutRecordMapper.selectCount(new LambdaQueryWrapper<OptoutRecord>()
                .eq(OptoutRecord::getPhoneNumber, toNumber)
                .eq(OptoutRecord::getIsActive, true)
                .and(w -> w.isNull(OptoutRecord::getCustomerId)
                        .or().eq(OptoutRecord::getCustomerId, customerId)));
        if (optoutHit > 0) {
            log.warn("Optout hit: number={}, customerId={}", toNumber, customerId);
            return RiskResult.block("号码已退订");
        }
        return RiskResult.pass();
    }

    private RiskResult checkRateLimit(Long customerId, String toNumber) {
        // MVP: check rate limit by querying message table (production: Redis INCR)
        List<RateLimitRule> rules = rateLimitRuleMapper.selectList(
                new LambdaQueryWrapper<RateLimitRule>()
                        .eq(RateLimitRule::getIsActive, true)
                        .eq(RateLimitRule::getAction, "BLOCK")
                        .and(w -> w.isNull(RateLimitRule::getCustomerId)
                                .or().eq(RateLimitRule::getCustomerId, customerId)));

        for (RateLimitRule rule : rules) {
            LocalDateTime windowStart = LocalDateTime.now().minusSeconds(rule.getWindowSeconds());

            LambdaQueryWrapper<Message> countWrapper = new LambdaQueryWrapper<Message>()
                    .ge(Message::getCreatedAt, windowStart);

            switch (rule.getDimension()) {
                case "NUMBER":
                    countWrapper.eq(Message::getToNumber, toNumber);
                    break;
                case "CUSTOMER":
                    countWrapper.eq(Message::getCustomerId, customerId);
                    break;
                default:
                    continue;
            }

            Long count = messageMapper.selectCount(countWrapper);
            if (count >= rule.getMaxCount()) {
                log.warn("Rate limit hit: dimension={}, count={}, max={}", rule.getDimension(), count, rule.getMaxCount());
                return RiskResult.block("触发频控: " + rule.getDimension() + " " + count + "/" + rule.getMaxCount());
            }
        }

        return RiskResult.pass();
    }
}
