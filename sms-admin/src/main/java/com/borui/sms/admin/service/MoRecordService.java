package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.MoRecord;
import com.borui.sms.mapper.MoRecordMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MoRecordService {

    private final MoRecordMapper moRecordMapper;

    public PageResult<MoRecord> list(int page, int size,
                                     String countryCode,
                                     String keyword,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime) {
        Page<MoRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<MoRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(countryCode)) wrapper.eq(MoRecord::getCountryCode, countryCode);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(MoRecord::getMoMessageId, keyword)
                    .or().like(MoRecord::getFromNumber, keyword)
                    .or().like(MoRecord::getToSid, keyword)
                    .or().like(MoRecord::getContent, keyword));
        }
        if (startTime != null) wrapper.ge(MoRecord::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(MoRecord::getCreatedAt, endTime);

        wrapper.orderByDesc(MoRecord::getCreatedAt);
        moRecordMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }
}
