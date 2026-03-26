package com.borui.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.borui.sms.common.domain.entity.ApiAccessLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiAccessLogMapper extends BaseMapper<ApiAccessLog> {
}
