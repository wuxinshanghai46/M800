-- ============================================
-- SMS Platform V2.0 - Sprint 8 Additional Tables
-- MySQL 8.0+
-- ============================================

USE sms_platform;

-- -------------------------------------------
-- 1. Finance & Settlement
-- -------------------------------------------

CREATE TABLE IF NOT EXISTS bill (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_no         VARCHAR(50) NOT NULL COMMENT 'Bill number',
    customer_id     BIGINT NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    total_messages  INT NOT NULL DEFAULT 0,
    total_segments  INT NOT NULL DEFAULT 0,
    amount          DECIMAL(12,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    status          VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'draft / issued / paid / overdue',
    issued_at       DATETIME DEFAULT NULL,
    paid_at         DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bill_no (bill_no),
    KEY idx_bill_customer (customer_id),
    KEY idx_bill_status (status)
) ENGINE=InnoDB COMMENT='账单';

CREATE TABLE IF NOT EXISTS vendor_settlement_price (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id       BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    channel_id      BIGINT DEFAULT NULL,
    sms_attribute   TINYINT DEFAULT NULL COMMENT '1=OTP, 2=Transaction, 3=Notification, 4=Marketing',
    price           DECIMAL(10,6) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    effective_date  DATE NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_vsp_vendor (vendor_id),
    KEY idx_vsp_country (country_code)
) ENGINE=InnoDB COMMENT='供应商结算价';

CREATE TABLE IF NOT EXISTS exchange_rate (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate            DECIMAL(12,6) NOT NULL,
    effective_date  DATE NOT NULL,
    updated_by      VARCHAR(100) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_er_currencies (source_currency, target_currency),
    KEY idx_er_date (effective_date)
) ENGINE=InnoDB COMMENT='汇率';

-- -------------------------------------------
-- 2. Operations Strategy
-- -------------------------------------------

CREATE TABLE IF NOT EXISTS review_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name       VARCHAR(100) NOT NULL,
    description     VARCHAR(500) DEFAULT '',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='人工审核规则';

CREATE TABLE IF NOT EXISTS review_ticket (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    message_id      VARCHAR(50) DEFAULT '',
    content_preview VARCHAR(500) DEFAULT '',
    trigger_rule    VARCHAR(200) DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / approved / rejected',
    reviewer        VARCHAR(100) DEFAULT '',
    review_at       DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_rt_customer (customer_id),
    KEY idx_rt_status (status)
) ENGINE=InnoDB COMMENT='审核工单';

CREATE TABLE IF NOT EXISTS routing_strategy_assignment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT DEFAULT NULL,
    sms_attribute   TINYINT DEFAULT NULL COMMENT '1=OTP, 2=Transaction, 3=Notification, 4=Marketing',
    country_code    VARCHAR(5) DEFAULT '',
    channel_group_id BIGINT DEFAULT NULL,
    channel_id      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_rsa_customer (customer_id),
    KEY idx_rsa_country (country_code)
) ENGINE=InnoDB COMMENT='路由策略分配';

CREATE TABLE IF NOT EXISTS template_replace_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trigger_condition VARCHAR(50) NOT NULL COMMENT 'country / channel / customer',
    trigger_value   VARCHAR(100) DEFAULT '',
    match_type      VARCHAR(20) NOT NULL COMMENT 'keyword / regex',
    find_content    VARCHAR(500) NOT NULL,
    replace_with    VARCHAR(500) NOT NULL DEFAULT '',
    scope           VARCHAR(20) NOT NULL DEFAULT 'global' COMMENT 'global / channel / customer',
    trigger_count   INT NOT NULL DEFAULT 0,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='模板替换规则';

CREATE TABLE IF NOT EXISTS sid_replace_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT DEFAULT NULL,
    original_sid    VARCHAR(50) NOT NULL,
    target_channel  VARCHAR(50) DEFAULT '',
    replacement_sid VARCHAR(50) NOT NULL,
    replace_reason  VARCHAR(500) DEFAULT '',
    trigger_count   INT NOT NULL DEFAULT 0,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_srr_customer (customer_id)
) ENGINE=InnoDB COMMENT='SID替换规则';

-- -------------------------------------------
-- 3. System Configuration
-- -------------------------------------------

CREATE TABLE IF NOT EXISTS system_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_group    VARCHAR(50) NOT NULL,
    config_key      VARCHAR(100) NOT NULL,
    config_value    TEXT DEFAULT NULL,
    description     VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_group_key (config_group, config_key)
) ENGINE=InnoDB COMMENT='系统配置';

-- -------------------------------------------
-- 4. Monitoring & Alerting
-- -------------------------------------------

CREATE TABLE IF NOT EXISTS alert_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name       VARCHAR(100) NOT NULL,
    rule_type       VARCHAR(50) NOT NULL,
    condition_expr  VARCHAR(500) NOT NULL,
    threshold       VARCHAR(100) NOT NULL DEFAULT '',
    severity        VARCHAR(20) NOT NULL DEFAULT 'warning' COMMENT 'critical / warning / info',
    notify_channels VARCHAR(500) DEFAULT '' COMMENT 'JSON array: email / sms / webhook',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='告警规则';

CREATE TABLE IF NOT EXISTS alert_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    rule_name       VARCHAR(100) NOT NULL DEFAULT '',
    severity        VARCHAR(20) NOT NULL DEFAULT 'warning',
    title           VARCHAR(200) NOT NULL,
    content         TEXT DEFAULT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / acknowledged / resolved',
    acknowledged_by VARCHAR(100) DEFAULT '',
    acknowledged_at DATETIME DEFAULT NULL,
    resolved_at     DATETIME DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ar_rule (rule_id),
    KEY idx_ar_status (status),
    KEY idx_ar_severity (severity)
) ENGINE=InnoDB COMMENT='告警记录';

-- -------------------------------------------
-- 5. Template Management
-- -------------------------------------------

CREATE TABLE IF NOT EXISTS sms_template (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code   VARCHAR(50) NOT NULL,
    template_name   VARCHAR(100) NOT NULL,
    customer_id     BIGINT DEFAULT NULL,
    template_type   VARCHAR(30) NOT NULL COMMENT 'OTP / Transactional / Marketing / Service',
    content         TEXT NOT NULL,
    variables       TEXT DEFAULT NULL COMMENT 'JSON array',
    country_code    VARCHAR(5) DEFAULT '',
    language        VARCHAR(20) DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / approved / rejected',
    reviewer        VARCHAR(100) DEFAULT '',
    review_at       DATETIME DEFAULT NULL,
    reject_reason   VARCHAR(500) DEFAULT '',
    dlt_template_id VARCHAR(100) DEFAULT '' COMMENT 'DLT Template ID (India)',
    dlt_entity_id   VARCHAR(100) DEFAULT '' COMMENT 'DLT Entity ID (India)',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_code (template_code),
    KEY idx_tpl_customer (customer_id),
    KEY idx_tpl_status (status),
    KEY idx_tpl_country (country_code)
) ENGINE=InnoDB COMMENT='短信模板';

CREATE TABLE IF NOT EXISTS template_variable (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    var_name        VARCHAR(50) NOT NULL,
    var_type        VARCHAR(20) NOT NULL COMMENT 'string / number / date / phone / enum / url',
    description     VARCHAR(500) DEFAULT '',
    default_value   VARCHAR(200) DEFAULT '',
    max_length      INT DEFAULT NULL,
    required        TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='模板变量';

-- -------------------------------------------
-- 6. Number Routing
-- -------------------------------------------

CREATE TABLE IF NOT EXISTS number_segment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code    VARCHAR(5) NOT NULL,
    prefix          VARCHAR(20) NOT NULL,
    operator        VARCHAR(100) DEFAULT '',
    number_type     VARCHAR(20) NOT NULL DEFAULT 'mobile' COMMENT 'mobile / fixed / voip',
    source          VARCHAR(30) NOT NULL DEFAULT 'manual' COMMENT 'libphonenumber / manual',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ns_country (country_code),
    KEY idx_ns_prefix (prefix)
) ENGINE=InnoDB COMMENT='号段信息';

CREATE TABLE IF NOT EXISTS mnp_cache (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number    VARCHAR(20) NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    original_operator VARCHAR(100) DEFAULT '',
    current_operator  VARCHAR(100) DEFAULT '',
    ported          TINYINT(1) NOT NULL DEFAULT 0,
    query_source    VARCHAR(50) DEFAULT '',
    expire_at       DATETIME DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mnp_phone (phone_number),
    KEY idx_mnp_country (country_code),
    KEY idx_mnp_expire (expire_at)
) ENGINE=InnoDB COMMENT='MNP携号转网缓存';

CREATE TABLE IF NOT EXISTS default_route (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code    VARCHAR(5) NOT NULL,
    operator        VARCHAR(100) DEFAULT '',
    sms_attribute   TINYINT DEFAULT NULL COMMENT '1=OTP, 2=Transaction, 3=Notification, 4=Marketing',
    channel_id      BIGINT NOT NULL,
    priority        INT NOT NULL DEFAULT 10 COMMENT 'lower=higher priority',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dr_country (country_code),
    KEY idx_dr_channel (channel_id)
) ENGINE=InnoDB COMMENT='默认路由';

CREATE TABLE IF NOT EXISTS number_filter_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name       VARCHAR(100) NOT NULL,
    filter_type     VARCHAR(20) NOT NULL COMMENT 'prefix / regex / range',
    filter_value    VARCHAR(500) NOT NULL,
    action          VARCHAR(20) NOT NULL DEFAULT 'block' COMMENT 'block / allow / route_to',
    target_channel_id BIGINT DEFAULT NULL,
    country_code    VARCHAR(5) DEFAULT '',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_nfr_country (country_code)
) ENGINE=InnoDB COMMENT='号码过滤规则';
