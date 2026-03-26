package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.vo.req.CountryReq;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.Country;
import com.borui.sms.mapper.CountryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryMapper countryMapper;

    public PageResult<Country> list(int page, int size, String keyword) {
        Page<Country> p = new Page<>(page, size);
        LambdaQueryWrapper<Country> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Country::getCountryCode, keyword)
                    .or().like(Country::getCountryName, keyword)
                    .or().like(Country::getCountryNameEn, keyword));
        }
        wrapper.orderByAsc(Country::getCountryCode);
        countryMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Country getById(Long id) {
        Country country = countryMapper.selectById(id);
        if (country == null) throw new BizException(ErrorCode.NOT_FOUND, "国家不存在");
        return country;
    }

    public Country create(CountryReq req) {
        Long exists = countryMapper.selectCount(
                new LambdaQueryWrapper<Country>().eq(Country::getCountryCode, req.getCountryCode()));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "国家代码已存在");

        Country country = new Country();
        country.setCountryCode(req.getCountryCode().toUpperCase());
        country.setCountryName(req.getCountryName());
        country.setCountryNameEn(req.getCountryNameEn());
        country.setPhoneCode(req.getPhoneCode());
        country.setMccMnc(req.getMccMnc());
        country.setIsActive(req.getIsActive());
        country.setRemark(req.getRemark());
        countryMapper.insert(country);
        return country;
    }

    public Country update(Long id, CountryReq req) {
        Country country = getById(id);
        country.setCountryName(req.getCountryName());
        country.setCountryNameEn(req.getCountryNameEn());
        country.setPhoneCode(req.getPhoneCode());
        country.setMccMnc(req.getMccMnc());
        country.setIsActive(req.getIsActive());
        country.setRemark(req.getRemark());
        countryMapper.updateById(country);
        return country;
    }

    public void delete(Long id) {
        getById(id);
        countryMapper.deleteById(id);
    }

    public List<Country> listAll() {
        return countryMapper.selectList(
                new LambdaQueryWrapper<Country>().eq(Country::getIsActive, true).orderByAsc(Country::getCountryCode));
    }
}
