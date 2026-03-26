package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.admin.dlr.DlrProcessor;
import com.borui.sms.common.domain.entity.AlertRecord;
import com.borui.sms.common.domain.entity.AlertRule;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.entity.CustomerAccount;
import com.borui.sms.common.domain.enums.ChannelStatus;
import com.borui.sms.mapper.AlertRecordMapper;
import com.borui.sms.mapper.AlertRuleMapper;
import com.borui.sms.mapper.ChannelMapper;
import com.borui.sms.mapper.CustomerAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically evaluates active alert rules and creates AlertRecord entries when thresholds are breached.
 *
 * Supported rule types:
 *   - DELIVERY_RATE_DROP  : delivery rate in last N minutes < threshold%
 *   - HIGH_FAILURE_RATE   : failure rate in last N minutes > threshold%
 *   - CHANNEL_OFFLINE     : any active channel is not ONLINE
 *   - DLR_QUEUE_OVERFLOW  : DLR queue size > threshold
 *   - BALANCE_LOW         : any customer balance < threshold (USD)
 *
 * The conditionExpr field format:  window_minutes=5   (optional, default 5)
 * The threshold field format:      numeric value (e.g. "90" for 90% delivery rate)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationScheduler {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final ChannelMapper channelMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final DlrProcessor dlrProcessor;
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * Evaluate all active alert rules every 5 minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void evaluate() {
        List<AlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getIsActive, true));

        for (AlertRule rule : rules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("Alert rule evaluation error: ruleId={}, ruleName={}, error={}",
                        rule.getId(), rule.getRuleName(), e.getMessage());
            }
        }
    }

    private void evaluateRule(AlertRule rule) {
        String ruleType = rule.getRuleType();
        if (ruleType == null) return;

        switch (ruleType.toUpperCase()) {
            case "DELIVERY_RATE_DROP" -> checkDeliveryRateDrop(rule);
            case "HIGH_FAILURE_RATE"  -> checkHighFailureRate(rule);
            case "CHANNEL_OFFLINE"    -> checkChannelOffline(rule);
            case "DLR_QUEUE_OVERFLOW" -> checkDlrQueueOverflow(rule);
            case "BALANCE_LOW"        -> checkBalanceLow(rule);
            default -> log.debug("Unknown alert rule type: {}", ruleType);
        }
    }

    // ===== Rule evaluators =====

    private void checkDeliveryRateDrop(AlertRule rule) {
        int windowMinutes = parseWindowMinutes(rule.getConditionExpr(), 5);
        double threshold = parseThreshold(rule.getThreshold(), 90.0);
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        long[] stats = queryMessageStats(since);
        long total = stats[0], delivered = stats[1];

        if (total < 10) return; // not enough data

        double deliveryRate = (double) delivered / total * 100;
        if (deliveryRate < threshold) {
            fireAlert(rule,
                    String.format("投递率下降: %.1f%% (阈值: %.0f%%, 窗口: %d分钟, 总量: %d)",
                            deliveryRate, threshold, windowMinutes, total));
        }
    }

    private void checkHighFailureRate(AlertRule rule) {
        int windowMinutes = parseWindowMinutes(rule.getConditionExpr(), 5);
        double threshold = parseThreshold(rule.getThreshold(), 20.0);
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        long[] stats = queryMessageStats(since);
        long total = stats[0], failed = stats[2];

        if (total < 10) return;

        double failureRate = (double) failed / total * 100;
        if (failureRate > threshold) {
            fireAlert(rule,
                    String.format("失败率过高: %.1f%% (阈值: %.0f%%, 窗口: %d分钟, 总量: %d)",
                            failureRate, threshold, windowMinutes, total));
        }
    }

    private void checkChannelOffline(AlertRule rule) {
        List<Channel> offlineChannels = channelMapper.selectList(
                new LambdaQueryWrapper<Channel>()
                        .eq(Channel::getIsActive, true)
                        .ne(Channel::getStatus, ChannelStatus.ONLINE));

        if (!offlineChannels.isEmpty()) {
            String names = offlineChannels.stream()
                    .map(c -> c.getChannelName() + "(" + c.getStatus().name() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("");
            fireAlert(rule, "通道离线: " + names);
        }
    }

    private void checkDlrQueueOverflow(AlertRule rule) {
        int threshold = (int) parseThreshold(rule.getThreshold(), 500.0);
        int queueSize = dlrProcessor.getQueueSize();

        if (queueSize > threshold) {
            fireAlert(rule,
                    String.format("DLR 队列积压: %d 条 (阈值: %d)", queueSize, threshold));
        }
    }

    private void checkBalanceLow(AlertRule rule) {
        double threshold = parseThreshold(rule.getThreshold(), 10.0);
        BigDecimal thresholdBD = BigDecimal.valueOf(threshold);

        List<CustomerAccount> accounts = customerAccountMapper.selectList(
                new LambdaQueryWrapper<CustomerAccount>()
                        .lt(CustomerAccount::getBalance, thresholdBD)
                        .gt(CustomerAccount::getBalance, BigDecimal.ZERO));

        if (!accounts.isEmpty()) {
            fireAlert(rule,
                    String.format("%d 个客户账户余额低于 %.2f USD", accounts.size(), threshold));
        }
    }

    // ===== Helpers =====

    /**
     * Fire an alert. Deduplicates: skip if an 'active' alert for the same rule already exists.
     */
    private void fireAlert(AlertRule rule, String content) {
        Long activeCount = alertRecordMapper.selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getRuleId, rule.getId())
                        .eq(AlertRecord::getStatus, "active"));

        if (activeCount > 0) {
            log.debug("Alert already active for rule {}, skipping", rule.getId());
            return;
        }

        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setSeverity(rule.getSeverity() != null ? rule.getSeverity() : "warning");
        record.setTitle(rule.getRuleName());
        record.setContent(content);
        record.setStatus("active");
        alertRecordMapper.insert(record);

        log.warn("ALERT fired: rule='{}', severity={}, content={}", rule.getRuleName(), record.getSeverity(), content);
    }

    /** Returns [total, delivered, failed] for messages created since the given time. */
    private long[] queryMessageStats(LocalDateTime since) {
        String sql = "SELECT COUNT(*) as total, " +
                "SUM(CASE WHEN status=2 THEN 1 ELSE 0 END) as delivered, " +
                "SUM(CASE WHEN status IN(3,4,5,6) THEN 1 ELSE 0 END) as failed " +
                "FROM message WHERE created_at >= ?";
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, since);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new long[]{rs.getLong("total"), rs.getLong("delivered"), rs.getLong("failed")};
            }
        } catch (Exception e) {
            log.error("queryMessageStats error", e);
        }
        return new long[]{0, 0, 0};
    }

    /** Parse window_minutes=N from conditionExpr, defaulting to provided value. */
    private int parseWindowMinutes(String expr, int defaultValue) {
        if (expr == null) return defaultValue;
        for (String part : expr.split("[,;\\s]+")) {
            if (part.startsWith("window_minutes=")) {
                try { return Integer.parseInt(part.substring("window_minutes=".length()).trim()); }
                catch (NumberFormatException ignored) {}
            }
        }
        return defaultValue;
    }

    private double parseThreshold(String threshold, double defaultValue) {
        if (threshold == null || threshold.isBlank()) return defaultValue;
        try { return Double.parseDouble(threshold.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}
