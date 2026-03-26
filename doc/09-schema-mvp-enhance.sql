-- =============================================
-- MVP Enhancement: competitive analysis gap fill
-- Date: 2026-03-19
-- =============================================

-- 1. Customer: add agent hierarchy support
ALTER TABLE customer
    ADD COLUMN customer_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=direct, 2=agent' AFTER payment_type,
    ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT 'parent customer ID for agent hierarchy' AFTER customer_type,
    ADD KEY idx_customer_parent (parent_id);

-- 2. Customer Country Price: add billing mode
ALTER TABLE customer_country_price
    ADD COLUMN billing_mode TINYINT DEFAULT NULL COMMENT '1=submit, 2=delivered, 3=reached; NULL=use default(delivered)' AFTER price;

-- 3. Message: add sms_attribute and billing_mode for tracking
ALTER TABLE message
    ADD COLUMN sms_attribute TINYINT DEFAULT NULL COMMENT '1=OTP,2=Txn,3=Notif,4=Mkt' AFTER to_number,
    ADD COLUMN billing_mode TINYINT DEFAULT NULL COMMENT '1=submit,2=delivered,3=reached; snapshot at send time' AFTER sms_attribute,
    ADD KEY idx_msg_sms_attr (sms_attribute);

-- 4. Optout list: dedicated table for unsubscribe management
CREATE TABLE IF NOT EXISTS optout_list (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number    VARCHAR(20) NOT NULL COMMENT 'E.164 phone number',
    country_code    VARCHAR(5) DEFAULT NULL COMMENT 'country code, null=all',
    customer_id     BIGINT DEFAULT NULL COMMENT 'null=global optout',
    source          VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MO_REPLY/MANUAL/API/COMPLAINT',
    keyword         VARCHAR(50) DEFAULT NULL COMMENT 'trigger keyword (STOP/TD/etc)',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_optout_phone (phone_number),
    KEY idx_optout_customer (customer_id),
    KEY idx_optout_phone_customer (phone_number, customer_id)
) ENGINE=InnoDB COMMENT='退订名单';
