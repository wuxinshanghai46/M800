package com.borui.sms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.borui.sms.common.common.result.PageResult;
import com.borui.sms.common.domain.entity.SysRole;
import com.borui.sms.common.domain.entity.SysUser;
import com.borui.sms.common.domain.entity.SystemConfig;
import com.borui.sms.mapper.SysRoleMapper;
import com.borui.sms.mapper.SysUserMapper;
import com.borui.sms.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    // ==================== 配置管理 (Tab1~4共用) ====================

    public List<SystemConfig> getConfigByGroup(String group) {
        return systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigGroup, group)
                        .orderByAsc(SystemConfig::getId));
    }

    public Map<String, String> getConfigMapByGroup(String group) {
        List<SystemConfig> configs = getConfigByGroup(group);
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemConfig c : configs) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }

    public void updateConfig(String group, String key, String value) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigGroup, group)
                        .eq(SystemConfig::getConfigKey, key));
        if (config != null) {
            config.setConfigValue(value);
            systemConfigMapper.updateById(config);
        } else {
            config = new SystemConfig();
            config.setConfigGroup(group);
            config.setConfigKey(key);
            config.setConfigValue(value);
            systemConfigMapper.insert(config);
        }
    }

    public void batchUpdateConfig(String group, Map<String, String> configs) {
        configs.forEach((key, value) -> updateConfig(group, key, value));
    }

    // ==================== 角色管理 ====================

    public List<SysRole> listRoles() {
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public void saveRole(SysRole role) {
        if (role.getId() != null) {
            sysRoleMapper.updateById(role);
        } else {
            sysRoleMapper.insert(role);
        }
    }

    public void deleteRole(Long id) {
        sysRoleMapper.deleteById(id);
    }

    // ==================== 用户管理 ====================

    public PageResult<SysUser> userPage(String keyword, Long roleId, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        if (roleId != null) wrapper.eq(SysUser::getRoleId, roleId);
        wrapper.orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> p = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void saveUser(SysUser user) {
        if (user.getId() != null) {
            // update: don't overwrite password
            SysUser existing = sysUserMapper.selectById(user.getId());
            if (existing != null) {
                user.setPasswordHash(existing.getPasswordHash());
            }
            sysUserMapper.updateById(user);
        } else {
            // new user: hash the password
            if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            } else {
                // default password
                user.setPasswordHash(passwordEncoder.encode("admin123"));
            }
            sysUserMapper.insert(user);
        }
    }

    public void toggleUser(Long id, Boolean active) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            user.setIsActive(active);
            sysUserMapper.updateById(user);
        }
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            sysUserMapper.updateById(user);
        }
    }
}
