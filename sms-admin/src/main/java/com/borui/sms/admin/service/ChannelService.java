package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.admin.smpp.SmppSessionManager;
import com.borui.sms.admin.vo.req.ChannelReq;
import com.borui.sms.admin.vo.req.ChannelGroupReq;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.*;
import com.borui.sms.common.domain.enums.ChannelStatus;
import com.borui.sms.common.domain.enums.ChannelType;
import com.borui.sms.common.domain.enums.ScheduleMode;
import com.borui.sms.common.domain.enums.SmsAttribute;
import com.borui.sms.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChannelService {

    private final SmppSessionManager smppSessionManager;
    private final ChannelMapper channelMapper;
    private final ChannelGroupMapper channelGroupMapper;
    private final ChannelGroupMemberMapper channelGroupMemberMapper;
    private final VendorMapper vendorMapper;
    private final CountryMapper countryMapper;
    private final RestTemplate testRestTemplate;

    public ChannelService(SmppSessionManager smppSessionManager,
                          ChannelMapper channelMapper,
                          ChannelGroupMapper channelGroupMapper,
                          ChannelGroupMemberMapper channelGroupMemberMapper,
                          VendorMapper vendorMapper,
                          CountryMapper countryMapper,
                          RestTemplateBuilder restTemplateBuilder) {
        this.smppSessionManager = smppSessionManager;
        this.channelMapper = channelMapper;
        this.channelGroupMapper = channelGroupMapper;
        this.channelGroupMemberMapper = channelGroupMemberMapper;
        this.vendorMapper = vendorMapper;
        this.countryMapper = countryMapper;
        this.testRestTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ---- Channel CRUD ----

    public PageResult<Channel> list(int page, int size, String keyword, Long vendorId, String countryCode) {
        Page<Channel> p = new Page<>(page, size);
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Channel::getChannelCode, keyword)
                    .or().like(Channel::getChannelName, keyword));
        }
        if (vendorId != null) wrapper.eq(Channel::getVendorId, vendorId);
        if (StringUtils.isNotBlank(countryCode)) wrapper.eq(Channel::getCountryCode, countryCode);
        wrapper.orderByDesc(Channel::getCreatedAt);
        channelMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public Channel getById(Long id) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) throw new BizException(ErrorCode.CHANNEL_NOT_FOUND);
        return channel;
    }

    public Channel create(ChannelReq req) {
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

        Long exists = channelMapper.selectCount(
                new LambdaQueryWrapper<Channel>().eq(Channel::getChannelCode, req.getChannelCode()));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "通道代码已存在");

        Channel ch = new Channel();
        ch.setChannelCode(req.getChannelCode());
        ch.setChannelName(req.getChannelName());
        ch.setVendorId(req.getVendorId());
        ch.setCountryCode(req.getCountryCode());
        ch.setChannelType(ChannelType.values()[req.getChannelType() - 1]);
        ch.setStatus(ChannelStatus.values()[req.getStatus()]);
        if (req.getSmsAttribute() != null) {
            ch.setSmsAttribute(SmsAttribute.values()[req.getSmsAttribute() - 1]);
        }
        ch.setTps(req.getTps());
        ch.setPriority(req.getPriority());
        // SMPP
        ch.setSmppHost(req.getSmppHost());
        ch.setSmppPort(req.getSmppPort());
        ch.setSmppSystemId(req.getSmppSystemId());
        ch.setSmppPassword(req.getSmppPassword());
        ch.setSmppSystemType(req.getSmppSystemType());
        ch.setSmppWindowSize(req.getSmppWindowSize());
        ch.setSmppEnquireLinkInterval(req.getSmppEnquireLinkInterval());
        ch.setSmppBindType(req.getSmppBindType());
        ch.setSmppDestTon(req.getSmppDestTon());
        ch.setSmppDestNpi(req.getSmppDestNpi());
        // HTTP
        ch.setHttpUrl(req.getHttpUrl());
        ch.setHttpMethod(req.getHttpMethod());
        ch.setHttpHeaders(req.getHttpHeaders());
        ch.setHttpBodyTemplate(req.getHttpBodyTemplate());
        // SMS params
        ch.setDefaultEncoding(req.getDefaultEncoding());
        ch.setMaxSegments(req.getMaxSegments());
        ch.setDlrWaitTimeout(req.getDlrWaitTimeout());
        ch.setRetryCount(req.getRetryCount());
        ch.setCostPrice(req.getCostPrice());
        ch.setCostCurrency(req.getCostCurrency());
        ch.setIsActive(req.getIsActive());
        ch.setRemark(req.getRemark());
        channelMapper.insert(ch);
        return ch;
    }

    public Channel update(Long id, ChannelReq req) {
        Channel ch = getById(id);
        // 校验供应商存在
        if (req.getVendorId() != null && vendorMapper.selectById(req.getVendorId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        // 校验国家代码存在
        if (req.getCountryCode() != null) {
            Long countryExists = countryMapper.selectCount(
                    new LambdaQueryWrapper<Country>().eq(Country::getCountryCode, req.getCountryCode()));
            if (countryExists == 0) {
                throw new BizException(ErrorCode.NOT_FOUND, "国家代码不存在: " + req.getCountryCode());
            }
        }
        if (req.getChannelName() != null) ch.setChannelName(req.getChannelName());
        if (req.getVendorId() != null) ch.setVendorId(req.getVendorId());
        if (req.getCountryCode() != null) ch.setCountryCode(req.getCountryCode());
        if (req.getChannelType() != null) ch.setChannelType(ChannelType.values()[req.getChannelType() - 1]);
        if (req.getStatus() != null) ch.setStatus(ChannelStatus.values()[req.getStatus()]);
        if (req.getSmsAttribute() != null) {
            ch.setSmsAttribute(SmsAttribute.values()[req.getSmsAttribute() - 1]);
        }
        if (req.getTps() != null) ch.setTps(req.getTps());
        if (req.getPriority() != null) ch.setPriority(req.getPriority());
        ch.setSmppHost(req.getSmppHost());
        ch.setSmppPort(req.getSmppPort());
        ch.setSmppSystemId(req.getSmppSystemId());
        ch.setSmppPassword(req.getSmppPassword());
        ch.setSmppSystemType(req.getSmppSystemType());
        ch.setSmppWindowSize(req.getSmppWindowSize());
        ch.setSmppEnquireLinkInterval(req.getSmppEnquireLinkInterval());
        ch.setSmppBindType(req.getSmppBindType());
        ch.setSmppDestTon(req.getSmppDestTon());
        ch.setSmppDestNpi(req.getSmppDestNpi());
        ch.setHttpUrl(req.getHttpUrl());
        ch.setHttpMethod(req.getHttpMethod());
        ch.setHttpHeaders(req.getHttpHeaders());
        ch.setHttpBodyTemplate(req.getHttpBodyTemplate());
        ch.setDefaultEncoding(req.getDefaultEncoding());
        ch.setMaxSegments(req.getMaxSegments());
        ch.setDlrWaitTimeout(req.getDlrWaitTimeout());
        ch.setRetryCount(req.getRetryCount());
        ch.setCostPrice(req.getCostPrice());
        ch.setCostCurrency(req.getCostCurrency());
        if (req.getIsActive() != null) ch.setIsActive(req.getIsActive());
        ch.setRemark(req.getRemark());
        channelMapper.updateById(ch);
        return ch;
    }

    public Map<String, Object> testSmppConnection(Long id) {
        Channel channel = getById(id);
        if (channel.getChannelType() != ChannelType.SMPP) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "该通道不是 SMPP 类型");
        }
        if (channel.getSmppHost() == null || channel.getSmppHost().isBlank()) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "SMPP 服务器地址未配置");
        }
        return smppSessionManager.testConnection(channel);
    }

    public Map<Long, Boolean> getSmppConnectionStatus() {
        return smppSessionManager.getConnectionStatus();
    }

    /**
     * Test HTTP channel connectivity by sending a HEAD/GET request to the configured URL.
     * Returns a structured result with error code and hint based on the failure type.
     */
    public Map<String, Object> testHttpConnection(Long id) {
        Channel channel = getById(id);
        if (channel.getChannelType() != ChannelType.HTTP) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "该通道不是 HTTP 类型");
        }
        if (StringUtils.isBlank(channel.getHttpUrl())) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "HTTP 接口地址未配置");
        }

        Map<String, Object> result = new HashMap<>();
        // Use a stripped URL (remove template variables) for connectivity probe
        String probeUrl = channel.getHttpUrl().replaceAll("\\$\\{[^}]+}", "test");

        try {
            ResponseEntity<String> response = testRestTemplate.getForEntity(probeUrl, String.class);
            int statusCode = response.getStatusCode().value();

            if (response.getStatusCode().is2xxSuccessful()) {
                result.put("success", true);
                result.put("errorCode", "");
                result.put("message", "连接成功 (HTTP " + statusCode + ")");
            } else {
                // Treat non-2xx as channel-side issues but connection is reachable
                result.put("success", true);
                result.put("errorCode", "HTTP_" + statusCode);
                result.put("message", "地址可达，HTTP 状态码: " + statusCode + " — 正式发送时请确认请求体参数是否正确");
            }

        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            String[] info = resolveHttpErrorInfo(statusCode);
            log.warn("HTTP test 4xx: channel={}, status={}", channel.getChannelCode(), statusCode);
            result.put("success", false);
            result.put("errorCode", "HTTP_" + statusCode);
            result.put("message", "[HTTP " + statusCode + "] " + info[0]);
            result.put("hint", info[1]);

        } catch (HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            log.warn("HTTP test 5xx: channel={}, status={}", channel.getChannelCode(), statusCode);
            result.put("success", false);
            result.put("errorCode", "HTTP_" + statusCode);
            result.put("message", "[HTTP " + statusCode + "] 供应商服务器错误");
            result.put("hint", "供应商服务端异常，请联系供应商技术支持排查");

        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException) {
                String host = extractHost(probeUrl);
                log.warn("HTTP test DNS failure: channel={}, host={}", channel.getChannelCode(), host);
                result.put("success", false);
                result.put("errorCode", "DNS_FAILURE");
                result.put("message", "域名解析失败: 无法解析服务器地址 \"" + host + "\"");
                result.put("hint", "请检查 HTTP 接口地址中的域名是否正确，或确认网络 DNS 是否正常");
            } else if (cause instanceof ConnectException) {
                log.warn("HTTP test connection refused: channel={}, url={}", channel.getChannelCode(), probeUrl);
                result.put("success", false);
                result.put("errorCode", "CONNECTION_REFUSED");
                result.put("message", "连接被拒绝: 服务器不可达");
                result.put("hint", "请检查 HTTP 接口地址和端口是否正确，以及防火墙是否允许该连接");
            } else if (cause instanceof SocketTimeoutException) {
                log.warn("HTTP test timeout: channel={}, url={}", channel.getChannelCode(), probeUrl);
                result.put("success", false);
                result.put("errorCode", "CONNECT_TIMEOUT");
                result.put("message", "连接超时: 服务器无响应");
                result.put("hint", "服务器不可达或响应过慢，请检查网络连通性");
            } else {
                log.warn("HTTP test access error: channel={}, error={}", channel.getChannelCode(), e.getMessage());
                result.put("success", false);
                result.put("errorCode", "IO_ERROR");
                result.put("message", "连接失败: " + e.getMessage());
                result.put("hint", "请检查 HTTP 接口地址配置是否正确");
            }

        } catch (Exception e) {
            log.error("HTTP test unexpected error: channel={}", channel.getChannelCode(), e);
            result.put("success", false);
            result.put("errorCode", "UNKNOWN_ERROR");
            result.put("message", "测试失败: " + e.getMessage());
            result.put("hint", "请检查 HTTP 接口地址配置");
        }

        return result;
    }

    private String[] resolveHttpErrorInfo(int statusCode) {
        return switch (statusCode) {
            case 400 -> new String[]{"请求参数错误", "检查 HTTP Body 模板格式是否符合供应商接口文档要求"};
            case 401 -> new String[]{"认证失败（未授权）", "检查 HTTP Headers 中的认证信息（API Key / Token / Authorization）是否配置正确"};
            case 403 -> new String[]{"访问被禁止", "账号无权限访问该接口，请联系供应商确认账号状态和 IP 白名单"};
            case 404 -> new String[]{"接口地址不存在", "检查 HTTP URL 是否正确，供应商可能已更新接口路径"};
            case 405 -> new String[]{"请求方法不支持", "检查 HTTP Method 配置（POST/GET），应与供应商接口文档一致"};
            case 429 -> new String[]{"请求频率超限", "降低发送速率，检查 TPS 配置；供应商对调用频率有限制"};
            default  -> new String[]{"客户端错误 (HTTP " + statusCode + ")", "请对照供应商接口文档排查请求参数"};
        };
    }

    private String extractHost(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    public void delete(Long id) {
        getById(id);
        // 检查是否有通道组引用
        Long memberCount = channelGroupMemberMapper.selectCount(
                new LambdaQueryWrapper<ChannelGroupMember>().eq(ChannelGroupMember::getChannelId, id));
        if (memberCount > 0) {
            throw new BizException(ErrorCode.OPERATION_FAILED, "该通道已被 " + memberCount + " 个通道组引用，请先移除引用");
        }
        channelMapper.deleteById(id);
    }

    // ---- Channel Group ----

    public PageResult<ChannelGroup> listGroups(int page, int size) {
        Page<ChannelGroup> p = new Page<>(page, size);
        LambdaQueryWrapper<ChannelGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChannelGroup::getCreatedAt);
        channelGroupMapper.selectPage(p, wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public ChannelGroup createGroup(ChannelGroupReq req) {
        ChannelGroup group = new ChannelGroup();
        group.setGroupName(req.getGroupName());
        group.setScheduleMode(ScheduleMode.values()[req.getScheduleMode() - 1]);
        group.setIsActive(req.getIsActive());
        group.setRemark(req.getRemark());
        channelGroupMapper.insert(group);
        return group;
    }

    public ChannelGroup updateGroup(Long id, ChannelGroupReq req) {
        ChannelGroup group = channelGroupMapper.selectById(id);
        if (group == null) throw new BizException(ErrorCode.NOT_FOUND, "通道组不存在");
        group.setGroupName(req.getGroupName());
        group.setScheduleMode(ScheduleMode.values()[req.getScheduleMode() - 1]);
        group.setIsActive(req.getIsActive());
        group.setRemark(req.getRemark());
        channelGroupMapper.updateById(group);
        return group;
    }

    @Transactional
    public void deleteGroup(Long id) {
        channelGroupMapper.deleteById(id);
        channelGroupMemberMapper.delete(
                new LambdaQueryWrapper<ChannelGroupMember>().eq(ChannelGroupMember::getGroupId, id));
    }

    public List<ChannelGroupMember> listGroupMembers(Long groupId) {
        return channelGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChannelGroupMember>().eq(ChannelGroupMember::getGroupId, groupId));
    }

    public ChannelGroupMember addGroupMember(Long groupId, Long channelId, Integer priority, Integer weight) {
        Long exists = channelGroupMemberMapper.selectCount(new LambdaQueryWrapper<ChannelGroupMember>()
                .eq(ChannelGroupMember::getGroupId, groupId)
                .eq(ChannelGroupMember::getChannelId, channelId));
        if (exists > 0) throw new BizException(ErrorCode.DUPLICATE, "通道已在组中");

        ChannelGroupMember member = new ChannelGroupMember();
        member.setGroupId(groupId);
        member.setChannelId(channelId);
        member.setPriority(priority != null ? priority : 10);
        member.setWeight(weight != null ? weight : 100);
        member.setIsActive(true);
        channelGroupMemberMapper.insert(member);
        return member;
    }

    public void removeGroupMember(Long groupId, Long channelId) {
        channelGroupMemberMapper.delete(new LambdaQueryWrapper<ChannelGroupMember>()
                .eq(ChannelGroupMember::getGroupId, groupId)
                .eq(ChannelGroupMember::getChannelId, channelId));
    }
}
