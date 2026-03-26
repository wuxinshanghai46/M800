package com.borui.sms.admin.service;

import com.borui.sms.admin.vo.resp.SendStatsResp;
import com.borui.sms.mapper.MessageMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistics service - MVP version using SQL GROUP BY.
 * Production version will use Redis real-time aggregation + stats tables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SqlSessionFactory sqlSessionFactory;

    /**
     * Country-level statistics
     */
    public List<SendStatsResp> statsByCountry(LocalDateTime start, LocalDateTime end) {
        return queryStats("country_code", start, end);
    }

    /**
     * Vendor-level statistics
     */
    public List<SendStatsResp> statsByVendor(LocalDateTime start, LocalDateTime end) {
        return queryStats("vendor_id", start, end);
    }

    /**
     * Channel-level statistics
     */
    public List<SendStatsResp> statsByChannel(LocalDateTime start, LocalDateTime end) {
        return queryStats("channel_id", start, end);
    }

    /**
     * Customer-level statistics
     */
    public List<SendStatsResp> statsByCustomer(LocalDateTime start, LocalDateTime end) {
        return queryStats("customer_id", start, end);
    }

    /**
     * Dashboard summary: today's totals
     */
    public Map<String, Object> dashboardSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> result = new HashMap<>();

        String sql = "SELECT " +
                "COUNT(*) as total_sent, " +
                "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as total_delivered, " +
                "SUM(CASE WHEN status IN (3,4,5,6) THEN 1 ELSE 0 END) as total_failed, " +
                "SUM(segments) as total_segments, " +
                "SUM(CASE WHEN status = 2 THEN price * segments ELSE 0 END) as total_revenue, " +
                "SUM(CASE WHEN status = 2 THEN cost * segments ELSE 0 END) as total_cost " +
                "FROM message WHERE created_at >= ? AND created_at <= ?";

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, todayStart);
            ps.setObject(2, now);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long sent = rs.getLong("total_sent");
                long delivered = rs.getLong("total_delivered");
                long failed = rs.getLong("total_failed");
                BigDecimal revenue = rs.getBigDecimal("total_revenue");
                BigDecimal cost = rs.getBigDecimal("total_cost");

                result.put("totalSent", sent);
                result.put("totalDelivered", delivered);
                result.put("totalFailed", failed);
                result.put("totalSegments", rs.getLong("total_segments"));
                result.put("totalRevenue", revenue != null ? revenue : BigDecimal.ZERO);
                result.put("totalCost", cost != null ? cost : BigDecimal.ZERO);
                result.put("totalProfit", revenue != null && cost != null ? revenue.subtract(cost) : BigDecimal.ZERO);
                result.put("deliveryRate", sent > 0 ? Math.round(delivered * 10000.0 / sent) / 100.0 : 0);
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    private List<SendStatsResp> queryStats(String groupByField, LocalDateTime start, LocalDateTime end) {
        List<SendStatsResp> results = new ArrayList<>();

        String sql = "SELECT " + groupByField + " as dim_value, " +
                "COUNT(*) as sent, " +
                "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as delivered, " +
                "SUM(CASE WHEN status IN (3,4,5,6) THEN 1 ELSE 0 END) as failed, " +
                "SUM(segments) as total_segments, " +
                "SUM(CASE WHEN status = 2 THEN price * segments ELSE 0 END) as revenue, " +
                "SUM(CASE WHEN status = 2 THEN cost * segments ELSE 0 END) as cost " +
                "FROM message WHERE created_at >= ? AND created_at <= ? " +
                "GROUP BY " + groupByField + " ORDER BY sent DESC";

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String dimValue = rs.getString("dim_value");
                long sent = rs.getLong("sent");
                long delivered = rs.getLong("delivered");
                BigDecimal revenue = rs.getBigDecimal("revenue");
                BigDecimal cost = rs.getBigDecimal("cost");
                if (revenue == null) revenue = BigDecimal.ZERO;
                if (cost == null) cost = BigDecimal.ZERO;

                results.add(SendStatsResp.builder()
                        .dimension(groupByField.replace("_", ""))
                        .dimensionValue(dimValue != null ? dimValue : "UNKNOWN")
                        .sent(sent)
                        .delivered(delivered)
                        .failed(rs.getLong("failed"))
                        .segments(rs.getLong("total_segments"))
                        .revenue(revenue.setScale(4, RoundingMode.HALF_UP))
                        .cost(cost.setScale(4, RoundingMode.HALF_UP))
                        .profit(revenue.subtract(cost).setScale(4, RoundingMode.HALF_UP))
                        .deliveryRate(sent > 0 ? Math.round(delivered * 10000.0 / sent) / 100.0 : 0)
                        .build());
            }
        } catch (Exception e) {
            log.error("queryStats[{}] error", groupByField, e);
        }

        return results;
    }
}
