-- ============================================
-- SMS Platform V2.0 - Database Schema (MVP)
-- MySQL 8.0+
-- ============================================

CREATE DATABASE IF NOT EXISTS sms_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE sms_platform;

-- -------------------------------------------
-- 1. Basic Resources
-- -------------------------------------------

CREATE TABLE country (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code    VARCHAR(5) NOT NULL COMMENT 'ISO 3166-1 alpha-2',
    country_name    VARCHAR(100) NOT NULL,
    country_name_en VARCHAR(100) NOT NULL,
    phone_code      VARCHAR(10) NOT NULL COMMENT 'e.g. +1, +86',
    mcc_mnc         VARCHAR(500) DEFAULT '' COMMENT 'MCC-MNC list, comma separated',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_country_code (country_code)
) ENGINE=InnoDB COMMENT='国家/地区';

CREATE TABLE vendor (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_code     VARCHAR(50) NOT NULL,
    vendor_name     VARCHAR(100) NOT NULL,
    country_code    VARCHAR(5) DEFAULT '' COMMENT '供应商注册国家',
    contact_name    VARCHAR(100) DEFAULT '',
    contact_email   VARCHAR(200) DEFAULT '',
    contact_phone   VARCHAR(50) DEFAULT '',
    api_type        VARCHAR(10) DEFAULT 'SMPP' COMMENT 'SMPP / HTTP',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vendor_code (vendor_code)
) ENGINE=InnoDB COMMENT='供应商';

CREATE TABLE vendor_country (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id       BIGINT NOT NULL,
    country_id      BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vendor_country (vendor_id, country_id),
    KEY idx_vc_country (country_code)
) ENGINE=InnoDB COMMENT='供应商覆盖国家';

CREATE TABLE channel (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code    VARCHAR(50) NOT NULL,
    channel_name    VARCHAR(100) NOT NULL,
    vendor_id       BIGINT NOT NULL,
    country_id      BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    channel_type    TINYINT NOT NULL COMMENT '1=SMPP, 2=HTTP',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled, 2=testing',
    sms_attribute   TINYINT DEFAULT NULL COMMENT '1=OTP, 2=Transaction, 3=Notification, 4=Marketing',
    tps             INT NOT NULL DEFAULT 100,
    priority        INT NOT NULL DEFAULT 10 COMMENT 'lower=higher priority',

    -- SMPP config
    smpp_host               VARCHAR(200) DEFAULT '',
    smpp_port               INT DEFAULT 2775,
    smpp_system_id          VARCHAR(50) DEFAULT '',
    smpp_password           VARCHAR(100) DEFAULT '',
    smpp_system_type        VARCHAR(50) DEFAULT '',
    smpp_window_size        INT DEFAULT 50,
    smpp_enquire_link_interval INT DEFAULT 30,

    -- HTTP config
    http_url                VARCHAR(500) DEFAULT '',
    http_method             VARCHAR(10) DEFAULT 'POST',
    http_headers            TEXT COMMENT 'JSON',
    http_body_template      TEXT COMMENT 'Template',

    -- SMS params
    default_encoding        VARCHAR(10) DEFAULT 'AUTO' COMMENT 'GSM7/UCS2/AUTO',
    max_segments            INT DEFAULT 10,
    dlr_wait_timeout        INT DEFAULT 300 COMMENT 'seconds',
    retry_count             INT DEFAULT 3,

    cost_price              DECIMAL(10,6) DEFAULT 0 COMMENT 'vendor cost per segment',
    cost_currency           VARCHAR(3) DEFAULT 'USD',
    is_active               TINYINT(1) NOT NULL DEFAULT 1,
    remark                  VARCHAR(500) DEFAULT '',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_code (channel_code),
    KEY idx_ch_vendor (vendor_id),
    KEY idx_ch_country (country_code)
) ENGINE=InnoDB COMMENT='通道';

CREATE TABLE channel_group (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name      VARCHAR(100) NOT NULL,
    schedule_mode   TINYINT NOT NULL DEFAULT 1 COMMENT '1=failover, 2=weight, 3=priority_weight',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='通道组';

CREATE TABLE channel_group_member (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    channel_id      BIGINT NOT NULL,
    priority        INT NOT NULL DEFAULT 10,
    weight          INT NOT NULL DEFAULT 100,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_channel (group_id, channel_id),
    KEY idx_gm_channel (channel_id)
) ENGINE=InnoDB COMMENT='通道组成员';

CREATE TABLE sid_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sid             VARCHAR(20) NOT NULL COMMENT 'Sender ID value',
    sid_type        VARCHAR(20) NOT NULL DEFAULT 'ALPHA' COMMENT 'ALPHA/NUMERIC/SHORTCODE',
    sms_type        VARCHAR(20) DEFAULT 'NOTIFICATION' COMMENT 'NOTIFICATION/MARKETING/ALL',
    vendor_id       BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sid_vendor (vendor_id),
    KEY idx_sid_country (country_code)
) ENGINE=InnoDB COMMENT='SID主库';

-- -------------------------------------------
-- 2. Customer
-- -------------------------------------------

CREATE TABLE customer (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code   VARCHAR(50) NOT NULL,
    customer_name   VARCHAR(100) NOT NULL,
    company_name    VARCHAR(200) DEFAULT '',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=active, 2=frozen, 3=closed',
    payment_type    TINYINT NOT NULL DEFAULT 1 COMMENT '1=prepaid, 2=postpaid',
    sales_manager   VARCHAR(100) DEFAULT '',
    contact_name    VARCHAR(100) DEFAULT '',
    contact_email   VARCHAR(200) DEFAULT '',
    contact_phone   VARCHAR(50) DEFAULT '',
    timezone        VARCHAR(50) DEFAULT 'UTC',
    bill_currency   VARCHAR(3) DEFAULT 'USD',
    bill_auto_send  TINYINT(1) DEFAULT 0,
    bill_email      VARCHAR(200) DEFAULT '',
    sid_selectable  TINYINT(1) DEFAULT 0,
    preferred_sid_format VARCHAR(20) DEFAULT 'ALPHA',
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_code (customer_code)
) ENGINE=InnoDB COMMENT='客户';

CREATE TABLE customer_account (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    balance         DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'available balance',
    frozen_amount   DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'frozen for pending msgs',
    credit_limit    DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'for postpaid',
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ca_customer (customer_id)
) ENGINE=InnoDB COMMENT='客户账户';

CREATE TABLE customer_country (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    country_id      BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cc (customer_id, country_id),
    KEY idx_cc_country (country_code)
) ENGINE=InnoDB COMMENT='客户开通国家';

CREATE TABLE customer_country_price (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    sms_attribute   TINYINT DEFAULT NULL COMMENT '1=OTP,2=Txn,3=Notif,4=Mkt, NULL=default',
    price           DECIMAL(10,6) NOT NULL COMMENT 'per segment',
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ccp (customer_id, country_code, sms_attribute),
    KEY idx_ccp_country (country_code)
) ENGINE=InnoDB COMMENT='客户国家报价';

CREATE TABLE customer_api_credential (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    api_key         VARCHAR(100) NOT NULL,
    api_secret      VARCHAR(200) NOT NULL COMMENT 'bcrypt hashed',
    ip_whitelist    VARCHAR(500) DEFAULT '' COMMENT 'comma separated IPs',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_api_key (api_key),
    KEY idx_cred_customer (customer_id)
) ENGINE=InnoDB COMMENT='客户API凭证';

-- -------------------------------------------
-- 3. Message
-- -------------------------------------------

CREATE TABLE message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id      VARCHAR(32) NOT NULL,
    customer_id     BIGINT NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    vendor_id       BIGINT DEFAULT NULL,
    channel_id      BIGINT DEFAULT NULL,
    sid             VARCHAR(20) DEFAULT NULL,
    to_number       VARCHAR(20) NOT NULL,
    content_hash    VARCHAR(64) DEFAULT NULL COMMENT 'SHA-256',
    encoding        TINYINT DEFAULT NULL COMMENT '0=GSM7, 1=UCS2',
    segments        TINYINT DEFAULT 1,
    status          TINYINT NOT NULL DEFAULT 0,
    vendor_msg_id   VARCHAR(64) DEFAULT NULL,
    submit_at       DATETIME DEFAULT NULL,
    deliver_at      DATETIME DEFAULT NULL,
    price           DECIMAL(10,6) DEFAULT NULL,
    cost            DECIMAL(10,6) DEFAULT NULL,
    currency        VARCHAR(3) DEFAULT 'USD',
    error_code      VARCHAR(20) DEFAULT NULL,
    client_ref      VARCHAR(64) DEFAULT NULL,
    callback_status TINYINT DEFAULT 0 COMMENT '0=pending,1=success,2=failed',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_msg_customer_time (customer_id, created_at),
    KEY idx_msg_status (status, created_at),
    KEY idx_msg_vendor_msg (vendor_msg_id)
) ENGINE=InnoDB COMMENT='消息主表';

CREATE TABLE mo_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    mo_message_id   VARCHAR(32) NOT NULL COMMENT '上行消息ID',
    from_number     VARCHAR(20) NOT NULL COMMENT '来源号码',
    to_sid          VARCHAR(20) NOT NULL COMMENT '目标SID',
    content         TEXT COMMENT '上行内容',
    country_code    VARCHAR(5) NOT NULL,
    channel_id      BIGINT DEFAULT NULL,
    channel_name    VARCHAR(100) DEFAULT '',
    match_keyword   VARCHAR(50) DEFAULT '' COMMENT '命中关键词',
    action          VARCHAR(50) DEFAULT '' COMMENT '处理动作',
    push_status     VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PUSHED/FAILED',
    push_url        VARCHAR(500) DEFAULT '',
    push_response   TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mo_message_id (mo_message_id),
    KEY idx_mo_time (created_at),
    KEY idx_mo_country (country_code)
) ENGINE=InnoDB COMMENT='上行记录';

-- -------------------------------------------
-- 4. System
-- -------------------------------------------

CREATE TABLE operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator        VARCHAR(100) NOT NULL,
    module          VARCHAR(50) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    target_type     VARCHAR(50) DEFAULT '',
    target_id       VARCHAR(50) DEFAULT '',
    detail          TEXT COMMENT 'JSON diff',
    ip              VARCHAR(50) DEFAULT '',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ol_operator (operator),
    KEY idx_ol_time (created_at)
) ENGINE=InnoDB COMMENT='操作日志';
