package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.vo.req.SidInfoReq;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.mapper.CountryMapper;
import com.borui.sms.mapper.CustomerSidMapper;
import com.borui.sms.mapper.SidInfoMapper;
import com.borui.sms.mapper.VendorMapper;
import com.borui.sms.mapper.VendorCountryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SidInfoService {

    private final SidInfoMapper sidInfoMapper;
    private final VendorMapper vendorMapper;
    private final CountryMapper countryMapper;
    private final VendorCountryMapper vendorCountryMapper;
    private final CustomerSidMapper customerSidMapper;

    public PageResult<SidInfo> list(int page, int size, String keyword, Long vendorId, String countryCode, String sidType, String smsType) {
        Page<SidInfo> p = new Page<>(page, size);
        LambdaQueryWrapper<SidInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(SidInfo::getSid, keyword);
        }
        if (vendorId != null) wrapper.eq(SidInfo::getVendorId, vendorId);
        if (StringUtils.isNotBlank(countryCode)) wrapper.eq(SidInfo::getCountryCode, countryCode);
        if (StringUtils.isNotBlank(sidType)) wrapper.eq(SidInfo::getSidType, sidType);
        if (StringUtils.isNotBlank(smsType)) wrapper.eq(SidInfo::getSmsType, smsType);
        wrapper.orderByDesc(SidInfo::getCreatedAt);
        sidInfoMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public SidInfo getById(Long id) {
        SidInfo sid = sidInfoMapper.selectById(id);
        if (sid == null) throw new BizException(ErrorCode.NOT_FOUND, "SID不存在");
        return sid;
    }

    @Transactional
    public SidInfo create(SidInfoReq req) {
        // 校验供应商存在
        if (vendorMapper.selectById(req.getVendorId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        // 校验国家代码存在
        Long countryExists = countryMapper.selectCount(
                new LambdaQueryWrapper<Country>().eq(Country::getCountryCode, req.getCountryCode()));
        if (countryExists == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "国家代码不存在: " + req.getCountryCode());
        }
        // 校验供应商已绑定该国家
        // 优先查 vendor_country 关联表；若未找到则以 vendor.countryCode（前端过滤依赖字段）兜底，
        // 兜底成功时自动补写绑定记录，保持数据一致
        Long vcExists = vendorCountryMapper.selectCount(new LambdaQueryWrapper<VendorCountry>()
                .eq(VendorCountry::getVendorId, req.getVendorId())
                .eq(VendorCountry::getCountryCode, req.getCountryCode()));
        if (vcExists == 0) {
            Vendor vendor = vendorMapper.selectById(req.getVendorId());
            if (vendor == null || !req.getCountryCode().equals(vendor.getCountryCode())) {
                throw new BizException(ErrorCode.OPERATION_FAILED, "该供应商未绑定此国家，请先在供应商管理中绑定");
            }
            // vendor.countryCode 匹配，自动补写 vendor_country 记录供后续查询
            VendorCountry autoVc = new VendorCountry();
            autoVc.setVendorId(req.getVendorId());
            autoVc.setCountryCode(req.getCountryCode());
            autoVc.setIsActive(true);
            vendorCountryMapper.insert(autoVc);
        }

        SidInfo sid = new SidInfo();
        sid.setSid(req.getSid());
        sid.setSidType(req.getSidType());
        sid.setVendorId(req.getVendorId());
        sid.setCountryCode(req.getCountryCode());
        sid.setSmsType(req.getSmsType());
        sid.setValidityMonths(req.getValidityMonths());
        sid.setIsActive(req.getIsActive());
        sid.setRemark(req.getRemark());
        sidInfoMapper.insert(sid);
        return sid;
    }

    public SidInfo update(Long id, SidInfoReq req) {
        SidInfo sid = getById(id);
        sid.setSid(req.getSid());
        sid.setSidType(req.getSidType());
        sid.setVendorId(req.getVendorId());
        sid.setCountryCode(req.getCountryCode());
        sid.setSmsType(req.getSmsType());
        sid.setValidityMonths(req.getValidityMonths());
        sid.setIsActive(req.getIsActive());
        sid.setRemark(req.getRemark());
        sidInfoMapper.updateById(sid);
        return sid;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        // 清理客户 SID 授权关联
        customerSidMapper.delete(
                new LambdaQueryWrapper<CustomerSid>().eq(CustomerSid::getSidId, id));
        sidInfoMapper.deleteById(id);
    }
}
