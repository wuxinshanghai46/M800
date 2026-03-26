package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class FinanceService {

    private final BillMapper billMapper;
    private final VendorSettlementPriceMapper vendorSettlementPriceMapper;
    private final ExchangeRateMapper exchangeRateMapper;
    private final CustomerMapper customerMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final MessageMapper messageMapper;
    private final SqlSessionFactory sqlSessionFactory;

    // ==================== Tab1: 利润报表 ====================

    public Map<String, Object> profitSummary(LocalDate start, LocalDate end,
                                              Long customerId, String countryCode, Long vendorId) {
        Map<String, Object> result = new HashMap<>();
        StringBuilder where = new StringBuilder("WHERE created_at >= ? AND created_at < ?");
        List<Object> params = new ArrayList<>(Arrays.asList(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay()));

        if (customerId != null) { where.append(" AND customer_id = ?"); params.add(customerId); }
        if (countryCode != null) { where.append(" AND country_code = ?"); params.add(countryCode); }
        if (vendorId != null) { where.append(" AND vendor_id = ?"); params.add(vendorId); }

        String sql = "SELECT COUNT(*) as total_msg, SUM(segments) as total_seg, " +
                "SUM(CASE WHEN status=2 THEN price*segments ELSE 0 END) as revenue, " +
                "SUM(CASE WHEN status=2 THEN cost*segments ELSE 0 END) as cost " +
                "FROM message " + where;

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal revenue = Optional.ofNullable(rs.getBigDecimal("revenue")).orElse(BigDecimal.ZERO);
                BigDecimal cost = Optional.ofNullable(rs.getBigDecimal("cost")).orElse(BigDecimal.ZERO);
                BigDecimal profit = revenue.subtract(cost);
                double profitRate = revenue.compareTo(BigDecimal.ZERO) > 0
                        ? profit.divide(revenue, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;

                result.put("totalRevenue", revenue.setScale(2, RoundingMode.HALF_UP));
                result.put("totalCost", cost.setScale(2, RoundingMode.HALF_UP));
                result.put("totalProfit", profit.setScale(2, RoundingMode.HALF_UP));
                result.put("profitRate", Math.round(profitRate * 100) / 100.0);
            }
        } catch (Exception e) {
            log.error("profitSummary error", e);
        }
        return result;
    }

    public PageResult<Map<String, Object>> profitReport(LocalDate start, LocalDate end,
                                                         Long customerId, String countryCode,
                                                         Long vendorId, String granularity,
                                                         int page, int size) {
        String dateExpr = switch (granularity != null ? granularity : "day") {
            case "week" -> "DATE_FORMAT(created_at, '%x-W%v')";
            case "month" -> "DATE_FORMAT(created_at, '%Y-%m')";
            default -> "DATE(created_at)";
        };

        StringBuilder where = new StringBuilder("WHERE created_at >= ? AND created_at < ?");
        List<Object> params = new ArrayList<>(Arrays.asList(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay()));

        if (customerId != null) { where.append(" AND customer_id = ?"); params.add(customerId); }
        if (countryCode != null) { where.append(" AND country_code = ?"); params.add(countryCode); }
        if (vendorId != null) { where.append(" AND vendor_id = ?"); params.add(vendorId); }

        // Count total groups
        String countSql = "SELECT COUNT(DISTINCT CONCAT(" + dateExpr + ",'-',customer_id,'-',country_code,'-',vendor_id)) FROM message " + where;

        String dataSql = "SELECT " + dateExpr + " as date_dim, customer_id, country_code, vendor_id, channel_id, " +
                "COUNT(*) as msg_count, SUM(segments) as seg_count, " +
                "SUM(CASE WHEN status=2 THEN price*segments ELSE 0 END) as revenue, " +
                "SUM(CASE WHEN status=2 THEN cost*segments ELSE 0 END) as cost " +
                "FROM message " + where +
                " GROUP BY date_dim, customer_id, country_code, vendor_id, channel_id" +
                " ORDER BY date_dim DESC, revenue DESC" +
                " LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = new ArrayList<>();
        long total = 0;

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection()) {

            // Get total
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) total = rs.getLong(1);
            }

            // Get data
            List<Object> dataParams = new ArrayList<>(params);
            dataParams.add(size);
            dataParams.add((page - 1) * size);
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                for (int i = 0; i < dataParams.size(); i++) ps.setObject(i + 1, dataParams.get(i));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", rs.getString("date_dim"));
                    row.put("customerId", rs.getLong("customer_id"));
                    row.put("countryCode", rs.getString("country_code"));
                    row.put("vendorId", rs.getLong("vendor_id"));
                    row.put("channelId", rs.getLong("channel_id"));
                    row.put("msgCount", rs.getInt("msg_count"));
                    row.put("segCount", rs.getInt("seg_count"));
                    BigDecimal revenue = Optional.ofNullable(rs.getBigDecimal("revenue")).orElse(BigDecimal.ZERO);
                    BigDecimal cost = Optional.ofNullable(rs.getBigDecimal("cost")).orElse(BigDecimal.ZERO);
                    BigDecimal profit = revenue.subtract(cost);
                    row.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP));
                    row.put("cost", cost.setScale(2, RoundingMode.HALF_UP));
                    row.put("profit", profit.setScale(2, RoundingMode.HALF_UP));
                    double profitRate = revenue.compareTo(BigDecimal.ZERO) > 0
                            ? profit.divide(revenue, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
                    row.put("profitRate", Math.round(profitRate * 100) / 100.0);
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            log.error("profitReport error", e);
        }

        return PageResult.of(rows, total, page, size);
    }

    // ==================== Tab2: 账单管理 ====================

    public PageResult<Bill> billPage(Long customerId, String status, int page, int size) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        if (customerId != null) wrapper.eq(Bill::getCustomerId, customerId);
        if (status != null && !status.isBlank()) wrapper.eq(Bill::getStatus, status);
        wrapper.orderByDesc(Bill::getCreatedAt);

        Page<Bill> p = billMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Bill generateBill(Long customerId, LocalDate periodStart, LocalDate periodEnd) {
        // Aggregate message data for this customer and period
        String sql = "SELECT COUNT(*) as msg_count, IFNULL(SUM(segments),0) as seg_count, " +
                "IFNULL(SUM(CASE WHEN status=2 THEN price*segments ELSE 0 END),0) as amount " +
                "FROM message WHERE customer_id = ? AND created_at >= ? AND created_at < ?";

        int msgCount = 0;
        int segCount = 0;
        BigDecimal amount = BigDecimal.ZERO;

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setObject(2, periodStart.atStartOfDay());
            ps.setObject(3, periodEnd.plusDays(1).atStartOfDay());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                msgCount = rs.getInt("msg_count");
                segCount = rs.getInt("seg_count");
                amount = Optional.ofNullable(rs.getBigDecimal("amount")).orElse(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.error("generateBill aggregate error", e);
        }

        Bill bill = new Bill();
        bill.setBillNo("INV-" + periodStart.toString().replace("-", "") + "-" + String.format("%03d", customerId));
        bill.setCustomerId(customerId);
        bill.setPeriodStart(periodStart);
        bill.setPeriodEnd(periodEnd);
        bill.setTotalMessages(msgCount);
        bill.setTotalSegments(segCount);
        bill.setAmount(amount.setScale(4, RoundingMode.HALF_UP));
        bill.setCurrency("USD");
        bill.setStatus("draft");
        billMapper.insert(bill);
        return bill;
    }

    @Transactional
    public void issueBill(Long billId) {
        Bill bill = billMapper.selectById(billId);
        if (bill != null && "draft".equals(bill.getStatus())) {
            bill.setStatus("issued");
            bill.setIssuedAt(LocalDateTime.now());
            billMapper.updateById(bill);
        }
    }

    @Transactional
    public void payBill(Long billId) {
        Bill bill = billMapper.selectById(billId);
        if (bill != null && "issued".equals(bill.getStatus())) {
            bill.setStatus("paid");
            bill.setPaidAt(LocalDateTime.now());
            billMapper.updateById(bill);
        }
    }

    public Bill getBill(Long billId) {
        return billMapper.selectById(billId);
    }

    // ==================== Tab3: 客户账户 ====================

    public List<Map<String, Object>> accountList(String paymentType) {
        List<Map<String, Object>> result = new ArrayList<>();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (paymentType != null && !paymentType.isBlank()) {
            wrapper.eq(Customer::getPaymentType, paymentType.equals("prepaid")
                    ? com.borui.sms.common.domain.enums.PaymentType.PREPAID
                    : com.borui.sms.common.domain.enums.PaymentType.POSTPAID);
        }
        wrapper.eq(Customer::getStatus, com.borui.sms.common.domain.enums.CustomerStatus.ACTIVE);
        List<Customer> customers = customerMapper.selectList(wrapper);

        for (Customer c : customers) {
            CustomerAccount account = customerAccountMapper.selectOne(
                    new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getCustomerId, c.getId()));
            if (account == null) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerId", c.getId());
            String displayName = (c.getCompanyName() != null && !c.getCompanyName().isBlank())
                    ? c.getCompanyName() : c.getCustomerName();
            row.put("customerName", displayName);
            row.put("paymentType", c.getPaymentType() != null ? c.getPaymentType().name().toLowerCase() : "prepaid");
            row.put("balance", account.getBalance());
            row.put("frozenAmount", account.getFrozenAmount());
            row.put("creditLimit", account.getCreditLimit());
            row.put("currency", account.getCurrency());
            result.add(row);
        }
        return result;
    }

    // ==================== Tab4: 供应商结算价 ====================

    public PageResult<VendorSettlementPrice> vendorPricePage(Long vendorId, String countryCode,
                                                              int page, int size) {
        LambdaQueryWrapper<VendorSettlementPrice> wrapper = new LambdaQueryWrapper<>();
        if (vendorId != null) wrapper.eq(VendorSettlementPrice::getVendorId, vendorId);
        if (countryCode != null && !countryCode.isBlank()) {
            wrapper.eq(VendorSettlementPrice::getCountryCode, countryCode);
        }
        wrapper.orderByDesc(VendorSettlementPrice::getEffectiveDate);

        Page<VendorSettlementPrice> p = vendorSettlementPriceMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void addVendorPrice(VendorSettlementPrice price) {
        vendorSettlementPriceMapper.insert(price);
    }

    public void updateVendorPrice(VendorSettlementPrice price) {
        vendorSettlementPriceMapper.updateById(price);
    }

    public void deleteVendorPrice(Long id) {
        vendorSettlementPriceMapper.deleteById(id);
    }

    // ==================== Tab5: 汇率管理 ====================

    public List<ExchangeRate> exchangeRateList() {
        return exchangeRateMapper.selectList(
                new LambdaQueryWrapper<ExchangeRate>().orderByDesc(ExchangeRate::getEffectiveDate));
    }

    public void addExchangeRate(ExchangeRate rate) {
        exchangeRateMapper.insert(rate);
    }

    public void updateExchangeRate(ExchangeRate rate) {
        exchangeRateMapper.updateById(rate);
    }

    public void deleteExchangeRate(Long id) {
        exchangeRateMapper.deleteById(id);
    }
}
