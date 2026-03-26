-- ============================================
-- SMS Platform V2.0 - Auth Tables
-- MySQL 8.0+
-- ============================================

USE sms_platform;

-- -------------------------------------------
-- 系统角色
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name       VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code       VARCHAR(50)  NOT NULL COMMENT '角色编码',
    permissions     VARCHAR(2000) DEFAULT '' COMMENT '权限标识，逗号分隔',
    description     VARCHAR(200) DEFAULT '',
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='系统角色';

-- -------------------------------------------
-- 系统用户
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL COMMENT '登录名',
    password_hash   VARCHAR(200) NOT NULL COMMENT 'BCrypt密码',
    real_name       VARCHAR(100) DEFAULT '' COMMENT '真实姓名',
    email           VARCHAR(200) DEFAULT '',
    phone           VARCHAR(50)  DEFAULT '',
    role_id         BIGINT       NOT NULL COMMENT '关联sys_role.id',
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    last_login_at   DATETIME     DEFAULT NULL,
    last_login_ip   VARCHAR(50)  DEFAULT '',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户';

-- -------------------------------------------
-- 默认角色
-- -------------------------------------------
INSERT INTO sys_role (role_name, role_code, permissions, description) VALUES
('超级管理员', 'SUPER_ADMIN',
 'dashboard,country,vendor,channel,sid,customer,risk,message,stats,send-test,audit,monitoring,finance,system,ops-strategy,number-routing,template,quality',
 '拥有所有权限'),
('运营管理员', 'OPS_ADMIN',
 'dashboard,country,vendor,channel,sid,customer,risk,message,stats,send-test,audit,monitoring,ops-strategy,number-routing,template,quality',
 '运营管理，无财务和系统配置权限'),
('财务', 'FINANCE',
 'dashboard,customer,stats,finance',
 '仅查看统计和财务模块'),
('只读', 'VIEWER',
 'dashboard,message,stats',
 '只读查看概览和发送统计');

-- -------------------------------------------
-- 默认管理员账号 (密码: admin123 的BCrypt值)
-- -------------------------------------------
INSERT INTO sys_user (username, password_hash, real_name, role_id, is_active) VALUES
('admin', '$2a$10$DyxU6ILrT0C4huTgVnPxYur.s7lKAqNQvyWcQN1HuwRu9Ov7tg15C', '系统管理员', 1, 1);
