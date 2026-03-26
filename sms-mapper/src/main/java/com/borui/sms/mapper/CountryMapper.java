package com.borui.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.borui.sms.common.domain.entity.Country;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CountryMapper extends BaseMapper<Country> {
}
