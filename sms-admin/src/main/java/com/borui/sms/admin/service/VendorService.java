package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.vo.req.VendorReq;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.Channel;
import com.borui.sms.common.domain.entity.Country;
import com.borui.sms.common.domain.entity.Vendor;
import com.borui.sms.common.domain.entity.VendorCountry;
import com.borui.sms.mapper.ChannelMapper;
import com.borui.sms.mapper.CountryMapper;
import com.borui.sms.mapper.VendorCountryMapper;
import com.borui.sms.mapper.VendorMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorMapper vendorMapper;
    private final VendorCountryMapper vendorCountryMapper;
    private final ChannelMapper channelMapper;
    private final CountryMapper countryMapper;

    public PageResult<Vendor> list(int page, int size, String keyword) {
        Page<Vendor> p = new Page<>(page, size);
        LambdaQueryWrapper<Vendor> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Vendor::getVendorCode, keyword)
                    .or().like(Vendor::getVendorName, keyword));
        }
        wrapper.orderByDesc(Vendor::getCreatedAt);
        vendorMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Vendor getById(Long id) {
        Vendor vendor = vendorMapper.selectById(id);
        if (vendor == null) throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在");
        return vendor;
    }

    @Transactional
    public Vendor create(VendorReq req) {
        Long exists = vendorMapper.selectCount(
                new LambdaQueryWrapper<Vendor>().eq(Vendor::getVendorCode, req.getVendorCode()));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "供应商代码已存在");

        Vendor vendor = new Vendor();
        vendor.setVendorCode(req.getVendorCode());
        vendor.setVendorName(req.getVendorName());
        vendor.setCountryCode(req.getCountryCode());
        vendor.setContactName(req.getContactName());
        vendor.setContactEmail(req.getContactEmail());
        vendor.setContactPhone(req.getContactPhone());
        vendor.setApiType(req.getApiType());
        vendor.setIsActive(req.getIsActive());
        vendor.setRemark(req.getRemark());
        vendorMapper.insert(vendor);

        // 自动将供应商主国家绑定到 vendor_country，确保 SID 创建时校验通过
        if (req.getCountryCode() != null) {
            Long vcExists = vendorCountryMapper.selectCount(new LambdaQueryWrapper<VendorCountry>()
                    .eq(VendorCountry::getVendorId, vendor.getId())
                    .eq(VendorCountry::getCountryCode, req.getCountryCode()));
            if (vcExists == 0) {
                VendorCountry vc = new VendorCountry();
                vc.setVendorId(vendor.getId());
                vc.setCountryCode(req.getCountryCode());
                vc.setIsActive(true);
                vendorCountryMapper.insert(vc);
            }
        }
        return vendor;
    }

    @Transactional
    public Vendor update(Long id, VendorReq req) {
        Vendor vendor = getById(id);
        boolean wasActive = Boolean.TRUE.equals(vendor.getIsActive());
        boolean willBeActive = Boolean.TRUE.equals(req.getIsActive());

        vendor.setVendorName(req.getVendorName());
        if (req.getCountryCode() != null) {
            vendor.setCountryCode(req.getCountryCode());
        }
        vendor.setContactName(req.getContactName());
        vendor.setContactEmail(req.getContactEmail());
        vendor.setContactPhone(req.getContactPhone());
        vendor.setApiType(req.getApiType());
        vendor.setIsActive(req.getIsActive());
        vendor.setRemark(req.getRemark());
        vendorMapper.updateById(vendor);

        // 禁用供应商时，级联禁用其下所有通道
        if (wasActive && !willBeActive) {
            cascadeDisableChannels(id);
        }
        return vendor;
    }

    /**
     * 级联禁用供应商下所有活跃通道
     * @return 被禁用的通道数量
     */
    public int cascadeDisableChannels(Long vendorId) {
        List<Channel> channels = channelMapper.selectList(
                new LambdaQueryWrapper<Channel>()
                        .eq(Channel::getVendorId, vendorId)
                        .eq(Channel::getIsActive, true));
        for (Channel ch : channels) {
            ch.setIsActive(false);
            channelMapper.updateById(ch);
        }
        return channels.size();
    }

    public void delete(Long id) {
        getById(id);
        // 检查是否有关联通道
        Long channelCount = channelMapper.selectCount(
                new LambdaQueryWrapper<Channel>().eq(Channel::getVendorId, id));
        if (channelCount > 0) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "该供应商下有 " + channelCount + " 条通道，无法删除");
        }
        // 同时清理国家绑定
        vendorCountryMapper.delete(
                new LambdaQueryWrapper<VendorCountry>().eq(VendorCountry::getVendorId, id));
        vendorMapper.deleteById(id);
    }

    // Vendor-Country bindings
    public List<VendorCountry> listCountries(Long vendorId) {
        return vendorCountryMapper.selectList(
                new LambdaQueryWrapper<VendorCountry>().eq(VendorCountry::getVendorId, vendorId));
    }

    public VendorCountry bindCountry(Long vendorId, String countryCode) {
        // 校验国家代码存在
        Long countryExists = countryMapper.selectCount(
                new LambdaQueryWrapper<Country>().eq(Country::getCountryCode, countryCode));
        if (countryExists == 0) throw new BizException(ErrorCode.NOT_FOUND, "国家代码不存在: " + countryCode);

        Long exists = vendorCountryMapper.selectCount(new LambdaQueryWrapper<VendorCountry>()
                .eq(VendorCountry::getVendorId, vendorId)
                .eq(VendorCountry::getCountryCode, countryCode));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "该国家已绑定");

        VendorCountry vc = new VendorCountry();
        vc.setVendorId(vendorId);
        vc.setCountryCode(countryCode);
        vc.setIsActive(true);
        vendorCountryMapper.insert(vc);
        return vc;
    }

    public void unbindCountry(Long vendorId, String countryCode) {
        // 检查该供应商在该国家下是否有关联通道
        Long channelCount = channelMapper.selectCount(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getVendorId, vendorId)
                .eq(Channel::getCountryCode, countryCode));
        if (channelCount > 0) {
            throw new BizException(ErrorCode.OPERATION_FAILED,
                    "该供应商在此国家下有 " + channelCount + " 条通道，请先移除通道再解绑");
        }
        vendorCountryMapper.delete(new LambdaQueryWrapper<VendorCountry>()
                .eq(VendorCountry::getVendorId, vendorId)
                .eq(VendorCountry::getCountryCode, countryCode));
    }
}
