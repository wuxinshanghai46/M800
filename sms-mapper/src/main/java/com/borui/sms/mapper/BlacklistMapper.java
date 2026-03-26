package com.borui.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.borui.sms.common.domain.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
}
