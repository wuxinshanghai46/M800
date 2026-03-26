package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.borui.sms.admin.security.JwtUtils;
import com.borui.sms.admin.security.LoginUser;
import com.borui.sms.admin.vo.req.LoginReq;
import com.borui.sms.admin.vo.resp.LoginResp;
import com.borui.sms.common.common.exception.BizException;
import com.borui.sms.common.common.result.ErrorCode;
import com.borui.sms.common.domain.entity.SysRole;
import com.borui.sms.common.domain.entity.SysUser;
import com.borui.sms.mapper.SysRoleMapper;
import com.borui.sms.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final LoginLogService loginLogService;

    public LoginResp login(LoginReq req, String clientIp, String userAgent) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            if (user != null) {
                loginLogService.record(user.getId(), "admin", "password",
                        clientIp, userAgent, "failed", "密码错误");
            }
            throw new BizException(ErrorCode.AUTH_FAILED, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            loginLogService.record(user.getId(), "admin", "password",
                    clientIp, userAgent, "locked", "账号已禁用");
            throw new BizException(ErrorCode.AUTH_FAILED, "账号已禁用");
        }

        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        if (role == null || !Boolean.TRUE.equals(role.getIsActive())) {
            loginLogService.record(user.getId(), "admin", "password",
                    clientIp, userAgent, "failed", "角色无效");
            throw new BizException(ErrorCode.AUTH_FAILED, "角色无效");
        }

        // update last login info
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        sysUserMapper.updateById(user);

        loginLogService.record(user.getId(), "admin", "password",
                clientIp, userAgent, "success", null);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role.getRoleCode());

        List<String> permissions = Collections.emptyList();
        if (role.getPermissions() != null && !role.getPermissions().isBlank()) {
            permissions = Arrays.asList(role.getPermissions().split(","));
        }

        return new LoginResp(token, user.getId(), user.getUsername(),
                user.getRealName(), role.getRoleCode(), role.getRoleName(), permissions);
    }

    public LoginUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        throw new BizException(ErrorCode.AUTH_FAILED);
    }
}
