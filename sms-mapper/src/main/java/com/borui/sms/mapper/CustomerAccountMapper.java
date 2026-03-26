package com.borui.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.borui.sms.common.domain.entity.CustomerAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerAccountMapper extends BaseMapper<CustomerAccount> {
}
