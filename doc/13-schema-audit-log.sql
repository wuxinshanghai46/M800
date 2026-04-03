-- =============================================
-- Sprint 13: Enhance audit log module
-- Align operation_log with design spec, add login_log / api_access_log
-- =============================================

USE sms_platform;

-- -------------------------------------------
-- 1. Enhance operation_log
--    Drop simplified columns, add full audit fields
-- -------------------------------------------
ALTER TABLE operation_log
    DROP COLUMN operator,
    DROP COLUMN detail,
    ADD COLUMN trace_id       VARCHAR(64)   DEFAULT NULL             AFTER id,
    ADD COLUMN operator_id    BIGINT        DEFAULT NULL             AFTER trace_id,
    ADD COLUMN operator_name  VARCHAR(50)   NOT NULL DEFAULT ''      AFTER operator_id,
    ADD COLUMN operator_type  VARCHAR(20)   NOT NULL DEFAULT 'admin' COMMENT 'admin/customer' AFTER operator_name,
    ADD COLUMN target_name    VARCHAR(200)  DEFAULT ''               AFTER target_id,
    ADD COLUMN summary        VARCHAR(500)  DEFAULT ''               AFTER target_name,
    ADD COLUMN before_data    JSON          DEFAULT NULL             AFTER summary,
    ADD COLUMN after_data     JSON          DEFAULT NULL             AFTER before_data,
    MODIFY COLUMN ip          VARCHAR(45)   DEFAULT ''               ,
    ADD COLUMN user_agent     VARCHAR(500)  DEFAULT ''               AFTER ip,
    ADD COLUMN op_result      VARCHAR(10)   NOT NULL DEFAULT 'success' COMMENT 'success/failed' AFTER user_agent,
    ADD COLUMN error_message  VARCHAR(500)  DEFAULT ''               AFTER op_result,
    ADD KEY idx_ol_operator_name (operator_name);

-- Note: ip column already exists in original schema — if migration fails, remove the ADD COLUMN ip line
-- and instead run: ALTER TABLE operation_log MODIFY COLUMN ip VARCHAR(45) DEFAULT '';

-- -------------------------------------------
-- 2. Login log
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS login_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    user_type       VARCHAR(20)   NOT NULL DEFAULT 'admin'    COMMENT 'admin/customer',
    login_type      VARCHAR(20)   NOT NULL DEFAULT 'password' COMMENT 'password/sso/api_key',
    ip_address      VARCHAR(45)   DEFAULT '',
    user_agent      VARCHAR(500)  DEFAULT '',
    geo_location    VARCHAR(100)  DEFAULT '',
    result          VARCHAR(10)   NOT NULL DEFAULT 'success'  COMMENT 'success/failed/locked',
    fail_reason     VARCHAR(200)  DEFAULT '',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ll_user (user_id),
    KEY idx_ll_time (created_at)
) ENGINE=InnoDB COMMENT='登录日志';

-- -------------------------------------------
-- 3. API access log
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS api_access_log (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id       BIGINT        NOT NULL,
    api_path          VARCHAR(200)  NOT NULL DEFAULT '',
    method            VARCHAR(10)   DEFAULT '',
    request_ip        VARCHAR(45)   DEFAULT '',
    request_params    JSON          DEFAULT NULL COMMENT '脱敏后的请求参数',
    response_code     INT           DEFAULT NULL,
    response_time_ms  INT           DEFAULT NULL,
    error_code        VARCHAR(20)   DEFAULT '',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_aal_customer (customer_id),
    KEY idx_aal_time    (created_at)
) ENGINE=InnoDB COMMENT='API访问日志';
