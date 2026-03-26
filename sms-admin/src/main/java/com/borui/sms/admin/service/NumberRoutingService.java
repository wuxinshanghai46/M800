package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.DefaultRoute;
import com.borui.sms.common.domain.entity.MnpCache;
import com.borui.sms.common.domain.entity.NumberFilterRule;
import com.borui.sms.common.domain.entity.NumberSegment;
import com.borui.sms.mapper.DefaultRouteMapper;
import com.borui.sms.mapper.MnpCacheMapper;
import com.borui.sms.mapper.NumberFilterRuleMapper;
import com.borui.sms.mapper.NumberSegmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NumberRoutingService {

    private final NumberSegmentMapper numberSegmentMapper;
    private final MnpCacheMapper mnpCacheMapper;
    private final DefaultRouteMapper defaultRouteMapper;
    private final NumberFilterRuleMapper numberFilterRuleMapper;

    // ==================== Tab1: 号段归属表 ====================

    public PageResult<NumberSegment> segmentPage(String countryCode, String operator,
                                                  int page, int size) {
        LambdaQueryWrapper<NumberSegment> wrapper = new LambdaQueryWrapper<>();
        if (countryCode != null && !countryCode.isBlank()) wrapper.eq(NumberSegment::getCountryCode, countryCode);
        if (operator != null && !operator.isBlank()) wrapper.like(NumberSegment::getOperator, operator);
        wrapper.orderByAsc(NumberSegment::getCountryCode, NumberSegment::getPrefix);
        Page<NumberSegment> p = numberSegmentMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void addSegment(NumberSegment segment) {
        numberSegmentMapper.insert(segment);
    }

    public void deleteSegment(Long id) {
        numberSegmentMapper.deleteById(id);
    }

    // ==================== Tab2: MNP 缓存表 ====================

    public PageResult<MnpCache> mnpCachePage(String countryCode, String phoneNumber,
                                              int page, int size) {
        LambdaQueryWrapper<MnpCache> wrapper = new LambdaQueryWrapper<>();
        if (countryCode != null && !countryCode.isBlank()) wrapper.eq(MnpCache::getCountryCode, countryCode);
        if (phoneNumber != null && !phoneNumber.isBlank()) wrapper.like(MnpCache::getPhoneNumber, phoneNumber);
        wrapper.orderByDesc(MnpCache::getUpdatedAt);
        Page<MnpCache> p = mnpCacheMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void clearExpiredMnpCache() {
        mnpCacheMapper.delete(new LambdaQueryWrapper<MnpCache>()
                .lt(MnpCache::getExpireAt, java.time.LocalDateTime.now()));
    }

    // ==================== Tab3: 默认路由表 ====================

    public List<DefaultRoute> listDefaultRoutes(String countryCode) {
        LambdaQueryWrapper<DefaultRoute> wrapper = new LambdaQueryWrapper<>();
        if (countryCode != null && !countryCode.isBlank()) wrapper.eq(DefaultRoute::getCountryCode, countryCode);
        wrapper.orderByAsc(DefaultRoute::getCountryCode, DefaultRoute::getPriority);
        return defaultRouteMapper.selectList(wrapper);
    }

    public void saveDefaultRoute(DefaultRoute route) {
        if (route.getId() != null) {
            defaultRouteMapper.updateById(route);
        } else {
            defaultRouteMapper.insert(route);
        }
    }

    public void deleteDefaultRoute(Long id) {
        defaultRouteMapper.deleteById(id);
    }

    public void toggleDefaultRoute(Long id, Boolean active) {
        DefaultRoute route = defaultRouteMapper.selectById(id);
        if (route != null) {
            route.setIsActive(active);
            defaultRouteMapper.updateById(route);
        }
    }

    // ==================== Tab4/5: 号码过滤规则 ====================

    public List<NumberFilterRule> listFilterRules(String countryCode) {
        LambdaQueryWrapper<NumberFilterRule> wrapper = new LambdaQueryWrapper<>();
        if (countryCode != null && !countryCode.isBlank()) wrapper.eq(NumberFilterRule::getCountryCode, countryCode);
        wrapper.orderByDesc(NumberFilterRule::getCreatedAt);
        return numberFilterRuleMapper.selectList(wrapper);
    }

    public void saveFilterRule(NumberFilterRule rule) {
        if (rule.getId() != null) {
            numberFilterRuleMapper.updateById(rule);
        } else {
            numberFilterRuleMapper.insert(rule);
        }
    }

    public void deleteFilterRule(Long id) {
        numberFilterRuleMapper.deleteById(id);
    }

    public void toggleFilterRule(Long id, Boolean active) {
        NumberFilterRule rule = numberFilterRuleMapper.selectById(id);
        if (rule != null) {
            rule.setIsActive(active);
            numberFilterRuleMapper.updateById(rule);
        }
    }
}
