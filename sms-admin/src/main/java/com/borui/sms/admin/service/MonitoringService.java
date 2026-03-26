package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.dlr.DlrProcessor;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.AlertRecord;
import com.borui.sms.common.domain.entity.AlertRule;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.entity.Vendor;
import com.borui.sms.common.domain.enums.ChannelStatus;
import com.borui.sms.mapper.AlertRecordMapper;
import com.borui.sms.mapper.AlertRuleMapper;
import com.borui.sms.mapper.ChannelMapper;
import com.borui.sms.mapper.VendorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final ChannelMapper channelMapper;
    private final VendorMapper vendorMapper;
    private final SqlSessionFactory sqlSessionFactory;
    private final DlrProcessor dlrProcessor;

    // ==================== Tab1: 实时看板 ====================

    public Map<String, Object> dashboardStats() {
        Map<String, Object> result = new HashMap<>();

        // Alert counts by severity
        long criticalCount = alertRecordMapper.selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStatus, "active")
                        .eq(AlertRecord::getSeverity, "critical"));
        long warningCount = alertRecordMapper.selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStatus, "active")
                        .eq(AlertRecord::getSeverity, "warning"));
        long infoCount = alertRecordMapper.selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStatus, "active")
                        .eq(AlertRecord::getSeverity, "info"));

        result.put("criticalAlerts", criticalCount);
        result.put("warningAlerts", warningCount);
        result.put("infoAlerts", infoCount);
        result.put("totalActiveAlerts", criticalCount + warningCount + infoCount);

        // Today's message stats
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        String sql = "SELECT COUNT(*) as total, " +
                "SUM(CASE WHEN status=2 THEN 1 ELSE 0 END) as delivered, " +
                "SUM(CASE WHEN status IN(3,4,5) THEN 1 ELSE 0 END) as failed " +
                "FROM message WHERE created_at >= ?";
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, todayStart);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long total = rs.getLong("total");
                long delivered = rs.getLong("delivered");
                long failed = rs.getLong("failed");
                result.put("todayTotal", total);
                result.put("todayDelivered", delivered);
                result.put("todayFailed", failed);
                result.put("deliveryRate", total > 0
                        ? BigDecimal.valueOf(delivered * 100.0 / total).setScale(1, RoundingMode.HALF_UP) : 0);
            }
        } catch (Exception e) {
            log.error("dashboardStats error", e);
        }

        // Active channels
        long activeChannels = channelMapper.selectCount(
                new LambdaQueryWrapper<Channel>()
                        .eq(Channel::getIsActive, true)
                        .eq(Channel::getStatus, ChannelStatus.ONLINE));
        result.put("activeChannels", activeChannels);

        return result;
    }

    // ==================== Tab2: 告警中心 ====================

    public PageResult<AlertRecord> alertRecordPage(String status, String severity, int page, int size) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(AlertRecord::getStatus, status);
        if (severity != null && !severity.isBlank()) wrapper.eq(AlertRecord::getSeverity, severity);
        wrapper.orderByDesc(AlertRecord::getCreatedAt);
        Page<AlertRecord> p = alertRecordMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void acknowledgeAlert(Long id, String user) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record != null && "active".equals(record.getStatus())) {
            record.setStatus("acknowledged");
            record.setAcknowledgedBy(user);
            record.setAcknowledgedAt(LocalDateTime.now());
            alertRecordMapper.updateById(record);
        }
    }

    public void resolveAlert(Long id) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record != null && !"resolved".equals(record.getStatus())) {
            record.setStatus("resolved");
            record.setResolvedAt(LocalDateTime.now());
            alertRecordMapper.updateById(record);
        }
    }

    // ==================== Tab3: 通道监控 ====================

    public List<Map<String, Object>> channelMonitorList() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Channel> channels = channelMapper.selectList(
                new LambdaQueryWrapper<Channel>().orderByAsc(Channel::getId));

        // Get per-channel stats for today
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        String sql = "SELECT channel_id, COUNT(*) as total, " +
                "SUM(CASE WHEN status=2 THEN 1 ELSE 0 END) as delivered, " +
                "SUM(CASE WHEN status IN(3,4,5) THEN 1 ELSE 0 END) as failed " +
                "FROM message WHERE created_at >= ? GROUP BY channel_id";

        Map<Long, long[]> channelStats = new HashMap<>();
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, todayStart);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                channelStats.put(rs.getLong("channel_id"),
                        new long[]{rs.getLong("total"), rs.getLong("delivered"), rs.getLong("failed")});
            }
        } catch (Exception e) {
            log.error("channelMonitorList error", e);
        }

        // Build vendor name map
        Map<Long, String> vendorNameMap = new HashMap<>();
        List<Vendor> vendors = vendorMapper.selectList(null);
        for (Vendor v : vendors) {
            vendorNameMap.put(v.getId(), v.getVendorName());
        }

        for (Channel ch : channels) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("channelId", ch.getId());
            row.put("channelName", ch.getChannelName());
            row.put("channelCode", ch.getChannelCode());
            row.put("channelType", ch.getChannelType());
            row.put("vendorId", ch.getVendorId());
            row.put("vendorName", vendorNameMap.getOrDefault(ch.getVendorId(), ""));
            row.put("countryCode", ch.getCountryCode());
            row.put("isActive", ch.getIsActive());
            row.put("tps", ch.getTps());
            long[] stats = channelStats.getOrDefault(ch.getId(), new long[]{0, 0, 0});
            row.put("todayTotal", stats[0]);
            row.put("todayDelivered", stats[1]);
            row.put("todayFailed", stats[2]);
            row.put("deliveryRate", stats[0] > 0
                    ? BigDecimal.valueOf(stats[1] * 100.0 / stats[0]).setScale(1, RoundingMode.HALF_UP) : 0);
            result.add(row);
        }
        return result;
    }

    // ==================== Tab4: 告警规则 ====================

    public List<AlertRule> listAlertRules() {
        return alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().orderByAsc(AlertRule::getId));
    }

    public void saveAlertRule(AlertRule rule) {
        if (rule.getId() != null) {
            alertRuleMapper.updateById(rule);
        } else {
            alertRuleMapper.insert(rule);
        }
    }

    public void toggleAlertRule(Long id, Boolean active) {
        AlertRule rule = alertRuleMapper.selectById(id);
        if (rule != null) {
            rule.setIsActive(active);
            alertRuleMapper.updateById(rule);
        }
    }

    public void deleteAlertRule(Long id) {
        alertRuleMapper.deleteById(id);
    }

    // ==================== Tab5: 队列监控 ====================

    public Map<String, Object> queueStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("dlrQueueSize", dlrProcessor.getQueueSize());
        result.put("sendQueueSize", 0);       // async send queue — future enhancement
        result.put("callbackQueueSize", 0);   // callback retry queue — future enhancement
        result.put("deadLetterSize", 0);      // dead letter queue — future enhancement
        return result;
    }
}
