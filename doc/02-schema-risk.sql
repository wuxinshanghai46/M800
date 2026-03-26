-- Risk control tables (Sprint 4)
USE sms_platform;

CREATE TABLE IF NOT EXISTS blacklist (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20) NOT NULL COMMENT 'NUMBER/PREFIX/SID/KEYWORD',
    value           VARCHAR(200) NOT NULL,
    scope           VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    customer_id     BIGINT DEFAULT NULL,
    reason          VARCHAR(500) DEFAULT '',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_bl_type_value (type, value),
    KEY idx_bl_customer (customer_id)
) ENGINE=InnoDB COMMENT='黑名单';

CREATE TABLE IF NOT EXISTS sensitive_word (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    word            VARCHAR(200) NOT NULL,
    match_type      VARCHAR(20) NOT NULL DEFAULT 'CONTAINS' COMMENT 'EXACT/CONTAINS/REGEX',
    action          VARCHAR(20) NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK/WARN/LOG',
    category        VARCHAR(50) DEFAULT 'OTHER',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sw_active (is_active)
) ENGINE=InnoDB COMMENT='敏感词';

CREATE TABLE IF NOT EXISTS rate_limit_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dimension       VARCHAR(30) NOT NULL COMMENT 'NUMBER/CUSTOMER/CUSTOMER_COUNTRY/CHANNEL',
    customer_id     BIGINT DEFAULT NULL,
    country_code    VARCHAR(5) DEFAULT NULL,
    max_count       INT NOT NULL,
    window_seconds  INT NOT NULL,
    action          VARCHAR(20) NOT NULL DEFAULT 'BLOCK',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_rl_dimension (dimension)
) ENGINE=InnoDB COMMENT='频控规则';
